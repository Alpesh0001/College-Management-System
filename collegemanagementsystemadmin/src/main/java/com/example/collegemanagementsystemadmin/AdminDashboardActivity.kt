package com.example.collegemanagementsystemadmin

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : CoreBaseActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Header Views
    private lateinit var tvWelcome: TextView
    private lateinit var tvAdminRole: TextView
    private lateinit var imgAdminProfile: ImageView
    private lateinit var btnNotifications: ImageView
    private lateinit var btnProfile: ImageView

    // Count TextViews
    private lateinit var tvStudentCount: TextView
    private lateinit var tvCourseCount: TextView
    private lateinit var tvSubjectCount: TextView
    private lateinit var tvFacultyCount: TextView
    private lateinit var tvDivisionCount: TextView
    private lateinit var tvLibraryCount: TextView

    // Cards
    private lateinit var cardStudents: MaterialCardView
    private lateinit var cardCourses: MaterialCardView
    private lateinit var cardSubjects: MaterialCardView
    private lateinit var cardFaculty: MaterialCardView
    private lateinit var cardDivision: MaterialCardView
    private lateinit var cardLibrary: MaterialCardView
    private lateinit var cardFees: MaterialCardView
    private lateinit var cardReports: MaterialCardView
    private lateinit var cardSettings: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

        initViews()
        setupHeaderIcons()
        loadAdminData()
        loadDashboardCounts()
        setupCardClicks()
        setupBackPress()
    }

    override fun onResume() {
        super.onResume()
        // Refresh counts when returning to dashboard
        loadDashboardCounts()
    }

    private fun initViews() {
        // Header views
        tvWelcome = findViewById(R.id.tvWelcome)
        tvAdminRole = findViewById(R.id.tvAdminRole)
        imgAdminProfile = findViewById(R.id.imgAdminProfile)
        btnNotifications = findViewById(R.id.btnNotifications)
        btnProfile = findViewById(R.id.btnProfile)

        // Count views
        tvStudentCount = findViewById(R.id.tvStudentCount)
        tvCourseCount = findViewById(R.id.tvCourseCount)
        tvSubjectCount = findViewById(R.id.tvSubjectCount)
        tvFacultyCount = findViewById(R.id.tvFacultyCount)
        tvDivisionCount = findViewById(R.id.tvDivisionCount)
        tvLibraryCount = findViewById(R.id.tvLibraryCount)

        // Cards
        cardStudents = findViewById(R.id.cardStudents)
        cardCourses = findViewById(R.id.cardCourses)
        cardSubjects = findViewById(R.id.cardSubjects)
        cardFaculty = findViewById(R.id.cardFaculty)
        cardDivision = findViewById(R.id.cardDivision)
        cardLibrary = findViewById(R.id.cardLibrary)
        cardFees = findViewById(R.id.cardFees)
        cardReports = findViewById(R.id.cardReports)
        cardSettings = findViewById(R.id.cardSettings)
    }

    private fun setupHeaderIcons() {
        btnNotifications.setOnClickListener {
            Toast.makeText(this, "Notifications - Coming Soon", Toast.LENGTH_SHORT).show()
            // TODO: Open notifications activity
        }

        btnProfile.setOnClickListener {
            Toast.makeText(this, "Profile - Coming Soon", Toast.LENGTH_SHORT).show()
            // TODO: Open profile activity
        }
    }

    private fun loadAdminData() {
        val currentUser = auth.currentUser ?: return

        // Fetch admin data from Firestore
        db.collection("admin")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val adminName = document.getString("name") ?: "Admin"
                    tvWelcome.text = "Welcome, $adminName!"
                    tvAdminRole.text = "System Administrator"
                } else {
                    tvWelcome.text = "Welcome Back!"
                    tvAdminRole.text = "Administrator"
                }
            }
            .addOnFailureListener {
                tvWelcome.text = "Welcome Back!"
                tvAdminRole.text = "Administrator"
            }
    }

    private fun loadDashboardCounts() {
        // Load Students count
        db.collection("students")
            .get()
            .addOnSuccessListener { snapshot ->
                tvStudentCount.text = snapshot.size().toString()
            }
            .addOnFailureListener {
                tvStudentCount.text = "0"
            }

        // Load Courses count
        db.collection("courses")
            .get()
            .addOnSuccessListener { snapshot ->
                tvCourseCount.text = snapshot.size().toString()
            }
            .addOnFailureListener {
                tvCourseCount.text = "0"
            }

        // Load Subjects count (NEW!)
        db.collection("subjects")
            .get()
            .addOnSuccessListener { snapshot ->
                tvSubjectCount.text = snapshot.size().toString()
            }
            .addOnFailureListener {
                tvSubjectCount.text = "0"
            }

        // Load Faculty count
        db.collection("faculties")
            .get()
            .addOnSuccessListener { snapshot ->
                tvFacultyCount.text = snapshot.size().toString()
            }
            .addOnFailureListener {
                tvFacultyCount.text = "0"
            }

        // Load Divisions count
        db.collection("divisions")
            .get()
            .addOnSuccessListener { snapshot ->
                tvDivisionCount.text = snapshot.size().toString()
            }
            .addOnFailureListener {
                tvDivisionCount.text = "0"
            }

        // Load Library books count
        db.collection("library")
            .get()
            .addOnSuccessListener { snapshot ->
                tvLibraryCount.text = snapshot.size().toString()
            }
            .addOnFailureListener {
                tvLibraryCount.text = "0"
            }

    }

    private fun setupCardClicks() {
        cardStudents.setOnClickListener {
            startActivity(Intent(this, AdminStudentsActivity::class.java))
        }

        cardCourses.setOnClickListener {
            startActivity(Intent(this, AdminCourseActivity::class.java))
        }

        cardSubjects.setOnClickListener {
            startActivity(Intent(this, AdminSubjectsActivity::class.java))
        }

        cardFaculty.setOnClickListener {
            startActivity(Intent(this, AdminFacultyActivity::class.java))
        }

        cardDivision.setOnClickListener {
            startActivity(Intent(this, AdminDivisionActivity::class.java))
        }

        cardLibrary.setOnClickListener {
            Toast.makeText(this, "Manage Library - Coming Soon", Toast.LENGTH_SHORT).show()
            // TODO: Open library activity
        }

        cardFees.setOnClickListener {
            startActivity(Intent(this, FeesManagementActivity::class.java))
        }

        cardReports.setOnClickListener {
            Toast.makeText(this, "View Reports - Coming Soon", Toast.LENGTH_SHORT).show()
            // TODO: Open reports activity
        }

        cardSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Do you want to exit the app?")
            .setPositiveButton("Yes") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog() {
        val options = arrayOf("Profile", "Change Password", "Logout")

        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        Toast.makeText(this, "Profile - Coming Soon", Toast.LENGTH_SHORT).show()
                        // TODO: Open profile activity
                    }
                    1 -> {
                        Toast.makeText(this, "Change Password - Coming Soon", Toast.LENGTH_SHORT).show()
                        // TODO: Open change password activity
                    }
                    2 -> performLogout()
                }
            }
            .show()
    }

    private fun performLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                redirectToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
