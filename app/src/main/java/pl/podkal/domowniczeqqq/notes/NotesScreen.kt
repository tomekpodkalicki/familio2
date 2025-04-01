package pl.podkal.domowniczeqqq.notes

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import pl.podkal.domowniczeq.R
import pl.podkal.domowniczeqqq.navigation.BottomNavBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ----------------------- MODEL -----------------------
data class Note(
    var id: String = UUID.randomUUID().toString(),
    var userId: String = "",
    var title: String = "",
    var content: String = "",
    var category: String = "Zwykła",  // Domyślna kategoria
    var color: String = "#FFF9C4",    // Kolor w formie hex
    var archived: Boolean = false,
    var createdAt: Long = System.currentTimeMillis() // Data utworzenia (timestamp)
)

data class LinkedAccounts(
    var groupId: String? = null,
    var users: List<String> = emptyList()
)

// Enum do widoku (siatka / lista)
enum class ViewType {
    GRID, LIST
}

// Prosta konwersja String -> Color
fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFFFFF9C4) // fallback (jasny żółty)
    }
}

// Formatowanie daty (milisekundy -> "dd.MM.yyyy HH:mm")
fun formatMillisToDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

// Wylogowanie
fun logout() {
    Firebase.auth.signOut()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = Firebase.auth.currentUser
    val userId = currentUser?.uid.orEmpty()

    val topBarColor = Color(0xFF3DD1C6)
    val backgroundColor = Color(0xFFF8F8F8)

    // Lista notatek (z Firestore)
    val notesList = remember { mutableStateListOf<Note>() }

    // Zaznaczone notatki (po ID) – do usuwania
    var selectedNoteIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Wyszukiwanie
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Rozwijane menu (3 kropki) w TopBar
    var showMenu by remember { mutableStateOf(false) }

    // Zmiana widoku: siatka / lista (domyślnie siatka)
    var viewType by remember { mutableStateOf(ViewType.GRID) }

    // Dialog do dodawania/edycji
    var showDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }

    // Pola w dialogu
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }

    // Kategorie – parę przykładowych plus „Inna” (wtedy user wpisze swoją)
    val categoryOptions = listOf("Zwykła", "Praca", "Dom", "Zakupy", "Inna")
    var selectedCategory by remember { mutableStateOf("Zwykła") }
    var customCategoryText by remember { mutableStateOf("") }

    // Kolor + archiwizacja (archiwum domyślnie false)
    var noteColor by remember { mutableStateOf("#FFF9C4") }
    var noteArchived by remember { mutableStateOf(false) }

    // Nasłuchiwanie Firestore: pobieramy notatki (tylko niearchiwizowane)
    DisposableEffect(userId) {
        if (userId.isBlank()) {
            notesList.clear()
            return@DisposableEffect onDispose {}
        }
        val registration = db.collection("linked_accounts")
            .whereArrayContains("users", userId)
            .addSnapshotListener { linkedSnapshot, error ->
                if (error != null) return@addSnapshotListener

                val userGroups = linkedSnapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(LinkedAccounts::class.java)?.groupId
                } ?: emptyList()

                db.collection("notes")
                    .whereIn("groupId", listOf(userId) + userGroups)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("NotesScreen", "Listen failed.", error)
                            return@addSnapshotListener
                        }
                        notesList.clear()
                        snapshot?.documents?.forEach { doc ->
                            val n = doc.toObject<Note>() ?: return@forEach
                            n.id = doc.id
                            notesList.add(n)
                        }
                        // Po odświeżeniu czyścimy zaznaczone
                        selectedNoteIds = emptySet()
                    }
            }
        onDispose {
            registration.remove()
        }
    }

    // Filtrowanie notatek na podstawie searchQuery
    val filteredNotes = remember(notesList, searchQuery) {
        if (searchQuery.isBlank()) {
            notesList
        } else {
            val q = searchQuery.lowercase()
            notesList.filter {
                it.title.lowercase().contains(q)
                        || it.content.lowercase().contains(q)
                        || it.category.lowercase().contains(q)
            }
        }
    }

    // Funkcja usuwania zaznaczonych notatek
    fun deleteSelectedNotes() {
        selectedNoteIds.forEach { noteId ->
            db.collection("notes").document(noteId).delete()
        }
        selectedNoteIds = emptySet()
    }

    // ------------------ UI -----------------------
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = backgroundColor,
        bottomBar = { BottomNavBar(navController) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor),
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
                            text = "Moje Notatki",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    // Ikona lupy (szukaj)
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Szukaj",
                            tint = Color.White
                        )
                    }

                    // Menu (MoreVert)
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Przejście do ekranu Archiwum
                        DropdownMenuItem(
                            text = { Text("Archiwum") },
                            onClick = {
                                showMenu = false
                                navController.navigate("archive_screen")
                            }
                        )
                        // ZAMIANA nazw widoku -> jeśli obecnie GRID, pisz "Lista"; jeśli LIST, pisz "Siatka"
                        DropdownMenuItem(
                            text = {
                                Text("Widok: ${if (viewType == ViewType.GRID) "Lista" else "Siatka"}")
                            },
                            onClick = {
                                viewType = if (viewType == ViewType.GRID) ViewType.LIST else ViewType.GRID
                                showMenu = false
                                // Po zmianie widoku czyścimy zaznaczenia
                                selectedNoteIds = emptySet()
                            }
                        )
                    }

                    // Wylogowanie
                    IconButton(onClick = {
                        logout()
                        navController.navigate("login_screen") {
                            popUpTo("notes_screen") { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Wyloguj",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // Dwa przyciski obok siebie w wierszu
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FAB: Dodawanie notatki
                FloatingActionButton(
                    onClick = {
                        if (userId.isBlank()) return@FloatingActionButton
                        editingNote = null
                        noteTitle = ""
                        noteContent = ""
                        selectedCategory = "Zwykła"
                        customCategoryText = ""
                        noteColor = "#FFF9C4"
                        noteArchived = false
                        showDialog = true
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Dodaj notatkę")
                }

                // Drugi FAB (kosz) – tylko w widoku listy
                if (viewType == ViewType.LIST) {
                    // Sterujemy wyglądem i działaniem zamiast `enabled`
                    val isAnythingSelected = selectedNoteIds.isNotEmpty()

                    FloatingActionButton(
                        onClick = {
                            // Jeśli cokolwiek zaznaczone, usuń
                            if (isAnythingSelected) {
                                deleteSelectedNotes()
                            }
                            // Można ewentualnie w else zrobić Toast czy inny komunikat
                        },
                        // Kolor zależny od stanu
                        containerColor = if (isAnythingSelected) {
                            // domyślny styl
                            FloatingActionButtonDefaults.containerColor
                        } else {
                            // lekko wyszarzony, żeby wyglądał jak disabled
                            Color.Gray.copy(alpha = 0.6f)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Usuń zaznaczone"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Pole wyszukiwania
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Szukaj w tytule, treści, kategorii...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            // Obsługa braku usera
            if (userId.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Brak zalogowanego użytkownika.")
                }
                return@Scaffold
            }

            // Główna część ekranu
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Brak notatek do wyświetlenia.")
                }
            } else {
                when (viewType) {
                    // WIDOK SIATKA (dwie kolumny)
                    ViewType.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = filteredNotes,
                                key = { it.id }
                            ) { note ->
                                NoteGridItemUI(
                                    note = note,
                                    onClick = {
                                        // Edycja notatki
                                        editingNote = note
                                        noteTitle = note.title
                                        noteContent = note.content
                                        selectedCategory = note.category
                                        customCategoryText = ""
                                        noteColor = note.color
                                        noteArchived = note.archived
                                        showDialog = true
                                    }
                                )
                            }
                        }
                    }
                    // WIDOK LISTA (checkbox + multi-selecja)
                    ViewType.LIST -> {
                        LazyColumn(
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = filteredNotes,
                                key = { it.id }
                            ) { note ->
                                val isSelected = selectedNoteIds.contains(note.id)
                                NoteListItemUI(
                                    note = note,
                                    isSelected = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedNoteIds = if (checked) {
                                            selectedNoteIds + note.id
                                        } else {
                                            selectedNoteIds - note.id
                                        }
                                    },
                                    onClick = {
                                        // Klik poza checkboxem -> edycja notatki
                                        editingNote = note
                                        noteTitle = note.title
                                        noteContent = note.content
                                        selectedCategory = note.category
                                        customCategoryText = ""
                                        noteColor = note.color
                                        noteArchived = note.archived
                                        showDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------ Dialog dodawania/edycji ------------------
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                if (editingNote == null) Text("Dodaj notatkę")
                else Text("Edytuj notatkę")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Tytuł") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Treść") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // KATEGORIA
                    Text("Kategoria:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedCategory,
                            onValueChange = {},
                            label = { Text("Wybierz kategorię") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categoryOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedCategory = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    // Jeśli "Inna", pokaż dodatkowe pole
                    if (selectedCategory == "Inna") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customCategoryText,
                            onValueChange = { customCategoryText = it },
                            label = { Text("Wpisz własną kategorię") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Archiwizacja tylko przy edycji
                    if (editingNote != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Zarchiwizowana?", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Switch(
                            checked = noteArchived,
                            onCheckedChange = { noteArchived = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Kolor tła
                    Text("Wybierz kolor tła:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val colorOptions = listOf(
                            "#FFF9C4", "#A5D6A7", "#EF9A9A", "#80CBC4", "#E1BEE7"
                        )
                        colorOptions.forEach { c ->
                            val isSelected = (noteColor == c)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(c))
                                    .drawBehind {
                                        if (isSelected) {
                                            drawCircle(
                                                color = Color.Black,
                                                style = Stroke(width = 4f)
                                            )
                                        }
                                    }
                                    .clickable {
                                        noteColor = c
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val notesRef = db.collection("notes")
                    val finalCategory = if (selectedCategory == "Inna" && customCategoryText.isNotBlank()) {
                        customCategoryText
                    } else {
                        selectedCategory
                    }

                    if (editingNote == null) {
                        // Dodawanie nowej notatki
                        val newDoc = notesRef.document()
                        val newNote = Note(
                            id = newDoc.id,
                            userId = userId,
                            title = noteTitle,
                            content = noteContent,
                            category = finalCategory,
                            color = noteColor,
                            archived = false,
                            createdAt = System.currentTimeMillis()
                        )
                        newDoc.set(newNote)
                    } else {
                        // Edycja
                        notesRef.document(editingNote!!.id)
                            .update(
                                mapOf(
                                    "title" to noteTitle,
                                    "content" to noteContent,
                                    "category" to finalCategory,
                                    "color" to noteColor,
                                    "archived" to noteArchived
                                )
                            )
                    }
                    showDialog = false
                }) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                Row {
                    // Archiwizuj (jeśli edycja i notatka nie jest archiwizowana)
                    if (editingNote != null && !noteArchived) {
                        TextButton(onClick = {
                            db.collection("notes")
                                .document(editingNote!!.id)
                                .update("archived", true)
                            showDialog = false
                        }) {
                            Text("Archiwizuj")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    // Usuń notatkę (tylko przy edycji)
                    if (editingNote != null) {
                        IconButton(onClick = {
                            db.collection("notes").document(editingNote!!.id).delete()
                            showDialog = false
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Usuń notatkę")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = { showDialog = false }) {
                        Text("Anuluj")
                    }
                }
            },
            properties = DialogProperties()
        )
    }
}

// ------------------ WIDOK SIATKI: Pojedynczy item notatki ------------------
@Composable
fun NoteGridItemUI(note: Note, onClick: () -> Unit) {
    val bgColor = parseHexColor(note.color)
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(text = note.title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.content, maxLines = 5)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Data dodania: ${formatMillisToDate(note.createdAt)}")
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Kategoria: ${note.category}", fontWeight = FontWeight.Light)
        }
    }
}

// ------------------ WIDOK LISTY: Pojedynczy item notatki ------------------
@Composable
fun NoteListItemUI(
    note: Note,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val bgColor = parseHexColor(note.color)
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .background(bgColor, shape = RoundedCornerShape(4.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(
                    // Aby klik nie kolidował z checkboxem, można użyć rowData
                    onClick = { onClick() }
                )
                .padding(8.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { checked -> onCheckedChange(checked) }
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(text = note.title, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = note.content)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Data dodania: ${formatMillisToDate(note.createdAt)}")
                Text(
                    text = "Kategoria: ${note.category}",
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}