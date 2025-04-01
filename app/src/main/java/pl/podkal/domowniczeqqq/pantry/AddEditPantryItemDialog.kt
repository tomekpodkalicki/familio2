package pl.podkal.domowniczeqqq.pantry

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPantryItemDialog(
    pantryItem: PantryItem?,
    onDismiss: () -> Unit,
    onSave: (PantryItem) -> Unit,
    currentLocation: String = "Spiżarnia"
) {
    val isEditMode = pantryItem != null
    val context = LocalContext.current

    var name by remember { mutableStateOf(pantryItem?.name ?: "") }
    var category by remember { mutableStateOf(pantryItem?.category ?: "") }
    // Location is now fixed to Spiżarnia - we get it from the parent screen
    var location by remember { mutableStateOf(pantryItem?.location ?: currentLocation) }
    var quantity by remember { mutableStateOf(pantryItem?.quantity?.toInt()?.toString() ?: "1") }
    var unit by remember { mutableStateOf(pantryItem?.unit ?: "szt.") }
    var expiryDate by remember { mutableStateOf(pantryItem?.expiryDate) }
    var purchaseDate by remember { mutableStateOf(pantryItem?.purchaseDate) }
    var price by remember { mutableStateOf(pantryItem?.price?.toString() ?: "") }

    // Dropdown states
    var expandedCategoryDropdown by remember { mutableStateOf(false) }
    var expandedNameDropdown by remember { mutableStateOf(false) }
    var expandedUnitDropdown by remember { mutableStateOf(false) }
    var expandedQuantityDropdown by remember { mutableStateOf(false) }

    // Validation states
    var nameError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }

    // Get available categories and product names
    val categories = remember { PantryItem.CATEGORIES.keys.toList() }
    val productSuggestions = remember(category) {
        if (category.isNotBlank() && PantryItem.CATEGORIES.containsKey(category)) {
            PantryItem.CATEGORIES[category] ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Update unit when category changes
    LaunchedEffect(category) {
        if (category.isNotBlank() && PantryItem.DEFAULT_UNITS.containsKey(category)) {
            unit = PantryItem.DEFAULT_UNITS[category] ?: "szt."
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEditMode) "Edytuj produkt" else "Dodaj nowy produkt",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategoryDropdown,
                    onExpandedChange = { expandedCategoryDropdown = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategoria*") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCategoryDropdown,
                        onDismissRequest = { expandedCategoryDropdown = false }
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    expandedCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Name field with dropdown suggestions
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = it.isEmpty()
                        },
                        label = { Text("Nazwa*") },
                        isError = nameError,
                        supportingText = { if (nameError) Text("Nazwa jest wymagana") },
                        trailingIcon = {
                            if (productSuggestions.isNotEmpty()) {
                                IconButton(onClick = { expandedNameDropdown = !expandedNameDropdown }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Pokaż sugestie"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && productSuggestions.isNotEmpty()) {
                                    expandedNameDropdown = true
                                }
                            }
                    )

                    DropdownMenu(
                        expanded = expandedNameDropdown && productSuggestions.isNotEmpty(),
                        onDismissRequest = { expandedNameDropdown = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        productSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    name = suggestion
                                    expandedNameDropdown = false
                                    // Update unit based on product name
                                    PantryItem.DEFAULT_UNITS[suggestion]?.let { defaultUnit ->
                                        unit = defaultUnit
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quantity section with +/- buttons and dropdown
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Ilość",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minus button
                        IconButton(
                            onClick = {
                                val currentQuantity = quantity.toIntOrNull() ?: 1
                                if (currentQuantity > 1) {
                                    quantity = (currentQuantity - 1).toString()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.small
                                )
                        ) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF3DD1C6)
                            )
                        }

                        // Quantity value with dropdown
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = quantity,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .clickable { expandedQuantityDropdown = true }
                                    .padding(8.dp)
                            )

                            DropdownMenu(
                                expanded = expandedQuantityDropdown,
                                onDismissRequest = { expandedQuantityDropdown = false }
                            ) {
                                // Add options 1-10
                                (1..10).forEach { num ->
                                    DropdownMenuItem(
                                        text = { Text(num.toString()) },
                                        onClick = {
                                            quantity = num.toString()
                                            expandedQuantityDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Plus button
                        IconButton(
                            onClick = {
                                val currentQuantity = quantity.toIntOrNull() ?: 1
                                quantity = (currentQuantity + 1).toString()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.small
                                )
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF3DD1C6)
                            )
                        }
                    }

                    // Unit selector
                    Text(
                        text = "Jednostka",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("szt.", "l", "kg", "ml", "g").forEach { unitOption ->
                            val isSelected = unit == unitOption

                            Surface(
                                color = if (isSelected) Color(0xFF3DD1C6) else Color.Transparent,
                                shape = MaterialTheme.shapes.small,
                                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray) else null,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clickable { unit = unitOption }
                            ) {
                                Text(
                                    text = unitOption,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Expiry date picker
                DatePickerField(
                    label = "Data ważności",
                    date = expiryDate,
                    onDateSelected = { expiryDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Purchase date picker
                DatePickerField(
                    label = "Data zakupu",
                    date = purchaseDate,
                    onDateSelected = { purchaseDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Cena (zł)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isEmpty()) {
                                nameError = true
                                return@Button
                            }

                            if (category.isEmpty()) {
                                category = "Inne"
                            }

                            val validQuantity = try {
                                quantity.toIntOrNull()?.toDouble() ?: 0.0
                            } catch (e: Exception) {
                                0.0
                            }

                            if (validQuantity <= 0) {
                                quantityError = true
                                return@Button
                            }

                            val validPrice = try {
                                price.toDoubleOrNull()
                            } catch (e: Exception) {
                                null
                            }

                            val newItem = PantryItem(
                                id = pantryItem?.id ?: "",
                                userId = Firebase.auth.currentUser?.uid ?: "",
                                groupId = pantryItem?.groupId ?: Firebase.auth.currentUser?.uid ?: "",
                                name = name,
                                category = category,
                                location = location,
                                quantity = validQuantity,
                                unit = unit.ifEmpty { "szt." },
                                expiryDate = expiryDate,
                                purchaseDate = purchaseDate,
                                price = validPrice
                            )

                            onSave(newItem)
                        }
                    ) {
                        Text(if (isEditMode) "Zaktualizuj" else "Dodaj")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    date: Long?,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formattedDate = formatDate(date)
    val calendar = Calendar.getInstance()
    date?.let { calendar.timeInMillis = it }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            onDateSelected(calendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    OutlinedTextField(
        value = formattedDate ?: "",
        onValueChange = { /* Read only */ },
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            Row {
                if (date != null) {
                    IconButton(onClick = { onDateSelected(0) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Wyczyść datę"
                        )
                    }
                }
                IconButton(onClick = { datePickerDialog.show() }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Wybierz datę"
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun formatDate(timestamp: Long?): String? {
    if (timestamp == null || timestamp <= 0) return null

    val date = Date(timestamp)
    val format = SimpleDateFormat("dd.MM.yyyy", Locale("pl"))
    return format.format(date)
}