package pl.podkal.domowniczeqqq.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("notificationId", 0)
        Log.d("NotifReceiver", "onReceive (id=$id)")

        val title   = intent.getStringExtra("title") ?: return
        val content = intent.getStringExtra("content") ?: return
        NotificationHelper.showNotification(context, title, content, id)
    }
}
