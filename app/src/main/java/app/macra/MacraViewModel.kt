package app.macra

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.macra.data.Csv
import app.macra.data.Data
import app.macra.data.Entry
import app.macra.data.FOOD_MAP
import app.macra.data.Food
import app.macra.data.Meal
import app.macra.data.OpenFoodFacts
import app.macra.data.Settings
import app.macra.data.Store
import app.macra.health.Health
import app.macra.reminders.Reminders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

enum class Tab { Diary, Progress, Settings }

enum class Screen { Search, Scan, Food, Weight }

data class FoodDraft(
    val food: Food,
    val g: String,
    val meal: Meal,
    val edit: Boolean,
    val origG: Int = 0,
    val origMeal: Meal? = null,
    val entryId: Long? = null,
)

sealed interface ScanState {
    data object Scanning : ScanState
    data class LookingUp(val code: String) : ScanState
    data class Missing(val code: String, val noNutrition: Boolean) : ScanState
    data class Failed(val code: String) : ScanState
}

class Snack(val msg: String, val undo: (() -> Unit)?)

data class Online(val query: String = "", val loading: Boolean = false, val results: List<Food> = emptyList(), val failed: Boolean = false)

data class Ui(
    val tab: Tab = Tab.Diary,
    val stack: List<Screen> = emptyList(),
    val today: Long,
    val day: Long,
    val anchor: Long,
    val query: String = "",
    val searchMeal: Meal = Meal.Lunch,
    val fd: FoodDraft? = null,
    val scan: ScanState = ScanState.Scanning,
    val torch: Boolean = false,
    val wDraft: Double = 75.0,
    val range: Int = 30,
    val barSel: Int = 6,
    val snack: Snack? = null,
    val online: Online = Online(),
) {
    val top: Screen? get() = stack.lastOrNull()
}

class MacraViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Application get() = getApplication()

    private val _data = MutableStateFlow(Store.load(app))
    val data: StateFlow<Data> = _data.asStateFlow()

    private val _ui = MutableStateFlow(LocalDate.now().toEpochDay().let { Ui(today = it, day = it, anchor = it) })
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val io = Dispatchers.IO.limitedParallelism(1)
    private var saveJob: Job? = null
    private var snackJob: Job? = null
    private var searchJob: Job? = null
    private var ignoreCode: String? = null

    private fun ui(f: (Ui) -> Ui) = _ui.update(f)

    private fun update(f: (Data) -> Data) {
        _data.update(f)
        saveJob?.cancel()
        saveJob = viewModelScope.launch(io) {
            delay(250)
            Store.save(ctx, _data.value)
        }
    }

    fun flush() {
        saveJob?.cancel()
        val snapshot = _data.value
        viewModelScope.launch(io) { Store.save(ctx, snapshot) }
    }

    override fun onCleared() {
        Store.save(ctx, _data.value)
    }

    fun onResume() {
        val t = LocalDate.now().toEpochDay()
        ui { u ->
            if (u.today == t) u
            else u.copy(
                today = t,
                day = if (u.day == u.today) t else minOf(u.day, t),
                anchor = if (u.anchor == u.today) t else minOf(u.anchor, t),
                barSel = 6,
            )
        }
        Reminders.schedule(ctx, _data.value.settings)
        if (_data.value.settings.healthConnect) syncHealth(announce = false)
    }

    // Navigation

    fun setTab(t: Tab) = ui { it.copy(tab = t, stack = emptyList(), fd = null) }

    fun goBack() = ui { u ->
        val popped = u.stack.lastOrNull()
        u.copy(stack = u.stack.dropLast(1), fd = if (popped == Screen.Food) null else u.fd, torch = if (popped == Screen.Scan) false else u.torch)
    }

    fun handleAction(action: String?) {
        if (action.isNullOrEmpty()) return
        val t = LocalDate.now().toEpochDay()
        when {
            action == "weigh" -> {
                ui { it.copy(today = t, tab = Tab.Progress, stack = emptyList(), fd = null) }
                openWeight()
            }
            action == "water" -> ui { it.copy(today = t, day = t, anchor = t, tab = Tab.Diary, stack = emptyList(), fd = null) }
            action.startsWith("meal:") -> {
                val meal = Meal.entries.firstOrNull { it.name == action.removePrefix("meal:") }
                ui { it.copy(today = t, day = t, anchor = t, tab = Tab.Diary, stack = emptyList(), fd = null) }
                openSearch(meal)
            }
        }
    }

    // Days

    fun selectDay(day: Long) = ui { it.copy(day = minOf(day, it.today)) }

    fun shiftWeek(dir: Int) = ui { it.copy(anchor = (it.anchor + 7L * dir).coerceAtMost(it.today)) }

    fun jumpTo(day: Long) = ui { u ->
        val d = minOf(day, u.today)
        u.copy(day = d, anchor = if (d in (u.anchor - 3)..(u.anchor + 3)) u.anchor else minOf(d + 3, u.today))
    }

    // Food logging

    private fun mealForNow(): Meal {
        val m = LocalTime.now().let { it.hour * 60 + it.minute }
        return when {
            m < 10 * 60 + 30 -> Meal.Breakfast
            m < 14 * 60 + 30 -> Meal.Lunch
            m < 17 * 60 -> Meal.Snacks
            m < 21 * 60 + 30 -> Meal.Dinner
            else -> Meal.Snacks
        }
    }

    fun openSearch(meal: Meal? = null) {
        searchJob?.cancel()
        ui { it.copy(stack = listOf(Screen.Search), query = "", searchMeal = meal ?: mealForNow(), online = Online()) }
    }

    fun setSearchMeal(m: Meal) = ui { it.copy(searchMeal = m) }

    fun setQuery(q: String) {
        ui { it.copy(query = q) }
        runOnline(q, debounce = true)
    }

    fun submitQuery() = runOnline(_ui.value.query, debounce = false)

    private fun runOnline(q: String, debounce: Boolean) {
        val t = q.trim()
        if (t.length < 3) {
            searchJob?.cancel()
            ui { it.copy(online = Online()) }
            return
        }
        if (!debounce && _ui.value.online.query == t && !_ui.value.online.failed) return
        searchJob?.cancel()
        ui { it.copy(online = it.online.copy(query = t, loading = true, failed = false)) }
        searchJob = viewModelScope.launch {
            if (debounce) delay(600)
            try {
                val r = OpenFoodFacts.search(t)
                if (_ui.value.query.trim() == t) ui { it.copy(online = Online(t, false, r)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_ui.value.query.trim() == t) ui { it.copy(online = Online(t, false, emptyList(), failed = true)) }
            }
        }
    }

    /** Foods logged recently, newest first. */
    fun recent(d: Data, today: Long): List<Food> {
        val seen = mutableSetOf<String>()
        val out = mutableListOf<Food>()
        for (day in today downTo today - 13) {
            d.dayEntries(day).asReversed().forEach { e -> if (seen.add(e.food)) out += d.food(e.food) }
        }
        return out.take(12)
    }

    fun openFood(food: Food) = ui { u ->
        u.copy(fd = FoodDraft(food, food.def.toString(), u.searchMeal, edit = false), stack = u.stack + Screen.Food)
    }

    fun openEdit(meal: Meal, e: Entry) = ui {
        it.copy(fd = FoodDraft(_data.value.food(e.food), e.g.toString(), meal, true, e.g, meal, e.id), stack = listOf(Screen.Food))
    }

    fun setFdG(v: String) = ui { u -> u.fd?.let { u.copy(fd = it.copy(g = v.filter(Char::isDigit).take(5))) } ?: u }

    fun stepFd(d: Int) = ui { u ->
        u.fd?.let {
            val g = ((it.g.toIntOrNull() ?: 0) + d).toDouble()
            u.copy(fd = it.copy(g = maxOf(0, (Math.round(g / 5) * 5).toInt()).toString()))
        } ?: u
    }

    fun setFdMeal(m: Meal) = ui { u -> u.fd?.let { u.copy(fd = it.copy(meal = m)) } ?: u }

    private fun remember(d: Data, f: Food): Data =
        if (FOOD_MAP.containsKey(f.id) || d.foods.any { it.id == f.id }) d else d.copy(foods = d.foods + f)

    private fun addEntry(d: Data, day: Long, meal: Meal, foodId: String, g: Int): Data {
        val log = d.days[day].orEmpty()
        val list = log[meal].orEmpty() + Entry(d.nextId, foodId, g)
        return d.copy(days = d.days + (day to (log + (meal to list))), nextId = d.nextId + 1)
    }

    private fun restoreDays(prev: Data) = update { it.copy(days = prev.days) }

    fun quickAdd(food: Food) {
        val u = _ui.value
        val prev = _data.value
        update { addEntry(remember(it, food), u.day, u.searchMeal, food.id, food.def) }
        snack("Added to ${u.searchMeal.label}: ${food.name}, ${food.def} g") { restoreDays(prev) }
    }

    fun saveFd() {
        val u = _ui.value
        val fd = u.fd ?: return
        val g = fd.g.toIntOrNull() ?: 0
        if (g <= 0) return
        val prev = _data.value
        update { d0 ->
            val d = remember(d0, fd.food)
            if (!fd.edit) {
                addEntry(d, u.day, fd.meal, fd.food.id, g)
            } else {
                val log = d.days[u.day].orEmpty().toMutableMap()
                val orig = fd.origMeal ?: fd.meal
                if (fd.meal == orig) {
                    log[fd.meal] = log[fd.meal].orEmpty().map { if (it.id == fd.entryId) it.copy(g = g) else it }
                } else {
                    log[orig] = log[orig].orEmpty().filter { it.id != fd.entryId }
                    log[fd.meal] = log[fd.meal].orEmpty() + Entry(fd.entryId ?: d.nextId, fd.food.id, g)
                }
                d.copy(days = d.days + (u.day to log))
            }
        }
        ui {
            var st = it.stack.dropLast(1)
            if (st.lastOrNull() == Screen.Scan) st = st.dropLast(1)
            if (fd.edit) st = emptyList()
            it.copy(stack = st, fd = null, torch = false)
        }
        snack(if (fd.edit) "Updated ${fd.food.name}, $g g" else "Added to ${fd.meal.label}: ${fd.food.name}, $g g") { restoreDays(prev) }
    }

    fun removeFd() {
        val u = _ui.value
        val fd = u.fd ?: return
        val orig = fd.origMeal ?: return
        val prev = _data.value
        update { d ->
            val log = d.days[u.day].orEmpty().toMutableMap()
            log[orig] = log[orig].orEmpty().filter { it.id != fd.entryId }
            d.copy(days = d.days + (u.day to log))
        }
        ui { it.copy(stack = emptyList(), fd = null) }
        snack("Removed ${fd.food.name}") { restoreDays(prev) }
    }

    // Water

    fun setWater(ml: Int) {
        val day = _ui.value.day
        update { it.copy(water = it.water + (day to ml.coerceIn(0, 8000))) }
    }

    // Scan

    fun openScan() = ui { u ->
        ignoreCode = null
        val fromSearch = u.top == Screen.Search
        u.copy(
            stack = if (fromSearch) u.stack + Screen.Scan else listOf(Screen.Scan),
            scan = ScanState.Scanning,
            torch = false,
            searchMeal = if (fromSearch) u.searchMeal else mealForNow(),
        )
    }

    fun onBarcode(code: String) {
        if (_ui.value.scan != ScanState.Scanning || _ui.value.top != Screen.Scan || code == ignoreCode) return
        val local = _data.value.foods.firstOrNull { it.barcode == code || it.barcode == "0$code" || "0${it.barcode}" == code }
        if (local != null) {
            found(local, code)
            return
        }
        val looking = ScanState.LookingUp(code)
        ui { it.copy(scan = looking) }
        viewModelScope.launch {
            val r = OpenFoodFacts.product(code)
            if (_ui.value.scan != looking) return@launch
            if (r is OpenFoodFacts.Lookup.Found) {
                found(r.food, code)
                return@launch
            }
            ui {
                it.copy(
                    scan = when (r) {
                        is OpenFoodFacts.Lookup.Found -> ScanState.Scanning
                        OpenFoodFacts.Lookup.NotFound -> ScanState.Missing(code, noNutrition = false)
                        OpenFoodFacts.Lookup.NoNutrition -> ScanState.Missing(code, noNutrition = true)
                        is OpenFoodFacts.Lookup.Failed -> ScanState.Failed(code)
                    },
                )
            }
        }
    }

    fun retryLookup() {
        val s = _ui.value.scan as? ScanState.Failed ?: return
        ui { it.copy(scan = ScanState.Scanning) }
        onBarcode(s.code)
    }

    /** Opens the product. The scanner then ignores this barcode, so that it does not open the product again. */
    private fun found(food: Food, code: String) {
        ignoreCode = code
        ui { it.copy(scan = ScanState.Scanning) }
        openFood(food)
    }

    fun scanAgain() = ui { it.copy(scan = ScanState.Scanning) }

    fun scanToSearch() = ui { u ->
        val i = u.stack.indexOf(Screen.Search)
        if (i >= 0) u.copy(stack = u.stack.take(i + 1), torch = false) else u.copy(stack = listOf(Screen.Search), query = "", online = Online(), torch = false)
    }

    fun toggleTorch() = ui { it.copy(torch = !it.torch) }

    // Weight

    fun openWeight() {
        val d = _data.value
        val t = _ui.value.today
        ui { it.copy(stack = listOf(Screen.Weight), wDraft = d.weightOn(t) ?: d.weights.lastOrNull()?.kg ?: 75.0) }
    }

    fun stepWeight(delta: Double) = ui { it.copy(wDraft = (Math.round((it.wDraft + delta) * 10) / 10.0).coerceIn(20.0, 400.0)) }

    fun saveWeight() {
        val kg = Math.round(_ui.value.wDraft * 10) / 10.0
        val today = _ui.value.today
        update { it.withWeight(today, kg) }
        ui { it.copy(stack = emptyList()) }
        snack(String.format(java.util.Locale.US, "Logged %.1f kg for today", kg))
        if (_data.value.settings.healthConnect) {
            viewModelScope.launch {
                try {
                    Health.writeWeight(ctx, today, kg)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun setRange(days: Int) = ui { it.copy(range = days) }

    fun selectBar(i: Int) = ui { it.copy(barSel = i) }

    // Goals, profile, settings

    fun stepGoal(key: String, d: Int, min: Int, max: Int) = update { s ->
        val g = s.goals
        s.copy(
            goals = when (key) {
                "p" -> g.copy(p = (g.p + d).coerceIn(min, max))
                "c" -> g.copy(c = (g.c + d).coerceIn(min, max))
                "fat" -> g.copy(fat = (g.fat + d).coerceIn(min, max))
                else -> g.copy(water = (g.water + d).coerceIn(min, max))
            },
        )
    }

    /** The first step sets the goal to the latest weight. Later steps change it by [delta] kg. */
    fun stepGoalWeight(delta: Double) = update { d ->
        val goal = d.profile.goalKg
        val next = if (goal == null) {
            d.weights.lastOrNull()?.kg?.let { Math.round(it * 2) / 2.0 } ?: 75.0
        } else {
            (Math.round((goal + delta) * 2) / 2.0).coerceIn(30.0, 300.0)
        }
        d.copy(profile = d.profile.copy(goalKg = next))
    }

    fun setSettings(f: (Settings) -> Settings) {
        update { it.copy(settings = f(it.settings)) }
        Reminders.schedule(ctx, _data.value.settings)
    }

    fun onHealthResult(granted: Set<String>) {
        if (granted.containsAll(Health.PERMISSIONS)) {
            setSettings { it.copy(healthConnect = true) }
            syncHealth(announce = true)
        } else {
            snack("Health Connect access is off. Allow weight access to sync.")
        }
    }

    fun syncHealth(announce: Boolean) {
        viewModelScope.launch {
            try {
                if (!Health.granted(ctx)) {
                    setSettings { it.copy(healthConnect = false) }
                    if (announce) snack("Health Connect access is off")
                    return@launch
                }
                val today = _ui.value.today
                val remote = Health.readWeights(ctx, 365)
                val localDays = _data.value.weights.map { it.day }.toSet()
                val add = remote.filter { it.day !in localDays }
                if (add.isNotEmpty()) update { d -> add.fold(d) { acc, w -> acc.withWeight(w.day, w.kg) } }
                val remoteDays = remote.map { it.day }.toSet()
                val push = _data.value.weights.filter { it.day !in remoteDays && it.day > today - 365 }
                push.forEach { Health.writeWeight(ctx, it.day, it.kg) }
                if (announce) snack("Health Connect: ${add.size} weights in, ${push.size} out")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (announce) snack("Health Connect sync failed. Try again later.")
            }
        }
    }

    fun export(uri: Uri, name: String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    ctx.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(Csv.build(_data.value)) } != null
                } catch (_: Exception) {
                    false
                }
            }
            snack(if (ok) "Exported $name" else "Export failed. Pick a different folder.")
        }
    }

    // Snackbar

    fun snack(msg: String, undo: (() -> Unit)? = null) {
        snackJob?.cancel()
        ui { it.copy(snack = Snack(msg, undo)) }
        snackJob = viewModelScope.launch {
            delay(4000)
            ui { it.copy(snack = null) }
        }
    }

    fun undo() {
        val u = _ui.value.snack?.undo
        snackJob?.cancel()
        ui { it.copy(snack = null) }
        u?.invoke()
    }

    fun gramsOf(fd: FoodDraft): Int = fd.g.toIntOrNull() ?: 0
}
