package com.example.collegemanagementsystemfaculty.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.collegemanagementsystemfaculty.AssignmentListActivity
import com.example.collegemanagementsystemfaculty.ManageMaterialsActivity
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.TimeTableActivity
import com.example.collegemanagementsystemfaculty.ViewStudentsActivity
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    private lateinit var tvGreeting: TextView
    private lateinit var tvFacultyName: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var tvDate: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvLabelStudents: TextView
    private lateinit var tvLabelFaculty: TextView
    private lateinit var tvLabelAttendance: TextView
    private lateinit var tvLabelSubjects: TextView

    private lateinit var tvTotalStudents: TextView
    private lateinit var tvTotalFaculty: TextView
    private lateinit var tvTodayAttendance: TextView
    private lateinit var tvTotalSubjects: TextView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())

        bindViews(view)
        setupHeader()
        updateOverviewLabels()
        setupClickListeners()
        loadDashboardStats()   // ✅ Only once
    }

    private fun bindViews(view: View) {
        tvGreeting        = view.findViewById(R.id.tvGreeting)
        tvFacultyName     = view.findViewById(R.id.tvFacultyName)
        tvDepartment      = view.findViewById(R.id.tvDepartment)
        tvDate            = view.findViewById(R.id.tvDate)
        ivAvatar          = view.findViewById(R.id.ivAvatar)

        tvTotalStudents   = view.findViewById(R.id.tvTotalStudents)
        tvTotalFaculty    = view.findViewById(R.id.tvTotalFaculty)
        tvTodayAttendance = view.findViewById(R.id.tvTodayAttendance)
        tvTotalSubjects   = view.findViewById(R.id.tvTotalSubjects)
        tvLabelStudents   = view.findViewById(R.id.tvLabelStudents)
        tvLabelFaculty    = view.findViewById(R.id.tvLabelFaculty)
        tvLabelAttendance = view.findViewById(R.id.tvLabelAttendance)
        tvLabelSubjects   = view.findViewById(R.id.tvLabelSubjects)
    }

    private fun setupHeader() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        tvGreeting.text = when {
            hour < 12 -> "Good Morning 👋"
            hour < 17 -> "Good Afternoon ☀️"
            else      -> "Good Evening 🌙"
        }

        tvFacultyName.text = session.getFullName()

        tvDepartment.text =
            "${session.getDesignation().ifEmpty { session.getRole() }} — ${session.getCourseName()}"

        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
        tvDate.text = sdf.format(Date())

        val photoUrl = session.getPhotoUrl()

        if (photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .into(ivAvatar)
        }
    }

    // 🔥 ROLE-BASED DASHBOARD STATS
    private fun loadDashboardStats() {

        val courseId = session.getCourseId()
        val facultyId = session.getFacultyId()
        val role = session.getRole()

        if (courseId.isEmpty()) return

        // =====================================================
        // 🏫 HOD VIEW
        // =====================================================
        if (role.equals("HOD", true)) {

            loadTotalStudents(courseId)
            loadTotalFaculty(courseId)
            loadTotalSubjects(courseId)
            loadTodayAttendance(courseId)

        }
        // =====================================================
        // 👨‍🏫 FACULTY VIEW
        // =====================================================
        else {
            loadTotalStudents(courseId)
            loadTodayLecturesAndAttendance(facultyId)
        }
    }

    private fun updateOverviewLabels() {

        val role = session.getRole()

        if (role.equals("HOD", true)) {

            tvLabelStudents.text   = "Total Students"
            tvLabelFaculty.text    = "Total Faculty"
            tvLabelAttendance.text = "Today Attendance"
            tvLabelSubjects.text   = "Total Subjects"

        } else {

            tvLabelStudents.text   = "Total Students"
            tvLabelFaculty.text    = "Today Lectures"
            tvLabelAttendance.text = "Attendance Taken"
            tvLabelSubjects.text   = "Pending Lectures"
        }
    }
    // ================= HOD DATA =================

    private fun loadTotalStudents(courseId: String) {
        db.collection("students")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener {
                tvTotalStudents.text = it.size().toString()
            }
    }

    private fun loadTotalFaculty(courseId: String) {
        db.collection("faculties")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener {
                tvTotalFaculty.text = it.size().toString()
            }
    }

    private fun loadTotalSubjects(courseId: String) {
        db.collection("subjects")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener {
                tvTotalSubjects.text = it.size().toString()
            }
    }

    private fun loadTodayLecturesAndAttendance(facultyId: String) {

        val todayDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 🔹 Get today's lectures
        db.collectionGroup("timetable")
            .whereEqualTo("facultyId", facultyId)
            .whereEqualTo("day", todayDay)
            .get()
            .addOnSuccessListener { lectureSnap ->

                val totalLectures = lectureSnap.size()

                // 👉 Show Today Lectures
                tvTotalFaculty.text = totalLectures.toString()

                if (totalLectures == 0) {
                    tvTodayAttendance.text = "0"
                    tvTotalSubjects.text   = "0"
                    return@addOnSuccessListener
                }

                // 🔹 Now check attendance marked
                db.collection("attendance")
                    .document(todayDate)
                    .get()
                    .addOnSuccessListener { doc ->

                        var marked = 0

                        if (doc.exists()) {

                            val slots = doc.get("slots") as? List<*> ?: emptyList<Any>()

                            for (slot in slots) {
                                val slotMap = slot as? Map<*, *> ?: continue
                                val divisions = slotMap["divisions"] as? List<*> ?: continue

                                for (div in divisions) {
                                    val divMap = div as? Map<*, *> ?: continue

                                    if (divMap["facultyId"] == facultyId &&
                                        divMap["isMarked"] == true) {

                                        marked++
                                    }
                                }
                            }
                        }

                        val pending = totalLectures - marked

                        // 👉 Attendance Taken
                        tvTodayAttendance.text = marked.toString()

                        // 👉 Pending Lectures
                        tvTotalSubjects.text = pending.toString()
                    }
            }
    }

    // ================= ATTENDANCE =================

    private fun loadTodayAttendance(courseId: String) {

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("attendance").document(today)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) {
                    tvTodayAttendance.text = "N/A"
                    return@addOnSuccessListener
                }

                var present = 0
                var total = 0

                val slots = doc.get("slots") as? List<*> ?: emptyList<Any>()

                for (slot in slots) {
                    val slotMap = slot as? Map<*, *> ?: continue
                    val divisions = slotMap["divisions"] as? List<*> ?: continue

                    for (div in divisions) {
                        val divMap = div as? Map<*, *> ?: continue

                        if (divMap["courseId"] == courseId) {
                            present += (divMap["presentCount"] as? Long)?.toInt() ?: 0
                            total += (divMap["totalStudents"] as? Long)?.toInt() ?: 0
                        }
                    }
                }

                tvTodayAttendance.text =
                    if (total > 0) "${(present * 100) / total}%"
                    else "0%"
            }
    }

    // ================= CLICK LISTENERS =================

    private fun setupClickListeners() {

        // Attendance tab navigation
        view?.findViewById<View>(R.id.cardMarkAttendance)?.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNav)
                ?.selectedItemId = R.id.attendanceFragment
        }

        // 🔥 ROLE-BASED TIMETABLE NAVIGATION
        view?.findViewById<View>(R.id.cardTimeTable)?.setOnClickListener {

            val role = session.getRole()

            if (role.equals("HOD", true)) {
                // HOD → Manage timetable
                startActivity(Intent(requireContext(), TimeTableActivity::class.java))
            } else {
                // Faculty → View timetable fragment
                activity?.findViewById<BottomNavigationView>(R.id.bottomNav)
                    ?.selectedItemId = R.id.timetableFragment
            }
        }

        // View Students
        view?.findViewById<View>(R.id.cardViewStudents)?.setOnClickListener {
            startActivity(Intent(requireContext(), ViewStudentsActivity::class.java))
        }

        view?.findViewById<View>(R.id.cardReports)?.setOnClickListener {
            Toast.makeText(context, "Reports coming soon!", Toast.LENGTH_SHORT).show()
        }

        view?.findViewById<View>(R.id.cardAssignments)?.setOnClickListener {
            startActivity(Intent(requireContext(), AssignmentListActivity::class.java))
        }

        view?.findViewById<View>(R.id.cardMaterials)?.setOnClickListener {
            startActivity(Intent(requireContext(), ManageMaterialsActivity::class.java))
        }
    }
}
