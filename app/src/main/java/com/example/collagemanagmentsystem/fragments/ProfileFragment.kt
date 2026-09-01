package com.example.collagemanagmentsystem.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.collagemanagmentsystem.ChangePasswordActivity
import com.example.collagemanagmentsystem.LoginActivity
import com.example.collagemanagmentsystem.R
import com.example.collagemanagmentsystem.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    // ── Header Views ───────────────────────────────────
    private lateinit var imgProfile   : CircleImageView
    private lateinit var tvFullName   : TextView
    private lateinit var tvDesignation: TextView  // "Active Student"

    // ── Stat Grid Views ────────────────────────────────
    private lateinit var tvSemesterShort : TextView
    private lateinit var tvRollNoShort   : TextView
    private lateinit var tvGrNoShort     : TextView

    // ── Academic Detail Views ──────────────────────────
    private lateinit var tvDepartment : TextView
    private lateinit var tvDivision   : TextView

    // ── Contact Views ──────────────────────────────────
    private lateinit var tvEmail : TextView
    private lateinit var tvPhone : TextView

    // ── Personal Detail Views ──────────────────────────
    private lateinit var tvGender      : TextView
    private lateinit var tvDob         : TextView
    private lateinit var tvBloodGroup  : TextView
    private lateinit var tvAddress     : TextView
    private lateinit var tvJoiningDate : TextView

    // ── Action Buttons ─────────────────────────────────
    private lateinit var btnChangePassword : View
    private lateinit var btnLogout         : View

    private var profileLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        bindViews(view)
        setupClickListeners()

        // ✅ Load only once
        if (!profileLoaded) {
            loadStudentProfile()
            profileLoaded = true
        }
    }

    // ─────────────────────────────────────────────────
    // ✅ Bind Views — student XML IDs
    // ─────────────────────────────────────────────────
    private fun bindViews(view: View) {
        imgProfile    = view.findViewById(R.id.imgProfile)
        tvFullName    = view.findViewById(R.id.tvFullName)
        tvDesignation = view.findViewById(R.id.tvDesignation)

        // Stat Grid
        tvSemesterShort = view.findViewById(R.id.tvSemesterShort)
        tvRollNoShort   = view.findViewById(R.id.tvRollNoShort)
        tvGrNoShort     = view.findViewById(R.id.tvGrNoShort)

        // Academic
        tvDepartment = view.findViewById(R.id.tvDepartment)
        tvDivision   = view.findViewById(R.id.tvDivision)

        // Contact
        tvEmail = view.findViewById(R.id.tvEmail)
        tvPhone = view.findViewById(R.id.tvPhone)

        // Personal
        tvGender      = view.findViewById(R.id.tvGender)
        tvDob         = view.findViewById(R.id.tvDob)
        tvBloodGroup  = view.findViewById(R.id.tvbloodgroup)
        tvAddress     = view.findViewById(R.id.tvaddress)
        tvJoiningDate = view.findViewById(R.id.tvJoiningDate)

        // Buttons
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnLogout         = view.findViewById(R.id.btnLogout)
    }

    // ─────────────────────────────────────────────────
    // ✅ Load Student Profile — Cache first, then Server
    // ─────────────────────────────────────────────────
    private fun loadStudentProfile() {
        val studentId = session.getStudentId()
        if (studentId.isEmpty()) return

        val docRef = db.collection("students").document(studentId)

        // 🔥 Step 1 — Try Cache first (instant)
        docRef.get(Source.CACHE)
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    bindProfileData(doc)
                } else {
                    // 🔥 Step 2 — Cache miss → load from Server
                    fetchFromServer(docRef)
                }
            }
            .addOnFailureListener {
                // Cache not available → load from Server
                fetchFromServer(docRef)
            }
    }

    private fun fetchFromServer(
        docRef: com.google.firebase.firestore.DocumentReference
    ) {
        docRef.get(Source.SERVER)
            .addOnSuccessListener { doc ->
                if (doc.exists()) bindProfileData(doc)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to load profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ─────────────────────────────────────────────────
    // ✅ Bind Data to Views
    // ─────────────────────────────────────────────────
    private fun bindProfileData(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ) {
        val fullName      = doc.getString("fullName").orEmpty()
        val status        = doc.getString("status").orEmpty()
        val semester      = doc.getString("semester").orEmpty()
        val rollNo        = doc.getString("rollNo").orEmpty()
        val grNo          = doc.getString("grNo").orEmpty()
        val courseName    = doc.getString("courseName").orEmpty()
        val courseCode    = doc.getString("courseCode").orEmpty()
        val divisionName  = doc.getString("divisionName").orEmpty()
        val email         = doc.getString("email").orEmpty()
        val phone         = doc.getString("phone").orEmpty()
        val gender        = doc.getString("gender").orEmpty()
        val dob           = doc.getString("dob").orEmpty()
        val bloodGroup    = doc.getString("bloodGroup").orEmpty()
        val address       = doc.getString("address").orEmpty()
        val admissionYear = doc.getString("admissionYear").orEmpty()
        val photoUrl      = doc.getString("photoUrl").orEmpty()

        // ✅ Header
        tvFullName.text    = fullName.uppercase()
        tvDesignation.text = "$status Student"

        // ✅ Stat Grid
        tvSemesterShort.text = semester.ifEmpty { "-" }
        tvRollNoShort.text   = rollNo.ifEmpty { "-" }
        tvGrNoShort.text     = if (grNo.length > 5) grNo.takeLast(5) else grNo

        // ✅ Academic
        tvDepartment.text = "$courseName ($courseCode)"
        tvDivision.text   = divisionName.ifEmpty { "-" }

        // ✅ Contact
        tvEmail.text = email.ifEmpty { "-" }
        tvPhone.text = phone.ifEmpty { "-" }

        // ✅ Personal
        tvGender.text      = gender.ifEmpty { "-" }
        tvDob.text         = dob.ifEmpty { "-" }
        tvBloodGroup.text  = bloodGroup.ifEmpty { "-" }
        tvAddress.text     = address.ifEmpty { "-" }
        tvJoiningDate.text = admissionYear.ifEmpty { "-" }

        // ✅ Load photo with Glide
        if (photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(imgProfile)
        }
    }

    // ─────────────────────────────────────────────────
    // ✅ Click Listeners
    // ─────────────────────────────────────────────────
    private fun setupClickListeners() {

        btnChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }

        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                session.logout()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Stay", null)
            .show()
    }
}
