package pl.podkal.domowniczeqqq.shopping

/* wszystkie potrzebne importy */
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

// --------------------  MODEL  --------------------

data class ShoppingItem(
    val id: String = "",
    val userId: String = "",
    val groupId: String = "",
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

/* ------------------------------------------------------------------
 *  SHOPPING SCREEN – listener 1 × whereIn  (bez znikania elementów)
 * ------------------------------------------------------------------*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(navController: NavController) {

    // ---------- kolory ----------
    val appBarColor      = Color(0xFF3DD1C6)
    val backgroundColor  = Color(0xFFF8F8F8)

    // ---------- stany UI ----------
    var shoppingItems by remember { mutableStateOf(listOf<ShoppingItem>()) }
    var searchQuery   by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem  by remember { mutableStateOf<ShoppingItem?>(null) }
    var toastMessage  by remember { mutableStateOf("") }
    var showToast     by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db      = FirebaseFirestore.getInstance()
    val userId  = Firebase.auth.currentUser?.uid.orEmpty()

    /* ----------------------------------------------------------------
     *  1️⃣  słuchacz linked_accounts  →  lista groupIds
     * ---------------------------------------------------------------- */
    var groupIds by remember { mutableStateOf<List<String>>(emptyList()) }

    DisposableEffect(userId) {
        if (userId.isBlank()) return@DisposableEffect onDispose { }
        val reg = db.collection("linked_accounts")
            .whereArrayContains("users", userId)
            .addSnapshotListener { snap, _ ->
                groupIds = snap?.documents?.map { it.id } ?: emptyList()
            }
        onDispose { reg.remove() }
    }

    /* ----------------------------------------------------------------
     * 2️⃣  wspólny słuchacz na shopping_items (userId + groupIds)
     * ---------------------------------------------------------------- */
    DisposableEffect(userId, groupIds) {
        if (userId.isBlank()) return@DisposableEffect onDispose { }
        val allGroups = listOf(userId) + groupIds
        val reg = db.collection("shopping_items")
            .whereIn("groupId", allGroups)
            .addSnapshotListener { snap, _ ->
                shoppingItems = snap?.documents?.mapNotNull { d ->
                    d.toObject(ShoppingItem::class.java)?.copy(id = d.id)
                } ?: emptyList()
            }
        onDispose { reg.remove() }
    }

    /* ---------- pojedynczy toast ---------- */
    if (showToast) { Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show(); showToast = false }

    /* -----------------------------  UI  ----------------------------- */
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarColor),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.logo), "Logo", Modifier.height(40.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Text("Moja Lista Zakupów", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true }, containerColor = appBarColor,
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Dodaj produkt") }
            )
        }, floatingActionButtonPosition = FabPosition.End
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            /* ---------------- Search ---------------- */
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton({ searchQuery = "" }) { Icon(Icons.Default.Clear, null) } }
                } else null,
                placeholder = { Text("Szukaj produktów…") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            /* ---------------- Lista ---------------- */
            LazyColumn(Modifier.weight(1f)) {
                val filtered = shoppingItems.filter { it.name.contains(searchQuery, true) }
                items(filtered, key = { it.id }) { item ->
                    ShoppingItemRow(
                        shoppingItem = item,
                        onClick = { selectedItem = item; showAddDialog = true },
                        onDelete = { del ->
                            db.collection("shopping_items").document(del.id).delete()
                            toastMessage = "Produkt usunięty"; showToast = true
                        }, db = db
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        /* ---------------- Dialog dodaj/edytuj ---------------- */
        if (showAddDialog) {
            AddEditShoppingItemDialog(
                shoppingItem = selectedItem,
                onDismiss = { showAddDialog = false; selectedItem = null },
                onSave = { newItem ->
                    if (selectedItem == null) {
                        db.collection("shopping_items").add(newItem.copy(userId = userId))
                        toastMessage = "Dodano produkt"; showToast = true
                    } else {
                        db.collection("shopping_items").document(selectedItem!!.id).set(newItem.copy(userId = userId))
                        toastMessage = "Zaktualizowano"; showToast = true
                    }
                    showAddDialog = false; selectedItem = null
                }
            )
        }
    }
}

/* --------------------  ROW  -------------------- */
@Composable
private fun ShoppingItemRow(
    shoppingItem: ShoppingItem,
    onClick: () -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    db: FirebaseFirestore
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF3DD1C6).copy(.2f)), contentAlignment = Alignment.Center) {
                Text(shoppingItem.name.first().uppercase(), color = Color(0xFF3DD1C6), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Text(shoppingItem.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            CounterButton("-", Color(0xFFEA4335)) {
                val q = (shoppingItem.quantity - 1).coerceAtLeast(1.0)
                db.collection("shopping_items").document(shoppingItem.id).update("quantity", q)
            }
            Text("%.0f".format(shoppingItem.quantity), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
            CounterButton("+", Color(0xFF4CAF50)) {
                db.collection("shopping_items").document(shoppingItem.id).update("quantity", shoppingItem.quantity + 1)
            }
            IconButton({ onDelete(shoppingItem) }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFEA4335)) }
        }
    }
}

@Composable
private fun CounterButton(txt: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(32.dp).background(tint.copy(.2f), CircleShape).padding(4.dp)) {
        Text(txt, style = MaterialTheme.typography.titleLarge, color = tint)
    }
}
