package com.example.collegemanagementsystemfaculty

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity
import com.example.collegemanagementsystemfaculty.utils.SessionManager

@SuppressLint("CustomSplash")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoContainer = findViewById<View>(R.id.logoContainer)
        val bottomBranding = findViewById<View>(R.id.bottomBranding)

        // ✅ Modern Entrance Animation
        animateEntrance(logoContainer, bottomBranding)

        // ✅ Session Logic
        Handler(Looper.getMainLooper()).postDelayed({
            checkSessionAndNavigate()
        }, 2500) // Slightly longer to allow animation to play
    }

    private fun animateEntrance(main: View, bottom: View) {
        // Main Logo Container Animation (Scale + Fade)
        main.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Bottom Branding Animation (Fade In)
        bottom.postDelayed({
            bottom.animate()
                .alpha(1f)
                .setDuration(800)
                .start()
        }, 500)
    }

    private fun checkSessionAndNavigate() {
        val session = SessionManager(this)
        val intent = if (session.isLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
        
        // ✅ Smooth Activity Transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
