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
import com.example.collegemanagementsystemadmin.adapters.FacultyAdapter
import com.example.collegemanagementsystemadmin.models.Faculty
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class AdminFacultyActivity : CoreBaseActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var topBar: MaterialToolbar
    private lateinit var etSearch: EditText
    private lateinit var btnOpenFilters: Button
    private lateinit var btnAddFaculty: Button
    private lateinit var activeFiltersContainer: LinearLayout
    private lateinit var tvActiveFilters: TextView
    private lateinit var tvFacultyCount: TextView
    private lateinit var rvFaculties: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var progressOverlay: ProgressBar

    private lateinit var facultyAdapter: FacultyAdapter
    private val allFaculties = mutableListOf<Faculty>()
    private val filteredFaculties = mutableListOf<Faculty>()

    // Filter state
    private var filterCourse: String? = null
    private var filterRole: String? = null
    private var filterDesignation: String? = null
    private var filterStatus: String? = null
    private var sortOrder: String = "Name (A-Z)"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_faculty)

        bindViews()
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        setupSearch()
        loadFaculties()
    }

    private fun bindViews() {
        topBar = findViewById(R.id.topBar)
        etSearch = findViewById(R.id.etSearch)
        btnOpenFilters = findViewById(R.id.btnOpenFilters)
        btnAddFaculty = findViewById(R.id.btnAddFaculty)
        activeFiltersContainer = findViewById(R.id.activeFiltersContainer)
        tvActiveFilters = findViewById(R.id.tvActiveFilters)
        tvFacultyCount = findViewById(R.id.tvFacultyCount)
        rvFaculties = findViewById(R.id.rvFaculties)
        emptyState = findViewById(R.id.emptyState)
        progressOverlay = findViewById(R.id.progressOverlay)
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        facultyAdapter = FacultyAdapter(
            faculties = filteredFaculties,
            onItemClick = { faculty ->
                openFacultyDetail(faculty, "view")
            },
            onEditClick = { faculty ->
                openFacultyDetail(faculty, "edit")
            },
            onDeleteClick = { faculty ->
                confirmDelete(faculty)
            }
        )

        rvFaculties.apply {
            layoutManager = LinearLayoutManager(this@AdminFacultyActivity)
            adapter = facultyAdapter
        }
    }

    private fun setupButtons() {
        btnAddFaculty.setOnClickListener {
            val intent = Intent(this, AdminAddFacultyActivity::class.java)
            intent.putExtra("mode", "add")
            startActivityForResult(intent, REQUEST_ADD_FACULTY)
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

    private fun loadFaculties() {
        progressOverlay.visibility = View.VISIBLE

        db.collection("faculties")
            .get()
            .addOnSuccessListener { snapshot ->
                allFaculties.clear()
                snapshot.documents.forEach { doc ->
                    val faculty = Faculty(
                        id = doc.id,
                        employeeId = doc.getString("employeeId") ?: "",
                        fullName = doc.getString("fullName") ?: "",
                        dateOfBirth = doc.getString("dateOfBirth") ?: "",
                        gender = doc.getString("gender") ?: "",
                        phone = doc.getString("phone") ?: "",
                        email = doc.getString("email") ?: "",
                        address = doc.getString("address") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        qualification = doc.getString("qualification") ?: "",
                        specialization = doc.getString("specialization") ?: "",
                        experience = doc.getString("experience") ?: "",
                        joiningDate = doc.getString("joiningDate") ?: "",
                        designation = doc.getString("designation") ?: "",
                        role = doc.getString("role") ?: "Faculty",
                        courseCode = doc.getString("courseCode") ?: "",
                        courseName = doc.getString("courseName") ?: "",
                        courseId = doc.getString("courseId") ?: "",
                        salary = (doc.get("salary") as? Long)?.toInt(),
                        status = doc.getString("status") ?: "Active",
                        tempPassword = doc.getString("tempPassword") ?: "",
                        passwordStatus = doc.getString("passwordStatus") ?: "not_set",
                        createdAt = doc.getTimestamp("createdAt"),
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                    allFaculties.add(faculty)
                }
                applyFiltersAndSearch()
                progressOverlay.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load faculties: ${e.message}", Toast.LENGTH_LONG).show()
                progressOverlay.visibility = View.GONE
                updateEmptyState()
            }
    }

    private fun showFilterDialog() {
        val dialog = FacultyFilterBottomSheetDialog { course, role, designation, status, sort ->
            filterCourse = course
            filterRole = role
            filterDesignation = designation
            filterStatus = status
            sortOrder = sort
            applyFiltersAndSearch()
            updateActiveFiltersDisplay()
        }
        dialog.show(supportFragmentManager, "FacultyFilterDialog")
    }

    private fun applyFiltersAndSearch() {
        val searchQuery = etSearch.text.toString().trim().lowercase()

        filteredFaculties.clear()
        filteredFaculties.addAll(
            allFaculties.filter { faculty ->
                // Apply search
                val matchesSearch = if (searchQuery.isEmpty()) {
                    true
                } else {
                    faculty.fullName.lowercase().contains(searchQuery) ||
                            faculty.employeeId.lowercase().contains(searchQuery) ||
                            faculty.email.lowercase().contains(searchQuery)
                }

                // Apply filters
                val matchesCourse = filterCourse?.let { faculty.courseCode == it } ?: true
                val matchesRole = filterRole?.let { faculty.role == it } ?: true
                val matchesDesignation = filterDesignation?.let { faculty.designation == it } ?: true
                val matchesStatus = filterStatus?.let { faculty.status == it } ?: true

                matchesSearch && matchesCourse && matchesRole && matchesDesignation && matchesStatus
            }
        )

        // Apply sorting
        when (sortOrder) {
            "Name (A-Z)" -> filteredFaculties.sortBy { it.fullName.lowercase() }
            "Name (Z-A)" -> filteredFaculties.sortByDescending { it.fullName.lowercase() }
            "Employee ID" -> filteredFaculties.sortBy { it.employeeId }
            "Recently Added" -> filteredFaculties.sortByDescending { it.createdAt }
        }

        facultyAdapter.updateList(filteredFaculties)
        updateFacultyCount()
        updateEmptyState()
    }

    private fun updateActiveFiltersDisplay() {
        val hasFilters = filterCourse != null || filterRole != null ||
                filterDesignation != null || filterStatus != null

        if (hasFilters) {
            val filterTexts = mutableListOf<String>()
            filterCourse?.let { filterTexts.add("Course: $it") }
            filterRole?.let { filterTexts.add("Role: $it") }
            filterDesignation?.let { filterTexts.add("Designation: $it") }
            filterStatus?.let { filterTexts.add("Status: $it") }

            tvActiveFilters.text = filterTexts.joinToString(" | ")
            activeFiltersContainer.visibility = View.VISIBLE
        } else {
            activeFiltersContainer.visibility = View.GONE
        }
    }

    private fun updateFacultyCount() {
        val count = filteredFaculties.size
        tvFacultyCount.text = "Showing $count ${if (count != 1) "faculties" else "faculty"}"
    }

    private fun updateEmptyState() {
        if (filteredFaculties.isEmpty()) {
            rvFaculties.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvFaculties.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun openFacultyDetail(faculty: Faculty, mode: String) {
        val intent = Intent(this, AdminAddFacultyActivity::class.java)
        intent.putExtra("mode", mode)
        intent.putExtra("facultyId", faculty.id)
        startActivityForResult(intent, REQUEST_EDIT_FACULTY)
    }

    private fun confirmDelete(faculty: Faculty) {
        AlertDialog.Builder(this)
            .setTitle("Delete Faculty")
            .setMessage("Are you sure you want to delete ${faculty.fullName}?\n\nEmployee ID: ${faculty.employeeId}\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteFaculty(faculty)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFaculty(faculty: Faculty) {
        progressOverlay.visibility = View.VISIBLE

        db.collection("faculties").document(faculty.id)
            .delete()
            .addOnSuccessListener {
                // If faculty was HOD, remove from course
                if (faculty.role == "HOD" && faculty.courseId.isNotEmpty()) {
                    db.collection("courses").document(faculty.courseId)
                        .update(
                            mapOf(
                                "hodId" to null,
                                "hodName" to null,
                                "hodEmail" to null,
                                "hodPhone" to null
                            )
                        )
                }

                Toast.makeText(this, "✅ Faculty deleted successfully", Toast.LENGTH_SHORT).show()
                loadFaculties() // Reload list
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
                REQUEST_ADD_FACULTY, REQUEST_EDIT_FACULTY -> {
                    loadFaculties() // Reload list after add/edit
                }
            }
        }
    }

    companion object {
        private const val REQUEST_ADD_FACULTY = 201
        private const val REQUEST_EDIT_FACULTY = 202
    }
}
