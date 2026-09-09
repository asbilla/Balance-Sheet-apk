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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.pref.BusinessProfile
import com.example.data.repository.TransactionRepository
import com.example.ui.theme.BalanceBlue
import com.example.ui.theme.IncomeGreen
import com.example.util.GoogleAppsScriptSnippet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    repository: TransactionRepository,
    onConfigured: () -> Unit,
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

    var urlInput by remember { mutableStateOf(repository.getWebAppUrl()) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }
    var showCodeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sheet Integration Setup",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. BUSINESS PROFILE DETAILS (ON TOP AS REQUESTED)
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

            // 2. GOOGLE SHEETS BACKEND CONNECTION
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(BalanceBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Link Icon",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Google Sheets Backend",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connect to your deployed Google Apps Script Web App to sync daily business reports.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // URL input field
            Text(
                text = "Web App URL",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = urlInput,
                onValueChange = {
                    urlInput = it
                    testResult = null
                    testSuccess = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("webapp_url_input"),
                placeholder = {
                    Text(
                        text = "https://script.google.com/macros/s/.../exec",
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "URL",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (urlInput.isNotEmpty()) {
                        IconButton(onClick = {
                            urlInput = ""
                            testResult = null
                            testSuccess = null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear URL"
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = false,
                maxLines = 3
            )

            // Test Connection Button & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val trimmed = urlInput.trim()
                        if (trimmed.isEmpty()) {
                            Toast.makeText(context, "Please enter a Web App URL first", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        isTesting = true
                        testResult = null
                        testSuccess = null
                        scope.launch {
                            val res = repository.testConnection(trimmed)
                            isTesting = false
                            if (res.isSuccess) {
                                testSuccess = true
                                testResult = "Connected! Google Sheets responded successfully."
                            } else {
                                testSuccess = false
                                testResult = "Connection failed: ${res.exceptionOrNull()?.localizedMessage ?: "Unknown error"}"
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_connection_button"),
                    enabled = urlInput.isNotBlank() && !isTesting,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Test",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Connection")
                    }
                }

                OutlinedButton(
                    onClick = {
                        val clipData = clipboardManager.getText()?.text
                        if (!clipData.isNullOrBlank()) {
                            urlInput = clipData.trim()
                            Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("paste_url_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Paste",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste")
                }
            }

            // Test Feedback Alert
            AnimatedVisibility(visible = testResult != null) {
                testResult?.let { msg ->
                    val isSuccess = testSuccess == true
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = if (isSuccess) "Success" else "Error",
                                tint = if (isSuccess) IncomeGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = msg,
                                color = if (isSuccess) Color(0xFF14532D) else Color(0xFF7F1D1D),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Save & Continue Action Button
            Button(
                onClick = {
                    val trimmed = urlInput.trim()
                    if (trimmed.isBlank()) {
                        Toast.makeText(context, "Please enter a valid Web App URL", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val profile = BusinessProfile(
                        businessName = businessName.trim(),
                        abnAcn = abnAcn.trim(),
                        businessAddress = businessAddress.trim(),
                        phoneMobile = phoneMobile.trim(),
                        email = email.trim()
                    )
                    repository.setWebAppUrl(trimmed)
                    scope.launch {
                        repository.saveBusinessProfile(profile, syncToSheets = trimmed.startsWith("https://script.google.com/"))
                    }
                    Toast.makeText(context, "Business Profile & Configuration Saved!", Toast.LENGTH_SHORT).show()
                    onConfigured()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_and_continue_button"),
                enabled = urlInput.trim().startsWith("https://script.google.com/"),
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

            // Skip for Demo button if user wants to test offline local Room DB first
            if (!repository.isConfigured()) {
                TextButton(
                    onClick = {
                        val profile = BusinessProfile(
                            businessName = businessName.trim(),
                            abnAcn = abnAcn.trim(),
                            businessAddress = businessAddress.trim(),
                            phoneMobile = phoneMobile.trim(),
                            email = email.trim()
                        )
                        // User can continue with offline local database
                        repository.setWebAppUrl("https://script.google.com/macros/s/offline-demo/exec")
                        scope.launch {
                            repository.saveBusinessProfile(profile, syncToSheets = false)
                        }
                        Toast.makeText(context, "Offline Mode: Business details stored in local database", Toast.LENGTH_LONG).show()
                        onConfigured()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Continue in Offline-Only Mode (Configure Later)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Google Apps Script Setup Guide
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Instructions",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How to Deploy Google Sheet API",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Text(
                        text = "1. Open Google Sheets and go to Extensions > Apps Script.\n" +
                               "2. Paste the provided backend code snippet.\n" +
                               "3. Click Deploy > New Deployment > Web app.\n" +
                               "4. Set 'Execute as: Me' and 'Who has access: Anyone'.\n" +
                               "5. Copy the generated Web App URL and paste above.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showCodeDialog = !showCodeDialog },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Code",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showCodeDialog) "Hide Apps Script Code" else "View & Copy Apps Script Code",
                            fontSize = 13.sp
                        )
                    }

                    AnimatedVisibility(visible = showCodeDialog) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Code.gs",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(GoogleAppsScriptSnippet.CODE))
                                        Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Full Code", fontSize = 12.sp)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = GoogleAppsScriptSnippet.CODE.take(500) + "\n\n... (Click 'Copy Full Code' above to copy all)",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
