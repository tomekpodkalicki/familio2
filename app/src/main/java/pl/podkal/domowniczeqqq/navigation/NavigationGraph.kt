package pl.podkal.domowniczeqqq.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import pl.podkal.domowniczeqqq.finance.FinanceScreen
import pl.podkal.domowniczeqqq.home.HomeScreen
import pl.podkal.domowniczeqqq.home.auth
import pl.podkal.domowniczeqqq.login.LoginScreen
import pl.podkal.domowniczeqqq.login.LoginViewModel
import pl.podkal.domowniczeqqq.notes.ArchiveScreen
import pl.podkal.domowniczeqqq.notes.NotesScreen
import pl.podkal.domowniczeqqq.pantry.PantryScreen
import pl.podkal.domowniczeqqq.profile.ProfileScreen
import pl.podkal.domowniczeqqq.receipts.ReceiptsScreen
import pl.podkal.domowniczeqqq.register.RegisterScreen
import pl.podkal.domowniczeqqq.shopping.ShoppingScreen
import kotlin.math.abs

// Navigation order for swipe gestures (only main screens)
private val navigationOrder = listOf(
    Screen.Home.route,
    Screen.Finance.route,
    Screen.Receipts.route,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavigationGraph(navController: NavHostController) {
    val loginViewModel: LoginViewModel = hiltViewModel()
    val destination = loginViewModel.savedStateHandle.get<String>("navigation_destination") ?: "home_screen"
    val currentUser = auth.currentUser

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            navController.navigate("home_screen") {
                popUpTo("login_screen") { inclusive = true }
            }
        } else {
            navController.navigate("login_screen") {
                popUpTo("home_screen") { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login_screen") {
        composable("login_screen") { LoginScreen(navController) }
        composable("register_screen") { RegisterScreen(navController) }

        // Main screens with swipe gesture support
        composable("home_screen") {
            SwipeNavigation(navController, Screen.Home.route) {
                HomeScreen(navController)
            }
        }
        composable("finance") {
            SwipeNavigation(navController, Screen.Finance.route) {
                FinanceScreen(navController)
            }
        }
        composable("receipts") {
            SwipeNavigation(navController, Screen.Receipts.route) {
                ReceiptsScreen(navController)
            }
        }

        // Other screens
        composable("notes_screen") { NotesScreen(navController) }
        composable("archive_screen") { ArchiveScreen(navController) }
        composable("pantry_screen") { PantryScreen(navController) }
        composable("shopping_screen") { ShoppingScreen(navController) }
        composable("profile_screen") { ProfileScreen(navController) }
    }
}

@Composable
fun SwipeNavigation(
    navController: NavHostController,
    currentRoute: String,
    content: @Composable () -> Unit
) {
    // Minimum distance in dp to trigger navigation
    val swipeThreshold = 50.dp
    val density = LocalDensity.current.density
    val swipeThresholdPx = swipeThreshold.value * density

    // Track initial touch position
    var startX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { startX = it.x },
                    onDragEnd = {},
                    onDragCancel = {},
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()

                        // On swipe end, check direction and distance
                        if (abs(startX - change.position.x) > swipeThresholdPx) {
                            val currentIndex = navigationOrder.indexOf(currentRoute)

                            // Only navigate if current screen is in the navigation order
                            if (currentIndex != -1) {
                                // Swipe right to go to previous screen
                                if (change.position.x > startX && currentIndex > 0) {
                                    val previousRoute = navigationOrder[currentIndex - 1]
                                    navController.navigate(previousRoute) {
                                        popUpTo(
                                            navController.graph.startDestinationRoute
                                                ?: Screen.Home.route
                                        ) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                // Swipe left to go to next screen
                                else if (change.position.x < startX && currentIndex < navigationOrder.size - 1) {
                                    val nextRoute = navigationOrder[currentIndex + 1]
                                    navController.navigate(nextRoute) {
                                        popUpTo(
                                            navController.graph.startDestinationRoute
                                                ?: Screen.Home.route
                                        ) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }

                                // Reset start position
                                startX = change.position.x
                            }
                        }
                    }
                )
            }
    ) {
        content()
    }
}