package com.example.ui.appointments

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppointmentEntity
import com.example.data.repository.TransactionRepository
import com.example.ui.theme.AppointmentPurple
import com.example.ui.theme.AppointmentPurpleContainer
import com.example.ui.theme.AppointmentPurpleText
import com.example.ui.theme.BalanceBlue
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    repository: TransactionRepository,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allAppointments by repository.allAppointments.collectAsStateWithLifecycle(initialValue = emptyList())
    val appointmentSettings by repository.appointmentSettings.collectAsStateWithLifecycle(initialValue = repository.getAppointmentSettings())
    val availableTimeSlots = remember(appointmentSettings) {
        repository.generateTimeSlots(appointmentSettings)
    }

    val todayIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var selectedDate by remember { mutableStateOf(todayIso) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") } // "All", "Scheduled", "Confirmed", "Completed", "Cancelled"
    var showAllDates by remember { mutableStateOf(false) }

    // Dialog / Bottom Sheet state
    var showBookingSheet by remember { mutableStateOf(false) }
    var appointmentToEdit by remember { mutableStateOf<AppointmentEntity?>(null) }
    var appointmentToDelete by remember { mutableStateOf<AppointmentEntity?>(null) }

    // Filter appointments
    val filteredAppointments = remember(allAppointments, selectedDate, showAllDates, selectedStatusFilter, searchQuery) {
        allAppointments.filter { appt ->
            val dateMatches = showAllDates || appt.appointmentDate == selectedDate
            val statusMatches = selectedStatusFilter == "All" || appt.status.equals(selectedStatusFilter, ignoreCase = true)
            val searchMatches = searchQuery.isBlank() ||
                    appt.customerName.contains(searchQuery, ignoreCase = true) ||
                    appt.customerPhone.contains(searchQuery, ignoreCase = true) ||
                    appt.serviceName.contains(searchQuery, ignoreCase = true) ||
                    appt.notes.contains(searchQuery, ignoreCase = true)
            dateMatches && statusMatches && searchMatches
        }
    }

    // Active booked slots for the selected date
    val bookedSlotsForSelectedDate = remember(allAppointments, selectedDate) {
        allAppointments
            .filter { it.appointmentDate == selectedDate && it.status != "Cancelled" }
            .map { it.appointmentTime }
            .toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Appointments & Bookings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (appointmentSettings.bookingEnabled) {
                                "${appointmentSettings.slotDurationMinutes}m slots • ${appointmentSettings.formatWorkingHours()}"
                            } else {
                                "Calendar Booking Disabled in Settings"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (appointmentSettings.bookingEnabled) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseRed
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("appointments_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("appointments_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Calendar Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    appointmentToEdit = null
                    showBookingSheet = true
                },
                containerColor = AppointmentPurple,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_book_appointment")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneCallback,
                        contentDescription = "Book Appointment"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Book Caller",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // PROMINENT QUICK CALLER BOOKING HERO CARD
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AppointmentPurpleContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, AppointmentPurple.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(AppointmentPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneCallback,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Client Calling in?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AppointmentPurpleText
                            )
                            Text(
                                text = "Tap to book date, slot & client info",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            appointmentToEdit = null
                            showBookingSheet = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppointmentPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("hero_book_caller_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Book Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // DATE SELECTOR HORIZONTAL STRIP
            DateStripSection(
                selectedDate = selectedDate,
                onSelectDate = {
                    selectedDate = it
                    showAllDates = false
                },
                showAllDates = showAllDates,
                onToggleAllDates = { showAllDates = !showAllDates },
                allAppointments = allAppointments,
                context = context
            )

            // SEARCH & STATUS FILTERS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search caller name, phone, service...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )

                // Status Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusList = listOf("All", "Scheduled", "Confirmed", "Completed", "Cancelled")
                    statusList.forEach { status ->
                        val isSelected = selectedStatusFilter == status
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStatusFilter = status },
                            label = {
                                Text(
                                    text = status,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppointmentPurple,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = AppointmentPurple
                            )
                        )
                    }
                }
            }

            // APPOINTMENTS LIST OR EMPTY STATE
            if (filteredAppointments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotBlank()) "No appointments match your search" else "No appointments for this date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tap 'Book Caller' when a client phones in to assign an available time slot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                appointmentToEdit = null
                                showBookingSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppointmentPurple)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Book First Appointment")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = if (showAllDates) "All Appointments (${filteredAppointments.size})" else "Bookings on $selectedDate (${filteredAppointments.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }

                    items(filteredAppointments, key = { it.id }) { appointment ->
                        AppointmentItemCard(
                            appointment = appointment,
                            onCall = { phone ->
                                dialPhone(context, phone)
                            },
                            onSms = { phone ->
                                sendSms(context, phone, appointment)
                            },
                            onEdit = {
                                appointmentToEdit = appointment
                                showBookingSheet = true
                            },
                            onDelete = {
                                appointmentToDelete = appointment
                            },
                            onStatusChange = { newStatus ->
                                scope.launch {
                                    repository.updateAppointmentStatus(appointment.id, newStatus)
                                    Toast.makeText(context, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
                    }
                }
            }
        }
    }

    // BOOKING / EDIT BOTTOM SHEET
    if (showBookingSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBookingSheet = false
                appointmentToEdit = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            BookAppointmentSheetContent(
                initialAppointment = appointmentToEdit,
                preselectedDate = selectedDate,
                availableTimeSlots = availableTimeSlots,
                bookedSlots = bookedSlotsForSelectedDate,
                cachedProducts = repository.getCachedProducts(),
                onDismiss = {
                    showBookingSheet = false
                    appointmentToEdit = null
                },
                onSave = { newOrUpdated ->
                    scope.launch {
                        if (newOrUpdated.id == 0L) {
                            repository.saveAppointment(newOrUpdated)
                            Toast.makeText(context, "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            repository.updateAppointment(newOrUpdated)
                            Toast.makeText(context, "Appointment updated!", Toast.LENGTH_SHORT).show()
                        }
                        showBookingSheet = false
                        appointmentToEdit = null
                    }
                },
                context = context
            )
        }
    }

    // DELETE CONFIRMATION DIALOG
    appointmentToDelete?.let { appt ->
        AlertDialog(
            onDismissRequest = { appointmentToDelete = null },
            title = { Text("Delete Appointment") },
            text = { Text("Are you sure you want to delete the appointment for ${appt.customerName} at ${appt.appointmentTime}?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteAppointment(appt)
                            Toast.makeText(context, "Appointment deleted", Toast.LENGTH_SHORT).show()
                            appointmentToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { appointmentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DateStripSection(
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    showAllDates: Boolean,
    onToggleAllDates: () -> Unit,
    allAppointments: List<AppointmentEntity>,
    context: Context
) {
    val calendar = Calendar.getInstance()
    val dateFormatIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dayNumberFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    val daysList = remember {
        val list = mutableListOf<DateInfo>()
        val cal = Calendar.getInstance()
        for (i in 0 until 14) {
            val d = cal.time
            list.add(
                DateInfo(
                    iso = dateFormatIso.format(d),
                    dayName = if (i == 0) "Today" else if (i == 1) "Tmrw" else dayNameFormat.format(d),
                    dayNum = dayNumberFormat.format(d),
                    month = monthFormat.format(d)
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All" chip
        Surface(
            onClick = onToggleAllDates,
            shape = RoundedCornerShape(12.dp),
            color = if (showAllDates) AppointmentPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            contentColor = if (showAllDates) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .height(60.dp)
                .padding(end = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ALL\nDates",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp
                )
            }
        }

        // Horizontal Days List
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            daysList.forEach { day ->
                val isSelected = !showAllDates && day.iso == selectedDate
                val countForDay = allAppointments.count { it.appointmentDate == day.iso && it.status != "Cancelled" }

                Surface(
                    onClick = { onSelectDate(day.iso) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) AppointmentPurple else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) AppointmentPurple else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .width(52.dp)
                        .height(60.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = day.dayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = day.dayNum,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        if (countForDay > 0) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else AppointmentPurple)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                    }
                }
            }
        }

        // Pick Custom Date button
        IconButton(
            onClick = {
                val cal = Calendar.getInstance()
                val dpd = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val picked = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth)
                        }
                        onSelectDate(dateFormatIso.format(picked.time))
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                dpd.show()
            },
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = "Pick Date",
                tint = AppointmentPurple
            )
        }
    }
}

data class DateInfo(
    val iso: String,
    val dayName: String,
    val dayNum: String,
    val month: String
)

@Composable
fun AppointmentItemCard(
    appointment: AppointmentEntity,
    onCall: (String) -> Unit,
    onSms: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val statusColor = when (appointment.status.lowercase()) {
        "confirmed" -> IncomeGreen
        "scheduled" -> BalanceBlue
        "completed" -> Color(0xFF64748B)
        "cancelled" -> ExpenseRed
        else -> MaterialTheme.colorScheme.primary
    }

    val statusBg = when (appointment.status.lowercase()) {
        "confirmed" -> Color(0xFFDCFCE7)
        "scheduled" -> Color(0xFFDBEAFE)
        "completed" -> Color(0xFFF1F5F9)
        "cancelled" -> Color(0xFFFEE2E2)
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar, Name, Time, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(statusBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = appointment.customerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${appointment.appointmentTime} (${appointment.durationMinutes}m)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${appointment.appointmentDate}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Status Badge & Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBg,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = appointment.status.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mark as Confirmed") },
                                onClick = {
                                    onStatusChange("Confirmed")
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mark as Completed") },
                                onClick = {
                                    onStatusChange("Completed")
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mark as Cancelled") },
                                onClick = {
                                    onStatusChange("Cancelled")
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = ExpenseRed)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Edit Details") },
                                onClick = {
                                    onEdit()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = ExpenseRed) },
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ExpenseRed)
                                }
                            )
                        }
                    }
                }
            }

            // Service & Price details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Service: ${appointment.serviceName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (appointment.price > 0) {
                    Text(
                        text = String.format(Locale.US, "$%.2f", appointment.price),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = IncomeGreen
                    )
                }
            }

            // Notes if present
            if (appointment.notes.isNotBlank()) {
                Text(
                    text = "📝 ${appointment.notes}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // CALLER PHONE ACTIONS ROW: One-tap Call & One-tap SMS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (appointment.customerPhone.isNotBlank()) appointment.customerPhone else "No phone provided",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (appointment.customerPhone.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Quick SMS
                        OutlinedButton(
                            onClick = { onSms(appointment.customerPhone) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Message, contentDescription = "SMS", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "SMS", fontSize = 12.sp)
                        }

                        // Quick Call
                        Button(
                            onClick = { onCall(appointment.customerPhone) },
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookAppointmentSheetContent(
    initialAppointment: AppointmentEntity?,
    preselectedDate: String,
    availableTimeSlots: List<String>,
    bookedSlots: Set<String>,
    cachedProducts: List<com.example.data.model.ProductItem>,
    onDismiss: () -> Unit,
    onSave: (AppointmentEntity) -> Unit,
    context: Context
) {
    var customerName by remember { mutableStateOf(initialAppointment?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(initialAppointment?.customerPhone ?: "") }
    var serviceName by remember { mutableStateOf(initialAppointment?.serviceName ?: "") }
    var appointmentDate by remember { mutableStateOf(initialAppointment?.appointmentDate ?: preselectedDate) }
    var appointmentTime by remember {
        mutableStateOf(initialAppointment?.appointmentTime ?: (availableTimeSlots.firstOrNull() ?: "09:00 AM"))
    }
    var durationMinutes by remember { mutableStateOf(initialAppointment?.durationMinutes ?: 30) }
    var status by remember { mutableStateOf(initialAppointment?.status ?: "Scheduled") }
    var notes by remember { mutableStateOf(initialAppointment?.notes ?: "") }
    var priceText by remember { mutableStateOf(if (initialAppointment != null && initialAppointment.price > 0) initialAppointment.price.toString() else "") }

    var customTimeInput by remember { mutableStateOf(false) }

    val isEditing = initialAppointment != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isEditing) "Edit Appointment" else "📞 Book Caller Appointment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fill client & schedule details while on phone call",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
        }

        HorizontalDivider()

        // 1. Caller Name & Phone
        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Caller / Customer Name *") },
            placeholder = { Text("e.g. Sarah Jenkins") },
            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_appointment_customer_name")
        )

        OutlinedTextField(
            value = customerPhone,
            onValueChange = { customerPhone = it },
            label = { Text("Caller Phone Number *") },
            placeholder = { Text("e.g. +61 400 123 456") },
            leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_appointment_customer_phone")
        )

        // 2. Service Selection (Quick chips from business catalog or manual typing)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Service Requested:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = serviceName,
                onValueChange = { serviceName = it },
                placeholder = { Text("e.g. Eyebrow Threading, Full Face, Tinting") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_appointment_service")
            )

            // Popular services quick chips
            if (cachedProducts.isNotEmpty()) {
                Text(
                    text = "Quick Select from Catalog:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quickServices = cachedProducts.take(8)
                    quickServices.forEach { item ->
                        val isSelected = serviceName.equals(item.name, ignoreCase = true)
                        Surface(
                            onClick = {
                                serviceName = item.name
                                if (priceText.isBlank() || priceText == "0.0") {
                                    priceText = String.format(Locale.US, "%.2f", item.price)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AppointmentPurpleContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) AppointmentPurple else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = "${item.name} ($${item.price.toInt()})",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AppointmentPurpleText else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Date Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Appointment Date:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = appointmentDate,
                    onValueChange = { appointmentDate = it },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val parts = appointmentDate.split("-")
                            val y = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                            val m = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1
                            val d = parts.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

                            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                                val picked = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                appointmentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(picked.time)
                            }, y, m, d).show()
                        }) {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 4. Time Slot Selection
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time Slot (From Settings):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = { customTimeInput = !customTimeInput },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (customTimeInput) "Choose from Slots" else "Custom Time",
                        fontSize = 12.sp,
                        color = AppointmentPurple
                    )
                }
            }

            if (customTimeInput) {
                OutlinedTextField(
                    value = appointmentTime,
                    onValueChange = { appointmentTime = it },
                    label = { Text("Custom Time (e.g. 10:15 AM)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                if (availableTimeSlots.isEmpty()) {
                    Text(
                        text = "No time slots configured. Set hours in Settings.",
                        fontSize = 12.sp,
                        color = ExpenseRed
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableTimeSlots.forEach { slot ->
                            val isSelected = appointmentTime.equals(slot, ignoreCase = true)
                            val isAlreadyBooked = bookedSlots.contains(slot) && !(isEditing && initialAppointment?.appointmentTime == slot)

                            Surface(
                                onClick = {
                                    appointmentTime = slot
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isSelected -> AppointmentPurple
                                    isAlreadyBooked -> Color(0xFFFEE2E2)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isSelected -> AppointmentPurple
                                        isAlreadyBooked -> ExpenseRed.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = slot,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> Color.White
                                            isAlreadyBooked -> ExpenseRed
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (isAlreadyBooked && !isSelected) {
                                        Text(
                                            text = " (Booked)",
                                            fontSize = 9.sp,
                                            color = ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Duration & Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Duration:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(15, 30, 45, 60).forEach { dur ->
                        val isSelected = durationMinutes == dur
                        Surface(
                            onClick = { durationMinutes = dur },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AppointmentPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${dur}m",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Status:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Scheduled", "Confirmed").forEach { st ->
                        val isSelected = status == st
                        Surface(
                            onClick = { status = st },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) (if (st == "Confirmed") IncomeGreen else BalanceBlue) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = st,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Price (Optional)
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Price ($ AUD, Optional)") },
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // 7. Caller Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Caller Notes / Requests") },
            placeholder = { Text("e.g. First-time client, prefers window seat, allergic to latex") },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Save Button
        Button(
            onClick = {
                if (customerName.isBlank()) {
                    Toast.makeText(context, "Please enter customer / caller name", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (customerPhone.isBlank()) {
                    Toast.makeText(context, "Please enter customer phone number", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val parsedPrice = priceText.toDoubleOrNull() ?: 0.0
                val entity = AppointmentEntity(
                    id = initialAppointment?.id ?: 0L,
                    customerName = customerName.trim(),
                    customerPhone = customerPhone.trim(),
                    serviceName = if (serviceName.isNotBlank()) serviceName.trim() else "Appointment",
                    appointmentDate = appointmentDate,
                    appointmentTime = appointmentTime.trim(),
                    durationMinutes = durationMinutes,
                    status = status,
                    notes = notes.trim(),
                    price = parsedPrice,
                    createdAt = initialAppointment?.createdAt ?: System.currentTimeMillis()
                )
                onSave(entity)
            },
            colors = ButtonDefaults.buttonColors(containerColor = AppointmentPurple),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_appointment_button")
        ) {
            Text(
                text = if (isEditing) "Save Changes" else "Confirm & Book Appointment",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun dialPhone(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to launch dialer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun sendSms(context: Context, phone: String, appointment: AppointmentEntity) {
    try {
        val message = "Hi ${appointment.customerName}, your appointment for ${appointment.serviceName} is scheduled on ${appointment.appointmentDate} at ${appointment.appointmentTime}."
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to launch SMS app: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
