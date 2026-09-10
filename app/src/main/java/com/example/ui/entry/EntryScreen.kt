package com.example.ui.entry

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductItem
import com.example.data.repository.TransactionRepository
import com.example.ui.theme.BalanceBlue
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(
    entryType: String,
    repository: TransactionRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var selectedDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var isSaving by remember { mutableStateOf(false) }

    // POS Products State (for "Daily Income")
    var products by remember { mutableStateOf<List<ProductItem>>(emptyList()) }
    var isSyncingProducts by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<ProductItem?>(null) }
    var posQuantity by remember { mutableIntStateOf(1) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    // Color theme based on entry type
    val (accentColor, containerColor, textColor, icon) = when (entryType) {
        "Daily Income" -> Quadruple(IncomeGreen, IncomeGreenContainer, IncomeGreenText, Icons.Default.TrendingUp)
        "Expense" -> Quadruple(ExpenseRed, ExpenseRedContainer, ExpenseRedText, Icons.Default.MoneyOff)
        else -> Quadruple(BillOrange, BillOrangeContainer, BillOrangeText, Icons.Default.ReceiptLong)
    }

    // Load POS Products for Daily Income
    LaunchedEffect(entryType) {
        if (entryType == "Daily Income") {
            products = repository.getProducts(forceRefresh = false)
            // Fetch latest from Google Sheets "Products" tab in background
            scope.launch {
                val refreshResult = repository.refreshProductsFromSheets()
                if (refreshResult.isSuccess) {
                    val remoteList = refreshResult.getOrDefault(emptyList())
                    if (remoteList.isNotEmpty()) {
                        products = remoteList
                    }
                }
            }
        }
    }

    // Suggested categories for Expense & Bill
    val defaultCategorySuggestions = when (entryType) {
        "Expense" -> listOf("Office Supplies", "Transport / Fuel", "Meals", "Software Subs", "Maintenance", "Inventory", "Shipping")
        "Bill" -> listOf("Electricity", "Store Rent", "High-speed Internet", "Water Bill", "Business Tax", "Vendor Invoice", "Insurance")
        else -> emptyList()
    }

    // Date Picker Dialog setup
    val calendar = remember(selectedDate) { parseOrCurrentCalendar(selectedDate) }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            selectedDate = formatted
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New $entryType",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("entry_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
            // Type Badge Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = entryType,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = entryType,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = if (entryType == "Daily Income") "Point of Sale • Tap items or enter manually" else "Record transaction details",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // POINT OF SALE (POS) QUICK SUGGESTIONS SECTION (For "Daily Income")
            if (entryType == "Daily Income") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = "POS",
                                    tint = BalanceBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Quick Suggestions (POS)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Loaded from spreadsheet 'Products' (${products.size} items)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Sync / Refresh Products Button
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        isSyncingProducts = true
                                        scope.launch {
                                            val res = repository.refreshProductsFromSheets()
                                            isSyncingProducts = false
                                            if (res.isSuccess) {
                                                products = res.getOrDefault(products)
                                                Toast.makeText(context, "Loaded ${products.size} products from 'Products' sheet", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Using local cache (${res.exceptionOrNull()?.message})", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("sync_products_button")
                                ) {
                                    if (isSyncingProducts) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = BalanceBlue
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh Products",
                                            tint = BalanceBlue
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showAddProductDialog = true },
                                    modifier = Modifier.testTag("add_product_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add New Product",
                                        tint = IncomeGreen
                                    )
                                }
                            }
                        }

                        // POS Product Grid (2 items per row/lane)
                        val productChunks = products.chunked(2)
                        productChunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { item ->
                                    val isSelected = selectedProduct?.name == item.name
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (isSelected) {
                                                    // Increment quantity on subsequent taps
                                                    posQuantity++
                                                    val total = item.price * posQuantity
                                                    amountText = String.format(Locale.US, "%.2f", total)
                                                    notesText = "${item.name} (x$posQuantity)"
                                                } else {
                                                    selectedProduct = item
                                                    posQuantity = 1
                                                    amountText = String.format(Locale.US, "%.2f", item.price)
                                                    notesText = item.name
                                                }
                                            }
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .testTag("pos_item_${item.name}"),
                                        color = if (isSelected) IncomeGreenContainer else MaterialTheme.colorScheme.surface,
                                        shadowElevation = if (isSelected) 2.dp else 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.name,
                                                    fontSize = 12.sp,
                                                    lineHeight = 14.sp,
                                                    maxLines = 2,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) IncomeGreenText else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (item.category.isNotBlank()) {
                                                    Text(
                                                        text = item.category,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "$${String.format(Locale.US, "%.2f", item.price)}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                // Fill empty slot if the last row is not full
                                if (rowItems.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // Selected Item Quantity Controller (Point of Sale Bar)
                        AnimatedVisibility(visible = selectedProduct != null) {
                            selectedProduct?.let { prod ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Selected: ${prod.name}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Base rate: $${String.format(Locale.US, "%.2f", prod.price)} each",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Quantity Stepper
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    if (posQuantity > 1) {
                                                        posQuantity--
                                                        val total = prod.price * posQuantity
                                                        amountText = String.format(Locale.US, "%.2f", total)
                                                        notesText = if (posQuantity > 1) "${prod.name} (x$posQuantity)" else prod.name
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Remove,
                                                    contentDescription = "Decrease",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Text(
                                                text = "Qty: $posQuantity",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )

                                            IconButton(
                                                onClick = {
                                                    posQuantity++
                                                    val total = prod.price * posQuantity
                                                    amountText = String.format(Locale.US, "%.2f", total)
                                                    notesText = "${prod.name} (x$posQuantity)"
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Increase",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Amount Input Field
            Text(
                text = "Amount ($)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                        amountText = input
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input"),
                placeholder = { Text("0.00") },
                leadingIcon = {
                    Text(
                        text = "$",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Category / Description Input Field
            Text(
                text = if (entryType == "Daily Income") "Item / Notes" else "Category / Description",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input"),
                placeholder = {
                    Text(
                        if (entryType == "Daily Income") "e.g., Espresso, Retail Item, Client Order..."
                        else "e.g., Office Supplies, Transport, Electricity..."
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Category",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = false,
                maxLines = 3
            )

            // Standard Quick Category Suggestions Chips for Expense / Bill
            if (entryType != "Daily Income" && defaultCategorySuggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Quick Suggestions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        defaultCategorySuggestions.forEach { tag ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { notesText = tag }
                                    .border(
                                        1.dp,
                                        if (notesText == tag) accentColor else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(20.dp)
                                    ),
                                color = if (notesText == tag) containerColor else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 12.sp,
                                    fontWeight = if (notesText == tag) FontWeight.Bold else FontWeight.Normal,
                                    color = if (notesText == tag) textColor else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Date Selection (Auto-captured timestamp / date)
            Text(
                text = "Date (Auto-captured)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { datePickerDialog.show() }
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .testTag("date_picker_button"),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Calendar",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        val displayDate = remember(selectedDate) {
                            val parts = selectedDate.trim().split("-")
                            if (parts.size == 3) {
                                if (parts[0].length == 4) "${parts[2]}-${parts[1]}-${parts[0]}" else selectedDate
                            } else selectedDate
                        }
                        Text(
                            text = displayDate,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = "Change Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prominent Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        Toast.makeText(context, "Please enter a valid amount greater than 0", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (notesText.isBlank()) {
                        Toast.makeText(context, "Please enter a note or item name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSaving = true
                    scope.launch {
                        try {
                            repository.saveTransaction(
                                type = entryType,
                                amount = amount,
                                category = notesText,
                                date = selectedDate
                            )
                            Toast.makeText(context, "Entry Saved Locally", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_entry_button"),
                enabled = !isSaving && amountText.isNotBlank() && notesText.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Saving...", color = Color.White)
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save $entryType",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Add Custom POS Product Dialog
    if (showAddProductDialog) {
        var newProdName by remember { mutableStateOf("") }
        var newProdPrice by remember { mutableStateOf("") }
        var newProdCategory by remember { mutableStateOf("") }
        var isSubmittingProd by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmittingProd) showAddProductDialog = false },
            title = {
                Text(
                    text = "Add POS Product",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This item will be saved to your 'Products' sheet and quick suggestions.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newProdName,
                        onValueChange = { newProdName = it },
                        label = { Text("Product / Item Name") },
                        placeholder = { Text("e.g., Flat White Coffee") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProdPrice,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) newProdPrice = it },
                        label = { Text("Price ($)") },
                        placeholder = { Text("4.50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProdCategory,
                        onValueChange = { newProdCategory = it },
                        label = { Text("Category (Optional)") },
                        placeholder = { Text("e.g., Beverage, Food") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPrice = newProdPrice.toDoubleOrNull()
                        if (newProdName.isBlank() || parsedPrice == null || parsedPrice <= 0.0) {
                            Toast.makeText(context, "Please enter a valid product name and price", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmittingProd = true
                        scope.launch {
                            val newProduct = ProductItem(
                                name = newProdName.trim(),
                                price = parsedPrice,
                                category = newProdCategory.trim()
                            )
                            val res = repository.saveProduct(newProduct)
                            isSubmittingProd = false
                            showAddProductDialog = false
                            // Refresh local list
                            products = repository.getCachedProducts()
                            Toast.makeText(context, "Added product ${newProduct.name}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSubmittingProd && newProdName.isNotBlank() && newProdPrice.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                ) {
                    if (isSubmittingProd) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Save Product")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddProductDialog = false },
                    enabled = !isSubmittingProd
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun parseOrCurrentCalendar(dateStr: String): Calendar {
    val cal = Calendar.getInstance()
    try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            if (parts[0].length == 4) {
                val year = parts[0].toInt()
                val month = parts[1].toInt() - 1
                val day = parts[2].toInt()
                cal.set(year, month, day)
            } else {
                val day = parts[0].toInt()
                val month = parts[1].toInt() - 1
                val year = parts[2].toInt()
                cal.set(year, month, day)
            }
        }
    } catch (_: Exception) {}
    return cal
}
