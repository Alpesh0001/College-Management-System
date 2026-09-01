package com.example.collegemanagementsystemfaculty.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.adapters.MyLectureAdapter
import com.example.collegemanagementsystemfaculty.models.MyLecture
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimetableFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    // ✅ Views
    private lateinit var tvTodayDate    : TextView
    private lateinit var tvFacultyName  : TextView
    private lateinit var tvLectureCount : TextView
    private lateinit var btnGoToToday   : TextView
    private lateinit var dayTabsContainer: LinearLayout
    private lateinit var recyclerMyLectures: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var layoutLoading  : LinearLayout
    private var isLoading = false
    // ✅ Data
    private val days = listOf(
        "Monday","Tuesday","Wednesday",
        "Thursday","Friday","Saturday"
    )

    // ✅ Store ALL lectures per day in memory
    private val allLectures = mutableMapOf<String, MutableList<MyLecture>>()
    private lateinit var lectureAdapter: MyLectureAdapter
    private var selectedDay = "Monday"
    private var todayName   = "Monday"

    // ═══════════════════════════════════
    // ✅ onCreateView
    // ═══════════════════════════════════
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_timetable, container, false)
    }

    // ═══════════════════════════════════
    // ✅ onViewCreated
    // ═══════════════════════════════════
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())

        // ✅ Init empty lists for all days
        days.forEach { allLectures[it] = mutableListOf() }

        bindViews(view)
        setupHeader()
        setupDayTabs()
        setupRecyclerView()
        setupGoToToday()
        loadMyLectures()
    }

    // ═══════════════════════════════════
    // ✅ Bind Views
    // ═══════════════════════════════════
    private fun bindViews(view: View) {
        tvTodayDate      = view.findViewById(R.id.tvTodayDate)
        tvFacultyName    = view.findViewById(R.id.tvFacultyName)
        tvLectureCount   = view.findViewById(R.id.tvLectureCount)
        btnGoToToday     = view.findViewById(R.id.btnGoToToday)
        dayTabsContainer = view.findViewById(R.id.dayTabsContainer)
        recyclerMyLectures = view.findViewById(R.id.recyclerMyLectures)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        tvEmptySubtitle  = view.findViewById(R.id.tvEmptySubtitle)
        layoutLoading    = view.findViewById(R.id.layoutLoading)
    }

    // ═══════════════════════════════════
    // ✅ Setup Header
    // ═══════════════════════════════════
    private fun setupHeader() {
        // ✅ Today's date
        val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        tvTodayDate.text = dateFormat.format(Calendar.getInstance().time)

        // ✅ Faculty name from session
        tvFacultyName.text = session.getFullName()

        // ✅ Get today's day name
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        todayName = dayFormat.format(Calendar.getInstance().time)

        // ✅ If today is Sunday → default Monday
        selectedDay = if (todayName == "Sunday") "Monday" else todayName
    }

    // ═══════════════════════════════════
    // ✅ Setup Day Tabs
    // ═══════════════════════════════════
    private fun setupDayTabs() {
        dayTabsContainer.removeAllViews()

        days.forEach { day ->
            val tab = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_day_tab, dayTabsContainer, false) as TextView

            tab.text = day.substring(0, 3).uppercase()

            // ✅ Mark TODAY tab with dot indicator
            if (day == todayName) {
                tab.text = "${day.substring(0, 3).uppercase()} •"
            }

            tab.setOnClickListener {
                selectedDay = day
                updateDayTabs()
                showLecturesForDay(day)
            }

            dayTabsContainer.addView(tab)
        }

        updateDayTabs()
    }

    // ✅ Update tab selected/unselected UI
    private fun updateDayTabs() {
        for (i in 0 until dayTabsContainer.childCount) {
            val tab = dayTabsContainer.getChildAt(i) as TextView
            val isSelected = days[i] == selectedDay

            tab.setBackgroundResource(
                if (isSelected) R.drawable.tab_selected_bg
                else R.drawable.tab_unselected_bg
            )
            tab.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isSelected) android.R.color.white
                    else R.color.grey_text
                )
            )
        }
    }

    // ═══════════════════════════════════
    // ✅ Setup RecyclerView
    // ═══════════════════════════════════
    private fun setupRecyclerView() {
        recyclerMyLectures.layoutManager = LinearLayoutManager(requireContext())
        lectureAdapter = MyLectureAdapter(mutableListOf())
        recyclerMyLectures.adapter = lectureAdapter
    }

    // ═══════════════════════════════════
    // ✅ Go To Today Button
    // ═══════════════════════════════════
    private fun setupGoToToday() {
        btnGoToToday.setOnClickListener {
            selectedDay = if (todayName == "Sunday") "Monday" else todayName
            updateDayTabs()
            showLecturesForDay(selectedDay)
        }
    }

    private fun loadMyLectures() {

        val facultyId = session.getFacultyId()

        if (facultyId.isEmpty()) {
            showEmpty("Session expired. Please login again.")
            return
        }

        if (isLoading) return
        isLoading = true

        days.forEach { allLectures[it]?.clear() }

        showLoading(true)

        // 🔥 TRY CACHE FIRST
        db.collectionGroup("timetable")
            .whereEqualTo("facultyId", facultyId)
            .get(com.google.firebase.firestore.Source.CACHE)
            .addOnSuccessListener { snapshot ->

                if (!snapshot.isEmpty) {
                    processTimetableSnapshot(snapshot)
                } else {
                    // 🔥 If cache empty → SERVER
                    db.collectionGroup("timetable")
                        .whereEqualTo("facultyId", facultyId)
                        .get(com.google.firebase.firestore.Source.SERVER)
                        .addOnSuccessListener { serverSnap ->
                            processTimetableSnapshot(serverSnap)
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            showLoading(false)
                            Log.e("TimetableFragment", "Failed to load: ${e.message}")
                            showEmpty("Failed to load: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                showLoading(false)
                showEmpty("Failed to load: ${e.message}")
            }
    }

    private fun processTimetableSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot) {

        if (snapshot.isEmpty) {
            isLoading = false
            showLoading(false)
            showLecturesForDay(selectedDay)
            return
        }

        val totalDocs = snapshot.documents.size
        var processed = 0

        snapshot.documents.forEach { doc ->

            val day         = doc.getString("day") ?: ""
            val timeFrom    = doc.getString("timeFrom") ?: ""
            val timeTo      = doc.getString("timeTo") ?: ""
            val subjectName = doc.getString("subjectName") ?: ""
            val subjectCode = doc.getString("subjectCode") ?: ""
            val roomNo      = doc.getString("roomNo") ?: ""
            val slotType    = doc.getString("slotType") ?: "lecture"
            val divisionId  = doc.reference.parent.parent?.id ?: ""

            if (day.isEmpty() || timeFrom.isEmpty() || divisionId.isEmpty()) {
                processed++
                if (processed == totalDocs) finishLoading()
                return@forEach
            }

            db.collection("divisions")
                .document(divisionId)
                .get()
                .addOnSuccessListener { divDoc ->

                    val lecture = MyLecture(
                        slotId = doc.id,
                        divisionId = divisionId,
                        divisionName = divDoc.getString("divisionName") ?: "Unknown",
                        day = day,
                        timeFrom = timeFrom,
                        timeTo = timeTo,
                        subjectName = subjectName,
                        subjectCode = subjectCode,
                        roomNo = roomNo,
                        slotType = slotType
                    )

                    if (days.contains(day)) {
                        allLectures[day]?.add(lecture)
                    }

                    processed++
                    if (processed == totalDocs) finishLoading()
                }
                .addOnFailureListener {
                    processed++
                    if (processed == totalDocs) finishLoading()
                }
        }
    }

    private fun finishLoading() {
        if (!isAdded) return

        allLectures.forEach { (_, lectures) ->
            lectures.sortBy { convertTimeToMinutes(it.timeFrom) }
        }

        isLoading = false
        showLoading(false)
        showLecturesForDay(selectedDay)
    }


    private fun onAllLecturesLoaded() {
        if (!isAdded) return  // ✅ Safety check

        // ✅ Sort each day by time
        allLectures.forEach { (_, lectures) ->
            lectures.sortBy { convertTimeToMinutes(it.timeFrom) }
        }

        showLoading(false)

        // ✅ Show today's lectures
        showLecturesForDay(selectedDay)
    }

    // ═══════════════════════════════════
    // ✅ Show Lectures For Selected Day
    // ═══════════════════════════════════
    private fun showLecturesForDay(day: String) {
        val lectures = allLectures[day] ?: mutableListOf()

        lectureAdapter.updateLectures(lectures)

        // ✅ Update count text
        val count = lectures.size
        tvLectureCount.text = when {
            day == todayName -> "$count lecture${if (count == 1) "" else "s"} today"
            count == 0       -> "No lectures on $day"
            else             -> "$count lecture${if (count == 1) "" else "s"} on $day"
        }

        // ✅ Show/Hide empty state
        if (lectures.isEmpty()) {
            recyclerMyLectures.visibility = View.GONE
            layoutEmptyState.visibility   = View.VISIBLE
            tvEmptySubtitle.text = if (day == todayName)
                "Enjoy your free day! 🎉"
            else
                "No lectures scheduled for $day"
        } else {
            recyclerMyLectures.visibility = View.VISIBLE
            layoutEmptyState.visibility   = View.GONE
        }
    }

    // ═══════════════════════════════════
    // ✅ Show / Hide Loading
    // ═══════════════════════════════════
    private fun showLoading(show: Boolean) {
        if (!isAdded) return
        layoutLoading.visibility      = if (show) View.VISIBLE else View.GONE
        recyclerMyLectures.visibility = if (show) View.GONE    else View.VISIBLE
        if (show) layoutEmptyState.visibility = View.GONE
    }

    // ✅ Show empty with custom message
    private fun showEmpty(message: String) {
        if (!isAdded) return
        recyclerMyLectures.visibility = View.GONE
        layoutLoading.visibility      = View.GONE
        layoutEmptyState.visibility   = View.VISIBLE
        tvEmptySubtitle.text          = message
    }

    override fun onResume() {
        super.onResume()
        if (allLectures.values.all { it.isEmpty() }) {
            loadMyLectures()
        }
    }

    private fun convertTimeToMinutes(time: String): Int {
        return try {
            val clean    = time.trim().uppercase(Locale.getDefault())
            val isPM     = clean.contains("PM")
            val isAM     = clean.contains("AM")
            val timePart = clean.replace("AM", "").replace("PM", "").trim()
            val parts    = timePart.split(":")
            var hour     = parts[0].trim().toInt()
            val min      = if (parts.size > 1) parts[1].trim().toInt() else 0
            if (isPM && hour != 12) hour += 12
            if (isAM && hour == 12) hour = 0
            hour * 60 + min
        } catch (e: Exception) { 0 }
    }
}
