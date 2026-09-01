package com.example.collegemanagementsystemadmin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemadmin.adapters.StudentAdapter
import com.example.collegemanagementsystemadmin.models.Student
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class AdminStudentsActivity : CoreBaseActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var topBar: MaterialToolbar
    private lateinit var etSearch: EditText
    private lateinit var btnOpenFilters: Button
    private lateinit var btnAddStudent: Button
    private lateinit var activeFiltersContainer: LinearLayout
    private lateinit var tvActiveFilters: TextView
    private lateinit var tvStudentCount: TextView
    private lateinit var rvStudents: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var progressOverlay: ProgressBar

    private lateinit var studentAdapter: StudentAdapter
    private val allStudents = mutableListOf<Student>()
    private val filteredStudents = mutableListOf<Student>()

    // Filter state
    private var filterCourse: String? = null
    private var filterYear: String? = null
    private var filterSem: String? = null
    private var sortOrder: String = "Name (A-Z)"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_students)

        bindViews()
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        setupSearch()
        loadStudents()
    }

    private fun bindViews() {
        topBar = findViewById(R.id.topBar)
        etSearch = findViewById(R.id.etSearch)
        btnOpenFilters = findViewById(R.id.btnOpenFilters)
        btnAddStudent = findViewById(R.id.btnAddStudent)
        activeFiltersContainer = findViewById(R.id.activeFiltersContainer)
        tvActiveFilters = findViewById(R.id.tvActiveFilters)
        tvStudentCount = findViewById(R.id.tvStudentCount)
        rvStudents = findViewById(R.id.rvStudents)
        emptyState = findViewById(R.id.emptyState)
        progressOverlay = findViewById(R.id.progressOverlay)
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentAdapter(
            students = filteredStudents,
            onItemClick = { student ->
                openStudentDetail(student, "view")
            },
            onEditClick = { student ->
                openStudentDetail(student, "edit")
            },
            onDeleteClick = { student ->
                confirmDelete(student)
            }
        )

        rvStudents.apply {
            layoutManager = LinearLayoutManager(this@AdminStudentsActivity)
            adapter = studentAdapter
        }
    }

    private fun setupButtons() {
        btnAddStudent.setOnClickListener {
            val intent = Intent(this, AdminAddStudentActivity::class.java)
            intent.putExtra("mode", "add")
            startActivityForResult(intent, REQUEST_ADD_STUDENT)
        }

        btnOpenFilters.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFiltersAndSearch()
            }
        })
    }

    private fun loadStudents() {
        progressOverlay.visibility = View.VISIBLE

        db.collection("students")
            .get()
            .addOnSuccessListener { snapshot ->
                allStudents.clear()
                snapshot.documents.forEach { doc ->
                    val student = Student(
                        id = doc.id,
                        fullName = doc.getString("fullName") ?: "",
                        grNo = doc.getString("grNo") ?: "",
                        rollNo = doc.getString("rollNo") ?: "",
                        dob = doc.getString("dob") ?: "",
                        gender = doc.getString("gender") ?: "",
                        bloodGroup = doc.getString("bloodGroup") ?: "",
                        phone = doc.getString("phone") ?: "",
                        email = doc.getString("email") ?: "",
                        address = doc.getString("address") ?: "",
                        courseId = doc.getString("courseId") ?: "",
                        courseName = doc.getString("courseName") ?: "",
                        courseCode = doc.getString("courseCode") ?: "",
                        year = doc.getString("year") ?: "",
                        semester = doc.getString("semester") ?: "",
                        admissionYear = doc.getString("admissionYear") ?: "",
                        status = doc.getString("status") ?: "",
                        tempPassword = doc.getString("tempPassword") ?: "",
                        passwordStatus = doc.getString("passwordStatus") ?: "not_set",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                    allStudents.add(student)
                }
                applyFiltersAndSearch()
                progressOverlay.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load students: ${e.message}", Toast.LENGTH_LONG).show()
                progressOverlay.visibility = View.GONE
                updateEmptyState()
            }
    }

    private fun showFilterDialog() {
        val dialog = FilterBottomSheetDialog { course, year, sem, sort ->
            filterCourse = course
            filterYear = year
            filterSem = sem
            sortOrder = sort
            applyFiltersAndSearch()
            updateActiveFiltersDisplay()
        }
        dialog.show(supportFragmentManager, "FilterDialog")
    }

    private fun applyFiltersAndSearch() {
        val searchQuery = etSearch.text.toString().trim().lowercase()

        filteredStudents.clear()
        filteredStudents.addAll(
            allStudents.filter { student ->
                // Apply search
                val matchesSearch = if (searchQuery.isEmpty()) {
                    true
                } else {
                    student.fullName.lowercase().contains(searchQuery) ||
                            student.rollNo.lowercase().contains(searchQuery) ||
                            student.grNo.lowercase().contains(searchQuery)
                }

                // Apply filters
                val matchesCourse = filterCourse?.let { student.courseCode == it } ?: true
                val matchesYear = filterYear?.let { student.year == it } ?: true
                val matchesSem = filterSem?.let { student.semester == it } ?: true

                matchesSearch && matchesCourse && matchesYear && matchesSem
            }
        )

        // Apply sorting
        when (sortOrder) {
            "Name (A-Z)" -> filteredStudents.sortBy { it.fullName.lowercase() }
            "Name (Z-A)" -> filteredStudents.sortByDescending { it.fullName.lowercase() }
            "Roll Number" -> filteredStudents.sortBy { it.rollNo }
            "Recently Added" -> filteredStudents.sortByDescending { it.createdAt }
        }

        studentAdapter.updateList(filteredStudents)
        updateStudentCount()
        updateEmptyState()
    }

    private fun updateActiveFiltersDisplay() {
        val hasFilters = filterCourse != null || filterYear != null || filterSem != null

        if (hasFilters) {
            val filterTexts = mutableListOf<String>()
            filterCourse?.let { filterTexts.add("Course: $it") }
            filterYear?.let { filterTexts.add("Year: $it") }
            filterSem?.let { filterTexts.add("Semester: $it") }

            tvActiveFilters.text = filterTexts.joinToString(" | ")
            activeFiltersContainer.visibility = View.VISIBLE
        } else {
            activeFiltersContainer.visibility = View.GONE
        }
    }

    private fun updateStudentCount() {
        val count = filteredStudents.size
        tvStudentCount.text = "Showing $count student${if (count != 1) "s" else ""}"
    }

    private fun updateEmptyState() {
        if (filteredStudents.isEmpty()) {
            rvStudents.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvStudents.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun openStudentDetail(student: Student, mode: String) {
        val intent = Intent(this, AdminAddStudentActivity::class.java)
        intent.putExtra("mode", mode)
        intent.putExtra("studentId", student.id)
        startActivityForResult(intent, REQUEST_EDIT_STUDENT)
    }

    private fun confirmDelete(student: Student) {
        AlertDialog.Builder(this)
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to delete ${student.fullName}?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteStudent(student)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteStudent(student: Student) {
        progressOverlay.visibility = View.VISIBLE

        db.collection("students").document(student.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Student deleted successfully", Toast.LENGTH_SHORT).show()
                loadStudents() // Reload list
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
                progressOverlay.visibility = View.GONE
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_ADD_STUDENT, REQUEST_EDIT_STUDENT -> {
                    loadStudents() // Reload list after add/edit
                }
            }
        }
    }

    companion object {
        private const val REQUEST_ADD_STUDENT = 101
        private const val REQUEST_EDIT_STUDENT = 102
    }
}
