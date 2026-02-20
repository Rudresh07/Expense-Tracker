package com.rudy.expensetracker.analytics

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

class FirebaseAnalytics{

    fun logEvent(event: String, params: Map<String, String>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                putString(key, value)
            }
        }
        Firebase.analytics.logEvent(event, bundle)
    }

}
