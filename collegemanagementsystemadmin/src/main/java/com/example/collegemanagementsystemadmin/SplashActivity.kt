package com.example.collegemanagementsystemadmin

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : CoreBaseActivity() {

    private lateinit var txtAppName: TextView
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show system splash
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.imgLogo)
        txtAppName = findViewById(R.id.txtAppName)

        // Fade + scale logo animation
        val fadeLogo = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).apply {
            duration = 1200
        }
        val scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.7f, 1f).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.7f, 1f).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
        }

        fadeLogo.start()
        scaleX.start()
        scaleY.start()

        // Title fade-in
        ObjectAnimator.ofFloat(txtAppName, "alpha", 0f, 1f).apply {
            duration = 800
            startDelay = 600
            start()
        }

        // After animation, navigate
        fadeLogo.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                navigateToNext()
            }
        })
    }

    private fun navigateToNext() {
        lifecycleScope.launch {
            // Small delay for smooth transition
            delay(500)

            val currentUser = auth.currentUser

            val intent = if (currentUser != null) {
                // Already logged in → Dashboard
                Intent(this@SplashActivity, AdminDashboardActivity::class.java)
            } else {
                // Not logged in → Login/Intro
                Intent(this@SplashActivity, NextActivity::class.java)
            }

            startActivity(intent)
            finish()
        }
    }
}
