package com.example.collegemanagementsystemfaculty

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.adapters.DivisionAdapter
import com.example.collegemanagementsystemfaculty.models.Division
import com.example.collegemanagementsystemfaculty.utils.CoreBaseActivity
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore

class TimeTableActivity : CoreBaseActivity() {  // ✅ Changed to CoreBaseActivity

    private val db = FirebaseFirestore.getInstance()

    private lateinit var recyclerDivisions: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvDivisionCount: TextView
    private lateinit var emptyState: LinearLayout
    private lateinit var btnBack: View
    private lateinit var session: SessionManager
    private lateinit var divisionAdapter: DivisionAdapter
    private val allDivisions = mutableListOf<Division>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timetable)

        session = SessionManager(this)

        bindViews()
        setupRecyclerView()
        setupSearch()
        loadDivisionsFromFirestore()

        btnBack.setOnClickListener { finish() }
    }

    private fun bindViews() {
        recyclerDivisions = findViewById(R.id.recyclerDivisions)
        etSearch          = findViewById(R.id.etSearch)
        tvDivisionCount   = findViewById(R.id.tvDivisionCount)
        emptyState        = findViewById(R.id.emptyState)
        btnBack           = findViewById(R.id.btnBack)
        // ✅ No progressBar here — using CoreBaseActivity loader instead
    }

    private fun setupRecyclerView() {
        recyclerDivisions.layoutManager = LinearLayoutManager(this)
        divisionAdapter = DivisionAdapter(
            divisions     = mutableListOf(),
            onViewClick   = { division ->
                val intent = Intent(this, DivisionTimeTableActivity::class.java).apply {
                    putExtra("division_id",   division.id)
                    putExtra("division_name", division.divisionName)
                    putExtra("course_name",   division.courseName)
                    putExtra("year",          division.year)
                    putExtra("semester",      division.semester)
                    putExtra("mode",          "view")
                }
                startActivity(intent)
            },
            onCreateClick = { division ->
                val intent = Intent(this, DivisionTimeTableActivity::class.java).apply {
                    putExtra("division_id",   division.id)
                    putExtra("division_name", division.divisionName)
                    putExtra("course_name",   division.courseName)
                    putExtra("year",          division.year)
                    putExtra("semester",      division.semester)
                    putExtra("mode", if (division.hasTimetable) "edit" else "create")
                }
                startActivityForResult(intent, REQUEST_TIMETABLE)
            },
            onDeleteClick = { division ->
                confirmDeleteTimetable(division)
            }
        )
        recyclerDivisions.adapter = divisionAdapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                divisionAdapter.filter(s.toString())
                updateEmptyState()
            }
        })
    }

    private fun loadDivisionsFromFirestore() {

        val hodCourseId = session.getCourseId()

        if (hodCourseId.isEmpty()) {
            Toast.makeText(this, "❌ Course not assigned!", Toast.LENGTH_LONG).show()
            updateEmptyState()
            return
        }

        showBlockingLoader("Loading divisions...")

        db.collection("divisions")
            .whereEqualTo("status", "Active")
            .whereEqualTo("courseId", hodCourseId)
            .addSnapshotListener { snapshot, error ->

                hideBlockingLoader()

                if (error != null) {
                    Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_LONG).show()
                    updateEmptyState()
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    updateEmptyState()
                    return@addSnapshotListener
                }

                allDivisions.clear()

                for (doc in snapshot.documents) {
                    val division = Division(
                        id               = doc.id,
                        divisionName     = doc.getString("divisionName") ?: "",
                        courseId         = doc.getString("courseId") ?: "",
                        courseName       = doc.getString("courseName") ?: "",
                        courseCode       = doc.getString("courseCode") ?: "",
                        year             = doc.getString("year") ?: "",
                        semester         = doc.getString("semester") ?: "",
                        capacity         = (doc.getLong("capacity") ?: 0).toInt(),
                        currentStrength  = (doc.getLong("currentStrength") ?: 0).toInt(),
                        classTeacherId   = doc.getString("classTeacherId"),
                        classTeacherName = doc.getString("classTeacherName"),
                        status           = doc.getString("status") ?: "Active",
                        hasTimetable     = doc.getBoolean("hasTimetable") ?: false
                    )

                    allDivisions.add(division)
                }

                allDivisions.sortBy { it.divisionName }
                divisionAdapter.updateList(allDivisions)
                tvDivisionCount.text = "Total: ${allDivisions.size} divisions"

                updateEmptyState()
            }
    }

    private fun confirmDeleteTimetable(division: Division) {
        AlertDialog.Builder(this)
            .setTitle("Delete Time Table")
            .setMessage("Delete time table for ${division.divisionName}?\n\nThis cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTimetable(division)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTimetable(division: Division) {

        // ✅ Show CoreBaseActivity loader
        showBlockingLoader("Deleting timetable...")

        db.collection("divisions").document(division.id)
            .collection("timetable")
            .get()
            .addOnSuccessListener { slots ->
                val batch = db.batch()

                slots.documents.forEach { slot ->
                    batch.delete(slot.reference)
                }

                batch.update(
                    db.collection("divisions").document(division.id),
                    "hasTimetable", false
                )

                batch.commit()
                    .addOnSuccessListener {
                        // ✅ Hide loader
                        hideBlockingLoader()
                        Toast.makeText(this, "✅ Timetable deleted!", Toast.LENGTH_SHORT).show()
                        loadDivisionsFromFirestore()
                    }
                    .addOnFailureListener { e ->
                        // ✅ Hide loader on failure
                        hideBlockingLoader()
                        Toast.makeText(this, "❌ Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                hideBlockingLoader()
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_TIMETABLE) {
            loadDivisionsFromFirestore()
        }
    }

    private fun updateEmptyState() {

        val count = divisionAdapter.itemCount

        if (count == 0) {
            recyclerDivisions.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recyclerDivisions.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    companion object {
        private const val REQUEST_TIMETABLE = 401
    }
    override fun onDestroy() {
        hideBlockingLoader()
        super.onDestroy()
    }
}
