package com.example.collegemanagementsystemadmin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.collegemanagementsystemadmin.models.Division
import com.example.collegemanagementsystemadmin.models.RollRange
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AdminAddDivisionActivity : CoreBaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var mode: String = "add"
    private var divisionId: String? = null

    private lateinit var topBar: MaterialToolbar
    private lateinit var scroll: ScrollView

    // Basic Info
    private lateinit var etDivisionName: EditText
    private lateinit var ddCourse: AutoCompleteTextView
    private lateinit var ddYear: AutoCompleteTextView
    private lateinit var ddSemester: AutoCompleteTextView
    private lateinit var etCapacity: EditText

    // Roll Ranges
    private lateinit var etRollFrom: EditText
    private lateinit var etRollTo: EditText
    private lateinit var btnAddRange: Button
    private lateinit var chipGroupRollRanges: ChipGroup
    private lateinit var tvRangesLabel: TextView
    private lateinit var tvTotalRolls: TextView

    // Class Teacher
    private lateinit var ddClassTeacher: AutoCompleteTextView

    // Status
    private lateinit var ddStatus: AutoCompleteTextView

    // Action Buttons
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    data class CourseItem(
        val id: String,
        val name: String,
        val code: String,
        val durationYears: Int
    ) {
        override fun toString(): String = "$name ($code)"
    }

    data class FacultyItem(val id: String, val name: String) {
        override fun toString(): String = name
    }

    private val courses = mutableListOf<CourseItem>()
    private val faculties = mutableListOf<FacultyItem>()
    private var selectedCourse: CourseItem? = null
    private var selectedFaculty: FacultyItem? = null
    private val rollRanges = mutableListOf<RollRange>()

    // ✅ NEW: Pending save params (used after reassign completes)
    private var pendingSaveDivisionName = ""
    private var pendingSaveCourse: CourseItem? = null
    private var pendingSaveYear = ""
    private var pendingSaveSemester = ""
    private var pendingSaveCapacity = 0
    private var pendingSaveStatus = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_division)

        mode = intent.getStringExtra("mode") ?: "add"
        divisionId = intent.getStringExtra("divisionId")

        bindViews()
        setupToolbar()
        setupStaticDropdowns()
        setupButtons()
        loadCourses()
        loadFaculties()

        if (mode != "add") {
            topBar.title = if (mode == "view") "View Division" else "Edit Division"
            loadDivisionById()
        }

        applyModeLocks()
    }

    private fun bindViews() {
        topBar = findViewById(R.id.topBar)
        scroll = findViewById(R.id.scrollViewRoot)

        etDivisionName = findViewById(R.id.etDivisionName)
        ddCourse = findViewById(R.id.ddCourse)
        ddYear = findViewById(R.id.ddYear)
        ddSemester = findViewById(R.id.ddSemester)
        etCapacity = findViewById(R.id.etCapacity)

        etRollFrom = findViewById(R.id.etRollFrom)
        etRollTo = findViewById(R.id.etRollTo)
        btnAddRange = findViewById(R.id.btnAddRange)
        chipGroupRollRanges = findViewById(R.id.chipGroupRollRanges)
        tvRangesLabel = findViewById(R.id.tvRangesLabel)
        tvTotalRolls = findViewById(R.id.tvTotalRolls)

        ddClassTeacher = findViewById(R.id.ddClassTeacher)
        ddStatus = findViewById(R.id.ddStatus)

        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        listOf(ddCourse, ddYear, ddSemester, ddClassTeacher, ddStatus).forEach { v ->
            v.setOnClickListener { v.showDropDown() }
        }
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener { finish() }
    }

    private fun setupStaticDropdowns() {
        ddYear.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, emptyList<String>())
        )
        ddSemester.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, emptyList<String>())
        )
        ddStatus.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("Active", "Inactive"))
        )
        ddStatus.setText("Active", false)
    }

    private fun setupButtons() {
        btnAddRange.setOnClickListener { addRollRange() }
        btnSave.setOnClickListener { onSave() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun applyModeLocks() {
        val isView = mode == "view"
        if (!isView) return

        val allFields = listOf<View>(
            etDivisionName, ddCourse, ddYear, ddSemester, etCapacity,
            etRollFrom, etRollTo, ddClassTeacher, ddStatus
        )
        allFields.forEach { v ->
            v.isEnabled = false
            if (v is EditText) {
                v.isFocusable = false
                v.isFocusableInTouchMode = false
                v.isCursorVisible = false
            }
            if (v is AutoCompleteTextView) {
                v.isFocusable = false
                v.isFocusableInTouchMode = false
                v.isClickable = false
                v.setOnClickListener(null)
            }
        }

        listOf(
            R.id.tilDivisionName, R.id.tilCourse, R.id.tilYear,
            R.id.tilSemester, R.id.tilCapacity, R.id.tilRollFrom,
            R.id.tilRollTo, R.id.tilClassTeacher, R.id.tilStatus
        ).forEach { id ->
            findViewById<TextInputLayout>(id)?.isEnabled = false
        }

        btnAddRange.isEnabled = false
        btnSave.visibility = View.GONE
        btnCancel.text = "Close"
    }

    private fun loadCourses() {
        db.collection("courses").get()
            .addOnSuccessListener { snap ->
                courses.clear()
                snap.documents
                    .filter { d -> d.getString("status") == "Active" }
                    .sortedBy { d -> d.getString("name").orEmpty() }
                    .forEach { d ->
                        val durationYears = when (val v = d.get("durationYears")) {
                            is Number -> v.toInt()
                            is String -> v.toIntOrNull() ?: 3
                            else -> 3
                        }
                        courses.add(
                            CourseItem(
                                d.id,
                                d.getString("name").orEmpty(),
                                d.getString("code").orEmpty(),
                                durationYears
                            )
                        )
                    }
                ddCourse.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_list_item_1, courses)
                )
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to load courses: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }

        ddCourse.setOnItemClickListener { _, _, pos, _ ->
            selectedCourse = courses.getOrNull(pos)
            selectedCourse?.let { updateYearSemForCourse(it) }
        }
    }

    private fun updateYearSemForCourse(course: CourseItem) {
        val years = (1..course.durationYears.coerceAtLeast(1)).map { it.toString() }
        ddYear.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, years)
        )
        ddYear.setText("", false)
        ddSemester.setText("", false)

        ddYear.setOnItemClickListener { _, _, _, _ ->
            val yearNo = ddYear.text?.toString()?.trim()?.toIntOrNull() ?: 1
            val sems = listOf(
                ((yearNo * 2) - 1).toString(),
                (yearNo * 2).toString()
            )
            ddSemester.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_list_item_1, sems)
            )
            ddSemester.setText("", false)
        }
    }

    private fun loadFaculties() {
        db.collection("faculties")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { snap ->
                faculties.clear()
                faculties.add(FacultyItem("", "None"))
                snap.documents
                    .sortedBy { d -> d.getString("fullName").orEmpty() }
                    .forEach { d ->
                        faculties.add(FacultyItem(d.id, d.getString("fullName").orEmpty()))
                    }
                ddClassTeacher.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_list_item_1, faculties)
                )
                ddClassTeacher.setText("None", false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to load faculties: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }

        ddClassTeacher.setOnItemClickListener { _, _, pos, _ ->
            selectedFaculty = faculties.getOrNull(pos)
        }
    }

    private fun addRollRange() {
        val fromText = etRollFrom.text.toString().trim()
        val toText = etRollTo.text.toString().trim()

        if (fromText.isEmpty()) {
            Toast.makeText(this, "Enter 'From' roll number", Toast.LENGTH_SHORT).show()
            etRollFrom.requestFocus(); return
        }
        val from = fromText.toIntOrNull()
        if (from == null) {
            Toast.makeText(this, "Invalid 'From' roll number", Toast.LENGTH_SHORT).show()
            etRollFrom.requestFocus(); return
        }
        val to = if (toText.isEmpty()) from else toText.toIntOrNull()
        if (to == null) {
            Toast.makeText(this, "Invalid 'To' roll number", Toast.LENGTH_SHORT).show()
            etRollTo.requestFocus(); return
        }
        if (from > to) {
            Toast.makeText(this, "'From' cannot be greater than 'To'", Toast.LENGTH_SHORT).show()
            return
        }

        val newRange = RollRange(from, to)
        for (existingRange in rollRanges) {
            for (roll in from..to) {
                if (existingRange.containsRoll(roll)) {
                    Toast.makeText(
                        this,
                        "Roll $roll already exists in range ${existingRange.toDisplayString()}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
            }
        }

        rollRanges.add(newRange)
        etRollFrom.text?.clear()
        etRollTo.text?.clear()
        updateRollRangesDisplay()
        Toast.makeText(this, "✅ Range added: ${newRange.toDisplayString()}", Toast.LENGTH_SHORT)
            .show()
    }

    private fun updateRollRangesDisplay() {
        chipGroupRollRanges.removeAllViews()

        if (rollRanges.isEmpty()) {
            tvRangesLabel.visibility = View.GONE
            tvTotalRolls.visibility = View.GONE
            return
        }

        tvRangesLabel.visibility = View.VISIBLE
        tvTotalRolls.visibility = View.VISIBLE

        rollRanges.forEachIndexed { index, range ->
            val chip = Chip(this)
            chip.text = range.toDisplayString()
            chip.isCloseIconVisible = mode != "view"
            chip.setChipBackgroundColorResource(R.color.colorPrimary)
            chip.setTextColor(getColor(android.R.color.white))
            chip.setCloseIconTintResource(android.R.color.white)
            chip.textSize = 13f
            chip.setOnCloseIconClickListener {
                rollRanges.removeAt(index)
                updateRollRangesDisplay()
                Toast.makeText(this, "Range removed", Toast.LENGTH_SHORT).show()
            }
            chipGroupRollRanges.addView(chip)
        }

        val totalRolls = rollRanges.sumOf { it.end - it.start + 1 }
        tvTotalRolls.text = "Total Rolls Assigned: $totalRolls"
    }

    private fun loadDivisionById() {
        val id = divisionId ?: return
        findViewById<View>(R.id.blockingOverlay)?.visibility = View.VISIBLE

        db.collection("divisions").document(id).get()
            .addOnSuccessListener { d ->
                if (!d.exists()) {
                    Toast.makeText(this, "Division not found", Toast.LENGTH_LONG).show()
                    finish(); return@addOnSuccessListener
                }
                val division = d.toObject(Division::class.java) ?: return@addOnSuccessListener

                etDivisionName.setText(division.divisionName)
                etCapacity.setText(division.capacity.toString())
                ddYear.setText(division.year, false)
                ddSemester.setText(division.semester, false)
                ddStatus.setText(division.status, false)

                val courseIdx = courses.indexOfFirst { it.id == division.courseId }
                if (courseIdx >= 0) {
                    selectedCourse = courses[courseIdx]
                    ddCourse.setText(courses[courseIdx].toString(), false)
                }

                if (!division.classTeacherId.isNullOrEmpty()) {
                    val facultyIdx = faculties.indexOfFirst { it.id == division.classTeacherId }
                    if (facultyIdx >= 0) {
                        selectedFaculty = faculties[facultyIdx]
                        ddClassTeacher.setText(faculties[facultyIdx].name, false)
                    }
                }

                rollRanges.clear()
                rollRanges.addAll(division.rollNumberRanges)
                updateRollRangesDisplay()

                findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
                Toast.makeText(this, "Load failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun onSave() {
        val divisionName = etDivisionName.text.toString().trim().uppercase()
        val course = selectedCourse
        val year = ddYear.text.toString().trim()
        val semester = ddSemester.text.toString().trim()
        val capacityText = etCapacity.text.toString().trim()
        val status = ddStatus.text.toString().trim()

        if (divisionName.isEmpty()) {
            Toast.makeText(this, "⚠️ Enter division name", Toast.LENGTH_SHORT).show()
            etDivisionName.requestFocus(); return
        }
        if (course == null) {
            Toast.makeText(this, "⚠️ Select a course", Toast.LENGTH_SHORT).show()
            ddCourse.requestFocus(); return
        }
        if (year.isEmpty()) {
            Toast.makeText(this, "⚠️ Select year", Toast.LENGTH_SHORT).show()
            ddYear.requestFocus(); return
        }
        if (semester.isEmpty()) {
            Toast.makeText(this, "⚠️ Select semester", Toast.LENGTH_SHORT).show()
            ddSemester.requestFocus(); return
        }
        if (capacityText.isEmpty()) {
            Toast.makeText(this, "⚠️ Enter capacity", Toast.LENGTH_SHORT).show()
            etCapacity.requestFocus(); return
        }
        val capacity = capacityText.toIntOrNull()
        if (capacity == null || capacity <= 0) {
            Toast.makeText(this, "⚠️ Invalid capacity", Toast.LENGTH_SHORT).show()
            etCapacity.requestFocus(); return
        }
        if (rollRanges.isEmpty()) {
            Toast.makeText(this, "⚠️ Add at least one roll range", Toast.LENGTH_SHORT).show()
            etRollFrom.requestFocus(); return
        }

        val totalRolls = rollRanges.sumOf { it.end - it.start + 1 }
        if (totalRolls > capacity) {
            Toast.makeText(
                this,
                "❌ Total rolls ($totalRolls) cannot exceed capacity ($capacity)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // ✅ Store pending save params
        pendingSaveDivisionName = divisionName
        pendingSaveCourse = course
        pendingSaveYear = year
        pendingSaveSemester = semester
        pendingSaveCapacity = capacity
        pendingSaveStatus = status

        if (mode == "edit" && divisionId != null) {
            checkRollRangeChangeImpact(divisionName, course, year, semester, capacity, status)
        } else {
            findViewById<View>(R.id.blockingOverlay)?.visibility = View.VISIBLE
            checkRollOverlapAndSave(divisionName, course, year, semester, capacity, status)
        }
    }

    // ─── STEP 1: Check impact of roll range change ───────────────────────────
    private fun checkRollRangeChangeImpact(
        divisionName: String,
        course: CourseItem,
        year: String,
        semester: String,
        capacity: Int,
        status: String
    ) {
        val id = divisionId ?: return
        findViewById<View>(R.id.blockingOverlay)?.visibility = View.VISIBLE

        db.collection("divisions").document(id).get()
            .addOnSuccessListener { doc ->

                val oldRangesData = doc.get("rollNumberRanges")
                        as? List<Map<String, Any>> ?: emptyList()

                val oldRollSet = mutableSetOf<Int>()
                oldRangesData.forEach { rm ->
                    val s = (rm["start"] as? Long)?.toInt() ?: 0
                    val e = (rm["end"] as? Long)?.toInt() ?: 0
                    for (i in s..e) oldRollSet.add(i)
                }

                val newRollSet = mutableSetOf<Int>()
                rollRanges.forEach { range ->
                    for (i in range.start..range.end) newRollSet.add(i)
                }

                if (oldRollSet == newRollSet) {
                    // ✅ No range change → save directly
                    findAffectedStudentsAndUpdate(
                        divisionName, course, year, semester,
                        capacity, status,
                        affectedCount = 0, proceedDirectly = true
                    )
                    return@addOnSuccessListener
                }

                db.collection("students")
                    .whereEqualTo("divisionId", id)
                    .get()
                    .addOnSuccessListener { studentsSnap ->

                        val affectedStudents = studentsSnap.documents.filter { studentDoc ->
                            val rollNum = extractRollNumber(
                                studentDoc.getString("rollNo") ?: ""
                            )
                            rollNum > 0 && !newRollSet.contains(rollNum)
                        }

                        findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE

                        if (affectedStudents.isEmpty()) {
                            findAffectedStudentsAndUpdate(
                                divisionName, course, year, semester,
                                capacity, status,
                                affectedCount = 0, proceedDirectly = true
                            )
                        } else {
                            // ✅ Show warning with affected count
                            androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("⚠️ Roll Range Changed!")
                                .setMessage(
                                    "${affectedStudents.size} student(s) in this division\n" +
                                            "will be affected by this roll range change.\n\n" +
                                            "You will be asked to reassign them."
                                )
                                .setPositiveButton("Yes, Continue") { _, _ ->
                                    findViewById<View>(R.id.blockingOverlay)
                                        ?.visibility = View.VISIBLE
                                    findAffectedStudentsAndUpdate(
                                        divisionName, course, year, semester,
                                        capacity, status,
                                        affectedCount = affectedStudents.size,
                                        proceedDirectly = false
                                    )
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                    .addOnFailureListener {
                        findAffectedStudentsAndUpdate(
                            divisionName, course, year, semester,
                            capacity, status,
                            affectedCount = 0, proceedDirectly = true
                        )
                    }
            }
            .addOnFailureListener {
                findViewById<View>(R.id.blockingOverlay)?.visibility = View.VISIBLE
                checkRollOverlapAndSave(divisionName, course, year, semester, capacity, status)
            }
    }

    // ─── STEP 2: Find affected students → show reassign dialog ───────────────
    private fun findAffectedStudentsAndUpdate(
        divisionName: String,
        course: CourseItem,
        year: String,
        semester: String,
        capacity: Int,
        status: String,
        affectedCount: Int,
        proceedDirectly: Boolean
    ) {
        if (proceedDirectly || affectedCount == 0) {
            checkRollOverlapAndSave(divisionName, course, year, semester, capacity, status)
            return
        }

        val id = divisionId ?: return

        // ✅ Build new roll set for THIS division
        val newRollSet = mutableSetOf<Int>()
        rollRanges.forEach { range ->
            for (i in range.start..range.end) newRollSet.add(i)
        }

        // ✅ Get affected students
        db.collection("students")
            .whereEqualTo("divisionId", id)
            .get()
            .addOnSuccessListener { studentsSnap ->

                val affectedStudents = studentsSnap.documents.filter { doc ->
                    val rollNum = extractRollNumber(doc.getString("rollNo") ?: "")
                    rollNum > 0 && !newRollSet.contains(rollNum)
                }

                if (affectedStudents.isEmpty()) {
                    checkRollOverlapAndSave(divisionName, course, year, semester, capacity, status)
                    return@addOnSuccessListener
                }

                // ✅ Get all OTHER divisions for same course + year + sem
                db.collection("divisions")
                    .whereEqualTo("courseId", course.id)
                    .whereEqualTo("year", year)
                    .whereEqualTo("semester", semester)
                    .get()
                    .addOnSuccessListener { allDivSnap ->

                        val otherDivisions = allDivSnap.documents.filter { doc ->
                            doc.id != id
                        }

                        findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE

                        if (otherDivisions.isEmpty()) {
                            // ✅ No other divisions → show steps dialog
                            showNoDivisionAvailableDialog(
                                affectedStudents,
                                divisionName, course,
                                year, semester, capacity, status
                            )
                        } else {
                            // ✅ Show reassign dialog with division list
                            showReassignDialog(
                                affectedStudents,
                                otherDivisions,
                                divisionName, course,
                                year, semester, capacity, status
                            )
                        }
                    }
                    .addOnFailureListener {
                        checkRollOverlapAndSave(
                            divisionName,
                            course,
                            year,
                            semester,
                            capacity,
                            status
                        )
                    }
            }
            .addOnFailureListener {
                checkRollOverlapAndSave(divisionName, course, year, semester, capacity, status)
            }
    }

    // ─── STEP 3A: Show reassign dialog (other divisions exist) ───────────────
    private fun showReassignDialog(
        affectedStudents: List<com.google.firebase.firestore.DocumentSnapshot>,
        otherDivisions: List<com.google.firebase.firestore.DocumentSnapshot>,
        divisionName: String,
        course: CourseItem,
        year: String,
        semester: String,
        capacity: Int,
        status: String
    ) {
        val divisionNames = otherDivisions
            .map { doc -> doc.getString("divisionName") ?: "Unknown" }
            .toMutableList()

        // ✅ Add "Create New Division" as last option
        divisionNames.add("➕ Create New Division")

        val divisionNamesArray = divisionNames.toTypedArray()

        val studentList = affectedStudents.joinToString("\n") { doc ->
            "• ${doc.getString("fullName")} (${doc.getString("rollNo")})"
        }

        var selectedIndex = 0

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ ${affectedStudents.size} Students Affected!")
            .setMessage(
                "These students will leave current division:\n\n" +
                        "$studentList\n\n" +
                        "Select a new division for them:"
            )
            .setSingleChoiceItems(divisionNamesArray, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Reassign & Save") { _, _ ->

                // ✅ Admin chose "Create New Division"
                if (selectedIndex == divisionNamesArray.size - 1) {
                    showCreateDivisionStepsDialog(affectedStudents)
                    return@setPositiveButton
                }

                // ✅ Admin chose an existing division
                val targetDoc = otherDivisions[selectedIndex]
                val targetId = targetDoc.id
                val targetName = targetDoc.getString("divisionName") ?: ""

                findViewById<View>(R.id.blockingOverlay)?.visibility = View.VISIBLE

                val batch = db.batch()
                affectedStudents.forEach { doc ->
                    batch.update(
                        db.collection("students").document(doc.id),
                        mapOf(
                            "divisionId" to targetId,
                            "divisionName" to targetName
                        )
                    )
                }

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "✅ ${affectedStudents.size} student(s) moved to $targetName",
                            Toast.LENGTH_SHORT
                        ).show()
                        checkRollOverlapAndSave(
                            divisionName, course, year, semester, capacity, status
                        )
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "❌ Reassign failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
                    }
            }
            .setNegativeButton("Cancel") { _, _ ->
                findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
            }
            .show()
    }

    // ─── STEP 3B: No other divisions → show steps dialog ─────────────────────
    // ─── STEP 3B: No other divisions → show steps dialog ─────────────────────
    private fun showNoDivisionAvailableDialog(
        affectedStudents: List<com.google.firebase.firestore.DocumentSnapshot>,
        divisionName: String,
        course: CourseItem,
        year: String,
        semester: String,
        capacity: Int,
        status: String
    ) {
        val studentList = affectedStudents.joinToString("\n") { doc ->
            "• ${doc.getString("fullName")} (${doc.getString("rollNo")})"
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ No Other Divisions Available!")
            .setMessage(
                "${affectedStudents.size} student(s) will be affected:\n\n" +
                        "$studentList\n\n" +
                        "📋 Steps to Fix:\n\n" +
                        "STEP 1️⃣ → Click 'OK, Cancel Edit'\n" +
                        "STEP 2️⃣ → Go to Division List\n" +
                        "STEP 3️⃣ → Create a New Division\n" +
                        "STEP 4️⃣ → Come back & Edit this division again\n" +
                        "STEP 5️⃣ → Reassign students to new division ✅"
            )
            .setPositiveButton("OK, Cancel Edit") { _, _ ->
                // ✅ Block the save — force admin to create division first
                findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
            }
            // ✅ REMOVED: "Save Anyway (Unassign Students)" button
            .setCancelable(false)  // ✅ Admin MUST click "OK, Cancel Edit"
            .show()
    }


    // ─── STEP 3C: Admin selected "Create New Division" ───────────────────────
    private fun showCreateDivisionStepsDialog(
        affectedStudents: List<com.google.firebase.firestore.DocumentSnapshot>
    ) {
        val studentList = affectedStudents.joinToString("\n") { doc ->
            "• ${doc.getString("fullName")} (${doc.getString("rollNo")})"
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📋 Create New Division First!")
            .setMessage(
                "You selected 'Create New Division'.\n\n" +
                        "Please follow these steps:\n\n" +
                        "STEP 1️⃣ → Click 'OK, Go Back'\n" +
                        "STEP 2️⃣ → Go to Division List\n" +
                        "STEP 3️⃣ → Create the New Division\n" +
                        "           (with correct roll range)\n" +
                        "STEP 4️⃣ → Come back & Edit this division again\n" +
                        "STEP 5️⃣ → New division will appear\n" +
                        "           in the reassign list ✅\n\n" +
                        "Affected students:\n$studentList"
            )
            .setPositiveButton("OK, Go Back") { _, _ ->
                // ✅ Don't save → admin creates division first
                findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
            }
            .show()
    }

    // ─── STEP 4: Check roll overlap with other divisions ─────────────────────
    private fun checkRollOverlapAndSave(
        divisionName: String,
        course: CourseItem,
        year: String,
        semester: String,
        capacity: Int,
        status: String
    ) {
        val newRolls = mutableSetOf<Int>()
        rollRanges.forEach { r ->
            for (i in r.start..r.end) newRolls.add(i)
        }

        db.collection("divisions")
            .whereEqualTo("courseId", course.id)
            .whereEqualTo("year", year)
            .whereEqualTo("semester", semester)
            .get()
            .addOnSuccessListener { snap ->
                val usedRolls = mutableSetOf<Int>()
                snap.documents.forEach { doc ->
                    if (mode == "edit" && doc.id == divisionId) return@forEach
                    val rangesData = doc.get("rollNumberRanges")
                            as? List<Map<String, Any>> ?: emptyList()
                    rangesData.forEach { rm ->
                        val s = (rm["start"] as? Long)?.toInt() ?: 0
                        val e = (rm["end"] as? Long)?.toInt() ?: 0
                        for (i in s..e) usedRolls.add(i)
                    }
                }

                val conflict = newRolls.firstOrNull { it in usedRolls }
                if (conflict != null) {
                    findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "❌ Roll $conflict already assigned to another division",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                proceedSaveDivision(
                    divisionName, course.id, year, semester,
                    capacity, status, course.name, course.code
                )
            }
            .addOnFailureListener { e ->
                findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
                Toast.makeText(this, "❌ Validation failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ─── STEP 5: Save to Firestore ────────────────────────────────────────────
    private fun proceedSaveDivision(
        divisionName: String,
        courseId: String,
        year: String,
        semester: String,
        capacity: Int,
        status: String,
        courseName: String,
        courseCode: String
    ) {
        db.collection("divisions")
            .whereEqualTo("divisionName", divisionName)
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("year", year)
            .whereEqualTo("semester", semester)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty && mode == "add") {
                    findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "❌ Division $divisionName already exists",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val data = hashMapOf<String, Any?>(
                    "divisionName" to divisionName,
                    "courseId" to courseId,
                    "courseName" to courseName,
                    "courseCode" to courseCode,
                    "year" to year,
                    "semester" to semester,
                    "capacity" to capacity,
                    "currentStrength" to 0,
                    "rollNumberRanges" to rollRanges.map {
                        mapOf("start" to it.start, "end" to it.end)
                    },
                    "classTeacherId" to (selectedFaculty?.id ?: ""),
                    "classTeacherName" to (selectedFaculty?.name ?: ""),
                    "classTeacherEmail" to null,
                    "status" to status,
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )

                if (mode == "edit" && divisionId != null) {
                    db.collection("divisions").document(divisionId!!)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
                            Toast.makeText(this, "✅ Division updated", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
                            Toast.makeText(this, "❌ Update failed: ${e.message}", Toast.LENGTH_LONG)
                                .show()
                        }
                } else {
                    db.collection("divisions").add(data)
                        .addOnSuccessListener {
                            findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
                            Toast.makeText(this, "✅ Division saved", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
                            Toast.makeText(this, "❌ Save failed: ${e.message}", Toast.LENGTH_LONG)
                                .show()
                        }
                }
            }
            .addOnFailureListener { e ->
                findViewById<View>(R.id.blockingOverlay)?.visibility = View.GONE
                Toast.makeText(this, "❌ Validation failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun extractRollNumber(rollNo: String): Int {
        return try {
            rollNo.takeLast(3).trimStart('0').toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
