package com.example.collegemanagementsystemfaculty

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import com.example.collegemanagementsystemfaculty.utils.CoreBaseActivity
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : CoreBaseActivity() {

    // ── Views ──────────────────────────────────────────
    private lateinit var tilEmployeeId: TextInputLayout
    private lateinit var etEmployeeId: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: View
    private lateinit var tvError: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    // ── Animation Views ────────────────────────────────
    private lateinit var logoGlow: View        // ✅ logo_outer_glow
    private lateinit var logoContainer: View   // ✅ logo_container
    private lateinit var loginCard: MaterialCardView

    // ── Firebase + Session ─────────────────────────────
    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    // ── Lock Logic ─────────────────────────────────────
    private var wrongAttempts = 0
    private var isLocked = false
    private var lockTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        session = SessionManager(this)

        bindViews()
        startEntranceAnimations()
        setupButtons()

        if (intent.getBooleanExtra("passwordReset", false)) {
            showSuccess("✅ Password reset successful! Please login.")
        }
    }

    // ─────────────────────────────────────────────────
    // ✅ Bind Views — updated to match new XML IDs
    // ─────────────────────────────────────────────────
    private fun bindViews() {
        tilEmployeeId    = findViewById(R.id.tilEmployeeId)
        etEmployeeId     = findViewById(R.id.etEmployeeId)
        tilPassword      = findViewById(R.id.tilPassword)
        etPassword       = findViewById(R.id.etPassword)
        btnLogin         = findViewById(R.id.btnLogin)
        tvError          = findViewById(R.id.tvError)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvRegister       = findViewById(R.id.txtContact)

        // ✅ Animation views — match new XML IDs
        logoGlow      = findViewById(R.id.logo_outer_glow)
        logoContainer = findViewById(R.id.logo_container)
        loginCard     = findViewById(R.id.loginCard)

        // ✅ Initial animation states
        logoGlow.alpha      = 0f
        logoGlow.scaleX     = 0.7f
        logoGlow.scaleY     = 0.7f

        logoContainer.alpha  = 0f
        logoContainer.scaleX = 0.7f
        logoContainer.scaleY = 0.7f

        loginCard.alpha        = 0f
        loginCard.translationY = 200f
    }

    // ─────────────────────────────────────────────────
    // ✅ Entrance Animation — same as Student App
    // ─────────────────────────────────────────────────
    private fun startEntranceAnimations() {

        // Logo glow: Scale + Fade in
        logoGlow.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(900)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Logo container: Scale + Fade in
        logoContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(900)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Card slides up + fades in
        loginCard.animate()
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

        btnLogin.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start()
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            false
        }

        btnLogin.setOnClickListener {
            if (isLocked) return@setOnClickListener

            val empId = etEmployeeId.text.toString().trim()
            val pass  = etPassword.text.toString().trim()

            if (empId.isEmpty()) {
                tilEmployeeId.error = "Enter Employee ID"
                return@setOnClickListener
            } else tilEmployeeId.error = null

            if (pass.isEmpty()) {
                tilPassword.error = "Enter Password"
                return@setOnClickListener
            } else tilPassword.error = null

            tvError.visibility = View.GONE
            showBlockingLoader("Authenticating...")
            loginUser(empId, pass)
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // ─────────────────────────────────────────────────
    // ✅ Firestore Login — unchanged
    // ─────────────────────────────────────────────────
    private fun loginUser(employeeId: String, password: String) {
        db.collection("faculties")
            .whereEqualTo("employeeId", employeeId)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                hideBlockingLoader()

                if (snap.isEmpty) {
                    handleWrongAttempt("❌ Employee ID not found")
                    return@addOnSuccessListener
                }

                val doc        = snap.documents[0]
                val status     = doc.getString("status") ?: ""
                val passStatus = doc.getString("passwordStatus") ?: ""
                val actualPass = doc.getString("password") ?: ""
                val tempPass   = doc.getString("tempPassword") ?: ""
                val role       = doc.getString("role") ?: ""

                if (status != "Active") {
                    showError("❌ Account inactive. Contact Admin.")
                    return@addOnSuccessListener
                }

                if (passStatus.lowercase() == "not_set") {
                    if (password == tempPass) {
                        showRegisterPrompt()
                    } else {
                        handleWrongAttempt("❌ Use temporary password to register")
                    }
                    return@addOnSuccessListener
                }

                if (password != actualPass) {
                    handleWrongAttempt("❌ Incorrect password")
                    return@addOnSuccessListener
                }

                // ✅ Success
                session.saveSession(
                    facultyId      = doc.id,
                    employeeId     = employeeId,
                    fullName       = doc.getString("fullName") ?: "",
                    email          = doc.getString("email") ?: "",
                    phone          = doc.getString("phone") ?: "",
                    role           = role,
                    courseId       = doc.getString("courseId") ?: "",
                    courseName     = doc.getString("courseName") ?: "",
                    courseCode     = doc.getString("courseCode") ?: "",
                    photoUrl       = doc.getString("photoUrl") ?: "",
                    designation    = doc.getString("designation") ?: "",
                    passwordStatus = passStatus
                )

                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            .addOnFailureListener {
                hideBlockingLoader()
                showError("❌ Error: ${it.localizedMessage}")
            }
    }

    // ─────────────────────────────────────────────────
    // ✅ Lock Logic — unchanged
    // ─────────────────────────────────────────────────
    private fun handleWrongAttempt(msg: String) {
        wrongAttempts++
        if (wrongAttempts >= 3) lockLogin()
        else showError("$msg (${3 - wrongAttempts} attempts left)")
    }

    private fun lockLogin() {
        isLocked = true
        btnLogin.isEnabled = false
        lockTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(ms: Long) {
                showError("🔒 Locked. Try again in ${ms / 1000}s")
            }
            override fun onFinish() {
                isLocked = false
                wrongAttempts = 0
                btnLogin.isEnabled = true
                tvError.visibility = View.GONE
            }
        }.start()
    }

    // ─────────────────────────────────────────────────
    // ✅ Messages — unchanged
    // ─────────────────────────────────────────────────
    private fun showError(msg: String) {
        tvError.text = msg
        tvError.setTextColor(resources.getColor(R.color.red_400, null))
        tvError.visibility = View.VISIBLE
    }

    private fun showSuccess(msg: String) {
        tvError.text = msg
        tvError.setTextColor(resources.getColor(R.color.green_primary, null))
        tvError.visibility = View.VISIBLE
    }

    private fun showRegisterPrompt() {
        showError("⚠️ Please register to set your permanent password.")
    }

    override fun onDestroy() {
        lockTimer?.cancel()
        super.onDestroy()
    }
}
