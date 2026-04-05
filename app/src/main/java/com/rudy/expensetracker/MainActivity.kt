package com.rudy.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rudy.expensetracker.ui.theme.ExpenseTrackerTheme
import com.rudy.expensetracker.navigation.ExpenseNavGraph
import com.rudy.expensetracker.widget.EXTRA_NAVIGATE_TO

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        val deepLinkRoute = intent.getStringExtra(EXTRA_NAVIGATE_TO)

        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExpenseNavGraph(deepLinkRoute = deepLinkRoute)
                }
            }
        }
    }
}

