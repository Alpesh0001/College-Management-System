package com.example.collegemanagementsystemfaculty

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.adapters.TimeSlotAdapter
import com.example.collegemanagementsystemfaculty.models.TimeSlot
import com.example.collegemanagementsystemfaculty.utils.CoreBaseActivity
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class DivisionTimeTableActivity : CoreBaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    private lateinit var btnBack: View
    private lateinit var btnSave: Button
    private lateinit var layoutBottomSave: LinearLayout
    private lateinit var tvToolbarTitle: TextView
    private lateinit var tvDivisionName: TextView
    private lateinit var tvCourseInfo: TextView
    private lateinit var tvModeBadge: TextView
    private lateinit var dayTabsContainer: LinearLayout
    private lateinit var recyclerTimeSlots: RecyclerView
    private lateinit var fabAddLecture: ExtendedFloatingActionButton
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptySubtitle: TextView

    private val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
    private var selectedDay = "Monday"
    private var mode = "view"

    private lateinit var divisionId: String
    private lateinit var divisionName: String
    private lateinit var courseName: String
    private lateinit var year: String
    private lateinit var semester: String

    private val allSlots = mutableMapOf<String, MutableList<TimeSlot>>()
    private val deletedSlotIds = mutableListOf<String>() // ✅ TRACK DELETED IDs
    private lateinit var timeSlotAdapter: TimeSlotAdapter

    data class SubjectItem(val id: String, val name: String, val code: String) {
        override fun toString() = name
    }
    data class FacultyItem(val id: String, val name: String) {
        override fun toString() = name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_division_time_table)

        session      = SessionManager(this)
        divisionId   = intent.getStringExtra("division_id")   ?: ""
        divisionName = intent.getStringExtra("division_name") ?: ""
        courseName   = intent.getStringExtra("course_name")   ?: ""
        year         = intent.getStringExtra("year")          ?: ""
        semester     = intent.getStringExtra("semester")      ?: ""
        mode         = intent.getStringExtra("mode")          ?: "view"

        days.forEach { allSlots[it] = mutableListOf() }

        bindViews()
        setupToolbar()
        setupDayTabs()
        setupRecyclerView()
        loadTimetable()
    }

    private fun bindViews() {
        btnBack           = findViewById(R.id.btnBack)
        btnSave           = findViewById(R.id.btnSave)
        layoutBottomSave  = findViewById(R.id.layoutBottomSave)
        tvToolbarTitle    = findViewById(R.id.tvToolbarTitle)
        tvDivisionName    = findViewById(R.id.tvDivisionName)
        tvCourseInfo      = findViewById(R.id.tvCourseInfo)
        tvModeBadge       = findViewById(R.id.tvModeBadge)
        dayTabsContainer  = findViewById(R.id.dayTabsContainer)
        recyclerTimeSlots = findViewById(R.id.recyclerTimeSlots)
        fabAddLecture     = findViewById(R.id.fabAddLecture)
        layoutEmptyState  = findViewById(R.id.layoutEmptyState)
        tvEmptySubtitle   = findViewById(R.id.tvEmptySubtitle)
    }

    private fun setupToolbar() {
        tvToolbarTitle.text = "$divisionName Time Table"
        tvDivisionName.text = divisionName
        tvCourseInfo.text   = "$courseName — Year $year — Sem $semester"

        btnBack.setOnClickListener { finish() }

        if (session.isHOD() && mode != "view") {
            tvModeBadge.text = if (mode == "create") "CREATE" else "EDIT"
            tvModeBadge.setBackgroundResource(R.drawable.badge_green)
            fabAddLecture.visibility    = View.VISIBLE
            layoutBottomSave.visibility = View.VISIBLE
            tvEmptySubtitle.text        = "Tap '+ Add Lecture' below to add"
            fabAddLecture.setOnClickListener { showLectureBottomSheet(null, -1) }
            btnSave.setOnClickListener { saveTimetable() }
        } else {
            tvModeBadge.text = "VIEW"
            tvModeBadge.setBackgroundResource(R.drawable.badge_orange)
            fabAddLecture.visibility    = View.GONE
            layoutBottomSave.visibility = View.GONE
            tvEmptySubtitle.text        = "No lectures added yet"
        }
    }

    private fun setupDayTabs() {
        days.forEach { day ->
            val tab = LayoutInflater.from(this)
                .inflate(R.layout.item_day_tab, dayTabsContainer, false) as TextView
            tab.text = day.substring(0, 3).uppercase()
            tab.setOnClickListener {
                selectedDay = day
                updateDayTabs()
                showSlotsForDay(day)
            }
            dayTabsContainer.addView(tab)
        }
        updateDayTabs()
    }

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
                    this,
                    if (isSelected) android.R.color.white
                    else R.color.grey_text
                )
            )
        }
    }

    private fun setupRecyclerView() {
        recyclerTimeSlots.layoutManager = LinearLayoutManager(this)
        timeSlotAdapter = TimeSlotAdapter(
            slots         = mutableListOf(),
            isEditable    = session.isHOD() && mode != "view",
            onEditClick   = { slot, position ->
                showLectureBottomSheet(slot, position)
            },
            onDeleteClick = { slot, position ->
                showDeleteConfirmDialog(slot, position)
            }
        )
        recyclerTimeSlots.adapter = timeSlotAdapter
    }

    private fun showDeleteConfirmDialog(slot: TimeSlot, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Lecture?")
            .setMessage("Remove ${slot.subjectName} (${slot.timeFrom} - ${slot.timeTo}) from ${slot.day}?")
            .setPositiveButton("Delete") { _, _ ->
                // ✅ ADD TO DELETED LIST IF IT HAS A FIRESTORE ID
                if (slot.id.isNotEmpty()) {
                    deletedSlotIds.add(slot.id)
                }
                allSlots[slot.day]?.remove(slot)
                timeSlotAdapter.removeSlot(position)
                checkEmptyState()
                Toast.makeText(this, "✅ Lecture removed!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadTimetable() {
        if (divisionId.isEmpty()) return

        showBlockingLoader("Loading timetable...")
        deletedSlotIds.clear()

        db.collection("divisions")
            .document(divisionId)
            .collection("timetable")
            .addSnapshotListener { snapshot, error ->

                hideBlockingLoader()

                if (error != null) {
                    Toast.makeText(this, "❌ Failed: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                days.forEach { allSlots[it]?.clear() }

                if (snapshot == null || snapshot.isEmpty) {
                    showSlotsForDay(selectedDay)
                    return@addSnapshotListener
                }

                snapshot.documents.forEach { doc ->
                    val slot = TimeSlot(
                        id          = doc.id,
                        day         = doc.getString("day") ?: "",
                        timeFrom    = doc.getString("timeFrom") ?: "",
                        timeTo      = doc.getString("timeTo") ?: "",
                        subjectName = doc.getString("subjectName") ?: "",
                        subjectCode = doc.getString("subjectCode") ?: "",
                        facultyName = doc.getString("facultyName") ?: "",
                        facultyId   = doc.getString("facultyId") ?: "",
                        roomNo      = doc.getString("roomNo") ?: "",
                        slotType    = doc.getString("slotType") ?: "lecture"
                    )

                    if (slot.day.isNotEmpty() && slot.timeFrom.isNotEmpty()) {
                        allSlots[slot.day]?.add(slot)
                    }
                }

                allSlots.forEach { (_, slots) ->
                    slots.sortBy { convertTimeToMinutes(it.timeFrom) }
                }

                showSlotsForDay(selectedDay)
            }
    }


    private fun showSlotsForDay(day: String) {
        val slots = allSlots[day] ?: mutableListOf()
        timeSlotAdapter.updateSlots(slots)
        checkEmptyState()
    }

    private fun checkEmptyState() {
        val slots = allSlots[selectedDay] ?: mutableListOf()
        if (slots.isEmpty()) {
            layoutEmptyState.visibility  = View.VISIBLE
            recyclerTimeSlots.visibility = View.GONE
        } else {
            layoutEmptyState.visibility  = View.GONE
            recyclerTimeSlots.visibility = View.VISIBLE
        }
    }

    private fun showLectureBottomSheet(existingSlot: TimeSlot?, editPosition: Int) {
        val bottomSheet = BottomSheetDialog(this)
        val sheetView   = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add_lecture, null)
        bottomSheet.setContentView(sheetView)
        bottomSheet.behavior.isDraggable = true

        val tvSheetTitle      = sheetView.findViewById<TextView>(R.id.tvSheetTitle)
        val tvSheetSubtitle   = sheetView.findViewById<TextView>(R.id.tvSheetSubtitle)
        val tilTimeFrom       = sheetView.findViewById<TextInputLayout>(R.id.tilTimeFrom)
        val etTimeFrom        = sheetView.findViewById<TextInputEditText>(R.id.etTimeFrom)
        val tilTimeTo         = sheetView.findViewById<TextInputLayout>(R.id.tilTimeTo)
        val etTimeTo          = sheetView.findViewById<TextInputEditText>(R.id.etTimeTo)
        val tilSubject        = sheetView.findViewById<TextInputLayout>(R.id.tilSubject)
        val ddSubject         = sheetView.findViewById<AutoCompleteTextView>(R.id.ddSubject)
        val etSubjectCode     = sheetView.findViewById<TextInputEditText>(R.id.etSubjectCode)
        val tilFaculty        = sheetView.findViewById<TextInputLayout>(R.id.tilFaculty)
        val ddFaculty         = sheetView.findViewById<AutoCompleteTextView>(R.id.ddFaculty)
        val etRoomNo          = sheetView.findViewById<TextInputEditText>(R.id.etRoomNo)
        val tvConflictWarning = sheetView.findViewById<TextView>(R.id.tvConflictWarning)
        val progressSheet     = sheetView.findViewById<ProgressBar>(R.id.progressSheet)
        val btnCancelSheet    = sheetView.findViewById<Button>(R.id.btnCancelSheet)
        val btnSaveSheet      = sheetView.findViewById<Button>(R.id.btnSaveSheet)

        val isEditMode = existingSlot != null
        tvSheetTitle.text    = if (isEditMode) "Edit Lecture — $selectedDay" else "Add Lecture — $selectedDay"
        tvSheetSubtitle.text = if (isEditMode) "Update lecture details below" else "Fill in lecture details below"
        btnSaveSheet.text    = if (isEditMode) "Update ✅" else "Save ✅"

        if (isEditMode && existingSlot != null) {
            etTimeFrom.setText(existingSlot.timeFrom)
            etTimeTo.setText(existingSlot.timeTo)
            etRoomNo.setText(existingSlot.roomNo)
        }

        val subjectList = mutableListOf<SubjectItem>()
        val facultyList = mutableListOf<FacultyItem>()
        var selectedSubject: SubjectItem? = null
        var selectedFaculty: FacultyItem? = null

        val openFromPicker = {
            val current = parseTime(etTimeFrom.text.toString())
            TimePickerDialog(this, { _, h, m -> etTimeFrom.setText(formatTime(h, m)) }, current.first, current.second, false).show()
        }
        etTimeFrom.setOnClickListener { openFromPicker() }
        tilTimeFrom.setEndIconOnClickListener { openFromPicker() }

        val openToPicker = {
            val current = parseTime(etTimeTo.text.toString())
            TimePickerDialog(this, { _, h, m -> etTimeTo.setText(formatTime(h, m)) }, current.first, current.second, false).show()
        }
        etTimeTo.setOnClickListener { openToPicker() }
        tilTimeTo.setEndIconOnClickListener { openToPicker() }

        progressSheet.visibility = View.VISIBLE
        btnSaveSheet.isEnabled   = false

        db.collection("subjects")
            .whereEqualTo("courseId", session.getCourseId())
            .whereEqualTo("year",     year.toIntOrNull() ?: 1)
            .whereEqualTo("semester", semester.toIntOrNull() ?: 1)
            .whereEqualTo("status",   "Active")
            .get()
            .addOnSuccessListener { subjectSnap ->
                subjectList.clear()
                subjectSnap.documents.forEach { doc ->
                    val name = doc.getString("name") ?: ""
                    val code = doc.getString("subjectId") ?: ""
                    if (name.isNotEmpty()) subjectList.add(SubjectItem(doc.id, name, code))
                }
                subjectList.sortBy { it.name }
                val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, subjectList)
                ddSubject.setAdapter(subjectAdapter)

                if (isEditMode && existingSlot != null) {
                    ddSubject.setText(existingSlot.subjectName, false)
                    etSubjectCode.setText(existingSlot.subjectCode)
                    selectedSubject = subjectList.find { it.name == existingSlot.subjectName }
                }

                db.collection("faculties").whereEqualTo("status", "Active").get()
                    .addOnSuccessListener { facultySnap ->
                        progressSheet.visibility = View.GONE
                        btnSaveSheet.isEnabled   = true
                        facultyList.clear()
                        facultySnap.documents.forEach { doc ->
                            val name = doc.getString("fullName") ?: ""
                            if (name.isNotEmpty()) facultyList.add(FacultyItem(doc.id, name))
                        }
                        facultyList.sortBy { it.name }
                        val facultyAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, facultyList)
                        ddFaculty.setAdapter(facultyAdapter)

                        if (isEditMode && existingSlot != null) {
                            ddFaculty.setText(existingSlot.facultyName, false)
                            selectedFaculty = facultyList.find { it.id == existingSlot.facultyId }
                        }
                    }
            }

        ddSubject.setOnItemClickListener { _, _, pos, _ ->
            selectedSubject = subjectList[pos]
            etSubjectCode.setText(selectedSubject?.code)
        }

        ddFaculty.setOnItemClickListener { _, _, pos, _ ->
            selectedFaculty = facultyList[pos]
            val timeFrom = etTimeFrom.text.toString().trim()
            val timeTo   = etTimeTo.text.toString().trim()
            if (timeFrom.isNotEmpty() && timeTo.isNotEmpty()) {
                checkFacultyConflict(selectedFaculty!!.id, selectedFaculty!!.name, selectedDay, timeFrom, timeTo, divisionId, existingSlot?.id ?: "") { msg ->
                    if (msg != null) {
                        tvConflictWarning.text = "⚠️ $msg"
                        tvConflictWarning.visibility = View.VISIBLE
                    }
                }
            }
        }

        btnSaveSheet.setOnClickListener {
            val timeFrom = etTimeFrom.text.toString().trim()
            val timeTo   = etTimeTo.text.toString().trim()
            val roomNo   = etRoomNo.text.toString().trim()
            if (timeFrom.isEmpty() || timeTo.isEmpty() || selectedSubject == null || selectedFaculty == null) {
                Toast.makeText(this, "⚠️ Please fill all details!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newFrom = convertTimeToMinutes(timeFrom)
            val newTo   = convertTimeToMinutes(timeTo)
            val daySlots = allSlots[selectedDay] ?: mutableListOf()

            val overlap = daySlots.find { existing ->
                if (isEditMode && existing.id == existingSlot?.id && existing.id.isNotEmpty()) return@find false
                if (isEditMode && existing.timeFrom == existingSlot?.timeFrom && existing.id.isEmpty()) return@find false
                val eF = convertTimeToMinutes(existing.timeFrom)
                val eT = convertTimeToMinutes(existing.timeTo)
                newFrom < eT && newTo > eF
            }

            if (overlap != null) {
                Toast.makeText(this, "⚠️ Overlaps with ${overlap.subjectName}!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEditMode && existingSlot != null) {
                val updated = existingSlot.copy(
                    timeFrom = timeFrom, timeTo = timeTo, subjectName = selectedSubject!!.name,
                    subjectCode = selectedSubject!!.code, facultyName = selectedFaculty!!.name,
                    facultyId = selectedFaculty!!.id, roomNo = roomNo
                )
                val idx = daySlots.indexOfFirst { if (it.id.isNotEmpty()) it.id == existingSlot.id else it.timeFrom == existingSlot.timeFrom }
                if (idx != -1) daySlots[idx] = updated
            } else {
                // ✅ NEW SLOT HAS EMPTY ID
                allSlots[selectedDay]?.add(TimeSlot("", selectedDay, timeFrom, timeTo, selectedSubject!!.name, selectedSubject!!.code, selectedFaculty!!.name, selectedFaculty!!.id, roomNo))
            }
            allSlots[selectedDay]?.sortBy { convertTimeToMinutes(it.timeFrom) }
            showSlotsForDay(selectedDay)
            bottomSheet.dismiss()
        }

        btnCancelSheet.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }

    private fun checkFacultyConflict(facultyId: String, facultyName: String, day: String, timeFrom: String, timeTo: String, curDivId: String, exSlotId: String, onResult: (String?) -> Unit) {
        db.collectionGroup("timetable").whereEqualTo("facultyId", facultyId).whereEqualTo("day", day).get().addOnSuccessListener { snap ->
            val nF = convertTimeToMinutes(timeFrom)
            val nT = convertTimeToMinutes(timeTo)
            val conflict = snap.documents.find { doc ->
                if (doc.reference.parent.parent?.id == curDivId || doc.id == exSlotId) return@find false
                val eF = convertTimeToMinutes(doc.getString("timeFrom") ?: "")
                val eT = convertTimeToMinutes(doc.getString("timeTo") ?: "")
                nF < eT && nT > eF
            }
            if (conflict != null) {
                val conflictDivId = conflict.reference.parent.parent?.id ?: ""
                db.collection("divisions").document(conflictDivId).get().addOnSuccessListener { d ->
                    onResult("$facultyName is already teaching in ${d.getString("divisionName") ?: "another division"}")
                }
            } else onResult(null)
        }
    }

    private fun saveTimetable() {
        val divRef = db.collection("divisions").document(divisionId)
        btnSave.isEnabled = false
        btnSave.text      = "Saving..."
        showBlockingLoader("Saving timetable...")

        val batch = db.batch()
        var total = 0

        // ✅ SMART UPDATE: ADD OR UPDATE DOCUMENTS WITHOUT DELETING ALL
        allSlots.forEach { (_, slots) ->
            slots.forEach { slot ->
                if (slot.subjectName.isNotEmpty() && slot.timeFrom.isNotEmpty()) {
                    val slotRef = if (slot.id.isNotEmpty()) {
                        divRef.collection("timetable").document(slot.id) // ✅ Update existing
                    } else {
                        divRef.collection("timetable").document() // ✅ Create new
                    }
                    batch.set(slotRef, hashMapOf("day" to slot.day, "timeFrom" to slot.timeFrom, "timeTo" to slot.timeTo, "subjectName" to slot.subjectName, "subjectCode" to slot.subjectCode, "facultyName" to slot.facultyName, "facultyId" to slot.facultyId, "roomNo" to slot.roomNo, "slotType" to slot.slotType))
                    total++
                }
            }
        }

        // ✅ ONLY DELETE THE DOCUMENTS REMOVED BY USER
        deletedSlotIds.forEach { id ->
            batch.delete(divRef.collection("timetable").document(id))
        }

        batch.update(divRef, "hasTimetable", total > 0)

        batch.commit().addOnSuccessListener {
            hideBlockingLoader()
            btnSave.isEnabled = true
            btnSave.text      = "💾 Save Timetable"
            Toast.makeText(this, "✅ Timetable saved! ($total lectures)", Toast.LENGTH_SHORT).show()
            deletedSlotIds.clear() // ✅ Reset tracker
            setResult(RESULT_OK)
            finish()
        }.addOnFailureListener { e ->
            hideBlockingLoader()
            btnSave.isEnabled = true
            btnSave.text      = "💾 Save Timetable"
            Toast.makeText(this, "❌ Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun convertTimeToMinutes(time: String): Int {
        return try {
            val clean = time.trim().uppercase(Locale.getDefault())
            val isPM  = clean.contains("PM")
            val isAM  = clean.contains("AM")
            val timePart = clean.replace("AM", "").replace("PM", "").trim()
            val parts = timePart.split(":")
            var hour  = parts[0].trim().toInt()
            val min   = if (parts.size > 1) parts[1].trim().toInt() else 0
            if (isPM && hour != 12) hour += 12
            if (isAM && hour == 12) hour = 0
            hour * 60 + min
        } catch (e: Exception) { 0 }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h12 = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
        return String.format("%d:%02d %s", h12, minute, amPm)
    }

    private fun parseTime(time: String): Pair<Int, Int> {
        return try {
            val clean = time.trim().uppercase(Locale.getDefault())
            val isPM = clean.contains("PM")
            val isAM = clean.contains("AM")
            val timePart = clean.replace("AM","").replace("PM","").trim()
            val parts = timePart.split(":")
            var h = parts[0].trim().toInt()
            val m = if (parts.size > 1) parts[1].trim().toInt() else 0
            if (isPM && h != 12) h += 12
            if (isAM && h == 12) h = 0
            Pair(h, m)
        } catch (e: Exception) { Pair(9, 0) }
    }
}
