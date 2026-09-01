package com.example.collagemanagmentsystem

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.collagemanagmentsystem.utils.CoreBaseActivity
import com.example.collagemanagmentsystem.utils.EmailSender
import com.example.collagemanagmentsystem.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class RegisterActivity : CoreBaseActivity() {

    // ── Step 1 Views ───────────────────────────────────
    private lateinit var layoutStep1: LinearLayout
    private lateinit var tilGrNo: TextInputLayout
    private lateinit var etGrNo: TextInputEditText
    private lateinit var tilTempPassword: TextInputLayout
    private lateinit var etTempPassword: TextInputEditText
    private lateinit var btnVerifyTemp: MaterialButton

    // ── Step 2 Views ───────────────────────────────────
    private lateinit var layoutStep2: LinearLayout
    private lateinit var tvEmailInfo: TextView
    private lateinit var etOtp: TextInputEditText
    private lateinit var tvOtpError: TextView
    private lateinit var tvResendOtp: TextView
    private lateinit var btnVerifyOtp: MaterialButton

    // ── Step 3 Views ───────────────────────────────────
    private lateinit var layoutStep3: LinearLayout
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var tvPassError: TextView
    private lateinit var btnSavePass: MaterialButton

    // ── Animation Views ────────────────────────────────
    private lateinit var logoGlow: View
    private lateinit var logoContainer: View
    private lateinit var registerCard: View

    // ── Common Views ───────────────────────────────────
    private lateinit var tvError: TextView
    private lateinit var tvBackToLogin: TextView

    // ── Firebase ───────────────────────────────────────
    private val db = FirebaseFirestore.getInstance()

    // ── Student Data ───────────────────────────────────
    private var generatedOtp   = ""
    private var studentId      = ""
    private var studentGrNo    = ""
    private var studentEmail   = ""
    private var studentName    = ""
    private var studentPhone   = ""
    private var studentRollNo  = ""
    private var studentCourseId    = ""
    private var studentCourseName  = ""
    private var studentCourseCode  = ""
    private var studentDivisionId  = ""
    private var studentDivisionName = ""
    private var studentSemester    = ""
    private var studentYear        = ""
    private var studentGender      = ""
    private var studentDob         = ""
    private var studentBloodGroup  = ""
    private var studentAddress     = ""
    private var studentAdmissionYear = ""
    private var studentPhotoUrl    = ""
    private var studentStatus      = ""

    private var resendTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        bindViews()
        startEntranceAnimations()
        setupButtons()
        showStep(1)
    }

    // ─────────────────────────────────────────────────
    // ✅ Bind Views
    // ─────────────────────────────────────────────────
    private fun bindViews() {
        // Step 1
        layoutStep1    = findViewById(R.id.layoutStep1)
        tilGrNo        = findViewById(R.id.tilGrNo)
        etGrNo         = findViewById(R.id.etGrNo)
        tilTempPassword = findViewById(R.id.tilTempPassword)
        etTempPassword = findViewById(R.id.etTempPassword)
        btnVerifyTemp  = findViewById(R.id.btnVerifyTemp)

        // Step 2
        layoutStep2    = findViewById(R.id.layoutStep2)
        tvEmailInfo    = findViewById(R.id.tvEmailInfo)
        etOtp          = findViewById(R.id.etOtp)
        tvOtpError     = findViewById(R.id.tvOtpError)
        tvResendOtp    = findViewById(R.id.tvResendOtp)
        btnVerifyOtp   = findViewById(R.id.btnVerifyOtp)

        // Step 3
        layoutStep3       = findViewById(R.id.layoutStep3)
        etNewPassword     = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        tvPassError       = findViewById(R.id.tvPassError)
        btnSavePass       = findViewById(R.id.btnSavePass)

        // Common
        tvError       = findViewById(R.id.tvError)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        // Animation
        logoGlow      = findViewById(R.id.logo_outer_glow)
        logoContainer = findViewById(R.id.logo_container)
        registerCard  = findViewById(R.id.registerCard)

        // Initial animation states
        logoGlow.alpha      = 0f
        logoGlow.scaleX     = 0.7f
        logoGlow.scaleY     = 0.7f
        logoContainer.alpha  = 0f
        logoContainer.scaleX = 0.7f
        logoContainer.scaleY = 0.7f
        registerCard.alpha        = 0f
        registerCard.translationY = 200f
    }

    // ─────────────────────────────────────────────────
    // ✅ Entrance Animation — same as Login
    // ─────────────────────────────────────────────────
    private fun startEntranceAnimations() {
        logoGlow.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(900)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        logoContainer.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(900)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        registerCard.animate()
            .translationY(0f).alpha(1f)
            .setDuration(700)
            .setStartDelay(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    // ─────────────────────────────────────────────────
    // ✅ Buttons Setup
    // ─────────────────────────────────────────────────
    private fun setupButtons() {

        // Step 1: Verify GR + TempPass
        btnVerifyTemp.setOnClickListener {
            val grNo     = etGrNo.text.toString().trim()
            val tempPass = etTempPassword.text.toString().trim()

            if (grNo.isEmpty()) {
                tilGrNo.error = "Enter GR Number"
                return@setOnClickListener
            } else tilGrNo.error = null

            if (tempPass.isEmpty()) {
                tilTempPassword.error = "Enter temporary password"
                return@setOnClickListener
            } else tilTempPassword.error = null

            tvError.visibility = View.GONE
            showBlockingLoader("Verifying Details...")
            verifyTempPassword(grNo, tempPass)
        }

        // Step 2: Verify OTP
        btnVerifyOtp.setOnClickListener {
            val otp = etOtp.text.toString().trim()
            if (otp.length != 6) {
                showOtpError("❌ Enter valid 6-digit OTP")
                return@setOnClickListener
            }
            verifyOtp(otp)
        }

        // Step 3: Save Password
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
    // ✅ Step 1: Verify GR + Temp Password
    // ─────────────────────────────────────────────────
    private fun verifyTempPassword(grNo: String, tempPass: String) {
        db.collection("students")
            .whereEqualTo("grNo", grNo)
            .whereEqualTo("status", "Active")
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                hideBlockingLoader()

                if (snap.isEmpty) {
                    showError("❌ GR Number not found or account inactive")
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

                // ✅ Collect all student data
                studentId           = doc.id
                studentGrNo         = grNo
                studentEmail        = doc.getString("email").orEmpty()
                studentName         = doc.getString("fullName").orEmpty()
                studentPhone        = doc.getString("phone").orEmpty()
                studentRollNo       = doc.getString("rollNo").orEmpty()
                studentCourseId     = doc.getString("courseId").orEmpty()
                studentCourseName   = doc.getString("courseName").orEmpty()
                studentCourseCode   = doc.getString("courseCode").orEmpty()
                studentDivisionId   = doc.getString("divisionId").orEmpty()
                studentDivisionName = doc.getString("divisionName").orEmpty()
                studentSemester     = doc.getString("semester").orEmpty()
                studentYear         = doc.getString("year").orEmpty()
                studentGender       = doc.getString("gender").orEmpty()
                studentDob          = doc.getString("dob").orEmpty()
                studentBloodGroup   = doc.getString("bloodGroup").orEmpty()
                studentAddress      = doc.getString("address").orEmpty()
                studentAdmissionYear = doc.getString("admissionYear").orEmpty()
                studentPhotoUrl     = doc.getString("photoUrl").orEmpty()
                studentStatus       = doc.getString("status").orEmpty()

                sendOtp()
                showStep(2)
            }
            .addOnFailureListener {
                hideBlockingLoader()
                showError("❌ Connection error: ${it.localizedMessage}")
            }
    }

    // ─────────────────────────────────────────────────
    // ✅ Step 2: Send OTP
    // ─────────────────────────────────────────────────
    private fun sendOtp() {
        generatedOtp = Random.nextInt(100000, 999999).toString()
        tvEmailInfo.text = "A verification code was sent to ${maskEmail(studentEmail)}"
        startResendTimer()

        EmailSender.sendOtp(
            toEmail   = studentEmail,
            toName    = studentName,
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

    // ─────────────────────────────────────────────────
    // ✅ Step 2: Verify OTP
    // ─────────────────────────────────────────────────
    private fun verifyOtp(enteredOtp: String) {
        if (enteredOtp == generatedOtp) {
            resendTimer?.cancel()
            showStep(3)
        } else {
            showOtpError("❌ Invalid verification code")
        }
    }

    // ─────────────────────────────────────────────────
    // ✅ Step 3: Save Password to Firestore
    // ─────────────────────────────────────────────────
    private fun saveNewPassword(newPassword: String) {
        db.collection("students").document(studentId)
            .update(
                mapOf(
                    "password"       to newPassword,
                    "passwordStatus" to "active"
                )
            )
            .addOnSuccessListener {
                hideBlockingLoader()

                // ✅ Save full session
                SessionManager(this).saveSession(
                    studentId      = studentId,
                    fullName       = studentName,
                    email          = studentEmail,
                    phone          = studentPhone,
                    grNo           = studentGrNo,
                    rollNo         = studentRollNo,
                    courseId       = studentCourseId,
                    courseName     = studentCourseName,
                    courseCode     = studentCourseCode,
                    divisionId     = studentDivisionId,
                    divisionName   = studentDivisionName,
                    semester       = studentSemester,
                    year           = studentYear,
                    gender         = studentGender,
                    dob            = studentDob,
                    bloodGroup     = studentBloodGroup,
                    address        = studentAddress,
                    admissionYear  = studentAdmissionYear,
                    photoUrl       = studentPhotoUrl,
                    status         = studentStatus,
                    passwordStatus = "active"
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
