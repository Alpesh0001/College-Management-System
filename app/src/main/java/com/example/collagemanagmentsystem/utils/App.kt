package com.example.collagemanagmentsystem.utils

import android.app.Application
import com.example.collagemanagmentsystem.utils.SyncManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        // ✅ Firestore offline cache
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        // ✅ Network monitor
        NetworkMonitor.start(this)

        // ✅ Start background sync if already logged in
        val session = SessionManager(this)
        if (session.isLoggedIn()) {
            SyncManager.startBackgroundSync(this)
        }
    }
}
