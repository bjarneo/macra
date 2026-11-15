package app.macra.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.macra.MainActivity
import app.macra.R
import app.macra.data.Meal
import app.macra.data.Settings
import app.macra.data.Store
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/**
 * Schedules one alarm at a time: the next reminder slot. When it fires, the
 * receiver posts the due reminders and schedules the slot after it.
 */
object Reminders {
    const val EXTRA_ACTION = "macra.action"
    private const val CHANNEL = "reminders"
    private const val EXTRA_MINUTE = "minute"

    data class Slot(val minute: Int, val kind: String, val meal: Meal? = null)

    fun slots(s: Settings): List<Slot> = buildList {
        if (s.remWeigh) add(Slot(7 * 60 + 30, "weigh"))
        if (s.remMeals) {
            add(Slot(8 * 60, "meal", Meal.Breakfast))
            add(Slot(12 * 60 + 30, "meal", Meal.Lunch))
            add(Slot(19 * 60, "meal", Meal.Dinner))
        }
        if (s.remWater) for (h in 8..20 step 2) add(Slot(h * 60, "water"))
    }

    private fun pending(ctx: Context, minute: Int): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java).putExtra(EXTRA_MINUTE, minute)
        return PendingIntent.getBroadcast(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(ctx: Context, s: Settings) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val minutes = slots(s).map { it.minute }.distinct().sorted()
        am.cancel(pending(ctx, 0))
        if (minutes.isEmpty()) return
        val now = LocalDateTime.now()
        val nowMin = now.hour * 60 + now.minute
        val nextToday = minutes.firstOrNull { it > nowMin }
        val (day, minute) = if (nextToday != null) LocalDate.now() to nextToday else LocalDate.now().plusDays(1) to minutes.first()
        val at = day.atTime(LocalTime.of(minute / 60, minute % 60)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setWindow(AlarmManager.RTC_WAKEUP, at, 5 * 60 * 1000L, pending(ctx, minute))
    }

    fun channel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT))
    }

    internal fun fire(ctx: Context, minute: Int) {
        val data = Store.load(ctx)
        val today = LocalDate.now().toEpochDay()
        val due = slots(data.settings).filter { it.minute == minute }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < 33
        ) {
            channel(ctx)
            due.forEach { slot ->
                when (slot.kind) {
                    "meal" -> {
                        val meal = slot.meal ?: return@forEach
                        if (data.days[today]?.get(meal).orEmpty().isEmpty()) {
                            post(ctx, 100 + meal.ordinal, "Log ${meal.label.lowercase(Locale.US)}", "Add what you ate to keep today's totals correct.", "meal:${meal.name}")
                        }
                    }
                    "water" -> {
                        val ml = data.water[today] ?: 0
                        val goal = data.goals.water
                        if (ml < goal) {
                            post(ctx, 110, "Drink water", String.format(Locale.US, "%.2f L of %.2f L today.", ml / 1000.0, goal / 1000.0), "water")
                        }
                    }
                    "weigh" -> if (data.weightOn(today) == null) {
                        post(ctx, 120, "Weigh in", "Log today's weight before breakfast.", "weigh")
                    }
                }
            }
        }
        schedule(ctx, data.settings)
    }

    private fun post(ctx: Context, id: Int, title: String, text: String, action: String) {
        val open = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_ACTION, action)
        val pi = PendingIntent.getActivity(ctx, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_macra)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val minute = intent.getIntExtra("minute", -1)
        val pending = goAsync()
        Thread {
            try {
                if (minute >= 0) Reminders.fire(ctx, minute)
            } finally {
                pending.finish()
            }
        }.start()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                Reminders.schedule(ctx, Store.load(ctx).settings)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
