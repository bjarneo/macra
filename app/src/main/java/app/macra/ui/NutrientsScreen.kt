package app.macra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.macra.MacraViewModel
import app.macra.Ui
import app.macra.data.Data
import app.macra.data.Meal
import kotlin.math.roundToInt

@Composable
fun NutrientsScreen(d: Data, u: Ui, vm: MacraViewModel) {
    val T = LocalT.current
    val g = d.goals
    val goalK = g.kcal.toDouble()
    val tot = d.dayTotal(u.day)
    val kS2 = (tot.p * 4 + tot.c * 4 + tot.fat * 9).takeIf { it > 0 } ?: 1.0
    val cur = d.weights.lastOrNull()?.kg
    val macros = listOf(Triple("Protein", tot.p, g.p) to (T.p to 4), Triple("Carbs", tot.c, g.c) to (T.c to 4), Triple("Fat", tot.fat, g.fat) to (T.f to 9))

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconBtn("arrow_back", vm::goBack, desc = "Back")
            Column {
                Txt("Nutrition", 18f, 750)
                Txt(dateSub(u.day), 12f, color = T.muted)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(gap = 14) {
                Label("Energy split")
                BigNum(fmt(tot.kcal), "of ${fmt(goalK)} kcal", 44f, 14f, ls = -0.045f)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Split(macros.map { (m, cm) -> (m.second * cm.second).toFloat() to cm.first }, 14.dp, 4.dp)
                    Split(macros.map { (m, cm) -> (m.third * cm.second).toFloat() to cm.first }, 5.dp, 3.dp, Modifier.alpha(0.45f))
                    Txt("Top: today · Bottom: target", 11f, color = T.muted)
                }
                macros.forEach { (m, cm) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Dot(cm.first)
                        Txt(m.first, 14f, modifier = Modifier.weight(1f))
                        Txt("${(m.second * cm.second / kS2 * 100).roundToInt()}%", 14f, 750, tnum = true)
                        Txt("target ${(m.third * cm.second / goalK * 100).roundToInt()}%", 14f, color = T.muted, modifier = Modifier.widthIn(min = 86.dp), tnum = true, align = TextAlign.End)
                    }
                }
            }
            macros.forEach { (m, cm) ->
                val (name, e, goal) = m
                val left = goal - e
                Card(radius = 20, pad = PaddingValues(horizontal = 20.dp, vertical = 18.dp), gap = 10) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Dot(cm.first)
                        Label(name)
                    }
                    BigNum(fmt(e), "/ $goal g", 32f, 14f, ls = -0.04f)
                    Meter(pct(e, goal.toDouble()), cm.first, 8.dp)
                    val leftTxt = if (left >= -0.5) "${fmt(left)} g left" else "${fmt(-left)} g over"
                    Txt(if (cur != null) "${r1(e / cur)} g/kg body weight · $leftTxt" else leftTxt, 13f, color = T.muted, tnum = true)
                }
            }
            Card(radius = 20, pad = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp), gap = 0) {
                MealRow(listOf("By meal", "kcal", "P", "C", "F"), header = true)
                Meal.entries.forEach { meal ->
                    val t = d.total(d.days[u.day]?.get(meal).orEmpty())
                    Divider()
                    MealRow(listOf(meal.label, fmt(t.kcal), fmt(t.p), fmt(t.c), fmt(t.fat)))
                }
                Divider()
                MealRow(listOf("Total", fmt(tot.kcal), fmt(tot.p), fmt(tot.c), fmt(tot.fat)), bold = true)
            }
            Card(radius = 20, pad = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
                Label("Other nutrients")
                listOf(
                    listOf("Fibre", tot.fib, 30.0, "g", "min"),
                    listOf("Sugars", tot.sug, 50.0, "g", "max"),
                    listOf("Saturated fat", tot.sat, 25.0, "g", "max"),
                    listOf("Sodium", tot.na, 2300.0, "mg", "max"),
                ).forEach { row ->
                    val name = row[0] as String
                    val v = row[1] as Double
                    val goal = row[2] as Double
                    val unit = row[3] as String
                    val min = row[4] == "min"
                    val over = !min && v > goal
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Txt(name, 15f, 600, modifier = Modifier.weight(1f).alignByBaseline())
                            Txt("${fmt(v)} $unit", 15f, 750, modifier = Modifier.alignByBaseline(), tnum = true)
                        }
                        Meter(pct(v, goal), if (over) T.acc else T.ink, 6.dp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Txt("${if (min) "Target" else "Limit"} ${fmt(goal)} $unit", 12f, color = T.muted, tnum = true)
                            val status = when {
                                min -> if (v >= goal) "Met" else "${fmt(goal - v)} $unit to go"
                                over -> "${fmt(v - goal)} $unit over"
                                else -> "${fmt(goal - v)} $unit left"
                            }
                            Txt(status, 12f, color = if (over) T.acc else T.muted, tnum = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealRow(cells: List<String>, header: Boolean = false, bold: Boolean = false) {
    val T = LocalT.current
    Row(
        Modifier.fillMaxWidth().padding(top = if (header) 0.dp else 11.dp, bottom = if (header) 10.dp else 11.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cells.forEachIndexed { i, c ->
            val m = Modifier.weight(if (i == 0) 1.5f else 1f)
            val align = if (i == 0) TextAlign.Start else TextAlign.End
            if (header) {
                Txt(c.uppercase(), 11f, 700, T.muted, m, ls = 0.08f, align = align, maxLines = 1)
            } else {
                Txt(c, 14f, if (bold) 750 else 400, T.ink, m, tnum = true, align = align, maxLines = 1)
            }
        }
    }
}
