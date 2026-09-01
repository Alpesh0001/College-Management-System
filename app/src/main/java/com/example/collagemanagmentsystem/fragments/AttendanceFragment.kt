package com.example.collagemanagmentsystem.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collagemanagmentsystem.R
import com.example.collagemanagmentsystem.adapters.SubjectAttendanceAdapter
import com.example.collagemanagmentsystem.models.StudentSubjectAttendance
import com.example.collagemanagmentsystem.utils.SessionManager
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class AttendanceFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    // UI
    private lateinit var semesterDropdown: MaterialAutoCompleteTextView
    private lateinit var overallProgress: CircularProgressIndicator
    private lateinit var tvOverallPercent: TextView
    private lateinit var tvLecturesCount: TextView
    private lateinit var statusBadge: View
    private lateinit var tvStatusText: TextView
    private lateinit var rvSubjectAttendance: RecyclerView
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyText: TextView

    private lateinit var adapter: SubjectAttendanceAdapter

    // Data
    private var availableSemesters: List<Int> = emptyList()
    private var currentSem: Int = 1
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        bindViews(view)
        setupRecycler()
        setupSemesterDropdown()
        loadCourseSemesters()
    }

    private fun bindViews(view: View) {
        semesterDropdown    = view.findViewById(R.id.semesterDropdown)
        overallProgress     = view.findViewById(R.id.overallProgress)
        tvOverallPercent    = view.findViewById(R.id.tvOverallPercent)
        tvLecturesCount     = view.findViewById(R.id.lecturesCount)
        statusBadge         = view.findViewById(R.id.statusBadge)
        tvStatusText        = view.findViewById(R.id.tvStatusText)
        rvSubjectAttendance = view.findViewById(R.id.rvSubjectAttendance)
        layoutLoading       = view.findViewById(R.id.layoutLoading)
        layoutEmpty         = view.findViewById(R.id.layoutEmpty)
        tvEmptyText         = view.findViewById(R.id.tvEmptyText)
    }


    private fun setupRecycler() {
        rvSubjectAttendance.layoutManager = LinearLayoutManager(requireContext())
        adapter = SubjectAttendanceAdapter(mutableListOf())
        rvSubjectAttendance.adapter = adapter
    }

    private fun setupSemesterDropdown() {
        val semFromSession = session.getSemester().toIntOrNull() ?: 1
        currentSem = semFromSession

        semesterDropdown.setOnItemClickListener { _, _, position, _ ->
            val sem = availableSemesters.getOrNull(position) ?: return@setOnItemClickListener
            if (sem != currentSem) {
                currentSem = sem
                loadAttendanceForCurrentSem()
            }
        }
    }

    private fun loadCourseSemesters() {
        showLoading(true)

        val courseId = session.getCourseId()  // "bca"
        if (courseId.isEmpty()) {
            showLoading(false)
            showEmpty("Course not found.")
            return
        }

        db.collection("courses")
            .whereEqualTo("courseKey", courseId)  // ✅ Query courseKey instead of doc ID
            .limit(1)
            .get(Source.CACHE)
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val doc = snap.documents[0]
                    val years = (doc.getLong("durationYears") ?: 3L).toInt()
                    val totalSem = years * 2
                    availableSemesters = (1..totalSem).toList()
                    setupSemesterAdapter()
                    loadAttendanceForCurrentSem()
                } else {
                    // Try server
                    db.collection("courses")
                        .whereEqualTo("courseKey", courseId)
                        .limit(1)
                        .get(Source.SERVER)
                        .addOnSuccessListener { serverSnap ->
                            if (!serverSnap.isEmpty) {
                                val doc = serverSnap.documents[0]
                                val years = (doc.getLong("durationYears") ?: 3L).toInt()
                                val totalSem = years * 2
                                availableSemesters = (1..totalSem).toList()
                                setupSemesterAdapter()
                                loadAttendanceForCurrentSem()
                            } else {
                                showLoading(false)
                                showEmpty("Course info missing.")
                            }
                        }
                        .addOnFailureListener {
                            showLoading(false)
                            showEmpty("Failed to load course info.")
                        }
                }
            }
            .addOnFailureListener {
                showLoading(false)
                showEmpty("Failed to load course info.")
            }
    }


    private fun setupSemesterAdapter() {
        if (availableSemesters.isEmpty()) return

        val labels = availableSemesters.map { "Semester $it" }
        val adapterSem = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            labels
        )
        semesterDropdown.setAdapter(adapterSem)

        // Select current sem if exists, else first
        val index = availableSemesters.indexOf(currentSem).takeIf { it >= 0 } ?: 0
        semesterDropdown.setText(labels[index], false)
        currentSem = availableSemesters[index]
    }


    private fun loadAttendanceForCurrentSem() {
        if (isLoading) return
        isLoading = true
        showLoading(true)

        val studentId = session.getStudentId()
        Log.d("Attendance", "studentId: '$studentId'")

        if (studentId.isEmpty()) {
            Log.e("Attendance", "❌ studentId empty")
            showLoading(false)
            showEmpty("Session expired. Please login again.")
            isLoading = false
            return
        }

        val docId = "${studentId}_${currentSem}"
        Log.d("Attendance", "docId: '$docId', currentSem: $currentSem")

        // ✅ DIRECT SERVER — skip broken cache!
        db.collection("studentSummary")
            .document(docId)
            .get()
            .addOnSuccessListener { doc ->
                Log.d("Attendance", "✅ Server result: exists=${doc.exists()}")
                if (doc.exists()) {
                    Log.d("Attendance", "✅ Found data - binding")
                    bindAttendanceDoc(doc)
                } else {
                    Log.e("Attendance", "❌ Document doesn't exist")
                    showNoAttendance()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Attendance", "❌ Server FAIL", e)
                showLoading(false)
                showEmpty("Failed to load attendance.")
                isLoading = false
            }
    }


    private fun bindAttendanceDoc(doc: com.google.firebase.firestore.DocumentSnapshot) {
        Log.d("Attendance", "🔥 bindAttendanceDoc called")
        isLoading = false
        showLoading(false)

        val totalLectures = (doc.getLong("totalLectures") ?: 0L).toInt()
        val totalPresent = (doc.getLong("totalPresent") ?: 0L).toInt()
        val totalAbsent = (doc.getLong("totalAbsent") ?: 0L).toInt()

        Log.d("Attendance", "Totals: L=$totalLectures, P=$totalPresent, A=$totalAbsent")

        val overallPercent = if (totalLectures > 0) (totalPresent * 100) / totalLectures else 0
        Log.d("Attendance", "Overall %: $overallPercent")

        overallProgress.max = 100
        overallProgress.progress = overallPercent

        // Make sure you give id to center percent TextView in XML
        // then this will work:
        val tvPercent = requireView().findViewById<TextView>(R.id.tvOverallPercent)
        tvPercent.text = "$overallPercent%"

        tvLecturesCount.text = "Present: $totalPresent / $totalLectures lectures"

        // Status Badge
        val statusTextView = requireView().findViewById<TextView>(R.id.tvStatusText)
        val badgeCard = requireView().findViewById<com.google.android.material.card.MaterialCardView>(
            R.id.statusBadge
        )

        val (statusText, colorRes, bgColor, strokeColor) = when {
            overallPercent >= 75 -> Quad(
                "GOOD STANDING",
                R.color.green,
                0xFFE8F5E9.toInt(),
                R.color.green
            )
            overallPercent >= 60 -> Quad(
                "AT RISK",
                R.color.orange,
                0xFFFFF3E0.toInt(),
                R.color.orange
            )
            else -> Quad(
                "SHORTAGE",
                R.color.red,
                0xFFFFEBEE.toInt(),
                R.color.red
            )
        }

        val color = ContextCompat.getColor(requireContext(), colorRes)
        statusTextView.text = statusText
        statusTextView.setTextColor(color)
        badgeCard.setCardBackgroundColor(bgColor)
        badgeCard.strokeColor = ContextCompat.getColor(requireContext(), strokeColor)

        // Subject-wise list
        val subjectsRaw = (doc.get("subjects") as? List<*>)
            ?.filterIsInstance<Map<String, Any>>()
            ?: emptyList()

        val subjectList = subjectsRaw.map { map ->
            StudentSubjectAttendance(
                subjectName = map["subjectName"] as? String ?: "",
                subjectCode = map["subjectCode"] as? String ?: "",
                present = (map["present"] as? Long)?.toInt() ?: 0,
                absent = (map["absent"] as? Long)?.toInt() ?: 0,
                total = (map["total"] as? Long)?.toInt() ?: 0
            )
        }.sortedBy { it.subjectName }

        if (subjectList.isEmpty()) {
            showEmpty("No subject-wise data found.")
        } else {
            layoutEmpty.visibility = View.GONE
            rvSubjectAttendance.visibility = View.VISIBLE
            adapter.updateData(subjectList)
        }
    }

    private fun showNoAttendance() {
        isLoading = false
        showLoading(false)
        overallProgress.progress = 0
        requireView().findViewById<TextView>(R.id.tvOverallPercent).text = "0%"
        tvLecturesCount.text = "No attendance data yet."
        adapter.updateData(emptyList())
        showEmpty("No attendance data for this semester.")
    }

    private fun showLoading(show: Boolean) {
        // You need to add a simple loading layout in XML, or change this to use a ProgressBar
        layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        rvSubjectAttendance.visibility = if (show) View.GONE else View.VISIBLE
        if (show) layoutEmpty.visibility = View.GONE
    }

    private fun showEmpty(message: String) {
        layoutEmpty.visibility = View.VISIBLE
        rvSubjectAttendance.visibility = View.GONE
        tvEmptyText.text = message
    }

    // Helper data holder
    private data class Quad(
        val text: String,
        val colorRes: Int,
        val bgColorInt: Int,
        val strokeColorRes: Int
    )
}
