package com.example.collagemanagmentsystem

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import com.example.collagemanagmentsystem.utils.CoreBaseActivity
import com.example.collagemanagmentsystem.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class ChangePasswordActivity : CoreBaseActivity() {

    // ── Views ─────────────────────────────────
    private lateinit var tilCurrentPassword: TextInputLayout
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var tvError: android.widget.TextView
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var tvCancel: android.widget.TextView
    private lateinit var tvForgotPassword: android.widget.TextView

    // ── Data ──────────────────────────────────
    private lateinit var session: SessionManager
    private val db = FirebaseFirestore.getInstance()
    private var studentDocId = ""
    private var currentStoredPassword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        session = SessionManager(this)
        studentDocId = session.getStudentId() ?: ""

        if (studentDocId.isEmpty()) {
            Toast.makeText(this, "❌ Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupButtons()
        loadCurrentPassword()
    }

    // ─────────────────────────────────────────
    private fun bindViews() {
        tilCurrentPassword  = findViewById(R.id.tilCurrentPassword)
        etCurrentPassword   = findViewById(R.id.etCurrentPassword)
        tilNewPassword      = findViewById(R.id.tilNewPassword)
        etNewPassword       = findViewById(R.id.etNewPassword)
        tilConfirmPassword  = findViewById(R.id.tilConfirmPassword)
        etConfirmPassword   = findViewById(R.id.etConfirmPassword)
        tvError             = findViewById(R.id.tvError)
        btnChangePassword   = findViewById(R.id.btnChangePassword)
        tvCancel            = findViewById(R.id.tvCancel)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)


    }

    // ─────────────────────────────────────────
    private fun setupButtons() {
        btnChangePassword.setOnClickListener {
            val currentPass = etCurrentPassword.text.toString().trim()
            val newPass     = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            // ── Clear errors ──
            tilCurrentPassword.error    = null
            tilNewPassword.error        = null
            tilConfirmPassword.error    = null
            hideError()

            // ── Validations ──
            if (currentPass.isEmpty()) {
                tilCurrentPassword.error = "Enter your current password"
                etCurrentPassword.requestFocus()
                return@setOnClickListener
            }
            if (currentPass != currentStoredPassword) {
                tilCurrentPassword.error = "❌ Current password is incorrect"
                etCurrentPassword.requestFocus()
                return@setOnClickListener
            }
            if (newPass.isEmpty()) {
                tilNewPassword.error = "Enter your new password"
                etNewPassword.requestFocus()
                return@setOnClickListener
            }
            if (newPass.length < 6) {
                tilNewPassword.error = "Password must be at least 6 characters"
                etNewPassword.requestFocus()
                return@setOnClickListener
            }
            if (newPass == currentStoredPassword) {
                tilNewPassword.error = "New password cannot be same as current"
                etNewPassword.requestFocus()
                return@setOnClickListener
            }
            if (confirmPass.isEmpty()) {
                tilConfirmPassword.error = "Please confirm your password"
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }
            if (newPass != confirmPass) {
                tilConfirmPassword.error = "Passwords do not match"
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            // ✅ All valid — save password
            showBlockingLoader("Updating password...")
            btnChangePassword.isEnabled = false
            saveNewPassword(newPass)
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvCancel.setOnClickListener {
            finish()
        }
    }

    // ─────────────────────────────────────────
    private fun loadCurrentPassword() {
        showBlockingLoader("Loading profile...")
        db.collection("students")
            .document(studentDocId)
            .get()
            .addOnSuccessListener { doc ->
                hideBlockingLoader()
                if (doc.exists()) {
                    currentStoredPassword = doc.getString("password") ?: ""
                }
            }
            .addOnFailureListener {
                hideBlockingLoader()
            }
    }

    // ─────────────────────────────────────────
    private fun saveNewPassword(newPassword: String) {
        db.collection("students")
            .document(studentDocId)
            .update(
                mapOf(
                    "password" to newPassword
                )
            )
            .addOnSuccessListener {
                hideBlockingLoader()
                btnChangePassword.isEnabled = true

                // ✅ Success message
                showSuccess("✅ Password changed successfully!")
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 1500)
            }
            .addOnFailureListener { e ->
                hideBlockingLoader()
                btnChangePassword.isEnabled = true
                showError("❌ Failed to update password: ${e.message}")
            }
    }

    // ─────────────────────────────────────────
    private fun showError(message: String) {
        tvError.setTextColor(resources.getColor(R.color.colorError, null))
        tvError.text       = message
        tvError.visibility = View.VISIBLE
    }

    private fun showSuccess(message: String) {
        tvError.setTextColor(resources.getColor(R.color.colorSuccess, null))
        tvError.text       = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}
