package com.example.collegemanagementsystemadmin.utils

import android.R
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.collegemanagementsystemadmin.SplashActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

open class CoreBaseActivity : AppCompatActivity() {

    private var overlay: View? = null
    private var dialog: AlertDialog? = null
    private var showJob: Job? = null
    private var lastConnected: Boolean? = null

    // loader + status views inside overlay
    private var loaderView: ImageView? = null
    private var statusText: TextView? = null

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        attachOverlayOnTop()

        lifecycleScope.launch {
            NetworkMonitor.isConnected.collect { connected ->
                val prev = lastConnected
                lastConnected = connected

                if (connected) {
                    hideOverlay()
                    return@collect
                }

                // show overlay immediately when offline
                overlay?.visibility = View.VISIBLE
                startLoaderAnimation()
                startStatusSequence()   // change text one by one

                if (prev == true) {
                    // went from online -> offline: start delayed check
                    showJob?.cancel()
                    showJob = lifecycleScope.launch {
                        delay(7000)      // 7 seconds; change to 5000–10000 ms as you want
                        if (!NetworkMonitor.isConnected.value) showDialog()
                    }
                } else {
                    // already offline when Activity created: also delay showing dialog
                    showJob?.cancel()
                    showJob = lifecycleScope.launch {
                        delay(7000)
                        if (!NetworkMonitor.isConnected.value) showDialog()
                    }
                }
            }
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        attachOverlayOnTop()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        attachOverlayOnTop()
    }

    private fun attachOverlayOnTop() {
        val root = findViewById<View>(R.id.content) as? ViewGroup ?: return

        val existing = root.findViewById<View>(
            com.example.collegemanagementsystemadmin.R.id.blockingOverlay
        )
        if (existing != null) {
            overlay = existing
            loaderView = existing.findViewById(
                com.example.collegemanagementsystemadmin.R.id.imgLoader
            )
            statusText = existing.findViewById(
                com.example.collegemanagementsystemadmin.R.id.tvStatus
            )
            overlay?.visibility = View.GONE
            return
        }

        val overlayView = LayoutInflater.from(this)
            .inflate(
                com.example.collegemanagementsystemadmin.R.layout.view_blocking_loader,
                root,
                false
            )

        overlay = overlayView
        loaderView = overlayView.findViewById(
            com.example.collegemanagementsystemadmin.R.id.imgLoader
        )
        statusText = overlayView.findViewById(
            com.example.collegemanagementsystemadmin.R.id.tvStatus
        )
        overlay?.visibility = View.GONE
        root.addView(overlayView)
    }

    private fun hideOverlay() {
        stopLoaderAnimation()
        overlay?.visibility = View.GONE
        dialog?.dismiss()
        showJob?.cancel()
    }

    // start rotate animation on custom loader image
    private fun startLoaderAnimation() {
        val v = loaderView ?: return
        val anim = AnimationUtils.loadAnimation(
            this,
            com.example.collegemanagementsystemadmin.R.anim.progress_ring_rotate
        )
        v.startAnimation(anim)
    }

    // stop rotate animation
    private fun stopLoaderAnimation() {
        loaderView?.clearAnimation()
    }

    // change status text step by step while waiting
    private fun startStatusSequence() {
        lifecycleScope.launch {
            statusText?.text = "Checking connection..."
            delay(2500)
            if (!NetworkMonitor.isConnected.value) {
                statusText?.text = "Still trying to connect..."
            }
            delay(2500)
            if (!NetworkMonitor.isConnected.value) {
                statusText?.text = "No internet yet, please wait..."
            }
        }
    }

    private fun showDialog() {
        if (dialog?.isShowing == true) return

        val dialogView = layoutInflater.inflate(
            com.example.collegemanagementsystemadmin.R.layout.dialog_no_internet,
            null
        )

        dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
        dialog?.show()

        val tvTryAgain =
            dialogView.findViewById<View>(com.example.collegemanagementsystemadmin.R.id.tvTryAgain)

        tvTryAgain.setOnClickListener {
            dialog?.dismiss()
            overlay?.visibility = View.VISIBLE

            // restart SAME Activity; if still offline, dialog shows again
//            val i = intent
//            i.addFlags(
//                Intent.FLAG_ACTIVITY_NEW_TASK or
//                        Intent.FLAG_ACTIVITY_CLEAR_TASK
//            )
//            startActivity(i)
//         go to Splash
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
