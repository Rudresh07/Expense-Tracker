package com.rudy.expensetracker.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudy.expensetracker.model.Transaction
import com.rudy.expensetracker.model.TransactionWithCategory
import com.rudy.expensetracker.ui.theme.Orange
import com.rudy.expensetracker.utils.IconManager
import com.rudy.expensetracker.utils.toColor
import com.rudy.expensetracker.viewmodel.TransactionViewmodel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    onBackClick: () -> Unit,
    onEditTransaction: (Int) -> Unit = {}
) {
    var selectedTimeFilter by rememberSaveable { mutableStateOf("All") }
    var selectedTypeFilter by rememberSaveable { mutableStateOf("All") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("All") }
    var showTimeDropdown by rememberSaveable { mutableStateOf(false) }
    var showTypeDropdown by rememberSaveable { mutableStateOf(false) }
    var showCategoryDropdown by rememberSaveable { mutableStateOf(false) }
    var transactionToDelete by rememberSaveable { mutableStateOf<Transaction?>(null) }

    val formatter = DateTimeFormatter.ofPattern("dd MM yyyy")
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val isDarkTheme = isSystemInDarkTheme()

    val viewModel: TransactionViewmodel = koinViewModel()
    val allTransactions by viewModel.transactionList.collectAsState()

    // Dark mode colors
    val backgroundColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFF8F9FA)
    val surfaceColor = if (isDarkTheme) Color(0xFF2C2C2E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val textSecondaryColor = if (isDarkTheme) Color(0xFFAEAEB2) else Color.Gray
    val appBarColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White

    Log.d("AllTransactionsScreen", "All Transactions: $allTransactions")

    // Filter options
    val timeFilterOptions = listOf("All", "Today", "7 days", "30 days", "90 days", "This Year")
    val typeFilterOptions = listOf("All", "Income", "Expense")
    val categoryFilterOptions = listOf("All") + allTransactions.map { it.category.name }.distinct()

    // Apply filters to transactions
    val filteredTransactions = remember(allTransactions, selectedTimeFilter, selectedTypeFilter, selectedCategoryFilter) {
        var filtered = allTransactions

        // Filter by time
        if (selectedTimeFilter != "All") {
            val cutoffDate: LocalDate = when (selectedTimeFilter) {
                "Today" -> LocalDate.now()
                "7 days" -> LocalDate.now().minusDays(7)
                "30 days" -> LocalDate.now().minusDays(30)
                "90 days" -> LocalDate.now().minusDays(90)
                "This Year" -> LocalDate.now().withDayOfYear(1)
                else -> LocalDate.now().minusDays(7)
            }

            filtered = filtered.filter { it ->
                try {
                    val transactionDate = LocalDate.parse(it.transaction.date, formatter)
                    transactionDate.isAfter(cutoffDate) || transactionDate.isEqual(cutoffDate)
                } catch (e: Exception) {
                    false
                }
            }
        }

        // Filter by type
        if (selectedTypeFilter != "All") {
            filtered = when (selectedTypeFilter) {
                "Income" -> filtered.filter { it.transaction.amount > 0 }
                "Expense" -> filtered.filter { it.transaction.amount < 0 }
                else -> filtered
            }
        }

        // Filter by category
        if (selectedCategoryFilter != "All") {
            filtered = filtered.filter { it.category.name == selectedCategoryFilter }
        }

        // Sort by latest first
        filtered.sortedByDescending { it ->
            try {
                LocalDate.parse(it.transaction.date, formatter)
            } catch (e: Exception) {
                LocalDate.MIN
            }
        }
    }

    // Delete confirmation dialog
    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Text(
                    "Delete Transaction",
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this ${transaction.category} transaction of ₹${abs(transaction.amount)}?",
                    color = textSecondaryColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transaction)
                        transactionToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel", color = Orange, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (isLandscape) {
        // Landscape Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // Left Side - Filters and Info
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxSize()
                    .background(appBarColor)
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Filters",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                // Filters
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Time Filter
                    Column {
                        Text(
                            text = "Time Period",
                            fontSize = 12.sp,
                            color = textSecondaryColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box {
                            FilterChip(
                                selected = selectedTimeFilter != "All",
                                onClick = { showTimeDropdown = true },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            selectedTimeFilter,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Orange.copy(alpha = 0.2f),
                                    selectedLabelColor = Orange,
                                    containerColor = if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFFF0F0F0),
                                    labelColor = textColor
                                )
                            )

                            DropdownMenu(
                                expanded = showTimeDropdown,
                                onDismissRequest = { showTimeDropdown = false }
                            ) {
                                timeFilterOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = textColor) },
                                        onClick = {
                                            selectedTimeFilter = option
                                            showTimeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Type Filter
                    Column {
                        Text(
                            text = "Transaction Type",
                            fontSize = 12.sp,
                            color = textSecondaryColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box {
                            FilterChip(
                                selected = selectedTypeFilter != "All",
                                onClick = { showTypeDropdown = true },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            selectedTypeFilter,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Orange.copy(alpha = 0.2f),
                                    selectedLabelColor = Orange,
                                    containerColor = if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFFF0F0F0),
                                    labelColor = textColor
                                )
                            )

                            DropdownMenu(
                                expanded = showTypeDropdown,
                                onDismissRequest = { showTypeDropdown = false }
                            ) {
                                typeFilterOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = textColor) },
                                        onClick = {
                                            selectedTypeFilter = option
                                            showTypeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Category Filter
                    Column {
                        Text(
                            text = "Category",
                            fontSize = 12.sp,
                            color = textSecondaryColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box {
                            FilterChip(
                                selected = selectedCategoryFilter != "All",
                                onClick = { showCategoryDropdown = true },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (selectedCategoryFilter.length > 12)
                                                "${selectedCategoryFilter.take(10)}..."
                                            else selectedCategoryFilter,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Orange.copy(alpha = 0.2f),
                                    selectedLabelColor = Orange,
                                    containerColor = if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFFF0F0F0),
                                    labelColor = textColor
                                )
                            )

                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                categoryFilterOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = textColor) },
                                        onClick = {
                                            selectedCategoryFilter = option
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Results Info
                Text(
                    text = "${filteredTransactions.size} transactions found",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )

                if (selectedTimeFilter != "All" || selectedTypeFilter != "All" || selectedCategoryFilter != "All") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Clear All Filters",
                        fontSize = 13.sp,
                        color = Orange,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            selectedTimeFilter = "All"
                            selectedTypeFilter = "All"
                            selectedCategoryFilter = "All"
                        }
                    )
                }
            }

            // Right Side - Transactions List
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                // Header
                Text(
                    text = "All Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(16.dp)
                )

                // Transactions List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions) { it ->
                        Column {
                            Text(text = it.transaction.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor)

                            Spacer(modifier = Modifier.height(4.dp))

                            AllTransactionItem(
                                transaction = it,
                                isLandscape = true,
                                surfaceColor = surfaceColor,
                                textColor = textColor,
                                textSecondaryColor = textSecondaryColor,
                                onDeleteClick = { transactionToDelete = it },
                                onEditClick = { onEditTransaction(it) }
                            )
                        }
                    }

                    if (filteredTransactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No transactions found",
                                        color = textSecondaryColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Try adjusting your filters",
                                        color = textSecondaryColor,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    } else {
        // Portrait Layout (Original Design)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // Top App Bar with padding
            TopAppBar(
                modifier = Modifier.padding(top = 16.dp),
                title = {
                    Text(
                        text = "All Transactions",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarColor
                )
            )

            // Filter Section with clear labels
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(appBarColor)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Filters",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Time Filter
                        Column {
                            Text(
                                text = "Time Period",
                                fontSize = 10.sp,
                                color = textSecondaryColor,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box {
                                FilterChip(
                                    selected = selectedTimeFilter != "All",
                                    onClick = { showTimeDropdown = true },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                selectedTimeFilter,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Orange.copy(alpha = 0.2f),
                                        selectedLabelColor = Orange,
                                        containerColor = if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFFF0F0F0),
                                        labelColor = textColor
                                    )
                                )

                                DropdownMenu(
                                    expanded = showTimeDropdown,
                                    onDismissRequest = { showTimeDropdown = false }
                                ) {
                                    timeFilterOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, color = textColor) },
                                            onClick = {
                                                selectedTimeFilter = option
                                                showTimeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Type Filter
                        Column {
                            Text(
                                text = "Transaction Type",
                                fontSize = 10.sp,
                                color = textSecondaryColor,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box {
                                FilterChip(
                                    selected = selectedTypeFilter != "All",
                                    onClick = { showTypeDropdown = true },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                selectedTypeFilter,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Orange.copy(alpha = 0.2f),
                                        selectedLabelColor = Orange,
                                        containerColor = if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFFF0F0F0),
                                        labelColor = textColor
                                    )
                                )

                                DropdownMenu(
                                    expanded = showTypeDropdown,
                                    onDismissRequest = { showTypeDropdown = false }
                                ) {
                                    typeFilterOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, color = textColor) },
                                            onClick = {
                                                selectedTypeFilter = option
                                                showTypeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Category Filter
                        Column {
                            Text(
                                text = "Category",
                                fontSize = 10.sp,
                                color = textSecondaryColor,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box {
                                FilterChip(
                                    selected = selectedCategoryFilter != "All",
                                    onClick = { showCategoryDropdown = true },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (selectedCategoryFilter.length > 12)
                                                    "${selectedCategoryFilter.take(10)}..."
                                                else selectedCategoryFilter,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Orange.copy(alpha = 0.2f),
                                        selectedLabelColor = Orange,
                                        containerColor = if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFFF0F0F0),
                                        labelColor = textColor
                                    )
                                )

                                DropdownMenu(
                                    expanded = showCategoryDropdown,
                                    onDismissRequest = { showCategoryDropdown = false }
                                ) {
                                    categoryFilterOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, color = textColor) },
                                            onClick = {
                                                selectedCategoryFilter = option
                                                showCategoryDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Results count and clear filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredTransactions.size} transactions found",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )

                if (selectedTimeFilter != "All" || selectedTypeFilter != "All" || selectedCategoryFilter != "All") {
                    Text(
                        text = "Clear All Filters",
                        fontSize = 13.sp,
                        color = Orange,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            selectedTimeFilter = "All"
                            selectedTypeFilter = "All"
                            selectedCategoryFilter = "All"
                        }
                    )
                }
            }

            // Transactions List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions) { it ->

                    AllTransactionItem(
                        transaction = it,
                        isLandscape = true,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor,
                        onDeleteClick = { transactionToDelete = it },
                        onEditClick = { onEditTransaction(it) }
                    )

                }

                if (filteredTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No transactions found",
                                    color = textSecondaryColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try adjusting your filters to see more results",
                                    color = textSecondaryColor,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun AllTransactionItem(
    transaction: TransactionWithCategory,
    isLandscape: Boolean = false,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    onDeleteClick: (Transaction) -> Unit,
    onEditClick: (Int) -> Unit
) {
    val displayDate = try {
        LocalDate.parse(transaction.transaction.date, DateTimeFormatter.ofPattern("dd MM yyyy"))
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: Exception) {
        transaction.transaction.date
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLandscape) 2.dp else 4.dp),
        shape = RoundedCornerShape(if (isLandscape) 12.dp else 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLandscape) 10.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            )
            {
                Box(
                    modifier = Modifier
                        .size(if (isLandscape) 40.dp else 52.dp)
                        .clip(CircleShape)
                        .background(transaction.category.colorValue.toColor()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconManager.getIconByName(transaction.category.iconName),
                        contentDescription = transaction.category.name,
                        tint = Color.White,
                        modifier = Modifier.size(if (isLandscape) 20.dp else 26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(if (isLandscape) 8.dp else 14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.category.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isLandscape) 13.sp else 17.sp,
                        color = textColor
                    )
                    Text(
                        text = displayDate,
                        color = textSecondaryColor,
                        fontSize = if (isLandscape) 10.sp else 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    // Add note field if it exists and is not empty
                    if (!transaction.transaction.note.isNullOrBlank()) {
                        Text(
                            text = transaction.transaction.note,
                            color = textSecondaryColor.copy(alpha = 0.8f),
                            fontSize = if (isLandscape) 9.sp else 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = if (isLandscape) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            )
            {
                Column(horizontalAlignment = Alignment.End) {

                    Text(
                        text = transaction.transaction.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isLandscape) 13.sp else 17.sp,
                        color = textColor
                    )
                    Text(
                        text = "₹${abs(transaction.transaction.amount)}",
                        color = if (transaction.transaction.amount < 0) Color(0xFFFF3B30) else Color(0xFF34C759),
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isLandscape) 13.sp else 17.sp
                    )
                    Text(
                        text = if (transaction.transaction.amount < 0) "Expense" else "Income",
                        color = if (transaction.transaction.amount < 0) Color(0xFFFF3B30) else Color(0xFF34C759),
                        fontSize = if (isLandscape) 8.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(if (isLandscape) 6.dp else 10.dp))

                // Action buttons - horizontally aligned in landscape for better space usage
                if (isLandscape) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onEditClick(transaction.transaction.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Orange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteClick(transaction.transaction) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { onEditClick(transaction.transaction.id) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Orange,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteClick(transaction.transaction) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
