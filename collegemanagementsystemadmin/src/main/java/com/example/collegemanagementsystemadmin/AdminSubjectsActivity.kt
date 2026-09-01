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
import com.example.collegemanagementsystemadmin.adapters.SubjectAdapter
import com.example.collegemanagementsystemadmin.models.SubjectUi
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class AdminSubjectsActivity : CoreBaseActivity() {

    // Views
    private lateinit var topBar: MaterialToolbar
    private lateinit var etSearch: EditText
    private lateinit var btnOpenFilters: Button
    private lateinit var btnAddSubject: Button
    private lateinit var btnAddFromEmpty: Button
    private lateinit var tvSubjectCount: TextView
    private lateinit var rvSubjects: RecyclerView
    private lateinit var emptyState: View
    private lateinit var progress: ProgressBar
    private lateinit var contentContainer: View
    private lateinit var activeFiltersContainer: LinearLayout
    private lateinit var tvActiveFilters: TextView

    // Adapter & Data
    private lateinit var adapter: SubjectAdapter
    private val allItems = mutableListOf<SubjectUi>()

    // Filter options
    private val courseOptions = mutableListOf<String>()
    private val courseDetailsMap = mutableMapOf<String, Pair<String, Int>>() // courseName -> (courseId, durationYears)

    private var selectedCourse = "All Courses"
    private var selectedYear = "All Years"
    private var selectedSem = "All Semesters"
    private var selectedStatus = "All Status"

    // Firebase
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_subjects)

        bindViews()
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupButtons()
        fetchSubjects()
    }

    override fun onResume() {
        super.onResume()
        // Refresh when returning from add/edit
        fetchSubjects()
    }

    private fun bindViews() {
        topBar = findViewById(R.id.topBar)
        etSearch = findViewById(R.id.etSearch)
        btnOpenFilters = findViewById(R.id.btnOpenFilters)
        btnAddSubject = findViewById(R.id.btnAddSubject)
        btnAddFromEmpty = findViewById(R.id.btnAddFromEmpty)
        tvSubjectCount = findViewById(R.id.tvSubjectCount)
        rvSubjects = findViewById(R.id.rvSubjects)
        emptyState = findViewById(R.id.emptyState)
        progress = findViewById(R.id.progressOverlay)
        contentContainer = findViewById(R.id.contentContainer)
        activeFiltersContainer = findViewById(R.id.activeFiltersContainer)
        tvActiveFilters = findViewById(R.id.tvActiveFilters)
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        rvSubjects.layoutManager = LinearLayoutManager(this)
        adapter = SubjectAdapter(
            items = mutableListOf(),
            onView = { subject -> openViewSubject(subject) },
            onEdit = { subject -> openEditSubject(subject) },
            onDelete = { subject -> confirmDeleteSubject(subject) }
        )
        rvSubjects.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupButtons() {
        btnAddSubject.setOnClickListener {
            openAddSubject()
        }

        btnAddFromEmpty.setOnClickListener {
            openAddSubject()
        }

        btnOpenFilters.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        val sheet = SubjectFilterBottomSheetFragment()
        sheet.courseOptions = courseOptions
        sheet.courseDetailsMap = courseDetailsMap

        sheet.onApply = { course, year, sem, status ->
            selectedCourse = course
            selectedYear = year
            selectedSem = sem
            selectedStatus = status
            applyFilters()
            updateActiveFiltersDisplay()
        }

        sheet.onClear = {
            selectedCourse = "All Courses"
            selectedYear = "All Years"
            selectedSem = "All Semesters"
            selectedStatus = "All Status"
            applyFilters()
            updateActiveFiltersDisplay()
        }

        sheet.show(supportFragmentManager, "SubjectFilterSheet")
    }

    private fun updateActiveFiltersDisplay() {
        val hasFilters = selectedCourse != "All Courses" ||
                selectedYear != "All Years" ||
                selectedSem != "All Semesters" ||
                selectedStatus != "All Status"

        if (hasFilters) {
            val filterText = buildString {
                if (selectedCourse != "All Courses") append("Course: $selectedCourse | ")
                if (selectedYear != "All Years") append("Year: ${selectedYear.replace("Year ", "")} | ")
                if (selectedSem != "All Semesters") append("Sem: ${selectedSem.replace("Semester ", "")} | ")
                if (selectedStatus != "All Status") append("Status: $selectedStatus")
            }.trimEnd('|', ' ')

            tvActiveFilters.text = filterText
            activeFiltersContainer.visibility = View.VISIBLE
        } else {
            activeFiltersContainer.visibility = View.GONE
        }
    }

    private fun openAddSubject() {
        val intent = Intent(this, AdminAddSubjectActivity::class.java)
        startActivity(intent)
    }

    private fun openEditSubject(subject: SubjectUi) {
        val intent = Intent(this, AdminAddSubjectActivity::class.java)
        intent.putExtra("id", subject.id)
        intent.putExtra("mode", "edit")
        startActivity(intent)
    }

    private fun openViewSubject(subject: SubjectUi) {
        val intent = Intent(this, AdminAddSubjectActivity::class.java)
        intent.putExtra("id", subject.id)
        intent.putExtra("mode", "view")
        startActivity(intent)
    }

    private fun confirmDeleteSubject(subject: SubjectUi) {
        AlertDialog.Builder(this)
            .setTitle("Delete Subject?")
            .setMessage("Are you sure you want to delete \"${subject.name}\"?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteSubject(subject)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSubject(subject: SubjectUi) {
        setLoading(true)

        db.collection("subjects").document(subject.id)
            .delete()
            .addOnSuccessListener {
                setLoading(false)

                // Remove from lists
                allItems.remove(subject)
                adapter.removeItem(subject)

                updateSubjectCount()
                updateEmpty()

                Toast.makeText(
                    this,
                    "✅ Subject deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(
                    this,
                    "❌ Delete failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun fetchSubjects() {
        setLoading(true)

        // First, load course details
        db.collection("courses")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { courseSnapshot ->
                courseDetailsMap.clear()

                for (doc in courseSnapshot.documents) {
                    val name = doc.getString("name") ?: ""
                    val code = doc.getString("code") ?: ""
                    val display = "$name ($code)"

                    val duration = when (val raw = doc.get("durationYears")) {
                        is Number -> raw.toInt()
                        is String -> raw.toIntOrNull() ?: 3
                        else -> 3
                    }

                    courseDetailsMap[display] = Pair(code, duration)
                }

                // Then load subjects
                db.collection("subjects")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        allItems.clear()

                        for (doc in snapshot.documents) {
                            val id = doc.id
                            val name = doc.getString("name")?.trim() ?: ""
                            val subjectId = doc.getString("subjectId")?.trim() ?: ""
                            val courseId = doc.getString("courseId")?.trim() ?: ""
                            val courseName = doc.getString("courseName")?.trim() ?: ""

                            val year = when (val rawYear = doc.get("year")) {
                                is Number -> rawYear.toInt().toString()
                                is String -> rawYear.trim()
                                else -> ""
                            }

                            val semester = when (val rawSem = doc.get("semester")) {
                                is Number -> rawSem.toInt().toString()
                                is String -> rawSem.trim()
                                else -> ""
                            }

                            val status = doc.getString("status")?.trim() ?: "Active"

                            allItems.add(SubjectUi(id, name, subjectId, courseId, courseName, year, semester, status))
                        }

                        populateCourseFilter()
                        applyFilters()
                        setLoading(false)
                        updateEmpty()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        updateEmpty()
                        Toast.makeText(
                            this,
                            "Failed to load subjects: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(
                    this,
                    "Failed to load courses: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun populateCourseFilter() {
        // Extract unique course names
        val uniqueCourses = allItems.map { it.courseName }.distinct().sorted()

        courseOptions.clear()
        courseOptions.add("All Courses")
        courseOptions.addAll(uniqueCourses)
    }

    private fun applyFilters() {
        val query = etSearch.text.toString().trim().lowercase()

        var list = allItems.toList()

        // Filter by course
        if (selectedCourse != "All Courses") {
            list = list.filter { it.courseName == selectedCourse }
        }

        // Filter by year
        if (selectedYear != "All Years") {
            val yearNum = selectedYear.replace("Year ", "")
            list = list.filter { it.year == yearNum }
        }

        // Filter by semester
        if (selectedSem != "All Semesters") {
            val semNum = selectedSem.replace("Semester ", "")
            list = list.filter { it.semester == semNum }
        }

        // Filter by status
        if (selectedStatus != "All Status") {
            list = list.filter { it.status.equals(selectedStatus, ignoreCase = true) }
        }

        // Filter by search query
        if (query.isNotEmpty()) {
            list = list.filter {
                it.name.lowercase().contains(query) ||
                        it.subjectId.lowercase().contains(query) ||
                        it.courseId.lowercase().contains(query) ||
                        it.courseName.lowercase().contains(query)
            }
        }

        // Sort by name
        list = list.sortedBy { it.name.lowercase() }

        adapter.replaceAll(list)
        updateSubjectCount()
        updateEmpty()
    }

    private fun updateSubjectCount() {
        val count = adapter.itemCount
        tvSubjectCount.text = if (count == 1) "1 subject" else "$count subjects"
    }

    private fun updateEmpty() {
        emptyState.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        rvSubjects.visibility = if (adapter.itemCount == 0) View.GONE else View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        contentContainer.alpha = if (loading) 0.6f else 1f
        contentContainer.isEnabled = !loading
    }
}
