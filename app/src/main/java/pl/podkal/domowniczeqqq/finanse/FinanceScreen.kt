package pl.podkal.domowniczeqqq.finance

import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import pl.podkal.domowniczeq.R
import pl.podkal.domowniczeqqq.navigation.BottomNavBar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LinkedAccounts(val users: List<String> = emptyList())

// Define a state class to hold all relevant finance data and state
class FinanceState {
    val transactions = mutableStateListOf<Transaction>()
    var filterCategory by mutableStateOf<String?>(null)
    var filterStartDate by mutableStateOf<Date?>(null)
    var filterEndDate by mutableStateOf<Date?>(null)
    var showAddDialog by mutableStateOf(false)
    var showFilterDialog by mutableStateOf(false)
    var title by mutableStateOf("")
    var amount by mutableStateOf("")
    var isExpense by mutableStateOf(true)
    var category by mutableStateOf("")
    var transactionDate by mutableStateOf(Date())
    var selectedColor by mutableStateOf(Color(0xFF4CAF50))
    var refreshCounter by mutableStateOf(0)

    fun resetForm() {
        title = ""
        amount = ""
        isExpense = true
        category = ""
        transactionDate = Date()
        // Use default color for the expense type from CategoryInfo
        selectedColor = CategoryInfo.getColor("", true)
    }

    fun clearFilters() {
        filterCategory = null
        filterStartDate = null
        filterEndDate = null
        refreshCounter++
    }

    /**
     * Improved transaction deletion with immediate UI update
     */
    fun deleteTransaction(transaction: Transaction, db: FirebaseFirestore, context: Context) {
        // First remove it from the local list to update UI immediately
        val localIndex = transactions.indexOfFirst { it.id == transaction.id }
        val removedTransaction = if (localIndex >= 0) transactions.removeAt(localIndex) else null

        // Force refresh UI
        refreshCounter++

        // Then delete from database
        db.collection("transactions").document(transaction.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Transakcja usunięta", Toast.LENGTH_SHORT).show()
                // No need to refresh again - already updated the UI
            }
            .addOnFailureListener { e ->
                // Put the transaction back if deletion failed
                if (removedTransaction != null) {
                    if (localIndex >= 0 && localIndex < transactions.size) {
                        transactions.add(localIndex, removedTransaction)
                    } else {
                        transactions.add(removedTransaction)
                    }
                    // Force refresh UI again
                    refreshCounter++
                }
                Toast.makeText(context, "Błąd podczas usuwania: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = Firebase.auth.currentUser
    val userId = currentUser?.uid
    val context = LocalContext.current

    // Create a single state object for the entire screen
    val state = remember { FinanceState() }

    // Load transactions using DisposableEffect for immediate updates
    DisposableEffect(userId, state.filterCategory, state.filterStartDate, state.filterEndDate, state.refreshCounter) {
        Log.d("FinanceScreen", "DisposableEffect triggered with refreshCounter=${state.refreshCounter}")

        // Function to load transactions
        val loadTransactions = {
            if (userId == null) {
                state.transactions.clear()
            } else {
                // First get linked accounts
                db.collection("linked_accounts")
                    .whereArrayContains("users", userId)
                    .get()
                    .addOnSuccessListener { linkedSnapshot ->
                        val userIds = linkedSnapshot.documents.flatMap { doc ->
                            doc.toObject(LinkedAccounts::class.java)?.users ?: emptyList()
                        }.distinct()

                        // Add current user if not in the list
                        val allUserIds = (userIds + userId).distinct()

                        // Always perform a fresh fetch from Firestore
                        var query = db.collection("transactions")
                            .whereIn("userId", allUserIds)

                        // Apply category filter if set
                        if (state.filterCategory != null) {
                            query = query.whereEqualTo("category", state.filterCategory)
                        }

                        // Execute fetch
                        query.get()
                            .addOnSuccessListener { snapshot ->
                                val fetchedTransactions = snapshot.documents.mapNotNull { document ->
                                    try {
                                        val id = document.id
                                        val title = document.getString("title") ?: ""
                                        val amount = document.getDouble("amount") ?: 0.0
                                        val isExpense = document.getBoolean("isExpense") ?: true
                                        val category = document.getString("category") ?: ""
                                        val date = document.getDate("date") ?: Date()
                                        val colorInt = document.getLong("color")?.toInt() ?: Color.Gray.toArgb()

                                        Transaction(
                                            id = id,
                                            title = title,
                                            amount = amount,
                                            isExpense = isExpense,
                                            category = category,
                                            date = date,
                                            color = Color(colorInt)
                                        )
                                    } catch (e: Exception) {
                                        Log.e("FinanceScreen", "Error parsing transaction: ${e.message}")
                                        null
                                    }
                                }

                                // Apply date filtering in memory
                                val filteredTransactions = fetchedTransactions.filter { transaction ->
                                    (state.filterStartDate == null || transaction.date >= state.filterStartDate) &&
                                            (state.filterEndDate == null || transaction.date <= state.filterEndDate)
                                }

                                // Update state with filtered transactions
                                state.transactions.clear()
                                state.transactions.addAll(filteredTransactions)

                                Log.d("FinanceScreen", "Successfully loaded ${filteredTransactions.size} transactions")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FinanceScreen", "Error fetching transactions: ${e.message}")
                                Toast.makeText(context, "Błąd ładowania transakcji: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FinanceScreen", "Error fetching linked accounts: ${exception.message}")
                        Toast.makeText(context, "Błąd pobierania powiązanych kont", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Load transactions immediately
        loadTransactions()

        // Return a cleanup that does nothing
        onDispose { }
    }

    // Calculate total balance
    val totalBalance = state.transactions.sumOf {
        if (it.isExpense) -it.amount else it.amount
    }
    val totalIncome = state.transactions.filter { !it.isExpense }.sumOf { it.amount }
    val totalExpenses = state.transactions.filter { it.isExpense }.sumOf { it.amount }

    // Format numbers
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pl", "PL"))
    val formattedBalance = currencyFormat.format(totalBalance)
    val formattedIncome = currencyFormat.format(totalIncome)
    val formattedExpenses = currencyFormat.format(totalExpenses)

    // Get available categories directly from CategoryInfo
    val expenseCategories = CategoryInfo.getCategories(true).map { it.replaceFirstChar { it.uppercase() } } + listOf("Inne")
    val incomeCategories = CategoryInfo.getCategories(false).map { it.replaceFirstChar { it.uppercase() } } + listOf("Inne")

    // Date formatter
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale("pl"))

    // Disable back button
    BackHandler { /* Ignore back press */ }

    val appBarColor = Color(0xFF3DD1C6) // Turkusowy (primary)
    val backgroundColor = Color(0xFFF8F8F8) // Jasnoszary (background)
    var showPieCharts by remember { mutableStateOf(true) }


    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarColor
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.FillHeight
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Moje Finanse",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    // Toggle pie charts button
                    IconButton(onClick = { showPieCharts = !showPieCharts }) {
                        Icon(
                            imageVector = if (showPieCharts) Icons.Default.PieChart else Icons.Default.BarChart,
                            contentDescription = "Przełącz widok wykresów"
                        )
                    }
                    // Filter button
                    IconButton(onClick = { state.showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtruj"
                        )
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Reset form values
                    state.resetForm()
                    state.showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.tertiary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj transakcję"
                )
            }
        },
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (userId == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Brak zalogowanego użytkownika")
                }
            } else {
                // Balance Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Saldo",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = formattedBalance,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Income and Expense Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Income side
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Przychody",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formattedIncome,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }

                            // Expenses side
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Wydatki",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formattedExpenses,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }

                // Charts section
                if (showPieCharts) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Expenses Chart with label
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            CategoryPieChart(
                                transactions = state.transactions,
                                isExpense = true,
                                totalAmount = totalExpenses,
                                modifier = Modifier.fillMaxWidth()
                            )
                            // "Expenses" label below chart
                            Text(
                                text = "Wydatki",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336), // Red for expenses
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Income Chart with label
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            CategoryPieChart(
                                transactions = state.transactions,
                                isExpense = false,
                                totalAmount = totalIncome,
                                modifier = Modifier.fillMaxWidth()
                            )
                            // "Income" label below chart
                            Text(
                                text = "Przychody",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50), // Green for income
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Active filters display
                if (state.filterCategory != null || state.filterStartDate != null || state.filterEndDate != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Aktywne filtry:",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    if (state.filterCategory != null) {
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text("Kategoria: ${state.filterCategory}") },
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }

                                    if (state.filterStartDate != null) {
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text("Od: ${dateFormatter.format(state.filterStartDate!!)}") },
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }

                                    if (state.filterEndDate != null) {
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text("Do: ${dateFormatter.format(state.filterEndDate!!)}") },
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { state.clearFilters() }
                                ) {
                                    Text("Wyczyść")
                                }
                            }
                        }
                    }
                }

                // Transactions Header
                Text(
                    text = "Ostatnie transakcje",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Transactions list
                if (state.transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak transakcji",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    state.transactions.sortedByDescending { it.date }.forEach { transaction ->
                        TransactionItem(transaction = transaction, state = state, db = db, context = context)
                    }
                }

                // Add some space at the bottom
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Add Transaction Dialog
        if (state.showAddDialog && userId != null) {
            AddTransactionDialog(
                state = state,
                onDismiss = { state.showAddDialog = false },
                onAddTransaction = { title, amount, isExpense, category, date, color ->
                    // Create transaction data
                    val transactionData = hashMapOf(
                        "title" to title,
                        "amount" to amount,
                        "isExpense" to isExpense,
                        "category" to category,
                        "date" to date,
                        "color" to color.toArgb(),
                        "userId" to userId
                    )

                    // Create a transaction object without an ID
                    val temporaryTransaction = Transaction(
                        id = "temp_" + System.currentTimeMillis(),
                        title = title,
                        amount = amount,
                        isExpense = isExpense,
                        category = category,
                        date = date,
                        color = color,
                        userId = userId
                    )

                    // Add to local state first for immediate UI update
                    state.transactions.add(temporaryTransaction)

                    // Update UI
                    state.refreshCounter++

                    // Add to Firestore
                    db.collection("transactions")
                        .add(transactionData)
                        .addOnSuccessListener { documentReference ->
                            Toast.makeText(context, "Transakcja dodana", Toast.LENGTH_SHORT).show()

                            // Replace temporary item with the real one that has proper ID
                            val index = state.transactions.indexOfFirst { it.id == temporaryTransaction.id }
                            if (index >= 0) {
                                state.transactions[index] = temporaryTransaction.copy(id = documentReference.id)
                            }

                            // Update UI
                            state.refreshCounter++
                        }
                        .addOnFailureListener { e ->
                            // Remove the temporary item if it failed
                            state.transactions.removeAll { it.id == temporaryTransaction.id }

                            // Update UI
                            state.refreshCounter++

                            Toast.makeText(context, "Błąd podczas dodawania: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                expenseCategories = expenseCategories,
                incomeCategories = incomeCategories,
                colorOptions = listOf() // Not used anymore as colors are automatically assigned
            )
        }

        // Filter Dialog
        if (state.showFilterDialog) {
            FilterDialog(
                state = state,
                onDismiss = { state.showFilterDialog = false },
                onApplyFilters = { category, startDate, endDate ->
                    state.filterCategory = category
                    state.filterStartDate = startDate
                    state.filterEndDate = endDate
                    state.showFilterDialog = false
                    // The DisposableEffect will handle the refreshing
                },
                expenseCategories = expenseCategories,
                incomeCategories = incomeCategories
            )
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, state: FinanceState, db: FirebaseFirestore, context: Context) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pl", "PL"))
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale("pl"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category color indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(transaction.color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Display category icon instead of first letter
                Icon(
                    imageVector = CategoryInfo.getIcon(transaction.category, transaction.isExpense),
                    contentDescription = transaction.category,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Transaction details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${transaction.category} • ${dateFormatter.format(transaction.date)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Amount
            Text(
                text = currencyFormat.format(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isExpense) Color(0xFFF44336) else Color(0xFF4CAF50)
            )

            IconButton(onClick = { state.deleteTransaction(transaction, db, context) }) {
                Icon(painter = painterResource(id = R.drawable.ic_delete), contentDescription = "Usuń")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    state: FinanceState,
    onDismiss: () -> Unit,
    onAddTransaction: (String, Double, Boolean, String, Date, Color) -> Unit,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    colorOptions: List<Color> // Not used anymore, but kept for backward compatibility
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale("pl"))

    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj transakcję") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Title field
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { state.title = it },
                    label = { Text("Nazwa") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                // Amount field
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { state.amount = it },
                    label = { Text("Kwota") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                // Type selector (Income/Expense)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Button(
                        onClick = {
                            state.isExpense = false
                            // Update color based on new expense state and current category
                            if (state.category.isNotBlank()) {
                                state.selectedColor = CategoryInfo.getColor(state.category, false)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!state.isExpense) Color(0xFF4CAF50) else Color.Gray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Przychód")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            state.isExpense = true
                            // Update color based on new expense state and current category
                            if (state.category.isNotBlank()) {
                                state.selectedColor = CategoryInfo.getColor(state.category, true)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isExpense) Color(0xFFF44336) else Color.Gray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Wydatek")
                    }
                }

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = state.category,
                        onValueChange = { state.category = it },
                        readOnly = true,
                        label = { Text("Kategoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        val categories = if (state.isExpense) expenseCategories else incomeCategories
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    state.category = category
                                    // Automatically set color based on category
                                    state.selectedColor = CategoryInfo.getColor(category, state.isExpense)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            val calendar = Calendar.getInstance()
                            calendar.time = state.transactionDate
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    state.transactionDate = calendar.time
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Wybierz datę",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Data: ${dateFormatter.format(state.transactionDate)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Category color preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Display category color and icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(state.selectedColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.category.isNotBlank()) {
                            val icon = CategoryInfo.getIcon(state.category, state.isExpense)
                            Icon(
                                imageVector = icon,
                                contentDescription = state.category,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Add text explanation
                    Text(
                        text = "Kolor kategorii zostanie ustawiony automatycznie",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = state.amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (state.title.isBlank() || amountValue <= 0 || state.category.isBlank()) {
                        Toast.makeText(context, "Wypełnij wszystkie pola poprawnie", Toast.LENGTH_SHORT).show()
                    } else {
                        onAddTransaction(
                            state.title,
                            amountValue,
                            state.isExpense,
                            state.category,
                            state.transactionDate,
                            state.selectedColor
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
        properties = DialogProperties(dismissOnClickOutside = true)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    state: FinanceState,
    onDismiss: () -> Unit,
    onApplyFilters: (String?, Date?, Date?) -> Unit,
    expenseCategories: List<String>,
    incomeCategories: List<String>
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale("pl"))

    var filterCategory by remember { mutableStateOf(state.filterCategory) }
    var filterStartDate by remember { mutableStateOf(state.filterStartDate) }
    var filterEndDate by remember { mutableStateOf(state.filterEndDate) }

    var categoryExpanded by remember { mutableStateOf(false) }
    val allCategories = remember { (expenseCategories + incomeCategories).distinct() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtruj transakcje") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 400.dp)
            ) {
                // Category filter
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = filterCategory ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Kategoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        // Add "All" option
                        DropdownMenuItem(
                            text = { Text("Wszystkie") },
                            onClick = {
                                filterCategory = null
                                categoryExpanded = false
                            }
                        )

                        // Add all categories
                        allCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    filterCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Start date picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            val calendar = Calendar.getInstance()
                            if (filterStartDate != null) calendar.time = filterStartDate!!
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    filterStartDate = calendar.time
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Wybierz datę początkową",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = if (filterStartDate != null) "Od: ${dateFormatter.format(filterStartDate!!)}" else "Od: (brak)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (filterStartDate != null) {
                    TextButton(
                        onClick = { filterStartDate = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Wyczyść")
                    }
                }

                // End date picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            val calendar = Calendar.getInstance()
                            if (filterEndDate != null) calendar.time = filterEndDate!!
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    filterEndDate = calendar.time
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Wybierz datę końcową",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = if (filterEndDate != null) "Do: ${dateFormatter.format(filterEndDate!!)}" else "Do: (brak)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (filterEndDate != null) {
                    TextButton(
                        onClick = { filterEndDate = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Wyczyść")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApplyFilters(filterCategory, filterStartDate, filterEndDate)
                }
            ) {
                Text("Zastosuj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
        properties = DialogProperties(dismissOnClickOutside = true)
    )
}