package pl.podkal.domowniczeqqq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pl.podkal.domowniczeqqq.login.AuthenticationManager
import pl.podkal.domowniczeqqq.login.LoginScreen
import pl.podkal.domowniczeqqq.navigation.NavigationGraph
import pl.podkal.domowniczeqqq.ui.theme.DomowniczeqTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authenticationManager = AuthenticationManager(this)
        setContent {
            DomowniczeqTheme {
               val navController = rememberNavController()
                NavigationGraph(navController)

            }
        }
    }
}

