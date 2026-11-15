@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.macra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.macra.FoodDraft
import app.macra.MacraViewModel
import app.macra.Ui
import app.macra.data.Data
import app.macra.data.Meal
import app.macra.data.Nutr
import app.macra.data.per
import kotlin.math.roundToInt

@Composable
fun FoodScreen(fd: FoodDraft, d: Data, u: Ui, vm: MacraViewModel) {
    val T = LocalT.current
    val kb = LocalSoftwareKeyboardController.current
    val f = fd.food
    val gN = fd.g.toIntOrNull() ?: 0
    val n = f.per(gN.toDouble())
    val g = d.goals
    val goalK = g.kcal.toDouble()
    val tot = d.dayTotal(u.day)
    val old = if (fd.edit) f.per(fd.origG.toDouble()) else Nutr()
    val after = tot - old + n
    val kP = n.p * 4
    val kC = n.c * 4
    val kF = n.fat * 9
    val kS = (kP + kC + kF).takeIf { it > 0 } ?: 1.0

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconBtn("close", vm::goBack, desc = "Close")
            Txt(if (fd.edit) "Edit entry" else "Add food", 16f, 650, modifier = Modifier.weight(1f))
            if (fd.edit) IconBtn("delete", vm::removeFd, desc = "Remove entry")
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Label(f.src)
                Txt(f.name, 28f, 800, ls = -0.035f, lh = 1.1f)
                Txt("Per 100 g · ${r1(f.kcal)} kcal · P ${r1(f.p)} · C ${r1(f.c)} · F ${r1(f.fat)}", 13f, color = T.muted, tnum = true)
            }
            Card(pad = PaddingValues(18.dp)) {
                Label("Amount")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StepBtn("remove", { vm.stepFd(-5) }, size = 56, radius = 18, iconSize = 26, desc = "Less")
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                            BasicTextField(
                                value = fd.g,
                                onValueChange = vm::setFdG,
                                singleLine = true,
                                textStyle = textStyle(48f, 800, T.ink, ls = -0.045f, tnum = true, align = TextAlign.End),
                                cursorBrush = SolidColor(T.ink),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { kb?.hide() }),
                                modifier = Modifier.width(130.dp).alignByBaseline(),
                            )
                            Txt("g", 20f, 700, T.muted, Modifier.alignByBaseline())
                        }
                        Box(Modifier.fillMaxWidth().height(2.dp).background(T.ink))
                    }
                    StepBtn("add", { vm.stepFd(5) }, size = 56, radius = 18, iconSize = 26, desc = "More")
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf("100 g" to 100) + f.serv.map { "${it.label} · ${it.g} g" to it.g }).distinctBy { it.second }.forEach { (label, grams) ->
                        Chip(label, gN == grams, { vm.setFdG(grams.toString()) }, 600)
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Meal.entries.forEach { m -> Chip(m.label, fd.meal == m, { vm.setFdMeal(m) }) }
            }
            Card {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Label("Energy")
                        BigNum(fmt(n.kcal), "kcal", 56f, 15f)
                    }
                    Txt("${(n.kcal / goalK * 100).roundToInt()}% of daily target", 13f, color = T.muted, align = TextAlign.End)
                }
                Split(listOf(kP.toFloat() to T.p, kC.toFloat() to T.c, kF.toFloat() to T.f), 12.dp, 4.dp, track = T.surface2)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf(Triple("Protein", n.p, kP) to T.p, Triple("Carbs", n.c, kC) to T.c, Triple("Fat", n.fat, kF) to T.f).forEach { (t, color) ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Dot(color)
                                Label(t.first)
                            }
                            BigNum(r1(t.second), "g", 24f, 13f, weight = 750, ls = -0.03f, unitWeight = 400, gap = 3)
                            Txt("${(t.third / kS * 100).roundToInt()}% of kcal", 12f, color = T.muted)
                        }
                    }
                }
            }
            Card(pad = PaddingValues(horizontal = 20.dp, vertical = 18.dp), gap = 12) {
                Label("Remaining after this entry")
                listOf(
                    Triple("Energy", goalK - tot.kcal to goalK - after.kcal, "kcal") to T.acc,
                    Triple("Protein", g.p - tot.p to g.p - after.p, "g") to T.p,
                    Triple("Carbs", g.c - tot.c to g.c - after.c, "g") to T.c,
                    Triple("Fat", g.fat - tot.fat to g.fat - after.fat, "g") to T.f,
                ).forEach { (t, color) ->
                    val (before, aft) = t.second
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Dot(color)
                        Txt(t.first, 15f, modifier = Modifier.weight(1f))
                        Txt(sfmt(before), 15f, color = T.muted, tnum = true)
                        Sym("arrow_forward", 16, T.muted)
                        Txt("${sfmt(aft)} ${t.third}", 15f, 750, if (aft < -0.5) T.acc else T.ink, Modifier.widthIn(min = 84.dp), tnum = true, align = TextAlign.End)
                    }
                }
            }
            Card(pad = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp), gap = 0) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Label("Nutrition facts")
                    Txt("for ${fmt(gN.toDouble())} g", 12f, color = T.muted)
                }
                listOf(
                    Triple("Energy", "${fmt(n.kcal)} kcal", true),
                    Triple("Protein", "${r1(n.p)} g", true),
                    Triple("Carbohydrates", "${r1(n.c)} g", true),
                    Triple("of which sugars", "${r1(n.sug)} g", false),
                    Triple("Fibre", "${r1(n.fib)} g", true),
                    Triple("Fat", "${r1(n.fat)} g", true),
                    Triple("of which saturates", "${r1(n.sat)} g", false),
                    Triple("Sodium", "${fmt(n.na)} mg", true),
                ).forEach { (label, v, main) ->
                    Divider()
                    Row(Modifier.fillMaxWidth().padding(start = if (main) 0.dp else 16.dp, top = 10.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        val c = if (main) T.ink else T.muted
                        Txt(label, 14f, if (main) 500 else 400, c)
                        Txt(v, 14f, if (main) 500 else 400, c, tnum = true)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(T.bg)) {
            Divider()
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .alpha(if (gN > 0) 1f else 0.4f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(T.acc)
                    .clickable(enabled = gN > 0, onClick = vm::saveFd)
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Txt(if (fd.edit) "Save changes" else "Add to ${fd.meal.label}", 16f, 750, T.onAcc)
                Txt("${fmt(n.kcal)} kcal", 16f, 750, T.onAcc, tnum = true)
            }
        }
    }
}
