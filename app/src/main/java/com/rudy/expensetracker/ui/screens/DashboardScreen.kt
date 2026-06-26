package com.rudy.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.rudy.expensetracker.analytics.FirebaseAnalytics
import com.rudy.expensetracker.database.CategoryDao
import com.rudy.expensetracker.database.ExpenseDao
import com.rudy.expensetracker.model.CategoryEntity
import com.rudy.expensetracker.model.Transaction
import com.rudy.expensetracker.model.TransactionWithCategory
import com.rudy.expensetracker.ui.screens.LandscapeLayout
import com.rudy.expensetracker.ui.theme.Orange
import com.rudy.expensetracker.utils.AuthManager
import com.rudy.expensetracker.utils.IconManager
import com.rudy.expensetracker.utils.PreferenceManager
import com.rudy.expensetracker.utils.toColor
import com.rudy.expensetracker.viewmodel.TransactionViewmodel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddExpenseClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onReviewClick: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = screenWidth > screenHeight
    val isFoldable = screenWidth > 800.dp

    val isDarkMode = isSystemInDarkTheme()

    // Theme colors
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textPrimaryColor = if (isDarkMode) Color.White else Color.Black
    val textSecondaryColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray
    val iconColor = if (isDarkMode) Color(0xFFE0E0E0) else Color.Gray

    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf("7 days") }
    var showFilterDropdown by rememberSaveable { mutableStateOf(false) }

    val viewModel: TransactionViewmodel = koinViewModel()
    val authManager: AuthManager = getKoin().get()
    val firebaseEvents: FirebaseAnalytics = getKoin().get()

    val preferenceManager: PreferenceManager = getKoin().get()
    var monthlyBudget by rememberSaveable { mutableStateOf(preferenceManager.getMonthlyBudget()) }

    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }

    val totalBalance by viewModel.totalBalance.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val transactions by viewModel.transactionList.collectAsState()
    val pendingReviewCount by viewModel.pendingReviewCount.collectAsState()
    val showPendingSheet by viewModel.showPendingReviewSheet.collectAsState()

    LaunchedEffect(pendingReviewCount) {
        viewModel.triggerPendingReviewSheetIfNeeded(pendingReviewCount)
    }

    val filterOptions = listOf("1 day", "7 days")
    val dateFormatter = DateTimeFormatter.ofPattern("dd MM yyyy")


    val currentMonthExpense = remember(transactions) {
        val now = LocalDate.now()
        val monthlyTxs = transactions.filter { tx ->
            runCatching {
                val d = LocalDate.parse(tx.transaction.date, dateFormatter)
                d.year == now.year && d.monthValue == now.monthValue
            }.getOrDefault(false)
        }
        // Net = expense − income. Negative means income > expense (progress bar shows 0).
        (-monthlyTxs.sumOf { it.transaction.amount }).coerceAtLeast(0.0)
    }

    // Filter transactions
    val filteredTransactions = remember(selectedFilter, transactions) {
        val days = when (selectedFilter) {
            "1 day" -> 1
            "7 days" -> 7
            else -> 7
        }
        val cutoffDate = LocalDate.now().minusDays(days.toLong())

        transactions
            .filter { it ->
                try {
                    val transactionDate = LocalDate.parse(it.transaction.date, dateFormatter)
                    transactionDate.isAfter(cutoffDate) || transactionDate.isEqual(cutoffDate)
                } catch (e: Exception) {
                    false
                }
            }
            .sortedByDescending { it ->
                try {
                    LocalDate.parse(it.transaction.date, dateFormatter)
                } catch (e: Exception) {
                    LocalDate.MIN
                }
            }
            .take(if (isLandscape || isFoldable) 20 else 10)
    }

    val todayExpense = viewModel.todayExpense.collectAsState().value

    if (isLandscape || isFoldable) {
        LandscapeLayout(
            backgroundColor = backgroundColor,
            surfaceColor = surfaceColor,
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor,
            iconColor = iconColor,
            isDarkMode = isDarkMode,
            isFoldable = isFoldable,
            authManager = authManager,
            totalBalance = totalBalance,
            todayExpense = todayExpense,
            totalIncome = totalIncome,
            pendingReviewCount = pendingReviewCount,
            onReviewClick = onReviewClick,
            selectedFilter = selectedFilter,
            showFilterDropdown = showFilterDropdown,
            filteredTransactions = filteredTransactions,
            filterOptions = filterOptions,
            showLogoutDialog = showLogoutDialog,
            monthlyBudget = monthlyBudget,
            monthlyExpense = currentMonthExpense,
            onSetBudgetClick = { showBudgetDialog = true },
            onAddExpenseClick = { onAddExpenseClick()
                                firebaseEvents.logEvent("add_expense_clicked",   params = mapOf(
                                    "userName" to authManager.getUserName(),
                                    "email" to authManager.getUserEmail(),
                                    "screenConfiguration" to (if(isLandscape) "landscape" else "portrait")
                                )) },

            onStatisticsClick = onStatisticsClick,
            onLogoutClick = onLogoutClick,
            onViewAllTransactions = onViewAllTransactions,
            onFilterChange = { selectedFilter = it },
            onFilterDropdownChange = { showFilterDropdown = it },
            onLogoutDialogChange = { showLogoutDialog = it }

        )
    } else {
        PortraitLayout(
            backgroundColor = backgroundColor,
            surfaceColor = surfaceColor,
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor,
            iconColor = iconColor,
            isDarkMode = isDarkMode,
            authManager = authManager,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            todayExpense = todayExpense,
            pendingReviewCount = pendingReviewCount,
            onReviewClick = onReviewClick,
            selectedFilter = selectedFilter,
            showFilterDropdown = showFilterDropdown,
            filteredTransactions = filteredTransactions,
            filterOptions = filterOptions,
            showLogoutDialog = showLogoutDialog,
            onAddExpenseClick = { onAddExpenseClick()
                firebaseEvents.logEvent("add_expense_clicked",   params = mapOf(
                    "userName" to authManager.getUserName(),
                    "email" to authManager.getUserEmail(),
                    "screenConfiguration" to "portrait"
                ))},
            onStatisticsClick = onStatisticsClick,
            onLogoutClick = onLogoutClick,
            onViewAllTransactions = onViewAllTransactions,
            onFilterChange = { selectedFilter = it },
            onFilterDropdownChange = { showFilterDropdown = it },
            onLogoutDialogChange = { showLogoutDialog = it },
            monthlyBudget = monthlyBudget,
            monthlyExpense = currentMonthExpense,
            onSetBudgetClick = { showBudgetDialog = true }
        )
    }

    BudgetSettingDialog(
        show = showBudgetDialog,
        currentBudget = monthlyBudget,
        surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
        textPrimaryColor = if (isDarkMode) Color.White else Color.Black,
        textSecondaryColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray,
        onDismiss = { showBudgetDialog = false },
        onConfirm = { newBudget ->
            preferenceManager.setMonthlyBudget(newBudget)
            monthlyBudget = newBudget
            showBudgetDialog = false
        }
    )

    if (showPendingSheet && pendingReviewCount > 0) {
        AlertDialog(
            onDismissRequest = { viewModel.onPendingReviewSheetShown() },
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
            title = {
                Text(
                    text = "Transactions need your input",
                    color = if (isDarkMode) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "$pendingReviewCount transaction${if (pendingReviewCount > 1) "s were" else " was"} auto-categorised as Other. Tap Review to assign the correct category.",
                    color = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onReviewClick()
                    viewModel.onPendingReviewSheetShown()
                }) {
                    Text("Review Now", color = Orange)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onPendingReviewSheetShown() }) {
                    Text("Later", color = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray)
                }
            }
        )
    }
}

@Composable
 fun LandscapeLayout(
    backgroundColor: Color,
    surfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    iconColor: Color,
    isDarkMode: Boolean,
    isFoldable: Boolean,
    authManager: AuthManager,
    totalBalance: Double,
    totalIncome: Double,
    todayExpense: Double,
    monthlyBudget: Double,
    monthlyExpense: Double,
    pendingReviewCount: Int,
    selectedFilter: String,
    showFilterDropdown: Boolean,
    filteredTransactions: List<TransactionWithCategory>,
    filterOptions: List<String>,
    showLogoutDialog: Boolean,
    onAddExpenseClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onReviewClick: () -> Unit,
    onFilterChange: (String) -> Unit,
    onFilterDropdownChange: (Boolean) -> Unit,
    onLogoutDialogChange: (Boolean) -> Unit,
    onSetBudgetClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // Left Panel - Balance and Navigation
        Column(
            modifier = Modifier
                .weight(if (isFoldable) 0.4f else 0.45f)
                .fillMaxHeight()
        ) {
            // Header
            HeaderSection(
                authManager = authManager,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                iconColor = iconColor,
                pendingReviewCount = pendingReviewCount,
                onReviewClick = onReviewClick,
                onLogoutDialogChange = onLogoutDialogChange,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Balance Card
            BalanceCard(
                isDarkMode = isDarkMode,
                totalBalance = totalBalance,
                totalIncome = totalIncome,
                monthlyExpense = monthlyExpense,
                todayExpense = todayExpense,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            BudgetCard(
                monthlyExpense = monthlyExpense,
                monthlyBudget = monthlyBudget,
                isDarkMode = isDarkMode,
                surfaceColor = surfaceColor,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                onSetBudgetClick = onSetBudgetClick,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Navigation
            BottomNavigationBar(
                isDarkMode = isDarkMode,
                iconColor = iconColor,
                onAddExpenseClick = onAddExpenseClick,
                onStatisticsClick = onStatisticsClick
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right Panel - Transactions
        Column(
            modifier = Modifier
                .weight(if (isFoldable) 0.6f else 0.55f)
                .fillMaxHeight()
        ) {
            // Transactions Header
            TransactionsHeader(
                textPrimaryColor = textPrimaryColor,
                isDarkMode = isDarkMode,
                selectedFilter = selectedFilter,
                showFilterDropdown = showFilterDropdown,
                filterOptions = filterOptions,
                surfaceColor = surfaceColor,
                onFilterChange = onFilterChange,
                onFilterDropdownChange = onFilterDropdownChange,
                onViewAllTransactions = onViewAllTransactions
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Transactions List - Use Grid for foldable/large screens
            if (isFoldable) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTransactions) { it ->
                        TransactionItem(
                            transaction = it,
                            isDarkMode = isDarkMode,
                            surfaceColor = surfaceColor,
                            textPrimaryColor = textPrimaryColor,
                            textSecondaryColor = textSecondaryColor,
                            isCompact = true
                        )
                    }

                    if (filteredTransactions.isEmpty()) {
                        item {
                            EmptyTransactionsMessage(
                                textSecondaryColor = textSecondaryColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions) { it ->
                        TransactionItem(
                            transaction = it,
                            isDarkMode = isDarkMode,
                            surfaceColor = surfaceColor,
                            textPrimaryColor = textPrimaryColor,
                            textSecondaryColor = textSecondaryColor
                        )
                    }

                    if (filteredTransactions.isEmpty()) {
                        item {
                            EmptyTransactionsMessage(
                                textSecondaryColor = textSecondaryColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Logout Dialog
    LogoutDialog(
        showLogoutDialog = showLogoutDialog,
        textPrimaryColor = textPrimaryColor,
        textSecondaryColor = textSecondaryColor,
        surfaceColor = surfaceColor,
        authManager = authManager,
        onLogoutDialogChange = onLogoutDialogChange,
        onLogoutClick = onLogoutClick
    )
}

@Composable
 fun PortraitLayout(
    backgroundColor: Color,
    surfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    iconColor: Color,
    isDarkMode: Boolean,
    authManager: AuthManager,
    totalBalance: Double,
    totalIncome: Double,
    todayExpense: Double,
    monthlyBudget: Double,
    monthlyExpense: Double,
    pendingReviewCount: Int,
    selectedFilter: String,
    showFilterDropdown: Boolean,
    filteredTransactions: List<TransactionWithCategory>,
    filterOptions: List<String>,
    showLogoutDialog: Boolean,
    onAddExpenseClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onReviewClick: () -> Unit,
    onFilterChange: (String) -> Unit,
    onFilterDropdownChange: (Boolean) -> Unit,
    onLogoutDialogChange: (Boolean) -> Unit,
    onSetBudgetClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // Header
        HeaderSection(
            authManager = authManager,
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor,
            iconColor = iconColor,
            pendingReviewCount = pendingReviewCount,
            onReviewClick = onReviewClick,
            onLogoutDialogChange = onLogoutDialogChange,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Balance Card
        BalanceCard(
            isDarkMode = isDarkMode,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            monthlyExpense = monthlyExpense,
            todayExpense = todayExpense
        )

        Spacer(modifier = Modifier.height(12.dp))

        BudgetCard(
            monthlyExpense = monthlyExpense,
            monthlyBudget = monthlyBudget,
            isDarkMode = isDarkMode,
            surfaceColor = surfaceColor,
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor,
            onSetBudgetClick = onSetBudgetClick,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Transactions Header
        TransactionsHeader(
            textPrimaryColor = textPrimaryColor,
            isDarkMode = isDarkMode,
            selectedFilter = selectedFilter,
            showFilterDropdown = showFilterDropdown,
            filterOptions = filterOptions,
            surfaceColor = surfaceColor,
            onFilterChange = onFilterChange,
            onFilterDropdownChange = onFilterDropdownChange,
            onViewAllTransactions = onViewAllTransactions
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Transactions List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredTransactions) { it ->
                TransactionItem(
                    transaction = it,
                    isDarkMode = isDarkMode,
                    surfaceColor = surfaceColor,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor
                )
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    EmptyTransactionsMessage(
                        textSecondaryColor = textSecondaryColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Navigation
        BottomNavigationBar(
            isDarkMode = isDarkMode,
            iconColor = iconColor,
            onAddExpenseClick = onAddExpenseClick,
            onStatisticsClick = onStatisticsClick
        )
    }

    // Logout Dialog
    LogoutDialog(
        showLogoutDialog = showLogoutDialog,
        textPrimaryColor = textPrimaryColor,
        textSecondaryColor = textSecondaryColor,
        surfaceColor = surfaceColor,
        authManager = authManager,
        onLogoutDialogChange = onLogoutDialogChange,
        onLogoutClick = onLogoutClick
    )
}

@Composable
private fun HeaderSection(
    authManager: AuthManager,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    iconColor: Color,
    pendingReviewCount: Int,
    onReviewClick: () -> Unit,
    onLogoutDialogChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Welcome!", fontSize = 14.sp, color = textSecondaryColor)
            Text(
                text = authManager.getUserName(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimaryColor,
            )
        }
        Box {
            IconButton(onClick = onReviewClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Review transactions",
                    tint = Orange,
                    modifier = Modifier.size(26.dp),
                )
            }
            if (pendingReviewCount > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (pendingReviewCount > 9) "9+" else "$pendingReviewCount",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(
    isDarkMode: Boolean,
    totalBalance: Double,
    todayExpense: Double,
    totalIncome: Double,
    monthlyExpense: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Orange.copy(alpha = 0.9f) else Orange
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Balance",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%.2f", totalBalance)}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Today Expense",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%.2f", todayExpense)}", // ✅ Fixed
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IncomeExpenseItem(
                    label = "Income",
                    amount = totalIncome,
                    color = Color.Green
                )
                IncomeExpenseItem(
                    label = "Monthly",
                    amount = monthlyExpense,
                    color = Color.Red
                )
            }
        }
    }
}


@Composable
private fun IncomeExpenseItem(
    label: String,
    amount: Double,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White, fontSize = 12.sp)
            Text("₹${String.format(Locale.getDefault(),"%.2f", amount)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TransactionsHeader(
    textPrimaryColor: Color,
    isDarkMode: Boolean,
    selectedFilter: String,
    showFilterDropdown: Boolean,
    filterOptions: List<String>,
    surfaceColor: Color,
    onFilterChange: (String) -> Unit,
    onFilterDropdownChange: (Boolean) -> Unit,
    onViewAllTransactions: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent Transactions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimaryColor
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Filter Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clickable { onFilterDropdownChange(true) }
                        .background(
                            color = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF0F0F0),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedFilter,
                        fontSize = 12.sp,
                        color = Orange,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Filter",
                        tint = Orange,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showFilterDropdown,
                    onDismissRequest = { onFilterDropdownChange(false) },
                    modifier = Modifier.background(surfaceColor)
                ) {
                    filterOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option,
                                    color = textPrimaryColor
                                )
                            },
                            onClick = {
                                onFilterChange(option)
                                onFilterDropdownChange(false)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // View All button
            Text(
                text = "View All",
                fontSize = 12.sp,
                color = Orange,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(
                        color = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF0F0F0),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onViewAllTransactions() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    isDarkMode: Boolean,
    iconColor: Color,
    onAddExpenseClick: () -> Unit,
    onStatisticsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }) {
            Icon(
                Icons.Default.Home,
                contentDescription = "Home",
                tint = Orange,
                modifier = Modifier.size(28.dp)
            )
        }
        FloatingActionButton(
            onClick = onAddExpenseClick,
            containerColor = Orange,
            modifier = Modifier.size(60.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
        }
        IconButton(onClick = onStatisticsClick) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = "Statistics",
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun EmptyTransactionsMessage(
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No transactions found for the selected period",
            color = textSecondaryColor,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun LogoutDialog(
    showLogoutDialog: Boolean,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    surfaceColor: Color,
    authManager: AuthManager,
    onLogoutDialogChange: (Boolean) -> Unit,
    onLogoutClick: () -> Unit
) {
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { onLogoutDialogChange(false) },
            title = {
                Text(
                    "Logout",
                    color = textPrimaryColor
                )
            },
            text = {
                Text(
                    "Are you sure you want to logout?",
                    color = textSecondaryColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        authManager.logout()
                        onLogoutDialogChange(false)
                        onLogoutClick()
                    }
                ) {
                    Text("Logout", color = Orange)
                }
            },
            dismissButton = {
                TextButton(onClick = { onLogoutDialogChange(false) }) {
                    Text(
                        "Cancel",
                        color = textSecondaryColor
                    )
                }
            },
            containerColor = surfaceColor
        )
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionWithCategory,
    isDarkMode: Boolean = isSystemInDarkTheme(),
    surfaceColor: Color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
    textPrimaryColor: Color = if (isDarkMode) Color.White else Color.Black,
    textSecondaryColor: Color = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray,
    isCompact: Boolean = false
) {
    val displayDateTime = try {
        val formattedDate = transaction.transaction.date.replace(" ", "/")
        if (isCompact) {
            // Show only time for compact view
            transaction.transaction.time
        } else {
            "$formattedDate • ${transaction.transaction.time}"
        }
    } catch (e: Exception) {
        transaction.transaction.time
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isCompact) {
            // Compact layout for grid view
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(transaction.category.colorValue.toColor()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconManager.getIconByName(transaction.category.iconName),
                        contentDescription = transaction.category.name,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = transaction.category.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = textPrimaryColor,
                    maxLines = 1
                )
                Text(
                    text = displayDateTime,
                    color = textSecondaryColor,
                    fontSize = 10.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${String.format(Locale.getDefault(),"%.2f", abs(transaction.transaction.amount))}",
                    color = if (transaction.transaction.amount < 0)
                        Color(0xFFFF5252)
                    else
                        Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        } else {
            // Regular layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(transaction.category.colorValue.toColor()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconManager.getIconByName(transaction.category.iconName),
                            contentDescription = transaction.category.name,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = transaction.category.name,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = textPrimaryColor
                        )
                        Text(
                            text = displayDateTime,
                            color = textSecondaryColor,
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = "₹${String.format(Locale.getDefault(),"%.2f", abs(transaction.transaction.amount))}",
                    color = if (transaction.transaction.amount < 0)
                        Color(0xFFFF5252)
                    else
                        Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun BudgetCard(
    monthlyExpense: Double,
    monthlyBudget: Double,
    isDarkMode: Boolean,
    surfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onSetBudgetClick: () -> Unit,
) {
    val progress = if (monthlyBudget > 0)
        (monthlyExpense / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f
    val barColor = if (progress >= 0.9f) Color(0xFFFF5252) else Orange

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 4.dp else 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Monthly Budget",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimaryColor,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (monthlyBudget > 0) {
                        Text(
                            text = "₹${"%.0f".format(monthlyExpense)} / ₹${"%.0f".format(monthlyBudget)}",
                            fontSize = 12.sp,
                            color = textSecondaryColor,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = onSetBudgetClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Set Budget",
                            tint = Orange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (monthlyBudget > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = barColor,
                    trackColor = if (isDarkMode) Color(0xFF3A3A3A) else Color(0xFFE0E0E0),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progress * 100).toInt()}% used",
                    fontSize = 11.sp,
                    color = if (progress >= 0.9f) Color(0xFFFF5252) else textSecondaryColor,
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${"%.0f".format(monthlyExpense)} spent this month · Tap ✎ to set a budget",
                    fontSize = 12.sp,
                    color = textSecondaryColor,
                )
            }
        }
    }
}

@Composable
private fun BudgetSettingDialog(
    show: Boolean,
    currentBudget: Double,
    surfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    if (!show) return
    var input by remember(currentBudget) {
        mutableStateOf(if (currentBudget > 0) "%.0f".format(currentBudget) else "")
    }
    val isValid = input.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = { Text("Set Monthly Budget", color = textPrimaryColor) },
        text = {
            Column {
                Text(
                    text = "Enter your spending limit for this month",
                    fontSize = 13.sp,
                    color = textSecondaryColor,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (₹)", color = textSecondaryColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { input.toDoubleOrNull()?.let { onConfirm(it) } },
                enabled = isValid,
            ) {
                Text("Save", color = if (isValid) Orange else textSecondaryColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondaryColor)
            }
        }
    )
}

// ── Preview helpers ───────────────────────────────────────────────────────────

private object PreviewExpenseDao : ExpenseDao {
    override suspend fun insertExpense(expense: Transaction): Long = 0L
    override suspend fun updateExpense(expense: Transaction) {}
    override suspend fun deleteExpense(expense: Transaction) {}
    override fun getAllExpenses(): Flow<List<TransactionWithCategory>> = flowOf(emptyList())
    override fun getExpenseById(expenseId: Int): Flow<TransactionWithCategory?> = flowOf(null)
    override fun getTransactionsByMonthYear(month: String, year: String): Flow<List<TransactionWithCategory>> = flowOf(emptyList())
    override fun getTotalBalance(): Flow<Double> = flowOf(0.0)
    override fun getTodayExpense(currentDate: String): Flow<Double> = flowOf(0.0)
    override fun getTotalIncome(): Flow<Double> = flowOf(0.0)
    override fun getTotalExpense(): Flow<Double> = flowOf(0.0)
    override suspend fun deleteAllTransactions() {}
    override suspend fun updateCategoryAndClearReview(txnId: Int, categoryId: Int) {}
    override suspend fun clearNeedsReview(txnId: Int) {}
    override fun getPendingReviewCount(): Flow<Int> = flowOf(0)
    override fun getPendingReviewTransactions(): Flow<List<TransactionWithCategory>> = flowOf(emptyList())
    override suspend fun getStaleReviewTransactions(cutoffDate: String): List<TransactionWithCategory> = emptyList()
}

private object PreviewCategoryDao : CategoryDao {
    override suspend fun getAllCategories(): List<CategoryEntity> = emptyList()
    override suspend fun insertCategory(category: CategoryEntity) {}
    override suspend fun deleteCategory(category: CategoryEntity) {}
    override suspend fun deleteCategoryById(categoryId: Int) {}
    override suspend fun getCategoryByName(name: String): CategoryEntity? = null
    override suspend fun deleteAllCategories() {}
}

@Composable
private fun previewAuthManager(name: String): AuthManager {
    val context = LocalContext.current
    val prefs = PreferenceManager(context).apply { setUserName(name) }
    return AuthManager(prefs, PreviewExpenseDao, PreviewCategoryDao)
}

@Preview(name = "Header / light / no badge", showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun HeaderSectionNoBadgePreview() {
    HeaderSection(
        authManager = previewAuthManager("Rudresh Patel"),
        textPrimaryColor = Color.Black,
        textSecondaryColor = Color.Gray,
        iconColor = Color.Gray,
        pendingReviewCount = 0,
        onReviewClick = {},
        onLogoutDialogChange = {},
    )
}

@Preview(name = "Header / light / badge", showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun HeaderSectionBadgePreview() {
    HeaderSection(
        authManager = previewAuthManager("Rudresh Patel"),
        textPrimaryColor = Color.Black,
        textSecondaryColor = Color.Gray,
        iconColor = Color.Gray,
        pendingReviewCount = 5,
        onReviewClick = {},
        onLogoutDialogChange = {},
    )
}

@Preview(name = "Header / dark / overflow badge", showBackground = true,
    backgroundColor = 0xFF121212, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HeaderSectionDarkOverflowPreview() {
    HeaderSection(
        authManager = previewAuthManager("Rudresh Patel"),
        textPrimaryColor = Color.White,
        textSecondaryColor = Color(0xFFB0B0B0),
        iconColor = Color(0xFFE0E0E0),
        pendingReviewCount = 12,
        onReviewClick = {},
        onLogoutDialogChange = {},
    )
}