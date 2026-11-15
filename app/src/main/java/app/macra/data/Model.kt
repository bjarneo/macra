package app.macra.data

import kotlinx.serialization.Serializable

@Serializable
enum class Meal(val label: String) {
    Breakfast("Breakfast"),
    Lunch("Lunch"),
    Dinner("Dinner"),
    Snacks("Snacks"),
}

@Serializable
data class Serving(val label: String, val g: Int)

/** Nutrition values are per 100 g. Sodium is in mg, the rest in g. */
@Serializable
data class Food(
    val id: String,
    val name: String,
    val src: String = "Generic",
    val kcal: Double,
    val p: Double,
    val c: Double,
    val fat: Double,
    val fib: Double = 0.0,
    val sug: Double = 0.0,
    val sat: Double = 0.0,
    val na: Double = 0.0,
    val serv: List<Serving> = emptyList(),
    val def: Int = 100,
    val barcode: String? = null,
)

data class Nutr(
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val c: Double = 0.0,
    val fat: Double = 0.0,
    val fib: Double = 0.0,
    val sug: Double = 0.0,
    val sat: Double = 0.0,
    val na: Double = 0.0,
) {
    operator fun plus(o: Nutr) = Nutr(kcal + o.kcal, p + o.p, c + o.c, fat + o.fat, fib + o.fib, sug + o.sug, sat + o.sat, na + o.na)
    operator fun minus(o: Nutr) = Nutr(kcal - o.kcal, p - o.p, c - o.c, fat - o.fat, fib - o.fib, sug - o.sug, sat - o.sat, na - o.na)
}

fun Food.per(g: Double): Nutr {
    val r = g / 100.0
    return Nutr(kcal * r, p * r, c * r, fat * r, fib * r, sug * r, sat * r, na * r)
}

@Serializable
data class Entry(val id: Long, val food: String, val g: Int)

@Serializable
data class WeightEntry(val day: Long, val kg: Double)

@Serializable
data class Goals(val p: Int = 180, val c: Int = 230, val fat: Int = 70, val water: Int = 3000) {
    val kcal: Int get() = p * 4 + c * 4 + fat * 9
}

@Serializable
data class Profile(val goalKg: Double? = null)

@Serializable
data class Settings(
    val theme: String = "Dark",
    val remMeals: Boolean = false,
    val remWater: Boolean = false,
    val remWeigh: Boolean = false,
    val healthConnect: Boolean = false,
)

typealias DayLog = Map<Meal, List<Entry>>

@Serializable
data class Data(
    val days: Map<Long, DayLog> = emptyMap(),
    val water: Map<Long, Int> = emptyMap(),
    /** Sorted by day, one entry for each day. */
    val weights: List<WeightEntry> = emptyList(),
    val goals: Goals = Goals(),
    val profile: Profile = Profile(),
    val settings: Settings = Settings(),
    /** Foods from scans and online search. */
    val foods: List<Food> = emptyList(),
    val nextId: Long = 1,
) {
    fun food(id: String): Food = FOOD_MAP[id] ?: foods.firstOrNull { it.id == id } ?: UNKNOWN_FOOD.copy(id = id)

    fun dayEntries(day: Long): List<Entry> = days[day]?.let { d -> Meal.entries.flatMap { d[it].orEmpty() } }.orEmpty()

    fun total(list: List<Entry>): Nutr = list.fold(Nutr()) { a, e -> a + food(e.food).per(e.g.toDouble()) }

    fun dayTotal(day: Long): Nutr = total(dayEntries(day))

    fun weightOn(day: Long): Double? = weights.firstOrNull { it.day == day }?.kg

    fun withWeight(day: Long, kg: Double): Data {
        val list = weights.filter { it.day != day } + WeightEntry(day, kg)
        return copy(weights = list.sortedBy { it.day })
    }
}

private val UNKNOWN_FOOD = Food(id = "", name = "Unknown food", kcal = 0.0, p = 0.0, c = 0.0, fat = 0.0)

val FOODS: List<Food> = listOf(
    Food("chicken", "Chicken breast, grilled", kcal = 165.0, p = 31.0, c = 0.0, fat = 3.6, fib = 0.0, sug = 0.0, sat = 1.0, na = 74.0, serv = listOf(Serving("1 breast", 174)), def = 150),
    Food("oats", "Oats, rolled", kcal = 379.0, p = 13.2, c = 67.7, fat = 6.5, fib = 10.1, sug = 1.0, sat = 1.1, na = 6.0, serv = listOf(Serving("½ cup", 40)), def = 60),
    Food("yogurt", "Greek yogurt, 0% fat", kcal = 59.0, p = 10.3, c = 3.6, fat = 0.4, fib = 0.0, sug = 3.2, sat = 0.1, na = 36.0, serv = listOf(Serving("1 cup", 245)), def = 200),
    Food("banana", "Banana", kcal = 89.0, p = 1.1, c = 22.8, fat = 0.3, fib = 2.6, sug = 12.2, sat = 0.1, na = 1.0, serv = listOf(Serving("1 medium", 118)), def = 118),
    Food("rice", "White rice, cooked", kcal = 130.0, p = 2.7, c = 28.2, fat = 0.3, fib = 0.4, sug = 0.1, sat = 0.1, na = 1.0, serv = listOf(Serving("1 cup", 158)), def = 180),
    Food("egg", "Egg, whole", kcal = 143.0, p = 12.6, c = 0.7, fat = 9.5, fib = 0.0, sug = 0.4, sat = 3.1, na = 142.0, serv = listOf(Serving("1 large", 50)), def = 100),
    Food("salmon", "Salmon, Atlantic, baked", kcal = 206.0, p = 22.0, c = 0.0, fat = 12.4, fib = 0.0, sug = 0.0, sat = 2.5, na = 61.0, serv = listOf(Serving("1 fillet", 154)), def = 150),
    Food("broccoli", "Broccoli, steamed", kcal = 35.0, p = 2.4, c = 7.2, fat = 0.4, fib = 3.3, sug = 1.4, sat = 0.1, na = 41.0, serv = listOf(Serving("1 cup", 156)), def = 120),
    Food("whey", "Whey protein isolate", kcal = 370.0, p = 86.0, c = 4.0, fat = 1.5, fib = 0.0, sug = 2.0, sat = 0.8, na = 180.0, serv = listOf(Serving("1 scoop", 30)), def = 30),
    Food("almonds", "Almonds", kcal = 579.0, p = 21.2, c = 21.6, fat = 49.9, fib = 12.5, sug = 4.4, sat = 3.8, na = 1.0, serv = listOf(Serving("1 handful", 28)), def = 28),
    Food("avocado", "Avocado", kcal = 160.0, p = 2.0, c = 8.5, fat = 14.7, fib = 6.7, sug = 0.7, sat = 2.1, na = 7.0, serv = listOf(Serving("½ fruit", 68)), def = 68),
    Food("sweetpotato", "Sweet potato, baked", kcal = 90.0, p = 2.0, c = 20.7, fat = 0.2, fib = 3.3, sug = 6.5, sat = 0.1, na = 36.0, serv = listOf(Serving("1 medium", 114)), def = 200),
    Food("bread", "Wholemeal bread", kcal = 247.0, p = 13.0, c = 41.0, fat = 3.4, fib = 7.0, sug = 6.0, sat = 0.7, na = 450.0, serv = listOf(Serving("1 slice", 36)), def = 72),
    Food("pb", "Peanut butter, smooth", kcal = 588.0, p = 25.0, c = 20.0, fat = 50.0, fib = 6.0, sug = 9.0, sat = 10.0, na = 430.0, serv = listOf(Serving("1 tbsp", 16)), def = 16),
    Food("oil", "Olive oil, extra virgin", kcal = 884.0, p = 0.0, c = 0.0, fat = 100.0, fib = 0.0, sug = 0.0, sat = 13.8, na = 2.0, serv = listOf(Serving("1 tbsp", 14)), def = 10),
    Food("skyr", "Skyr, plain", kcal = 63.0, p = 11.0, c = 4.0, fat = 0.2, fib = 0.0, sug = 4.0, sat = 0.1, na = 40.0, serv = listOf(Serving("1 pot", 150)), def = 150),
    Food("beef", "Beef mince, 5% fat", kcal = 137.0, p = 21.4, c = 0.0, fat = 5.0, fib = 0.0, sug = 0.0, sat = 2.2, na = 66.0, serv = listOf(Serving("1 pack", 250)), def = 125),
    Food("blueberries", "Blueberries", kcal = 57.0, p = 0.7, c = 14.5, fat = 0.3, fib = 2.4, sug = 10.0, sat = 0.0, na = 1.0, serv = listOf(Serving("1 cup", 148)), def = 80),
    Food("cottage", "Cottage cheese, low fat", kcal = 72.0, p = 12.4, c = 2.7, fat = 1.0, fib = 0.0, sug = 2.7, sat = 0.6, na = 406.0, serv = listOf(Serving("½ cup", 113)), def = 150),
    Food("pasta", "Pasta, cooked", kcal = 158.0, p = 5.8, c = 30.9, fat = 0.9, fib = 1.8, sug = 0.6, sat = 0.2, na = 1.0, serv = listOf(Serving("1 cup", 140)), def = 180),
    Food("milk", "Milk, semi-skimmed", kcal = 47.0, p = 3.5, c = 4.8, fat = 1.7, fib = 0.0, sug = 4.8, sat = 1.1, na = 44.0, serv = listOf(Serving("1 glass", 250)), def = 250),
)

val FOOD_MAP: Map<String, Food> = FOODS.associateBy { it.id }
