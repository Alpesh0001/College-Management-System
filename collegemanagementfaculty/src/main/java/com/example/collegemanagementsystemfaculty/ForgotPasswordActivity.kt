package com.example.collegemanagementsystemfaculty

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.collegemanagementsystemfaculty.utils.CoreBaseActivity
import com.example.collegemanagementsystemfaculty.utils.EmailSender
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class ForgotPasswordActivity : CoreBaseActivity() {

    // ── Views ─────────────────────────────────

    // Header
    private lateinit var ivHeaderIcon: ImageView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvHeaderSubtitle: TextView

    // Global Error
    private lateinit var tvError: TextView

    // Step 1
    private lateinit var layoutStep1: LinearLayout
    private lateinit var tilEmployeeId: TextInputLayout
    private lateinit var etEmployeeId: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSendOtp: MaterialButton

    // Step 2
    private lateinit var layoutStep2: LinearLayout
    private lateinit var tvEmailInfo: TextView
    private lateinit var tilOtp: TextInputLayout
    private lateinit var etOtp: TextInputEditText
    private lateinit var tvResendOtp: TextView
    private lateinit var btnVerifyOtp: MaterialButton

    // Step 3
    private lateinit var layoutStep3: LinearLayout
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var tvPassError: TextView
    private lateinit var btnSavePassword: MaterialButton

    // Bottom
    private lateinit var tvBackToLogin: TextView

    // ── Data ──────────────────────────────────
    private val db           = FirebaseFirestore.getInstance()
    private var generatedOtp = ""
    private var facultyDocId = ""
    private var facultyEmail = ""
    private var facultyName  = ""
    private var currentStep  = 1

    // Resend Timer
    private var resendTimer: CountDownTimer? = null

    // ─────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        bindViews()
        showStep(1)
        setupButtons()
    }

    // ─────────────────────────────────────────
    // ✅ Bind All Views
    // ─────────────────────────────────────────
    private fun bindViews() {
        ivHeaderIcon     = findViewById(R.id.ivHeaderIcon)
        tvHeaderTitle    = findViewById(R.id.tvHeaderTitle)
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle)
        tvError          = findViewById(R.id.tvError)

        // Step 1
        layoutStep1    = findViewById(R.id.layoutStep1)
        tilEmployeeId  = findViewById(R.id.tilStudentId)   // ✅ same xml id
        etEmployeeId   = findViewById(R.id.etStudentId)    // ✅ same xml id
        tilEmail       = findViewById(R.id.tilEmail)
        etEmail        = findViewById(R.id.etEmail)
        btnSendOtp     = findViewById(R.id.btnSendOtp)

        // Step 2
        layoutStep2  = findViewById(R.id.layoutStep2)
        tvEmailInfo  = findViewById(R.id.tvEmailInfo)
        tilOtp       = findViewById(R.id.tilOtp)
        etOtp        = findViewById(R.id.etOtp)
        tvResendOtp  = findViewById(R.id.tvResendOtp)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)

        // Step 3
        layoutStep3        = findViewById(R.id.layoutStep3)
        tilNewPassword     = findViewById(R.id.tilNewPassword)
        etNewPassword      = findViewById(R.id.etNewPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etConfirmPassword  = findViewById(R.id.etConfirmPassword)
        tvPassError        = findViewById(R.id.tvPassError)
        btnSavePassword    = findViewById(R.id.btnSavePassword)

        // Bottom
        tvBackToLogin = findViewById(R.id.tvBackToLogin)
    }

    // ─────────────────────────────────────────
    // ✅ Show Step — hide others, update header
    // ─────────────────────────────────────────
    private fun showStep(step: Int) {
        currentStep = step
        hideError()

        layoutStep1.visibility = View.GONE
        layoutStep2.visibility = View.GONE
        layoutStep3.visibility = View.GONE

        when (step) {
            1 -> {
                layoutStep1.visibility = View.VISIBLE
                ivHeaderIcon.setImageResource(R.drawable.ic_lock)
                tvHeaderTitle.text    = "Forgot Password?"
                tvHeaderSubtitle.text = "RECOVER YOUR ACCOUNT"
                tvBackToLogin.text    = "Back to Login"

                // ✅ Update hint for Faculty
                tilEmployeeId.hint = "Employee ID"
            }
            2 -> {
                layoutStep2.visibility = View.VISIBLE
                ivHeaderIcon.setImageResource(R.drawable.ic_email)
                tvHeaderTitle.text    = "Verify OTP"
                tvHeaderSubtitle.text = "CHECK YOUR INBOX"
                tvBackToLogin.text    = "← Back"
                tvEmailInfo.text      = "OTP sent to ${maskEmail(facultyEmail)}"
                startResendTimer()
            }
            3 -> {
                layoutStep3.visibility = View.VISIBLE
                ivHeaderIcon.setImageResource(R.drawable.ic_lock)
                tvHeaderTitle.text    = "Set New Password"
                tvHeaderSubtitle.text = "CREATE STRONG PASSWORD"
                tvBackToLogin.text    = "Back to Login"
            }
        }
    }

    // ─────────────────────────────────────────
    // ✅ Setup All Buttons
    // ─────────────────────────────────────────
    private fun setupButtons() {

        // ── STEP 1: Send OTP ──────────────────
        btnSendOtp.setOnClickListener {
            val empId = etEmployeeId.text.toString().trim()
            val email = etEmail.text.toString().trim()

            tilEmployeeId.error = null
            tilEmail.error      = null

            if (empId.isEmpty()) {
                tilEmployeeId.error = "Enter your Employee ID"
                etEmployeeId.requestFocus()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                tilEmail.error = "Enter your registered email"
                etEmail.requestFocus()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email address"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            hideError()
            showBlockingLoader("Verifying details...")
            setStep1Enabled(false)
            verifyFacultyAndSendOtp(empId, email)
        }

        // ── STEP 2: Verify OTP ────────────────
        btnVerifyOtp.setOnClickListener {
            val enteredOtp = etOtp.text.toString().trim()

            tilOtp.error = null

            if (enteredOtp.isEmpty()) {
                tilOtp.error = "Enter the OTP"
                return@setOnClickListener
            }
            if (enteredOtp.length != 6) {
                tilOtp.error = "Enter a valid 6-digit OTP"
                return@setOnClickListener
            }
            if (enteredOtp != generatedOtp) {
                tilOtp.error = "❌ Incorrect OTP. Please try again."
                return@setOnClickListener
            }

            // ✅ OTP Correct
            tilOtp.error = null
            resendTimer?.cancel()
            hideError()
            setStep2Enabled(false)

            showBlockingLoader("Verifying OTP...")
            Handler(Looper.getMainLooper()).postDelayed({
                showBlockingLoader("OTP Verified! ✅")
                Handler(Looper.getMainLooper()).postDelayed({
                    hideBlockingLoader()
                    setStep2Enabled(true)
                    showStep(3)
                }, 800)
            }, 800)
        }

        // ── STEP 2: Resend OTP ────────────────
        tvResendOtp.setOnClickListener {
            if (tvResendOtp.isEnabled) resendOtp()
        }

        // ── STEP 3: Save Password ─────────────
        btnSavePassword.setOnClickListener {
            val newPass     = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            tvPassError.visibility   = View.GONE
            tilNewPassword.error     = null
            tilConfirmPassword.error = null

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

            hideError()
            showBlockingLoader("Saving new password...")
            setStep3Enabled(false)
            saveNewPassword(newPass)
        }

        // ── Back to Login / Back ──────────────
        tvBackToLogin.setOnClickListener {
            when (currentStep) {
                1 -> finish()
                2 -> {
                    resendTimer?.cancel()
                    showStep(1)
                }
                3 -> finish()
            }
        }
    }

    // ─────────────────────────────────────────
    // ✅ STEP 1 — Verify Faculty in Firestore
    // ─────────────────────────────────────────
    private fun verifyFacultyAndSendOtp(empId: String, email: String) {
        db.collection("faculties")
            .whereEqualTo("employeeId", empId)
            .whereEqualTo("status", "Active")
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->

                if (snap.isEmpty) {
                    hideBlockingLoader()
                    setStep1Enabled(true)
                    showError("❌ Employee ID not found or account inactive")
                    return@addOnSuccessListener
                }

                val doc            = snap.documents[0]
                val storedEmail    = doc.getString("email").orEmpty()
                val passwordStatus = doc.getString("passwordStatus").orEmpty()

                // Email mismatch
                if (!email.equals(storedEmail, ignoreCase = true)) {
                    hideBlockingLoader()
                    setStep1Enabled(true)
                    showError("❌ Email does not match our records")
                    return@addOnSuccessListener
                }

                // Not registered yet
                if (!passwordStatus.equals("active", ignoreCase = true)) {
                    hideBlockingLoader()
                    setStep1Enabled(true)
                    showError("⚠️ You haven't registered yet! Please register first.")
                    return@addOnSuccessListener
                }

                // ✅ All verified — generate & send OTP
                facultyDocId = doc.id
                facultyEmail = storedEmail
                facultyName  = doc.getString("fullName").orEmpty()
                generatedOtp = Random.nextInt(100000, 999999).toString()

                showBlockingLoader("Sending OTP to your email...")

                EmailSender.sendOtp(
                    toEmail   = facultyEmail,
                    toName    = facultyName,
                    otp       = generatedOtp,
                    onSuccess = {
                        runOnUiThread {
                            hideBlockingLoader()
                            setStep1Enabled(true)
                            showStep(2)
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            hideBlockingLoader()
                            setStep1Enabled(true)
                            showError("❌ Failed to send OTP: $error")
                        }
                    }
                )
            }
            .addOnFailureListener { e ->
                hideBlockingLoader()
                setStep1Enabled(true)
                showError("❌ Error: ${e.localizedMessage}")
            }
    }

    // ─────────────────────────────────────────
    // ✅ STEP 2 — Resend OTP
    // ─────────────────────────────────────────
    private fun resendOtp() {
        generatedOtp = Random.nextInt(100000, 999999).toString()

        showBlockingLoader("Sending new OTP...")
        setStep2Enabled(false)
        startResendTimer()

        EmailSender.sendOtp(
            toEmail   = facultyEmail,
            toName    = facultyName,
            otp       = generatedOtp,
            onSuccess = {
                runOnUiThread {
                    hideBlockingLoader()
                    setStep2Enabled(true)
                    showSuccess("✅ OTP resent successfully!")
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    hideBlockingLoader()
                    setStep2Enabled(true)
                    showError("❌ Failed to resend OTP: $error")
                }
            }
        )
    }

    // ─────────────────────────────────────────
    // ✅ STEP 3 — Save New Password
    // ─────────────────────────────────────────
    private fun saveNewPassword(newPassword: String) {
        if (facultyDocId.isEmpty()) {
            hideBlockingLoader()
            setStep3Enabled(true)
            showError("❌ Invalid session. Please try again.")
            return
        }

        db.collection("faculties").document(facultyDocId)
            .update(
                mapOf(
                    "password"       to newPassword,
                    "passwordStatus" to "active"
                )
            )
            .addOnSuccessListener {
                showBlockingLoader("Password saved! Redirecting to login...")
                Handler(Looper.getMainLooper()).postDelayed({
                    hideBlockingLoader()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("passwordReset", true)
                    }
                    startActivity(intent)
                    finish()
                }, 1000)
            }
            .addOnFailureListener { e ->
                hideBlockingLoader()
                setStep3Enabled(true)
                showError("❌ Failed to save password: ${e.message}")
            }
    }

    // ─────────────────────────────────────────
    // ✅ 60s Resend Timer
    // ─────────────────────────────────────────
    private fun startResendTimer() {
        tvResendOtp.isEnabled = false
        tvResendOtp.setTextColor(
            resources.getColor(R.color.grey_text, null)
        )
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvResendOtp.text = "Resend (${millisUntilFinished / 1000}s)"
            }
            override fun onFinish() {
                tvResendOtp.text      = "Resend OTP"
                tvResendOtp.isEnabled = true
                tvResendOtp.setTextColor(
                    resources.getColor(R.color.colorPrimary, null)
                )
            }
        }.start()
    }

    // ─────────────────────────────────────────
    // ✅ Enable / Disable Helpers
    // ─────────────────────────────────────────
    private fun setStep1Enabled(enabled: Boolean) {
        btnSendOtp.isEnabled    = enabled
        etEmployeeId.isEnabled  = enabled
        etEmail.isEnabled       = enabled
    }

    private fun setStep2Enabled(enabled: Boolean) {
        btnVerifyOtp.isEnabled  = enabled
        etOtp.isEnabled         = enabled
        tvResendOtp.isEnabled   = enabled
    }

    private fun setStep3Enabled(enabled: Boolean) {
        btnSavePassword.isEnabled   = enabled
        etNewPassword.isEnabled     = enabled
        etConfirmPassword.isEnabled = enabled
    }

    // ─────────────────────────────────────────
    // ✅ UI Helpers
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
        Handler(Looper.getMainLooper()).postDelayed({
            tvError.visibility = View.GONE
        }, 3000)
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }

    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 2) return email
        val visible = email.substring(0, 2)
        val masked  = "*".repeat(atIndex - 2)
        val domain  = email.substring(atIndex)
        return "$visible$masked$domain"
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }
}
