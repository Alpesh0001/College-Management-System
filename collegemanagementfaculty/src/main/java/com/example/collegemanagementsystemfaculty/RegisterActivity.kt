package com.example.collegemanagementsystemfaculty

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.collegemanagementsystemfaculty.utils.CoreBaseActivity
import com.example.collegemanagementsystemfaculty.utils.EmailSender
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class RegisterActivity : CoreBaseActivity() {

    // ── Step 1 Views ───────────────────────────────────
    private lateinit var layoutStep1: LinearLayout
    private lateinit var etEmployeeId: TextInputEditText
    private lateinit var etTempPassword: TextInputEditText
    private lateinit var btnVerifyTemp: MaterialButton

    // ── Step 2 Views ───────────────────────────────────
    private lateinit var layoutStep2: LinearLayout
    private lateinit var tvEmailInfo: TextView
    private lateinit var etOtp: TextInputEditText
    private lateinit var tvResendOtp: TextView
    private lateinit var tvOtpError: TextView
    private lateinit var btnVerifyOtp: MaterialButton

    // ── Step 3 Views ───────────────────────────────────
    private lateinit var layoutStep3: LinearLayout
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var tvPassError: TextView
    private lateinit var btnSavePass: MaterialButton

    // ✅ Animation Views — updated
    private lateinit var logoGlow: View
    private lateinit var logoContainer: View
    private lateinit var registerCard: View

    // ── Common ─────────────────────────────────────────
    private lateinit var tvError: TextView
    private lateinit var tvBackToLogin: TextView

    private val db = FirebaseFirestore.getInstance()

    private var generatedOtp      = ""
    private var facultyId         = ""
    private var facultyEmail      = ""
    private var facultyName       = ""
    private var facultyRole       = ""
    private var facultyCourseId   = ""
    private var facultyCourseName = ""
    private var facultyCourseCode = ""
    private var facultyDesignation = ""
    private var facultyPhone      = ""
    private var facultyEmployeeId = ""
    private var facultyPhotoUrl   = ""

    private var resendTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        bindViews()
        startEntranceAnimations()  // ✅ animation before showStep
        setupButtons()
        showStep(1)
    }

    // ─────────────────────────────────────────────────
    // ✅ Bind Views — updated IDs
    // ─────────────────────────────────────────────────
    private fun bindViews() {
        // Layouts
        layoutStep1 = findViewById(R.id.layoutStep1)
        layoutStep2 = findViewById(R.id.layoutStep2)
        layoutStep3 = findViewById(R.id.layoutStep3)

        // Step 1
        etEmployeeId   = findViewById(R.id.etEmployeeId)
        etTempPassword = findViewById(R.id.etTempPassword)
        btnVerifyTemp  = findViewById(R.id.btnVerifyTemp)

        // Step 2
        tvEmailInfo  = findViewById(R.id.tvEmailInfo)
        etOtp        = findViewById(R.id.etOtp)
        tvOtpError   = findViewById(R.id.tvOtpError)
        tvResendOtp  = findViewById(R.id.tvResendOtp)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)

        // Step 3
        etNewPassword     = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        tvPassError       = findViewById(R.id.tvPassError)
        btnSavePass       = findViewById(R.id.btnSavePass)

        // Common
        tvError       = findViewById(R.id.tvError)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        // ✅ Animation views — match new XML
        logoGlow      = findViewById(R.id.logo_outer_glow)
        logoContainer = findViewById(R.id.logo_container)
        registerCard  = findViewById(R.id.registerCard)

        // ✅ Initial animation states
        logoGlow.alpha       = 0f
        logoGlow.scaleX      = 0.7f
        logoGlow.scaleY      = 0.7f
        logoContainer.alpha  = 0f
        logoContainer.scaleX = 0.7f
        logoContainer.scaleY = 0.7f
        registerCard.alpha        = 0f
        registerCard.translationY = 200f
    }

    // ─────────────────────────────────────────────────
    // ✅ Entrance Animation — same as Login screen
    // ─────────────────────────────────────────────────
    private fun startEntranceAnimations() {

        // Logo glow scale + fade in
        logoGlow.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(900)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Logo container scale + fade in
        logoContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(900)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Card slides up + fades in
        registerCard.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(700)
            .setStartDelay(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    // ─────────────────────────────────────────────────
    // ✅ Buttons — unchanged
    // ─────────────────────────────────────────────────
    private fun setupButtons() {

        btnVerifyTemp.setOnClickListener {
            val empId    = etEmployeeId.text.toString().trim()
            val tempPass = etTempPassword.text.toString().trim()

            if (empId.isEmpty()) {
                etEmployeeId.error = "Enter Employee ID"
                return@setOnClickListener
            }
            if (tempPass.isEmpty()) {
                etTempPassword.error = "Enter temporary password"
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            showBlockingLoader("Verifying Details...")
            verifyTempPassword(empId, tempPass)
        }

        btnVerifyOtp.setOnClickListener {
            val otp = etOtp.text.toString().trim()
            if (otp.length != 6) {
                showOtpError("❌ Enter valid 6-digit OTP")
                return@setOnClickListener
            }
            verifyOtp(otp)
        }

        btnSavePass.setOnClickListener {
            val newPass     = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            if (newPass.length < 6) {
                showPassError("❌ Minimum 6 characters required")
                return@setOnClickListener
            }
            if (newPass != confirmPass) {
                showPassError("❌ Passwords do not match")
                return@setOnClickListener
            }

            tvPassError.visibility = View.GONE
            showBlockingLoader("Saving Password...")
            saveNewPassword(newPass)
        }

        tvResendOtp.setOnClickListener {
            if (tvResendOtp.isEnabled) sendOtp()
        }

        tvBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // ─────────────────────────────────────────────────
    // ✅ Firestore — unchanged
    // ─────────────────────────────────────────────────
    private fun verifyTempPassword(empId: String, tempPass: String) {
        db.collection("faculties")
            .whereEqualTo("employeeId", empId)
            .whereEqualTo("status", "Active")
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                hideBlockingLoader()

                if (snap.isEmpty) {
                    showError("❌ Employee ID not found or inactive")
                    return@addOnSuccessListener
                }

                val doc            = snap.documents[0]
                val storedTempPass = doc.getString("tempPassword").orEmpty()
                val passwordStatus = doc.getString("passwordStatus").orEmpty()

                if (passwordStatus.lowercase() == "active") {
                    showError("✅ Account already registered. Please login.")
                    return@addOnSuccessListener
                }

                if (tempPass != storedTempPass) {
                    showError("❌ Incorrect temporary password")
                    return@addOnSuccessListener
                }

                facultyId          = doc.id
                facultyEmployeeId  = doc.getString("employeeId").orEmpty()
                facultyEmail       = doc.getString("email").orEmpty()
                facultyName        = doc.getString("fullName").orEmpty()
                facultyRole        = doc.getString("role").orEmpty()
                facultyCourseId    = doc.getString("courseId").orEmpty()
                facultyCourseName  = doc.getString("courseName").orEmpty()
                facultyCourseCode  = doc.getString("courseCode").orEmpty()
                facultyDesignation = doc.getString("designation").orEmpty()
                facultyPhone       = doc.getString("phone").orEmpty()
                facultyPhotoUrl    = doc.getString("photoUrl").orEmpty()

                sendOtp()
                showStep(2)
            }
            .addOnFailureListener {
                hideBlockingLoader()
                showError("❌ Connection error: ${it.localizedMessage}")
            }
    }

    private fun sendOtp() {
        generatedOtp = Random.nextInt(100000, 999999).toString()
        tvEmailInfo.text = "A verification code was sent to ${maskEmail(facultyEmail)}"
        startResendTimer()

        EmailSender.sendOtp(
            toEmail   = facultyEmail,
            toName    = facultyName,
            otp       = generatedOtp,
            onSuccess = {
                runOnUiThread {
                    showOtpSuccess("✅ OTP sent to your email!")
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    showOtpError("❌ Failed to send OTP: $error")
                }
            }
        )
    }

    private fun verifyOtp(enteredOtp: String) {
        if (enteredOtp == generatedOtp) {
            resendTimer?.cancel()
            showStep(3)
        } else {
            showOtpError("❌ Invalid verification code")
        }
    }

    private fun saveNewPassword(newPassword: String) {
        db.collection("faculties").document(facultyId)
            .update(mapOf("password" to newPassword, "passwordStatus" to "active"))
            .addOnSuccessListener {
                hideBlockingLoader()

                SessionManager(this).saveSession(
                    facultyId, facultyEmployeeId, facultyName, facultyEmail,
                    facultyPhone, facultyRole, facultyCourseId, facultyCourseName,
                    facultyCourseCode, facultyPhotoUrl, facultyDesignation, "active"
                )

                Toast.makeText(this, "✅ Registration Successful!", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            .addOnFailureListener {
                hideBlockingLoader()
                showPassError("❌ Could not save password. Try again.")
            }
    }

    // ─────────────────────────────────────────────────
    // ✅ Helpers
    // ─────────────────────────────────────────────────
    private fun showStep(step: Int) {
        layoutStep1.visibility = if (step == 1) View.VISIBLE else View.GONE
        layoutStep2.visibility = if (step == 2) View.VISIBLE else View.GONE
        layoutStep3.visibility = if (step == 3) View.VISIBLE else View.GONE
        tvError.visibility = View.GONE
    }

    private fun startResendTimer() {
        tvResendOtp.isEnabled = false
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(ms: Long) {
                tvResendOtp.text = "Resend in ${ms / 1000}s"
            }
            override fun onFinish() {
                tvResendOtp.text = "Resend OTP"
                tvResendOtp.isEnabled = true
            }
        }.start()
    }

    private fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 2) return email
        return email.substring(0, 2) + "****" + email.substring(at)
    }

    private fun showError(msg: String) {
        tvError.text = msg
        tvError.setTextColor(resources.getColor(R.color.red_400, null))
        tvError.visibility = View.VISIBLE
    }

    private fun showOtpError(msg: String) {
        tvOtpError.text = msg
        tvOtpError.setTextColor(resources.getColor(R.color.red_400, null))
        tvOtpError.visibility = View.VISIBLE
    }

    private fun showOtpSuccess(msg: String) {
        tvOtpError.text = msg
        tvOtpError.setTextColor(resources.getColor(R.color.green_primary, null))
        tvOtpError.visibility = View.VISIBLE
    }

    private fun showPassError(msg: String) {
        tvPassError.text = msg
        tvPassError.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        super.onDestroy()
    }
}
