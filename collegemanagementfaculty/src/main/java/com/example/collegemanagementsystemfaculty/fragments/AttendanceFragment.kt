package com.example.collegemanagementsystemfaculty.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.MarkAttendanceActivity
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.adapters.AttendanceLectureAdapter
import com.example.collegemanagementsystemfaculty.models.MyLecture
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AttendanceFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    private lateinit var tvSelectedDate      : TextView
    private lateinit var tvFacultyName       : TextView
    private lateinit var tvTotalCount        : TextView
    private lateinit var tvMarkedCount       : TextView
    private lateinit var tvPendingCount      : TextView
    private lateinit var tvDateLabel         : TextView
    private lateinit var btnPrevDate         : ImageButton
    private lateinit var btnNextDate         : ImageButton
    private lateinit var btnGoToToday        : TextView
    private lateinit var rvAttendanceLectures: RecyclerView
    private lateinit var layoutEmptyState    : LinearLayout
    private lateinit var tvEmptySubtitle     : TextView
    private lateinit var layoutLoading       : LinearLayout

    private val allLectures = mutableMapOf<String, MutableList<MyLecture>>()
    private val days = listOf(
        "Monday", "Tuesday", "Wednesday",
        "Thursday", "Friday", "Saturday"
    )
    private lateinit var adapter: AttendanceLectureAdapter

    private var selectedCalendar = Calendar.getInstance()
    private var todayCalendar    = Calendar.getInstance()
    private var isLoading        = false
    private var lecturesLoaded = false
    private val markedSlots      = mutableMapOf<String, Boolean>()

    private val markAttendanceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // ✅ Like Hotel Flow: Refresh everything immediately
                refreshAttendanceData()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        days.forEach { allLectures[it] = mutableListOf() }

        bindViews(view)
        setupHeader()
        setupDateNavigation()
        setupRecyclerView()
        
        // Initial Load
        loadMyLectures()
    }

    private fun bindViews(view: View) {
        tvSelectedDate       = view.findViewById(R.id.tvSelectedDate)
        tvFacultyName        = view.findViewById(R.id.tvFacultyName)
        tvTotalCount         = view.findViewById(R.id.tvTotalCount)
        tvMarkedCount        = view.findViewById(R.id.tvMarkedCount)
        tvPendingCount       = view.findViewById(R.id.tvPendingCount)
        tvDateLabel          = view.findViewById(R.id.tvDateLabel)
        btnPrevDate          = view.findViewById(R.id.btnPrevDate)
        btnNextDate          = view.findViewById(R.id.btnNextDate)
        btnGoToToday         = view.findViewById(R.id.btnGoToToday)
        rvAttendanceLectures = view.findViewById(R.id.rvAttendanceLectures)
        layoutEmptyState     = view.findViewById(R.id.layoutEmptyState)
        tvEmptySubtitle      = view.findViewById(R.id.tvEmptySubtitle)
        layoutLoading        = view.findViewById(R.id.layoutLoading)
    }

    private fun setupHeader() {
        tvFacultyName.text = session.getFullName()
        updateDateDisplay()
    }

    private fun setupDateNavigation() {
        btnPrevDate.setOnClickListener {
            selectedCalendar.add(Calendar.DAY_OF_YEAR, -1)
            onDateNavigationChanged()
        }
        btnNextDate.setOnClickListener {
            selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
            onDateNavigationChanged()
        }
        btnGoToToday.setOnClickListener {
            selectedCalendar = Calendar.getInstance()
            onDateNavigationChanged()
        }
    }

    private fun onDateNavigationChanged() {
        updateDateDisplay()
        // ✅ Instant update from cache when changing dates
        loadMarkedStatusForDate {
            showLecturesForSelectedDate()
        }
    }

    private fun updateDateDisplay() {
        val isToday = isSameDay(selectedCalendar, todayCalendar)
        val headerFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        tvSelectedDate.text = headerFormat.format(selectedCalendar.time)
        val fullFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        tvDateLabel.text = if (isToday) "Today, ${fullFormat.format(selectedCalendar.time)}" else fullFormat.format(selectedCalendar.time)
        btnGoToToday.visibility = if (isToday) View.GONE else View.VISIBLE
        btnNextDate.alpha = if (isToday) 0.3f else 1.0f
        btnNextDate.isEnabled = !isToday
    }

    private fun setupRecyclerView() {
        rvAttendanceLectures.layoutManager = LinearLayoutManager(requireContext())
        adapter = AttendanceLectureAdapter(mutableListOf(), markedSlots) { openAttendanceMarking(it) }
        rvAttendanceLectures.adapter = adapter
    }

    private fun loadMyLectures() {

        if (lecturesLoaded) return

        val facultyId = session.getFacultyId()
        if (facultyId.isEmpty()) return

        if (isLoading) return
        isLoading = true

        val hasData = allLectures.values.any { it.isNotEmpty() }
        if (!hasData) showLoading(true)

        db.collectionGroup("timetable")
            .whereEqualTo("facultyId", facultyId)
            .get()
            .addOnSuccessListener { snapshot ->

                lecturesLoaded = true
                isLoading = false

                days.forEach { allLectures[it]?.clear() }

                if (snapshot.isEmpty) {
                    showLoading(false)
                    showLecturesForSelectedDate()
                    return@addOnSuccessListener
                }

                snapshot.documents.forEach { doc ->

                    val day = doc.getString("day") ?: return@forEach
                    val divisionId = doc.reference.parent.parent?.id ?: ""

                    if (!days.contains(day) || divisionId.isEmpty()) return@forEach

                    val lecture = MyLecture(
                        slotId       = doc.id,
                        divisionId   = divisionId,
                        divisionName = "Division",
                        day          = day,
                        timeFrom     = doc.getString("timeFrom") ?: "",
                        timeTo       = doc.getString("timeTo") ?: "",
                        subjectName  = doc.getString("subjectName") ?: "",
                        subjectCode  = doc.getString("subjectCode") ?: "",
                        roomNo       = doc.getString("roomNo") ?: "",
                        slotType     = doc.getString("slotType") ?: "lecture"
                    )

                    allLectures[day]?.add(lecture)
                }

                // ✅ Sort once
                allLectures.forEach { (_, list) ->
                    list.sortBy { convertTimeToMinutes(it.timeFrom) }
                }

                showLoading(false)

                // ✅ Load attendance marks after lectures ready
                refreshAttendanceData()
            }
            .addOnFailureListener {
                isLoading = false
                showLoading(false)
            }
    }

    private fun checkFinished(processed: Int, total: Int) {
        if (processed == total) {
            allLectures.forEach { (_, lectures) -> lectures.sortBy { convertTimeToMinutes(it.timeFrom) } }
            showLoading(false)
            refreshAttendanceData()
        }
    }

    private fun addLecture(lecture: MyLecture) {
        if (days.contains(lecture.day)) {
            allLectures[lecture.day]?.add(lecture)
        }
    }

    private fun refreshAttendanceData() {
        loadMarkedStatusForDate { showLecturesForSelectedDate() }
    }

    private fun loadMarkedStatusForDate(onDone: () -> Unit) {
        val dateStr = getSelectedDateString()
        val facultyId = session.getFacultyId()

        // ✅ CRITICAL: Check Cache first like the Hotel Edit mode logic
        db.collection("attendance").document(dateStr)
            .get(Source.CACHE)
            .addOnSuccessListener { doc ->
                processMarkedDoc(doc, facultyId, dateStr)
                onDone()
            }
            .addOnFailureListener {
                db.collection("attendance").document(dateStr).get()
                    .addOnSuccessListener { doc ->
                        processMarkedDoc(doc, facultyId, dateStr)
                        onDone()
                    }.addOnFailureListener { onDone() }
            }
    }

    private fun processMarkedDoc(doc: com.google.firebase.firestore.DocumentSnapshot, facultyId: String, dateStr: String) {
        markedSlots.clear()
        if (doc.exists()) {
            val slots = doc.get("slots") as? List<*> ?: return
            slots.forEach { slot ->
                val slotMap = slot as? Map<*, *> ?: return@forEach
                val slotId = slotMap["slotId"] as? String ?: return@forEach
                val divisions = slotMap["divisions"] as? List<*> ?: return@forEach
                divisions.forEach { div ->
                    val divMap = div as? Map<*, *> ?: return@forEach
                    if (divMap["facultyId"] == facultyId && (divMap["isMarked"] as? Boolean == true)) {
                        markedSlots["${slotId}_${dateStr}"] = true
                    }
                }
            }
        }
    }

    private fun showLecturesForSelectedDate() {
        if (!isAdded) return
        val dayName = getDayName(selectedCalendar)
        val lectures = allLectures[dayName] ?: mutableListOf()
        val dateStr = getSelectedDateString()

        val marked = lectures.count { markedSlots["${it.slotId}_${dateStr}"] == true }
        tvTotalCount.text = lectures.size.toString()
        tvMarkedCount.text = marked.toString()
        tvPendingCount.text = (lectures.size - marked).toString()

        adapter.updateLectures(lectures, markedSlots, dateStr)
        rvAttendanceLectures.visibility = if (lectures.isEmpty()) View.GONE else View.VISIBLE
        layoutEmptyState.visibility = if (lectures.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openAttendanceMarking(lecture: MyLecture) {
        val dateStr = getSelectedDateString()
        val intent = Intent(requireContext(), MarkAttendanceActivity::class.java).apply {
            putExtra("slotId", lecture.slotId); putExtra("divisionId", lecture.divisionId)
            putExtra("divisionName", lecture.divisionName); putExtra("subjectName", lecture.subjectName)
            putExtra("subjectCode", lecture.subjectCode); putExtra("timeFrom", lecture.timeFrom)
            putExtra("timeTo", lecture.timeTo); putExtra("date", dateStr)
            putExtra("day", lecture.day); putExtra("isEdit", markedSlots["${lecture.slotId}_${dateStr}"] == true)
        }
        markAttendanceLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()

        // Only refresh marks — NOT lectures
        if (lecturesLoaded && !isLoading) {
            refreshAttendanceData()
        }
    }

    private fun showLoading(show: Boolean) {
        if (!isAdded) return
        layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        rvAttendanceLectures.visibility = if (show) View.GONE else View.VISIBLE
        if (show) layoutEmptyState.visibility = View.GONE
    }

    private fun showEmpty(message: String) {
        if (!isAdded) return
        rvAttendanceLectures.visibility = View.GONE
        layoutLoading.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        tvEmptySubtitle.text = message
    }

    private fun getSelectedDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.time)
    private fun getDayName(calendar: Calendar): String = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
    private fun isSameDay(c1: Calendar, c2: Calendar): Boolean = c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    private fun convertTimeToMinutes(time: String): Int {
        return try {
            val clean = time.trim().uppercase(); val isPM = clean.contains("PM"); val isAM = clean.contains("AM")
            val parts = clean.replace("AM", "").replace("PM", "").trim().split(":")
            var hour = parts[0].toInt(); val min = if (parts.size > 1) parts[1].toInt() else 0
            if (isPM && hour != 12) hour += 12; if (isAM && hour == 12) hour = 0; hour * 60 + min
        } catch (e: Exception) { 0 }
    }
}
