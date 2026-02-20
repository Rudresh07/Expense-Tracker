package com.rudy.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudy.expensetracker.model.Transaction
import com.rudy.expensetracker.model.TransactionWithCategory
import com.rudy.expensetracker.ui.theme.Orange
import com.rudy.expensetracker.utils.IconManager
import com.rudy.expensetracker.utils.toColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

// Add this to your AllTransactionsScreen.kt file
// Update the AllTransactionItem composable to include click handling

@Composable
fun AllTransactionItem(
    transaction: TransactionWithCategory,
    isLandscape: Boolean = false,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    onDeleteClick: (Transaction) -> Unit,
    onEditClick: (Int) -> Unit,
    onItemClick: (Int) -> Unit = {} // Add this parameter
) {
    val displayDate = try {
        LocalDate.parse(transaction.transaction.date, DateTimeFormatter.ofPattern("dd MM yyyy"))
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: Exception) {
        transaction.transaction.date
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(transaction.transaction.id) }, // Add clickable modifier
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLandscape) 2.dp else 4.dp),
        shape = RoundedCornerShape(if (isLandscape) 12.dp else 16.dp)
    ) {
        // Rest of your existing AllTransactionItem code remains the same
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLandscape) 10.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
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
            ) {
                Column(horizontalAlignment = Alignment.End) {
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