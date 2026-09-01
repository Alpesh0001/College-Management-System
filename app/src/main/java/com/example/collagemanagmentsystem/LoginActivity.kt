package com.example.collagemanagmentsystem

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.collagemanagmentsystem.utils.CoreBaseActivity
import com.example.collagemanagmentsystem.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : CoreBaseActivity() {

    // ── Views ──────────────────────────────────────────────
    private lateinit var tilGrNo: TextInputLayout
    private lateinit var etGrNo: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvError: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    // ── Animation Views (keep as-is) ──────────────────────
    private lateinit var logoGlow: View
    private lateinit var logoContainer: View
    private lateinit var appName: TextView
    private lateinit var tagline: TextView
    private lateinit var loginCard: MaterialCardView
    private lateinit var versionText: TextView

    // ── Firebase + Session ────────────────────────────────
    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    // ── Lock Logic ────────────────────────────────────────
    private var wrongAttempts = 0
    private var isLocked = false
    private var lockTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        session = SessionManager(this)

        bindViews()
        setupButtonAnimations()
        prepareEntryAnimations()
        startEntryAnimations()
        setupLoginButton()

        // ✅ Show success message if coming back from SetPasswordActivity
        if (intent.getBooleanExtra("passwordReset", false)) {
            showSuccess("✅ Password set successfully! Please login.")
        }
    }

    // ─────────────────────────────────────────────────────
    // ✅ Bind Views
    // ─────────────────────────────────────────────────────
    private fun bindViews() {
        tilGrNo          = findViewById(R.id.tilGrNo)
        etGrNo           = findViewById(R.id.etGrNo)
        tilPassword      = findViewById(R.id.tilPassword)
        etPassword       = findViewById(R.id.etPassword)
        btnLogin         = findViewById(R.id.btn_login)
        tvError          = findViewById(R.id.tv_error)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        tvRegister       = findViewById(R.id.tv_register)

        // Animation views
        logoGlow      = findViewById(R.id.logo_outer_glow)
        logoContainer = findViewById(R.id.logo_container)
        appName       = findViewById(R.id.tv_app_name)
        tagline       = findViewById(R.id.tv_tagline)
        loginCard     = findViewById(R.id.login_card)
        versionText   = findViewById(R.id.tv_version)
    }

    // ─────────────────────────────────────────────────────
    // ✅ Button press animation (your original code — untouched)
    // ─────────────────────────────────────────────────────
    private fun setupButtonAnimations() {
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

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            finish()
        }
    }

    // ─────────────────────────────────────────────────────
    // ✅ Login Button — actual login logic
    // ─────────────────────────────────────────────────────
    private fun setupLoginButton() {
        btnLogin.setOnClickListener {
            if (isLocked) return@setOnClickListener

            val grNo = etGrNo.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            // Validate inputs
            if (grNo.isEmpty()) {
                tilGrNo.error = "Enter GR Number"
                return@setOnClickListener
            } else tilGrNo.error = null

            if (pass.isEmpty()) {
                tilPassword.error = "Enter Password"
                return@setOnClickListener
            } else tilPassword.error = null

            tvError.visibility = View.GONE
            showBlockingLoader("Authenticating...")
            loginStudent(grNo, pass)
        }
    }

    // ─────────────────────────────────────────────────────
    // ✅ Firestore Login — students collection
    // ─────────────────────────────────────────────────────
    private fun loginStudent(grNo: String, password: String) {
        db.collection("students")
            .whereEqualTo("grNo", grNo)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                hideBlockingLoader()

                if (snap.isEmpty) {
                    handleWrongAttempt("❌ GR Number not found")
                    return@addOnSuccessListener
                }

                val doc        = snap.documents[0]
                val status     = doc.getString("status") ?: ""
                val passStatus = doc.getString("passwordStatus") ?: ""
                val tempPass   = doc.getString("tempPassword") ?: ""
                val actualPass = doc.getString("password") ?: ""

                // ✅ Check if account is active
                if (status != "Active") {
                    showError("❌ Account inactive. Contact Admin.")
                    return@addOnSuccessListener
                }

                // ✅ Password not set yet → use tempPassword to go to SetPassword screen
                if (passStatus.lowercase() == "not_set") {
                    if (password == tempPass) {
                        // Save minimal session so SetPasswordActivity knows who it is
                        session.saveSession(
                            studentId     = doc.id,
                            fullName      = doc.getString("fullName") ?: "",
                            email         = doc.getString("email") ?: "",
                            phone         = doc.getString("phone") ?: "",
                            grNo          = grNo,
                            rollNo        = doc.getString("rollNo") ?: "",
                            courseId      = doc.getString("courseId") ?: "",
                            courseName    = doc.getString("courseName") ?: "",
                            courseCode    = doc.getString("courseCode") ?: "",
                            divisionId    = doc.getString("divisionId") ?: "",
                            divisionName  = doc.getString("divisionName") ?: "",
                            semester      = doc.getString("semester") ?: "",
                            year          = doc.getString("year") ?: "",
                            gender        = doc.getString("gender") ?: "",
                            dob           = doc.getString("dob") ?: "",
                            bloodGroup    = doc.getString("bloodGroup") ?: "",
                            address       = doc.getString("address") ?: "",
                            admissionYear = doc.getString("admissionYear") ?: "",
                            photoUrl      = doc.getString("photoUrl") ?: "",
                            status        = status,
                            passwordStatus = passStatus
                        )
                        // ✅ Go to SetPasswordActivity
                        startActivity(Intent(this, RegisterActivity::class.java))
                        finish()
                    } else {
                        handleWrongAttempt("❌ Use your temporary password first")
                    }
                    return@addOnSuccessListener
                }

                // ✅ Normal login — check actual password
                if (password != actualPass) {
                    handleWrongAttempt("❌ Incorrect password")
                    return@addOnSuccessListener
                }

                // ✅ SUCCESS — save full session
                session.saveSession(
                    studentId     = doc.id,
                    fullName      = doc.getString("fullName") ?: "",
                    email         = doc.getString("email") ?: "",
                    phone         = doc.getString("phone") ?: "",
                    grNo          = grNo,
                    rollNo        = doc.getString("rollNo") ?: "",
                    courseId      = doc.getString("courseId") ?: "",
                    courseName    = doc.getString("courseName") ?: "",
                    courseCode    = doc.getString("courseCode") ?: "",
                    divisionId    = doc.getString("divisionId") ?: "",
                    divisionName  = doc.getString("divisionName") ?: "",
                    semester      = doc.getString("semester") ?: "",
                    year          = doc.getString("year") ?: "",
                    gender        = doc.getString("gender") ?: "",
                    dob           = doc.getString("dob") ?: "",
                    bloodGroup    = doc.getString("bloodGroup") ?: "",
                    address       = doc.getString("address") ?: "",
                    admissionYear = doc.getString("admissionYear") ?: "",
                    photoUrl      = doc.getString("photoUrl") ?: "",
                    status        = status,
                    passwordStatus = passStatus
                )

                // ✅ Navigate to Dashboard
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            .addOnFailureListener {
                hideBlockingLoader()
                showError("❌ Error: ${it.localizedMessage}")
            }
    }

    // ─────────────────────────────────────────────────────
    // ✅ Wrong attempt handler with lock
    // ─────────────────────────────────────────────────────
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
                showError("🔒 Too many attempts. Try again in ${ms / 1000}s")
            }
            override fun onFinish() {
                isLocked = false
                wrongAttempts = 0
                btnLogin.isEnabled = true
                tvError.visibility = View.GONE
            }
        }.start()
    }

    // ─────────────────────────────────────────────────────
    // ✅ Error / Success messages
    // ─────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────
    // ✅ Entry Animations (your original code — untouched)
    // ─────────────────────────────────────────────────────
    private fun prepareEntryAnimations() {
        logoGlow.alpha    = 0f
        logoGlow.scaleX   = 0.7f
        logoGlow.scaleY   = 0.7f

        logoContainer.alpha  = 0f
        logoContainer.scaleX = 0.7f
        logoContainer.scaleY = 0.7f

        appName.alpha        = 0f
        appName.translationY = 30f

        tagline.alpha = 0f

        loginCard.alpha        = 0f
        loginCard.translationY = 200f

        versionText.alpha = 0f
    }

    private fun startEntryAnimations() {
        val logoAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logoGlow, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(logoGlow, View.SCALE_X, 1f),
                ObjectAnimator.ofFloat(logoGlow, View.SCALE_Y, 1f),
                ObjectAnimator.ofFloat(logoContainer, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(logoContainer, View.SCALE_X, 1f),
                ObjectAnimator.ofFloat(logoContainer, View.SCALE_Y, 1f)
            )
            duration = 800
            interpolator = OvershootInterpolator()
        }

        val titleAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(appName, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(appName, View.TRANSLATION_Y, 0f)
            )
            duration = 600
            startDelay = 200
        }

        val taglineAnim = ObjectAnimator.ofFloat(tagline, View.ALPHA, 1f).apply {
            duration = 600
            startDelay = 400
        }

        val cardAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(loginCard, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(loginCard, View.TRANSLATION_Y, 0f)
            )
            duration = 700
            startDelay = 300
            interpolator = DecelerateInterpolator()
        }

        val footerAnim = ObjectAnimator.ofFloat(versionText, View.ALPHA, 0.6f).apply {
            duration = 800
            startDelay = 1000
        }

        AnimatorSet().apply {
            playTogether(logoAnim, titleAnim, taglineAnim, cardAnim, footerAnim)
            start()
        }
    }

    override fun onDestroy() {
        lockTimer?.cancel()
        super.onDestroy()
    }
}
