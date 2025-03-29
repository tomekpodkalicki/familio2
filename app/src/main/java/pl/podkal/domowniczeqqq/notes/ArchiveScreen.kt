package pl.podkal.domowniczeqqq.notes

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import pl.podkal.domowniczeqqq.navigation.BottomNavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val userId = Firebase.auth.currentUser?.uid.orEmpty()

    var archivedNotes by remember { mutableStateOf(listOf<Note>()) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Domyślnie wyświetlaj w formie listy lub siatki
    var viewType by remember { mutableStateOf(ViewType.LIST) }

    // Kolor tła dla spójności z NotesScreen (można zmienić na dowolny)
    val backgroundColor = Color(0xFFF8F8F8)

    // Nasłuch archiwalnych notatek (archived = true)
    DisposableEffect(userId) {
        if (userId.isBlank()) {
            archivedNotes = emptyList()
            return@DisposableEffect onDispose {}
        }
        val registration = db.collection("notes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("archived", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ArchiveScreen", "Listen failed", error)
                    return@addSnapshotListener
                }
                val temp = mutableListOf<Note>()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(Note::class.java)?.let {
                        it.id = doc.id
                        temp.add(it)
                    }
                }
                archivedNotes = temp
            }
        onDispose {
            registration.remove()
        }
    }

    // Filtrowanie wg wyszukiwanej frazy
    val filtered = remember(archivedNotes, searchQuery) {
        if (searchQuery.isBlank()) archivedNotes
        else {
            val q = searchQuery.lowercase()
            archivedNotes.filter {
                it.title.lowercase().contains(q) ||
                        it.content.lowercase().contains(q) ||
                        it.category.lowercase().contains(q)
            }
        }
    }

    // Scaffold z obsługą dolnego paska (BottomNavBar) tak jak w NotesScreen
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Archiwum") },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "Szukaj")
                    }
                    IconButton(onClick = {
                        viewType = if (viewType == ViewType.GRID) ViewType.LIST else ViewType.GRID
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Zmień widok")
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Szukaj w archiwum...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Brak notatek w archiwum.")
                }
            } else {
                if (viewType == ViewType.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { note ->
                            ArchivedNoteItem(note, db)
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(8.dp)) {
                        items(filtered, key = { it.id }) { note ->
                            ArchivedNoteItem(note, db)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivedNoteItem(note: Note, db: FirebaseFirestore) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = parseHexColor(note.color))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = note.title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.content, maxLines = 5)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Kategoria: ${note.category}")
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "(Archiwum)", color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            // Przycisk odarchiwizowania
            Button(onClick = {
                db.collection("notes").document(note.id)
                    .update("archived", false)
            }) {
                Text("Odarchiwizuj")
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Przycisk usunięcia na stałe
            Button(onClick = {
                db.collection("notes").document(note.id).delete()
            }) {
                Text("Usuń na zawsze")
            }
        }
    }
}
