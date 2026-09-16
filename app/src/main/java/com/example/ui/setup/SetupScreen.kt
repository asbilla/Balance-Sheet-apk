package com.example.ui.setup

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentSettings
import com.example.data.pref.BusinessProfile
import com.example.data.repository.TransactionRepository
import com.example.ui.theme.AppointmentPurple
import com.example.ui.theme.AppointmentPurpleContainer
import com.example.ui.theme.AppointmentPurpleText
import com.example.ui.theme.BalanceBlue
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import androidx.compose.material.icons.filled.Category
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    repository: TransactionRepository,
    onConfigured: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val initialProfile = remember { repository.getBusinessProfile() }
    var businessName by remember { mutableStateOf(initialProfile.businessName) }
    var abnAcn by remember { mutableStateOf(initialProfile.abnAcn) }
    var businessAddress by remember { mutableStateOf(initialProfile.businessAddress) }
    var phoneMobile by remember { mutableStateOf(initialProfile.phoneMobile) }
    var email by remember { mutableStateOf(initialProfile.email) }

    val initialAppointmentSettings = remember { repository.getAppointmentSettings() }
    var appointmentsEnabled by remember { mutableStateOf(initialAppointmentSettings.bookingEnabled) }
    var slotDurationMinutes by remember { mutableStateOf(initialAppointmentSettings.slotDurationMinutes) }
    var startHour by remember { mutableStateOf(initialAppointmentSettings.startHour) }
    var endHour by remember { mutableStateOf(initialAppointmentSettings.endHour) }
    var bufferMinutes by remember { mutableStateOf(initialAppointmentSettings.bufferMinutes) }
    var workingDays by remember { mutableStateOf(initialAppointmentSettings.workingDays) }

    val currentTheme by repository.themeMode.collectAsState(initial = repository.getThemeMode())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Business & System Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("setup_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // NEW & PROMINENT THEME SELECTION SECTION
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = BalanceBlue.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BalanceBlue.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = BalanceBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Choose Application Theme",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BalanceBlue
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionItem(
                            label = "System",
                            selected = currentTheme == "System",
                            onClick = { repository.setThemeMode("System") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionItem(
                            label = "Light",
                            selected = currentTheme == "Light",
                            onClick = { repository.setThemeMode("Light") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionItem(
                            label = "Dark",
                            selected = currentTheme == "Dark",
                            onClick = { repository.setThemeMode("Dark") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 1. BUSINESS PROFILE DETAILS
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BalanceBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Business Profile",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Business Details & Registration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Saved to Sheet1 of Spreadsheet & PDF Header",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 1st for Business Name:
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "1st for Business Name:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("business_name_input"),
                            placeholder = {
                                Text("Enter Business Name (e.g., Sunshine Bakery)", fontSize = 13.sp)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "Business Name",
                                    tint = BalanceBlue
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // 2nd for ABN/ACN:
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "2nd for ABN/ACN:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = abnAcn,
                            onValueChange = { abnAcn = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("abn_acn_input"),
                            placeholder = {
                                Text("Enter ABN or ACN (e.g., ABN 12 345 678 901)", fontSize = 13.sp)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = "ABN/ACN",
                                    tint = BalanceBlue
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // 3rd for Business Address:
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "3rd for Business Address:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = businessAddress,
                            onValueChange = { businessAddress = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("business_address_input"),
                            placeholder = {
                                Text("Enter Business Address (e.g., 120 Collins St, Melbourne VIC)", fontSize = 13.sp)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Business Address",
                                    tint = BalanceBlue
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 2
                        )
                    }

                    // 4th for Phone/Mobile:
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "4th for Phone/Mobile:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = phoneMobile,
                            onValueChange = { phoneMobile = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_mobile_input"),
                            placeholder = {
                                Text("Enter Phone or Mobile (e.g., +61 400 123 456)", fontSize = 13.sp)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Phone/Mobile",
                                    tint = BalanceBlue
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // 5th for email address:
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "5th for email address:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_address_input"),
                            placeholder = {
                                Text("Enter Email Address (e.g., contact@sunshinebakery.com)", fontSize = 13.sp)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email Address",
                                    tint = BalanceBlue
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // 2. CALENDAR APPOINTMENTS & TIME SLOT SETTINGS
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, AppointmentPurple.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AppointmentPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar Appointments",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Calendar Appointments & Time Slots",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Configure booking ability & client schedule slots",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Calendar Appointments Ability Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Calendar Appointments Ability",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appointmentsEnabled) "Enabled: Callers can book appointments" else "Disabled: Phone booking paused",
                                fontSize = 12.sp,
                                color = if (appointmentsEnabled) IncomeGreen else ExpenseRed
                            )
                        }
                        Switch(
                            checked = appointmentsEnabled,
                            onCheckedChange = { appointmentsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppointmentPurple
                            ),
                            modifier = Modifier.testTag("toggle_appointments_enabled")
                        )
                    }

                    if (appointmentsEnabled) {
                        // Time Slot Duration Selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Time Slot Duration:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15, 30, 45, 60).forEach { mins ->
                                    val isSelected = slotDurationMinutes == mins
                                    Surface(
                                        onClick = { slotDurationMinutes = mins },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) AppointmentPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) AppointmentPurple else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${mins} min",
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Daily Working Hours (Opening & Closing)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Daily Operating Hours:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Opening Time
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Opens At",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(8, 9, 10).forEach { h ->
                                            val isSelected = startHour == h
                                            Surface(
                                                onClick = { startHour = h },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) AppointmentPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${if (h < 10) "0$h" else "$h"}:00 AM",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Closing Time
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Closes At",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(17, 18, 19, 20).forEach { h ->
                                            val isSelected = endHour == h
                                            val displayH = if (h > 12) h - 12 else h
                                            Surface(
                                                onClick = { endHour = h },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) AppointmentPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "0${displayH}:00 PM",
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
                        }

                        // Buffer between appointments
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Rest / Buffer Between Slots:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0 to "None", 5 to "5 min", 10 to "10 min", 15 to "15 min").forEach { (bMins, label) ->
                                    val isSelected = bufferMinutes == bMins
                                    Surface(
                                        onClick = { bufferMinutes = bMins },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) AppointmentPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Working Days
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Working Days (Open for Bookings):",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val dayNames = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                dayNames.forEach { (dayIdx, name) ->
                                    val isWorking = workingDays.contains(dayIdx)
                                    Surface(
                                        onClick = {
                                            workingDays = if (isWorking) {
                                                if (workingDays.size > 1) workingDays - dayIdx else workingDays
                                            } else {
                                                workingDays + dayIdx
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isWorking) AppointmentPurpleContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isWorking) AppointmentPurple else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = name,
                                                fontSize = 11.sp,
                                                fontWeight = if (isWorking) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isWorking) AppointmentPurpleText else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Live Slot Preview
                        val previewSettings = remember(appointmentsEnabled, slotDurationMinutes, startHour, endHour, bufferMinutes, workingDays) {
                            AppointmentSettings(
                                bookingEnabled = appointmentsEnabled,
                                slotDurationMinutes = slotDurationMinutes,
                                startHour = startHour,
                                startMinute = 0,
                                endHour = endHour,
                                endMinute = 0,
                                bufferMinutes = bufferMinutes,
                                workingDays = workingDays
                            )
                        }
                        val sampleSlots = remember(previewSettings) {
                            repository.generateTimeSlots(previewSettings)
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Generated Schedule: ${sampleSlots.size} slots / day",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = AppointmentPurple
                                )
                                Text(
                                    text = sampleSlots.take(6).joinToString("  •  ") + if (sampleSlots.size > 6) "  •  ..." else "",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Save Appointment Settings Button
                    Button(
                        onClick = {
                            val newSettings = AppointmentSettings(
                                bookingEnabled = appointmentsEnabled,
                                slotDurationMinutes = slotDurationMinutes,
                                startHour = startHour,
                                startMinute = 0,
                                endHour = endHour,
                                endMinute = 0,
                                bufferMinutes = bufferMinutes,
                                workingDays = workingDays
                            )
                            repository.setAppointmentSettings(newSettings)
                            Toast.makeText(context, "Calendar & Time Slot Settings Saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppointmentPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_appointment_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Appointment Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 3. MENU & SERVICES MANAGEMENT
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFF4F46E5).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4F46E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Menu & Services",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Menu Items & Services",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add or edit products, pricing & categories",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "Customize your POS Quick Suggestions and service list. All items are stored locally on your device for fast access.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { onConfigured() }, // In this context onConfigured will navigate back or to Dashboard, but usually we'd have a separate nav
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Menu Manager", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Save & Continue Action Button
            Button(
                onClick = {
                    val profile = BusinessProfile(
                        businessName = businessName.trim(),
                        abnAcn = abnAcn.trim(),
                        businessAddress = businessAddress.trim(),
                        phoneMobile = phoneMobile.trim(),
                        email = email.trim()
                    )
                    val newSettings = AppointmentSettings(
                        bookingEnabled = appointmentsEnabled,
                        slotDurationMinutes = slotDurationMinutes,
                        startHour = startHour,
                        startMinute = 0,
                        endHour = endHour,
                        endMinute = 0,
                        bufferMinutes = bufferMinutes,
                        workingDays = workingDays
                    )
                    repository.setAppointmentSettings(newSettings)
                    scope.launch {
                        repository.saveBusinessProfile(profile, syncToSheets = false)
                    }
                    Toast.makeText(context, "Business Profile Saved!", Toast.LENGTH_SHORT).show()
                    onConfigured()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_and_continue_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BalanceBlue)
            ) {
                Text(
                    text = "Save & Open Dashboard",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun ThemeOptionItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) BalanceBlue else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) BalanceBlue else MaterialTheme.colorScheme.outlineVariant),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
