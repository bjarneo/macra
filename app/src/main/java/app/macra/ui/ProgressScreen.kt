package app.macra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.macra.MacraViewModel
import app.macra.Ui
import app.macra.data.Data
import app.macra.data.WeightEntry
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun ProgressScreen(d: Data, u: Ui, vm: MacraViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(dateSub(u.today))
            Txt("Progress", 30f, 800, ls = -0.035f, lh = 1.1f)
        }
        WeightCard(d, u, vm)
        EnergyCard(d, u, vm)
        ProteinCard(d, u)
        AveragesCard(d, u)
    }
}

@Composable
private fun WeightCard(d: Data, u: Ui, vm: MacraViewModel) {
    val T = LocalT.current
    val n = u.range
    val start = u.today - (n - 1)
    val all = d.weights
    val ws = all.filter { it.day in start..u.today }
    val ma = ws.map { w -> all.filter { it.day in (w.day - 6)..w.day }.map { it.kg }.average() }
    val cur = all.lastOrNull()?.kg
    val goal = d.profile.goalKg
    val rangeName = mapOf(30 to "30 days", 90 to "3 months", 180 to "6 months")[n] ?: "$n days"
    Card {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("Weight")
                BigNum(cur?.let(::kg1) ?: "—", "kg", 56f, 15f)
            }
            Seg(listOf(30 to "1M", 90 to "3M", 180 to "6M"), n, vm::setRange, h = 30, padH = 11)
        }
        val change = when {
            ws.size >= 2 -> "${signed1(ws.last().kg - ws.first().kg)} kg over $rangeName · 7-day average ${kg1(ma.last())} kg"
            cur != null -> "Last logged ${dayMonth(date(all.last().day))}"
            else -> "No weight logged yet"
        }
        Txt(change, 13f, color = T.muted, tnum = true)
        if (ws.size >= 2) {
            WeightChart(ws, ma, start, n)
        } else {
            Box(
                Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(T.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Txt("Log your weight on 2 days\nin this range to see a trend.", 13f, color = T.muted, align = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(dayMonth(date(start)), dayMonth(date(start + n / 2)), "Today").forEach { Txt(it, 11f, color = T.muted) }
        }
        Column {
            Divider()
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(onClick = vm::openEditProfile),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Label(if (goal != null) "Goal ${kg1(goal)} kg" else "No goal set")
                    val toGo = when {
                        goal == null -> "Tap to set a goal weight"
                        cur == null -> "Log a weight to start"
                        abs(cur - goal) < 0.05 -> "Goal reached"
                        else -> "${kg1(abs(cur - goal))} kg to go"
                    }
                    Txt(toGo, 14f, 600, tnum = true)
                }
                AccentBtn("Log weight", "add", vm::openWeight)
            }
        }
    }
}

@Composable
private fun WeightChart(ws: List<WeightEntry>, ma: List<Double>, start: Long, n: Int) {
    val T = LocalT.current
    val tm = rememberTextMeasurer()
    val vals = ws.map { it.kg } + ma
    val lo = floor((vals.min() - 0.2) * 2) / 2
    var hi = ceil((vals.max() + 0.2) * 2) / 2
    if (hi - lo < 0.5) hi = lo + 0.5
    val labelStyle = textStyle(11f, 400, T.muted, tnum = true)
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        val pt = 14.dp.toPx()
        val pb = 8.dp.toPx()
        fun x(day: Long) = (day - start).toFloat() / (n - 1).coerceAtLeast(1) * w
        fun y(v: Double) = pt + ((hi - v) / (hi - lo)).toFloat() * (h - pt - pb)
        val ticks = listOf(hi, (hi + lo) / 2, lo)
        ticks.forEach { v -> drawLine(T.line, Offset(0f, y(v)), Offset(w, y(v)), 1.dp.toPx()) }
        val raw = Path()
        ws.forEachIndexed { i, e -> if (i == 0) raw.moveTo(x(e.day), y(e.kg)) else raw.lineTo(x(e.day), y(e.kg)) }
        drawPath(raw, T.muted.copy(alpha = 0.55f), style = Stroke(1.25.dp.toPx(), join = StrokeJoin.Round))
        val trend = Path()
        ws.forEachIndexed { i, e -> if (i == 0) trend.moveTo(x(e.day), y(ma[i])) else trend.lineTo(x(e.day), y(ma[i])) }
        drawPath(trend, T.acc, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        ticks.forEach { v ->
            val layout = tm.measure(kg1(v), labelStyle)
            drawText(layout, topLeft = Offset(0f, y(v) - layout.size.height * 1.15f))
        }
        val last = ws.last()
        val c = Offset(x(last.day), y(last.kg))
        drawCircle(T.surface, 9.dp.toPx(), c)
        drawCircle(T.acc, 6.dp.toPx(), c)
    }
}

@Composable
private fun EnergyCard(d: Data, u: Ui, vm: MacraViewModel) {
    val T = LocalT.current
    val goalK = d.goals.kcal.toDouble()
    val days7 = (u.today - 6..u.today).map { it to d.dayTotal(it) }
    val maxK = maxOf(goalK * 1.15, days7.maxOf { it.second.kcal })
    val logged = days7.take(6).filter { it.second.kcal > 0 }
    val avg = if (logged.isEmpty()) "—" else fmt(logged.sumOf { it.second.kcal } / logged.size)
    val sel = days7[u.barSel.coerceIn(0, 6)]
    Card {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("Energy · last 7 days")
                BigNum(avg, "kcal/day avg", 40f, 13f, ls = -0.045f)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Txt("${fmt(sel.second.kcal)} kcal", 16f, 750, tnum = true)
                Txt(if (sel.first == u.today) "Today, so far" else "${dow(date(sel.first))} ${dayMonth(date(sel.first))}", 12f, color = T.muted)
            }
        }
        Box(Modifier.fillMaxWidth().height(140.dp)) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                days7.forEachIndexed { i, (_, t) ->
                    val on = i == u.barSel
                    val h by animateFloatAsState((t.kcal / maxK).toFloat().coerceIn(0f, 1f), tween(300), label = "bar$i")
                    val c by animateColorAsState(if (on) T.acc else T.bar, tween(150), label = "barc$i")
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = null, indication = null) { vm.selectBar(i) },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (h > 0f) {
                            Box(
                                Modifier.fillMaxWidth().fillMaxHeight(h)
                                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                    .background(c),
                            )
                        }
                    }
                }
            }
            val goalFrac = (goalK / maxK).toFloat()
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1 - goalFrac)
                drawLine(
                    T.muted.copy(alpha = 0.8f), Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                )
            }
        }
        Row(Modifier.fillMaxWidth().offset(y = (-6).dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days7.forEachIndexed { i, (day, _) ->
                val on = i == u.barSel
                Txt(dowLetter(date(day)), 12f, if (on) 800 else 500, if (on) T.ink else T.muted, Modifier.weight(1f), align = TextAlign.Center)
            }
        }
        Txt("Dashed line: ${fmt(goalK)} kcal target. Average excludes today and days with no entries.", 12f, color = T.muted)
    }
}

@Composable
private fun ProteinCard(d: Data, u: Ui) {
    val T = LocalT.current
    val g = d.goals
    val days7 = (u.today - 6..u.today).map { it to d.dayTotal(it) }
    val hits = days7.count { it.second.p >= g.p * 0.9 }
    Card(gap = 14) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label("Protein target hit", Modifier.alignByBaseline())
            Txt("≥ 90% of ${g.p} g", 12f, color = T.muted, modifier = Modifier.alignByBaseline())
        }
        BigNum(hits.toString(), "of 7 days", 40f, 14f, ls = -0.045f)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days7.forEach { (day, t) ->
                val hit = t.p >= g.p * 0.9
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(if (hit) T.p else T.surface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Txt(fmt(t.p), 12f, 750, if (hit) (if (T.dark) Color(0xFF0C0C0D) else Color.White) else T.ink, tnum = true, maxLines = 1)
                    }
                    Txt(dowLetter(date(day)), 11f, color = T.muted)
                }
            }
        }
    }
}

@Composable
private fun AveragesCard(d: Data, u: Ui) {
    val T = LocalT.current
    val g = d.goals
    val full = (u.today - 6 until u.today).map { d.dayTotal(it) }.filter { it.kcal > 0 }
    Card(gap = 14) {
        Label("Macro averages · last 6 full days")
        listOf(
            Triple("Protein", T.p, g.p) to { t: app.macra.data.Nutr -> t.p },
            Triple("Carbs", T.c, g.c) to { t: app.macra.data.Nutr -> t.c },
            Triple("Fat", T.f, g.fat) to { t: app.macra.data.Nutr -> t.fat },
        ).forEach { (m, get) ->
            val (name, color, goal) = m
            val a = if (full.isEmpty()) 0.0 else full.sumOf(get) / full.size
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dot(color)
                    Txt(name, 15f, 600, modifier = Modifier.weight(1f))
                    Txt(if (full.isEmpty()) "—" else "${fmt(a)} g", 15f, 750, tnum = true)
                    Txt("/ $goal g", 12f, color = T.muted, tnum = true)
                }
                Meter(pct(a, goal.toDouble()), color, 4.dp)
            }
        }
        if (full.isEmpty()) Txt("Averages use days with entries. Log food to fill them in.", 12f, color = T.muted)
    }
}
