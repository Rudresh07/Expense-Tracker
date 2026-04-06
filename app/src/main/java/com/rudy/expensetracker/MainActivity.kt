package com.rudy.expensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rudy.expensetracker.navigation.ExpenseNavGraph
import com.rudy.expensetracker.sms.PastSmsScanWorker
import com.rudy.expensetracker.ui.theme.ExpenseTrackerTheme
import com.rudy.expensetracker.utils.PreferenceManager
import android.util.Log
import com.rudy.expensetracker.widget.EXTRA_NAVIGATE_TO
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val preferenceManager: PreferenceManager by inject()

    // Request RECEIVE_SMS + READ_SMS together
    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val readGranted = results[Manifest.permission.READ_SMS] == true
        val receiveGranted = results[Manifest.permission.RECEIVE_SMS] == true
        Log.d(TAG, "Permission result — READ_SMS=$readGranted, RECEIVE_SMS=$receiveGranted")
        if (readGranted) {
            enqueuePastSmsScanIfNeeded()
        } else {
            Log.w(TAG, "READ_SMS denied — past SMS scan will not run")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        val readGranted    = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)    == PackageManager.PERMISSION_GRANTED
        val receiveGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "onCreate — READ_SMS=$readGranted, RECEIVE_SMS=$receiveGranted, scanDone=${preferenceManager.isInitialSmsScanDone()}")

        if (!readGranted || !receiveGranted) {
            Log.d(TAG, "Launching permission request")
            smsPermissionLauncher.launch(
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            )
        } else {
            // Permissions already granted (returning user) — run scan if not done yet
            enqueuePastSmsScanIfNeeded()
        }

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

    private fun enqueuePastSmsScanIfNeeded() {
        if (!preferenceManager.isInitialSmsScanDone()) {
            Log.d(TAG, "Enqueueing PastSmsScanWorker")
            PastSmsScanWorker.enqueue(this)
            preferenceManager.setInitialSmsScanDone()
        } else {
            Log.d(TAG, "Scan already done, skipping enqueue")
        }
    }

    companion object {
        private const val TAG = "PastSmsScan"
    }
}