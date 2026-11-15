package app.macra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.macra.MacraViewModel
import app.macra.Ui
import app.macra.data.Data
import app.macra.data.FOODS
import app.macra.data.Food
import app.macra.data.Meal
import app.macra.data.per
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(d: Data, u: Ui, vm: MacraViewModel) {
    val T = LocalT.current
    val focus = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        delay(240)
        runCatching { focus.requestFocus() }
        kb?.show()
    }
    val q = u.query.trim().lowercase()
    val recent = remember(d.days, u.today) { vm.recent(d, u.today) }
    val local: List<Food>
    val label: String
    if (q.isEmpty()) {
        local = recent.ifEmpty { FOODS }
        label = if (recent.isEmpty()) "Common foods" else "Recent"
    } else {
        val words = q.split(Regex("\\s+"))
        local = (FOODS + d.foods).filter { f -> words.all { f.name.lowercase().contains(it) } }
        label = "${local.size} result${if (local.size == 1) "" else "s"}"
    }
    val showOnline = q.length >= 3 && u.online.query == u.query.trim()
    val localIds = local.map { it.id }.toSet()
    val online = if (showOnline) u.online.results.filter { it.id !in localIds } else emptyList()
    val noResults = q.isNotEmpty() && local.isEmpty() && online.isEmpty() && !(showOnline && u.online.loading) && !(showOnline && u.online.failed)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconBtn("arrow_back", vm::goBack, desc = "Back")
            Row(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(T.surface).padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("search", 22, T.muted)
                BasicTextField(
                    value = u.query,
                    onValueChange = vm::setQuery,
                    singleLine = true,
                    textStyle = textStyle(16f, 400, T.ink),
                    cursorBrush = SolidColor(T.ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.submitQuery(); kb?.hide() }),
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (u.query.isEmpty()) Txt("Search foods", 16f, color = T.muted)
                            inner()
                        }
                    },
                )
                if (u.query.isNotEmpty()) IconBtn("close", { vm.setQuery(""); runCatching { focus.requestFocus() } }, size = 40, fg = T.muted, iconSize = 20, desc = "Clear")
            }
            IconBtn("barcode_scanner", vm::openScan, size = 52, bg = T.surface, desc = "Scan barcode")
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Meal.entries.forEach { m -> Chip(m.label, u.searchMeal == m, { vm.setSearchMeal(m) }) }
        }
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Label(label)
            Label("kcal")
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(local, key = { "l:" + it.id }) { f -> ResultRow(f, null, vm) }
            if (showOnline) {
                item(key = "online") {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Label("Open Food Facts", Modifier.weight(1f))
                        if (u.online.loading) CircularProgressIndicator(Modifier.size(14.dp), color = T.muted, strokeWidth = 2.dp)
                    }
                }
                if (u.online.failed) {
                    item(key = "failed") {
                        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Txt("Open Food Facts did not respond.", 14f, color = T.muted, modifier = Modifier.weight(1f))
                            OutlineBtn("Retry", null, vm::submitQuery, height = 36)
                        }
                    }
                } else if (!u.online.loading && online.isEmpty() && local.isNotEmpty()) {
                    item(key = "none") { Txt("No other matches online.", 14f, color = T.muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) }
                }
                items(online, key = { "o:" + it.id }) { f -> ResultRow(f, f.src.removePrefix("Open Food Facts").removePrefix(" · ").ifBlank { null }, vm) }
            }
            if (noResults) {
                item(key = "empty") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Txt("No match for “${u.query.trim()}”", 16f, 650, align = TextAlign.Center)
                        Txt(
                            if (q.length < 3) "Type 3 letters or more to search Open Food Facts."
                            else "Try a shorter term, or scan the barcode.",
                            14f, color = T.muted, align = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(f: Food, brand: String?, vm: MacraViewModel) {
    val T = LocalT.current
    val n = f.per(f.def.toDouble())
    Row(
        Modifier.fillMaxWidth().clickable { vm.openFood(f) }.padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(f.name, 16f, 600, maxLines = 1, ellipsis = true)
            val macros = "${f.def} g · P ${fmt(n.p)} · C ${fmt(n.c)} · F ${fmt(n.fat)}"
            Txt(if (brand != null) "$brand · $macros" else macros, 13f, color = T.muted, tnum = true, maxLines = 1, ellipsis = true)
        }
        Txt(fmt(n.kcal), 16f, 750, tnum = true)
        IconBtn("add", { vm.quickAdd(f) }, size = 40, bg = T.surface, iconSize = 22, desc = "Add ${f.name}")
    }
}
