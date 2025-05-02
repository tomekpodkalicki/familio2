package pl.podkal.domowniczeqqq.notes

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import pl.podkal.domowniczeq.R
import pl.podkal.domowniczeqqq.navigation.BottomNavBar
import java.text.SimpleDateFormat
import java.util.*

// -----------------------  MODELE  -----------------------

data class Note(
    var id: String = UUID.randomUUID().toString(),
    var userId: String = "",
    var title: String = "",
    var content: String = "",
    var category: String = "Zwykła",
    var color: String = "#FFF9C4",
    var archived: Boolean = false,
    var createdAt: Long = System.currentTimeMillis()
)

enum class ViewType { GRID, LIST }

fun parseHexColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFFFFF9C4))

fun formatMillisToDate(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(millis))

fun logout() = Firebase.auth.signOut()

// ------------------------  UI  --------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(navController: NavController) {

    val db = FirebaseFirestore.getInstance()
    val userId = Firebase.auth.currentUser?.uid.orEmpty()

    val notesList = remember { mutableStateListOf<Note>() }
    var selectedNoteIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu   by remember { mutableStateOf(false) }
    var viewType   by remember { mutableStateOf(ViewType.GRID) }

    var showDialog     by remember { mutableStateOf(false) }
    var editingNote    by remember { mutableStateOf<Note?>(null) }
    var noteTitle      by remember { mutableStateOf("") }
    var noteContent    by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Zwykła") }
    var customCategoryText by remember { mutableStateOf("") }
    var noteColor      by remember { mutableStateOf("#FFF9C4") }
    var noteArchived   by remember { mutableStateOf(false) }

    val categoryOptions = listOf("Zwykła", "Praca", "Dom", "Zakupy", "Inna")


    DisposableEffect(userId) {
        if (userId.isBlank()) { notesList.clear(); onDispose { } }
        else {
            val reg = db.collection("notes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("archived", false)
                .addSnapshotListener { snap, err ->
                    if (err != null) { Log.e("Notes", "listen error", err); return@addSnapshotListener }
                    notesList.clear()
                    snap?.documents?.forEach { d ->
                        d.toObject(Note::class.java)?.also { it.id = d.id; notesList += it }
                    }
                    selectedNoteIds = emptySet()
                }
            onDispose { reg.remove() }
        }
    }

    val filteredNotes = remember(notesList, searchQuery) {
        if (searchQuery.isBlank()) notesList else {
            val q = searchQuery.lowercase()
            notesList.filter {
                it.title.lowercase().contains(q) ||
                        it.content.lowercase().contains(q) ||
                        it.category.lowercase().contains(q)
            }
        }
    }

    fun deleteSelectedNotes() {
        selectedNoteIds.forEach { db.collection("notes").document(it).delete() }
        selectedNoteIds = emptySet()
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FloatingActionButton(
                    onClick = {
                        editingNote = null
                        noteTitle = ""; noteContent = ""
                        selectedCategory = "Zwykła"; customCategoryText = ""
                        noteColor = "#FFF9C4"; noteArchived = false
                        showDialog = true
                    }) { Icon(Icons.Default.Add, null) }

                if (viewType == ViewType.LIST) {
                    val enabled = selectedNoteIds.isNotEmpty()
                    FloatingActionButton(
                        onClick = { if (enabled) deleteSelectedNotes() },
                        containerColor = if (enabled)
                            FloatingActionButtonDefaults.containerColor
                        else Color.Gray.copy(alpha = .6f)
                    ) { Icon(Icons.Default.Delete, null) }
                }
            }
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3DD1C6)),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(R.drawable.logo), "Logo",
                            modifier = Modifier.height(40.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.FillHeight
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Moje Notatki", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                actions = {
                    IconButton({ showSearch = !showSearch }) { Icon(Icons.Default.Search, null, tint = Color.White) }
                    IconButton({ showMenu = !showMenu })    { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Archiwum") },
                            onClick = { showMenu = false; navController.navigate("archive_screen") })
                        DropdownMenuItem(
                            text = { Text("Widok: ${if (viewType==ViewType.GRID) "Lista" else "Siatka"}") },
                            onClick = {
                                viewType = if (viewType==ViewType.GRID) ViewType.LIST else ViewType.GRID
                                showMenu = false; selectedNoteIds = emptySet()
                            })
                    }
                    IconButton({
                        logout(); navController.navigate("login_screen") {
                        popUpTo("notes_screen") { inclusive = true }
                    }
                    }) { Icon(Icons.Default.Logout, null, tint = Color.White) }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            if (showSearch)
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    label = { Text("Szukaj...") },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )

            if (userId.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Brak zalogowanego użytkownika.") }
                return@Scaffold
            }

            if (filteredNotes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Brak notatek do wyświetlenia.") }
            } else when (viewType) {
                ViewType.GRID -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2), contentPadding = PaddingValues(8.dp), modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotes, key = { it.id }) { n ->
                        NoteGridItemUI(
                            note = n,
                            onClick = {
                                editingNote      = n
                                noteTitle        = n.title
                                noteContent      = n.content
                                selectedCategory = n.category
                                customCategoryText = ""
                                noteColor        = n.color
                                noteArchived     = n.archived
                                showDialog       = true
                            },
                            onEdit = {
                                editingNote      = n
                                noteTitle        = n.title
                                noteContent      = n.content
                                selectedCategory = n.category
                                customCategoryText = ""
                                noteColor        = n.color
                                noteArchived     = n.archived
                                showDialog       = true
                            },
                            onDelete = { db.collection("notes").document(n.id).delete() }
                        )
                    }
                }
                ViewType.LIST -> LazyColumn(contentPadding = PaddingValues(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredNotes, key = { it.id }) { n ->
                        val isSel = selectedNoteIds.contains(n.id)
                        NoteListItemUI(
                            note = n,
                            isSelected = isSel,
                            onCheckedChange = { chk -> selectedNoteIds = if (chk) selectedNoteIds + n.id else selectedNoteIds - n.id },
                            onClick = {
                                editingNote      = n
                                noteTitle        = n.title
                                noteContent      = n.content
                                selectedCategory = n.category
                                customCategoryText = ""
                                noteColor        = n.color
                                noteArchived     = n.archived
                                showDialog       = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingNote==null) "Dodaj notatkę" else "Edytuj notatkę") },
            text = {
                Column {
                    OutlinedTextField(noteTitle, { noteTitle = it }, label={Text("Tytuł")}, singleLine=true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(noteContent, { noteContent = it }, label={Text("Treść")}, modifier = Modifier.fillMaxWidth().height(100.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Kategoria:", fontWeight = FontWeight.Bold)
                    var exp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp=!exp }) {
                        OutlinedTextField(readOnly = true, value = selectedCategory, onValueChange = {}, label={Text("Wybierz kategorię")},
                            trailingIcon={ ExposedDropdownMenuDefaults.TrailingIcon(exp) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(exp, { exp=false }) { categoryOptions.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { selectedCategory = c; exp=false }) } }
                    }
                    if (selectedCategory=="Inna") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(customCategoryText, { customCategoryText = it }, label={Text("Własna kategoria")}, singleLine=true, modifier = Modifier.fillMaxWidth())
                    }
                    if (editingNote!=null) {
                        Spacer(Modifier.height(16.dp)); Text("Zarchiwizowana?", fontWeight = FontWeight.Bold)
                        Switch(noteArchived, { noteArchived = it })
                    }
                    Spacer(Modifier.height(16.dp)); Text("Kolor tła:", fontWeight = FontWeight.Bold)
                    val colors = listOf("#FFF9C4","#A5D6A7","#EF9A9A","#80CBC4","#E1BEE7")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { colors.forEach { c ->
                        val sel = (noteColor==c)
                        Box(Modifier.size(32.dp).clip(CircleShape).background(parseHexColor(c)).drawBehind { if (sel) drawCircle(Color.Black, style= Stroke(4f)) }.clickable { noteColor=c })
                    } }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalCat = if (selectedCategory=="Inna" && customCategoryText.isNotBlank()) customCategoryText else selectedCategory
                    val ref = db.collection("notes")
                    if (editingNote==null) {
                        val doc = ref.document()
                        doc.set(Note(id=doc.id, userId=userId, title=noteTitle, content=noteContent, category=finalCat, color=noteColor, createdAt=System.currentTimeMillis()))
                    } else {
                        ref.document(editingNote!!.id).update(mapOf("title" to noteTitle,"content" to noteContent,"category" to finalCat,"color" to noteColor,"archived" to noteArchived))
                    }
                    showDialog = false
                }) { Text("Zapisz") }
            },
            dismissButton = {
                Row {
                    if (editingNote!=null && !noteArchived) { TextButton({ db.collection("notes").document(editingNote!!.id).update("archived",true); showDialog=false }) { Text("Archiwizuj") }; Spacer(Modifier.width(8.dp)) }
                    if (editingNote!=null) { IconButton({ db.collection("notes").document(editingNote!!.id).delete(); showDialog=false }) { Icon(Icons.Default.Delete, null) }; Spacer(Modifier.width(8.dp)) }
                    TextButton({ showDialog=false }) { Text("Anuluj") }
                }
            }, properties = DialogProperties()
        )
    }
}

/* -------------------- GRID ITEM -------------------- */
@Composable
fun NoteGridItemUI(
    note: Note,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = parseHexColor(note.color)
    Card(
        modifier = Modifier.padding(8.dp).fillMaxWidth().aspectRatio(0.9f).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bg),
        shape  = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp).fillMaxSize()) {
                Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(note.content, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(note.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(.7f))
                    Text(formatMillisToDate(note.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(.5f))
                }
            }
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onEdit, Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface.copy(.85f), CircleShape)) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onDelete, Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface.copy(.85f), CircleShape)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/* -------------------- LIST ITEM -------------------- */
@Composable
fun NoteListItemUI(
    note: Note,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val bg = parseHexColor(note.color)
    Box(Modifier.padding(vertical = 4.dp).fillMaxWidth().background(bg, RoundedCornerShape(4.dp))) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
            Checkbox(isSelected, onCheckedChange)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(note.title, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(note.content)
                Spacer(Modifier.height(4.dp))
                Text("Data dodania: ${formatMillisToDate(note.createdAt)}")
                Text("Kategoria: ${note.category}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Light)
            }
        }
    }
}
