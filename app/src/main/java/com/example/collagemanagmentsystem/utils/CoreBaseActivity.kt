package com.example.collagemanagmentsystem.utils

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
import com.example.collagemanagmentsystem.SplashActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class CoreBaseActivity : AppCompatActivity() {

    private var overlay: View? = null
    private var dialog: AlertDialog? = null
    private var showJob: Job? = null
    private var lastConnected: Boolean? = null
    private var loaderView: ImageView? = null
    private var statusText: TextView? = null
    private var progressRing: ImageView? = null

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        attachOverlayOnTop()

        lifecycleScope.launch {
            NetworkMonitor.isConnected.collect { connected ->
                val prev = lastConnected
                lastConnected = connected

                if (connected) {
                    hideOverlay()

                    // ✅ Internet just came back → trigger sync if needed
                    if (prev == false) {
                        onNetworkRestored()
                    }
                    return@collect
                }

                overlay?.visibility = View.VISIBLE
                startLoaderAnimation()
                startStatusSequence()

                showJob?.cancel()
                showJob = lifecycleScope.launch {
                    delay(7000)
                    if (!NetworkMonitor.isConnected.value) showDialog()
                }
            }
        }
    }

    // ✅ Called when internet comes back
    // Override in any Activity/Fragment if needed
    open fun onNetworkRestored() {
        val session = SessionManager(this)
        if (session.isLoggedIn() && SyncManager.isSyncNeeded(this)) {
            SyncManager.syncNow(context = this)
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
            com.example.collagemanagmentsystem.R.id.blockingOverlay
        )
        if (existing != null) {
            overlay = existing
            loaderView = existing.findViewById(com.example.collagemanagmentsystem.R.id.imgLoader)
            statusText = existing.findViewById(com.example.collagemanagmentsystem.R.id.tvStatus)
            progressRing = existing.findViewById(com.example.collagemanagmentsystem.R.id.progressRing)
            overlay?.visibility = View.GONE
            return
        }

        val overlayView = LayoutInflater.from(this).inflate(
            com.example.collagemanagmentsystem.R.layout.view_blocking_loader,
            root, false
        )

        overlay = overlayView
        loaderView = overlayView.findViewById(com.example.collagemanagmentsystem.R.id.imgLoader)
        statusText = overlayView.findViewById(com.example.collagemanagmentsystem.R.id.tvStatus)
        progressRing = overlayView.findViewById(com.example.collagemanagmentsystem.R.id.progressRing)
        overlay?.visibility = View.GONE
        root.addView(overlayView)
    }

    // ═══════════════════════════════════════════════════════════════
    // ✅ PUBLIC LOADING METHODS
    // ═══════════════════════════════════════════════════════════════

    fun showBlockingLoader(message: String) {
        overlay?.visibility = View.VISIBLE
        statusText?.text = message
        statusText?.visibility = View.VISIBLE
        progressRing?.visibility = View.VISIBLE
        startLoaderAnimation()
    }

    fun hideBlockingLoader() {
        overlay?.visibility = View.GONE
        stopLoaderAnimation()
        progressRing?.visibility = View.GONE
    }

    fun showProgressRing() {
        overlay?.visibility = View.VISIBLE
        progressRing?.visibility = View.VISIBLE
        statusText?.visibility = View.GONE
        startLoaderAnimation()
    }

    fun hideProgressRing() {
        hideBlockingLoader()
    }

    private fun hideOverlay() {
        stopLoaderAnimation()
        overlay?.visibility = View.GONE
        dialog?.dismiss()
        showJob?.cancel()
    }

    private fun startLoaderAnimation() {
        val v = loaderView ?: return
        val anim = AnimationUtils.loadAnimation(
            this,
            com.example.collagemanagmentsystem.R.anim.progress_ring_rotate
        )
        v.startAnimation(anim)
    }

    private fun stopLoaderAnimation() {
        loaderView?.clearAnimation()
    }

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
            com.example.collagemanagmentsystem.R.layout.dialog_no_internet, null
        )

        dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog?.window?.setBackgroundDrawableResource(
            com.example.collagemanagmentsystem.R.color.transparent
        )
        dialog?.show()

        dialogView.findViewById<View>(
            com.example.collagemanagmentsystem.R.id.tvTryAgain
        ).setOnClickListener {
            dialog?.dismiss()
            overlay?.visibility = View.VISIBLE
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
        }
    }
}
