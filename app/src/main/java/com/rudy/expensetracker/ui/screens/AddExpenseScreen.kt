package com.rudy.expensetracker.ui.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.rudy.expensetracker.model.CategoryEntity
import com.rudy.expensetracker.model.Transaction
import com.rudy.expensetracker.ui.theme.Orange
import com.rudy.expensetracker.utils.IconManager
import com.rudy.expensetracker.utils.toColor
import com.rudy.expensetracker.viewmodel.CategoryViewModel
import com.rudy.expensetracker.viewmodel.TransactionViewmodel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    transactionId: Int?, // null = add new
    onBackClick: () -> Unit,
    onExpenseAdded: () -> Unit,
    forceLightMode: Boolean = false,
    forcePortraitOnly: Boolean = false
) {
    val isDarkMode = if (forceLightMode) false else isSystemInDarkTheme()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp >= 600 // Basic tablet detection
    val shouldUseLandscapeLayout = !forcePortraitOnly && (isLandscape || isTablet)

    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textPrimaryColor = if (isDarkMode) Color.White else Color.Black
    val textSecondaryColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray
    val iconColor = if (isDarkMode) Color(0xFFE0E0E0) else Color.Gray
    val borderColor = if (isDarkMode) Color(0xFF3A3A3A) else Color(0xFFE0E0E0)

    val viewmodel: TransactionViewmodel = koinViewModel()
    val categoryViewModel: CategoryViewModel = koinViewModel()
    val categories by categoryViewModel.categories.collectAsState()
    val transactionToEdit = viewmodel.transactionDetail.collectAsState()

    var amount by remember {
        mutableStateOf(
            if (transactionId != null) {
                // For editing, convert amount back to positive and format properly
                val editAmount = transactionToEdit.value?.transaction?.amount ?: 0.0
                if (editAmount != 0.0) {
                    kotlin.math.abs(editAmount).toString()
                } else ""
            } else ""
        )
    }

    var memo by remember {
        mutableStateOf(transactionToEdit.value?.transaction?.note ?: "")
    }

    var selectedCategory by remember {
        mutableStateOf(transactionToEdit.value?.category)
    }

    var selectedTransactionType by remember {
        mutableIntStateOf(
            transactionToEdit.value?.transaction?.transactionType ?: 1
        )
    }

    var selectedDateMillis by remember {
        mutableLongStateOf(
            if (transactionId != null) {
                // Parse the date from transaction if editing
                try {
                    val dateStr = transactionToEdit.value?.transaction?.date ?: ""
                    if (dateStr.isNotEmpty()) {
                        // Assuming date format is "dd MM yyyy" or "dd/MM/yyyy"
                        val sdf = SimpleDateFormat("dd MM yyyy", Locale.getDefault())
                        sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                    } else {
                        System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }
        )
    }

    var showMemoDialog by rememberSaveable { mutableStateOf(false) }
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var newCategoryName by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showIconDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var categoryToDelete by rememberSaveable { mutableStateOf<CategoryEntity?>(null) }
    var selectedIconName by rememberSaveable { mutableStateOf("category") }
    var selectedColor by remember { mutableStateOf(Color(0xFF4CAF50)) }
    var title by rememberSaveable { mutableStateOf("") }

    val today = Calendar.getInstance().timeInMillis
    val context = LocalContext.current

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewmodel.getTransactionById(transactionId)
        }
    }

    LaunchedEffect(transactionToEdit.value) {
        transactionToEdit.value?.let { transactionDetail ->
            val transaction = transactionDetail.transaction
            val category = transactionDetail.category

            // Update amount (convert to positive)
            amount = kotlin.math.abs(transaction.amount).let {
                if (it == 0.0) "" else it.toString()
            }

            // memo = transaction.memo ?: ""

            selectedCategory = category

            selectedTransactionType = transaction.transactionType

            try {
                val sdf = SimpleDateFormat("dd MM yyyy", Locale.getDefault())
                selectedDateMillis = sdf.parse(transaction.date)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                selectedDateMillis = System.currentTimeMillis()
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis,
        selectableDates = object : SelectableDates {

            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= today
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year <= Calendar.getInstance().get(Calendar.YEAR)
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        "OK",
                        color = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel", color = textSecondaryColor)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = surfaceColor,
                titleContentColor = textPrimaryColor,
                headlineContentColor = textPrimaryColor,
                weekdayContentColor = textSecondaryColor,
                subheadContentColor = textSecondaryColor,
                yearContentColor = textPrimaryColor,
                currentYearContentColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange,
                dayContentColor = textPrimaryColor,
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange,
                todayContentColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange,
                todayDateBorderColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange,
                dayInSelectionRangeContainerColor = if (selectedTransactionType == 0) Color(0xFF4CAF50).copy(alpha = 0.1f) else Orange.copy(alpha = 0.1f)
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = surfaceColor,
                    titleContentColor = textPrimaryColor,
                    headlineContentColor = textPrimaryColor,
                    weekdayContentColor = textSecondaryColor,
                    subheadContentColor = textSecondaryColor,
                    yearContentColor = textPrimaryColor,
                    currentYearContentColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange,
                    dayContentColor = textPrimaryColor,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange,
                    todayContentColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange,
                    todayDateBorderColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange,
                    dayInSelectionRangeContainerColor = if (selectedTransactionType == 0) Color(0xFF4CAF50).copy(alpha = 0.1f) else Orange.copy(alpha = 0.1f)
                )
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (shouldUseLandscapeLayout) {

        LandscapeAddExpenseLayout(
            isDarkMode = isDarkMode,
            backgroundColor = backgroundColor,
            surfaceColor = surfaceColor,
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor,
            iconColor = iconColor,
            borderColor = borderColor,
            amount = amount,
            onAmountChange = { amount = it },
            title = title,
            onTitleChange = { title = it },
            memo = memo,
            onMemoChange = { memo = it },
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            selectedTransactionType = selectedTransactionType,
            onTransactionTypeChange = { selectedTransactionType = it },
            selectedDateMillis = selectedDateMillis,
            onDateClick = { showDatePicker = true },
            categories = categories,
            onAddCategoryClick = { showAddCategoryDialog = true },
            onCategoryLongClick = { category ->
                categoryToDelete = category
                showDeleteDialog = true
            },
            onMemoDialogOpen = { showMemoDialog = true },
            onBackClick = onBackClick,
            onSave = {
                val amountValue = amount.toDoubleOrNull()
                if (selectedCategory != null && amountValue != null && title.isNotEmpty() && amountValue > 0) {
                    val finalAmount = if (selectedTransactionType == 0) amountValue else -amountValue

                    if (transactionId != null) {
                        // Update existing transaction
                        val updatedTransaction = Transaction(
                            id = transactionId,
                            category = selectedCategory!!.id,
                            time = getCurrentTime(),
                            transactionType = selectedTransactionType,
                            amount = finalAmount,
                            date = selectedDateMillis.toReadableDate(),
                            note = memo,
                            title = title
                        )
                        viewmodel.updateTransaction(updatedTransaction)
                    } else {
                        // Create new transaction
                        val newTransaction = Transaction(
                            category = selectedCategory!!.id,
                            time = getCurrentTime(),
                            transactionType = selectedTransactionType,
                            amount = finalAmount,
                            date = selectedDateMillis.toReadableDate(),
                            note = memo,
                            title = title
                        )
                        viewmodel.addTransaction(newTransaction)
                    }

                    scope.launch {
                        snackbarHostState.showSnackbar("Transaction saved 🎉")
                    }
                    onExpenseAdded()
                } else {
                    Toast.makeText(
                        context,
                        "Please select a category and enter a valid amount.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    } else {
        PortraitAddExpenseLayout(
            isDarkMode = isDarkMode,
            backgroundColor = backgroundColor,
            surfaceColor = surfaceColor,
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor,
            iconColor = iconColor,
            borderColor = borderColor,
            title = title,
            onTitleChange = { title = it },
            amount = amount,
            onAmountChange = { amount = it },
            memo = memo,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            selectedTransactionType = selectedTransactionType,
            onTransactionTypeChange = { selectedTransactionType = it },
            selectedDateMillis = selectedDateMillis,
            onDateClick = { showDatePicker = true },
            categories = categories,
            onAddCategoryClick = { showAddCategoryDialog = true },
            onCategoryLongClick = { category ->
                categoryToDelete = category
                showDeleteDialog = true
            },
            onMemoDialogOpen = { showMemoDialog = true },
            onBackClick = onBackClick,
            onSave = {
                val amountValue = amount.toDoubleOrNull()
                if (selectedCategory != null && amountValue != null && title.isNotEmpty() && amountValue > 0) {
                    val finalAmount = if (selectedTransactionType == 0) amountValue else -amountValue

                    if (transactionId != null) {
                        // Update existing transaction
                        val updatedTransaction = Transaction(
                            id = transactionId,
                            category = selectedCategory!!.id,
                            time = getCurrentTime(),
                            transactionType = selectedTransactionType,
                            amount = finalAmount,
                            date = selectedDateMillis.toReadableDate(),
                            note = memo,
                            title = title
                        )
                        viewmodel.updateTransaction(updatedTransaction)
                    } else {
                        // Create new transaction
                        val newTransaction = Transaction(
                            category = selectedCategory!!.id,
                            time = getCurrentTime(),
                            transactionType = selectedTransactionType,
                            amount = finalAmount,
                            date = selectedDateMillis.toReadableDate(),
                            note = memo,
                            title = title
                        )
                        viewmodel.addTransaction(newTransaction)
                    }

                    scope.launch {
                        snackbarHostState.showSnackbar("Transaction saved 🎉")
                    }
                    onExpenseAdded()
                } else {
                    Toast.makeText(
                        context,
                        "Please select a category and enter a valid amount.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // All Dialogs (remain the same for both layouts)
    DialogComponents(
        isDarkMode = isDarkMode,
        surfaceColor = surfaceColor,
        textPrimaryColor = textPrimaryColor,
        textSecondaryColor = textSecondaryColor,
        borderColor = borderColor,
        showMemoDialog = showMemoDialog,
        onMemoDialogDismiss = { showMemoDialog = false },
        memo = memo,
        onMemoChange = { memo = it },
        showDeleteDialog = showDeleteDialog,
        categoryToDelete = categoryToDelete,
        onDeleteConfirm = {
            categoryViewModel.deleteCategory(categoryToDelete!!)
            if (selectedCategory == categoryToDelete) {
                selectedCategory = null
            }
            showDeleteDialog = false
            categoryToDelete = null
            Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
        },
        onDeleteDismiss = {
            showDeleteDialog = false
            categoryToDelete = null
        },
        showAddCategoryDialog = showAddCategoryDialog,
        newCategoryName = newCategoryName,
        onCategoryNameChange = { newCategoryName = it },
        selectedIconName = selectedIconName,
        onIconClick = { showIconDialog = true },
        selectedColor = selectedColor,
        onColorSelected = { selectedColor = it },
        onCategoryConfirm = {
            if (newCategoryName.isNotEmpty()) {
                categoryViewModel.addCategory(
                    name = newCategoryName,
                    iconName = selectedIconName,
                    colorValue = selectedColor.value.toLong()
                )
                newCategoryName = ""
                selectedIconName = "category"
                selectedColor = Color(0xFF4CAF50)
                showAddCategoryDialog = false
                Toast.makeText(context, "Category created successfully!", Toast.LENGTH_SHORT).show()
            }
        },
        onCategoryDismiss = {
            newCategoryName = ""
            selectedIconName = "category"
            selectedColor = Color(0xFF4CAF50)
            showAddCategoryDialog = false
        },
        showIconDialog = showIconDialog,
        onIconSelected = { iconName ->
            selectedIconName = iconName
            showIconDialog = false
        },
        onIconDialogDismiss = { showIconDialog = false }
    )
}

@Composable
fun LandscapeAddExpenseLayout(
    isDarkMode: Boolean,
    backgroundColor: Color,
    surfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    iconColor: Color,
    borderColor: Color,
    amount: String,
    onAmountChange: (String) -> Unit,
    memo: String,
    title:String,
    onTitleChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    selectedCategory: CategoryEntity?,
    onCategorySelected: (CategoryEntity) -> Unit,
    selectedTransactionType: Int,
    onTransactionTypeChange: (Int) -> Unit,
    selectedDateMillis: Long,
    onDateClick: () -> Unit,
    categories: List<CategoryEntity>,
    onAddCategoryClick: () -> Unit,
    onCategoryLongClick: (CategoryEntity) -> Unit,
    onMemoDialogOpen: () -> Unit,
    onBackClick: () -> Unit,
    onSave: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Left side - Categories and controls
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = iconColor
                    )
                }
                Text(
                    text = "Add Transaction",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )
                TextButton(
                    onClick = onSave,
                    enabled = selectedCategory != null && amount.isNotEmpty() &&
                            amount.toDoubleOrNull()?.let { it > 0 } == true
                ) {
                    Text(
                        "SAVE",
                        color = if (selectedCategory != null && amount.isNotEmpty() &&
                            amount.toDoubleOrNull()?.let { it > 0 } == true && title.isNotEmpty()) {
                            if (selectedTransactionType == 1) Orange else Color(0xFF4CAF50)
                        } else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(  modifier = Modifier
                .weight(1f) // give bounded height
                .fillMaxHeight()
                .verticalScroll(scrollState) )
            {
                // Transaction Type Toggle
                Text(
                    text = "Transaction Type",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = { onTransactionTypeChange(0) },
                        selected = selectedTransactionType == 0,
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Color(0xFF4CAF50),
                            activeContentColor = Color.White,
                            inactiveContainerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF0F0F0),
                            inactiveContentColor = textPrimaryColor
                        )
                    ) {
                        Text("Income")
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = { onTransactionTypeChange(1) },
                        selected = selectedTransactionType == 1,
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Orange,
                            activeContentColor = Color.White,
                            inactiveContainerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF0F0F0),
                            inactiveContentColor = textPrimaryColor
                        )
                    ) {
                        Text("Expense")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                //Title Section

                Text(
                    text = "Title",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(bottom = 4.dp) // smaller gap
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth() ,
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(8.dp), // slightly smaller radius
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // flat look
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = title,
                            onValueChange = { onTitleChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp, // 👈 smaller text
                                fontWeight = FontWeight.Medium,
                                color = textPrimaryColor
                            ),
                            cursorBrush = SolidColor(textPrimaryColor),
                            decorationBox = { innerTextField ->
                                if (title.isEmpty()) {
                                    Text(
                                        text = "Title",
                                        color = textPrimaryColor.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Selection
                Text(
                    text = "Date",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDateClick() },
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDateMillis.toReadableDate("MMM dd, yyyy"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimaryColor
                        )
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Categories
                Text(
                    text = "Select Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { category ->
                        CategoryItem(
                            category = category,
                            isSelected = selectedCategory == category,
                            onClick = { onCategorySelected(category) },
                            onLongClick = { onCategoryLongClick(category) },
                            selectedTransactionType = selectedTransactionType,
                            isDarkMode = isDarkMode,
                            textColor = textPrimaryColor
                        )
                    }
                    item {
                        AddCategoryItem(
                            onClick = onAddCategoryClick,
                            isDarkMode = isDarkMode,
                            textColor = textSecondaryColor
                        )
                    }
                }
            }
        }

        // Right side - Amount input and display
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Amount Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onMemoDialogOpen() }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text =when {
                                    memo.isEmpty() -> "Add note"
                                    memo.length > 100 -> memo.take(15) + "..." + " (Too long: ${memo.length} chars)"
                                    else -> memo.take(15) + if (memo.length > 15) "..." else ""
                                },
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedTransactionType == 0) "+" else "-",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = amount.ifEmpty { "0" },
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Number Input Pad
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enter Amount",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimaryColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val numbers = listOf(
                        listOf("1", "2", "3", "4"),
                        listOf("5", "6", "7", "8"),
                        listOf("9", "0", "•", "⌫"),
                    )

                    numbers.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { button ->
                                SimpleInputButton(
                                    text = button,
                                    onClick = {
                                        when (button) {
                                            "⌫" -> if (amount.isNotEmpty()) {
                                                onAmountChange(amount.dropLast(1))
                                            }
                                            "•" -> {
                                                if (!amount.contains("•") && amount.isNotEmpty()) {
                                                    onAmountChange(amount + button)
                                                } else if (amount.isEmpty()) {
                                                    onAmountChange("0.")
                                                }
                                            }
                                            else -> if (amount.length < 10) {
                                                onAmountChange(amount + button)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp),
                                    selectedTransactionType = selectedTransactionType,
                                    isDarkMode = isDarkMode,
                                    surfaceColor = surfaceColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SimpleInputButton(
                            text = "Clear",
                            onClick = { onAmountChange("") },
                            modifier = Modifier.padding(4.dp),
                            isWide = true,
                            selectedTransactionType = selectedTransactionType,
                            isDarkMode = isDarkMode,
                            surfaceColor = surfaceColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PortraitAddExpenseLayout(
    isDarkMode: Boolean,
    backgroundColor: Color,
    surfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    iconColor: Color,
    borderColor: Color,
    title: String,
    onTitleChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    memo: String,
    selectedCategory: CategoryEntity?,
    onCategorySelected: (CategoryEntity) -> Unit,
    selectedTransactionType: Int,
    onTransactionTypeChange: (Int) -> Unit,
    selectedDateMillis: Long,
    onDateClick: () -> Unit,
    categories: List<CategoryEntity>,
    onAddCategoryClick: () -> Unit,
    onCategoryLongClick: (CategoryEntity) -> Unit,
    onMemoDialogOpen: () -> Unit,
    onBackClick: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = iconColor
                )
            }
            Text(
                text = "Add Transaction",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimaryColor
            )
            TextButton(
                onClick = onSave,
                enabled = selectedCategory != null && amount.isNotEmpty() &&
                        amount.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text(
                    "SAVE",
                    color = if (selectedCategory != null && amount.isNotEmpty() && title.isNotEmpty()&&
                        amount.toDoubleOrNull()?.let { it > 0 } == true) {
                        if (selectedTransactionType == 1) Orange else Color(0xFF4CAF50)
                    } else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type Toggle
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Transaction Type",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimaryColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { onTransactionTypeChange(0) },
                            selected = selectedTransactionType == 0,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Color(0xFF4CAF50),
                                activeContentColor = Color.White,
                                inactiveContainerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF0F0F0),
                                inactiveContentColor = textPrimaryColor
                            )
                        ) {
                            Text("Income")
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { onTransactionTypeChange(1) },
                            selected = selectedTransactionType == 1,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Orange,
                                activeContentColor = Color.White,
                                inactiveContainerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF0F0F0),
                                inactiveContentColor = textPrimaryColor
                            )
                        ) {
                            Text("Expense")
                        }
                    }
                }
            }
            //Title Section

           item {

               Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                   Text(
                       text = "Title",
                       fontSize = 16.sp,
                       fontWeight = FontWeight.Medium,
                       color = textPrimaryColor,
                       modifier = Modifier.padding(bottom = 4.dp) // smaller gap
                   )

                   Card(
                       modifier = Modifier
                           .fillMaxWidth() ,
                       colors = CardDefaults.cardColors(containerColor = surfaceColor),
                       shape = RoundedCornerShape(8.dp), // slightly smaller radius
                       elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // flat look
                   ) {
                       Row(
                           modifier = Modifier
                               .fillMaxSize()
                               .padding(16.dp),
                           verticalAlignment = Alignment.CenterVertically
                       ) {
                           BasicTextField(
                               value = title,
                               onValueChange = { onTitleChange(it) },
                               modifier = Modifier
                                   .fillMaxWidth()
                                   .height(28.dp),
                               singleLine = true,
                               textStyle = androidx.compose.ui.text.TextStyle(
                                   fontSize = 14.sp, // 👈 smaller text
                                   fontWeight = FontWeight.Medium,
                                   color = textPrimaryColor
                               ),
                               cursorBrush = SolidColor(textPrimaryColor),
                               decorationBox = { innerTextField ->
                                   if (title.isEmpty()) {
                                       Text(
                                           text = "Title",
                                           color = textPrimaryColor.copy(alpha = 0.6f),
                                           fontSize = 14.sp
                                       )
                                   }
                                   innerTextField()
                               }
                           )
                       }
                   }
               }
           }
            // Date Selection
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Date",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimaryColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDateClick() },
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDateMillis.toReadableDate("MMM dd, yyyy"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimaryColor
                            )
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Select Date",
                                tint = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange
                            )
                        }
                    }
                }
            }

            // Categories Title
            item {
                Text(
                    text = "Select Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Categories Grid
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .height(3 * 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(categories) { category ->
                        CategoryItem(
                            category = category,
                            isSelected = selectedCategory == category,
                            onClick = { onCategorySelected(category) },
                            onLongClick = { onCategoryLongClick(category) },
                            selectedTransactionType = selectedTransactionType,
                            isDarkMode = isDarkMode,
                            textColor = textPrimaryColor
                        )
                    }
                    item {
                        AddCategoryItem(
                            onClick = onAddCategoryClick,
                            isDarkMode = isDarkMode,
                            textColor = textSecondaryColor
                        )
                    }
                }
            }

            // Amount Display Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onMemoDialogOpen() }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text =when {
                                        memo.isEmpty() -> "Add note"
                                        memo.length > 100 -> memo.take(15) + "..." + " (Too long: ${memo.length} chars)"
                                        else -> memo.take(15) + if (memo.length > 15) "..." else ""
                                    },
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedTransactionType == 0) "+" else "-",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = amount.ifEmpty { "0" },
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Number Input Pad
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Enter Amount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimaryColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val numbers = listOf(
                            listOf("1", "2", "3", "4"),
                            listOf("5", "6", "7", "8"),
                            listOf("9", "0", "•", "⌫"),
                        )

                        numbers.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { button ->
                                    SimpleInputButton(
                                        text = button,
                                        onClick = {
                                            when (button) {
                                                "⌫" -> if (amount.isNotEmpty()) {
                                                    onAmountChange(amount.dropLast(1))
                                                }
                                                "•" -> {
                                                    if (!amount.contains("•") && amount.isNotEmpty()) {
                                                        onAmountChange(amount + button)
                                                    } else if (amount.isEmpty()) {
                                                        onAmountChange("0.")
                                                    }
                                                }
                                                else -> if (amount.length < 10) {
                                                    onAmountChange(amount + button)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp),
                                        selectedTransactionType = selectedTransactionType,
                                        isDarkMode = isDarkMode,
                                        surfaceColor = surfaceColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            SimpleInputButton(
                                text = "Clear",
                                onClick = { onAmountChange("") },
                                modifier = Modifier.padding(4.dp),
                                isWide = true,
                                selectedTransactionType = selectedTransactionType,
                                isDarkMode = isDarkMode,
                                surfaceColor = surfaceColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogComponents(
    isDarkMode: Boolean,
    surfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    borderColor: Color,
    showMemoDialog: Boolean,
    onMemoDialogDismiss: () -> Unit,
    memo: String,
    onMemoChange: (String) -> Unit,
    showDeleteDialog: Boolean,
    categoryToDelete: CategoryEntity?,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    showAddCategoryDialog: Boolean,
    newCategoryName: String,
    onCategoryNameChange: (String) -> Unit,
    selectedIconName: String,
    onIconClick: () -> Unit,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onCategoryConfirm: () -> Unit,
    onCategoryDismiss: () -> Unit,
    showIconDialog: Boolean,
    onIconSelected: (String) -> Unit,
    onIconDialogDismiss: () -> Unit
) {
    // Memo Dialog
    if (showMemoDialog) {
        AlertDialog(
            onDismissRequest = onMemoDialogDismiss,
            title = {
                Text(
                    "Add Note",
                    color = textPrimaryColor
                )
            },
            text = {
                OutlinedTextField(
                    value = memo,
                    onValueChange = { newValue ->
                        if (newValue.length <= 100) {
                            onMemoChange(newValue)
                        }
                    },
                    placeholder = {
                        Text(
                            "Enter note...",
                            color = textSecondaryColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimaryColor,
                        unfocusedTextColor = textPrimaryColor,
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = borderColor,
                        cursorColor = Orange
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = onMemoDialogDismiss) {
                    Text("Done", color = Orange)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onMemoChange("")
                    onMemoDialogDismiss()
                }) {
                    Text("Clear", color = textSecondaryColor)
                }
            },
            containerColor = surfaceColor
        )
    }

    // Delete Category Dialog
    if (showDeleteDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = {
                Text(
                    "Delete Category",
                    color = textPrimaryColor
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete '${categoryToDelete.name}'? This action cannot be undone.",
                    color = textSecondaryColor
                )
            },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismiss) {
                    Text("Cancel", color = textSecondaryColor)
                }
            },
            containerColor = surfaceColor
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            categoryName = newCategoryName,
            onCategoryNameChange = onCategoryNameChange,
            selectedIconName = selectedIconName,
            onIconClick = onIconClick,
            selectedColor = selectedColor,
            onColorSelected = onColorSelected,
            onConfirm = onCategoryConfirm,
            onDismiss = onCategoryDismiss,
            isDarkMode = isDarkMode,
            surfaceColor = surfaceColor,
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor,
            borderColor = borderColor
        )
    }

    if (showIconDialog) {
        IconSelectionDialog(
            selectedIconName = selectedIconName,
            onIconSelected = onIconSelected,
            onDismiss = onIconDialogDismiss,
            isDarkMode = isDarkMode,
            surfaceColor = surfaceColor,
            textPrimaryColor = textPrimaryColor,
            textSecondaryColor = textSecondaryColor
        )
    }
}

@Composable
fun IconSelectionDialog(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean,
    surfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color
) {
    val iconsByCategory = IconManager.getIconsByCategory()
    var selectedCategory by remember { mutableStateOf("Financial") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = surfaceColor,
        title = {
            Text(
                "Choose Icon",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textPrimaryColor
            )
        },
        text = {
            Column(modifier = Modifier.height(400.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(iconsByCategory.keys.toList()) { category ->
                        FilterChip(
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    category,
                                    fontSize = 12.sp
                                )
                            },
                            selected = selectedCategory == category,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Orange,
                                selectedLabelColor = Color.White,
                                containerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF0F0F0),
                                labelColor = textPrimaryColor
                            )
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(iconsByCategory[selectedCategory] ?: emptyList()) { (iconName, icon) ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedIconName == iconName) Orange
                                    else if (isDarkMode) Color(0xFF3A3A3A) else Color.Gray.copy(
                                        alpha = 0.2f
                                    )
                                )
                                .clickable { onIconSelected(iconName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = iconName,
                                tint = if (selectedIconName == iconName) Color.White else textSecondaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = Orange, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun AddCategoryDialog(
    categoryName: String,
    onCategoryNameChange: (String) -> Unit,
    selectedIconName: String,
    onIconClick: () -> Unit,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean,
    surfaceColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    borderColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = surfaceColor,
        title = {
            Text(
                "Create New Category",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textPrimaryColor
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = onCategoryNameChange,
                    placeholder = {
                        Text(
                            "Category name",
                            color = textSecondaryColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimaryColor,
                        unfocusedTextColor = textPrimaryColor,
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = borderColor,
                        cursorColor = Orange
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Icon",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = textPrimaryColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .clickable { onIconClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconManager.getIconByName(selectedIconName),
                        contentDescription = "Selected Icon",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Color",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = textPrimaryColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                val colors = listOf(
                    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800),
                    Color(0xFFF44336), Color(0xFF9C27B0), Color(0xFF607D8B),
                    Color(0xFF795548), Color(0xFF009688), Color(0xFFFFEB3B),
                    Color(0xFFE91E63), Color(0xFF3F51B5), Color(0xFFCDDC39),
                    Color(0xFF00BCD4), Color(0xFFFF5722), Color(0xFF8BC34A),
                    Color(0xFF673AB7),
                )


                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(color) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = categoryName.isNotEmpty()
            ) {
                Text("CREATE", color = Orange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondaryColor)
            }
        }
    )
}

@Composable
fun CategoryItem(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    selectedTransactionType: Int,
    isDarkMode: Boolean = false,
    textColor: Color = Color.Black
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected && selectedTransactionType == 1) Orange
                    else if (isSelected && selectedTransactionType == 0) Color(0xFF4CAF50)
                    else category.colorValue.toColor()
                )
                .then(
                    if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = IconManager.getIconByName(category.iconName),
                contentDescription = category.name,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = category.name,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = if (isSelected) Orange else textColor
        )
    }
}

@Composable
fun AddCategoryItem(
    onClick: () -> Unit,
    isDarkMode: Boolean = false,
    textColor: Color = Color.Gray
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    if (isDarkMode) Color(0xFF3A3A3A) else Color.Gray.copy(alpha = 0.3f)
                )
                .border(
                    2.dp,
                    if (isDarkMode) Color(0xFF5A5A5A) else Color.Gray.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Category",
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = textColor
        )
    }
}

@Composable
fun SimpleInputButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWide: Boolean = false,
    selectedTransactionType: Int,
    isDarkMode: Boolean = false,
    surfaceColor: Color = Color.White
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenHeight = configuration.screenHeightDp.dp

    // Responsive sizing based on orientation
    val buttonHeight = if (isLandscape) {
        // In landscape, use smaller height to prevent overlap
        minOf(45.dp, screenHeight * 0.08f)
    } else {
        60.dp
    }

    val iconSize = if (isLandscape) 16.dp else 20.dp
    val fontSize = if (isLandscape) {
        if (text == "Clear") 14.sp else 16.sp
    } else {
        if (text == "Clear") 16.sp else 18.sp
    }

    val backgroundColor = when (text) {
        "Clear" -> if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange
        else -> surfaceColor
    }

    val textColor = when (text) {
        "⌫" -> if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange
        "Clear" -> Color.White
        else -> if (isDarkMode) Color.White else Color.Black
    }

    Surface(
        modifier = modifier
            .height(buttonHeight)
            .then(
                if (isWide) {
                    if (isLandscape) {
                        // In landscape, make wide buttons more compact
                        Modifier.fillMaxWidth(0.4f)
                    } else {
                        Modifier.fillMaxWidth(0.45f)
                    }
                } else {
                    if (isLandscape) {
                        // In landscape, use fixed width instead of aspect ratio to prevent overlap
                        Modifier.width(buttonHeight)
                    } else {
                        Modifier.aspectRatio(1f)
                    }
                }
            )
            .padding(if (isLandscape) 1.dp else 2.dp), // Smaller padding in landscape
        shape = if (isWide) RoundedCornerShape(if (isLandscape) 8.dp else 12.dp) else CircleShape,
        shadowElevation = if (isDarkMode) {
            if (isLandscape) 4.dp else 6.dp
        } else {
            if (isLandscape) 2.dp else 3.dp
        },
        color = backgroundColor,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (text == "⌫") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = if (selectedTransactionType == 0) Color(0xFF4CAF50) else Orange,
                    modifier = Modifier.size(iconSize)
                )
            } else {
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun getCurrentTime(): String {
    val now = LocalTime.now()
    val hour = now.hour
    val minute = now.minute
    val amPm = if (hour >= 12) "PM" else "AM"
    val formattedHour = if (hour % 12 == 0) 12 else hour % 12
    return String.format("%02d:%02d %s", formattedHour, minute, amPm)
}

fun Long.toReadableDate(pattern: String = "dd MM yyyy"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

