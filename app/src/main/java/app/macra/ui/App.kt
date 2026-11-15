@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.macra.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.macra.FoodDraft
import app.macra.MacraViewModel
import app.macra.Screen
import app.macra.Tab
import app.macra.health.Health
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private fun scheme(T: Tokens) = if (T.dark) {
    darkColorScheme(
        primary = T.acc, onPrimary = T.onAcc, primaryContainer = T.surface2, onPrimaryContainer = T.ink,
        secondaryContainer = T.surface2, onSecondaryContainer = T.ink, background = T.bg, onBackground = T.ink,
        surface = T.surface, onSurface = T.ink, onSurfaceVariant = T.muted, surfaceContainerHigh = T.surface,
        surfaceContainerHighest = T.surface2, outline = T.bar, outlineVariant = T.line,
    )
} else {
    lightColorScheme(
        primary = T.acc, onPrimary = T.onAcc, primaryContainer = T.surface2, onPrimaryContainer = T.ink,
        secondaryContainer = T.surface2, onSecondaryContainer = T.ink, background = T.bg, onBackground = T.ink,
        surface = T.surface, onSurface = T.ink, onSurfaceVariant = T.muted, surfaceContainerHigh = T.surface,
        surfaceContainerHighest = T.surface2, outline = T.bar, outlineVariant = T.line,
    )
}

@Composable
fun MacraApp(vm: MacraViewModel) {
    val d by vm.data.collectAsStateWithLifecycle()
    val u by vm.ui.collectAsStateWithLifecycle()
    val T = remember(d.settings.theme, d.settings.accent) { tokens(d.settings.theme != "Light", d.settings.accent) }
    val ctx = LocalContext.current
    val activity = ctx as Activity
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val scanOpen = u.top == Screen.Scan

    SideEffect {
        val c = WindowCompat.getInsetsController(activity.window, view)
        c.isAppearanceLightStatusBars = !T.dark && !scanOpen
        c.isAppearanceLightNavigationBars = !T.dark && !scanOpen
    }

    // Camera permission for the barcode scanner.
    fun camOk() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var camGranted by remember { mutableStateOf(camOk()) }
    var camAsked by remember { mutableStateOf(false) }
    val camLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        camGranted = it
        camAsked = true
    }
    LifecycleResumeEffect(Unit) {
        camGranted = camOk()
        onPauseOrDispose { }
    }
    LaunchedEffect(scanOpen) {
        if (scanOpen && !camGranted && !camAsked) camLauncher.launch(Manifest.permission.CAMERA)
    }
    val allowCamera = {
        if (camAsked && !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            ctx.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
        } else {
            camLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Notifications for reminders.
    var pendingReminder by remember { mutableStateOf<String?>(null) }
    fun setReminder(kind: String, on: Boolean) = vm.setSettings {
        when (kind) {
            "meals" -> it.copy(remMeals = on)
            "water" -> it.copy(remWater = on)
            else -> it.copy(remWeigh = on)
        }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val kind = pendingReminder
        pendingReminder = null
        if (ok && kind != null) setReminder(kind, true) else vm.snack("Allow notifications to get reminders")
    }

    // Health Connect.
    val healthLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { vm.onHealthResult(it) }

    val onToggle: (String) -> Unit = { kind ->
        val s = d.settings
        when (kind) {
            "health" -> if (s.healthConnect) {
                vm.setSettings { it.copy(healthConnect = false) }
            } else {
                when (Health.status(ctx)) {
                    HealthConnectClient.SDK_AVAILABLE -> scope.launch {
                        if (Health.granted(ctx)) vm.onHealthResult(Health.PERMISSIONS) else healthLauncher.launch(Health.PERMISSIONS)
                    }
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                        vm.snack("Update Health Connect to sync weight")
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"))
                                    .setPackage("com.android.vending"),
                            )
                        }
                    }
                    else -> vm.snack("Health Connect is not available on this phone")
                }
            }
            else -> {
                val on = when (kind) {
                    "meals" -> s.remMeals
                    "water" -> s.remWater
                    else -> s.remWeigh
                }
                if (!on && Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    pendingReminder = kind
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    setReminder(kind, !on)
                }
            }
        }
    }

    // CSV export.
    var exportName by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.export(uri, exportName)
    }
    val onExport = {
        exportName = "macra-diary-${LocalDate.now()}.csv"
        exportLauncher.launch(exportName)
    }

    var pickDate by remember { mutableStateOf(false) }

    BackHandler(enabled = u.stack.isNotEmpty()) { vm.goBack() }
    BackHandler(enabled = u.stack.isEmpty() && u.tab != Tab.Diary) { vm.setTab(Tab.Diary) }

    MaterialTheme(colorScheme = scheme(T)) {
        CompositionLocalProvider(LocalT provides T, LocalContentColor provides T.ink) {
            Box(Modifier.fillMaxSize().background(T.bg)) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
                        when (u.tab) {
                            Tab.Diary -> DiaryScreen(d, u, vm) { pickDate = true }
                            Tab.Progress -> ProgressScreen(d, u, vm)
                            Tab.Profile -> ProfileScreen(d, u, vm, onToggle, onExport)
                        }
                        if (u.tab == Tab.Diary) Fabs(vm, Modifier.align(Alignment.BottomEnd))
                    }
                    TabBar(u.tab, vm::setTab)
                }

                Overlay(Screen.Search in u.stack, Slide.Right) {
                    Box(Modifier.fillMaxSize().background(T.bg).safeDrawingPadding()) { SearchScreen(d, u, vm) }
                }
                Overlay(Screen.Nutr in u.stack, Slide.Right) {
                    Box(Modifier.fillMaxSize().background(T.bg).safeDrawingPadding()) { NutrientsScreen(d, u, vm) }
                }
                Overlay(Screen.Scan in u.stack, Slide.Fade) {
                    ScanScreen(u, vm, camGranted, allowCamera)
                }
                Overlay(Screen.Food in u.stack && u.fd != null, Slide.Up) {
                    var last by remember { mutableStateOf<FoodDraft?>(null) }
                    if (u.fd != null) last = u.fd
                    last?.let { Box(Modifier.fillMaxSize().background(T.bg).safeDrawingPadding()) { FoodScreen(it, d, u, vm) } }
                }
                Overlay(u.top == Screen.Weight, Slide.Fade) {
                    WeightSheet(d, u, vm)
                }
                Overlay(u.top == Screen.EditProfile, Slide.Fade) {
                    EditProfileSheet(d.profile, d.weights.lastOrNull()?.kg, vm::goBack, vm::saveProfile)
                }

                SnackHost(u.snack, vm)
            }

            if (pickDate) {
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = u.day * 86_400_000L,
                    selectableDates = object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / 86_400_000L <= u.today
                        override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
                    },
                )
                DatePickerDialog(
                    onDismissRequest = { pickDate = false },
                    confirmButton = {
                        TextButton({
                            state.selectedDateMillis?.let { vm.jumpTo(it / 86_400_000L) }
                            pickDate = false
                        }) { Txt("Go", 15f, 700, T.ink) }
                    },
                    dismissButton = {
                        TextButton({
                            vm.jumpTo(u.today)
                            pickDate = false
                        }) { Txt("Today", 15f, 700, T.muted) }
                    },
                    colors = DatePickerDefaults.colors(containerColor = T.surface),
                ) {
                    DatePicker(
                        state,
                        title = { Label("Go to day", Modifier.padding(start = 24.dp, top = 20.dp)) },
                        showModeToggle = false,
                        colors = DatePickerDefaults.colors(
                            containerColor = T.surface,
                            selectedDayContainerColor = T.ink,
                            selectedDayContentColor = T.bg,
                            todayDateBorderColor = T.acc,
                            todayContentColor = T.ink,
                            dayContentColor = T.ink,
                            weekdayContentColor = T.muted,
                            disabledDayContentColor = T.muted.copy(alpha = 0.4f),
                            headlineContentColor = T.ink,
                            navigationContentColor = T.ink,
                            subheadContentColor = T.muted,
                            yearContentColor = T.ink,
                            selectedYearContainerColor = T.ink,
                            selectedYearContentColor = T.bg,
                        ),
                    )
                }
            }
        }
    }
}

private enum class Slide { Right, Up, Fade }

@Composable
private fun BoxScope.Overlay(visible: Boolean, kind: Slide, content: @Composable () -> Unit) {
    val px28 = with(LocalDensity.current) { 28.dp.roundToPx() }
    val px48 = with(LocalDensity.current) { 48.dp.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.matchParentSize(),
        enter = when (kind) {
            Slide.Right -> slideInHorizontally(tween(220, easing = EASE)) { px28 } + fadeIn(tween(220, easing = EASE))
            Slide.Up -> slideInVertically(tween(260, easing = EASE)) { px48 } + fadeIn(tween(260, easing = EASE))
            Slide.Fade -> fadeIn(tween(200))
        },
        exit = fadeOut(tween(140)),
    ) {
        // Block touches so that they do not reach the screen below.
        Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
            content()
        }
    }
}

@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val T = LocalT.current
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(T.surface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom))
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Handle()
            content()
        }
    }
}

@Composable
private fun WeightSheet(d: app.macra.data.Data, u: app.macra.Ui, vm: MacraViewModel) {
    val T = LocalT.current
    val now = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
    val prev = d.weights.lastOrNull { it.day < u.today }
    Sheet(vm::goBack) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label("Log weight")
            Txt("Today, ${dayMonth(date(u.today))} · $now", 15f, color = T.muted)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            StepBtn("remove", { vm.stepWeight(-0.1) }, size = 56, radius = 18, iconSize = 26, desc = "Lower")
            Row(Modifier.widthIn(min = 170.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                Txt(kg1(u.wDraft), 64f, 800, modifier = Modifier.alignByBaseline(), ls = -0.05f, tnum = true, maxLines = 1)
                Txt("kg", 20f, 700, T.muted, Modifier.alignByBaseline())
            }
            StepBtn("add", { vm.stepWeight(0.1) }, size = 56, radius = 18, iconSize = 26, desc = "Raise")
        }
        val delta = when {
            prev == null -> "First weigh-in"
            else -> {
                val whenTxt = if (prev.day == u.today - 1) "yesterday" else dayMonth(date(prev.day))
                "${signed1(u.wDraft - prev.kg)} kg vs $whenTxt (${kg1(prev.kg)} kg)"
            }
        }
        Txt(delta, 13f, color = T.muted, modifier = Modifier.fillMaxWidth(), tnum = true, align = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetBtn("Cancel", false, vm::goBack)
            SheetBtn("Save", true, vm::saveWeight)
        }
    }
}

@Composable
private fun Fabs(vm: MacraViewModel, modifier: Modifier) {
    val T = LocalT.current
    Column(modifier.padding(end = 16.dp, bottom = 16.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val s16 = RoundedCornerShape(16.dp)
        Box(
            Modifier.size(52.dp).shadow(10.dp, s16).clip(s16).background(T.surface2).clickable(onClick = vm::openScan),
            contentAlignment = Alignment.Center,
        ) { Sym("barcode_scanner", 24) }
        val s18 = RoundedCornerShape(18.dp)
        Row(
            Modifier.height(56.dp).shadow(12.dp, s18).clip(s18).background(T.acc).clickable { vm.openSearch() }.padding(start = 18.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Sym("add", 24, T.onAcc)
            Txt("Log food", 16f, 750, T.onAcc)
        }
    }
}

@Composable
private fun TabBar(tab: Tab, onSelect: (Tab) -> Unit) {
    val T = LocalT.current
    Column(Modifier.fillMaxWidth().background(T.bg)) {
        Divider()
        Row(Modifier.fillMaxWidth().height(80.dp)) {
            listOf(Triple(Tab.Diary, "Diary", "menu_book"), Triple(Tab.Progress, "Progress", "monitoring"), Triple(Tab.Profile, "Profile", "person")).forEach { (t, label, icon) ->
                val on = t == tab
                val pill by animateColorAsState(if (on) T.ink else Color.Transparent, tween(150), label = "pill")
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(t) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Box(Modifier.size(64.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(pill), contentAlignment = Alignment.Center) {
                        Sym(icon, 24, if (on) T.bg else T.muted, fill = on)
                    }
                    Txt(label, 12f, if (on) 750 else 500, if (on) T.ink else T.muted)
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun BoxScope.SnackHost(snack: app.macra.Snack?, vm: MacraViewModel) {
    val T = LocalT.current
    AnimatedVisibility(
        visible = snack != null,
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 92.dp),
        enter = slideInVertically(tween(220, easing = EASE)) { it } + fadeIn(tween(220)),
        exit = fadeOut(tween(150)),
    ) {
        var last by remember { mutableStateOf(snack) }
        if (snack != null) last = snack
        val s = last ?: return@AnimatedVisibility
        val shape = RoundedCornerShape(14.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).shadow(12.dp, shape).clip(shape).background(T.ink).padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Txt(s.msg, 14f, 400, T.bg, Modifier.weight(1f), lh = 1.35f)
            if (s.undo != null) {
                Box(Modifier.height(36.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = vm::undo).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Txt("Undo", 14f, 800, T.bg)
                }
            }
        }
    }
}
