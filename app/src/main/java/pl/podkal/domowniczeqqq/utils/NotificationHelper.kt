package pl.podkal.domowniczeqqq.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import pl.podkal.domowniczeq.R
import java.util.concurrent.atomic.AtomicReference

object NotificationHelper {

    private const val CHANNEL_ID   = "calendar_reminders"
    private const val CHANNEL_NAME = "Calendar Reminders"

    /** Bufor – jeżeli zabraknie uprawnień do exact-alarm, przechowujemy parametry
     *  i planujemy ponownie po powrocie z ustawień. */
    private val pendingRequest = AtomicReference<(() -> Unit)?>(null)

    // ---------- permissiony ----------

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                /*requestCode*/ 101
            )
        }
    }

    fun requestExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (am.canScheduleExactAlarms()) return true

        // poproś system o zgodę
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return false
    }

    // ---------- kanał ----------

    fun createNotificationChannel(context: Context) {
        if (context is Activity) requestNotificationPermission(context)

        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description     = "Notifications for calendar events"
            enableLights(true)
            enableVibration(true)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    // ---------- planowanie ----------

    fun scheduleNotification(
        context: Context,
        title: String,
        content: String,
        requestCode: Int,            // używaj stałego ID dla danego wydarzenia
        triggerTimeMillis: Long
    ) {
        Log.d("NotificationHelper", "Schedule @$triggerTimeMillis ($title)")

        // exact-alarm permission
        if (!requestExactAlarmPermission(context)) {
            pendingRequest.set {
                scheduleNotification(context, title, content, requestCode, triggerTimeMillis)
            }
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val broadcastIntent = Intent(context, NotificationBroadcastReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("content", content)
            putExtra("notificationId", requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, broadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent
        )
    }

    fun cancelNotification(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) alarmManager.cancel(pendingIntent)
    }

    // przy powrocie z ekranu ustawień
    fun retryIfPending() = pendingRequest.getAndSet(null)?.invoke()

    // ---------- wyświetlenie ----------

    fun showNotification(
        context: Context,
        title: String,
        content: String,
        notificationId: Int
    ) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId, builder.build())
    }
}
