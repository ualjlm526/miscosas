package com.objetivo70.app.notifications

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import com.objetivo70.app.MainActivity
import java.time.*
import java.util.Calendar

object ReminderManager {
    private const val CHANNEL = "objetivo70_reminders"
    private val reminders = listOf(
        Triple(101, LocalTime.of(8, 0), "Registrar desayuno"),
        Triple(102, LocalTime.of(14, 0), "Registrar comida"),
        Triple(103, LocalTime.of(21, 0), "Registrar cena")
    )

    fun scheduleDaily(context: Context, enabled: Boolean) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        reminders.forEach { (id, time, text) ->
            val pi = pending(context, id, text)
            alarm.cancel(pi)
            if (enabled) alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, nextMillis(time), AlarmManager.INTERVAL_DAY, pi)
        }
    }

    fun scheduleSundayWeighIn(context: Context, enabled: Boolean) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, 104, "Hoy es domingo: registra tu peso al despertar")
        alarm.cancel(pi)
        if (enabled) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY); set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 5); set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
            }
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, 7 * AlarmManager.INTERVAL_DAY, pi)
        }
    }

    private fun nextMillis(time: LocalTime): Long {
        val now = ZonedDateTime.now()
        var next = now.toLocalDate().atTime(time).atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.toInstant().toEpochMilli()
    }

    private fun pending(context: Context, id: Int, text: String): PendingIntent = PendingIntent.getBroadcast(
        context, id, Intent(context, ReminderReceiver::class.java).putExtra("text", text),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun notify(context: Context, text: String) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Objetivo 70")
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(text.hashCode(), notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderManager.notify(context, intent.getStringExtra("text") ?: "Recuerda registrar tu progreso")
    }
}
