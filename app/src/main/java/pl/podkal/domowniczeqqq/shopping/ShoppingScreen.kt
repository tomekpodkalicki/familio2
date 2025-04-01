package pl.podkal.domowniczeqqq.shopping

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import pl.podkal.domowniczeqqq.navigation.BottomNavBar

data class ShoppingItem(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "szt.",
    val category: String = "",
    val isChecked: Boolean = false
) {
    companion object {
        val CATEGORIES = mapOf(
            "Nabiał" to listOf("Mleko", "Jajka", "Ser", "Jogurt", "Śmietana"),
            "Pieczywo" to listOf("Chleb", "Bułki", "Bagietka"),
            "Warzywa i Owoce" to listOf("Jabłka", "Pomidory", "Ogórki", "Banany"),
            "Mięso" to listOf("Kurczak", "Mielone", "Szynka"),
            "Napoje" to listOf("Woda", "Sok", "Cola"),
            "Chemia" to listOf("Mydło", "Proszek", "Płyn do naczyń", "Papier toaletowy"),
            "Przekąski" to listOf("Chipsy", "Ciastka", "Orzeszki"),
            "Inne" to listOf()
        )

        val DEFAULT_UNITS = mapOf(
            "Nabiał" to mapOf(
                "Mleko" to "l",
                "Jogurt" to "szt.",
                "Ser" to "kg",
                "Jajka" to "szt."
            ),
            "Warzywa i Owoce" to "kg",
            "Mięso" to "kg",
            "Napoje" to "l",
            "Chemia" to "szt.",
            "Przekąski" to "szt."
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(navController: NavController) {
    // Kolory używane w widoku
    val appBarColor = Color(0xFF3DD1C6)
    val backgroundColor = Color(0xFFF8F8F8)

    var shoppingItems by remember { mutableStateOf(listOf<ShoppingItem>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var showSuccessToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = Firebase.auth.currentUser?.uid.orEmpty()

    DisposableEffect(userId) {
        if (userId.isBlank()) {
            shoppingItems = emptyList()
            return@DisposableEffect onDispose {}
        }
        val registration = db.collection("shopping_items")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                shoppingItems = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ShoppingItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
        onDispose { registration.remove() }
    }

    if (showSuccessToast) {
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        showSuccessToast = false
    }

    Scaffold(
        // Dodany modyfikator, by dolne insets (np. navigationBar) były uwzględnione
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = appBarColor,
                icon = {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj produkt")
                },
                text = { Text("Dodaj produkt") }
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Szukaj produktów...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Szukaj") },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                            }
                        }
                    } else null
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val filteredItems = shoppingItems.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }
                    items(filteredItems) { item ->
                        ShoppingItemCard(
                            shoppingItem = item,
                            onClick = {
                                selectedItem = item
                                showAddDialog = true
                            },
                            onDelete = { itemToDelete ->
                                db.collection("shopping_items")
                                    .document(itemToDelete.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        toastMessage = "Produkt usunięty"
                                        showSuccessToast = true
                                    }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    selectedItem = null
                    showAddDialog = true
                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd),
                containerColor = Color(0xFF3DD1C6)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj produkt",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ShoppingItemCard(
    shoppingItem: ShoppingItem,
    onClick: () -> Unit,
    onDelete: (ShoppingItem) -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3DD1C6).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = shoppingItem.name.take(1).uppercase(),
                    color = Color(0xFF3DD1C6),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = shoppingItem.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${shoppingItem.quantity} ${shoppingItem.unit}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            IconButton(
                onClick = { onDelete(shoppingItem) }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń produkt",
                    tint = Color(0xFFEA4335)
                )
            }
        }
    }
}