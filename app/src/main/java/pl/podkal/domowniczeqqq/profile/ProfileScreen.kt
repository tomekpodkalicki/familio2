package pl.podkal.domowniczeqqq.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import pl.podkal.domowniczeqqq.navigation.BottomNavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val appBarColor = Color(0xFF3DD1C6) // Turkusowy (primary)
    val backgroundColor = Color(0xFFF8F8F8) // Jasnoszary (background)

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarColor
                ),
                title = {
                    Text(
                        text = "Profil Użytkownika",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Profil Użytkownika - funkcja w trakcie tworzenia",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}