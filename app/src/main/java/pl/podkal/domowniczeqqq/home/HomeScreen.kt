package pl.podkal.domowniczeqqq.home

import android.app.TimePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

data class LinkedAccounts(val groupId: String? = null, val users: List<String> = emptyList())

// ---------------------- MODELE i OBIEKT PRZECHOWUJĄCY DANE ----------------------

// Predefiniowane kategorie wydarzeń
val defaultCategories = listOf(
    "Sprzątanie",
    "Zakupy",
    "Gotowanie",
    "Naprawa",
    "Opieka nad zwierzętami",
    "Pranie",
    "Inne"
)

/**
 * Updated model to also store day, month, year.
 */
data class ActivityData(
    var docId: String? = null, // do usuwania/edycji w Firestore
    var title: String,
    var color: Color,
    var category: String? = null,
    var time: LocalTime? = null,
    var timestamp: Long = System.currentTimeMillis(),
    var hasNotification: Boolean = false,
    var notificationTime: Int? = null, // w minutach przed wydarzeniem
    var groupId: String? = null,

    // NEW: store the event date explicitly
    var day: Int,
    var month: Int,
    var year: Int
)

object CalendarEventsManager {
    // userId -> (YearMonth -> mapa: dzień -> lista eventów)
    val userData = mutableStateMapOf<String, MutableMap<YearMonth, MutableMap<Int, MutableList<ActivityData>>>>()
}

val auth = Firebase.auth
val db = FirebaseFirestore.getInstance()

val polishMonthNames = mapOf(
    1 to "styczeń",
    2 to "luty",
    3 to "marzec",
    4 to "kwiecień",
    5 to "maj",
    6 to "czerwiec",
    7 to "lipiec",
    8 to "sierpień",
    9 to "wrzesień",
    10 to "październik",
    11 to "listopad",
    12 to "grudzień"
)

// ---------------------------------- HomeScreen ----------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current

    // Zalogowany użytkownik
    val user = auth.currentUser
    val userId = user?.uid ?: "unknownUser"

    // Blokujemy back w tym ekranie
    BackHandler { /* ignorujemy cofnięcie */ }

    // Stan aktualnie wybranego miesiąca
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(LocalDate.now())) }

    // Mapa usera
    val currentUserMap = CalendarEventsManager.userData.getOrPut(userId) {
        mutableStateMapOf()
    }
    val currentMonthActivities = currentUserMap.getOrPut(currentYearMonth) {
        mutableStateMapOf()
    }

    // Oblicz 42 pola (6x7)
    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value
    val offset = firstDayOfWeek - 1
    val totalCells = 42
    val calendarCells = remember(currentYearMonth) {
        buildList<Int?> {
            for (cellIndex in 0 until totalCells) {
                val dayNumber = cellIndex - offset + 1
                if (dayNumber in 1..daysInMonth) add(dayNumber) else add(null)
            }
        }
    }

    // Stany dialogów
    var showEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<ActivityData?>(null) }

    // We still keep `editingDay` for the case of adding a new event.
    var editingDay by remember { mutableStateOf<Int?>(null) }

    var activityTitle by remember { mutableStateOf("") }
    var chosenColor by remember { mutableStateOf(Color(0xFFDCE775)) }
    var category by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var timeText by remember { mutableStateOf("") }

    // Dla powiadomień
    var hasNotification by remember { mutableStateOf(false) }
    var notificationMinutes by remember { mutableStateOf<Int?>(null) }
    var notificationTimeExpanded by remember { mutableStateOf(false) }

    // Dialog do wyświetlenia wydarzeń w konkretnym dniu
    var showDayEventsDialog by remember { mutableStateOf(false) }
    var dayEvents by remember { mutableStateOf(emptyList<ActivityData>()) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    // Wszystkie wydarzenia w bieżącym miesiącu (później wyświetlamy listę)
    var events by remember { mutableStateOf<List<ActivityData>>(emptyList()) }

    // -------------- Firestore --------------
    DisposableEffect(userId, currentYearMonth) {
        val registration = db.collection("linked_accounts")
            .whereArrayContains("users", userId)
            .addSnapshotListener { linkedSnapshot, error ->
                if (error != null) return@addSnapshotListener

                val userGroups = linkedSnapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(LinkedAccounts::class.java)?.groupId
                } ?: emptyList()

                val listener = db.collection("events")
                    // Wczytujemy eventy, w których groupId jest userId LUB dowolne z userGroups
                    .whereIn("groupId", listOf(userId) + userGroups)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("Firestore", "Błąd odczytu: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot == null) return@addSnapshotListener

                        val newMonthMap = mutableMapOf<YearMonth, MutableMap<Int, MutableList<ActivityData>>>()

                        for (doc in snapshot.documents) {
                            val year = doc.getLong("year")?.toInt() ?: continue
                            val month = doc.getLong("month")?.toInt() ?: continue
                            val day = doc.getLong("day")?.toInt() ?: continue

                            val colorLong = doc.getLong("color")?.toInt() ?: continue
                            val color = Color(colorLong)

                            val timeString = doc.getString("time")
                            val localTime = timeString?.let {
                                try {
                                    LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
                                } catch (_: Exception) {
                                    null
                                }
                            }

                            val timestamp = doc.getTimestamp("createdAt")?.toDate()?.time
                                ?: System.currentTimeMillis()
                            val hasNotification = doc.getBoolean("hasNotification") ?: false
                            val groupId = doc.getString("groupId")
                            val notificationTime = doc.getLong("notificationTime")?.toInt()

                            val activityData = ActivityData(
                                docId = doc.id,
                                title = doc.getString("title") ?: "",
                                color = color,
                                category = doc.getString("category"),
                                time = localTime,
                                timestamp = timestamp,
                                hasNotification = hasNotification,
                                notificationTime = notificationTime,
                                groupId = groupId,
                                day = day,
                                month = month,
                                year = year
                            )

                            val ym = YearMonth.of(year, month)
                            val daysMap = newMonthMap.getOrPut(ym) { mutableMapOf() }
                            val dayList = daysMap.getOrPut(day) { mutableListOf() }
                            dayList.add(activityData)
                        }

                        CalendarEventsManager.userData[userId] = newMonthMap
                        events = newMonthMap[currentYearMonth]?.values?.flatten() ?: emptyList()
                    }
                onDispose { listener.remove() }
            }
        onDispose { registration.remove() }
    }

    // ------------------- UI -------------------
    Scaffold(
        containerColor = Color(0xFFF8F8F8),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3DD1C6)),
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
                            text = "Mój Kalendarz",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        logout()
                        navController.navigate("login_screen") {
                            popUpTo("home_screen") { inclusive = true }
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.logout),
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pasek wyboru miesiąca
            MonthYearBar(
                currentYearMonth = currentYearMonth,
                onMonthChange = { newMonth ->
                    currentYearMonth = currentYearMonth.withMonth(newMonth)
                },
                onYearChange = { newYear ->
                    currentYearMonth = currentYearMonth.withYear(newYear)
                },
                onPreviousMonth = {
                    currentYearMonth = currentYearMonth.minusMonths(1)
                },
                onNextMonth = {
                    currentYearMonth = currentYearMonth.plusMonths(1)
                }
            )

            // Obsługa przesunięcia w lewo/prawo
            val dragThresholdPx = with(LocalDensity.current) { 40.dp.toPx() }
            var swipedThisGesture by remember { mutableStateOf(false) }

            // 1) KALENDARZ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // stała wysokość
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { swipedThisGesture = false },
                            onDragEnd = { swipedThisGesture = false },
                            onDragCancel = { swipedThisGesture = false },
                            onDrag = { _, dragAmount ->
                                if (!swipedThisGesture) {
                                    if (dragAmount.x < -dragThresholdPx) {
                                        currentYearMonth = currentYearMonth.plusMonths(1)
                                        swipedThisGesture = true
                                    } else if (dragAmount.x > dragThresholdPx) {
                                        currentYearMonth = currentYearMonth.minusMonths(1)
                                        swipedThisGesture = true
                                    }
                                }
                            }
                        )
                    }
            ) {
                Column {
                    // Skróty dni tygodnia
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val daysOfWeek = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "Sb", "Nd")
                        daysOfWeek.forEach { dayName ->
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        userScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(calendarCells) { _, day ->
                            if (day == null) {
                                Spacer(modifier = Modifier.aspectRatio(1f))
                            } else {
                                val dayActivities = currentMonthActivities[day].orEmpty()
                                DayCell(
                                    day = day,
                                    activities = dayActivities,
                                    currentYearMonth = currentYearMonth,
                                    onClick = {
                                        // Creating a new event on this day
                                        editingDay = day
                                        editingEvent = null
                                        activityTitle = ""
                                        category = ""
                                        chosenColor = Color(0xFFDCE775)
                                        selectedTime = null
                                        timeText = ""
                                        hasNotification = false
                                        notificationMinutes = null
                                        showEventDialog = true
                                    },
                                    onLongClick = {
                                        // Alternatively, show day events (if you like)
                                        selectedDay = day
                                        dayEvents = dayActivities
                                        showDayEventsDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2) Sekcja wydarzeń w miesiącu
            Spacer(modifier = Modifier.height(16.dp))

            if (events.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Wydarzenia w miesiącu",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                // 3) Lista wydarzeń - jedyny pionowy scroll
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events.sortedBy { it.timestamp }) { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Editing existing event
                                    editingEvent = event
                                    editingDay = null // we won't use editingDay for existing
                                    activityTitle = event.title
                                    category = event.category ?: ""
                                    chosenColor = event.color
                                    selectedTime = event.time
                                    timeText = event.time?.format(
                                        DateTimeFormatter.ofPattern("HH:mm")
                                    ) ?: ""
                                    hasNotification = event.hasNotification
                                    notificationMinutes = event.notificationTime
                                    showEventDialog = true
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(event.color)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = event.title,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = Color.Black
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    if (event.category != null) {
                                                        Text(
                                                            text = event.category!!,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "Brak kategorii",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        if (event.hasNotification) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .background(Color.Red, CircleShape)
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                // Delete event
                                                                deleteEvent(event, context)
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(id = R.drawable.ic_delete),
                                                                contentDescription = "Usuń wydarzenie",
                                                                tint = Color.Gray,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { deleteEvent(event, context) }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_delete),
                                            contentDescription = "Usuń wydarzenie",
                                            tint = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Brak wydarzeń
                Text(
                    text = "Brak wydarzeń w tym miesiącu",
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3DD1C6))
                        .padding(8.dp)
                )
            }
        }
    }

    // ------------------ Dialog: Wydarzenia w wybranym dniu ------------------
    if (showDayEventsDialog && selectedDay != null) {
        AlertDialog(
            onDismissRequest = { showDayEventsDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Column {
                    Text(
                        text = "Wydarzenia",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$selectedDay.${currentYearMonth.monthValue}.${currentYearMonth.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (dayEvents.isEmpty()) {
                        Text(
                            "Brak wydarzeń.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        dayEvents.forEach { event ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = event.color.copy(alpha = 0.9f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .clickable {
                                                // Edit from DayEvents
                                                editingEvent = event
                                                editingDay = null
                                                activityTitle = event.title
                                                category = event.category ?: ""
                                                chosenColor = event.color
                                                selectedTime = event.time
                                                timeText = event.time?.format(
                                                    DateTimeFormatter.ofPattern("HH:mm")
                                                ) ?: ""
                                                hasNotification = event.hasNotification
                                                notificationMinutes = event.notificationTime
                                                showEventDialog = true
                                                showDayEventsDialog = false
                                            }
                                    ) {
                                        Text("Opis: ${event.title}", fontWeight = FontWeight.Bold)
                                        val date = LocalDate.of(event.year, event.month, event.day)
                                        val formattedDate = date.format(
                                            DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                        )
                                        Text("Data: $formattedDate")
                                        event.time?.let { time ->
                                            Text(time.format(DateTimeFormatter.ofPattern("HH:mm")))
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            deleteEvent(event, context)
                                            showDayEventsDialog = false
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Usuń",
                                            tint = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        // Add new event quickly
                        editingEvent = null
                        editingDay = selectedDay
                        activityTitle = ""
                        category = ""
                        chosenColor = Color(0xFFDCE775)
                        selectedTime = null
                        timeText = ""
                        hasNotification = false
                        notificationMinutes = null
                        showEventDialog = true
                        showDayEventsDialog = false
                    }) {
                        Text("Dodaj nowe wydarzenie")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDayEventsDialog = false }) {
                    Text("Zamknij")
                }
            }
        )
    }

    // ------------------ Dialog: Dodawanie / Edycja wydarzenia ------------------
    if (showEventDialog) {
        var showTimePickerDialog by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEventDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (editingEvent == null) "Nowe wydarzenie" else "Edycja wydarzenia",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // If editingEvent == null, that means we are adding a new event using editingDay
                    if (editingEvent == null && editingDay != null) {
                        Text(
                            text = "${editingDay}.${currentYearMonth.monthValue}.${currentYearMonth.year}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (editingEvent != null) {
                        // Show the old date from the event
                        Text(
                            text = "${editingEvent!!.day}.${editingEvent!!.month}.${editingEvent!!.year}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = activityTitle,
                        onValueChange = { activityTitle = it },
                        label = { Text("Nazwa wydarzenia") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    var expanded by remember { mutableStateOf(false) }
                    var customCategory by remember { mutableStateOf(false) }

                    if (!customCategory) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Wybierz kategorię") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                defaultCategories.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            category = option
                                            expanded = false
                                        }
                                    )
                                }
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Własna kategoria") },
                                    onClick = {
                                        customCategory = true
                                        expanded = false
                                    }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Własna kategoria") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    customCategory = false
                                    category = ""
                                }) {
                                    Icon(Icons.Default.Clear, "Powrót do listy")
                                }
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Powiadomienie:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = notificationTimeExpanded,
                            onExpandedChange = { notificationTimeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = when(notificationMinutes) {
                                    5 -> "5 minut przed"
                                    15 -> "15 minut przed"
                                    30 -> "30 minut przed"
                                    60 -> "1 godzina przed"
                                    1440 -> "1 dzień przed"
                                    else -> "Brak powiadomienia"
                                },
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = notificationTimeExpanded,
                                onDismissRequest = { notificationTimeExpanded = false }
                            ) {
                                listOf(null, 5, 15, 30, 60, 1440).forEach { minutes ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(when(minutes) {
                                                5 -> "5 minut przed"
                                                15 -> "15 minut przed"
                                                30 -> "30 minut przed"
                                                60 -> "1 godzina przed"
                                                1440 -> "1 dzień przed"
                                                else -> "Brak powiadomienia"
                                            })
                                        },
                                        onClick = {
                                            notificationMinutes = minutes
                                            notificationTimeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Text("Wybierz kolor:", style = MaterialTheme.typography.bodyMedium)
                    ColorPickerRow(
                        onColorSelected = { chosenColor = it },
                        selectedColor = chosenColor
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = timeText,
                            onValueChange = { timeText = it },
                            label = { Text("Godzina (np. 14:30)") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showTimePickerDialog = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Wybierz godzinę")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Powiadomienie:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        Checkbox(
                            checked = hasNotification,
                            onCheckedChange = {
                                hasNotification = it
                                // Optionally set a default if turning on
                                if (it && notificationMinutes == null) {
                                    notificationMinutes = 15
                                }
                            }
                        )
                    }

                    // Parsujemy "HH:mm"
                    LaunchedEffect(timeText) {
                        selectedTime = try {
                            LocalTime.parse(timeText, DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    // If adding a brand-new event
                    if (editingEvent == null) {
                        if (editingDay == null) {
                            Toast.makeText(context, "Nie wybrano dnia!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (activityTitle.isBlank()) {
                            Toast.makeText(context, "Uzupełnij nazwę wydarzenia!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val dataMap = hashMapOf(
                            "userId" to userId,
                            "title" to activityTitle,
                            "category" to (if (category.isBlank()) null else category),
                            "time" to (selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: null),
                            "color" to chosenColor.toArgb(),
                            "timestamp" to System.currentTimeMillis(),
                            "createdAt" to Date(),
                            "hasNotification" to hasNotification,
                            "notificationTime" to (notificationMinutes ?: 0),
                            "groupId" to userId,
                            "year" to currentYearMonth.year,
                            "month" to currentYearMonth.monthValue,
                            "day" to editingDay!!  // Safe because we checked above
                        )
                        db.collection("events")
                            .add(dataMap)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Dodano: $activityTitle", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Błąd zapisu: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // We are editing an existing event
                        if (activityTitle.isBlank()) {
                            Toast.makeText(context, "Uzupełnij nazwę wydarzenia!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // We'll keep the same day/month/year from the original event
                        val finalDay = editingEvent!!.day
                        val finalMonth = editingEvent!!.month
                        val finalYear = editingEvent!!.year

                        editingEvent!!.title = activityTitle
                        editingEvent!!.color = chosenColor
                        editingEvent!!.category = if (category.isBlank()) null else category
                        editingEvent!!.time = selectedTime
                        editingEvent!!.hasNotification = hasNotification
                        editingEvent!!.notificationTime = notificationMinutes

                        editingEvent!!.docId?.let { docId ->
                            val dataMap = hashMapOf(
                                "userId" to userId,
                                "title" to editingEvent!!.title,
                                "category" to editingEvent!!.category,
                                "time" to (selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: null),
                                "color" to chosenColor.toArgb(),
                                "timestamp" to System.currentTimeMillis(),
                                "createdAt" to Date(),
                                "hasNotification" to hasNotification,
                                "notificationTime" to (notificationMinutes ?: 0),
                                "groupId" to (editingEvent!!.groupId ?: userId),

                                // Reuse original date
                                "year" to finalYear,
                                "month" to finalMonth,
                                "day" to finalDay
                            )
                            db.collection("events").document(docId).set(dataMap)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Zaktualizowano: $activityTitle", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Błąd aktualizacji: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }

                    // Reset states
                    editingEvent = null
                    editingDay = null
                    activityTitle = ""
                    chosenColor = Color(0xFFDCE775)
                    category = ""
                    selectedTime = null
                    timeText = ""
                    hasNotification = false
                    notificationMinutes = null
                    showEventDialog = false
                }) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingEvent = null
                    editingDay = null
                    activityTitle = ""
                    chosenColor = Color(0xFFDCE775)
                    category = ""
                    selectedTime = null
                    timeText = ""
                    hasNotification = false
                    notificationMinutes = null
                    showEventDialog = false
                }) {
                    Text("Anuluj")
                }
            },
            properties = DialogProperties()
        )

        if (showTimePickerDialog) {
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val localTime = LocalTime.of(hour, minute)
                    timeText = localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    showTimePickerDialog = false
                },
                selectedTime?.hour ?: 12,
                selectedTime?.minute ?: 0,
                true
            ).show()
        }
    }
}

// ----------------- KOMPONENT: Komórka jednego dnia w kalendarzu -----------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayCell(
    day: Int,
    activities: List<ActivityData>,
    currentYearMonth: YearMonth,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val today = LocalDate.now()
    val isCurrentDay = (
            today.year == currentYearMonth.year &&
                    today.monthValue == currentYearMonth.monthValue &&
                    today.dayOfMonth == day
            )
    val borderWidth = if (isCurrentDay) 2.dp else 1.dp
    val borderColor = if (isCurrentDay) MaterialTheme.colorScheme.primary else Color.LightGray
    val hasActivities = activities.isNotEmpty()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                color = if (isCurrentDay) MaterialTheme.colorScheme.primary else Color.Black,
                fontWeight = if (isCurrentDay) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodySmall
            )
            if (hasActivities) {
                Spacer(modifier = Modifier.height(1.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(activities.first().color)
                )
            }
        }
    }
}

// Pasek do wyboru miesiąca i roku
@Composable
fun MonthYearBar(
    currentYearMonth: YearMonth,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthName = polishMonthNames[currentYearMonth.monthValue] ?: ""
    val year = currentYearMonth.year

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Poprzedni miesiąc")
        }
        Text(
            text = "$monthName $year",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Następny miesiąc")
        }
    }
}

// Rząd kolorów
@Composable
fun ColorPickerRow(
    onColorSelected: (Color) -> Unit,
    selectedColor: Color
) {
    val availableColors = listOf(
        Color(0xFFFFCDD2), // jasny róż
        Color(0xFFDCE775), // limonka
        Color(0xFFB2EBF2), // jasny błękit
        Color(0xFFFFF59D), // jasny żółty
        Color(0xFFFFAB91), // łosoś
        Color(0xFFC5E1A5), // jasna zieleń
        Color(0xFFE1BEE7)  // jasny fiolet
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        availableColors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onColorSelected(color) }
                    .border(
                        width = if (color == selectedColor) 3.dp else 1.dp,
                        color = if (color == selectedColor) Color.Black else Color.White,
                        shape = CircleShape
                    )
            )
        }
    }
}

fun logout() {
    auth.signOut()
}

fun deleteEvent(event: ActivityData, context: android.content.Context) {
    val docId = event.docId
    if (docId != null) {
        db.collection("events").document(docId).delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Wydarzenie usunięte", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Błąd usuwania: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
