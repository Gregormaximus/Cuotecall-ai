package com.kuote.agent

import android.app.Application
import com.google.firebase.FirebaseApp

class KuoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase explicitly
        FirebaseApp.initializeApp(this)
    }
}
