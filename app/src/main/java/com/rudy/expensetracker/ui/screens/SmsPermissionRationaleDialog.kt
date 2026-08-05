package com.rudy.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.rudy.expensetracker.ui.theme.LightOrange
import com.rudy.expensetracker.ui.theme.Orange

@Composable
fun SmsPermissionRationaleDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        icon = {
            Row(
                modifier = Modifier
                    .size(56.dp)
                    .background(LightOrange, CircleShape),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Sms,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Enable automatic transaction detection",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "ExpenseTracker reads incoming SMS to detect bank and UPI " +
                        "transactions and log them for you automatically.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                RationaleBullet("Detects bank & UPI transactions from SMS")
                RationaleBullet("Adds them to your expense log — no manual entry needed")
                RationaleBullet(
                    "For this month, all existing SMS messages will be scanned once and " +
                        "matching transactions will be added to the app automatically"
                )
                RationaleBullet("You can still add and track expenses manually if you skip this")
            }
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text("Allow SMS Access", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

@Composable
private fun RationaleBullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "•  ",
            style = MaterialTheme.typography.bodyMedium,
            color = Orange,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
