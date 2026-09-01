package com.example.collagemanagmentsystem

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collagemanagmentsystem.R
import com.example.collagemanagmentsystem.adapters.StudentAssignmentAdapter
import com.example.collagemanagmentsystem.models.AssignmentModel
import com.example.collagemanagmentsystem.utils.CoreBaseActivity
import com.example.collagemanagmentsystem.utils.SessionManager
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class StudentAssignmentListActivity : CoreBaseActivity() {

    // ── Views ─────────────────────────────────
    private lateinit var btnBack: ImageView
    private lateinit var etSearch: EditText
    private lateinit var tvAssignmentCount: TextView
    private lateinit var rvAssignments: RecyclerView
    private lateinit var layoutEmpty: LinearLayout

    // ── Filter Chips ──────────────────────────
    private lateinit var chipAll: Chip
    private lateinit var chipPending: Chip
    private lateinit var chipOverdue: Chip

    // ── Firebase + Session ────────────────────
    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    // ── Adapter + Data ────────────────────────
    private lateinit var adapter: StudentAssignmentAdapter
    private val allAssignments = mutableListOf<AssignmentModel>()

    // ── Active Filter ─────────────────────────
    private var activeFilter = "all" // "all" | "pending" | "overdue"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_assignment_list)

        session = SessionManager(this)

        bindViews()
        setupRecyclerView()
        setupClickListeners()
        setupSearch()
        setupChips()
        loadAssignments()
    }

    private fun bindViews() {
        btnBack            = findViewById(R.id.btnBack)
        etSearch           = findViewById(R.id.etSearch)
        tvAssignmentCount  = findViewById(R.id.tvAssignmentCount)
        rvAssignments      = findViewById(R.id.rvAssignments)
        layoutEmpty        = findViewById(R.id.layoutEmpty)
        chipAll            = findViewById(R.id.chipAll)
        chipPending        = findViewById(R.id.chipPending)
        chipOverdue        = findViewById(R.id.chipOverdue)
    }

    private fun setupRecyclerView() {
        adapter = StudentAssignmentAdapter(
            assignments = mutableListOf(),
            onCardClick = { assignment -> openDetail(assignment) }
        )
        rvAssignments.layoutManager = LinearLayoutManager(this)
        rvAssignments.adapter = adapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
    }

    // ─────────────────────────────────────────────
    // ✅ Search
    // ─────────────────────────────────────────────
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilterAndSearch()
            }
        })
    }

    // ─────────────────────────────────────────────
    // ✅ Chips
    // ─────────────────────────────────────────────
    private fun setupChips() {
        chipAll.setOnClickListener {
            activeFilter = "all"
            applyFilterAndSearch()
        }
        chipPending.setOnClickListener {
            activeFilter = "pending"
            applyFilterAndSearch()
        }
        chipOverdue.setOnClickListener {
            activeFilter = "overdue"
            applyFilterAndSearch()
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Load Assignments
    // Firestore: courseId + semester = currentSem
    // ─────────────────────────────────────────────
    private fun loadAssignments() {
        val courseId = session.getCourseId()
        val semNum   = session.getSemester() // already int string e.g. "3"

        if (courseId.isEmpty()) {
            showEmptyState()
            return
        }

        showLoadingState()

        val query = db.collection("assignments")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("semester", semNum)

        // ✅ Cache first → then server
        query.get(Source.CACHE)
            .addOnSuccessListener { cacheSnap ->
                if (!cacheSnap.isEmpty) processAssignments(cacheSnap.documents
                    .mapNotNull { doc ->
                        doc.toObject(AssignmentModel::class.java)
                            ?.also { it.id = doc.id }
                    })

                // ✅ Always refresh from server
                query.get(Source.SERVER)
                    .addOnSuccessListener { serverSnap ->
                        processAssignments(serverSnap.documents
                            .mapNotNull { doc ->
                                doc.toObject(AssignmentModel::class.java)
                                    ?.also { it.id = doc.id }
                            })
                    }
                    .addOnFailureListener {
                        if (allAssignments.isEmpty()) showEmptyState()
                    }
            }
            .addOnFailureListener {
                // ✅ Fallback to server directly
                query.get(Source.SERVER)
                    .addOnSuccessListener { serverSnap ->
                        processAssignments(serverSnap.documents
                            .mapNotNull { doc ->
                                doc.toObject(AssignmentModel::class.java)
                                    ?.also { it.id = doc.id }
                            })
                    }
                    .addOnFailureListener { showEmptyState() }
            }
    }

    private fun processAssignments(newList: List<AssignmentModel>) {
        allAssignments.clear()
        allAssignments.addAll(newList)
        applyFilterAndSearch()
    }

    // ─────────────────────────────────────────────
    // ✅ Filter + Search combined
    // ─────────────────────────────────────────────
    private fun applyFilterAndSearch() {
        val now      = System.currentTimeMillis()
        val query    = etSearch.text.toString().trim()

        // Step 1: apply chip filter
        var filtered = when (activeFilter) {
            "pending" -> allAssignments.filter {
                it.dueDate > now  // ✅ Due in future
            }
            "overdue" -> allAssignments.filter {
                it.dueDate in 1 until now  // ✅ Past due date
            }
            else -> allAssignments.toList() // all
        }

        // Step 2: apply search
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(query, true) ||
                        it.subject.contains(query, true) ||
                        it.description.contains(query, true)
            }
        }

        adapter.updateList(filtered)
        updateUI(filtered.size)
    }

    // ─────────────────────────────────────────────
    // ✅ Open Detail
    // ─────────────────────────────────────────────
    private fun openDetail(assignment: AssignmentModel) {
        startActivity(
            Intent(this, StudentAssignmentDetailActivity::class.java).apply {
                putExtra("ASSIGNMENT_ID", assignment.id)
            }
        )
    }

    // ─────────────────────────────────────────────
    // UI State helpers
    // ─────────────────────────────────────────────
    private fun showLoadingState() {
        tvAssignmentCount.text   = "Loading..."
        rvAssignments.visibility = View.GONE
        layoutEmpty.visibility   = View.GONE
    }

    private fun showEmptyState() {
        tvAssignmentCount.text   = "0 assignments"
        rvAssignments.visibility = View.GONE
        layoutEmpty.visibility   = View.VISIBLE
    }

    private fun updateUI(count: Int) {
        tvAssignmentCount.text =
            "$count assignment${if (count == 1) "" else "s"}"

        if (count == 0) {
            rvAssignments.visibility = View.GONE
            layoutEmpty.visibility   = View.VISIBLE
        } else {
            rvAssignments.visibility = View.VISIBLE
            layoutEmpty.visibility   = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        loadAssignments()
    }
}
