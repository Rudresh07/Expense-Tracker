package com.rudy.expensetracker

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.util.Log
import com.google.firebase.BuildConfig
import com.rudy.expensetracker.navigation.ExpenseNavGraph
import com.rudy.expensetracker.notifications.NotificationHelper
import com.rudy.expensetracker.sms.AutoConfirmWorker
import com.rudy.expensetracker.sms.PastSmsScanWorker
import com.rudy.expensetracker.ui.theme.ExpenseTrackerTheme
import com.rudy.expensetracker.utils.PreferenceManager
import com.rudy.expensetracker.widget.EXTRA_NAVIGATE_TO
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val preferenceManager: PreferenceManager by inject()
    private val notificationHelper: NotificationHelper by inject()

    // Request RECEIVE_SMS + READ_SMS together
    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val readGranted = results[Manifest.permission.READ_SMS] == true
        val receiveGranted = results[Manifest.permission.RECEIVE_SMS] == true
        Log.d(TAG, "Permission result — READ_SMS=$readGranted, RECEIVE_SMS=$receiveGranted")
        if (readGranted) {
            enqueuePastSmsScanIfNeeded()
            promptBatteryOptimizationIfNeeded()
        } else {
            Log.w(TAG, "READ_SMS denied — past SMS scan will not run")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "POST_NOTIFICATIONS granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        notificationHelper.createChannel()
        AutoConfirmWorker.enqueue(this)

        if (BuildConfig.DEBUG) {
            com.google.firebase.analytics.FirebaseAnalytics
                .getInstance(this)
                .setUserProperty("tester", "true")
        }

        val readGranted    = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)    == PackageManager.PERMISSION_GRANTED
        val receiveGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "onCreate — READ_SMS=$readGranted, RECEIVE_SMS=$receiveGranted, scanDone=${preferenceManager.isInitialSmsScanDone()}")

        if (!readGranted || !receiveGranted) {
            showSmsPermissionRationale()
        } else {
            // Permissions already granted (returning user) — run scan if not done yet
            enqueuePastSmsScanIfNeeded()
            promptBatteryOptimizationIfNeeded()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val deepLinkRoute = intent.getStringExtra(EXTRA_NAVIGATE_TO)

        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExpenseNavGraph(deepLinkRoute = deepLinkRoute)
                }
            }
        }
    }

    private fun showSmsPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Automatic transaction detection")
            .setMessage(
                "ExpenseTracker can read incoming SMS messages to automatically detect bank " +
                "and UPI transactions and add them to your expense log, so you don't have to " +
                "enter every transaction by hand. This requires SMS permissions.\n\n" +
                "If you don't grant this, you can still add and track expenses manually."
            )
            .setPositiveButton("Continue") { _, _ ->
                Log.d(TAG, "Launching permission request")
                smsPermissionLauncher.launch(
                    arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                )
            }
            .setNegativeButton("Not Now") { _, _ ->
                Log.w(TAG, "User declined SMS permission rationale")
            }
            .setCancelable(false)
            .show()
    }

    private fun enqueuePastSmsScanIfNeeded() {
        if (!preferenceManager.isInitialSmsScanDone()) {
            Log.d(TAG, "Enqueueing PastSmsScanWorker")
            PastSmsScanWorker.enqueue(this)
            preferenceManager.setInitialSmsScanDone()
        } else {
            Log.d(TAG, "Scan already done, skipping enqueue")
        }
    }

    private fun promptBatteryOptimizationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val pm = getSystemService(PowerManager::class.java) ?: return
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        if (preferenceManager.isBatteryOptimizationPromptShown()) return

        preferenceManager.setBatteryOptimizationPromptShown()

        AlertDialog.Builder(this)
            .setTitle("Enable background SMS reading")
            .setMessage(
                "To automatically detect transactions from SMS messages in the background, " +
                "please disable battery optimization for this app. " +
                "Without this, some SMS alerts may be missed when the app is closed."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
            .setNegativeButton("Not Now", null)
            .setCancelable(true)
            .show()
    }

    companion object {
        private const val TAG = "PastSmsScan"
    }
}