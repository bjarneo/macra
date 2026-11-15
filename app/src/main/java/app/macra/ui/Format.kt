package app.macra.ui

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val L = Locale.US

/** Rounds to a whole number with thousands separators. */
fun fmt(n: Double): String {
    val v = n.roundToLong()
    return String.format(L, "%,d", if (v == 0L) 0L else v)
}

/** Like [fmt], but negative values use a true minus sign. */
fun sfmt(n: Double): String = if (n.roundToLong() < 0) "−" + fmt(-n) else fmt(n)

/** One decimal, without a trailing ".0". */
fun r1(n: Double): String {
    val v = Math.round(n * 10) / 10.0
    return if (v == Math.floor(v)) v.toLong().toString() else String.format(L, "%.1f", v)
}

fun kg1(n: Double): String = String.format(L, "%.1f", n)

fun signed1(n: Double): String = (if (n < -0.04) "−" else "+") + kg1(abs(n))

fun litres(ml: Int): String = String.format(L, "%.2f L", ml / 1000.0)

fun date(day: Long): LocalDate = LocalDate.ofEpochDay(day)

fun dow(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.SHORT, L)

fun dowFull(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.FULL, L)

fun dowLetter(d: LocalDate): String = dow(d).take(1)

fun mon(d: LocalDate): String = d.month.getDisplayName(TextStyle.SHORT, L)

/** For example "12 Nov", with the year when it is not the current year. */
fun dayMonth(d: LocalDate, today: LocalDate = LocalDate.now()): String =
    if (d.year == today.year) "${d.dayOfMonth} ${mon(d)}" else "${d.dayOfMonth} ${mon(d)} ${d.year}"

/** For example "Thu 12 Nov". */
fun dateSub(day: Long): String {
    val d = date(day)
    return "${dow(d)} ${dayMonth(d)}"
}

fun dateTitle(day: Long, today: Long): String = when {
    day == today -> "Today"
    day == today - 1 -> "Yesterday"
    today - day < 7 -> dowFull(date(day))
    else -> dayMonth(date(day))
}
