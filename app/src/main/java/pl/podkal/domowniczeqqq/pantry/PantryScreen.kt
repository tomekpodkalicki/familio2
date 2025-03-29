package pl.podkal.domowniczeqqq.pantry

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import pl.podkal.domowniczeq.R
import pl.podkal.domowniczeqqq.navigation.BottomNavBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(navController: NavController) {
    val appBarColor = Color(0xFF3DD1C6)
    val backgroundColor = Color(0xFFF8F8F8)

    var pantryItems by remember { mutableStateOf(listOf<PantryItem>()) }
    var isGridView by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<PantryItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var currentSort by remember { mutableStateOf(SortOption.NAME) }
    var currentFilter by remember { mutableStateOf<String?>(null) }
    var currentLocation by remember { mutableStateOf("Spiżarnia") } // Default location is Pantry
    var showSuccessToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = Firebase.auth.currentUser?.uid.orEmpty()

    // Setup the firestore listener
    DisposableEffect(userId) {
        if (userId.isBlank()) {
            pantryItems = emptyList()
            return@DisposableEffect onDispose {}
        }

        val registration = db.collection("pantry_items")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val temp = mutableListOf<PantryItem>()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(PantryItem::class.java)?.let {
                        val item = it.copy(id = doc.id)
                        temp.add(item)
                    }
                }
                pantryItems = temp
            }

        onDispose {
            registration.remove()
        }
    }

    // Handle success toast
    LaunchedEffect(showSuccessToast) {
        if (showSuccessToast) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(3000)
            showSuccessToast = false
        }
    }

    val categories = pantryItems.mapNotNull { it.category }.distinct().sorted()
    val locations = pantryItems.map { it.location }.distinct().sorted()

    val filteredItems = pantryItems.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = currentFilter == null || item.category == currentFilter
        val matchesLocation = item.location == currentLocation // Fixed to current location
        matchesSearch && matchesCategory && matchesLocation
    }

    val sortedItems = when (currentSort) {
        SortOption.NAME -> filteredItems.sortedBy { it.name }
        SortOption.EXPIRY -> filteredItems.sortedBy { it.expiryDate }
        SortOption.QUANTITY -> filteredItems.sortedByDescending { it.quantity }
    }

    Scaffold(
        containerColor = backgroundColor,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Moja $currentLocation", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarColor
                ),
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Zmień widok",
                            tint = Color.White
                        )
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = !showSortMenu }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Sortuj",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            Text(
                                "Sortuj według:",
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.Bold
                            )

                            DropdownMenuItem(
                                text = { Text("Nazwy") },
                                onClick = {
                                    currentSort = SortOption.NAME
                                    showSortMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Daty ważności") },
                                onClick = {
                                    currentSort = SortOption.EXPIRY
                                    showSortMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Ilości") },
                                onClick = {
                                    currentSort = SortOption.QUANTITY
                                    showSortMenu = false
                                }
                            )

                            HorizontalDivider()

                            Text(
                                "Filtruj według kategorii:",
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.Bold
                            )

                            DropdownMenuItem(
                                text = { Text("Wszystkie kategorie") },
                                onClick = {
                                    currentFilter = null
                                    showSortMenu = false
                                }
                            )

                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        currentFilter = category
                                        showSortMenu = false
                                    }
                                )
                            }

                            // Location filtering has been removed to simplify interface
                        }
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = appBarColor,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj produkt",
                    tint = Color.White
                )
            }
        },

        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Location chips for navigating between different storage locations
            LocationChips(
                locations = listOf("Lodówka", "Spiżarnia", "Apteczka"),
                selectedLocation = currentLocation,
                onLocationSelected = { location ->
                    currentLocation = location
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // Category chips - only show for the current location
            if (categories.isNotEmpty()) {
                // Get categories for current location
                val locationCategories = pantryItems
                    .filter { it.location == currentLocation }
                    .mapNotNull { it.category }
                    .distinct()

                if (locationCategories.isNotEmpty()) {
                    CategoryChips(
                        categories = locationCategories,
                        selectedCategory = currentFilter,
                        onCategorySelected = { category ->
                            currentFilter = if (currentFilter == category) null else category
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            if (sortedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak produktów w lokalizacji $currentLocation.\nDodaj swój pierwszy produkt!",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sortedItems) { item ->
                            PantryItemGridCard(
                                pantryItem = item,
                                onClick = {
                                    selectedItem = item
                                    showAddDialog = true
                                },
                                onQuantityChange = { pantryItem, newQuantity ->
                                    db.collection("pantry_items").document(pantryItem.id)
                                        .update("quantity", newQuantity)
                                    toastMessage = "Zmieniono ilość produktu"
                                    showSuccessToast = true
                                },
                                onDelete = { pantryItem ->
                                    db.collection("pantry_items").document(pantryItem.id)
                                        .delete()
                                    toastMessage = "Usunięto produkt"
                                    showSuccessToast = true
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sortedItems) { item ->
                            PantryItemCard(
                                pantryItem = item,
                                onClick = {
                                    selectedItem = item
                                    showAddDialog = true
                                },
                                onQuantityChange = { pantryItem, newQuantity ->
                                    db.collection("pantry_items").document(pantryItem.id)
                                        .update("quantity", newQuantity)
                                    toastMessage = "Zmieniono ilość produktu"
                                    showSuccessToast = true
                                },
                                onDelete = { pantryItem ->
                                    db.collection("pantry_items").document(pantryItem.id)
                                        .delete()
                                    toastMessage = "Usunięto produkt"
                                    showSuccessToast = true
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddEditPantryItemDialog(
                pantryItem = selectedItem,
                currentLocation = currentLocation,
                onDismiss = {
                    showAddDialog = false
                    selectedItem = null
                },
                onSave = { newItem ->
                    if (selectedItem != null) {
                        // Update existing item
                        db.collection("pantry_items").document(selectedItem!!.id)
                            .set(newItem.copy(userId = userId))
                        toastMessage = "Produkt został zaktualizowany"
                        showSuccessToast = true
                    } else {
                        // Check for duplicate items by name
                        val existingItem = pantryItems.find {
                            it.name.equals(newItem.name, ignoreCase = true) &&
                                    it.location == newItem.location
                        }

                        if (existingItem != null) {
                            // Update quantity of existing item
                            val updatedQuantity = existingItem.quantity + newItem.quantity
                            db.collection("pantry_items").document(existingItem.id)
                                .update("quantity", updatedQuantity)
                            toastMessage = "Zaktualizowano ilość istniejącego produktu"
                            showSuccessToast = true
                        } else {
                            // Add new item
                            db.collection("pantry_items")
                                .add(newItem.copy(userId = userId))
                            toastMessage = "Dodano nowy produkt"
                            showSuccessToast = true
                        }
                    }
                    showAddDialog = false
                    selectedItem = null
                }
            )
        }
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Szukaj produktów...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Szukaj"
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Wyczyść"
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier
    )
}

@Composable
fun CategoryChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(bottom = 8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory

            Surface(
                color = if (isSelected) Color(0xFF3DD1C6) else Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .border(
                        width = 1.dp,
                        color = Color(0xFF3DD1C6),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onCategorySelected(category) }
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color.White else Color(0xFF3DD1C6),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun PantryItemCard(
    pantryItem: PantryItem,
    onClick: () -> Unit,
    onQuantityChange: ((PantryItem, Double) -> Unit)? = null,
    onDelete: ((PantryItem) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3DD1C6).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pantryItem.name.take(1).uppercase(),
                    color = Color(0xFF3DD1C6),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = pantryItem.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    pantryItem.category?.let { category ->
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Text(
                        text = pantryItem.location,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minus button
                    if (onQuantityChange != null) {
                        IconButton(
                            onClick = {
                                if (pantryItem.quantity > 1) {
                                    onQuantityChange(pantryItem, pantryItem.quantity - 1)
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Zmniejsz ilość",
                                tint = Color(0xFFEA4335), // Red color for minus
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "${pantryItem.quantity.toInt()} ${pantryItem.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Plus button
                    if (onQuantityChange != null) {
                        IconButton(
                            onClick = {
                                onQuantityChange(pantryItem, pantryItem.quantity + 1)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Zwiększ ilość",
                                tint = Color(0xFF34A853), // Green color for plus
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Delete/Basket icon
                    if (onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(pantryItem)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Usuń produkt",
                                tint = Color(0xFF3DD1C6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                pantryItem.expiryDate?.let { expiryDate ->
                    formatDate(expiryDate)?.let { formattedDate ->
                        Text(
                            text = "Ważne do: $formattedDate",
                            fontSize = 12.sp,
                            color = if (isExpiryClose(expiryDate)) Color.Red else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PantryItemGridCard(
    pantryItem: PantryItem,
    onClick: () -> Unit,
    onQuantityChange: ((PantryItem, Double) -> Unit)? = null,
    onDelete: ((PantryItem) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3DD1C6).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pantryItem.name.take(1).uppercase(),
                    color = Color(0xFF3DD1C6),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pantryItem.name,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                pantryItem.category?.let { category ->
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = pantryItem.location,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quantity with controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Minus button
                if (onQuantityChange != null) {
                    IconButton(
                        onClick = {
                            if (pantryItem.quantity > 1) {
                                onQuantityChange(pantryItem, pantryItem.quantity - 1)
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zmniejsz ilość",
                            tint = Color(0xFFEA4335), // Red color for minus
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "${pantryItem.quantity.toInt()} ${pantryItem.unit}",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Plus button
                if (onQuantityChange != null) {
                    IconButton(
                        onClick = {
                            onQuantityChange(pantryItem, pantryItem.quantity + 1)
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zwiększ ilość",
                            tint = Color(0xFF34A853), // Green color for plus
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Delete/Basket icon
            if (onDelete != null) {
                IconButton(
                    onClick = {
                        onDelete(pantryItem)
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Usuń produkt",
                        tint = Color(0xFF3DD1C6),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            pantryItem.expiryDate?.let { expiryDate ->
                formatDate(expiryDate)?.let { formattedDate ->
                    Text(
                        text = "Ważne do: $formattedDate",
                        fontSize = 12.sp,
                        color = if (isExpiryClose(expiryDate)) Color.Red else Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun formatDate(timestamp: Long): String? {
    if (timestamp <= 0) return null

    val date = Date(timestamp)
    val format = SimpleDateFormat("dd.MM.yyyy", Locale("pl"))
    return format.format(date)
}

private fun isExpiryClose(timestamp: Long): Boolean {
    val expiryDate = Date(timestamp)
    val now = Date()
    val diff = expiryDate.time - now.time
    val daysUntilExpiry = diff / (24 * 60 * 60 * 1000)
    return daysUntilExpiry < 7 && daysUntilExpiry >= 0
}

@Composable
fun LocationChips(
    locations: List<String>,
    selectedLocation: String?,
    onLocationSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Define colors for each location
    val locationColors = mapOf(
        "Lodówka" to Color(0xFF4285F4),    // Blue for refrigerator
        "Spiżarnia" to Color(0xFFEA4335),  // Red for pantry
        "Apteczka" to Color(0xFF34A853)    // Green for first aid kit
    )

    Row(
        modifier = modifier.padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween // Distribute evenly
    ) {
        locations.forEach { location ->
            val isSelected = location == selectedLocation
            val locationColor = locationColors[location] ?: Color(0xFF5F6368) // Default gray if not found

            Surface(
                color = if (isSelected) locationColor else Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f) // Take equal space
                    .padding(horizontal = 4.dp)
                    .border(
                        width = 1.dp,
                        color = locationColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onLocationSelected(location) }
            ) {
                Text(
                    text = location,
                    color = if (isSelected) Color.White else locationColor,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center, // Center text
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .fillMaxWidth() // Make text take full width of surface
                )
            }
        }
    }
}

enum class SortOption {
    NAME, EXPIRY, QUANTITY
}