package app.macra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val EASE = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

fun textStyle(
    size: Float,
    weight: Int = 400,
    color: Color,
    ls: Float = 0f,
    lh: Float? = null,
    tnum: Boolean = false,
    align: TextAlign = TextAlign.Unspecified,
) = TextStyle(
    color = color,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    fontFamily = Grotesk,
    letterSpacing = ls.em,
    lineHeight = lh?.let { (size * it).sp } ?: TextUnit.Unspecified,
    lineHeightStyle = if (lh != null) LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both) else null,
    fontFeatureSettings = if (tnum) "tnum" else null,
    textAlign = align,
)

@Composable
fun Txt(
    text: String,
    size: Float,
    weight: Int = 400,
    color: Color = LocalT.current.ink,
    modifier: Modifier = Modifier,
    ls: Float = 0f,
    lh: Float? = null,
    tnum: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false,
    align: TextAlign = TextAlign.Unspecified,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = textStyle(size, weight, color, ls, lh, tnum, align),
        maxLines = maxLines,
        softWrap = maxLines != 1,
        overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}

/** Small uppercase section label. */
@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = LocalT.current.muted) {
    Txt(text.uppercase(), 11f, 700, color, modifier, ls = 0.08f, maxLines = 1)
}

/** Material Symbols icon, drawn from the bundled ligature font. */
@Composable
fun Sym(name: String, size: Int = 24, color: Color = LocalT.current.ink, fill: Boolean = false, modifier: Modifier = Modifier) {
    val fs = with(LocalDensity.current) { size.dp.toSp() }
    Box(modifier.size(size.dp), contentAlignment = Alignment.Center) {
        BasicText(
            name,
            style = TextStyle(
                fontFamily = if (fill) SymbolsFill else Symbols,
                fontSize = fs,
                lineHeight = fs,
                color = color,
                textAlign = TextAlign.Center,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
fun Dot(color: Color, size: Int = 8) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(2.dp)).background(color))
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    radius: Int = 24,
    pad: PaddingValues = PaddingValues(20.dp),
    gap: Int = 16,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val T = LocalT.current
    val shape = RoundedCornerShape(radius.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(T.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(pad),
        verticalArrangement = Arrangement.spacedBy(gap.dp),
        content = content,
    )
}

@Composable
fun <K> Seg(options: List<Pair<K, String>>, current: K, onSelect: (K) -> Unit, h: Int = 32, padH: Int = 12) {
    val T = LocalT.current
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(T.surface2).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (k, label) ->
            val sel = k == current
            val bg by animateColorAsState(if (sel) T.ink else Color.Transparent, tween(150), label = "seg")
            Box(
                Modifier
                    .height(h.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(bg)
                    .clickable(role = Role.Tab) { onSelect(k) }
                    .padding(horizontal = padH.dp),
                contentAlignment = Alignment.Center,
            ) {
                Txt(label, 13f, 700, if (sel) T.bg else T.ink, maxLines = 1)
            }
        }
    }
}

@Composable
fun Chip(label: String, sel: Boolean, onClick: () -> Unit, weight: Int = 650) {
    val T = LocalT.current
    val shape = RoundedCornerShape(17.dp)
    Box(
        Modifier
            .height(34.dp)
            .clip(shape)
            .background(if (sel) T.ink else Color.Transparent)
            .border(1.dp, if (sel) T.ink else T.bar, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, 14f, weight, if (sel) T.bg else T.ink, maxLines = 1)
    }
}

@Composable
fun Toggle(on: Boolean) {
    val T = LocalT.current
    val track by animateColorAsState(if (on) T.acc else T.surface2, tween(150), label = "track")
    val border by animateColorAsState(if (on) T.acc else T.muted, tween(150), label = "border")
    val thumb by animateDpAsState(if (on) 24.dp else 16.dp, tween(150), label = "thumb")
    val x by animateDpAsState(if (on) 22.dp else 6.dp, tween(150), label = "x")
    Box(
        Modifier
            .size(52.dp, 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(track)
            .border(2.dp, border, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = x - 2.dp)
                .size(thumb)
                .clip(RoundedCornerShape(12.dp))
                .background(if (on) T.onAcc else T.muted),
        )
    }
}

@Composable
fun IconBtn(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 48,
    radius: Int = size / 2,
    bg: Color = Color.Transparent,
    fg: Color = LocalT.current.ink,
    iconSize: Int = 24,
    desc: String? = null,
) {
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape(radius.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .semantics { if (desc != null) contentDescription = desc; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Sym(icon, iconSize, fg)
    }
}

/** A step button that repeats while the finger stays down. */
@Composable
fun StepBtn(icon: String, onStep: () -> Unit, size: Int = 40, radius: Int = 14, iconSize: Int = 20, desc: String? = null) {
    val T = LocalT.current
    val cb by rememberUpdatedState(onStep)
    var pressed by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(radius.dp))
            .background(if (pressed) T.bar else T.surface2)
            .semantics { if (desc != null) contentDescription = desc; role = Role.Button }
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown()
                        pressed = true
                        cb()
                        val job = launch {
                            delay(420)
                            var n = 0
                            while (true) {
                                cb()
                                n++
                                delay(if (n < 12) 90L else if (n < 40) 45L else 20L)
                            }
                        }
                        waitForUpOrCancellation()
                        job.cancel()
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Sym(icon, iconSize, T.ink)
    }
}

@Composable
fun OutlineBtn(text: String, icon: String?, onClick: () -> Unit, modifier: Modifier = Modifier, height: Int = 40, color: Color = LocalT.current.ink, border: Color = LocalT.current.bar, bg: Color = Color.Transparent) {
    val shape = RoundedCornerShape((height / 2).dp)
    Row(
        modifier
            .height(height.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(start = if (icon != null) 14.dp else 18.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Sym(icon, 20, color)
        Txt(text, 14f, 650, color, maxLines = 1)
    }
}

@Composable
fun AccentBtn(text: String, icon: String?, onClick: () -> Unit, modifier: Modifier = Modifier, height: Int = 44) {
    val T = LocalT.current
    Row(
        modifier
            .height(height.dp)
            .clip(RoundedCornerShape((height / 2).dp))
            .background(T.acc)
            .clickable(onClick = onClick)
            .padding(start = if (icon != null) 14.dp else 18.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Sym(icon, 20, T.onAcc)
        Txt(text, 14f, 700, T.onAcc, maxLines = 1)
    }
}

/** Full-width action button used in sheets. */
@Composable
fun RowScope.SheetBtn(text: String, primary: Boolean, onClick: () -> Unit, weight: Float = 1f, enabled: Boolean = true) {
    val T = LocalT.current
    Box(
        Modifier
            .weight(weight)
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (primary) T.acc else T.surface2)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Txt(text, 15f, if (primary) 750 else 700, if (primary) T.onAcc else T.ink, maxLines = 1)
    }
}

@Composable
fun Meter(pct: Float, color: Color, height: Dp, modifier: Modifier = Modifier, radius: Dp = height / 2) {
    val T = LocalT.current
    val a by animateFloatAsState(pct.coerceIn(0f, 100f) / 100f, tween(300), label = "meter")
    val shape = RoundedCornerShape(radius)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(T.surface2)) {
        if (a > 0f) Box(Modifier.fillMaxWidth(a).fillMaxHeight().clip(shape).background(color))
    }
}

/** A horizontal split bar with parts in proportion to their values. */
@Composable
fun Split(parts: List<Pair<Float, Color>>, height: Dp, radius: Dp, modifier: Modifier = Modifier, track: Color = Color.Transparent) {
    val total = parts.sumOf { it.first.toDouble() }.toFloat()
    Row(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(radius)).background(track),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (total > 0f) parts.filter { it.first > 0f }.forEach { (v, c) ->
            Box(Modifier.weight(v / total).fillMaxHeight().background(c))
        }
    }
}

@Composable
fun Handle() {
    val T = LocalT.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.width(32.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(T.bar))
    }
}

@Composable
fun Segments(n: Int, filled: Int, on: Color, off: Color, height: Dp, radius: Dp, gap: Dp, modifier: Modifier = Modifier, onTap: ((Int) -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
        for (i in 0 until n) {
            val c by animateColorAsState(if (i < filled) on else off, tween(150), label = "segment")
            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(radius))
                    .background(c)
                    .then(if (onTap != null) Modifier.clickable { onTap(i) } else Modifier),
            )
        }
    }
}

/** Row of a big number and a smaller unit, aligned on the text baseline. */
@Composable
fun BigNum(value: String, unit: String, size: Float, unitSize: Float, weight: Int = 800, ls: Float = -0.05f, unitWeight: Int = 600, gap: Int = 6, color: Color = LocalT.current.ink, unitColor: Color = LocalT.current.muted) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
        Txt(value, size, weight, color, Modifier.alignByBaseline(), ls = ls, tnum = true, maxLines = 1)
        if (unit.isNotEmpty()) Txt(unit, unitSize, unitWeight, unitColor, Modifier.alignByBaseline(), tnum = true, maxLines = 1)
    }
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalT.current.line))
}

val NoPad = PaddingValues(0.dp)
