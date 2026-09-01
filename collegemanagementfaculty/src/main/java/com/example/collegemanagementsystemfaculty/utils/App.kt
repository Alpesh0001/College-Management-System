package com.example.collegemanagementsystemfaculty.utils

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        // ✅ ENABLE OFFLINE PERSISTENCE (Like Hotel Project)
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        NetworkMonitor.start(this)

        CloudinaryHelper.init(this)


    }
}
