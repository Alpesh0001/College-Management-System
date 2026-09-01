package com.example.collagemanagmentsystem.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

object SyncManager {

    private const val TAG = "SyncManager"
    private const val PREF_NAME = "sync_prefs"
    private const val KEY_LAST_SYNC = "last_sync_time"
    private const val SYNC_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

    private val firestore = FirebaseFirestore.getInstance()

    // ─────────────────────────────────────────────
    // ✅ Check if sync is needed
    // ─────────────────────────────────────────────
    fun isSyncNeeded(context: Context): Boolean {
        val lastSync = getLastSyncTime(context)
        val now = System.currentTimeMillis()
        val needed = (now - lastSync) > SYNC_INTERVAL_MS
        Log.d(TAG, "isSyncNeeded=$needed | lastSync=${lastSync} | diff=${(now - lastSync) / 1000}s")
        return needed
    }

    // ─────────────────────────────────────────────
    // ✅ Full sync — call from Splash or anywhere
    // ─────────────────────────────────────────────
    fun syncNow(
        context: Context,
        onStatus: (String) -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val session = SessionManager(context)

        if (!session.isLoggedIn()) {
            Log.d(TAG, "syncNow: not logged in → skip")
            onComplete()
            return
        }

        val studentId = session.getStudentId()
        val divisionId = session.getDivisionId()
        val rawSem = session.getSemester() ?: "Sem 1"
        val semNum = rawSem.filter { it.isDigit() }
        val courseId = session.getCourseId()
        val rawYear = session.getYear() ?: "1st Year"
        val yearNum = rawYear.filter { it.isDigit() }

        if (studentId.isNullOrEmpty()) {
            Log.d(TAG, "syncNow: studentId null → skip")
            onComplete()
            return
        }

        Log.d(TAG, "syncNow: starting full sync")
        val tasks = mutableListOf<Boolean>() // track completions
        val totalTasks = 4
        var completed = 0

        fun checkAllDone() {
            completed++
            Log.d(TAG, "syncNow: task $completed/$totalTasks done")
            if (completed >= totalTasks) {
                saveLastSyncTime(context)
                Log.d(TAG, "syncNow: all done ✅")
                onStatus("All synced ✅")
                onComplete()
            }
        }

        // ── Task 1: Student Profile ──────────────────────
        onStatus("Syncing profile...")
        firestore.collection("students")
            .document(studentId)
            .get(Source.SERVER)
            .addOnSuccessListener {
                Log.d(TAG, "sync: students ✅")
                checkAllDone()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "sync: students ❌ ${e.message}")
                checkAllDone() // still continue
            }

        // ── Task 2: Attendance Summary ───────────────────
        onStatus("Syncing attendance...")
        firestore.collection("studentSummary")
            .document("${studentId}_${semNum}")
            .get(Source.SERVER)
            .addOnSuccessListener {
                Log.d(TAG, "sync: studentSummary ✅")
                checkAllDone()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "sync: studentSummary ❌ ${e.message}")
                checkAllDone()
            }

        // ── Task 3: Timetable ────────────────────────────
        onStatus("Syncing timetable...")
        if (!divisionId.isNullOrEmpty()) {
            firestore.collection("divisions")
                .document(divisionId)
                .collection("timetable")
                .get(Source.SERVER)
                .addOnSuccessListener {
                    Log.d(TAG, "sync: timetable ✅")
                    checkAllDone()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "sync: timetable ❌ ${e.message}")
                    checkAllDone()
                }
        } else {
            checkAllDone() // skip if no divisionId
        }

        // ── Task 4: Assignments ──────────────────────────
        onStatus("Syncing assignments...")
        if (!courseId.isNullOrEmpty()) {
            firestore.collection("assignments")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("year", yearNum)
                .whereEqualTo("semester", semNum)
                .get(Source.SERVER)
                .addOnSuccessListener {
                    Log.d(TAG, "sync: assignments ✅ count=${it.size()}")
                    checkAllDone()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "sync: assignments ❌ ${e.message}")
                    checkAllDone()
                }
        } else {
            checkAllDone()
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Background Sync — call from MainActivity
    // Runs every 15 min using Handler
    // ─────────────────────────────────────────────
    private var backgroundHandler: android.os.Handler? = null
    private var backgroundRunnable: Runnable? = null

    fun startBackgroundSync(context: Context) {
        stopBackgroundSync() // stop any existing

        backgroundHandler = android.os.Handler(android.os.Looper.getMainLooper())
        backgroundRunnable = object : Runnable {
            override fun run() {
                Log.d(TAG, "background sync: triggered")
                if (isSyncNeeded(context)) {
                    syncNow(context)
                } else {
                    Log.d(TAG, "background sync: not needed yet")
                }
                // ✅ Schedule next run after 15 min
                backgroundHandler?.postDelayed(this, SYNC_INTERVAL_MS)
            }
        }
        // ✅ First background sync after 15 min
        backgroundHandler?.postDelayed(backgroundRunnable!!, SYNC_INTERVAL_MS)
        Log.d(TAG, "startBackgroundSync: started ✅")
    }

    fun stopBackgroundSync() {
        backgroundRunnable?.let { backgroundHandler?.removeCallbacks(it) }
        backgroundHandler = null
        backgroundRunnable = null
        Log.d(TAG, "stopBackgroundSync: stopped")
    }

    // ─────────────────────────────────────────────
    // HELPERS — Last Sync Time
    // ─────────────────────────────────────────────
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getLastSyncTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SYNC, 0L)
    }

    fun saveLastSyncTime(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
        Log.d(TAG, "saveLastSyncTime: saved ✅")
    }

    fun clearSyncTime(context: Context) {
        getPrefs(context).edit().remove(KEY_LAST_SYNC).apply()
    }
}
