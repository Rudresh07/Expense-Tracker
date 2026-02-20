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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.rudy.expensetracker.analytics.FirebaseAnalytics
import com.rudy.expensetracker.model.TransactionWithCategory
import com.rudy.expensetracker.ui.screens.LandscapeLayout
import com.rudy.expensetracker.ui.theme.Orange
import com.rudy.expensetracker.utils.AuthManager
import com.rudy.expensetracker.utils.IconManager
import com.rudy.expensetracker.utils.toColor
import com.rudy.expensetracker.viewmodel.TransactionViewmodel
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

    val totalBalance by viewModel.totalBalance.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val transactions by viewModel.transactionList.collectAsState()

    val filterOptions = listOf("1 day", "7 days")
    val dateFormatter = DateTimeFormatter.ofPattern("dd MM yyyy")


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
            totalExpense = totalExpense,
            selectedFilter = selectedFilter,
            showFilterDropdown = showFilterDropdown,
            filteredTransactions = filteredTransactions,
            filterOptions = filterOptions,
            showLogoutDialog = showLogoutDialog,
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
            totalExpense = totalExpense,
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
            onLogoutDialogChange = { showLogoutDialog = it }
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
    totalExpense: Double,
    todayExpense: Double,
    selectedFilter: String,
    showFilterDropdown: Boolean,
    filteredTransactions: List<TransactionWithCategory>,
    filterOptions: List<String>,
    showLogoutDialog: Boolean,
    onAddExpenseClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onFilterChange: (String) -> Unit,
    onFilterDropdownChange: (Boolean) -> Unit,
    onLogoutDialogChange: (Boolean) -> Unit
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
                onLogoutDialogChange = onLogoutDialogChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Balance Card
            BalanceCard(
                isDarkMode = isDarkMode,
                totalBalance = totalBalance,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                todayExpense = todayExpense,
                modifier = Modifier.fillMaxWidth()
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
    totalExpense: Double,
    todayExpense: Double,
    selectedFilter: String,
    showFilterDropdown: Boolean,
    filteredTransactions: List<TransactionWithCategory>,
    filterOptions: List<String>,
    showLogoutDialog: Boolean,
    onAddExpenseClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onFilterChange: (String) -> Unit,
    onFilterDropdownChange: (Boolean) -> Unit,
    onLogoutDialogChange: (Boolean) -> Unit
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
            onLogoutDialogChange = onLogoutDialogChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Balance Card
        BalanceCard(
            isDarkMode = isDarkMode,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            todayExpense = todayExpense
        )

        Spacer(modifier = Modifier.height(24.dp))

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
    onLogoutDialogChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Welcome!",
                fontSize = 14.sp,
                color = textSecondaryColor
            )
            Text(
                text = authManager.getUserName(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimaryColor
            )
        }
        Row {
            IconButton(onClick = { onLogoutDialogChange(true) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
            }
           /* IconButton(onClick = {throw RuntimeException("Test Crash") }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
            }*/
        }
    }
}

@Composable
private fun BalanceCard(
    isDarkMode: Boolean,
    totalBalance: Double,
    todayExpense: Double,
    totalIncome: Double,
    totalExpense: Double,
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
                    label = "Expense",
                    amount = totalExpense,
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