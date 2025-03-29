package pl.podkal.domowniczeqqq.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private val FamilioTeal = Color(0xFF1ABC9C)

@Composable
fun BottomNavBar(navController: NavController) {
    val selectedContentColor = Color.White
    val unselectedContentColor = Color.LightGray

    // Get only the main screens for bottom navigation
    val mainItems = Screen.getMainScreens()

    // More menu dropdown state
    var showMoreMenu by remember { mutableStateOf(false) }

    // Get screen width to position the dropdown menu properly
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    // Position offset for dropdown menu - position on right side just above the bottom navigation bar
    // Use screenWidth to calculate proper right-aligned position
    val moreMenuOffset = remember { DpOffset(x = screenWidth - 210.dp, y = (-15).dp) }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp) // Lower height for more compact bar
            .clip(CircleShape),
        containerColor = FamilioTeal,
        contentColor = selectedContentColor,
        tonalElevation = 0.dp, // Remove shadow
    ) {
        mainItems.forEachIndexed { index, screen ->
            if (screen == Screen.More) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            modifier = Modifier.size(26.dp) // Smaller icon
                        )
                    },
                    label = {
                        Text(
                            text = screen.title,
                            fontSize = 11.sp, // Smaller text
                            textAlign = TextAlign.Center
                        )
                    },
                    selected = currentRoute in Screen.getMoreScreens().map { it.route },
                    onClick = { showMoreMenu = !showMoreMenu },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedContentColor,
                        selectedTextColor = selectedContentColor,
                        unselectedIconColor = unselectedContentColor,
                        unselectedTextColor = unselectedContentColor,
                        indicatorColor = FamilioTeal
                    ),
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                )

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    modifier = Modifier
                        .width(200.dp) // Fixed width
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp)) // Rounded corners
                        .background(FamilioTeal), // Match the color with bottom bar
                    offset = moreMenuOffset
                ) {
                    Screen.getMoreScreens().forEach { moreScreen ->
                        val isSelected = currentRoute == moreScreen.route
                        val itemColor = if (isSelected) selectedContentColor else unselectedContentColor

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = moreScreen.title,
                                    color = itemColor // White if selected, light gray if not
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = moreScreen.icon,
                                    contentDescription = moreScreen.title,
                                    tint = itemColor // White if selected, light gray if not
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                navController.navigate(moreScreen.route) {
                                    popUpTo(navController.graph.startDestinationRoute ?: Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            } else {
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            modifier = Modifier.size(26.dp) // Smaller icon to match More item
                        )
                    },
                    label = {
                        Text(
                            text = screen.title,
                            fontSize = 11.sp, // Smaller text to match More item
                            textAlign = TextAlign.Center
                        )
                    },
                    selected = currentRoute == screen.route,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationRoute ?: Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedContentColor,
                        selectedTextColor = selectedContentColor,
                        unselectedIconColor = unselectedContentColor,
                        unselectedTextColor = unselectedContentColor,
                        indicatorColor = FamilioTeal
                    ),
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                )
            }
        }
    }
}

@Preview
@Composable
fun previewBottom() {
    BottomNavBar(navController = rememberNavController())
}