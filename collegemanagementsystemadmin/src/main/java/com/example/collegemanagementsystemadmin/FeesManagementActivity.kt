package com.example.collegemanagementsystemadmin

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class FeesManagementActivity : CoreBaseActivity() {

    private val TAG = "FeesManagementActivity"

    // Views
    private lateinit var topBar: MaterialToolbar
    private lateinit var btnOpenFilters: Button
    private lateinit var btnLoad: Button
    private lateinit var tvActiveFilters: TextView
    private lateinit var activeFiltersContainer: View
    private lateinit var cardStats: View
    private lateinit var tvTotalStudents: TextView
    private lateinit var tvTotalPaid: TextView
    private lateinit var tvTotalPending: TextView
    private lateinit var tvStudentCount: TextView
    private lateinit var rvStudents: RecyclerView
    private lateinit var layoutDefault: LinearLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressOverlay: ProgressBar
    private lateinit var etSearch: com.google.android.material.textfield.TextInputEditText
    private var allStudents = listOf<StudentFeesAdapter.StudentFeeItem>() // ✅ full list
    // Firebase
    private val db = FirebaseFirestore.getInstance()

    // Adapter
    private lateinit var adapter: StudentFeesAdapter

    // Filter state
    private var selectedCourseId: String? = null
    private var selectedCourseName: String? = null
    private var selectedYear: String? = null
    private var selectedSem: String? = null

    // Fees per sem from course
    private var semesterFeesMap = mapOf<String, Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fees_management)

        bindViews()
        setupToolbar()
        setupRecyclerView()
        setupButtons()
    }

    private fun bindViews() {
        topBar                 = findViewById(R.id.topBar)
        btnOpenFilters         = findViewById(R.id.btnOpenFilters)
        btnLoad                = findViewById(R.id.btnLoad)
        tvActiveFilters        = findViewById(R.id.tvActiveFilters)
        activeFiltersContainer = findViewById(R.id.activeFiltersContainer)
        cardStats              = findViewById(R.id.cardStats)
        tvTotalStudents        = findViewById(R.id.tvTotalStudents)
        tvTotalPaid            = findViewById(R.id.tvTotalPaid)
        tvTotalPending         = findViewById(R.id.tvTotalPending)
        tvStudentCount         = findViewById(R.id.tvStudentCount)
        rvStudents             = findViewById(R.id.rvStudents)
        layoutDefault          = findViewById(R.id.layoutDefault)
        layoutEmpty            = findViewById(R.id.layoutEmpty)
        progressOverlay        = findViewById(R.id.progressOverlay)
        etSearch = findViewById(R.id.etSearch)
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = StudentFeesAdapter(
            students = mutableListOf(),
            onStudentClick = { student ->
                // ✅ Open fee detail bottom sheet
                val totalAmount = semesterFeesMap[selectedSem] ?: 0L
                FeeUpdateBottomSheet(
                    student = student,
                    semNumber = selectedSem ?: "1",
                    totalAmount = totalAmount,
                    onSaved = {
                        // Refresh list after update
                        loadStudentFees()
                    }
                ).show(supportFragmentManager, "FeeUpdateBottomSheet")
            }
        )
        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.adapter = adapter
    }

    private fun setupButtons() {
        btnOpenFilters.setOnClickListener {
            FeesFilterBottomSheet { courseId, courseName, year, sem ->
                selectedCourseId   = courseId
                selectedCourseName = courseName
                selectedYear       = year
                selectedSem        = sem

                // ✅ Show active filters
                if (!courseId.isNullOrEmpty()) {
                    activeFiltersContainer.visibility = View.VISIBLE
                    tvActiveFilters.text =
                        "Course: $courseName | Year: $year | Sem: $sem"
                    btnLoad.visibility = View.VISIBLE
                } else {
                    activeFiltersContainer.visibility = View.GONE
                    btnLoad.visibility = View.GONE
                }
            }.show(supportFragmentManager, "FeesFilter")
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterStudents(s.toString().trim())
            }
        })

        btnLoad.setOnClickListener {
            if (selectedCourseId.isNullOrEmpty() ||
                selectedYear.isNullOrEmpty() ||
                selectedSem.isNullOrEmpty()) {
                Toast.makeText(this,
                    "Please select course, year and semester",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // ✅ First load semester fees amount, then load students
            loadSemesterFeesThenStudents()
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Step 1: Load semesterFees from course
    // ─────────────────────────────────────────────
    private fun loadSemesterFeesThenStudents() {
        showLoading(true)

        db.collection("courses")
            .document(selectedCourseId!!)
            .get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val rawFees = doc.get("semesterFees") as? Map<String, Any> ?: emptyMap()
                semesterFeesMap = rawFees.mapValues { entry ->
                    when (val v = entry.value) {
                        is Long   -> v
                        is Number -> v.toLong()
                        else      -> 0L
                    }
                }
                // ✅ Now load students
                loadStudentFees()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "❌ Failed to load fees: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    // ─────────────────────────────────────────────
// ✅ Filter students by name or roll number
// ─────────────────────────────────────────────
    private fun filterStudents(query: String) {
        if (allStudents.isEmpty()) return

        val filtered = if (query.isEmpty()) {
            allStudents
        } else {
            allStudents.filter { student ->
                student.name.contains(query, ignoreCase = true) ||
                        student.rollNo.contains(query, ignoreCase = true)
            }
        }

        adapter.updateList(filtered)
        tvStudentCount.text = "Showing ${filtered.size} students"

        // ✅ Show empty if no results
        if (filtered.isEmpty()) {
            rvStudents.visibility   = View.GONE
            layoutEmpty.visibility  = View.VISIBLE
        } else {
            rvStudents.visibility   = View.VISIBLE
            layoutEmpty.visibility  = View.GONE
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Step 2: Load students + their fees
    // ─────────────────────────────────────────────
    private fun loadStudentFees() {
        val courseId = selectedCourseId ?: return
        val year     = selectedYear ?: return
        val sem      = selectedSem ?: return

        // Total fees for this sem from course config
        val totalFees = semesterFeesMap[sem] ?: 0L

        db.collection("students")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("year", year)
            .whereEqualTo("semester", sem)
            .get()
            .addOnSuccessListener { studentsSnap ->
                showLoading(false)

                if (studentsSnap.isEmpty) {
                    showState(State.EMPTY)
                    return@addOnSuccessListener
                }

                val studentList = studentsSnap.documents.map { doc ->
                    StudentFeesAdapter.StudentFeeItem(
                        studentId   = doc.id,
                        name        = doc.getString("fullName") ?: "Unknown",
                        rollNo      = doc.getString("rollNo") ?: "-",
                        totalAmount = totalFees,
                        paidAmount  = 0L,  // will be updated below
                        status      = "pending"
                    )
                }.toMutableList()

                // ✅ Now load each student's fees document
                loadFeesForStudents(studentList, sem)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
                showState(State.EMPTY)
            }
    }

    // ─────────────────────────────────────────────
    // ✅ Step 3: Load fees docs for all students
    // ─────────────────────────────────────────────
    private fun loadFeesForStudents(
        students: MutableList<StudentFeesAdapter.StudentFeeItem>,
        sem: String
    ) {
        if (students.isEmpty()) {
            showState(State.EMPTY)
            return
        }

        var completed = 0
        val total = students.size

        students.forEachIndexed { index, student ->
            db.collection("fees")
                .document(student.studentId)
                .get()
                .addOnSuccessListener { feeDoc ->
                    if (feeDoc.exists()) {
                        // ✅ Read semesters array
                        @Suppress("UNCHECKED_CAST")
                        val sems = feeDoc.get("semesters")
                                as? List<Map<String, Any>> ?: emptyList()

                        // Find matching sem
                        val semData = sems.find { map ->
                            (map["semNumber"] as? Long)?.toString() == sem
                        }

                        if (semData != null) {
                            val paid   = semData["paidAmount"] as? Long ?: 0L
                            val status = semData["status"] as? String ?: "pending"
                            students[index] = student.copy(
                                paidAmount = paid,
                                status     = status
                            )
                        }
                    }

                    completed++
                    if (completed >= total) {
                        // ✅ All loaded → update UI
                        runOnUiThread { updateUI(students) }
                    }
                }
                .addOnFailureListener {
                    completed++
                    if (completed >= total) {
                        runOnUiThread { updateUI(students) }
                    }
                }
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Update RecyclerView + Stats
    // ─────────────────────────────────────────────
    private fun updateUI(students: List<StudentFeesAdapter.StudentFeeItem>) {
        if (students.isEmpty()) {
            showState(State.EMPTY)
            return
        }

        // ✅ Save full list for search
        allStudents = students

        // Stats
        tvTotalStudents.text = students.size.toString()
        tvTotalPaid.text     = students.count { it.status == "paid" }.toString()
        tvTotalPending.text  = students.count { it.status != "paid" }.toString()
        tvStudentCount.text  = "Showing ${students.size} students"

        adapter.updateList(students)
        showState(State.LIST)

        // ✅ Re-apply search if already typed
        val currentQuery = etSearch.text.toString().trim()
        if (currentQuery.isNotEmpty()) filterStudents(currentQuery)
    }


    // ─────────────────────────────────────────────
    // States
    // ─────────────────────────────────────────────
    enum class State { DEFAULT, LOADING, LIST, EMPTY }

    private fun showState(state: State) {
        layoutDefault.visibility  = View.GONE
        layoutEmpty.visibility    = View.GONE
        rvStudents.visibility     = View.GONE
        cardStats.visibility      = View.GONE
        tvStudentCount.visibility = View.GONE

        when (state) {
            State.DEFAULT -> layoutDefault.visibility  = View.VISIBLE
            State.EMPTY   -> layoutEmpty.visibility    = View.VISIBLE
            State.LIST    -> {
                rvStudents.visibility     = View.VISIBLE
                cardStats.visibility      = View.VISIBLE
                tvStudentCount.visibility = View.VISIBLE
            }
            State.LOADING -> { /* handled by progressOverlay */ }
        }
    }

    private fun showLoading(show: Boolean) {
        progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
        if (show) showState(State.LOADING)
    }
}
