package echo.music.iad1tya.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** One-off joke notification on 19 Nov 2026. Scheduled with an inexact alarm; also fires on next app start if the alarm was missed. */
object GtaLaunchNotifier {
  private const val CH = "gta_vi_launch"
  private const val PREFS = "gta_launch"

  fun check(context: Context) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    if (prefs.getBoolean("shown", false)) return
    val today = LocalDate.now()
    val launch = LocalDate.of(2026, 11, 19)
    when {
      today == launch -> show(context)
      today.isBefore(launch) -> {
        val at = LocalDateTime.of(2026, 11, 19, 9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = PendingIntent.getBroadcast(context, 1906, Intent(context, GtaLaunchReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.set(AlarmManager.RTC, at, pi)
      }
    }
  }

  fun show(context: Context) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    if (prefs.getBoolean("shown", false)) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(NotificationChannel(CH, "Special days", NotificationManager.IMPORTANCE_DEFAULT))
    val n = NotificationCompat.Builder(context, CH)
      .setSmallIcon(NotificationIcon.current(context)).setColor(0xFF2F7BFF.toInt())
      .setContentTitle("GTA VI is launched 🤣")
      .setContentText("Pattukal has a GTA VI theme and player style now. Try it in Settings → Theme.")
      .setAutoCancel(true)
      .build()
    try { nm.notify(1906, n); prefs.edit().putBoolean("shown", true).apply() } catch (_: SecurityException) {}
  }
}

class GtaLaunchReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) { GtaLaunchNotifier.show(context) }
}
