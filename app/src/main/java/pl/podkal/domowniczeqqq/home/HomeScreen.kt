package pl.podkal.domowniczeqqq.home

import android.app.TimePickerDialog
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import pl.podkal.domowniczeq.R
import pl.podkal.domowniczeqqq.navigation.BottomNavBar
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ---------------------- MODELE i OBIEKT PRZECHOWUJĄCY DANE ----------------------

data class ActivityData(
    var title: String,
    var color: Color,
    var category: String?,
    var time: LocalTime?
)

// Pojedyncza „pół-trwała” przechowalnia kalendarza
// (Jeśli chcesz mieć dane również po wylogowaniu/zamknięciu aplikacji, użyj bazy danych lub Firestore)
object CalendarEventsManager {
    // Mapowanie: userId -> (YearMonth -> mapa: dzień -> lista eventów)
    val userData = mutableStateMapOf<String, MutableMap<YearMonth, MutableMap<Int, MutableList<ActivityData>>>>()
}

// Firebase auth
val auth = Firebase.auth

// Mapa nazw miesięcy po polsku
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

    // By wyłączyć standardowy przycisk wstecz:
    BackHandler { /* Ignoruj cofanie */ }

    // Aktualny miesiąc (obserwowalny stan)
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(LocalDate.now())) }

    // Pobierz (lub utwórz) mapę dla zalogowanego usera
    val currentUserMap = CalendarEventsManager.userData.getOrPut(userId) {
        mutableStateMapOf()
    }

    // Z mapy usera pobierz/utwórz mapę dla bieżącego YearMonth
    val currentMonthActivities = currentUserMap.getOrPut(currentYearMonth) {
        mutableStateMapOf()
    }

    // Kalendarz: oblicz dni, wypełnij 42 pola (6 tygodni x 7 dni)
    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value // pon=1 ... nd=7
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

    // Stany do dialogów
    var showEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<ActivityData?>(null) }
    var editingDay by remember { mutableStateOf<Int?>(null) }

    var activityTitle by remember { mutableStateOf("") }
    var chosenColor by remember { mutableStateOf(Color(0xFFDCE775)) }
    var category by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var timeText by remember { mutableStateOf("") }

    // Dialog z wydarzeniami konkretnego dnia
    var showDayEventsDialog by remember { mutableStateOf(false) }
    var dayEvents by remember { mutableStateOf(emptyList<ActivityData>()) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = Color(0xFFF8F8F8),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3DD1C6)
                ),
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
                        // Przejdź do ekranu logowania
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
            // Pasek z wyborem miesiąca i roku
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

            // Obsługa przesunięć w lewo/prawo (1 miesiąc)
            val dragThresholdPx = with(LocalDensity.current) { 40.dp.toPx() }
            var swipedThisGesture by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { swipedThisGesture = false },
                            onDragEnd = { swipedThisGesture = false },
                            onDragCancel = { swipedThisGesture = false },
                            onDrag = { _, dragAmount ->
                                // Jeśli jeszcze nie przesunęliśmy w tym geście, sprawdzamy threshold
                                if (!swipedThisGesture) {
                                    if (dragAmount.x < -dragThresholdPx) {
                                        // W lewo -> kolejny miesiąc
                                        currentYearMonth = currentYearMonth.plusMonths(1)
                                        swipedThisGesture = true
                                    } else if (dragAmount.x > dragThresholdPx) {
                                        // W prawo -> poprzedni miesiąc
                                        currentYearMonth = currentYearMonth.minusMonths(1)
                                        swipedThisGesture = true
                                    }
                                }
                            }
                        )
                    }
            ) {
                Column {
                    // Nagłówek dni tygodnia (pn-nd)
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

                    // Siatka dni (42 pola)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(calendarCells) { _, day ->
                            if (day == null) {
                                Spacer(modifier = Modifier.aspectRatio(1f))
                            } else {
                                val dayActivities = currentMonthActivities[day].orEmpty()
                                val dayColor = dayActivities.lastOrNull()?.color ?: Color.White
                                DayCell(
                                    day = day,
                                    activities = dayActivities,
                                    backgroundColor = dayColor,
                                    currentYearMonth = currentYearMonth,
                                    onClick = {
                                        // Jeśli brak wydarzeń -> pokaż dialog dodawania
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
                                            // Jeśli są -> pokaż dialog z listą wydarzeń
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

            Spacer(modifier = Modifier.height(24.dp))

            // --- Lista wszystkich wydarzeń w miesiącu (flatten) ---
            val monthEventsMap = currentMonthActivities.toList().sortedBy { it.first }
            val flattenedEvents = monthEventsMap.flatMap { (day, events) ->
                events.map { day to it }
            }

            if (flattenedEvents.isNotEmpty()) {
                Text(
                    text = "Wydarzenia w miesiącu",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3DD1C6))
                        .padding(8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(flattenedEvents) { (day, event) ->
                        val date = LocalDate.of(
                            currentYearMonth.year,
                            currentYearMonth.monthValue,
                            day
                        )
                        val formattedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            colors = CardDefaults.cardColors(containerColor = event.color)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .clickable {
                                            // Edycja
                                            editingEvent = event
                                            editingDay = day
                                            activityTitle = event.title
                                            category = event.category ?: ""
                                            chosenColor = event.color
                                            selectedTime = event.time
                                            timeText = event.time?.format(
                                                DateTimeFormatter.ofPattern("HH:mm")
                                            ) ?: ""
                                            showEventDialog = true
                                        }
                                ) {
                                    Text("Opis: ${event.title}", fontWeight = FontWeight.Bold)
                                    Text("Data: $formattedDate")
                                    event.time?.let { time ->
                                        Text(time.format(DateTimeFormatter.ofPattern("HH:mm")))
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val dayList = currentMonthActivities[day]
                                        dayList?.remove(event)
                                        if (dayList?.isEmpty() == true) {
                                            currentMonthActivities.remove(day)
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Usuń wydarzenie",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
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

    // ------------ Dialog: lista wydarzeń w wybranym dniu ------------
    if (showDayEventsDialog && selectedDay != null) {
        AlertDialog(
            onDismissRequest = { showDayEventsDialog = false },
            title = {
                Text("Wydarzenia dnia $selectedDay/${currentYearMonth.monthValue}/${currentYearMonth.year}")
            },
            text = {
                Column {
                    if (dayEvents.isEmpty()) {
                        Text("Brak wydarzeń.")
                    } else {
                        dayEvents.forEach { event ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = event.color)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .clickable {
                                                // Edycja
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
                                            val dayList = currentMonthActivities[selectedDay]
                                            if (dayList != null) {
                                                dayList.remove(event)
                                                if (dayList.isEmpty()) {
                                                    currentMonthActivities.remove(selectedDay)
                                                }
                                                // odśwież dayEvents
                                                dayEvents = dayList.toList()
                                            }
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

    // ------------ Dialog: dodawanie/edycja jednego wydarzenia ------------
    if (showEventDialog) {
        AlertDialog(
            onDismissRequest = { showEventDialog = false },
            title = {
                if (editingEvent == null) {
                    Text("Dodaj wydarzenie - dzień $editingDay/${currentYearMonth.monthValue}/${currentYearMonth.year}")
                } else {
                    Text("Edytuj wydarzenie - dzień $editingDay/${currentYearMonth.monthValue}/${currentYearMonth.year}")
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = activityTitle,
                        onValueChange = { activityTitle = it },
                        label = { Text("Nazwa wydarzenia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategoria (opcjonalnie)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = timeText,
                            onValueChange = { timeText = it },
                            label = { Text("Godzina (np. 14:30)") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val now = LocalTime.now()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    selectedTime = LocalTime.of(hour, minute)
                                    timeText = selectedTime!!
                                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                                },
                                now.hour,
                                now.minute,
                                true
                            ).show()
                        }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Wybierz godzinę")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Wybierz kolor:", style = MaterialTheme.typography.bodyMedium)
                    ColorPickerRow(
                        onColorSelected = { chosenColor = it },
                        selectedColor = chosenColor
                    )

                    // Parsuj ręcznie wpisaną godzinę
                    LaunchedEffect(timeText) {
                        selectedTime = try {
                            LocalTime.parse(timeText, DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editingDay == null) {
                        Toast.makeText(context, "Nie wybrano dnia!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (activityTitle.isBlank()) {
                        Toast.makeText(context, "Uzupełnij opis!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val currentMonthMap = currentUserMap.getOrPut(currentYearMonth) {
                        mutableStateMapOf()
                    }

                    if (editingEvent == null) {
                        // Dodawanie nowego
                        val newActivity = ActivityData(
                            title = activityTitle,
                            color = chosenColor,
                            category = category.ifBlank { null },
                            time = selectedTime
                        )
                        val dayList = currentMonthMap.getOrPut(editingDay!!) {
                            mutableStateListOf()
                        }
                        dayList.add(newActivity)
                        Toast.makeText(context, "Dodano: $activityTitle", Toast.LENGTH_SHORT).show()
                    } else {
                        // Edycja
                        editingEvent!!.title = activityTitle
                        editingEvent!!.color = chosenColor
                        editingEvent!!.category = category.ifBlank { null }
                        editingEvent!!.time = selectedTime
                        Toast.makeText(context, "Zaktualizowano: $activityTitle", Toast.LENGTH_SHORT).show()
                    }

                    // Reset
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
                Button(onClick = {
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
    }
}

// ------------------------- DODATKOWE KOMPONENTY --------------------------

// Pojedyncza "komórka" dnia w kalendarzu
@Composable
fun DayCell(
    day: Int,
    activities: List<ActivityData>,
    backgroundColor: Color,
    currentYearMonth: YearMonth,
    onClick: () -> Unit
) {
    val today = LocalDate.now()
    val isCurrentDay = (
            today.year == currentYearMonth.year &&
                    today.monthValue == currentYearMonth.monthValue &&
                    today.dayOfMonth == day
            )
    val finalColor = if (isCurrentDay) Color.Red else backgroundColor
    val borderColor = if (backgroundColor == Color.White) Color.LightGray else Color.Transparent
    val borderWidth = if (isCurrentDay) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(finalColor)
            .border(borderWidth, if (isCurrentDay) Color.Red else borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp)
        ) {
            Text(day.toString(), fontWeight = FontWeight.Bold)
            if (activities.isNotEmpty()) {
                // Dla przykładu pokazujemy tytuł ostatniego wydarzenia
                Text(
                    text = activities.last().title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
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
            .background(Color(0xFFFAF4F0))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Poprzedni miesiąc")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            var showMonthDialog by remember { mutableStateOf(false) }
            var showYearDialog by remember { mutableStateOf(false) }

            Text(
                text = monthName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showMonthDialog = true },
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable { showYearDialog = true },
                textAlign = TextAlign.Center
            )

            if (showMonthDialog) {
                ChooseMonthDialog(
                    onDismiss = { showMonthDialog = false },
                    onMonthSelected = {
                        onMonthChange(it)
                        showMonthDialog = false
                    }
                )
            }
            if (showYearDialog) {
                ChooseYearDialog(
                    currentYear = year,
                    onDismiss = { showYearDialog = false },
                    onYearSelected = {
                        onYearChange(it)
                        showYearDialog = false
                    }
                )
            }
        }

        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Następny miesiąc")
        }
    }
}

// Dialog z listą miesięcy
@Composable
fun ChooseMonthDialog(
    onDismiss: () -> Unit,
    onMonthSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wybierz miesiąc") },
        text = {
            Column {
                (1..12).forEach { monthNumber ->
                    val monthName = polishMonthNames[monthNumber] ?: ""
                    Text(
                        text = monthName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onMonthSelected(monthNumber) }
                    )
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

// Dialog z listą lat w zakresie +/- 5 od bieżącego
@Composable
fun ChooseYearDialog(
    currentYear: Int,
    onDismiss: () -> Unit,
    onYearSelected: (Int) -> Unit
) {
    val yearRange = (currentYear - 5)..(currentYear + 5)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wybierz rok") },
        text = {
            Column {
                for (year in yearRange) {
                    Text(
                        text = year.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onYearSelected(year) }
                    )
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

// Paleta kolorów
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

// Funkcja wylogowania
fun logout() {
    auth.signOut()
}
