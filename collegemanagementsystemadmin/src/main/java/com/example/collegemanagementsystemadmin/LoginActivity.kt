package com.example.collegemanagementsystemadmin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.util.Patterns
import androidx.appcompat.app.AlertDialog
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*

class LoginActivity : CoreBaseActivity() {

    // UI views
    private lateinit var loginCard: View
    private lateinit var btnLogin: View
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText

    // Firebase
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    // custom progress dialog views
    private var loginDialog: AlertDialog? = null
    private var dialogTitle: TextView? = null
    private var dialogMessage: TextView? = null
    private var dialogProgress: ProgressBar? = null
    private var dialogJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        playEnterAnimation()

        btnLogin.setOnClickListener {
            startLoginFlow()
        }
    }

    private fun initViews() {
        loginCard = findViewById(R.id.loginCard)
        btnLogin = findViewById(R.id.btnLogin)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
    }

    private fun playEnterAnimation() {
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        loginCard.translationY = screenHeight * 0.30f
        loginCard.alpha = 0f
        loginCard.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(800L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // ===== LOGIN FLOW =====

    private fun startLoginFlow() {
        val pair = validateInputs() ?: return
        val email = pair.first
        val password = pair.second

        showLoginDialog()

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    onLoginFailedFriendly("Something went wrong while signing you in. Please try again.")
                    return@addOnSuccessListener
                }
                updateDialogMessage("Checking admin access...")
                checkIsAdmin(uid)
            }
            .addOnFailureListener { ex ->
                val msg = when (ex) {
                    is FirebaseAuthInvalidCredentialsException ->
                        "The email or password you entered is incorrect."
                    is FirebaseAuthInvalidUserException ->
                        "No account found with this email. Please check or create an account."
                    is FirebaseAuthUserCollisionException ->
                        "This email is already linked with another account."
                    else ->
                        "Unable to sign in right now. Please check your details and try again."
                }
                onLoginFailedFriendly(msg)
            }
    }

    private fun checkIsAdmin(uid: String) {
        db.collection("admin")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onLoginFailedFriendly("This account does not have admin access.")
                    return@addOnSuccessListener
                }

                val role = doc.getString("role")
                if (role == "admin") {
                    onLoginSuccess()
                } else {
                    onLoginFailedFriendly("This account does not have admin access.")
                }
            }
            .addOnFailureListener { ex ->
                val msg = "We could not verify your admin access. Please check your internet connection and try again."
                onLoginFailedFriendly(msg)
            }
    }

    private fun validateInputs(): Pair<String, String>? {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
            return null
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(
                this,
                "Please enter a valid email like abc@gmail.com",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
            return null
        }

        if (password.length < 6) {
            Toast.makeText(
                this,
                "Password must be at least 6 characters",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        return Pair(email, password)
    }

    // ===== custom progress dialog using dialog_success_profile.xml =====

    private fun showLoginDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_success_profile, null)

        dialogTitle = view.findViewById(R.id.txtSuccessTitle)
        dialogMessage = view.findViewById(R.id.txtSuccessMessage)
        dialogProgress = view.findViewById(R.id.progressSuccess)

        loginDialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        loginDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loginDialog?.show()

        dialogJob?.cancel()
        dialogJob = CoroutineScope(Dispatchers.Main).launch {
            dialogTitle?.text = "Admin login..."
            dialogMessage?.text = "Checking your details..."
            delay(2000)
            dialogMessage?.text = "Signing you in..."
            delay(2000)
            dialogMessage?.text = "Almost there..."
        }
    }

    private fun updateDialogMessage(text: String) {
        dialogJob?.cancel()
        dialogMessage?.text = text
    }

    private fun closeLoginDialog() {
        dialogJob?.cancel()
        dialogProgress?.visibility = View.GONE
        loginDialog?.dismiss()
    }

    // ===== success / error behavior =====

    private fun onLoginSuccess() {
        dialogJob?.cancel()
        dialogTitle?.text = "Login successful"
        dialogMessage?.text = "Redirecting to admin dashboard..."
        dialogProgress?.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            closeLoginDialog()
            startActivity(Intent(this@LoginActivity, AdminDashboardActivity::class.java))
            finish()
        }
    }

    private fun onLoginFailedFriendly(message: String) {
        dialogJob?.cancel()
        dialogTitle?.text = "Login failed"
        dialogMessage?.text = message
        dialogProgress?.visibility = View.GONE

        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            closeLoginDialog()
        }
    }
}
