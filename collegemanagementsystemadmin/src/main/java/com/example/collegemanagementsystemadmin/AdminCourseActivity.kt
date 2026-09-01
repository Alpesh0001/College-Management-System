package com.example.collegemanagementsystemadmin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemadmin.adapters.CourseAdapter
import com.example.collegemanagementsystemadmin.models.CourseUi
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class AdminCourseActivity : CoreBaseActivity() {

    // Views
    private lateinit var topBar: MaterialToolbar
    private lateinit var etSearch: EditText
    private lateinit var btnAddCourse: Button
    private lateinit var btnAddFromEmpty: Button
    private lateinit var tvCourseCount: TextView
    private lateinit var rvCourses: RecyclerView
    private lateinit var emptyState: View
    private lateinit var progress: ProgressBar
    private lateinit var contentContainer: View

    // Adapter & Data
    private lateinit var adapter: CourseAdapter
    private val allItems = mutableListOf<CourseUi>()
    private val shownItems = mutableListOf<CourseUi>()

    // Firebase
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_course)

        bindViews()
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupButtons()
        fetchCourses()
    }

    override fun onResume() {
        super.onResume()
        // Refresh list when returning from add/edit
        fetchCourses()
    }

    private fun bindViews() {
        topBar = findViewById(R.id.topBar)
        etSearch = findViewById(R.id.etSearchPlain)
        btnAddCourse = findViewById(R.id.btnAddDepartment)
        btnAddFromEmpty = findViewById(R.id.btnAddFromEmpty)
        tvCourseCount = findViewById(R.id.tvCourseCount)
        rvCourses = findViewById(R.id.rvDepartments)
        emptyState = findViewById(R.id.emptyState)
        progress = findViewById(R.id.progressOverlay)
        contentContainer = findViewById(R.id.contentContainer)
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        rvCourses.layoutManager = LinearLayoutManager(this)
        adapter = CourseAdapter(shownItems) { course ->
            // Open course details
            val intent = Intent(this, AdminCourseDetailsActivity::class.java)
            intent.putExtra("id", course.id)
            intent.putExtra("courseTitle", "${course.name} (${course.code})")
            startActivity(intent)
        }
        rvCourses.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applySearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupButtons() {
        btnAddCourse.setOnClickListener {
            openAddCourse()
        }

        btnAddFromEmpty.setOnClickListener {
            openAddCourse()
        }
    }

    private fun openAddCourse() {
        val intent = Intent(this, AdminAddCourseActivity::class.java)
        startActivity(intent)
    }

    private fun fetchCourses() {
        setLoading(true)

        db.collection("courses")
            .get()
            .addOnSuccessListener { snapshot ->
                allItems.clear()

                for (doc in snapshot.documents) {
                    val id = doc.id
                    val name = doc.getString("name")?.trim() ?: ""
                    val code = doc.getString("code")?.trim() ?: ""

                    // Handle durationYears as Number or String
                    val rawYearsAny = doc.get("durationYears")
                    val rawYears = when (rawYearsAny) {
                        is Number -> rawYearsAny.toInt().toString()
                        is String -> rawYearsAny.trim()
                        else -> ""
                    }
                    val years = normalizeYears(rawYears)

                    val status = doc.getString("status")?.trim() ?: "Active"

                    allItems.add(CourseUi(id, name, code, years, status))
                }

                shownItems.clear()
                shownItems.addAll(allItems)
                applySearch()
                setLoading(false)
                updateEmpty()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                updateEmpty()
                Toast.makeText(
                    this,
                    "Failed to load courses: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun applySearch() {
        val query = etSearch.text.toString().trim().lowercase()

        val list = if (query.isEmpty()) {
            allItems.toMutableList()
        } else {
            allItems.filter {
                it.name.lowercase().contains(query) ||
                        it.code.lowercase().contains(query)
            }.toMutableList()
        }

        // Sort alphabetically by name
        list.sortBy { it.name.lowercase() }

        adapter.replaceAll(list)
        updateCourseCount()
        updateEmpty()
    }

    private fun normalizeYears(input: String): String {
        if (input.isBlank()) return ""
        val digits = input.filter { it.isDigit() }
        if (digits.isNotBlank()) {
            val num = digits.toInt()
            return "$num Year${if (num > 1) "s" else ""}"
        }
        return input.trim()
    }

    private fun updateCourseCount() {
        val count = shownItems.size
        tvCourseCount.text = if (count == 1) "1 course" else "$count courses"
    }

    private fun updateEmpty() {
        emptyState.visibility = if (shownItems.isEmpty()) View.VISIBLE else View.GONE
        rvCourses.visibility = if (shownItems.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        contentContainer.alpha = if (loading) 0.6f else 1f
        contentContainer.isEnabled = !loading
    }
}
