package com.example.collagemanagmentsystem

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.collagemanagmentsystem.utils.SessionManager
import com.example.collagemanagmentsystem.utils.SyncManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

@SuppressLint("CustomSplash")
class SplashActivity : AppCompatActivity() {

    private lateinit var mainLayout: ConstraintLayout
    private lateinit var logoContainer: View
    private lateinit var appName: View
    private lateinit var tagline: View
    private lateinit var progressBar: View
    private lateinit var tvSyncStatus: TextView  // ✅ Add this TextView in XML

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        initViews()
        setupInsets()
        startCleanAnimation()

        // ✅ After animation starts → check session + sync
        Handler(Looper.getMainLooper()).postDelayed({
            checkSessionAndSync()
        }, 1200) // start sync after logo animation
    }

    private fun initViews() {
        mainLayout    = findViewById(R.id.main)
        logoContainer = findViewById(R.id.logo_container)
        appName       = findViewById(R.id.app_name)
        tagline       = findViewById(R.id.tagline)
        progressBar   = findViewById(R.id.loading_indicator)
        tvSyncStatus  = findViewById(R.id.tvSyncStatus)

        logoContainer.alpha  = 0f
        logoContainer.scaleX = 0.7f
        logoContainer.scaleY = 0.7f
        appName.alpha     = 0f
        tagline.alpha     = 0f
        progressBar.alpha = 0f
        tvSyncStatus.alpha = 0f
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, 0, bars.right, 0)
            insets
        }
    }

    private fun startCleanAnimation() {
        logoContainer.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(900)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        appName.animate()
            .alpha(1f).setDuration(700).setStartDelay(300).start()

        tagline.animate()
            .alpha(1f).setDuration(700).setStartDelay(600).start()

        progressBar.animate()
            .alpha(1f).setDuration(500).setStartDelay(900).start()
    }

    // ─────────────────────────────────────────────
    // ✅ STEP 1: Check session → if logged in sync
    // ─────────────────────────────────────────────
    private fun checkSessionAndSync() {
        val session = SessionManager(this)

        if (!session.isLoggedIn()) {
            navigateTo(LoginActivity::class.java)
            return
        }

        // ✅ Check if sync needed
        if (SyncManager.isSyncNeeded(this)) {
            // Do full sync
            SyncManager.syncNow(
                context = this,
                onStatus = { msg ->
                    showSyncStatus(msg)
                },
                onComplete = {
                    runOnUiThread {
                        Handler(Looper.getMainLooper()).postDelayed({
                            navigateTo(MainActivity::class.java)
                        }, 400)
                    }
                },
                onError = { _ ->
                    runOnUiThread {
                        showSyncStatus("Offline mode 📶")
                        Handler(Looper.getMainLooper()).postDelayed({
                            navigateTo(MainActivity::class.java)
                        }, 800)
                    }
                }
            )
        } else {
            // ✅ Already synced recently → skip → go directly
            showSyncStatus("Up to date ✅")
            Handler(Looper.getMainLooper()).postDelayed({
                navigateTo(MainActivity::class.java)
            }, 600)
        }
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────
    private fun showSyncStatus(message: String) {
        runOnUiThread {
            tvSyncStatus.text = message
            tvSyncStatus.animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun navigateTo(destination: Class<*>) {
        mainLayout.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                startActivity(Intent(this, destination))
                overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                finish()
            }
            .start()
    }
}
