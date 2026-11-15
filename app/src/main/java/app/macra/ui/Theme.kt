@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package app.macra.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.macra.R
import kotlin.math.pow

@Immutable
data class Tokens(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val bar: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val p: Color,
    val c: Color,
    val f: Color,
    val w: Color,
    val acc: Color,
    val onAcc: Color,
)

fun tokens(dark: Boolean): Tokens {
    val acc = Color(0xFFFF2E4C)
    val onAcc = onColor(acc)
    return if (dark) {
        Tokens(
            dark = true,
            bg = Color(0xFF0C0C0D), surface = Color(0xFF18181A), surface2 = Color(0xFF242427), bar = Color(0xFF3A3A3E),
            ink = Color(0xFFF5F5F2), muted = Color(0xFF9B9B97), line = Color.White.copy(alpha = 0.08f),
            p = Color(0xFF88AFFF), c = Color(0xFF6ED889), f = Color(0xFFF3BD5C), w = Color(0xFF59C8E8),
            acc = acc, onAcc = onAcc,
        )
    } else {
        Tokens(
            dark = false,
            bg = Color(0xFFF3F3F0), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEAEAE6), bar = Color(0xFFD2D2CD),
            ink = Color(0xFF0E0E0F), muted = Color(0xFF62625F), line = Color.Black.copy(alpha = 0.09f),
            p = Color(0xFF3F69D3), c = Color(0xFF25984D), f = Color(0xFFD98B09), w = Color(0xFF008DBE),
            acc = acc, onAcc = onAcc,
        )
    }
}

/** Picks near-black or white text for the best contrast on the accent color. */
fun onColor(c: Color): Color {
    fun lin(v: Float): Double = if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    val l = 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    return if ((l + 0.05) / 0.05 >= 1.05 / (l + 0.05)) Color(0xFF0C0C0D) else Color.White
}

val LocalT = staticCompositionLocalOf { tokens(true) }

private val WEIGHTS = listOf(400, 500, 600, 650, 700, 750, 800)

val Grotesk = FontFamily(
    WEIGHTS.map { w ->
        Font(R.font.host_grotesk, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

