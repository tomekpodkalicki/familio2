package pl.podkal.domowniczeqqq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.AndroidEntryPoint
import pl.podkal.domowniczeqqq.login.AuthenticationManager
import pl.podkal.domowniczeqqq.navigation.NavigationGraph
import pl.podkal.domowniczeqqq.ui.theme.DomowniczeqTheme

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase with offline persistence
        FirebaseApp.initializeApp(this)
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings

        val authenticationManager = AuthenticationManager(this)

        setContent {
            DomowniczeqTheme {
                val navController = rememberNavController()
                NavigationGraph(navController)
            }
        }
    }
}

