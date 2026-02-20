package com.rudy.expensetracker.ui.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudy.expensetracker.model.ExpenseStatistic
import com.rudy.expensetracker.ui.theme.Orange
import com.rudy.expensetracker.utils.toExpenseStatistics
import com.rudy.expensetracker.viewmodel.TransactionViewmodel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

enum class TransactionType {
    EXPENSE, INCOME, ALL
}

// Theme colors for dark mode
private val LightColors = lightColorScheme(
    primary = Orange,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    outline = Color.Gray
)

private val DarkColors = darkColorScheme(
    primary = Orange,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White,
    outline = Color(0xFF666666)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseStatisticsScreen(
    onBackClick: () -> Unit,
    isDarkMode: Boolean = isSystemInDarkTheme(),
    onThemeToggle: () -> Unit = {}
) {
    val colorScheme = if (isDarkMode) DarkColors else LightColors

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ExpenseStatisticsContent(
                onBackClick = onBackClick,
                isDarkMode = isDarkMode,
                onThemeToggle = onThemeToggle
            )
        }
    }
}

@Composable
private fun ExpenseStatisticsContent(
    onBackClick: () -> Unit,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit
) {
    val viewmodel: TransactionViewmodel = koinViewModel()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp

    // Detect foldable/tablet devices
    val isFoldableOrTablet = screenWidth > 600

    // State for filters
    var selectedMonth by rememberSaveable { mutableStateOf(LocalDate.now().monthValue) }
    var selectedYear by rememberSaveable { mutableStateOf(LocalDate.now().year) }
    var selectedTransactionType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var showMonthDropdown by rememberSaveable { mutableStateOf(false) }
    var showTypeDropdown by rememberSaveable { mutableStateOf(false) }

    // Load data based on filters
    LaunchedEffect(selectedMonth, selectedYear, selectedTransactionType) {
        val monthStr = String.format("%02d", selectedMonth)
        viewmodel.getFilteredTransaction(monthStr, selectedYear.toString())
    }

    val filteredData by viewmodel.filteredTransactionList.collectAsState()

    // Filter data based on transaction type
    val typeFilteredData = when (selectedTransactionType) {
        TransactionType.EXPENSE -> filteredData.filter { it.transaction.amount < 0 }
        TransactionType.INCOME -> filteredData.filter { it.transaction.amount > 0 }
        TransactionType.ALL -> filteredData
    }

    val statisticsData = typeFilteredData.toExpenseStatistics()
        .sortedWith(compareBy<ExpenseStatistic> { it.amount >= 0 }
            .thenByDescending { it.amount })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLandscape|| isFoldableOrTablet) {
            // Landscape Layout - More spacious design
            LandscapeLayout(
                selectedTransactionType = selectedTransactionType,
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                showMonthDropdown = showMonthDropdown,
                showTypeDropdown = showTypeDropdown,
                statisticsData = statisticsData,
                onBackClick = onBackClick,
                onThemeToggle = onThemeToggle,
                isDarkMode = isDarkMode,
                onMonthDropdownToggle = { showMonthDropdown = it },
                onTypeDropdownToggle = { showTypeDropdown = it },
                onMonthSelected = { selectedMonth = it },
                onTypeSelected = { selectedTransactionType = it },
                isFoldableOrTablet = isFoldableOrTablet
            )
        } else {
            // Portrait Layout
            PortraitLayout(
                selectedTransactionType = selectedTransactionType,
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                showMonthDropdown = showMonthDropdown,
                showTypeDropdown = showTypeDropdown,
                statisticsData = statisticsData,
                onBackClick = onBackClick,
                onThemeToggle = onThemeToggle,
                isDarkMode = isDarkMode,
                onMonthDropdownToggle = { showMonthDropdown = it },
                onTypeDropdownToggle = { showTypeDropdown = it },
                onMonthSelected = { selectedMonth = it },
                onTypeSelected = { selectedTransactionType = it }
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    selectedTransactionType: TransactionType,
    selectedMonth: Int,
    selectedYear: Int,
    showMonthDropdown: Boolean,
    showTypeDropdown: Boolean,
    statisticsData: List<ExpenseStatistic>,
    onBackClick: () -> Unit,
    onThemeToggle: () -> Unit,
    isDarkMode: Boolean,
    onMonthDropdownToggle: (Boolean) -> Unit,
    onTypeDropdownToggle: (Boolean) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onTypeSelected: (TransactionType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = dimensionResource(com.intuit.sdp.R.dimen._14sdp))
    ) {
        // Header
        HeaderSection(
            selectedTransactionType = selectedTransactionType,
            onBackClick = onBackClick,
            onThemeToggle = onThemeToggle,
            isDarkMode = isDarkMode
        )

        // Filters Section
        FiltersSection(
            selectedMonth = selectedMonth,
            selectedTransactionType = selectedTransactionType,
            showMonthDropdown = showMonthDropdown,
            showTypeDropdown = showTypeDropdown,
            onMonthDropdownToggle = onMonthDropdownToggle,
            onTypeDropdownToggle = onTypeDropdownToggle,
            onMonthSelected = onMonthSelected,
            onTypeSelected = onTypeSelected,
            modifier = Modifier.padding(horizontal = dimensionResource(com.intuit.sdp.R.dimen._14sdp))
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._14sdp)))

        if (statisticsData.isEmpty()) {
            EmptyStateSection(selectedTransactionType)
        } else {
            // Statistics Section
            StatisticsSection(
                selectedTransactionType = selectedTransactionType,
                statisticsData = statisticsData,
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
private fun LandscapeLayout(
    selectedTransactionType: TransactionType,
    selectedMonth: Int,
    selectedYear: Int,
    showMonthDropdown: Boolean,
    showTypeDropdown: Boolean,
    statisticsData: List<ExpenseStatistic>,
    onBackClick: () -> Unit,
    onThemeToggle: () -> Unit,
    isDarkMode: Boolean,
    onMonthDropdownToggle: (Boolean) -> Unit,
    onTypeDropdownToggle: (Boolean) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    isFoldableOrTablet: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        // Header with more generous padding
        Log.d("Padding","Systembar Padding: ${WindowInsets.statusBars.asPaddingValues()}")
        HeaderSection(
            selectedTransactionType = selectedTransactionType,
            onBackClick = onBackClick,
            onThemeToggle = onThemeToggle,
            isDarkMode = isDarkMode
        )

        // Filters Section
        FiltersSection(
            selectedMonth = selectedMonth,
            selectedTransactionType = selectedTransactionType,
            showMonthDropdown = showMonthDropdown,
            showTypeDropdown = showTypeDropdown,
            onMonthDropdownToggle = onMonthDropdownToggle,
            onTypeDropdownToggle = onTypeDropdownToggle,
            onMonthSelected = onMonthSelected,
            onTypeSelected = onTypeSelected,
            modifier = Modifier.padding(horizontal = dimensionResource(com.intuit.sdp.R.dimen._22sdp)) // More padding in landscape
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._18sdp)))

        if (statisticsData.isEmpty()) {
            EmptyStateSection(selectedTransactionType)
        } else {
            // Main content area with better spacing
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(com.intuit.sdp.R.dimen._22sdp)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(com.intuit.sdp.R.dimen._28sdp)) // Generous spacing
            ) {
                // Left side - Pie Chart (larger portion)
                Column(
                    modifier = Modifier
                        .weight(0.6f) // Give more space to chart
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "${selectedTransactionType.name.lowercase().replaceFirstChar { it.uppercase() }} Overview",
                        fontSize = dimensionResource(com.intuit.ssp.R.dimen._18ssp).value.sp, // Larger text
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = dimensionResource(com.intuit.sdp.R.dimen._22sdp))
                    )

                    // Larger, more prominent pie chart
                    Box(
                        modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._244sdp)), // Bigger chart
                        contentAlignment = Alignment.Center
                    ) {
                        EnhancedPieChart(
                            data = statisticsData,
                            modifier = Modifier.fillMaxSize(),
                            isDarkMode = isDarkMode
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._22sdp)))

                    // Total summary card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._14sdp))
                    ) {
                        Column(
                            modifier = Modifier.padding(dimensionResource(com.intuit.sdp.R.dimen._18sdp)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total ${selectedTransactionType.name.lowercase()}",
                                fontSize = dimensionResource(com.intuit.ssp.R.dimen._14ssp).value.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._7sdp)))
                            Text(
                                text = "₹${statisticsData.sumOf { it.amount }}",
                                fontSize = dimensionResource(com.intuit.ssp.R.dimen._24ssp).value.sp,
                                fontWeight = FontWeight.Bold,
                                color = Orange
                            )
                            Text(
                                text = "${statisticsData.size} categories",
                                fontSize = dimensionResource(com.intuit.ssp.R.dimen._12ssp).value.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Right side - Statistics List (compact horizontal cards)
                Column(
                    modifier = Modifier
                        .weight(0.4f) // Less space but still readable
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Category Breakdown",
                        fontSize = dimensionResource(com.intuit.ssp.R.dimen._16ssp).value.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = dimensionResource(com.intuit.sdp.R.dimen._14sdp))
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(com.intuit.sdp.R.dimen._10sdp))
                    ) {
                        items(statisticsData) { stat ->
                            LandscapeExpenseCard(
                                statistic = stat,
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    selectedTransactionType: TransactionType,
    onBackClick: () -> Unit,
    onThemeToggle: () -> Unit,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
           // .padding(dimensionResource(com.intuit.sdp.R.dimen._14sdp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = when (selectedTransactionType) {
                TransactionType.EXPENSE -> "Expenses"
                TransactionType.INCOME -> "Income"
                TransactionType.ALL -> "Transactions"
            },
            fontSize =dimensionResource(com.intuit.ssp.R.dimen._18ssp).value.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
       /* IconButton(onClick = onThemeToggle) {
            Icon(
                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle theme",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }*/
        Box(modifier= Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._22sdp))) {  }
    }
}

@Composable
private fun FiltersSection(
    selectedMonth: Int,
    selectedTransactionType: TransactionType,
    showMonthDropdown: Boolean,
    showTypeDropdown: Boolean,
    onMonthDropdownToggle: (Boolean) -> Unit,
    onTypeDropdownToggle: (Boolean) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(com.intuit.sdp.R.dimen._14sdp))
    ) {
        // Month Filter
        Box {
            Row(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._10sdp)))
                    .clickable { onMonthDropdownToggle(true) }
                    .padding(horizontal = dimensionResource(com.intuit.sdp.R.dimen._14sdp), vertical = dimensionResource(com.intuit.sdp.R.dimen._10sdp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getMonthName(selectedMonth),
                    fontSize = dimensionResource(com.intuit.ssp.R.dimen._12ssp).value.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(dimensionResource(com.intuit.sdp.R.dimen._7sdp)))
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._16sdp)),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            DropdownMenu(
                expanded = showMonthDropdown,
                onDismissRequest = { onMonthDropdownToggle(false) }
            ) {
                (1..12).forEach { month ->
                    DropdownMenuItem(
                        text = { Text(getMonthName(month)) },
                        onClick = {
                            onMonthSelected(month)
                            onMonthDropdownToggle(false)
                        }
                    )
                }
            }
        }

        // Transaction Type Filter
        Box {
            Row(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._10sdp)))
                    .clickable { onTypeDropdownToggle(true) }
                    .padding(horizontal = dimensionResource(com.intuit.sdp.R.dimen._14sdp), vertical = dimensionResource(com.intuit.sdp.R.dimen._10sdp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedTransactionType) {
                        TransactionType.EXPENSE -> "Expenses"
                        TransactionType.INCOME -> "Income"
                        TransactionType.ALL -> "All"
                    },
                    fontSize = dimensionResource(com.intuit.ssp.R.dimen._12ssp).value.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(dimensionResource(com.intuit.sdp.R.dimen._7sdp)))
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._16sdp)),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            DropdownMenu(
                expanded = showTypeDropdown,
                onDismissRequest = { onTypeDropdownToggle(false) }
            ) {
                TransactionType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(when (type) {
                                TransactionType.EXPENSE -> "Expenses"
                                TransactionType.INCOME -> "Income"
                                TransactionType.ALL -> "All Transactions"
                            })
                        },
                        onClick = {
                            onTypeSelected(type)
                            onTypeDropdownToggle(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateSection(selectedTransactionType: TransactionType) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(com.intuit.sdp.R.dimen._28sdp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No transactions yet",
            fontSize = dimensionResource(com.intuit.ssp.R.dimen._18ssp).value.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._10sdp)))
        Text(
            text = "Start by adding your first ${selectedTransactionType.name.lowercase()}",
            fontSize = dimensionResource(com.intuit.ssp.R.dimen._14ssp).value.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatisticsSection(
    selectedTransactionType: TransactionType,
    statisticsData: List<ExpenseStatistic>,
    isDarkMode: Boolean
) {
    Text(
        text = "${selectedTransactionType.name.lowercase().replaceFirstChar { it.uppercase() }} Statistics",
        fontSize = dimensionResource(com.intuit.ssp.R.dimen._16ssp).value.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = dimensionResource(com.intuit.sdp.R.dimen._14sdp))
    )

    Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._18sdp)))

    // Enhanced Pie Chart
    Box(
        modifier = Modifier
            .fillMaxWidth().height(dimensionResource(com.intuit.sdp.R.dimen._244sdp))
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        EnhancedPieChart(
            data = statisticsData,
            modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._230sdp)),
            isDarkMode = isDarkMode
        )
    }

    Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._22sdp)))

    // Expenses List Header
    Text(
        text = "${selectedTransactionType.name.lowercase().replaceFirstChar { it.uppercase() }} List",
        fontSize = dimensionResource(com.intuit.ssp.R.dimen._16ssp).value.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = dimensionResource(com.intuit.sdp.R.dimen._14sdp))
    )

    Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._14sdp)))

    // Expenses List
    LazyColumn(
        modifier = Modifier.padding(horizontal = dimensionResource(com.intuit.sdp.R.dimen._14sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(com.intuit.sdp.R.dimen._10sdp))
    ) {
        items(statisticsData) { stat ->
            ExpenseStatItem(
                statistic = stat,
                isDarkMode = isDarkMode
            )
        }
    }
}

// New compact landscape card design
@Composable
private fun LandscapeExpenseCard(
    statistic: ExpenseStatistic,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._10sdp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(com.intuit.sdp.R.dimen._14sdp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon and category
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(com.intuit.sdp.R.dimen._32sdp))
                        .clip(CircleShape)
                        .background(statistic.color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statistic.icon,
                        contentDescription = statistic.category,
                        tint = Color.White,
                        modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._16sdp))
                    )
                }
                Spacer(modifier = Modifier.width(dimensionResource(com.intuit.sdp.R.dimen._10sdp)))
                Column {
                    Text(
                        text = statistic.category,
                        fontWeight = FontWeight.Medium,
                        fontSize =dimensionResource(com.intuit.ssp.R.dimen._12ssp).value.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format("%.1f%%", statistic.percentage),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = dimensionResource(com.intuit.ssp.R.dimen._10ssp).value.sp
                    )
                }
            }

            // Amount
            Text(
                text = "₹${statistic.amount}",
                fontWeight = FontWeight.Bold,
                fontSize = dimensionResource(com.intuit.ssp.R.dimen._14ssp).value.sp,
                color = Orange
            )
        }
    }
}

@Composable
fun EnhancedPieChart(
    data: List<ExpenseStatistic>,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    Canvas(modifier = modifier) {
        val total = data.sumOf { it.amount }
        var startAngle = -90f
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width * 0.35f
        val innerRadius = size.width * 0.2f

        // Draw pie segments
        data.forEach { stat ->
            val sweepAngle = (stat.amount.toFloat() / total.toFloat()) * 360f

            // Draw outer arc
            drawArc(
                color = stat.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )

            // Calculate label position
            val labelAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
            val labelRadius = radius * 0.7f
            val labelX = center.x + (labelRadius * cos(labelAngle)).toFloat()
            val labelY = center.y + (labelRadius * sin(labelAngle)).toFloat()

            // Draw percentage labels if segment is large enough
            if (sweepAngle > 20f) {
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        textSize = 28f // Larger text
                        color = Color.White.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        setShadowLayer(2f, 1f, 1f, Color.Black.copy(alpha = 0.3f).toArgb())
                    }
                    canvas.nativeCanvas.drawText(
                        String.format("%.1f%%", stat.percentage),
                        labelX,
                        labelY + 8f,
                        paint
                    )
                }
            }
            startAngle += sweepAngle
        }

        // Draw inner circle (donut effect)
        drawCircle(
            color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
            radius = innerRadius,
            center = center
        )

        // Draw total amount in center
        drawIntoCanvas { canvas ->
            val textColor = if (isDarkMode) Color.White else Color.Black
            val subtextColor = if (isDarkMode) Color.Gray else Color.Gray

            val paint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                textSize = 36f // Larger text
                color = textColor.toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.nativeCanvas.drawText(
                "₹$total",
                center.x,
                center.y + 8f,
                paint
            )

            val smallPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                textSize = 22f // Larger subtitle
                color = subtextColor.toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.nativeCanvas.drawText(
                "Total",
                center.x,
                center.y - 24f,
                smallPaint
            )
        }
    }
}

@Composable
fun ExpenseStatItem(
    statistic: ExpenseStatistic,
    isDarkMode: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._14sdp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(com.intuit.sdp.R.dimen._18sdp)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(com.intuit.sdp.R.dimen._44sdp))
                        .clip(CircleShape)
                        .background(statistic.color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statistic.icon,
                        contentDescription = statistic.category,
                        tint = Color.White,
                        modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._22sdp))
                    )
                }
                Spacer(modifier = Modifier.width(dimensionResource(com.intuit.sdp.R.dimen._14sdp)))
                Column {
                    Text(
                        text = statistic.category,
                        fontWeight = FontWeight.Bold,
                        fontSize = dimensionResource(com.intuit.ssp.R.dimen._16ssp).value.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format("%.2f%%", statistic.percentage),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = dimensionResource(com.intuit.ssp.R.dimen._12ssp).value.sp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${statistic.amount}",
                    fontWeight = FontWeight.Bold,
                    fontSize = dimensionResource(com.intuit.ssp.R.dimen._16ssp).value.sp,
                    color = Orange
                )
                Spacer(modifier = Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._7sdp)))
                // Progress bar
                Box(
                    modifier = Modifier
                        .width(dimensionResource(com.intuit.sdp.R.dimen._72sdp))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((statistic.percentage / 100f).toFloat())
                            .background(statistic.color)
                    )
                }
            }
        }
    }
}

private fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Unknown"
    }
}
