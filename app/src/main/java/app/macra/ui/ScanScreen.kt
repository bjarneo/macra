package app.macra.ui

import android.annotation.SuppressLint
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.macra.MacraViewModel
import app.macra.ScanState
import app.macra.Ui
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private val SCAN_BG = Color(0xFF060606)
private val WHITE = Color.White

@Composable
fun ScanScreen(u: Ui, vm: MacraViewModel, camGranted: Boolean, onAllowCamera: () -> Unit) {
    val T = LocalT.current
    val haptic = LocalHapticFeedback.current
    val s = u.scan
    LaunchedEffect(s is ScanState.Scanning) {
        if (s !is ScanState.Scanning) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Box(Modifier.fillMaxSize().background(SCAN_BG)) {
        if (camGranted) {
            CameraPreview(active = s is ScanState.Scanning, torch = u.torch, onCode = vm::onBarcode)
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val step = 14.dp.toPx()
                var x = -size.height
                var i = 0
                while (x < size.width) {
                    drawLine(if (i % 2 == 0) Color(0xFF0F0F10) else Color(0xFF141416), Offset(x, 0f), Offset(x + size.height, size.height), step * 1.42f)
                    x += step * 1.42f
                    i++
                }
            }
        }
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconBtn("close", vm::goBack, bg = Color.Black.copy(alpha = 0.35f), fg = WHITE, desc = "Close")
                Txt("Scan barcode", 16f, 650, WHITE, Modifier.weight(1f).padding(start = 8.dp))
                if (camGranted) {
                    IconBtn(if (u.torch) "flashlight_off" else "flashlight_on", vm::toggleTorch, bg = Color.Black.copy(alpha = 0.35f), fg = WHITE, desc = "Torch")
                }
            }
            Box(Modifier.align(Alignment.TopCenter).padding(top = 196.dp).size(292.dp, 172.dp)) {
                Corners()
                when (s) {
                    ScanState.Scanning -> if (camGranted) ScanLine(T.acc)
                    is ScanState.LookingUp -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(40.dp), color = WHITE, strokeWidth = 3.dp)
                    }
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                            Sym(if (s is ScanState.Failed) "wifi_off" else "error", 32, WHITE)
                        }
                    }
                }
            }
            Column(
                Modifier.align(Alignment.TopCenter).padding(top = 392.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val hint = when {
                    !camGranted -> "Camera access is off"
                    s is ScanState.Scanning -> "Align the barcode inside the frame"
                    s is ScanState.LookingUp -> "Looking up ${s.code}"
                    else -> "Barcode read"
                }
                Txt(hint, 15f, 600, WHITE, align = TextAlign.Center)
                if (!camGranted) OutlineBtn("Allow camera", "barcode_scanner", onAllowCamera, Modifier.padding(top = 12.dp), color = WHITE, border = WHITE.copy(alpha = 0.3f))
            }
        }
        AnimatedVisibility(
            visible = s is ScanState.Missing || s is ScanState.Failed,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(280, easing = EASE)) { it / 3 } + fadeIn(tween(200)),
            exit = fadeOut(tween(120)),
        ) {
            var last by remember { mutableStateOf(s) }
            if (s !is ScanState.Scanning && s !is ScanState.LookingUp) last = s
            ResultSheet(last, vm)
        }
    }
}

@Composable
private fun ResultSheet(s: ScanState, vm: MacraViewModel) {
    val T = LocalT.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(T.surface)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Handle()
        when (s) {
            is ScanState.Missing -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Label("No match · EAN ${s.code}")
                    Txt(if (s.noNutrition) "No nutrition data for this product" else "This product is not in Open Food Facts", 22f, 800, ls = -0.02f)
                    Txt("Search by name, or scan a different barcode.", 13f, color = T.muted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetBtn("Scan again", false, vm::scanAgain)
                    SheetBtn("Search by name", true, vm::scanToSearch, weight = 1.4f)
                }
            }
            is ScanState.Failed -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Label("EAN ${s.code}")
                    Txt("Could not reach Open Food Facts", 22f, 800, ls = -0.02f)
                    Txt("Check the connection, then try again.", 13f, color = T.muted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetBtn("Scan again", false, vm::scanAgain)
                    SheetBtn("Try again", true, vm::retryLookup, weight = 1.4f)
                }
            }
            else -> Unit
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun Corners() {
    Canvas(Modifier.fillMaxSize()) {
        val sw = 4.dp.toPx()
        val len = 34.dp.toPx()
        val r = 16.dp.toPx()
        val w = size.width
        val h = size.height
        listOf(Offset(0f, 0f), Offset(w - len, 0f), Offset(0f, h - len), Offset(w - len, h - len)).forEach { o ->
            clipRect(o.x, o.y, o.x + len, o.y + len) {
                drawRoundRect(WHITE, Offset(sw / 2, sw / 2), Size(w - sw, h - sw), CornerRadius(r - sw / 2), style = Stroke(sw))
            }
        }
    }
}

@Composable
private fun ScanLine(color: Color) {
    val t = rememberInfiniteTransition(label = "scan")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse), label = "scanY")
    Canvas(Modifier.fillMaxSize().clipToBounds()) {
        val e = if (p < 0.5f) 2 * p * p else 1 - (-2 * p + 2) * (-2 * p + 2) / 2
        val top = 8.dp.toPx()
        val bottom = size.height - 10.dp.toPx()
        val y = top + (bottom - top) * e
        val x0 = 12.dp.toPx()
        val glow = 14.dp.toPx()
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.35f), Color.Transparent), startY = y - glow, endY = y + glow),
            Offset(x0, y - glow), Size(size.width - 2 * x0, glow * 2),
        )
        drawRect(color, Offset(x0, y - 1.dp.toPx()), Size(size.width - 2 * x0, 2.dp.toPx()))
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun CameraPreview(active: Boolean, torch: Boolean, onCode: (String) -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val isActive = rememberUpdatedState(active)
    val callback = rememberUpdatedState(onCode)
    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember {
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val scanner: BarcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                .build(),
        )
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(owner) {
        val future = ProcessCameraProvider.getInstance(ctx)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        future.addListener({
            if (disposed) return@addListener
            val p = future.get()
            provider = p
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy(android.util.Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build(),
                )
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val img = proxy.image
                if (!isActive.value || img == null) {
                    proxy.close()
                    return@setAnalyzer
                }
                val input = InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees)
                scanner.process(input)
                    .addOnSuccessListener { codes ->
                        val v = codes.firstNotNullOfOrNull { b -> b.rawValue?.takeIf { it.length >= 8 && it.all(Char::isDigit) } }
                        if (v != null && isActive.value) callback.value(v)
                    }
                    .addOnCompleteListener { proxy.close() }
            }
            try {
                p.unbindAll()
                camera = p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (_: Exception) {
                camera = null
            }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose {
            disposed = true
            provider?.unbindAll()
            camera = null
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }
    LaunchedEffect(camera, torch) {
        camera?.let { if (it.cameraInfo.hasFlashUnit()) it.cameraControl.enableTorch(torch) }
    }
    AndroidView({ previewView }, Modifier.fillMaxSize())
}
