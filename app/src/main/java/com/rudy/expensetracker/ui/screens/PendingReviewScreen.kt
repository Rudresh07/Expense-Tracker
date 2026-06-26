package com.rudy.expensetracker.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudy.expensetracker.model.CategoryEntity
import com.rudy.expensetracker.model.TransactionWithCategory
import com.rudy.expensetracker.notifications.NotificationHelper
import com.rudy.expensetracker.sms.WalletVpaDetector
import com.rudy.expensetracker.ui.theme.Orange
import com.rudy.expensetracker.viewmodel.CategoryViewModel
import com.rudy.expensetracker.viewmodel.TransactionViewmodel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import kotlin.math.abs

@Composable
fun PendingReviewScreen(
    onBackClick: () -> Unit,
    notificationHelper: NotificationHelper,
) {
    val viewModel: TransactionViewmodel = koinViewModel()
    val categoryViewModel: CategoryViewModel = koinViewModel()
    val pendingTransactions by viewModel.pendingReviewTransactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val isDarkMode = isSystemInDarkTheme()
    val context = LocalContext.current

    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color.Black
    val textSecondary = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray
    val borderColor = if (isDarkMode) Color(0xFF3A3A3A) else Color(0xFFE0E0E0)

    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showIconDialog by rememberSaveable { mutableStateOf(false) }
    var newCategoryName by rememberSaveable { mutableStateOf("") }
    var selectedIconName by rememberSaveable { mutableStateOf("category") }
    var selectedColor by remember { mutableStateOf(Color(0xFF4CAF50)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary,
                )
            }
            Text(
                text = "Review Transactions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (pendingTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You're all caught up!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No transactions need your review right now.",
                        fontSize = 14.sp,
                        color = textSecondary,
                    )
                }
            }
        } else {
            Text(
                text = "${pendingTransactions.size} transaction${if (pendingTransactions.size != 1) "s" else ""} need categorisation",
                fontSize = 13.sp,
                color = textSecondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(pendingTransactions, key = { it.transaction.id }) { txnWithCategory ->
                    PendingReviewCard(
                        txnWithCategory = txnWithCategory,
                        categories = categories,
                        isDarkMode = isDarkMode,
                        surfaceColor = surfaceColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onAddCategoryClick = { showAddCategoryDialog = true },
                        onCategorySelected = { categoryId ->
                            val txn = txnWithCategory.transaction
                            val rawVpa = Regex("""[A-Za-z0-9._\-]+@[A-Za-z0-9]+""").find(txn.note).let { it?.value }
                            val isWalletProxy = rawVpa != null && WalletVpaDetector.isWalletProxy(rawVpa)
                            viewModel.confirmCategory(
                                txnId = txn.id,
                                categoryId = categoryId,
                                merchantKey = txn.title.lowercase().trim(),
                                isWalletProxy = isWalletProxy,
                            )
                            notificationHelper.cancelNotification(txn.id)
                        }
                    )
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            categoryName = newCategoryName,
            onCategoryNameChange = { newCategoryName = it },
            selectedIconName = selectedIconName,
            onIconClick = { showIconDialog = true },
            selectedColor = selectedColor,
            onColorSelected = { selectedColor = it },
            onConfirm = {
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
            onDismiss = {
                newCategoryName = ""
                selectedIconName = "category"
                selectedColor = Color(0xFF4CAF50)
                showAddCategoryDialog = false
            },
            isDarkMode = isDarkMode,
            surfaceColor = surfaceColor,
            textPrimaryColor = textPrimary,
            textSecondaryColor = textSecondary,
            borderColor = borderColor,
        )
    }

    if (showIconDialog) {
        IconSelectionDialog(
            selectedIconName = selectedIconName,
            onIconSelected = { iconName ->
                selectedIconName = iconName
                showIconDialog = false
            },
            onDismiss = { showIconDialog = false },
            isDarkMode = isDarkMode,
            surfaceColor = surfaceColor,
            textPrimaryColor = textPrimary,
            textSecondaryColor = textSecondary,
        )
    }
}

@Composable
private fun PendingReviewCard(
    txnWithCategory: TransactionWithCategory,
    categories: List<CategoryEntity>,
    isDarkMode: Boolean,
    surfaceColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onAddCategoryClick: () -> Unit,
    onCategorySelected: (Int) -> Unit,
) {
    val txn = txnWithCategory.transaction

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 4.dp else 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(text = txn.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = textPrimary)
                    Text(text = "${txn.date} · ${txn.time}", fontSize = 12.sp, color = textSecondary)
                }
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%.2f", abs(txn.amount))}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFF5252),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Choose category:", fontSize = 12.sp, color = textSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            // Build a unified item list: all categories + one "add" slot at the end
            val itemCount = categories.size + 1
            val rows = (0 until itemCount).chunked(4)
            rows.forEach { indices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    indices.forEach { i ->
                        Box(modifier = Modifier.weight(1f)) {
                            if (i < categories.size) {
                                CategoryItem(
                                    category = categories[i],
                                    isSelected = false,
                                    onClick = { onCategorySelected(categories[i].id) },
                                    selectedTransactionType = txn.transactionType,
                                    isDarkMode = isDarkMode,
                                    textColor = textPrimary,
                                )
                            } else {
                                AddCategoryItem(
                                    onClick = onAddCategoryClick,
                                    isDarkMode = isDarkMode,
                                    textColor = textSecondary,
                                )
                            }
                        }
                    }
                    repeat(4 - indices.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
