package pl.podkal.domowniczeqqq.ui.theme

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp
import pl.podkal.domowniczeqqq.utils.NotificationHelper

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)

            // Create notification channel
            NotificationHelper.createNotificationChannel(this)

            // Configure Firestore for offline persistence
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings

            Log.d("MyApplication", "Firebase initialized with offline persistence")
        } catch (e: Exception) {
            Log.e("MyApplication", "Error initializing Firebase: ${e.message}")
        }
    }
}
