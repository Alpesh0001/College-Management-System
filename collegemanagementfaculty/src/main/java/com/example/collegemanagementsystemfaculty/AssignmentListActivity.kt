package com.example.collegemanagementsystemfaculty

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.adapters.AssignmentAdapter
import com.example.collegemanagementsystemfaculty.models.AssignmentModel
import com.example.collegemanagementsystemfaculty.utils.CoreBaseActivity
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source

class AssignmentListActivity : CoreBaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etSearch: EditText
    private lateinit var tvAssignmentCount: TextView
    private lateinit var rvAssignments: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fabAddAssignment: ExtendedFloatingActionButton

    private lateinit var session: SessionManager
    private val db = FirebaseFirestore.getInstance()

    private var adapter: AssignmentAdapter? = null
    private val allAssignments = mutableListOf<AssignmentModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignment_list)

        session = SessionManager(this)

        bindViews()
        setupRecyclerView()
        setupClickListeners()
        setupSearch()

        loadAssignments()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        etSearch = findViewById(R.id.etSearch)
        tvAssignmentCount = findViewById(R.id.tvAssignmentCount)
        rvAssignments = findViewById(R.id.rvAssignments)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        fabAddAssignment = findViewById(R.id.fabAddAssignment)
    }

    private fun setupRecyclerView() {
        rvAssignments.layoutManager = LinearLayoutManager(this)

        adapter = AssignmentAdapter(
            assignments = mutableListOf(),
            isEditable = true,
            onCardClick = { openAssignmentDetail(it) },
            onEditClick = { editAssignment(it) },
            onDeleteClick = { a, pos -> deleteAssignment(a, pos) }
        )

        rvAssignments.adapter = adapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        fabAddAssignment.setOnClickListener { openCreateAssignment() }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterAssignments(s.toString())
            }
        })
    }

    // ==========================================================
    // 🔥 MAIN LOADING FUNCTION (COURSE + FACULTY FILTER)
    // ==========================================================

    private fun loadAssignments() {

        val facultyId = session.getFacultyId()
        val courseId = session.getCourseId()

        if (facultyId.isEmpty() || courseId.isEmpty()) {
            showEmptyState()
            return
        }

        showLoadingState()

        val query = db.collection("assignments")
            .whereEqualTo("createdBy", facultyId)
            .whereEqualTo("courseId", courseId)

        // ⚡ CACHE FIRST
        query.get(Source.CACHE)
            .addOnSuccessListener { cacheSnap ->
                if (!cacheSnap.isEmpty) processAssignments(cacheSnap)

                // 🔥 SERVER NEXT
                query.get(Source.SERVER)
                    .addOnSuccessListener { serverSnap ->
                        processAssignments(serverSnap)
                    }
                    .addOnFailureListener {
                        if (allAssignments.isEmpty()) showEmptyState()
                    }
            }
            .addOnFailureListener {
                showEmptyState()
            }
    }

    private fun processAssignments(snapshot: QuerySnapshot) {

        val newList = mutableListOf<AssignmentModel>()

        for (doc in snapshot.documents) {
            val assignment = doc.toObject(AssignmentModel::class.java)
            assignment?.let {
                it.id = doc.id
                newList.add(it)
            }
        }

        Log.d("ASSIGNMENT", "Total loaded = ${newList.size}")

        allAssignments.clear()
        allAssignments.addAll(newList)

        adapter?.updateList(newList)

        updateUI()
    }

    // ==========================================================

    private fun showLoadingState() {
        tvAssignmentCount.text = "Loading..."
        rvAssignments.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        tvAssignmentCount.text = "0 assignments"
        rvAssignments.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
    }

    private fun updateUI() {
        val count = adapter?.itemCount ?: 0

        tvAssignmentCount.text =
            "$count assignment${if (count == 1) "" else "s"}"

        if (count == 0) {
            rvAssignments.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvAssignments.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun filterAssignments(query: String) {

        val filtered = if (query.isEmpty()) {
            allAssignments
        } else {
            allAssignments.filter {
                it.title.contains(query, true) ||
                        it.subject.contains(query, true) ||
                        it.description.contains(query, true)
            }
        }

        adapter?.updateList(filtered)
        updateUI()
    }

    // ==========================================================
    // ACTIONS
    // ==========================================================

    private fun openCreateAssignment() {
        startActivity(Intent(this, CreateAssignmentActivity::class.java).apply {
            putExtra("MODE", CreateAssignmentActivity.MODE_ADD)
        })
    }

    private fun editAssignment(a: AssignmentModel) {
        startActivity(Intent(this, CreateAssignmentActivity::class.java).apply {
            putExtra("MODE", CreateAssignmentActivity.MODE_EDIT)
            putExtra("ASSIGNMENT_ID", a.id)
        })
    }

    private fun openAssignmentDetail(a: AssignmentModel) {
        startActivity(Intent(this, CreateAssignmentActivity::class.java).apply {
            putExtra("MODE", CreateAssignmentActivity.MODE_VIEW)
            putExtra("ASSIGNMENT_ID", a.id)
        })
    }

    private fun deleteAssignment(a: AssignmentModel, position: Int) {

        AlertDialog.Builder(this)
            .setTitle("Delete Assignment")
            .setMessage("Delete \"${a.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("assignments")
                    .document(a.id)
                    .delete()
                    .addOnSuccessListener {
                        adapter?.removeItem(position)
                        Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadAssignments()
    }
}