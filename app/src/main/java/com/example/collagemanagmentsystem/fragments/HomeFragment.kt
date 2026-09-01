package com.example.collagemanagmentsystem.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.collagemanagmentsystem.FeesActivity
import com.example.collagemanagmentsystem.FeesReceiptActivity
import com.example.collagemanagmentsystem.MaterialsListActivity
import com.example.collagemanagmentsystem.R
import com.example.collagemanagmentsystem.StudentAssignmentListActivity
import com.example.collagemanagmentsystem.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var session: SessionManager

    // Header Views
    private lateinit var tvGreeting: TextView
    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentRoll: TextView
    private lateinit var tvDate: TextView

    // Stat Views
    private lateinit var tvAttendanceStat: TextView
    private lateinit var tvLecturesStat: TextView
    private lateinit var tvAssignmentsStat: TextView
    private lateinit var tvFeesStat: TextView

    // Stat Cards (clickable)
    private lateinit var cardAttendanceStat: MaterialCardView
    private lateinit var cardFeesStat: MaterialCardView

    // Portal Cards
    private lateinit var cardTimetable: MaterialCardView
    private lateinit var cardExams: MaterialCardView
    private lateinit var cardFees: MaterialCardView
    private lateinit var cardAttendanceDetails: MaterialCardView
    private lateinit var cardMaterials: MaterialCardView
    private lateinit var cardAssignments: MaterialCardView

    // ✅ Current Sem Fees Card
    private lateinit var cardCurrentSemFees: MaterialCardView
    private lateinit var tvCurrentSemLabel: TextView
    private lateinit var tvCurrentSemBadge: TextView
    private lateinit var tvCurrentPaid: TextView
    private lateinit var tvCurrentTotal: TextView
    private lateinit var tvCurrentRemaining: TextView

    // ✅ Current sem fees state
    private var currentSemTotal    = 0L
    private var currentSemPaid     = 0L
    private var currentSemStatus   = "pending"
    private var currentSemPaidDate = 0L
    private var currentSemNumber   = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        session   = SessionManager(requireContext())

        initViews(view)
        setupStaticUI()
        loadAttendance()
        loadTodayLectures()
        loadFeesStatus()
        loadCurrentSemFees()   // ✅ Load fees card
        loadAssignments()
        setupClickListeners()
    }

    // ✅ Refresh fees when returning from FeesActivity
    override fun onResume() {
        super.onResume()
        loadFeesStatus()
        loadCurrentSemFees()
    }

    private fun initViews(view: View) {
        // Header
        tvGreeting    = view.findViewById(R.id.tvGreeting)
        tvStudentName = view.findViewById(R.id.tvStudentName)
        tvStudentRoll = view.findViewById(R.id.tvStudentRoll)
        tvDate        = view.findViewById(R.id.tvDate)

        // Stats
        tvAttendanceStat  = view.findViewById(R.id.tvAttendanceStat)
        tvLecturesStat    = view.findViewById(R.id.tvLecturesStat)
        tvAssignmentsStat = view.findViewById(R.id.tvAssignmentsStat)
        tvFeesStat        = view.findViewById(R.id.tvFeesStat)

        // Portal cards
        cardTimetable         = view.findViewById(R.id.cardTimetable)
        cardExams             = view.findViewById(R.id.cardExams)
        cardFees              = view.findViewById(R.id.cardFees)
        cardAttendanceDetails = view.findViewById(R.id.cardAttendanceDetails)
        cardMaterials         = view.findViewById(R.id.cardMaterials)
        cardAssignments       = view.findViewById(R.id.cardAssignments)

        // ✅ Current sem fees card
        cardCurrentSemFees = view.findViewById(R.id.cardCurrentSemFees)
        tvCurrentSemLabel  = view.findViewById(R.id.tvCurrentSemLabel)
        tvCurrentSemBadge  = view.findViewById(R.id.tvCurrentSemBadge)
        tvCurrentPaid      = view.findViewById(R.id.tvCurrentPaid)
        tvCurrentTotal     = view.findViewById(R.id.tvCurrentTotal)
        tvCurrentRemaining = view.findViewById(R.id.tvCurrentRemaining)
    }

    // ─────────────────────────────────────────────
    // STATIC UI
    // ─────────────────────────────────────────────
    private fun setupStaticUI() {
        tvGreeting.text    = getGreeting()
        tvStudentName.text = session.getFullName()
        tvDate.text        = getCurrentDate()

        val course = session.getCourseName()
        val sem    = session.getSemester()
        val roll   = session.getRollNo()
        tvStudentRoll.text = "$course • Semester $sem • Roll: $roll"

        currentSemNumber = sem.toIntOrNull() ?: 1
    }

    // ─────────────────────────────────────────────
    // ✅ Load Current Sem Fees Card
    // ─────────────────────────────────────────────
    private fun loadCurrentSemFees() {
        val studentId = session.getStudentId()
        val courseId  = session.getCourseId()
        val sem       = session.getSemester()
        currentSemNumber = sem.toIntOrNull() ?: 1

        if (studentId.isEmpty() || courseId.isEmpty()) return

        // Step 1: get total fees from course
        firestore.collection("courses").document(courseId).get()
            .addOnSuccessListener { courseDoc ->
                @Suppress("UNCHECKED_CAST")
                val rawFees = courseDoc.get("semesterFees") as? Map<String, Any>
                    ?: emptyMap()

                val semFeesMap = rawFees.mapValues { entry ->
                    when (val v = entry.value) {
                        is Long   -> v
                        is Number -> v.toLong()
                        else      -> 0L
                    }
                }

                val totalFees = semFeesMap[currentSemNumber.toString()] ?: 0L
                currentSemTotal = totalFees

                // Step 2: get paid amount from fees doc
                firestore.collection("fees").document(studentId).get()
                    .addOnSuccessListener { feeDoc ->
                        @Suppress("UNCHECKED_CAST")
                        val sems = feeDoc.get("semesters")
                                as? List<Map<String, Any>> ?: emptyList()

                        val semData = sems.find { map ->
                            (map["semNumber"] as? Long)?.toInt() == currentSemNumber
                        }

                        val paid   = semData?.get("paidAmount") as? Long ?: 0L
                        val status = semData?.get("status") as? String ?: "pending"
                        val pDate  = semData?.get("paidDate") as? Long ?: 0L

                        currentSemPaid     = paid
                        currentSemStatus   = status
                        currentSemPaidDate = pDate

                        // ✅ Update fees card UI
                        updateCurrentSemCard(totalFees, paid, status)
                    }
                    .addOnFailureListener {
                        // No fees doc yet → all zeros
                        updateCurrentSemCard(totalFees, 0L, "pending")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "loadCurrentSemFees: ${e.message}")
            }
    }

    // ─────────────────────────────────────────────
    // ✅ Update Fees Card UI
    // ─────────────────────────────────────────────
    private fun updateCurrentSemCard(total: Long, paid: Long, status: String) {
        if (!isAdded) return  // ✅ Safety check

        val remaining = total - paid

        tvCurrentSemLabel.text  = "Semester $currentSemNumber"
        tvCurrentPaid.text      = "₹${String.format("%,d", paid)}"
        tvCurrentTotal.text     = "₹${String.format("%,d", total)}"
        tvCurrentRemaining.text = "₹${String.format("%,d", remaining)}"

        val (badgeText, badgeDrawable) = when (status.lowercase()) {
            "paid"    -> Pair("PAID",    R.drawable.bg_badge_green)
            "partial" -> Pair("PARTIAL", R.drawable.bg_badge_orange)
            else      -> Pair("PENDING", R.drawable.bg_badge_red)
        }
        tvCurrentSemBadge.text = badgeText
        tvCurrentSemBadge.setBackgroundResource(badgeDrawable)

        // ✅ Update fees stat card too
        tvFeesStat.text = badgeText
        val feeColor = when (status.lowercase()) {
            "paid"    -> R.color.green
            "partial" -> R.color.orange
            else      -> R.color.red
        }
        tvFeesStat.setTextColor(
            ContextCompat.getColor(requireContext(), feeColor)
        )

        // ✅ Card click → open FeesReceiptActivity if paid, else open FeesActivity
        cardCurrentSemFees.setOnClickListener {
            if (status.lowercase() == "paid") {
                // ✅ Open receipt directly
                requireContext().startActivity(
                    Intent(requireContext(), FeesReceiptActivity::class.java).apply {
                        putExtra("semNumber",   currentSemNumber)
                        putExtra("totalAmount", currentSemTotal)
                        putExtra("paidAmount",  currentSemPaid)
                        putExtra("status",      currentSemStatus)
                        putExtra("paidDate",    currentSemPaidDate)
                        putExtra("studentName", session.getFullName())
                        putExtra("rollNo",      session.getRollNo())
                        putExtra("courseName",  session.getCourseName())
                    }
                )
            } else {
                // ✅ Open FeesActivity to pay
                startActivity(Intent(requireContext(), FeesActivity::class.java))
            }
        }
    }

    // ─────────────────────────────────────────────
    // ATTENDANCE STAT
    // ─────────────────────────────────────────────
    private fun loadAttendance() {
        val studentId = session.getStudentId()
        val semNum    = session.getSemester().filter { it.isDigit() }

        firestore.collection("studentSummary")
            .document("${studentId}_${semNum}")
            .get(Source.CACHE)
            .addOnSuccessListener { doc ->
                if (doc.exists()) updateAttendanceUI(doc)
                else {
                    firestore.collection("studentSummary")
                        .document("${studentId}_${semNum}")
                        .get(Source.SERVER)
                        .addOnSuccessListener { serverDoc ->
                            if (serverDoc.exists()) updateAttendanceUI(serverDoc)
                            else tvAttendanceStat.text = "N/A"
                        }
                }
            }
            .addOnFailureListener { tvAttendanceStat.text = "N/A" }
    }

    private fun updateAttendanceUI(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ) {
        val total   = doc.getLong("totalLectures") ?: 0L
        val present = doc.getLong("totalPresent") ?: 0L
        tvAttendanceStat.text = if (total > 0)
            String.format("%.0f%%", present * 100.0 / total)
        else "0%"
    }

    // ─────────────────────────────────────────────
    // TODAY'S LECTURES
    // ─────────────────────────────────────────────
    private fun loadTodayLectures() {
        val divisionId = session.getDivisionId()
        val today      = getTodayDay()

        firestore.collection("divisions")
            .document(divisionId)
            .collection("timetable")
            .whereEqualTo("day", today)
            .get(Source.CACHE)
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    tvLecturesStat.text = String.format("%02d", snap.size())
                } else {
                    firestore.collection("divisions")
                        .document(divisionId)
                        .collection("timetable")
                        .whereEqualTo("day", today)
                        .get(Source.SERVER)
                        .addOnSuccessListener { serverSnap ->
                            tvLecturesStat.text =
                                String.format("%02d", serverSnap.size())
                        }
                }
            }
            .addOnFailureListener { tvLecturesStat.text = "00" }
    }

    // ─────────────────────────────────────────────
    // FEES STATUS (stat card only)
    // ─────────────────────────────────────────────
    private fun loadFeesStatus() {
        // ✅ This is now handled inside loadCurrentSemFees → updateCurrentSemCard
        // Keep empty or remove — no duplicate Firestore call needed
    }

    // ─────────────────────────────────────────────
    // ASSIGNMENTS
    // ─────────────────────────────────────────────
    private fun loadAssignments() {
        val courseId = session.getCourseId()
        val yearNum  = session.getYear().filter { it.isDigit() }
        val semNum   = session.getSemester().filter { it.isDigit() }
        val now      = System.currentTimeMillis()

        firestore.collection("assignments")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("year", yearNum)
            .whereEqualTo("semester", semNum)
            .whereGreaterThanOrEqualTo("dueDate", now)
            .get(Source.CACHE)
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    tvAssignmentsStat.text = String.format("%02d", snap.size())
                } else {
                    firestore.collection("assignments")
                        .whereEqualTo("courseId", courseId)
                        .whereEqualTo("year", yearNum)
                        .whereEqualTo("semester", semNum)
                        .whereGreaterThanOrEqualTo("dueDate", now)
                        .get(Source.SERVER)
                        .addOnSuccessListener { serverSnap ->
                            tvAssignmentsStat.text =
                                String.format("%02d", serverSnap.size())
                        }
                        .addOnFailureListener {
                            tvAssignmentsStat.text = "00"
                        }
                }
            }
            .addOnFailureListener { tvAssignmentsStat.text = "00" }
    }

    // ─────────────────────────────────────────────
    // CLICK LISTENERS
    // ─────────────────────────────────────────────
    private fun setupClickListeners() {

        // ✅ Timetable card → navigate to timetable fragment
        cardTimetable.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNav)
                ?.selectedItemId = R.id.timetableFragment
        }

        // ✅ Attendance card → navigate to attendance fragment
        cardAttendanceDetails.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNav)
                ?.selectedItemId = R.id.attendanceFragment
        }

        // ✅ Fees portal card → open FeesActivity
        cardFees.setOnClickListener {
            startActivity(Intent(requireContext(), FeesActivity::class.java))
        }

        // ✅ Assignments card
        cardAssignments.setOnClickListener {
            startActivity(Intent(requireContext(), StudentAssignmentListActivity::class.java))
        }

        // ✅ Materials card
        cardMaterials.setOnClickListener {
            startActivity(Intent(requireContext(), MaterialsListActivity::class.java))
        }

        // ✅ Exams card
        cardExams.setOnClickListener {
            Toast.makeText(context, "Exams coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────
    private fun getGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11  -> "Good Morning 👋"
            in 12..16 -> "Good Afternoon ☀️"
            else      -> "Good Evening 🌙"
        }
    }

    private fun getCurrentDate(): String =
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date())

    private fun getTodayDay(): String =
        SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
}
