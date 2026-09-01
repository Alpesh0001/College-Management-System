package com.example.collegemanagementsystemadmin

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class AdminCourseDetailsActivity : CoreBaseActivity() {

    // Views
    private lateinit var topBar: MaterialToolbar
    private lateinit var tilCourseName: TextInputLayout
    private lateinit var tilCourseCode: TextInputLayout
    private lateinit var tilDuration: TextInputLayout
    private lateinit var tilStatus: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etCode: TextInputEditText
    private lateinit var ddYears: AutoCompleteTextView
    private lateinit var ddStatus: AutoCompleteTextView
    private lateinit var btnEdit: Button
    private lateinit var btnSave: Button
    private lateinit var btnManageSubjects: Button
    private lateinit var btnDeleteCourse: Button
    private lateinit var tvSubjectCount: TextView
    private lateinit var progress: ProgressBar
    private lateinit var detailsContainer: View
    private lateinit var dangerZoneCard: View

    // ✅ Semester Fees Views
    private lateinit var btnEditFees: Button
    private lateinit var btnSaveFees: Button
    private lateinit var containerFeesView: LinearLayout
    private lateinit var containerFeesEdit: LinearLayout

    // ✅ Store edit inputs
    private val semFeeInputs = mutableMapOf<Int, TextInputEditText>()

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var courseId: String = ""
    private var subjectCount: Int = 0
    private var durationYears: Int = 3
    private var currentFeesMap = mutableMapOf<String, Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_course_details)

        courseId = intent.getStringExtra("id").orEmpty()
        if (courseId.isBlank()) {
            Toast.makeText(this, "Missing course ID", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        bindViews()
        setupToolbar()
        setupDropdowns()
        setupButtons()
        loadCourse()
        loadSubjectCount()
    }

    override fun onResume() {
        super.onResume()
        loadSubjectCount()
    }

    private fun bindViews() {
        topBar              = findViewById(R.id.topBar)
        tilCourseName       = findViewById(R.id.tilCourseName)
        tilCourseCode       = findViewById(R.id.tilCourseCode)
        tilDuration         = findViewById(R.id.tilDuration)
        tilStatus           = findViewById(R.id.tilStatus)
        etName              = findViewById(R.id.etCourseName)
        etCode              = findViewById(R.id.etCourseCode)
        ddYears             = findViewById(R.id.etDurationYears)
        ddStatus            = findViewById(R.id.ddStatus)
        btnEdit             = findViewById(R.id.btnEditCourse)
        btnSave             = findViewById(R.id.btnSaveCourse)
        btnManageSubjects   = findViewById(R.id.btnManageSubjects)
        btnDeleteCourse     = findViewById(R.id.btnDeleteCourse)
        tvSubjectCount      = findViewById(R.id.tvSubjectCount)
        progress            = findViewById(R.id.progressOverlay)
        detailsContainer    = findViewById(R.id.detailsContainer)
        dangerZoneCard      = findViewById(R.id.dangerZoneCard)

        // ✅ Fees views
        btnEditFees         = findViewById(R.id.btnEditFees)
        btnSaveFees         = findViewById(R.id.btnSaveFees)
        containerFeesView   = findViewById(R.id.containerFeesView)
        containerFeesEdit   = findViewById(R.id.containerFeesEdit)
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener { finish() }
    }

    private fun setupDropdowns() {
        ddYears.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1,
                listOf("1","2","3","4","5","6"))
        )
        ddYears.setOnClickListener { if (ddYears.isEnabled) ddYears.showDropDown() }

        ddStatus.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1,
                listOf("Active","Inactive"))
        )
        ddStatus.setOnClickListener { if (ddStatus.isEnabled) ddStatus.showDropDown() }
    }

    private fun setupButtons() {
        btnEdit.setOnClickListener { setEditMode(true) }
        btnSave.setOnClickListener { saveCourseEdits() }

        btnManageSubjects.setOnClickListener {
            startActivity(
                Intent(this, AdminSubjectsActivity::class.java).apply {
                    putExtra("courseId", courseId)
                    putExtra("courseName", etName.text.toString())
                }
            )
        }

        btnDeleteCourse.setOnClickListener { showDeleteConfirmation() }

        // ✅ Fees edit/save
        btnEditFees.setOnClickListener { setFeesEditMode(true) }
        btnSaveFees.setOnClickListener { saveFeesEdits() }
    }

    private fun setEditMode(editable: Boolean) {
        etName.isEnabled    = editable
        etCode.isEnabled    = editable
        ddYears.isEnabled   = editable
        ddStatus.isEnabled  = editable

        btnEdit.visibility  = if (editable) View.GONE else View.VISIBLE
        btnSave.visibility  = if (editable) View.VISIBLE else View.GONE
        dangerZoneCard.visibility = if (editable) View.VISIBLE else View.GONE

        // ✅ Show edit fees button only in edit mode
        btnEditFees.visibility = if (editable) View.VISIBLE else View.GONE

        // ✅ If leaving edit mode, reset fees to view mode
        if (!editable) setFeesEditMode(false)
    }

    // ─────────────────────────────────────────────
    // ✅ FEES VIEW MODE — show rows
    // ─────────────────────────────────────────────
    private fun buildFeesViewMode(feesMap: Map<String, Long>) {
        containerFeesView.removeAllViews()

        if (feesMap.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No fees configured yet"
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#666666"))
            }
            containerFeesView.addView(tv)
            return
        }

        // Sort by sem number
        val sorted = feesMap.entries.sortedBy { it.key.toIntOrNull() ?: 0 }

        for ((sem, amount) in sorted) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                p.bottomMargin = 10.dpToPx()
                layoutParams = p
            }

            val tvSem = TextView(this).apply {
                text = "Semester $sem"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvAmount = TextView(this).apply {
                text = "₹${String.format("%,d", amount)}"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(resources.getColor(R.color.colorPrimary, null))
            }

            row.addView(tvSem)
            row.addView(tvAmount)
            containerFeesView.addView(row)

            // Divider
            if (sem != sorted.last().key) {
                val div = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).also { it.bottomMargin = 10.dpToPx() }
                    setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
                }
                containerFeesView.addView(div)
            }
        }
    }

    // ─────────────────────────────────────────────
    // ✅ FEES EDIT MODE — show input fields
    // ─────────────────────────────────────────────
    private fun setFeesEditMode(editing: Boolean) {
        if (editing) {
            containerFeesView.visibility = View.GONE
            containerFeesEdit.visibility = View.VISIBLE
            btnSaveFees.visibility       = View.VISIBLE
            btnEditFees.visibility       = View.GONE
            buildFeesEditFields(currentFeesMap)
        } else {
            containerFeesView.visibility = View.VISIBLE
            containerFeesEdit.visibility = View.GONE
            btnSaveFees.visibility       = View.GONE
            btnEditFees.visibility       =
                if (btnEdit.visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    private fun buildFeesEditFields(existingFees: Map<String, Long>) {
        containerFeesEdit.removeAllViews()
        semFeeInputs.clear()

        val totalSems = durationYears * 2

        for (sem in 1..totalSems) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                p.bottomMargin = 12.dpToPx()
                layoutParams = p
            }

            val label = TextView(this).apply {
                text = "Semester $sem"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            val til = TextInputLayout(
                this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle
            ).apply {
                hint = "Amount (₹)"
                prefixText = "₹"
                layoutParams = LinearLayout.LayoutParams(
                    160.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val etFee = TextInputEditText(til.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                maxLines = 1
                // ✅ Pre-fill existing value
                val existing = existingFees[sem.toString()] ?: 0L
                if (existing > 0) setText(existing.toString())
            }

            til.addView(etFee)
            row.addView(label)
            row.addView(til)
            containerFeesEdit.addView(row)
            semFeeInputs[sem] = etFee
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Save updated fees to Firestore
    // ─────────────────────────────────────────────
    private fun saveFeesEdits() {
        val newFees = mutableMapOf<String, Long>()

        for ((sem, input) in semFeeInputs) {
            val value = input.text.toString().trim()
            if (value.isEmpty()) {
                input.error = "Required"
                Toast.makeText(this, "Enter fees for Semester $sem", Toast.LENGTH_SHORT).show()
                return
            }
            newFees[sem.toString()] = value.toLongOrNull() ?: 0L
        }

        setLoading(true)

        db.collection("courses").document(courseId)
            .update("semesterFees", newFees)
            .addOnSuccessListener {
                setLoading(false)
                currentFeesMap = newFees
                buildFeesViewMode(newFees)
                setFeesEditMode(false)
                Toast.makeText(this, "✅ Fees updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "❌ ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    // ─────────────────────────────────────────────
    // Load Course
    // ─────────────────────────────────────────────
    private fun loadCourse() {
        setLoading(true)

        db.collection("courses").document(courseId).get()
            .addOnSuccessListener { doc ->
                setLoading(false)

                if (!doc.exists()) {
                    Toast.makeText(this, "Course not found", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val name   = doc.getString("name").orEmpty()
                val code   = doc.getString("code").orEmpty()
                val status = doc.getString("status").orEmpty()
                durationYears = when (val raw = doc.get("durationYears")) {
                    is Number -> raw.toInt()
                    is String -> raw.filter { it.isDigit() }.toIntOrNull() ?: 3
                    else -> 3
                }

                // ✅ Load semesterFees map
                @Suppress("UNCHECKED_CAST")
                val feesRaw = doc.get("semesterFees") as? Map<String, Any> ?: emptyMap()
                currentFeesMap = feesRaw.mapValues { entry ->
                    when (val v = entry.value) {
                        is Long   -> v
                        is Number -> v.toLong()
                        else      -> 0L
                    }
                }.toMutableMap()

                etName.setText(name)
                etCode.setText(code)
                ddYears.setText(durationYears.toString(), false)
                ddStatus.setText(status.ifEmpty { "Active" }, false)
                topBar.title = "$name ($code)"

                // ✅ Build fees view
                buildFeesViewMode(currentFeesMap)
                setEditMode(false)
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadSubjectCount() {
        db.collection("subjects")
            .whereEqualTo("courseId", courseId)
            .get()
            .addOnSuccessListener { snap ->
                subjectCount = snap.size()
                tvSubjectCount.text = if (subjectCount == 1)
                    "1 subject in this course"
                else "$subjectCount subjects in this course"
            }
            .addOnFailureListener {
                tvSubjectCount.text = "Unable to load subjects"
            }
    }

    private fun saveCourseEdits() {
        val name   = etName.text.toString().trim()
        val code   = etCode.text.toString().trim()
        val years  = ddYears.text.toString().toIntOrNull() ?: 3
        val status = ddStatus.text.toString().trim()

        if (name.isBlank() || code.isBlank() || status.isBlank()) {
            Toast.makeText(this, "Fill all fields correctly", Toast.LENGTH_LONG).show()
            return
        }

        setLoading(true)

        db.collection("courses").document(courseId)
            .update(mapOf(
                "name"          to name,
                "code"          to code,
                "durationYears" to years,
                "status"        to status
            ))
            .addOnSuccessListener {
                setLoading(false)
                durationYears = years
                setEditMode(false)
                topBar.title = "$name ($code)"
                Toast.makeText(this, "✅ Saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "❌ ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteConfirmation() {
        val msg = if (subjectCount > 0)
            "⚠️ This will delete \"${etName.text}\" and all $subjectCount subjects.\n\nCannot be undone!"
        else
            "Delete \"${etName.text}\"?\n\nCannot be undone."

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Course?")
            .setMessage(msg)
            .setPositiveButton("Delete") { _, _ -> deleteCourse() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCourse() {
        setLoading(true)

        db.collection("subjects")
            .whereEqualTo("courseId", courseId)
            .get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.delete(db.collection("courses").document(courseId))
                batch.commit()
                    .addOnSuccessListener {
                        setLoading(false)
                        Toast.makeText(this, "✅ Deleted!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        Toast.makeText(this, "❌ ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "❌ ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility      = if (loading) View.VISIBLE else View.GONE
        detailsContainer.alpha   = if (loading) 0.6f else 1f
        detailsContainer.isEnabled = !loading
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()
}
