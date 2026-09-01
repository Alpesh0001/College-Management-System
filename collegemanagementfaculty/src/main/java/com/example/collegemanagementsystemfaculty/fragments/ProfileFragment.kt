package com.example.collegemanagementsystemfaculty.fragments

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
import com.example.collegemanagementsystemfaculty.ChangePasswordActivity
import com.example.collegemanagementsystemfaculty.LoginActivity
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    private lateinit var imgProfile      : CircleImageView
    private lateinit var tvFullName      : TextView
    private lateinit var tvDesignation   : TextView
    private lateinit var tvExperience    : TextView
    private lateinit var tvRoleLabel     : TextView
    private lateinit var tvEmployeeIdShort: TextView
    private lateinit var tvDepartment    : TextView
    private lateinit var tvQualification : TextView
    private lateinit var tvEmail         : TextView
    private lateinit var tvPhone         : TextView
    private lateinit var tvAddress       : TextView
    private lateinit var tvGender        : TextView
    private var profileLoaded = false
    private lateinit var tvDob           : TextView
    private lateinit var tvJoiningDate   : TextView
    private lateinit var btnChangePassword: View
    private lateinit var btnLogout       : View

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
        loadFacultyProfile()
        if (!profileLoaded) {
            loadFacultyProfile()
            profileLoaded = true
        }
    }

    private fun bindViews(view: View) {
        imgProfile        = view.findViewById(R.id.imgProfile)
        tvFullName        = view.findViewById(R.id.tvFullName)
        tvDesignation     = view.findViewById(R.id.tvDesignation)
        tvExperience      = view.findViewById(R.id.tvExperience)
        tvRoleLabel       = view.findViewById(R.id.tvRoleLabel)
        tvEmployeeIdShort = view.findViewById(R.id.tvEmployeeIdShort)
        tvDepartment      = view.findViewById(R.id.tvDepartment)
        tvQualification   = view.findViewById(R.id.tvQualification)
        tvEmail           = view.findViewById(R.id.tvEmail)
        tvPhone           = view.findViewById(R.id.tvPhone)
        tvAddress         = view.findViewById(R.id.tvAddress)
        tvGender          = view.findViewById(R.id.tvGender)
        tvDob             = view.findViewById(R.id.tvDob)
        tvJoiningDate     = view.findViewById(R.id.tvJoiningDate)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnLogout         = view.findViewById(R.id.btnLogout)
    }

    private fun loadFacultyProfile() {

        val facultyId = session.getFacultyId()
        if (facultyId.isEmpty()) return

        val docRef = db.collection("faculties").document(facultyId)

        // 🔥 STEP 1 — Try CACHE first (instant)
        docRef.get(com.google.firebase.firestore.Source.CACHE)
            .addOnSuccessListener { doc ->

                if (doc.exists()) {
                    bindProfileData(doc)
                } else {
                    // 🔥 STEP 2 — If not in cache → SERVER
                    docRef.get(com.google.firebase.firestore.Source.SERVER)
                        .addOnSuccessListener { serverDoc ->
                            if (serverDoc.exists()) {
                                bindProfileData(serverDoc)
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(),
                                "Failed to load profile: ${e.message}",
                                Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Failed to load profile: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun bindProfileData(doc: com.google.firebase.firestore.DocumentSnapshot) {

        val fullName      = doc.getString("fullName").orEmpty()
        val designation   = doc.getString("designation").orEmpty()
        val experience    = doc.getString("experience").orEmpty()
        val role          = doc.getString("role").orEmpty()
        val empId         = doc.getString("employeeId").orEmpty()
        val deptName      = doc.getString("courseName").orEmpty()
        val deptCode      = doc.getString("courseCode").orEmpty()
        val qualification = doc.getString("qualification").orEmpty()
        val specialization = doc.getString("specialization").orEmpty()
        val email         = doc.getString("email").orEmpty()
        val phone         = doc.getString("phone").orEmpty()
        val address       = doc.getString("address").orEmpty()
        val gender        = doc.getString("gender").orEmpty()
        val dob           = doc.getString("dateOfBirth").orEmpty()
        val joining       = doc.getString("joiningDate").orEmpty()
        val photoUrl      = doc.getString("photoUrl").orEmpty()

        tvFullName.text      = fullName
        tvDesignation.text   = designation
        tvExperience.text    = experience
        tvRoleLabel.text     = role
        tvEmployeeIdShort.text = if (empId.length > 5) empId.takeLast(5) else empId
        tvDepartment.text    = "$deptName ($deptCode)"
        tvQualification.text = "$qualification • $specialization"
        tvEmail.text         = email
        tvPhone.text         = phone
        tvAddress.text       = address
        tvGender.text        = gender
        tvDob.text           = dob
        tvJoiningDate.text   = joining

        if (photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(imgProfile)
        }
    }

    private fun setupClickListeners() {
        btnChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                session.logout()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Stay", null)
            .show()
    }
}
