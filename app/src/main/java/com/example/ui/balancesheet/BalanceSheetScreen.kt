package com.example.ui.balancesheet

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TransactionEntity
import com.example.data.remote.RemoteTransaction
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DateBalanceSummary(
    val date: String,
    val totalIncome: Double,
    val totalExpenses: Double,
    val totalBills: Double,
    val netBalance: Double,
    val transactions: List<DisplayTransaction>
)

data class DisplayTransaction(
    val id: String,
    val date: String,
    val type: String,
    val category: String,
    val amount: Double,
    val isSynced: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceSheetScreen(
    repository: TransactionRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val localTransactions by repository.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    var remoteTransactions by remember { mutableStateOf<List<RemoteTransaction>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Fetch remote transactions from Google Sheets on initial launch
    fun refreshSheetData() {
        isRefreshing = true
        scope.launch {
            val result = repository.fetchRemoteTransactions()
            isRefreshing = false
            if (result.isSuccess) {
                remoteTransactions = result.getOrDefault(emptyList())
                Toast.makeText(context, "Loaded ${remoteTransactions.size} records from Sheets", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    context,
                    "Sheets sync note: ${result.exceptionOrNull()?.message ?: "Using local DB"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshSheetData()
    }

    // Merge and group transactions by Date descending
    val dateSummaries by remember(localTransactions, remoteTransactions) {
        derivedStateOf {
            // Combine local and remote without duplicating (match by ID/UUID)
            val combined = mutableListOf<DisplayTransaction>()
            val localUuids = localTransactions.map { it.uuid }.toSet()

            // Add all local transactions
            for (local in localTransactions) {
                combined.add(
                    DisplayTransaction(
                        id = local.uuid,
                        date = local.date,
                        type = local.type,
                        category = local.category,
                        amount = local.amount,
                        isSynced = local.isSynced
                    )
                )
            }

            // Add remote transactions that aren't already represented locally
            for (remote in remoteTransactions) {
                if (remote.id.isEmpty() || !localUuids.contains(remote.id)) {
                    combined.add(
                        DisplayTransaction(
                            id = remote.id.ifEmpty { "remote-${remote.date}-${remote.amount}" },
                            date = remote.date,
                            type = remote.type,
                            category = remote.notes,
                            amount = remote.amount,
                            isSynced = true
                        )
                    )
                }
            }

            // Group by Date
            val grouped = combined.groupBy { it.date }

            // Sort by Date descending (newest date first)
            grouped.entries
                .sortedByDescending { it.key }
                .map { (date, txList) ->
                    val income = txList.filter { it.type == "Daily Income" }.sumOf { it.amount }
                    val expenses = txList.filter { it.type == "Expense" }.sumOf { it.amount }
                    val bills = txList.filter { it.type == "Bill" }.sumOf { it.amount }
                    val net = income - (expenses + bills)

                    DateBalanceSummary(
                        date = date,
                        totalIncome = income,
                        totalExpenses = expenses,
                        totalBills = bills,
                        netBalance = net,
                        transactions = txList.sortedByDescending { it.amount }
                    )
                }
        }
    }

    // Overall aggregate totals across all dates
    val overallIncome = dateSummaries.sumOf { it.totalIncome }
    val overallExpenses = dateSummaries.sumOf { it.totalExpenses }
    val overallBills = dateSummaries.sumOf { it.totalBills }
    val overallNet = overallIncome - (overallExpenses + overallBills)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Day-by-Day Balance Sheet",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("balancesheet_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshSheetData() },
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("refresh_balancesheet_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh from Sheets"
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
        if (dateSummaries.isEmpty() && !isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BalanceBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = BalanceBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "No Transactions Found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Log Daily Income, Expenses, or Bills from the Dashboard to see your balance breakdown here.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Aggregate Summary Card
                item {
                    OverallSummaryCard(
                        income = overallIncome,
                        expenses = overallExpenses,
                        bills = overallBills,
                        net = overallNet
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Breakdowns",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${dateSummaries.size} Days Recorded",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Date Cards sorted in descending chronological order
                items(dateSummaries, key = { it.date }) { summary ->
                    DateBalanceCard(summary = summary)
                }
            }
        }
    }
}

@Composable
fun OverallSummaryCard(
    income: Double,
    expenses: Double,
    bills: Double,
    net: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BalanceBlueContainer.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cumulative Net Balance",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BalanceBlueText
                )
                Surface(
                    shape = CircleShape,
                    color = if (net >= 0) IncomeGreenContainer else ExpenseRedContainer
                ) {
                    Text(
                        text = if (net >= 0) "SURPLUS" else "DEFICIT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (net >= 0) IncomeGreenText else ExpenseRedText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$${String.format(Locale.US, "%,.2f", net)}",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (net >= 0) IncomeGreen else ExpenseRed
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BalanceBlue.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "+$${String.format(Locale.US, "%,.2f", income)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
                Column {
                    Text("Total Expenses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "-$${String.format(Locale.US, "%,.2f", expenses)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
                Column {
                    Text("Total Bills", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "-$${String.format(Locale.US, "%,.2f", bills)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillOrange
                    )
                }
            }
        }
    }
}

@Composable
fun DateBalanceCard(summary: DateBalanceSummary) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrow_rot")

    val formattedDateHeader = remember(summary.date) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateObj = parser.parse(summary.date)
            if (dateObj != null) {
                SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(dateObj)
            } else {
                summary.date
            }
        } catch (_: Exception) {
            summary.date
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("date_card_${summary.date}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Date Title and Expand Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDateHeader,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = summary.date,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Net Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (summary.netBalance >= 0) IncomeGreenContainer else ExpenseRedContainer
                    ) {
                        Text(
                            text = (if (summary.netBalance >= 0) "+$" else "-$") +
                                    String.format(Locale.US, "%,.2f", kotlin.math.abs(summary.netBalance)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.netBalance >= 0) IncomeGreenText else ExpenseRedText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotationState)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Calculated 3 Categories: Income, Expenses, Bills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryBreakdownPill(
                    label = "Income",
                    amount = summary.totalIncome,
                    color = IncomeGreen,
                    containerColor = IncomeGreenContainer
                )
                CategoryBreakdownPill(
                    label = "Expenses",
                    amount = summary.totalExpenses,
                    color = ExpenseRed,
                    containerColor = ExpenseRedContainer
                )
                CategoryBreakdownPill(
                    label = "Bills",
                    amount = summary.totalBills,
                    color = BillOrange,
                    containerColor = BillOrangeContainer
                )
            }

            // Expandable List of Individual Transactions
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transactions on this day (${summary.transactions.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    summary.transactions.forEach { tx ->
                        TransactionItemRow(tx = tx)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownPill(
    label: String,
    amount: Double,
    color: Color,
    containerColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor.copy(alpha = 0.5f),
        modifier = Modifier.width(96.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$${String.format(Locale.US, "%,.2f", amount)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun TransactionItemRow(tx: DisplayTransaction) {
    val (badgeColor, badgeBg, icon) = when (tx.type) {
        "Daily Income" -> Triple(IncomeGreen, IncomeGreenContainer, Icons.Default.TrendingUp)
        "Expense" -> Triple(ExpenseRed, ExpenseRedContainer, Icons.Default.MoneyOff)
        else -> Triple(BillOrange, BillOrangeContainer, Icons.Default.ReceiptLong)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = tx.category.ifBlank { tx.type },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tx.type,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = (if (tx.type == "Daily Income") "+$" else "-$") +
                        String.format(Locale.US, "%,.2f", tx.amount),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = if (tx.isSynced) Icons.Default.CheckCircle else Icons.Default.CloudQueue,
                contentDescription = if (tx.isSynced) "Synced to Sheets" else "Pending Sync",
                tint = if (tx.isSynced) IncomeGreen else BillOrange,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
