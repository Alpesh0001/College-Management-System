package com.example.collegemanagementsystemadmin.utils

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // ✅ Enable Firestore offline persistence with unlimited cache
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        // Start global network listener
        NetworkMonitor.start(this)
    }
}
