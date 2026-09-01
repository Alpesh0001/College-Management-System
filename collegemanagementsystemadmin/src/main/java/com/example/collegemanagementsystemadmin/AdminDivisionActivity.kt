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
import com.example.collegemanagementsystemadmin.adapters.DivisionAdapter
import com.example.collegemanagementsystemadmin.models.Division
import com.example.collegemanagementsystemadmin.models.RollRange
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class AdminDivisionActivity : CoreBaseActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var topBar: MaterialToolbar
    private lateinit var etSearch: EditText
    private lateinit var btnOpenFilters: Button
    private lateinit var btnAddDivision: Button
    private lateinit var activeFiltersContainer: LinearLayout
    private lateinit var tvActiveFilters: TextView
    private lateinit var tvDivisionCount: TextView
    private lateinit var rvDivisions: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var progressOverlay: ProgressBar

    private lateinit var divisionAdapter: DivisionAdapter
    private val allDivisions = mutableListOf<Division>()
    private val filteredDivisions = mutableListOf<Division>()

    // Filter state
    private var filterCourse: String? = null
    private var filterYear: String? = null
    private var filterSemester: String? = null
    private var sortOrder: String = "Division Name"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_division)

        bindViews()
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        setupSearch()
        loadDivisions()
    }

    private fun bindViews() {
        topBar = findViewById(R.id.topBar)
        etSearch = findViewById(R.id.etSearch)
        btnOpenFilters = findViewById(R.id.btnOpenFilters)
        btnAddDivision = findViewById(R.id.btnAddDivision)
        activeFiltersContainer = findViewById(R.id.activeFiltersContainer)
        tvActiveFilters = findViewById(R.id.tvActiveFilters)
        tvDivisionCount = findViewById(R.id.tvDivisionCount)
        rvDivisions = findViewById(R.id.rvDivisions)
        emptyState = findViewById(R.id.emptyState)
        progressOverlay = findViewById(R.id.progressOverlay)
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        divisionAdapter = DivisionAdapter(
            divisions = filteredDivisions,
            onItemClick = { division ->
                openDivisionDetail(division, "view")
            },
            onEditClick = { division ->
                openDivisionDetail(division, "edit")
            },
            onDeleteClick = { division ->
                confirmDelete(division)
            }
        )

        rvDivisions.apply {
            layoutManager = LinearLayoutManager(this@AdminDivisionActivity)
            adapter = divisionAdapter
        }
    }

    private fun setupButtons() {
        btnAddDivision.setOnClickListener {
            val intent = Intent(this, AdminAddDivisionActivity::class.java)
            intent.putExtra("mode", "add")
            startActivityForResult(intent, REQUEST_ADD_DIVISION)
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

    private fun loadDivisions() {
        progressOverlay.visibility = View.VISIBLE

        db.collection("divisions")
            .get()
            .addOnSuccessListener { snapshot ->
                allDivisions.clear()
                snapshot.documents.forEach { doc ->
                    try {
                        // Parse roll ranges
                        val rollRangesData = doc.get("rollNumberRanges") as? List<Map<String, Any>> ?: emptyList()
                        val rollRanges = rollRangesData.map { rangeMap ->
                            val start = (rangeMap["start"] as? Long)?.toInt() ?: 0
                            val end = (rangeMap["end"] as? Long)?.toInt() ?: 0
                            RollRange(start, end)
                        }

                        val division = Division(
                            id = doc.id,
                            divisionName = doc.getString("divisionName") ?: "",
                            courseId = doc.getString("courseId") ?: "",
                            courseName = doc.getString("courseName") ?: "",
                            courseCode = doc.getString("courseCode") ?: "",
                            year = doc.getString("year") ?: "",
                            semester = doc.getString("semester") ?: "",
                            capacity = (doc.getLong("capacity") ?: 0).toInt(),
                            currentStrength = (doc.getLong("currentStrength") ?: 0).toInt(),
                            rollNumberRanges = rollRanges,
                            classTeacherId = doc.getString("classTeacherId"),
                            classTeacherName = doc.getString("classTeacherName"),
                            classTeacherEmail = doc.getString("classTeacherEmail"),
                            status = doc.getString("status") ?: "Active",
                            createdAt = doc.getTimestamp("createdAt"),
                            updatedAt = doc.getTimestamp("updatedAt")
                        )
                        allDivisions.add(division)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                applyFiltersAndSearch()
                progressOverlay.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load divisions: ${e.message}", Toast.LENGTH_LONG).show()
                progressOverlay.visibility = View.GONE
                updateEmptyState()
            }
    }

    private fun showFilterDialog() {
        val dialog = DivisionFilterBottomSheetDialog { course, year, semester, sort ->
            filterCourse = course
            filterYear = year
            filterSemester = semester
            sortOrder = sort
            applyFiltersAndSearch()
            updateActiveFiltersDisplay()
        }
        dialog.show(supportFragmentManager, "DivisionFilterDialog")
    }

    private fun applyFiltersAndSearch() {
        val searchQuery = etSearch.text.toString().trim().lowercase()

        filteredDivisions.clear()
        filteredDivisions.addAll(
            allDivisions.filter { division ->
                // Apply search
                val matchesSearch = if (searchQuery.isEmpty()) {
                    true
                } else {
                    division.divisionName.lowercase().contains(searchQuery) ||
                            division.courseCode.lowercase().contains(searchQuery) ||
                            division.courseName.lowercase().contains(searchQuery)
                }

                // Apply filters
                val matchesCourse = filterCourse?.let { division.courseCode == it } ?: true
                val matchesYear = filterYear?.let { division.year == it } ?: true
                val matchesSemester = filterSemester?.let { division.semester == it } ?: true

                matchesSearch && matchesCourse && matchesYear && matchesSemester
            }
        )

        // Apply sorting
        when (sortOrder) {
            "Division Name" -> filteredDivisions.sortBy { it.divisionName }
            "Course" -> filteredDivisions.sortBy { it.courseCode }
            "Year" -> filteredDivisions.sortBy { it.year.toIntOrNull() ?: 0 }
            "Recently Added" -> filteredDivisions.sortByDescending { it.createdAt }
        }

        divisionAdapter.updateList(filteredDivisions)
        updateDivisionCount()
        updateEmptyState()
    }

    private fun updateActiveFiltersDisplay() {
        val hasFilters = filterCourse != null || filterYear != null || filterSemester != null

        if (hasFilters) {
            val filterTexts = mutableListOf<String>()
            filterCourse?.let { filterTexts.add("Course: $it") }
            filterYear?.let { filterTexts.add("Year: $it") }
            filterSemester?.let { filterTexts.add("Semester: $it") }

            tvActiveFilters.text = filterTexts.joinToString(" | ")
            activeFiltersContainer.visibility = View.VISIBLE
        } else {
            activeFiltersContainer.visibility = View.GONE
        }
    }

    private fun updateDivisionCount() {
        val count = filteredDivisions.size
        tvDivisionCount.text = "Showing $count division${if (count != 1) "s" else ""}"
    }

    private fun updateEmptyState() {
        if (filteredDivisions.isEmpty()) {
            rvDivisions.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvDivisions.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun openDivisionDetail(division: Division, mode: String) {
        val intent = Intent(this, AdminAddDivisionActivity::class.java)
        intent.putExtra("mode", mode)
        intent.putExtra("divisionId", division.id)
        startActivityForResult(intent, REQUEST_EDIT_DIVISION)
    }

    private fun confirmDelete(division: Division) {
        AlertDialog.Builder(this)
            .setTitle("Delete Division")
            .setMessage("Are you sure you want to delete Division ${division.divisionName}?\n\nCourse: ${division.courseCode}\nYear: ${division.year} | Semester: ${division.semester}\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteDivision(division)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteDivision(division: Division) {
        progressOverlay.visibility = View.VISIBLE

        db.collection("divisions").document(division.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Division deleted successfully", Toast.LENGTH_SHORT).show()
                loadDivisions() // Reload list
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
                REQUEST_ADD_DIVISION, REQUEST_EDIT_DIVISION -> {
                    loadDivisions() // Reload list after add/edit
                }
            }
        }
    }

    companion object {
        private const val REQUEST_ADD_DIVISION = 301
        private const val REQUEST_EDIT_DIVISION = 302
    }
}
