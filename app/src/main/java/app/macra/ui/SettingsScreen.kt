package app.macra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.macra.MacraViewModel
import app.macra.data.Data

@Composable
fun SettingsScreen(d: Data, vm: MacraViewModel, onToggle: (String) -> Unit, onExport: () -> Unit) {
    val T = LocalT.current
    val ctx = LocalContext.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() }
    val s = d.settings
    val g = d.goals
    val goalKg = d.profile.goalKg
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Txt("Settings", 30f, 800, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp), ls = -0.035f, lh = 1.1f)

        Label("Daily targets", Modifier.padding(start = 4.dp, top = 4.dp))
        Group {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt("Energy", 16f, 650, modifier = Modifier.weight(1f))
                Txt("${fmt(g.kcal.toDouble())} kcal", 18f, 800, ls = -0.02f, tnum = true)
            }
            GoalRow("Protein", T.p, "${g.p} g", { vm.stepGoal("p", -5, 40, 400) }, { vm.stepGoal("p", 5, 40, 400) })
            GoalRow("Carbs", T.c, "${g.c} g", { vm.stepGoal("c", -5, 0, 700) }, { vm.stepGoal("c", 5, 0, 700) })
            GoalRow("Fat", T.f, "${g.fat} g", { vm.stepGoal("fat", -5, 20, 250) }, { vm.stepGoal("fat", 5, 20, 250) })
            GoalRow("Water", T.w, litres(g.water), { vm.stepGoal("water", -250, 1000, 6000) }, { vm.stepGoal("water", 250, 1000, 6000) })
            GoalRow("Goal weight", null, goalKg?.let { "${kg1(it)} kg" } ?: "Not set", { vm.stepGoalWeight(-0.5) }, { vm.stepGoalWeight(0.5) })
        }

        Label("Display", Modifier.padding(start = 4.dp, top = 12.dp))
        Group {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt("Theme", 16f, 650, modifier = Modifier.weight(1f))
                Seg(listOf("Dark" to "Dark", "Light" to "Light"), s.theme, { v -> vm.setSettings { it.copy(theme = v) } })
            }
        }

        Label("Reminders", Modifier.padding(start = 4.dp, top = 12.dp))
        Group {
            listOf(
                Triple("meals", "Meals", "08:00, 12:30, and 19:00") to s.remMeals,
                Triple("water", "Water", "Every 2 hours, 08:00 to 20:00") to s.remWater,
                Triple("weigh", "Weigh-in", "07:30") to s.remWeigh,
            ).forEachIndexed { i, (t, on) ->
                if (i > 0) Divider()
                ToggleRow(t.second, t.third, on) { onToggle(t.first) }
            }
        }

        Label("Data", Modifier.padding(start = 4.dp, top = 12.dp))
        Group {
            ToggleRow("Sync weight with Health Connect", null, s.healthConnect) { onToggle("health") }
            Divider()
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onExport).padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("Export CSV", 16f, 650, modifier = Modifier.weight(1f))
                Sym("download", 24, T.muted)
            }
        }
        if (version != null) {
            Txt("Macra $version", 12f, color = T.muted, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), align = TextAlign.Center)
        }
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(LocalT.current.surface)) { content() }
}

@Composable
private fun ToggleRow(label: String, sub: String?, on: Boolean, onClick: () -> Unit) {
    val T = LocalT.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(label, 16f, 650)
            if (sub != null) Txt(sub, 12f, color = T.muted)
        }
        Toggle(on)
    }
}

@Composable
private fun GoalRow(label: String, color: Color?, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Divider()
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (color != null) Dot(color)
            Txt(label, 16f, 650, maxLines = 1)
        }
        StepBtn("remove", onMinus, desc = "Lower $label")
        Txt(value, 16f, 750, modifier = Modifier.widthIn(min = 72.dp), tnum = true, align = TextAlign.Center, maxLines = 1)
        StepBtn("add", onPlus, desc = "Raise $label")
    }
}
