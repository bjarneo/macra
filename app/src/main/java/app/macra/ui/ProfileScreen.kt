@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.macra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.macra.MacraViewModel
import app.macra.Ui
import app.macra.data.Data
import app.macra.data.Profile
import kotlin.math.roundToInt

val ACTIVITIES = listOf("Sedentary", "Light", "Moderate", "Active", "Very active")

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1).uppercase() }

@Composable
fun ProfileScreen(
    d: Data,
    u: Ui,
    vm: MacraViewModel,
    onToggle: (String) -> Unit,
    onExport: () -> Unit,
) {
    val T = LocalT.current
    val p = d.profile
    val s = d.settings
    val g = d.goals
    val goalK = g.kcal
    val cur = d.weights.lastOrNull()?.kg
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = vm::openEditProfile).padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(T.acc), contentAlignment = Alignment.Center) {
                val ini = initials(p.name)
                if (ini.isNotEmpty()) Txt(ini, 22f, 800, T.onAcc, ls = -0.02f) else Sym("person", 30, T.onAcc, fill = true)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Txt(p.name.ifBlank { "Your profile" }, 24f, 800, ls = -0.03f, maxLines = 1, ellipsis = true)
                val goal = p.goalKg
                val sub = when {
                    cur == null && goal == null -> "Tap to add your details"
                    cur == null -> "Goal ${kg1(goal!!)} kg · log a weight to start"
                    goal == null -> "${kg1(cur)} kg · set a goal weight"
                    goal < cur - 0.05 -> "Cutting · ${kg1(cur)} → ${kg1(goal)} kg"
                    goal > cur + 0.05 -> "Gaining · ${kg1(cur)} → ${kg1(goal)} kg"
                    else -> "Maintaining · ${kg1(cur)} kg"
                }
                Txt(sub, 13f, color = T.muted, tnum = true)
            }
            Sym("edit", 20, T.muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Height" to (p.heightCm?.let { "$it cm" } ?: "—"),
                "Age" to (p.age?.toString() ?: "—"),
                "Activity" to p.activity,
            ).forEach { (k, v) ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(T.surface).clickable(onClick = vm::openEditProfile).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Label(k)
                    Txt(v, 18f, 750, maxLines = 1, ellipsis = true)
                }
            }
        }

        Label("Daily targets", Modifier.padding(start = 4.dp, top = 12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(T.surface)) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Txt("Energy", 16f, 650)
                    Txt("Calculated from macro targets", 12f, color = T.muted)
                }
                Txt("${fmt(goalK.toDouble())} kcal", 20f, 800, ls = -0.02f, tnum = true)
            }
            fun sub(grams: Int, kcalPer: Int): String {
                val share = "${(grams * kcalPer * 100.0 / goalK).roundToInt()}% of energy"
                return if (cur != null) "${r1(grams / cur)} g/kg · $share" else share
            }
            GoalRow("Protein", T.p, sub(g.p, 4), "${g.p} g", { vm.stepGoal("p", -5, 40, 400) }, { vm.stepGoal("p", 5, 40, 400) })
            GoalRow("Carbs", T.c, sub(g.c, 4), "${g.c} g", { vm.stepGoal("c", -5, 0, 700) }, { vm.stepGoal("c", 5, 0, 700) })
            GoalRow("Fat", T.f, sub(g.fat, 9), "${g.fat} g", { vm.stepGoal("fat", -5, 20, 250) }, { vm.stepGoal("fat", 5, 20, 250) })
            GoalRow("Water", T.w, "Logged in 250 ml steps", litres(g.water), { vm.stepGoal("water", -250, 1000, 6000) }, { vm.stepGoal("water", 250, 1000, 6000) })
        }

        Label("Display", Modifier.padding(start = 4.dp, top = 12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(T.surface)) {
            SettingRow("Theme", first = true) {
                Seg(listOf("Dark" to "Dark", "Light" to "Light"), s.theme, { v -> vm.setSettings { it.copy(theme = v) } })
            }
            SettingRow("Accent") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ACCENTS.forEach { hex ->
                        val c = parseHex(hex) ?: Color.Red
                        val on = hex.equals(s.accent, ignoreCase = true)
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .border(2.dp, if (on) T.ink else Color.Transparent, CircleShape)
                                .clickable { vm.setSettings { it.copy(accent = hex) } }
                                .padding(5.dp).clip(CircleShape).background(c),
                        )
                    }
                }
            }
            SettingRow("Summary") {
                Seg(listOf("Ledger" to "Ledger", "Rings" to "Rings", "Blocks" to "Blocks"), s.dash, { v -> vm.setSettings { it.copy(dash = v) } }, padH = 10)
            }
            SettingRow("Show") {
                Seg(listOf("Remaining" to "Remaining", "Eaten" to "Eaten"), s.count, { v -> vm.setSettings { it.copy(count = v) } })
            }
            SettingRow("Logging") {
                Seg(listOf(true to "One-tap", false to "Confirm"), s.quickAdd, { v -> vm.setSettings { it.copy(quickAdd = v) } })
            }
            Divider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Txt("Units", 16f, 650)
                Txt("Metric · kg, kcal", 14f, color = T.muted)
            }
        }

        Label("Reminders & data", Modifier.padding(start = 4.dp, top = 12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(T.surface)) {
            val toggles = listOf(
                Triple("meals", "Meal reminders", "08:00 · 12:30 · 19:00") to s.remMeals,
                Triple("water", "Water reminders", "Every 2 hours, 08:00–20:00") to s.remWater,
                Triple("weigh", "Weigh-in reminder", "Daily at 07:30") to s.remWeigh,
                Triple("health", "Health Connect", if (s.healthConnect) "Syncing weight" else "Not connected") to s.healthConnect,
            )
            toggles.forEachIndexed { i, (t, on) ->
                if (i > 0) Divider()
                Row(
                    Modifier.fillMaxWidth().clickable { onToggle(t.first) }.padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Txt(t.second, 16f, 650)
                        Txt(t.third, 12f, color = T.muted)
                    }
                    Toggle(on)
                }
            }
            Divider()
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onExport).padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Txt("Export diary", 16f, 650)
                    Txt("CSV, all entries, water, and weights", 12f, color = T.muted)
                }
                Sym("download", 24, T.muted)
            }
        }
        Txt("Macra 1.0", 12f, color = T.muted, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), align = TextAlign.Center)
    }
}

@Composable
private fun GoalRow(label: String, color: Color, sub: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val T = LocalT.current
    Divider()
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Dot(color)
                Txt(label, 16f, 650)
            }
            Txt(sub, 12f, color = T.muted, tnum = true)
        }
        StepBtn("remove", onMinus, desc = "Lower $label target")
        Txt(value, 16f, 750, modifier = Modifier.widthIn(min = 62.dp), tnum = true, align = TextAlign.Center, maxLines = 1)
        StepBtn("add", onPlus, desc = "Raise $label target")
    }
}

@Composable
private fun SettingRow(label: String, first: Boolean = false, content: @Composable () -> Unit) {
    if (!first) Divider()
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Txt(label, 16f, 650, modifier = Modifier.weight(1f), maxLines = 1)
        content()
    }
}

@Composable
fun EditProfileSheet(p: Profile, cur: Double?, onCancel: () -> Unit, onSave: (Profile) -> Unit) {
    val T = LocalT.current
    var name by remember { mutableStateOf(p.name) }
    var height by remember { mutableIntStateOf(p.heightCm ?: 175) }
    var age by remember { mutableIntStateOf(p.age ?: 30) }
    var activity by remember { mutableStateOf(p.activity) }
    var goal by remember { mutableDoubleStateOf(p.goalKg ?: cur?.let { Math.round(it * 2) / 2.0 } ?: 75.0) }
    var hasGoal by remember { mutableStateOf(p.goalKg != null) }
    Sheet(onCancel) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label("Your details")
            Txt("Used for g/kg targets and your goal.", 15f, color = T.muted)
        }
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(T.surface2).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                textStyle = textStyle(16f, 600, T.ink),
                cursorBrush = SolidColor(T.ink),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (name.isEmpty()) Txt("Name", 16f, 500, T.muted)
                    inner()
                },
            )
        }
        StepField("Height", "$height cm", { height = (height - 1).coerceAtLeast(100) }, { height = (height + 1).coerceAtMost(240) })
        StepField("Age", "$age", { age = (age - 1).coerceAtLeast(13) }, { age = (age + 1).coerceAtMost(110) })
        StepField(
            "Goal weight",
            if (hasGoal) "${kg1(goal)} kg" else "Not set",
            { hasGoal = true; goal = (goal - 0.5).coerceAtLeast(30.0) },
            { hasGoal = true; goal = (goal + 0.5).coerceAtMost(300.0) },
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Label("Activity")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ACTIVITIES.forEach { a -> Chip(a, a == activity, { activity = a }, 600) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetBtn("Cancel", false, onCancel)
            SheetBtn("Save", true, {
                onSave(p.copy(name = name.trim(), heightCm = height, age = age, activity = activity, goalKg = if (hasGoal) goal else null))
            })
        }
    }
}

@Composable
private fun StepField(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Txt(label, 16f, 650, modifier = Modifier.weight(1f))
        StepBtn("remove", onMinus, desc = "Lower $label")
        Txt(value, 16f, 750, modifier = Modifier.widthIn(min = 84.dp), tnum = true, align = TextAlign.Center, maxLines = 1)
        StepBtn("add", onPlus, desc = "Raise $label")
    }
}
