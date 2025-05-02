package pl.podkal.domowniczeqqq.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("NOTIFICATION_TITLE") ?: return
        val content = intent.getStringExtra("NOTIFICATION_CONTENT") ?: return

        NotificationHelper.showNotification(
            context,
            title,
            content,
            title.hashCode()
        )
    }
}
