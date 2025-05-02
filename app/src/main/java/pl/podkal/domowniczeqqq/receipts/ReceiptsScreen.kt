package pl.podkal.domowniczeqqq.receipts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import pl.podkal.domowniczeq.R
import pl.podkal.domowniczeqqq.finance.CategoryInfo
import pl.podkal.domowniczeqqq.navigation.BottomNavBar
import pl.podkal.domowniczeqqq.pantry.PantryItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val currentUser = Firebase.auth.currentUser
    val userId = currentUser?.uid
    val receipts = remember { mutableStateListOf<Receipt>() }
    val scope = rememberCoroutineScope()
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    var showUploadOptions by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var addToFinances by remember { mutableStateOf(false) }
    var addToPantry by remember { mutableStateOf(false) }
    var selectedReceipt by remember { mutableStateOf<Receipt?>(null) }

    DisposableEffect(userId) {
        if (userId == null) {
            receipts.clear()
            return@DisposableEffect onDispose {}
        }
        val registration = db.collection("receipts")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ReceiptsScreen", "Listen failed.", error)
                    return@addSnapshotListener
                }
                receipts.clear()
                snapshot?.documents?.forEach { doc ->
                    val url = doc.getString("imageUrl") ?: ""
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(doc.getLong("date") ?: System.currentTimeMillis()))
                    val name = doc.getString("name") ?: ""
                    val addToFinances = doc.getBoolean("addToFinances") ?: false
                    val addToPantry = doc.getBoolean("addToPantry") ?: false
                    val id = doc.id
                    val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                    val products = doc.get("products") as? List<String> ?: emptyList()
                    receipts.add(Receipt(url, date, name, addToFinances, addToPantry, totalAmount, products, id))
                }
            }
        onDispose {
            registration.remove()
        }
    }

    // Launchery do wybierania i robienia zdjęć
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                showUploadOptions = true
                imageUri = uri
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                showUploadOptions = true
            }
        }
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = createImageUri(context)
            if (uri != null) {
                imageUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val uri = createImageUri(context)
            if (uri != null) {
                imageUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            launcher.launch(permission)
        }
    }

    val appBarColor = Color(0xFF3DD1C6) // Turkusowy (primary)
    val backgroundColor = Color(0xFFF8F8F8) // Jasnoszary (background)
    if (showScanner) {
        ScannerDialog(
            onDismiss = { showScanner = false },
            onScan = {
                launchCamera()
                showScanner = false
            },
            onGallery = {
                galleryLauncher.launch("image/*")
                showScanner = false
            }
        )
    }

    if (showUploadOptions) {
        UploadOptionsDialog(
            onDismiss = { showUploadOptions = false; imageUri = null },
            onConfirm = { financeOption, pantryOption ->
                addToFinances = financeOption
                addToPantry = pantryOption
                showUploadOptions = false
                imageUri?.let { finalUri ->
                    uploadReceipt(
                        finalUri,
                        db,
                        storage,
                        userId,
                        scope,
                        context,
                        addToFinances,
                        addToPantry
                    )
                }
            }
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarColor
                ),
                title = {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = "Banner z paragonami",
                        modifier = Modifier
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillHeight
                    )
                }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            Row(modifier = Modifier.padding(16.dp)) {
                FloatingActionButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = "Galeria"
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                FloatingActionButton(
                    onClick = { showScanner = true },
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_receipt),
                        contentDescription = "Skanuj"
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                FloatingActionButton(
                    onClick = { launchCamera() },
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Aparat"
                    )
                }
            }
        },
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) { paddingValues ->
        if (userId == null) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Brak zalogowanego użytkownika")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.padding(paddingValues)
            ) {
                items(receipts, key = { it.id }) { receipt ->
                    ReceiptItem(
                        receipt = receipt,
                        onDelete = {
                            deleteReceipt(
                                receipt,
                                db,
                                storage,
                                context
                            )
                        },
                        onImageClick = { selectedReceipt = receipt }
                    )
                }
            }
        }
    }

    // Pełnoekranowy widok rozszerzonego zdjęcia
    selectedReceipt?.let { receipt ->
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var rotation by remember { mutableStateOf(0f) }
        val transformableState =
            rememberTransformableState { zoomChange, offsetChange, rotationChange ->
                scale *= zoomChange
                offset += offsetChange
                rotation += rotationChange
            }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopAppBar(
                    title = { Text(text = "Dodano: ${receipt.date}", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = {
                            selectedReceipt = null
                            scale = 1f
                            offset = Offset.Zero
                            rotation = 0f
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back_arrow),
                                contentDescription = "Powrót",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
                Image(
                    painter = rememberAsyncImagePainter(receipt.imageUrl),
                    contentDescription = receipt.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                            rotationZ = rotation
                        )
                        .transformable(state = transformableState)
                        .clickable { },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun ReceiptItem(
    receipt: Receipt,
    onDelete: () -> Unit,
    onImageClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(250.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = receipt.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Dodano: ${receipt.date}", style = MaterialTheme.typography.bodyMedium)
            Image(
                painter = rememberAsyncImagePainter(receipt.imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onImageClick() },
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Usuń")
            }
        }
    }
}

data class Receipt(
    val imageUrl: String = "",
    val date: String = "",
    val name: String = "",
    val addToFinances: Boolean = false,
    val addToPantry: Boolean = false,
    val totalAmount: Double = 0.0,
    val products: List<String> = emptyList(),
    val id: String = ""
)

/**
 * Funkcja przetwarzająca obraz paragonu.
 * Korzysta z ML Kit OCR, by:
 * - Wyłapać całkowitą kwotę (szukając wzorca "SUMA" lub "RAZEM")
 * - Dzielić tekst na linie i przy pomocy ulepszonego regexu wyłapywać nazwę produktu oraz ilość
 *
 * Wynik przekazuje w onSuccess jako parę: totalAmount oraz lista produktów sformatowana jako "Nazwa (ilość szt)".
 */
fun processReceiptImage(context: Context, uri: Uri, onSuccess: (Double?, List<String>) -> Unit) {
    InputImage.fromFilePath(context, uri).let { image ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                Log.d("OCR", "Recognized text: $text")
                val totalRegex = Regex("(?i)(SUMA|RAZEM)[^\\d]*(\\d+[.,]\\d{2})")
                val totalMatch = totalRegex.find(text)
                val totalAmount = totalMatch?.groupValues?.get(2)
                    ?.replace(",", ".")
                    ?.toDoubleOrNull() ?: run {
                    val allPricesRegex = Regex("(\\d+[.,]\\d{2})")
                    val possiblePrices = allPricesRegex.findAll(text)
                        .mapNotNull { it.value.replace(",", ".").toDoubleOrNull() }
                        .toList()
                    possiblePrices.maxOrNull()
                }

                // Wyłapanie produktów – dzielimy tekst na linie i szukamy linii zawierających informację o ilości
                // Ulepszony regex: dopuszcza litery, cyfry, spacje, ukośniki, kropki, przecinki i myślniki
                val productRegex = Regex("(?i)([A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśźż0-9\\s/.,-]+?)\\s+(\\d+)\\s*(?:szt(?:uka)?|x)\\b")
                val products = mutableListOf<String>()
                text.lines().forEach { line ->
                    val match = productRegex.find(line)
                    if (match != null) {
                        val productName = match.groupValues[1].trim()
                        val quantity = match.groupValues[2].toIntOrNull() ?: 1
                        products.add("$productName ($quantity szt)")
                    }
                }

                Log.d("OCR", "Found total amount: $totalAmount")
                Log.d("OCR", "Found products: $products")
                onSuccess(totalAmount, products)
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Error processing receipt", e)
                onSuccess(null, emptyList())
            }
    }
}

fun uploadReceipt(
    uri: Uri,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    userId: String?,
    scope: CoroutineScope,
    context: Context,
    addToFinances: Boolean,
    addToPantry: Boolean
) {
    if (userId == null) return
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageRef = storage.reference.child("receipts/$userId/$timestamp.jpg")

    processReceiptImage(context, uri) { totalAmount, products ->
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val receiptData = mapOf(
                        "userId" to (currentUser?.uid ?: ""),
                        "imageUrl" to downloadUrl.toString(),
                        "date" to System.currentTimeMillis(),
                        "name" to "$timestamp",
                        "addToFinances" to addToFinances,
                        "addToPantry" to addToPantry,
                        "totalAmount" to totalAmount,
                        "products" to products
                    )

                    db.collection("receipts").add(receiptData)
                        .addOnSuccessListener { documentRef ->
                            // Zapisanie kwoty (paragonu) do kolekcji "transactions" (wcześniej "finances")
                            if (addToFinances && totalAmount != null) {
                                db.collection("transactions").add(
                                    mapOf(
                                        "userId" to userId,
                                        "amount" to totalAmount,
                                        "date" to Date(System.currentTimeMillis()),
                                        "type" to "expense",
                                        "category" to "Żywność", // Using proper capitalization
                                        "color" to CategoryInfo.getColor("Żywność", true).toArgb(),
                                        "isExpense" to true,
                                        "title" to "Paragon",
                                        "receiptId" to documentRef.id
                                    )
                                )
                            }

                            // Parsowanie i zapis zeskanowanych produktów do kolekcji "pantry_items"
                            if (addToPantry && products.isNotEmpty()) {
                                // Załóżmy, że produkty są w formacie "Nazwa produktu (X szt)"
                                val productRegex = Regex("(.+) \\((\\d+) szt\\)")
                                products.forEach { productString ->
                                    val matchResult = productRegex.find(productString)
                                    val productName = matchResult?.groupValues?.get(1)?.trim() ?: productString
                                    val quantity = matchResult?.groupValues?.get(2)?.toIntOrNull() ?: 1
                                    val pantryItemData = mapOf(
                                        "id" to "",
                                        "userId" to userId,
                                        "name" to productName,
                                        "groupId" to userId, // Using userId as groupId for personal items
                                        "description" to "",
                                        "category" to (PantryItem.CATEGORIES.entries.firstOrNull { entry ->
                                            entry.value.any { it.equals(productName, ignoreCase = true) }
                                        }?.key?.toString() ?: "Inne"),
                                        "location" to "Lodówka",
                                        "quantity" to quantity.toDouble(),
                                        "unit" to (PantryItem.DEFAULT_UNITS[productName] ?: "szt."),
                                        "expiryDate" to null,
                                        "purchaseDate" to (receiptData["date"] as? Long ?: System.currentTimeMillis()),
                                        "dateAdded" to System.currentTimeMillis().toString(),
                                        "receiptId" to documentRef.id
                                    )
                                    db.collection("pantry_items").add(pantryItemData)
                                }
                            }

                            Toast.makeText(
                                context,
                                "Paragon został dodany i przetworzony!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Błąd podczas dodawania paragonu!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Błąd podczas dodawania paragonu!", Toast.LENGTH_SHORT).show()
            }
    }
}


fun deleteReceipt(
    receipt: Receipt,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    context: Context
) {
    val storageRef = storage.getReferenceFromUrl(receipt.imageUrl)
    storageRef.delete().addOnSuccessListener {
        db.collection("receipts").document(receipt.id).delete().addOnSuccessListener {
            Toast.makeText(context, "Zdjęcie zostało usunięte!", Toast.LENGTH_SHORT).show()
        }
    }
}

fun saveBitmapToUri(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun UploadOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (addToFinances: Boolean, addToPantry: Boolean) -> Unit
) {
    var addToFinances by remember { mutableStateOf(false) }
    var addToPantry by remember { mutableStateOf(false) }
    var showDetectedItems by remember { mutableStateOf(false) }
    var showTotalAmount by remember { mutableStateOf(false) }
    var detectedAmount by remember { mutableStateOf("0.00") }
    var detectedProducts by remember { mutableStateOf(listOf<String>()) }

    if (showDetectedItems) {
        AlertDialog(
            onDismissRequest = { showDetectedItems = false },
            title = { Text("Wykryte produkty") },
            text = {
                Column {
                    detectedProducts.forEach { product ->
                        Text(product)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDetectedItems = false }) {
                    Text("Zatwierdź")
                }
            }
        )
    }

    if (showTotalAmount) {
        AlertDialog(
            onDismissRequest = { showTotalAmount = false },
            title = { Text("Wykryta kwota") },
            text = {
                Column {
                    Text("Suma: $detectedAmount PLN")
                }
            },
            confirmButton = {
                Button(onClick = { showTotalAmount = false }) {
                    Text("Zatwierdź")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Opcje dodawania paragonu") },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Wybierz, gdzie chcesz dodać dane z paragonu:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (addToFinances) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .padding(16.dp)
                    ) {
                        Checkbox(
                            checked = addToFinances,
                            onCheckedChange = { addToFinances = it }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Dodaj kwotę do finansów",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Automatycznie dodaj całkowitą kwotę z paragonu do wydatków",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showTotalAmount = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Podgląd kwoty"
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (addToPantry) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .padding(16.dp)
                    ) {
                        Checkbox(
                            checked = addToPantry,
                            onCheckedChange = { addToPantry = it }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Dodaj produkty do spiżarni",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Automatycznie dodaj produkty z paragonu do spiżarni",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showDetectedItems = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Podgląd produktów"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(addToFinances, addToPantry) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Kontynuuj")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun ScannerDialog(
    onDismiss: () -> Unit,
    onScan: () -> Unit,
    onGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Skanowanie paragonu") },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Wybierz sposób skanowania paragonu:",
                    style = MaterialTheme.typography.bodyLarge
                )

                Button(
                    onClick = onScan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Zrób zdjęcie")
                }

                Button(
                    onClick = onGallery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Wybierz z galerii")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

fun createImageUri(context: Context): Uri? {
    val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}