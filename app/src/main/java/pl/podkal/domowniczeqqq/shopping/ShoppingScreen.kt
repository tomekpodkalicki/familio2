package pl.podkal.domowniczeqqq.shopping

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import pl.podkal.domowniczeq.R
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
                "Śmietana" to "ml",
                "Jajka" to "szt.",
                "Masło" to "g"
            ),
            "Pieczywo" to mapOf(
                "Chleb" to "szt.",
                "Bułki" to "szt.",
                "Bagietka" to "szt."
            ),
            "Warzywa i Owoce" to mapOf(
                "Jabłka" to "kg",
                "Pomidory" to "kg",
                "Ogórki" to "kg",
                "Banany" to "kg",
                "Ziemniaki" to "kg",
                "Cebula" to "kg",
                "Marchew" to "kg"
            ),
            "Mięso" to mapOf(
                "Kurczak" to "kg",
                "Mielone" to "kg",
                "Szynka" to "kg",
                "Kiełbasa" to "kg"
            ),
            "Napoje" to mapOf(
                "Woda" to "l",
                "Sok" to "l",
                "Cola" to "l",
                "Piwo" to "szt."
            ),
            "Chemia" to mapOf(
                "Mydło" to "szt.",
                "Proszek" to "kg",
                "Płyn do naczyń" to "ml",
                "Papier toaletowy" to "szt.",
                "Szampon" to "ml"
            ),
            "Przekąski" to mapOf(
                "Chipsy" to "szt.",
                "Ciastka" to "szt.",
                "Orzeszki" to "g",
                "Paluszki" to "szt."
            ),
            "Inne" to "szt."
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
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3DD1C6)
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Moja Lista Zakupów",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            )
        },
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
                        ShoppingItemRow(
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
                            },
                            db = db
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            if (showAddDialog) {
                AddEditShoppingItemDialog(
                    shoppingItem = selectedItem,
                    onDismiss = {
                        showAddDialog = false
                        selectedItem = null
                    },
                    onSave = { newItem ->
                        if (selectedItem != null) {
                            db.collection("shopping_items").document(selectedItem!!.id)
                                .set(newItem.copy(userId = userId))
                            toastMessage = "Produkt został zaktualizowany"
                            showSuccessToast = true
                        } else {
                            db.collection("shopping_items")
                                .add(newItem.copy(userId = userId))
                                .addOnSuccessListener { documentReference ->
                                    //Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                                }
                            toastMessage = "Dodano nowy produkt"
                            showSuccessToast = true
                        }
                        showAddDialog = false
                        selectedItem = null
                    }
                )
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    shoppingItem: ShoppingItem,
    onClick: () -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    db: FirebaseFirestore
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
            Text(
                text = shoppingItem.name,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val newQuantity = (shoppingItem.quantity - 1).coerceAtLeast(1.0)
                    if (newQuantity >= 1) {
                        db.collection("shopping_items")
                            .document(shoppingItem.id)
                            .update("quantity", newQuantity)
                    }
                },
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(0xFFEA4335).copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .padding(4.dp)
            ) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFEA4335)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "%.0f".format(shoppingItem.quantity),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val newQuantity = shoppingItem.quantity + 1
                    if (newQuantity >= 1) {
                        db.collection("shopping_items")
                            .document(shoppingItem.id)
                            .update("quantity", newQuantity)
                    }
                },
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .padding(4.dp)
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF4CAF50)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = { onDelete(shoppingItem) },
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
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