package app.macra.data

import java.time.LocalDate
import java.util.Locale

/** Builds one CSV file with all diary entries, water, and weights. */
object Csv {
    private fun q(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    private fun n(v: Double) = String.format(Locale.US, "%.1f", v)

    fun build(d: Data): String = buildString {
        appendLine("type,date,meal,food,source,grams,kcal,protein_g,carbs_g,fat_g,fibre_g,sugars_g,saturates_g,sodium_mg,water_ml,weight_kg")
        d.days.keys.sorted().forEach { day ->
            val date = LocalDate.ofEpochDay(day).toString()
            Meal.entries.forEach { meal ->
                d.days[day]?.get(meal).orEmpty().forEach { e ->
                    val f = d.food(e.food)
                    val x = f.per(e.g.toDouble())
                    appendLine(
                        listOf(
                            "food", date, meal.label, q(f.name), q(f.src), e.g.toString(),
                            n(x.kcal), n(x.p), n(x.c), n(x.fat), n(x.fib), n(x.sug), n(x.sat), n(x.na), "", "",
                        ).joinToString(","),
                    )
                }
            }
        }
        d.water.keys.sorted().forEach { day ->
            val ml = d.water[day] ?: 0
            if (ml > 0) appendLine("water,${LocalDate.ofEpochDay(day)},,,,,,,,,,,,,$ml,")
        }
        d.weights.forEach { w ->
            appendLine("weight,${LocalDate.ofEpochDay(w.day)},,,,,,,,,,,,,,${n(w.kg)}")
        }
    }
}
