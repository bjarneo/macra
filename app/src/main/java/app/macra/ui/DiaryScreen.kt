package app.macra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.macra.MacraViewModel
import app.macra.Ui
import app.macra.data.Data
import app.macra.data.Meal
import app.macra.data.Nutr
import app.macra.data.per
import kotlin.math.abs
import kotlin.math.roundToInt

data class MacroView(val name: String, val color: Color, val pct: Float, val main: String, val unit: String)

fun macroViews(tot: Nutr, d: Data, T: Tokens): List<MacroView> {
    val g = d.goals
    return listOf(Triple("Protein", tot.p, g.p) to T.p, Triple("Carbs", tot.c, g.c) to T.c, Triple("Fat", tot.fat, g.fat) to T.f).map { (t, color) ->
        val (name, eaten, goal) = t
        val left = goal - eaten
        MacroView(name, color, pct(eaten, goal.toDouble()), fmt(abs(left)), if (left >= -0.5) "g left" else "g over")
    }
}

fun pct(v: Double, of: Double): Float = if (of <= 0) 0f else (v / of * 100).toFloat().coerceIn(0f, 100f)

@Composable
fun DiaryScreen(d: Data, u: Ui, vm: MacraViewModel, onPickDate: () -> Unit) {
    val T = LocalT.current
    val goalK = d.goals.kcal.toDouble()
    val log = d.days[u.day].orEmpty()
    val tot = d.dayTotal(u.day)
    val kLeft = goalK - tot.kcal

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(dateTitle(u.day, u.today), 30f, 800, modifier = Modifier.weight(1f).padding(start = 4.dp), ls = -0.035f, lh = 1.1f, maxLines = 1)
            IconBtn("calendar_month", onPickDate, bg = T.surface, desc = "Go to date")
        }

        WeekStrip(d, u, vm)

        LedgerCard(if (kLeft >= -0.5) "Remaining" else "Over target", fmt(abs(kLeft)), pct(tot.kcal, goalK), goalK, macroViews(tot, d, T))

        Meal.entries.forEach { meal ->
            val list = log[meal].orEmpty()
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(T.surface)) {
                Row(
                    Modifier.padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Txt(meal.label, 17f, 750, modifier = Modifier.alignByBaseline(), ls = -0.01f)
                    if (list.isNotEmpty()) Txt("${fmt(d.total(list).kcal)} kcal", 13f, color = T.muted, modifier = Modifier.alignByBaseline(), tnum = true)
                    Box(Modifier.weight(1f))
                    IconBtn("add", { vm.openSearch(meal) }, size = 44, bg = T.surface2, desc = "Add to ${meal.label}")
                }
                list.forEach { e ->
                    val f = d.food(e.food)
                    val n = f.per(e.g.toDouble())
                    Divider()
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.openEdit(meal, e) }.padding(horizontal = 18.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Txt(f.name, 15f, 500, maxLines = 1, ellipsis = true)
                            Txt("${e.g} g · P ${fmt(n.p)} · C ${fmt(n.c)} · F ${fmt(n.fat)}", 12f, color = T.muted, tnum = true, maxLines = 1)
                        }
                        Txt(fmt(n.kcal), 15f, 650, tnum = true)
                    }
                }
            }
        }

        WaterCard(d, u, vm)

        Card(radius = 20, pad = PaddingValues(18.dp), gap = 14) {
            Txt("Nutrients", 17f, 750)
            listOf(
                listOf(Triple("Fibre", "${fmt(tot.fib)} g", "target 30 g"), Triple("Sugars", "${fmt(tot.sug)} g", "limit 50 g")),
                listOf(Triple("Saturated fat", "${fmt(tot.sat)} g", "limit 25 g"), Triple("Sodium", "${fmt(tot.na)} mg", "limit 2,300 mg")),
            ).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (name, v, sub) ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Label(name)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Txt(v, 16f, 700, modifier = Modifier.alignByBaseline(), tnum = true, maxLines = 1)
                                Txt(sub, 12f, color = T.muted, modifier = Modifier.alignByBaseline(), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(d: Data, u: Ui, vm: MacraViewModel) {
    val T = LocalT.current
    Row(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragStart = { acc = 0f },
                    onDragEnd = {
                        if (acc > 80f) vm.shiftWeek(-1) else if (acc < -80f) vm.shiftWeek(1)
                    },
                ) { _, dx -> acc += dx }
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (off in -3..3) {
            val day = u.anchor + off
            val sel = day == u.day
            val fut = day > u.today
            val has = d.dayEntries(day).isNotEmpty()
            val dt = date(day)
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (fut) 0.35f else 1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) T.ink else if (day == u.today) T.surface else Color.Transparent)
                    .clickable(enabled = !fut) { vm.selectDay(day) }
                    .padding(top = 8.dp, bottom = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val fg = if (sel) T.bg else T.ink
                Txt(dowLetter(dt), 11f, 600, fg, ls = 0.06f)
                Txt(dt.dayOfMonth.toString(), 17f, 750, fg, tnum = true)
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (has) T.acc else Color.Transparent))
            }
        }
    }
}

@Composable
private fun LedgerCard(kLabel: String, kBig: String, kPct: Float, goalK: Double, macros: List<MacroView>) {
    val T = LocalT.current
    Card(gap = 18) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(kLabel)
                BigNum(kBig, "kcal", 72f, 15f, ls = -0.055f)
            }
            Txt("of ${fmt(goalK)}", 13f, color = T.muted, tnum = true, lh = 1.6f)
        }
        Meter(kPct, T.acc, 10.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            macros.forEach { m ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Dot(m.color)
                        Label(m.name)
                    }
                    BigNum(m.main, m.unit, 24f, 12f, weight = 750, ls = -0.03f, gap = 4)
                    Meter(m.pct, m.color, 4.dp)
                }
            }
        }
    }
}

@Composable
private fun WaterCard(d: Data, u: Ui, vm: MacraViewModel) {
    val T = LocalT.current
    val ml = d.water[u.day] ?: 0
    val goal = d.goals.water
    val n = (goal / 250).coerceAtLeast(1)
    val filled = (ml / 250.0).roundToInt()
    Card(radius = 20, pad = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp), gap = 14) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sym("water_drop", 22, T.w)
            Txt("Water", 17f, 750, modifier = Modifier.weight(1f))
            Txt(litres(ml), 17f, 750, tnum = true)
            Txt("/ ${litres(goal)}", 13f, color = T.muted, tnum = true)
        }
        Segments(n, filled, T.w, T.surface2, 32.dp, 7.dp, 4.dp) { i ->
            vm.setWater(if (i + 1 == filled) i * 250 else (i + 1) * 250)
        }
    }
}
