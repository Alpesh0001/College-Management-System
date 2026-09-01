package com.example.collegemanagementsystemfaculty

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class StudentDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var imgProfile: CircleImageView
    private lateinit var tvName: TextView
    private lateinit var tvRoll: TextView
    private lateinit var tvDob: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvCourse: TextView
    private lateinit var tvYear: TextView
    private lateinit var tvSem: TextView
    private lateinit var tvDivision: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvGr: TextView
    private lateinit var topBar: MaterialToolbar

    private var studentId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_details)

        studentId = intent.getStringExtra("studentId") ?: ""

        bindViews()
        setupToolbar()
        loadStudent()
    }

    private fun bindViews() {

        imgProfile = findViewById(R.id.imgProfile)
        tvName = findViewById(R.id.tvName)
        tvRoll = findViewById(R.id.tvRoll)
        tvDob = findViewById(R.id.tvDob)
        tvGender = findViewById(R.id.tvGender)
        tvPhone = findViewById(R.id.tvPhone)
        tvEmail = findViewById(R.id.tvEmail)
        tvAddress = findViewById(R.id.tvAddress)
        tvCourse = findViewById(R.id.tvCourse)
        tvYear = findViewById(R.id.tvYear)
        tvSem = findViewById(R.id.tvSem)
        tvDivision = findViewById(R.id.tvDivision)
        tvStatus = findViewById(R.id.tvStatus)
        tvGr = findViewById(R.id.tvGr)
        topBar = findViewById(R.id.topBar)
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener { finish() }
    }

    private fun loadStudent() {

        if (studentId.isEmpty()) return

        db.collection("students")
            .document(studentId)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) return@addOnSuccessListener

                tvName.text = doc.getString("fullName") ?: ""
                tvRoll.text = "Roll No: " + (doc.getString("rollNo") ?: "")
                tvDob.text = "DOB: " + (doc.getString("dob") ?: "")
                tvGender.text = "Gender: " + (doc.getString("gender") ?: "")
                tvPhone.text = "Phone: " + (doc.getString("phone") ?: "")
                tvEmail.text = "Email: " + (doc.getString("email") ?: "")
                tvAddress.text = "Address: " + (doc.getString("address") ?: "")

                tvCourse.text = "Course: " + (doc.getString("courseName") ?: "")
                tvYear.text = "Year: " + (doc.getString("year") ?: "")
                tvSem.text = "Semester: " + (doc.getString("semester") ?: "")
                tvDivision.text = "Division: " + (doc.getString("divisionName") ?: "")
                tvStatus.text = "Status: " + (doc.getString("status") ?: "")
                tvGr.text = "GR No: " + (doc.getString("grNo") ?: "")

                val photoUrl = doc.getString("photoUrl") ?: ""

                if (photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_user)
                        .into(imgProfile)
                }
            }
    }
}