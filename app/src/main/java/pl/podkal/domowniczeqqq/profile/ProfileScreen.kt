package pl.podkal.domowniczeqqq.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import pl.podkal.domowniczeqqq.navigation.BottomNavBar

data class LinkedAccounts(
    val groupId: String,
    val users: List<String>,
    val ownerEmail: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val appBarColor = Color(0xFF3DD1C6)
    val backgroundColor = Color(0xFFF8F8F8)
    val context = LocalContext.current

    // Użytkownik i baza danych
    val user = Firebase.auth.currentUser
    val db = FirebaseFirestore.getInstance()

    // Zmienne stanu dla statystyk
    var pantryItemCount by remember { mutableStateOf(0) }
    var shoppingItemCount by remember { mutableStateOf(0) }
    var receiptsCount by remember { mutableStateOf(0) }

    // Pobieranie statystyk z Firestore
    LaunchedEffect(user?.uid) {
        user?.uid?.let { userId ->
            db.collection("pantry_items")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { pantryItemCount = it.size() }

            db.collection("shopping_items")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { shoppingItemCount = it.size() }

            db.collection("receipts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { receiptsCount = it.size() }
        }
    }

    var showLinkDialog by remember { mutableStateOf(false) }
    var linkedEmail by remember { mutableStateOf("") }
    var showSuccessToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var connectedAccounts by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(user?.uid) {
        user?.uid?.let { userId ->
            db.collection("linked_accounts")
                .whereArrayContains("users", userId)
                .get()
                .addOnSuccessListener { documents ->
                    val accounts = mutableListOf<String>()
                    documents.forEach { doc ->
                        val users = doc.get("users") as List<String>
                        users.forEach { u ->
                            if (u != userId) accounts.add(u)
                        }
                    }
                    connectedAccounts = accounts
                }
        }
    }


    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                // Nagłówek profilu
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(appBarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = user?.email ?: "Użytkownik",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Karty statystyk
                StatisticsCard(
                    title = "Spiżarnia",
                    count = pantryItemCount,
                    icon = Icons.Default.Kitchen
                )
                Spacer(modifier = Modifier.height(16.dp))
                StatisticsCard(
                    title = "Lista zakupów",
                    count = shoppingItemCount,
                    icon = Icons.Default.ShoppingCart
                )
                Spacer(modifier = Modifier.height(16.dp))
                StatisticsCard(
                    title = "Paragony",
                    count = receiptsCount,
                    icon = Icons.Default.Receipt
                )
                Spacer(modifier = Modifier.height(32.dp))

                if (connectedAccounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Połączone konta:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    connectedAccounts.forEach { userId ->
                        var email by remember { mutableStateOf("") }
                        LaunchedEffect(userId) {
                            db.collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener { doc ->
                                    email = doc.getString("email") ?: userId
                                }
                        }
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }


                // Przycisk wylogowania
                Button(
                    onClick = {
                        Firebase.auth.signOut()
                        navController.navigate("login_screen") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335))
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Wyloguj",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wyloguj się")
                }
                Button(onClick = { showLinkDialog = true }) {
                    Text("Połącz konto")
                }
            }
        }
    }

    if (showLinkDialog) {
        Dialog(onDismissRequest = { showLinkDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Połącz konto",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = linkedEmail,
                        onValueChange = { linkedEmail = it },
                        label = { Text("Email użytkownika") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showLinkDialog = false }) {
                            Text("Anuluj")
                        }
                        Button(
                            onClick = {
                                if (linkedEmail.isNotEmpty() && user != null) {
                                    // First check if this is not the same user
                                    if (linkedEmail == user.email) {
                                        toastMessage = "Nie możesz połączyć konta z samym sobą"
                                        showSuccessToast = true
                                        return@Button
                                    }

                                    // Get all users and find by email
                                    db.collection("users")
                                        .get()
                                        .addOnSuccessListener { documents ->
                                            val targetUser = documents.documents.find { doc ->
                                                doc.getString("email")?.equals(linkedEmail, ignoreCase = true) == true
                                            }

                                            if (targetUser != null) {
                                                val targetUserId = targetUser.id

                                                // Create invitation
                                                val invitation = mapOf(
                                                    "fromUserId" to user.uid!!,
                                                    "fromEmail" to (user.email ?: ""),
                                                    "toUserId" to targetUserId,
                                                    "status" to "pending",
                                                    "timestamp" to FieldValue.serverTimestamp()
                                                )

                                                db.collection("account_invitations")
                                                    .add(invitation)
                                                    .addOnSuccessListener {
                                                        toastMessage = "Zaproszenie zostało wysłane"
                                                        showSuccessToast = true
                                                        showLinkDialog = false
                                                    }
                                                    .addOnFailureListener {
                                                        toastMessage = "Błąd podczas wysyłania zaproszenia"
                                                        showSuccessToast = true
                                                    }
                                            } else {
                                                toastMessage = "Nie znaleziono użytkownika"
                                                showSuccessToast = true
                                            }
                                        }
                                }
                            }
                        ) {
                            Text("Wyślij Zaproszenie")
                        }
                    }
                }
            }
        }
    }

    if (showSuccessToast) {
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        showSuccessToast = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsCard(
    title: String,
    count: Int,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3DD1C6),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Ilość pozycji: $count",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}