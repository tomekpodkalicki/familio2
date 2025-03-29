package pl.podkal.domowniczeqqq.receipts

import android.content.Context
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import pl.podkal.domowniczeq.R
import pl.podkal.domowniczeqqq.navigation.BottomNavBar
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

    // Obecnie zalogowany użytkownik:
    val currentUser = Firebase.auth.currentUser
    val userId = currentUser?.uid

    // Lista paragonów obserwowana w Compose
    val receipts = remember { mutableStateListOf<Receipt>() }
    val scope = rememberCoroutineScope()
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // Stan do wyświetlania rozszerzonego widoku paragonu
    var selectedReceipt by remember { mutableStateOf<Receipt?>(null) }

    // Nasłuchiwanie zmian w kolekcji "receipts" (tylko dla zalogowanego usera)
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
                    val date = doc.getString("date") ?: ""
                    val name = doc.getString("name") ?: ""
                    val id = doc.id
                    receipts.add(Receipt(url, date, name, id))
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
            imageUri = uri
            uri?.let {
                uploadReceipt(
                    it,
                    db,
                    storage,
                    userId,
                    scope,
                    context
                )
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            bitmap?.let {
                val tempUri = saveBitmapToUri(context, it)
                tempUri?.let { safeUri ->
                    uploadReceipt(
                        safeUri,
                        db,
                        storage,
                        userId,
                        scope,
                        context
                    )
                }
            }
        }
    )

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
                    onClick = { cameraLauncher.launch(null) },
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
        // Stan do zooma, przesunięcia i rotacji
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

data class Receipt(val imageUrl: String, val date: String, val name: String, val id: String)

fun uploadReceipt(
    uri: Uri,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    userId: String?,
    scope: CoroutineScope,
    context: Context
) {
    if (userId == null) return
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageRef = storage.reference.child("receipts/$userId/$timestamp.jpg")

    storageRef.putFile(uri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val receiptData = mapOf(
                    "userId" to userId,
                    "imageUrl" to downloadUrl.toString(),
                    "date" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    "name" to "$timestamp"
                )
                db.collection("receipts").add(receiptData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Zdjęcie zostało dodane!", Toast.LENGTH_SHORT).show()
                    }
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
