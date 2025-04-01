package pl.podkal.domowniczeqqq.home

import android.app.TimePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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

// ---------------------- MODELE i OBIEKT PRZECHOWUJĄCY DANE ----------------------

data class ActivityData(
    var docId: String? = null, // do usuwania/edycji w Firestore
    var title: String,
    var color: Color,
    var category: String? = null,
    var time: LocalTime? = null,
    var timestamp: Long = System.currentTimeMillis()
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
    var editingDay by remember { mutableStateOf<Int?>(null) }

    var activityTitle by remember { mutableStateOf("") }
    var chosenColor by remember { mutableStateOf(Color(0xFFDCE775)) }
    var category by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var timeText by remember { mutableStateOf("") }

    var showDayEventsDialog by remember { mutableStateOf(false) }
    var dayEvents by remember { mutableStateOf(emptyList<ActivityData>()) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    var events by remember { mutableStateOf<List<ActivityData>>(emptyList()) }

    // -------------- Firestore --------------
    DisposableEffect(userId, currentYearMonth) {
        val listener = db.collection("events")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Błąd odczytu: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val newMonthMap = mutableMapOf<YearMonth, MutableMap<Int, MutableList<ActivityData>>>()

                for (doc in snapshot.documents) {
                    val docUserId = doc.getString("userId") ?: continue
                    if (docUserId != userId) continue

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

                    val activityData = ActivityData(
                        docId = doc.id,
                        title = doc.getString("title") ?: "",
                        color = color,
                        category = doc.getString("category"),
                        time = localTime,
                        timestamp = timestamp
                    )

                    val ym = YearMonth.of(year, month)
                    val daysMap = newMonthMap.getOrPut(ym) { mutableMapOf() }
                    val dayList = daysMap.getOrPut(day) { mutableListOf() }
                    dayList.add(activityData)
                }

                CalendarEventsManager.userData[userId] = newMonthMap
                // Wydarzenia dla bieżącego YearMonth
                events = newMonthMap[currentYearMonth]?.values?.flatten() ?: emptyList()
            }

        onDispose { listener.remove() }
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
        // Kolumna główna (bez scrolla)
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

            // 1) KALENDARZ - stała wysokość (np. 300.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // <-- stała wysokość
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
                // LazyVerticalGrid z userScrollEnabled = false,
                // aby uniknąć drugiego scrolla w pionie
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
                        userScrollEnabled = false,  // <--- klucz, by nie generować pionowego scrolla
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
                                        if (dayActivities.isEmpty()) {
                                            editingEvent = null
                                            editingDay = day
                                            activityTitle = ""
                                            category = ""
                                            chosenColor = Color(0xFFDCE775)
                                            selectedTime = null
                                            timeText = ""
                                            showEventDialog = true
                                        } else {
                                            selectedDay = day
                                            dayEvents = dayActivities
                                            showDayEventsDialog = true
                                        }
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
                                    // Edycja
                                    editingEvent = event
                                    editingDay = null
                                    activityTitle = event.title
                                    category = event.category ?: ""
                                    chosenColor = event.color
                                    selectedTime = event.time
                                    timeText = event.time?.format(
                                        DateTimeFormatter.ofPattern("HH:mm")
                                    ) ?: ""
                                    showEventDialog = true
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
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
                                    event.category?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                event.time?.let {
                                    Text(
                                        text = it.format(DateTimeFormatter.ofPattern("HH:mm")),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                                IconButton(onClick = {
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
                                    // Usuń lokalnie
                                    currentMonthActivities.values.forEach { list ->
                                        list.remove(event)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Usuń",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
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
                                                editingEvent = event
                                                editingDay = selectedDay
                                                activityTitle = event.title
                                                category = event.category ?: ""
                                                chosenColor = event.color
                                                selectedTime = event.time
                                                timeText = event.time?.format(
                                                    DateTimeFormatter.ofPattern("HH:mm")
                                                ) ?: ""
                                                showEventDialog = true
                                                showDayEventsDialog = false
                                            }
                                    ) {
                                        Text("Opis: ${event.title}", fontWeight = FontWeight.Bold)
                                        val date = LocalDate.of(
                                            currentYearMonth.year,
                                            currentYearMonth.monthValue,
                                            selectedDay!!
                                        )
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
                                            val list = currentMonthActivities[selectedDay]
                                            list?.remove(event)
                                            if (list.isNullOrEmpty()) {
                                                currentMonthActivities.remove(selectedDay)
                                            }
                                            // Usuń w Firestore
                                            val docId = event.docId
                                            if (docId != null) {
                                                db.collection("events").document(docId).delete()
                                                    .addOnSuccessListener {
                                                        Toast.makeText(
                                                            context,
                                                            "Wydarzenie usunięte",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(
                                                            context,
                                                            "Błąd usuwania: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            }
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
                        editingEvent = null
                        editingDay = selectedDay
                        activityTitle = ""
                        category = ""
                        chosenColor = Color(0xFFDCE775)
                        selectedTime = null
                        timeText = ""
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
                    editingDay?.let { dayNum ->
                        Text(
                            text = "$dayNum.${currentYearMonth.monthValue}.${currentYearMonth.year}",
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

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategoria (opcjonalnie)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
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

                    Text("Wybierz kolor:", style = MaterialTheme.typography.bodyMedium)
                    ColorPickerRow(
                        onColorSelected = { chosenColor = it },
                        selectedColor = chosenColor
                    )

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
                    if (editingDay == null && editingEvent == null) {
                        Toast.makeText(context, "Nie wybrano dnia!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (activityTitle.isBlank()) {
                        Toast.makeText(context, "Uzupełnij nazwę wydarzenia!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Dodaj nowe
                    if (editingEvent == null) {
                        val dataMap = hashMapOf(
                            "userId" to userId,
                            "title" to activityTitle,
                            "category" to (category.ifBlank { null } ?: ""),
                            "time" to (selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: null),
                            "color" to chosenColor.toArgb(),
                            "timestamp" to System.currentTimeMillis(),
                            "createdAt" to Date(),
                            "year" to currentYearMonth.year,
                            "month" to currentYearMonth.monthValue,
                            "day" to editingDay!!
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
                        // Edycja lokalna
                        editingEvent!!.title = activityTitle
                        editingEvent!!.color = chosenColor
                        editingEvent!!.category = category.ifBlank { null }
                        editingEvent!!.time = selectedTime

                        Toast.makeText(context, "Zaktualizowano: $activityTitle (lokalnie)", Toast.LENGTH_SHORT).show()
                    }

                    editingEvent = null
                    editingDay = null
                    activityTitle = ""
                    chosenColor = Color(0xFFDCE775)
                    category = ""
                    selectedTime = null
                    timeText = ""
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
@Composable
fun DayCell(
    day: Int,
    activities: List<ActivityData>,
    currentYearMonth: YearMonth,
    onClick: () -> Unit
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
            .clickable { onClick() }
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
            .padding(horizontal = 4.dp, vertical = 2.dp), // minimalne marginesy
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