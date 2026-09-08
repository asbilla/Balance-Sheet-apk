package com.example.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.TransactionRepository
import com.example.ui.theme.BalanceBlue
import com.example.ui.theme.BalanceBlueContainer
import com.example.ui.theme.BalanceBlueText
import com.example.ui.theme.BillOrange
import com.example.ui.theme.BillOrangeContainer
import com.example.ui.theme.BillOrangeText
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import com.example.ui.theme.ExpenseRedText
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import com.example.ui.theme.IncomeGreenText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: TransactionRepository,
    onNavigateToEntry: (entryType: String) -> Unit,
    onNavigateToBalanceSheet: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by repository.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val unsyncedCount by repository.unsyncedCount.collectAsStateWithLifecycle(initialValue = 0)

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Calculate today's summary
    val todayTransactions = transactions.filter { it.date == todayDate }
    val todayIncome = todayTransactions.filter { it.type == "Daily Income" }.sumOf { it.amount }
    val todayExpense = todayTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
    val todayBills = todayTransactions.filter { it.type == "Bill" }.sumOf { it.amount }
    val todayNet = todayIncome - (todayExpense + todayBills)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Business Report",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            repository.triggerBackgroundSync()
                            Toast.makeText(context, "Sync triggered...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("sync_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync",
                            tint = if (unsyncedCount > 0) BillOrange else IncomeGreen
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sync status pill / banner
            SyncStatusBanner(
                unsyncedCount = unsyncedCount,
                onSyncClick = {
                    repository.triggerBackgroundSync()
                    Toast.makeText(context, "Syncing unsynced entries with Sheets...", Toast.LENGTH_SHORT).show()
                }
            )

            // Today's Performance Snapshot Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Net Balance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (todayNet >= 0) IncomeGreenContainer else ExpenseRedContainer,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = if (todayNet >= 0) "SURPLUS" else "DEFICIT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (todayNet >= 0) IncomeGreenText else ExpenseRedText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", todayNet)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (todayNet >= 0) IncomeGreen else ExpenseRed
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 columns: Income, Expense, Bills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TodayMetricItem(
                            title = "Income",
                            amount = todayIncome,
                            color = IncomeGreen
                        )
                        TodayMetricItem(
                            title = "Expenses",
                            amount = todayExpense,
                            color = ExpenseRed
                        )
                        TodayMetricItem(
                            title = "Bills",
                            amount = todayBills,
                            color = BillOrange
                        )
                    }
                }
            }

            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )

            // 4 MAIN FULL-WIDTH ACTION BUTTONS AS REQUIRED:
            // 1. Daily Income (Green Accent) -> Opens EntryScreen configured for "Daily Income"
            ActionButtonCard(
                title = "Daily Income",
                subtitle = "Log daily store sales, revenue & collections",
                icon = Icons.Default.TrendingUp,
                accentColor = IncomeGreen,
                containerColor = IncomeGreenContainer,
                textColor = IncomeGreenText,
                testTag = "action_daily_income",
                onClick = { onNavigateToEntry("Daily Income") }
            )

            // 2. Expenses (Red Accent) -> Opens EntryScreen configured for "Expense"
            ActionButtonCard(
                title = "Expenses",
                subtitle = "Record daily operational expenses & supplies",
                icon = Icons.Default.MoneyOff,
                accentColor = ExpenseRed,
                containerColor = ExpenseRedContainer,
                textColor = ExpenseRedText,
                testTag = "action_expenses",
                onClick = { onNavigateToEntry("Expense") }
            )

            // 3. Bills (Orange Accent) -> Opens EntryScreen configured for "Bill"
            ActionButtonCard(
                title = "Bills",
                subtitle = "Track utility, rent, tax & vendor invoices",
                icon = Icons.Default.ReceiptLong,
                accentColor = BillOrange,
                containerColor = BillOrangeContainer,
                textColor = BillOrangeText,
                testTag = "action_bills",
                onClick = { onNavigateToEntry("Bill") }
            )

            // 4. Day-by-Day Balance Sheet (Blue Accent) -> Opens BalanceSheetScreen
            ActionButtonCard(
                title = "Day-by-Day Balance Sheet",
                subtitle = "View full chronological balance breakdowns",
                icon = Icons.Default.AccountBalance,
                accentColor = BalanceBlue,
                containerColor = BalanceBlueContainer,
                textColor = BalanceBlueText,
                testTag = "action_balance_sheet",
                onClick = onNavigateToBalanceSheet
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ActionButtonCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    textColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Open",
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun TodayMetricItem(
    title: String,
    amount: Double,
    color: Color
) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$${String.format(Locale.US, "%,.2f", amount)}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun SyncStatusBanner(
    unsyncedCount: Int,
    onSyncClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSyncClick),
        shape = RoundedCornerShape(12.dp),
        color = if (unsyncedCount > 0) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (unsyncedCount > 0) Icons.Default.CloudQueue else Icons.Default.CloudDone,
                    contentDescription = "Sync Status",
                    tint = if (unsyncedCount > 0) Color(0xFFD97706) else Color(0xFF16A34A),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (unsyncedCount > 0) "$unsyncedCount entries queued to sync" else "All entries synced to Sheets",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (unsyncedCount > 0) Color(0xFF92400E) else Color(0xFF166534)
                )
            }
            if (unsyncedCount > 0) {
                Text(
                    text = "Sync Now",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309)
                )
            }
        }
    }
}
