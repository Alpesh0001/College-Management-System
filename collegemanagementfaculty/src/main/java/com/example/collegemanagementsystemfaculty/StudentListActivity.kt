package com.example.collegemanagementsystemfaculty

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.adapters.StudentAdapter
import com.example.collegemanagementsystemfaculty.models.StudentModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class StudentListActivity : AppCompatActivity() {

    private lateinit var topBar: MaterialToolbar
    private lateinit var recyclerStudents: RecyclerView
    private lateinit var tvEmpty: TextView

    private lateinit var adapter: StudentAdapter
    private val studentList = mutableListOf<StudentModel>()

    private val db = FirebaseFirestore.getInstance()

    private var divisionId = ""
    private var divisionName = ""

    companion object {
        private const val TAG = "StudentListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_list)

        // 🔹 Get data from Intent
        divisionId   = intent.getStringExtra("division_id") ?: ""
        divisionName = intent.getStringExtra("division_name") ?: ""

        // ✅ Debug Log — Check if divisionId is received
        Log.d(TAG, "Received divisionId: '$divisionId'")
        Log.d(TAG, "Received divisionName: '$divisionName'")

        bindViews()
        setupToolbar()
        setupRecycler()
        loadStudents()
    }

    private fun bindViews() {
        topBar = findViewById(R.id.topBar)
        recyclerStudents = findViewById(R.id.recyclerStudents)
        tvEmpty = findViewById(R.id.tvEmpty)
    }

    private fun setupToolbar() {
        topBar.title = if (divisionName.isNotEmpty())
            "Students — $divisionName"
        else
            "Students"
        topBar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecycler() {
        adapter = StudentAdapter(studentList) { student ->
            val intent = Intent(this, StudentDetailsActivity::class.java)
            intent.putExtra("studentId", student.id)
            startActivity(intent)
        }
        recyclerStudents.layoutManager = LinearLayoutManager(this)
        recyclerStudents.adapter = adapter
    }

    // 🔥 FIXED: Load Students from Firestore
    private fun loadStudents() {

        // ✅ Check divisionId is not empty
        if (divisionId.isEmpty()) {
            Log.e(TAG, "ERROR: divisionId is EMPTY!")
            showEmpty("Invalid division — divisionId is empty")
            return
        }

        Log.d(TAG, "Querying Firestore: students where divisionId == '$divisionId'")

        // ✅ Show loading state
        tvEmpty.text = "Loading students..."
        tvEmpty.visibility = View.VISIBLE
        recyclerStudents.visibility = View.GONE

        db.collection("students")
            .whereEqualTo("divisionId", divisionId)
            .get()
            .addOnSuccessListener { snapshot ->

                // ✅ Debug Log — See how many docs returned
                Log.d(TAG, "Firestore snapshot size: ${snapshot.size()}")
                Log.d(TAG, "Firestore isEmpty: ${snapshot.isEmpty}")

                // ✅ Log all documents for debugging
                for (doc in snapshot.documents) {
                    Log.d(TAG, "Doc ID: ${doc.id}")
                    Log.d(TAG, "Doc Data: ${doc.data}")
                }

                studentList.clear()

                if (snapshot.isEmpty) {
                    Log.w(TAG, "No students found for divisionId: $divisionId")
                    showEmpty("No students found in this division")
                    return@addOnSuccessListener
                }

                for (doc in snapshot.documents) {
                    val student = StudentModel(
                        id = doc.id,
                        fullName = doc.getString("fullName") ?: "",
                        rollNo = doc.getString("rollNo") ?: "",
                        // ✅ FIXED: Use courseId, not courseName
                        courseName = doc.getString("courseName")
                            ?: doc.getString("courseId")
                            ?: "",
                        // ✅ FIXED: year may not exist, use semester
                        year = doc.getString("year")
                            ?: doc.getString("semester")
                            ?: "",
                        semester = doc.getString("semester") ?: "",
                        divisionId = doc.getString("divisionId") ?: "",
                        divisionName = doc.getString("divisionName") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: ""
                    )

                    Log.d(TAG, "Student added: ${student.fullName} | Roll: ${student.rollNo}")
                    studentList.add(student)
                }

                Log.d(TAG, "Total students loaded: ${studentList.size}")

                // ✅ Update adapter
                adapter.updateList(studentList)

                tvEmpty.visibility = View.GONE
                recyclerStudents.visibility = View.VISIBLE
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Firestore FAILED: ${exception.message}", exception)
                showEmpty("Failed to load students: ${exception.message}")
            }
    }

    // ✅ Helper function
    private fun showEmpty(message: String) {
        tvEmpty.text = message
        tvEmpty.visibility = View.VISIBLE
        recyclerStudents.visibility = View.GONE
    }
}
