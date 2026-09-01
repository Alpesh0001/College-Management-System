package com.example.collegemanagementsystemfaculty

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.adapters.StudentAttendanceAdapter
import com.example.collegemanagementsystemfaculty.models.StudentAttendanceItem
import com.example.collegemanagementsystemfaculty.utils.CoreBaseActivity
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import java.text.SimpleDateFormat
import java.util.Locale

class MarkAttendanceActivity : CoreBaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    private lateinit var btnBack           : View
    private lateinit var tvSubjectTitle    : TextView
    private lateinit var tvModeBadge       : TextView
    private lateinit var tvDivisionInfo    : TextView
    private lateinit var tvDateInfo        : TextView
    private lateinit var tvTotalStudents   : TextView
    private lateinit var tvPresentCount    : TextView
    private lateinit var tvAbsentCount     : TextView
    private lateinit var tvPercentage      : TextView
    private lateinit var tvStudentCount    : TextView
    private lateinit var btnMarkAllPresent : TextView
    private lateinit var btnMarkAllAbsent  : TextView
    private lateinit var rvStudents        : RecyclerView
    private lateinit var layoutLoading     : LinearLayout
    private lateinit var layoutEmpty       : LinearLayout
    private lateinit var btnSaveAttendance : MaterialButton
    // ✅ Add this field
    private var previousAbsentIds: MutableSet<String> = mutableSetOf()


    private var slotId       = ""
    private var divisionId   = ""
    private var divisionName = ""
    private var subjectName  = ""
    private var subjectCode  = ""
    private var timeFrom     = ""
    private var timeTo       = ""
    private var date         = ""
    private var day          = ""
    private var isEdit       = false

    private val studentList = mutableListOf<StudentAttendanceItem>()
    private lateinit var adapter: StudentAttendanceAdapter
    private var cachedSlots: MutableList<Map<String, Any>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mark_attendance)

        session      = SessionManager(this)
        slotId       = intent.getStringExtra("slotId")       ?: ""
        divisionId   = intent.getStringExtra("divisionId")   ?: ""
        divisionName = intent.getStringExtra("divisionName") ?: ""
        subjectName  = intent.getStringExtra("subjectName")  ?: ""
        subjectCode  = intent.getStringExtra("subjectCode")  ?: ""
        timeFrom     = intent.getStringExtra("timeFrom")     ?: ""
        timeTo       = intent.getStringExtra("timeTo")       ?: ""
        date         = intent.getStringExtra("date")         ?: ""
        day          = intent.getStringExtra("day")          ?: ""
        isEdit       = intent.getBooleanExtra("isEdit", false)

        bindViews()
        setupHeader()
        setupButtons()
        setupRecyclerView()
        loadStudents()
    }

    // ── bindViews, setupHeader, setupButtons,
    //    setupRecyclerView, loadStudents,
    //    processStudents, loadExistingAttendance,
    //    parseCachedSlots, updateSummary
    //    → ALL SAME AS BEFORE ──────────────────

    private fun bindViews() {
        btnBack            = findViewById(R.id.btnBack)
        tvSubjectTitle     = findViewById(R.id.tvSubjectTitle)
        tvModeBadge        = findViewById(R.id.tvModeBadge)
        tvDivisionInfo     = findViewById(R.id.tvDivisionInfo)
        tvDateInfo         = findViewById(R.id.tvDateInfo)
        tvTotalStudents    = findViewById(R.id.tvTotalStudents)
        tvPresentCount     = findViewById(R.id.tvPresentCount)
        tvAbsentCount      = findViewById(R.id.tvAbsentCount)
        tvPercentage       = findViewById(R.id.tvPercentage)
        tvStudentCount     = findViewById(R.id.tvStudentCount)
        btnMarkAllPresent  = findViewById(R.id.btnMarkAllPresent)
        btnMarkAllAbsent   = findViewById(R.id.btnMarkAllAbsent)
        rvStudents         = findViewById(R.id.rvStudents)
        layoutLoading      = findViewById(R.id.layoutLoading)
        layoutEmpty        = findViewById(R.id.layoutEmpty)
        btnSaveAttendance  = findViewById(R.id.btnSaveAttendance)
    }

    private fun setupHeader() {
        tvSubjectTitle.text = subjectName
        tvDivisionInfo.text = "$divisionName • $timeFrom – $timeTo"
        tvModeBadge.text    = if (isEdit) "EDIT" else "MARK"
        try {
            val inFmt  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            val parsed = inFmt.parse(date)
            tvDateInfo.text = if (parsed != null) outFmt.format(parsed) else date
        } catch (e: Exception) { tvDateInfo.text = date }
    }

    private fun setupButtons() {
        btnBack.setOnClickListener { finish() }
        btnMarkAllPresent.setOnClickListener {
            studentList.forEach { it.status = "Present" }
            adapter.notifyDataSetChanged(); updateSummary()
        }
        btnMarkAllAbsent.setOnClickListener {
            studentList.forEach { it.status = "Absent" }
            adapter.notifyDataSetChanged(); updateSummary()
        }
        btnSaveAttendance.setOnClickListener { saveAttendance() }
    }

    private fun setupRecyclerView() {
        rvStudents.layoutManager = GridLayoutManager(this, 3)
        adapter = StudentAttendanceAdapter(studentList, onToggle = { updateSummary() })
        rvStudents.adapter = adapter
    }

    private fun loadStudents() {
        showLoading(true)
        db.collection("students")
            .whereEqualTo("divisionId", divisionId)
            .whereEqualTo("status", "Active")
            .get(Source.CACHE)
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    db.collection("students")
                        .whereEqualTo("divisionId", divisionId)
                        .whereEqualTo("status", "Active")
                        .get(Source.SERVER)
                        .addOnSuccessListener { processStudents(it) }
                        .addOnFailureListener {
                            showLoading(false)
                            Toast.makeText(this, "❌ Failed: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                } else processStudents(snap)
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "❌ Failed to load", Toast.LENGTH_LONG).show()
            }
    }

    private fun processStudents(snap: com.google.firebase.firestore.QuerySnapshot) {
        if (snap.isEmpty) { showLoading(false); showEmpty(true); return }

        val students = snap.documents.mapNotNull { doc ->
            val name   = doc.getString("fullName") ?: return@mapNotNull null
            val rollNo = doc.getString("rollNo")   ?: return@mapNotNull null
            StudentAttendanceItem(
                studentId   = doc.id,
                studentName = name,
                rollNo      = rollNo,
                rollNumber  = extractRollNumber(rollNo),
                status      = "Present"
            )
        }.sortedBy { it.rollNumber }

        studentList.clear()
        studentList.addAll(students)

        loadExistingAttendance {
            showLoading(false); showEmpty(false)
            adapter.notifyDataSetChanged(); updateSummary()
        }
    }

    private fun loadExistingAttendance(onDone: () -> Unit) {
        db.collection("attendance").document(date)
            .get(Source.CACHE)
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    cachedSlots = (doc.get("slots") as? List<*>)
                        ?.filterIsInstance<Map<String, Any>>()
                        ?.toMutableList() ?: mutableListOf()
                    parseCachedSlots(); onDone()
                } else {
                    db.collection("attendance").document(date)
                        .get(Source.SERVER)
                        .addOnSuccessListener { serverDoc ->
                            if (serverDoc.exists()) {
                                cachedSlots = (serverDoc.get("slots") as? List<*>)
                                    ?.filterIsInstance<Map<String, Any>>()
                                    ?.toMutableList() ?: mutableListOf()
                                parseCachedSlots()
                            }
                            onDone()
                        }
                        .addOnFailureListener { onDone() }
                }
            }
            .addOnFailureListener { onDone() }
    }

    private fun parseCachedSlots() {
        for (slot in cachedSlots) {
            if (slot["slotId"] != slotId) continue
            val divisions = slot["divisions"] as? List<*> ?: continue
            for (division in divisions) {
                val divMap = division as? Map<*, *> ?: continue
                if (divMap["divisionId"] != divisionId) continue
                val absentList = divMap["absentStudents"] as? List<*> ?: continue
                absentList.forEach { absent ->
                    val absentMap = absent as? Map<*, *> ?: return@forEach
                    val sId = absentMap["studentId"] as? String ?: ""
                    studentList.find { it.studentId == sId }?.status = "Absent"
                }
                break
            }
            break
        }
    }

    private fun updateSummary() {
        val total   = studentList.size
        val present = studentList.count { it.status == "Present" }
        val absent  = studentList.count { it.status == "Absent" }
        val percent = if (total > 0) (present * 100) / total else 0
        tvTotalStudents.text = total.toString()
        tvPresentCount.text  = present.toString()
        tvAbsentCount.text   = absent.toString()
        tvPercentage.text    = "$percent%"
        tvStudentCount.text  = "$total students"
    }

    // ─────────────────────────────────────────────────
    // ✅ Save Attendance — UPDATED
    // ─────────────────────────────────────────────────
    private fun saveAttendance() {
        val unmarked = studentList.count {
            it.status != "Present" && it.status != "Absent"
        }
        if (unmarked > 0) {
            Toast.makeText(this, "⚠️ Please mark all students!", Toast.LENGTH_SHORT).show()
            return
        }

        showBlockingLoader("Saving...")
        btnSaveAttendance.isEnabled = false

        // ✅ CAPTURE PREVIOUS ABSENT IDs BEFORE cachedSlots is updated!
        previousAbsentIds.clear()
        if (isEdit) {
            for (slot in cachedSlots) {
                if (slot["slotId"] != slotId) continue
                val divisions = slot["divisions"] as? List<*> ?: continue
                for (div in divisions) {
                    val divMap = div as? Map<*, *> ?: continue
                    if (divMap["divisionId"] != divisionId) continue
                    val absentList = divMap["absentStudents"] as? List<*> ?: continue
                    absentList.forEach { absent ->
                        val absentMap = absent as? Map<*, *> ?: return@forEach
                        val sId = absentMap["studentId"] as? String ?: ""
                        if (sId.isNotEmpty()) previousAbsentIds.add(sId)
                    }
                    break
                }
                break
            }
        }

        // ── rest of saveAttendance() same as before ──
        val absentStudents = studentList.filter { it.status == "Absent" }
        val absentList = absentStudents.map {
            hashMapOf(
                "studentId"   to it.studentId,
                "rollNo"      to it.rollNo,
                "studentName" to it.studentName,
                "markedAt"    to Timestamp.now()
            )
        }

        val divisionData = hashMapOf(
            "divisionId"     to divisionId,
            "divisionName"   to divisionName,
            "subjectCode"    to subjectCode,
            "subjectName"    to subjectName,
            "facultyId"      to session.getFacultyId(),
            "facultyName"    to session.getFullName(),
            "totalStudents"  to studentList.size,
            "presentCount"   to (studentList.size - absentStudents.size),
            "absentCount"    to absentStudents.size,
            "isMarked"       to true,
            "absentStudents" to absentList
        )

        val newSlotEntry = hashMapOf(
            "slotId"    to slotId,
            "timeFrom"  to timeFrom,
            "timeTo"    to timeTo,
            "divisions" to listOf(divisionData)
        )

        val existingSlotIndex = cachedSlots.indexOfFirst { it["slotId"] == slotId }
        if (existingSlotIndex == -1) {
            cachedSlots.add(newSlotEntry)
        } else {
            val slot = cachedSlots[existingSlotIndex].toMutableMap()
            val divs = (slot["divisions"] as? List<*>)
                ?.filterIsInstance<Map<String, Any>>()
                ?.toMutableList() ?: mutableListOf()
            val dIdx = divs.indexOfFirst { it["divisionId"] == divisionId }
            if (dIdx == -1) divs.add(divisionData) else divs[dIdx] = divisionData
            slot["divisions"] = divs
            cachedSlots[existingSlotIndex] = slot
        }

        val data = hashMapOf("date" to date, "day" to day, "slots" to cachedSlots)

        db.collection("attendance").document(date)
            .set(data)
            .addOnSuccessListener {
                updateStudentSummary()
            }
            .addOnFailureListener { e -> onSaveFailure(e.message) }
    }


    // ─────────────────────────────────────────────────
// ✅ Step 1 — Get semester for each student
// ─────────────────────────────────────────────────
    private fun updateStudentSummary() {
        db.collection("students")
            .whereEqualTo("divisionId", divisionId)
            .whereEqualTo("status", "Active")
            .get(Source.CACHE)
            .addOnSuccessListener { snap -> processSummaryUpdate(snap) }
            .addOnFailureListener {
                db.collection("students")
                    .whereEqualTo("divisionId", divisionId)
                    .whereEqualTo("status", "Active")
                    .get(Source.SERVER)
                    .addOnSuccessListener { snap -> processSummaryUpdate(snap) }
                    .addOnFailureListener { onSaveSuccess() }
            }
    }

    // ─────────────────────────────────────────────────
// ✅ Step 2 — Build previous absent set from cachedSlots
// ─────────────────────────────────────────────────
    private fun processSummaryUpdate(
        snap: com.google.firebase.firestore.QuerySnapshot
    ) {
        if (snap.isEmpty) { onSaveSuccess(); return }

        val studentSemMap = snap.documents.associate { doc ->
            doc.id to (doc.getString("semester") ?: "1")
        }

        // ✅ Use class-level previousAbsentIds (captured BEFORE save)
        updateSubjectCounters(studentSemMap, previousAbsentIds)
    }


    // ─────────────────────────────────────────────────
// ✅ Step 3 — Update counters per student
// ─────────────────────────────────────────────────
    private fun updateSubjectCounters(
        studentSemMap   : Map<String, String>,
        previousAbsentIds: Set<String>
    ) {
        val total = studentList.size
        var done  = 0

        if (total == 0) { onSaveSuccess(); return }

        studentList.forEach { student ->
            val sem       = studentSemMap[student.studentId] ?: "1"
            val docId     = "${student.studentId}_${sem}"
            val docRef    = db.collection("studentSummary").document(docId)
            val isAbsent  = student.status == "Absent"
            val wasAbsent = previousAbsentIds.contains(student.studentId)

            db.runTransaction { transaction ->
                val snap = transaction.get(docRef)

                // ✅ Get existing subjects array
                val subjects = (snap.get("subjects") as? List<*>)
                    ?.filterIsInstance<Map<String, Any>>()
                    ?.map { it.toMutableMap() }
                    ?.toMutableList() ?: mutableListOf()

                // ✅ Find existing subject entry by subjectCode
                val subjectIndex = subjects.indexOfFirst {
                    it["subjectCode"] == subjectCode
                }

                if (subjectIndex == -1) {
                    // ✅ NEW subject entry — first time this subject is marked
                    val newSubject = mutableMapOf<String, Any>(
                        "subjectName" to subjectName,
                        "subjectCode" to subjectCode,
                        "present"     to if (isAbsent) 0 else 1,
                        "absent"      to if (isAbsent) 1 else 0,
                        "total"       to 1
                    )
                    subjects.add(newSubject)

                } else {
                    // ✅ Existing subject — adjust counters
                    val subject = subjects[subjectIndex]
                    var present = (subject["present"] as? Long)?.toInt() ?: 0
                    var absent  = (subject["absent"]  as? Long)?.toInt() ?: 0
                    var tot     = (subject["total"]   as? Long)?.toInt() ?: 0

                    when {
                        // ✅ New lecture (not edit) — just increment
                        !isEdit -> {
                            if (isAbsent) absent++ else present++
                            tot++
                        }
                        // ✅ Edit: Present → Absent
                        isEdit && !wasAbsent && isAbsent -> {
                            present = (present - 1).coerceAtLeast(0)
                            absent++
                            // total stays same ✅
                        }
                        // ✅ Edit: Absent → Present
                        isEdit && wasAbsent && !isAbsent -> {
                            absent = (absent - 1).coerceAtLeast(0)
                            present++
                            // total stays same ✅
                        }
                        // ✅ Edit: No change — skip
                        else -> {
                            // nothing to update
                        }
                    }

                    subject["present"] = present
                    subject["absent"]  = absent
                    subject["total"]   = tot
                    subjects[subjectIndex] = subject
                }

                // ✅ Recalculate overall totals from subjects
                val totalLectures = subjects.sumOf {
                    (it["total"] as? Long)?.toInt()
                        ?: (it["total"] as? Int) ?: 0
                }
                val totalPresent = subjects.sumOf {
                    (it["present"] as? Long)?.toInt()
                        ?: (it["present"] as? Int) ?: 0
                }
                val totalAbsent = subjects.sumOf {
                    (it["absent"] as? Long)?.toInt()
                        ?: (it["absent"] as? Int) ?: 0
                }

                // ✅ Write back
                transaction.set(
                    docRef,
                    mapOf(
                        "studentId"     to student.studentId,
                        "divisionId"    to divisionId,
                        "semester"      to sem,
                        "subjects"      to subjects,
                        "totalLectures" to totalLectures,
                        "totalPresent"  to totalPresent,
                        "totalAbsent"   to totalAbsent,
                        "lastUpdated"   to Timestamp.now()
                    ),
                    SetOptions.merge()
                )
            }
                .addOnSuccessListener {
                    done++
                    if (done == total) onSaveSuccess()
                }
                .addOnFailureListener {
                    done++
                    if (done == total) onSaveSuccess()
                }
        }
    }

    // ─────────────────────────────────────────────────
    // ✅ Same as before
    // ─────────────────────────────────────────────────
    private fun onSaveSuccess() {
        hideBlockingLoader()
        Toast.makeText(this, "✅ Saved!", Toast.LENGTH_SHORT).show()
        val resultIntent = android.content.Intent().apply {
            putExtra("slotId", slotId)
            putExtra("date",   date)
        }
        setResult(android.app.Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun onSaveFailure(msg: String?) {
        hideBlockingLoader()
        btnSaveAttendance.isEnabled = true
        Toast.makeText(this, "❌ Error: $msg", Toast.LENGTH_LONG).show()
    }

    private fun showLoading(show: Boolean) {
        layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        rvStudents.visibility    = if (show) View.GONE    else View.VISIBLE
        if (show) layoutEmpty.visibility = View.GONE
    }

    private fun showEmpty(show: Boolean) {
        layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        rvStudents.visibility  = if (show) View.GONE    else View.VISIBLE
    }

    private fun extractRollNumber(rollNo: String): Int {
        return try {
            rollNo.takeLast(3).trimStart('0').toIntOrNull() ?: 0
        } catch (e: Exception) { 0 }
    }
}
