package com.example.collegemanagementsystemfaculty

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.adapters.DivisionStudentAdapter
import com.example.collegemanagementsystemfaculty.models.Division
import com.example.collegemanagementsystemfaculty.utils.CoreBaseActivity
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore

class ViewStudentsActivity : CoreBaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    private lateinit var recyclerDivisions: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnBack: View

    private lateinit var adapter: DivisionStudentAdapter
    private val divisions = mutableListOf<Division>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_students)

        session = SessionManager(this)

        recyclerDivisions = findViewById(R.id.recyclerDivisions)
        layoutEmpty       = findViewById(R.id.layoutEmpty)
        btnBack           = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        setupRecycler()
        loadDivisions()
    }

    private fun setupRecycler() {
        recyclerDivisions.layoutManager = LinearLayoutManager(this)

        adapter = DivisionStudentAdapter(divisions) { division ->
            val intent = Intent(this, StudentListActivity::class.java).apply {
                putExtra("division_id", division.id)
                putExtra("division_name", division.divisionName)
            }
            startActivity(intent)
        }

        recyclerDivisions.adapter = adapter
    }

    // 🔥 MAIN LOGIC
    private fun loadDivisions() {

        showBlockingLoader("Loading classes...")

        val query = if (session.isHOD()) {
            // ✅ HOD → All divisions of course
            db.collection("divisions")
                .whereEqualTo("courseId", session.getCourseId())
                .whereEqualTo("status", "Active")
        } else {
            // ✅ Faculty → Only assigned class
            db.collection("divisions")
                .whereEqualTo("classTeacherId", session.getFacultyId())
                .whereEqualTo("status", "Active")
        }

        query.get()
            .addOnSuccessListener { snapshot ->

                hideBlockingLoader()

                divisions.clear()

                snapshot.documents.forEach { doc ->
                    val division = Division(
                        id           = doc.id,
                        divisionName = doc.getString("divisionName") ?: "",
                        courseId     = doc.getString("courseId") ?: "",
                        courseName   = doc.getString("courseName") ?: "",
                        courseCode   = doc.getString("courseCode") ?: "",
                        year         = doc.getString("year") ?: "",
                        semester     = doc.getString("semester") ?: "",
                        capacity     = (doc.getLong("capacity") ?: 0).toInt(),
                        currentStrength = (doc.getLong("currentStrength") ?: 0).toInt(),
                        classTeacherId   = doc.getString("classTeacherId"),
                        classTeacherName = doc.getString("classTeacherName"),
                        status = doc.getString("status") ?: "Active",
                        hasTimetable = false
                    )
                    divisions.add(division)
                }

                divisions.sortBy { it.divisionName }
                adapter.notifyDataSetChanged()

                updateUI()
            }
            .addOnFailureListener {
                hideBlockingLoader()
                Toast.makeText(this, "Failed to load classes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI() {
        if (divisions.isEmpty()) {
            recyclerDivisions.visibility = View.GONE
            layoutEmpty.visibility       = View.VISIBLE
        } else {
            recyclerDivisions.visibility = View.VISIBLE
            layoutEmpty.visibility       = View.GONE
        }
    }
}