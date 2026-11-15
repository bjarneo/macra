package app.macra.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

fun macroViews(tot: Nutr, d: Data, T: Tokens, remaining: Boolean): List<MacroView> {
    val g = d.goals
    return listOf(Triple("Protein", tot.p, g.p) to T.p, Triple("Carbs", tot.c, g.c) to T.c, Triple("Fat", tot.fat, g.fat) to T.f).map { (t, color) ->
        val (name, eaten, goal) = t
        val left = goal - eaten
        MacroView(
            name, color, pct(eaten, goal.toDouble()),
            if (remaining) fmt(abs(left)) else fmt(eaten),
            if (remaining) (if (left >= -0.5) "g left" else "g over") else "/ $goal g",
        )
    }
}

fun pct(v: Double, of: Double): Float = if (of <= 0) 0f else (v / of * 100).toFloat().coerceIn(0f, 100f)

@Composable
fun DiaryScreen(d: Data, u: Ui, vm: MacraViewModel, onPickDate: () -> Unit) {
    val T = LocalT.current
    val s = d.settings
    val goalK = d.goals.kcal.toDouble()
    val log = d.days[u.day].orEmpty()
    val tot = d.dayTotal(u.day)
    val rem = s.count == "Remaining"
    val kLeft = goalK - tot.kcal
    val kPct = pct(tot.kcal, goalK)
    val kBig = if (rem) fmt(abs(kLeft)) else fmt(tot.kcal)
    val kLabel = if (rem) (if (kLeft >= -0.5) "Remaining" else "Over target") else "Eaten"
    val macros = macroViews(tot, d, T, rem)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 176.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onPickDate).padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Label(dateSub(u.day))
                Txt(dateTitle(u.day, u.today), 30f, 800, ls = -0.035f, lh = 1.1f, maxLines = 1)
            }
            IconBtn("insights", vm::openNutrients, bg = T.surface, desc = "Nutrition details")
        }

        WeekStrip(d, u, vm)

        when (s.dash) {
            "Rings" -> RingsCard(kPct, kBig, rem, kLeft, goalK, macros)
            "Blocks" -> BlocksCard(kLabel, kBig, kPct, tot, goalK, macros)
            else -> LedgerCard(kLabel, kBig, kPct, tot, goalK, macros)
        }

        Meal.entries.forEach { meal ->
            val list = log[meal].orEmpty()
            val t = d.total(list)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(T.surface)) {
                Row(
                    Modifier.padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Txt(meal.label, 17f, 750, modifier = Modifier.alignByBaseline(), ls = -0.01f)
                            Txt("${fmt(t.kcal)} kcal", 13f, color = T.muted, modifier = Modifier.alignByBaseline(), tnum = true)
                        }
                        if (list.isNotEmpty()) Txt("P ${fmt(t.p)} · C ${fmt(t.c)} · F ${fmt(t.fat)} g", 12f, color = T.muted, tnum = true)
                    }
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

        Card(radius = 20, pad = PaddingValues(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 16.dp), onClick = vm::openNutrients) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Txt("Nutrients", 17f, 750)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(
                            Triple("Fibre", "${fmt(tot.fib)} g", "target 30 g"),
                            Triple("Sugars", "${fmt(tot.sug)} g", "limit 50 g"),
                            Triple("Sodium", "${fmt(tot.na)} mg", "limit 2,300 mg"),
                        ).forEach { (name, v, sub) ->
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Label(name)
                                Txt(v, 16f, 700, tnum = true, maxLines = 1)
                                Txt(sub, 11f, color = T.muted, maxLines = 1)
                            }
                        }
                    }
                }
                Sym("chevron_right", 24, T.muted)
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
private fun LedgerCard(kLabel: String, kBig: String, kPct: Float, tot: Nutr, goalK: Double, macros: List<MacroView>) {
    val T = LocalT.current
    Card(gap = 18) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(kLabel)
                BigNum(kBig, "kcal", 72f, 15f, ls = -0.055f)
            }
            Column(horizontalAlignment = Alignment.End) {
                Txt("Target ${fmt(goalK)} kcal", 13f, color = T.muted, tnum = true, lh = 1.6f)
                Txt("Eaten ${fmt(tot.kcal)}", 13f, color = T.muted, tnum = true, lh = 1.6f)
            }
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
private fun RingsCard(kPct: Float, kBig: String, rem: Boolean, kLeft: Double, goalK: Double, macros: List<MacroView>) {
    val T = LocalT.current
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Rings(listOf(kPct to T.acc) + macros.map { it.pct to it.color }, T.surface2)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val energy = MacroView("Energy", T.acc, kPct, kBig, if (rem) (if (kLeft >= -0.5) "kcal left" else "kcal over") else "/ ${fmt(goalK)} kcal")
                (listOf(energy) + macros).forEach { m ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Dot(m.color)
                            Label(m.name)
                        }
                        BigNum(m.main, m.unit, 19f, 12f, weight = 750, ls = -0.02f, unitWeight = 500, gap = 4)
                    }
                }
            }
        }
    }
}

@Composable
private fun Rings(rings: List<Pair<Float, Color>>, track: Color) {
    val anim = rings.mapIndexed { i, (p, _) -> animateFloatAsState(p, tween(400), label = "ring$i") }
    Canvas(Modifier.size(168.dp)) {
        val scale = size.width / 168f
        val sw = 11f * scale
        listOf(76f, 62f, 48f, 34f).forEachIndexed { i, r0 ->
            if (i >= rings.size) return@forEachIndexed
            val r = r0 * scale
            val tl = Offset(center.x - r, center.y - r)
            val sz = Size(r * 2, r * 2)
            drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(sw))
            val sweep = anim[i].value / 100f * 360f
            if (sweep > 0.5f) drawArc(rings[i].second, -90f, sweep, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun BlocksCard(kLabel: String, kBig: String, kPct: Float, tot: Nutr, goalK: Double, macros: List<MacroView>) {
    val T = LocalT.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(T.acc).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Label(kLabel, color = T.onAcc)
                Label("${(tot.kcal / goalK * 100).roundToInt()}%", color = T.onAcc)
            }
            BigNum(kBig, "kcal", 96f, 18f, ls = -0.06f, unitWeight = 700, gap = 8, color = T.onAcc, unitColor = T.onAcc)
            Txt("${fmt(tot.kcal)} eaten of ${fmt(goalK)} kcal", 13f, 500, T.onAcc, tnum = true)
            Segments(20, (kPct / 100f * 20).roundToInt(), T.onAcc, T.onAcc.copy(alpha = 0.18f), 14.dp, 3.dp, 3.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            macros.forEach { m ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(T.surface).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Dot(m.color)
                        Label(m.name)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Txt(m.main, 32f, 800, ls = -0.04f, lh = 1f, tnum = true, maxLines = 1)
                        Txt(m.unit, 12f, 500, T.muted, maxLines = 1)
                    }
                    Segments(10, (m.pct / 100f * 10).roundToInt(), m.color, T.surface2, 16.dp, 2.dp, 2.dp)
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineBtn("250 ml", "remove", { vm.setWater(ml - 250) })
            OutlineBtn("250 ml", "add", { vm.setWater(ml + 250) })
            OutlineBtn("500 ml", "add", { vm.setWater(ml + 500) })
        }
    }
}
