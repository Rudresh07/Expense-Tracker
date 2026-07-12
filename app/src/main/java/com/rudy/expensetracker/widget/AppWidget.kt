package com.rudy.expensetracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.rudy.expensetracker.MainActivity
import com.rudy.expensetracker.model.TransactionWithCategory
import com.rudy.expensetracker.repository.TransactionRepository
import com.rudy.expensetracker.utils.PreferenceManager
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

// Glance's single-arg ColorProvider is @RestrictTo(LIBRARY_GROUP); use it via suppression
// since the day/night overload doesn't exist in the current Glance version.
@Suppress("RestrictedApi")
private fun color(color: Color): ColorProvider = ColorProvider(color)

// --- Widget color palette ---
private val CardBg = Color(0xFF1C1C1E)
private val OrangeAccent = Color(0xFFFF6B35)
private val AddBtnBg = Color(0xFF2A2A2A)
private val GreenPillBg = Color(0xFF1A3A1A)
private val GreenText = Color(0xFF4CAF50)
private val RedPillBg = Color(0xFF3A1A1A)
private val RedText = Color(0xFFFF5252)
private val ProgressBg = Color(0xFF3A3A3A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB0B0B0)

private val HorizontalPadding = 16.dp
private val EXPANDED_HEIGHT_THRESHOLD = 180.dp

const val EXTRA_NAVIGATE_TO = "navigate_to"
const val ROUTE_ADD_EXPENSE = "add_expense"

// --- Widget data container (mirrors what the app already has) ---
data class WidgetState(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val expenseTransactions: List<TransactionWithCategory> = emptyList(),
)

class AppWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    suspend fun updateAll(context: Context) {
        GlanceAppWidgetManager(context)
            .getGlanceIds(javaClass)
            .forEach { update(context, it) }  // calls provideGlance for each placed widget
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = GlobalContext.get().get<TransactionRepository>()
        val prefs = GlobalContext.get().get<PreferenceManager>()

        val dateFormatter = DateTimeFormatter.ofPattern("dd MM yyyy")

        provideContent {
            // Collect Room flows directly inside the composition.
            // Any DB write causes the flows to emit → widget recomposes automatically,
            // same as a regular Compose screen observing a ViewModel StateFlow.
            val allTransactions by repository.allTransactions.collectAsState(initial = emptyList())
            val totalBalance by repository.totalBalance.collectAsState(initial = 0.0)
            val totalIncome by repository.totalIncome.collectAsState(initial = 0.0)

            // Expense-only rows, most recent first
            val expenseTransactions = remember(allTransactions) {
                allTransactions.filter { it.transaction.amount < 0 }.take(4)
            }

            // Net monthly spend = expense − income (0 if income > expense)
            val monthlyExpense = remember(allTransactions) {
                val now = LocalDate.now()
                (-allTransactions.filter { tx ->
                    runCatching {
                        val d = LocalDate.parse(tx.transaction.date, dateFormatter)
                        d.year == now.year && d.monthValue == now.monthValue
                    }.getOrDefault(false)
                }.sumOf { it.transaction.amount }).coerceAtLeast(0.0)
            }

            WidgetContent(
                WidgetState(
                    totalBalance = totalBalance,
                    totalIncome = totalIncome,
                    monthlyExpense = monthlyExpense,
                    // SharedPreferences isn't a Flow; updateAll() from the repository
                    // ensures the widget picks up budget changes from the app.
                    monthlyBudget = prefs.getMonthlyBudget(),
                    expenseTransactions = expenseTransactions,
                )
            )
        }
    }
}

private fun formatAmount(amount: Double): String {
    val abs = "%.2f".format(kotlin.math.abs(amount))
    return if (amount < 0) "-₹$abs" else "₹$abs"
}

@Composable
fun WidgetContent(state: WidgetState = WidgetState()) {
    val size = LocalSize.current
    val isExpanded = size.height >= EXPANDED_HEIGHT_THRESHOLD
    val contentWidth: Dp = size.width - HorizontalPadding * 2
    val context = LocalContext.current

    // Tapping anywhere on the widget opens the app
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(20.dp)
            .padding(HorizontalPadding)
            .clickable(actionStartActivity(openAppIntent)),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            WidgetHeader(isExpanded, context)
            Spacer(modifier = GlanceModifier.height(10.dp))
            BalanceSection(state.totalBalance)
            Spacer(modifier = GlanceModifier.height(8.dp))
            StatPills(state.totalIncome, state.monthlyExpense)

            if (isExpanded) {
                // Show budget bar only when a budget has been set
                if (state.monthlyBudget > 0) {
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    val progress = (state.monthlyExpense / state.monthlyBudget)
                        .toFloat().coerceIn(0f, 1f)
                    MonthlyBudgetSection(
                        progress = progress,
                        monthlyExpense = state.monthlyExpense,
                        budgetLimit = state.monthlyBudget,
                        contentWidth = contentWidth,
                    )
                }
                Spacer(modifier = GlanceModifier.height(10.dp))
                // Scrollable expense list
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(state.expenseTransactions, itemId = { it.transaction.id.toLong() }) { tx ->
                        TransactionRow(tx)
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                }
            } else {
                Spacer(modifier = GlanceModifier.height(10.dp))
                state.expenseTransactions.firstOrNull()?.let { TransactionRow(it) }
            }
        }
    }
}

@Composable
private fun WidgetHeader(isExpanded: Boolean, context: android.content.Context) {
    val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
        putExtra(EXTRA_NAVIGATE_TO, ROUTE_ADD_EXPENSE)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .background(OrangeAccent)
                .cornerRadius(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("E", style = TextStyle(color = color(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = "ExpenseTracker",
            style = TextStyle(color = color(TextPrimary), fontSize = 13.sp, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.defaultWeight(),
        )
        // "+ Add" overrides the root clickable for its own area
        Box(
            modifier = GlanceModifier
                .background(if (isExpanded) OrangeAccent else AddBtnBg)
                .cornerRadius(8.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .clickable(actionStartActivity(addExpenseIntent)),
            contentAlignment = Alignment.Center,
        ) {
            Text("+ Add", style = TextStyle(color = color(Color.White), fontSize = 11.sp, fontWeight = FontWeight.Medium))
        }
    }
}

@Composable
private fun BalanceSection(totalBalance: Double) {
    Column {
        Text(
            text = formatAmount(totalBalance),
            style = TextStyle(color = color(TextPrimary), fontSize = 26.sp, fontWeight = FontWeight.Bold),
        )
        Text(
            text = "TOTAL BALANCE",
            style = TextStyle(color = color(TextSecondary), fontSize = 10.sp, fontWeight = FontWeight.Normal),
        )
    }
}

@Composable
private fun StatPills(totalIncome: Double, monthlyExpense: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = GlanceModifier
                .background(GreenPillBg)
                .cornerRadius(20.dp)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "↗ Income ${"%.0f".format(totalIncome)}",
                style = TextStyle(color = color(GreenText), fontSize = 11.sp, fontWeight = FontWeight.Medium),
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier
                .background(RedPillBg)
                .cornerRadius(20.dp)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "↘ Month ${"%.0f".format(monthlyExpense)}",
                style = TextStyle(color = color(RedText), fontSize = 11.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
private fun MonthlyBudgetSection(
    progress: Float,
    monthlyExpense: Double,
    budgetLimit: Double,
    contentWidth: Dp,
) {
    val usedPercent = (progress * 100).toInt()
    val filledWidth: Dp = contentWidth * progress
    val barColor = if (progress >= 0.9f) RedText else OrangeAccent

    Column {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Monthly Budget",
                style = TextStyle(color = color(TextSecondary), fontSize = 11.sp),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = "$usedPercent% used",
                style = TextStyle(color = color(if (progress >= 0.9f) RedText else TextSecondary), fontSize = 11.sp),
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(6.dp)
                .background(ProgressBg)
                .cornerRadius(3.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = GlanceModifier
                    .width(filledWidth)
                    .fillMaxHeight()
                    .background(barColor)
                    .cornerRadius(3.dp),
            ) {}
        }
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = "₹${"%.0f".format(monthlyExpense)} of ₹${"%.0f".format(budgetLimit)}",
            style = TextStyle(color = color(TextSecondary), fontSize = 10.sp),
        )
    }
}

@Composable
private fun TransactionRow(tx: TransactionWithCategory) {
    val iconColor = Color(tx.category.colorValue)

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(36.dp)
                .background(iconColor.copy(alpha = 0.2f))
                .cornerRadius(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tx.category.name.first().toString(),
                style = TextStyle(color = color(iconColor), fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
        }
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = tx.transaction.title,
                style = TextStyle(color = color(TextPrimary), fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
            Text(
                text = tx.transaction.time,
                style = TextStyle(color = color(TextSecondary), fontSize = 11.sp),
            )
        }
        Text(
            text = formatAmount(tx.transaction.amount),
            style = TextStyle(color = color(RedText), fontSize = 13.sp, fontWeight = FontWeight.Medium),
        )
    }
}
