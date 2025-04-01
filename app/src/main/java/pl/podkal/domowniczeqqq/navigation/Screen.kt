package pl.podkal.domowniczeqqq.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    // Main bottom bar items
    object Home : Screen("home_screen", "Kalendarz", Icons.Filled.CalendarMonth)
    object Finance : Screen("finance", "Finanse", Icons.Filled.Money)
    object Receipts : Screen("receipts", "Multimedia", Icons.Filled.PhotoLibrary)
    object More : Screen("more", "Więcej", Icons.Filled.MoreHoriz)

    // Items for the "More" dropdown menu
    object Notes : Screen("notes_screen", "Notatki", Icons.Filled.NoteAlt)
    object Pantry : Screen("pantry_screen", "Spiżarnia", Icons.Filled.Kitchen)
    object Profile : Screen("profile_screen", "Profil", Icons.Filled.Person)

    // Get main screens for bottom navigation
    object Shopping : Screen("shopping_screen", "Lista zakupów", Icons.Filled.ShoppingCart)

    companion object {
        fun getMainScreens() = listOf(Home, Finance, Receipts, More)

        // Get dropdown screens that appear in the "More" menu
        fun getMoreScreens() = listOf(Notes, Pantry, Shopping, Profile)
    }
}