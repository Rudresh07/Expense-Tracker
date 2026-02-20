package com.rudy.expensetracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rudy.expensetracker.ui.screens.AddExpenseScreen
import com.rudy.expensetracker.ui.screens.AllTransactionsScreen
import com.rudy.expensetracker.ui.screens.AuthScreen
import com.rudy.expensetracker.ui.screens.DashboardScreen
import com.rudy.expensetracker.ui.screens.ExpenseStatisticsScreen
import com.rudy.expensetracker.ui.screens.SplashScreen

@Composable
fun ExpenseNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate("auth") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("auth") {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                onAddExpenseClick = { navController.navigate("add_expense") },
                onStatisticsClick = { navController.navigate("statistics") },
                onLogoutClick = {
                    navController.navigate("auth") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onViewAllTransactions = {
                    navController.navigate("all_transactions")
                }
            )
        }
        composable("add_expense") {
            AddExpenseScreen(
                transactionId = null, // new expense
                onBackClick = { navController.popBackStack() },
                onExpenseAdded = { navController.popBackStack() }
            )
        }

        composable(
            "add_expense/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.IntType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getInt("transactionId")
            AddExpenseScreen(
                transactionId = transactionId,
                onBackClick = { navController.popBackStack() },
                onExpenseAdded = { navController.popBackStack() }
            )
        }

        composable("statistics") {
            ExpenseStatisticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable ("all_transactions"){
            AllTransactionsScreen(
                onBackClick = {navController.popBackStack()},
                onEditTransaction = { transactionId -> navController.navigate("add_expense/$transactionId") },
            )
        }
    }
}