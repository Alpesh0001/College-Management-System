package com.example.collegemanagementsystemadmin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.example.collegemanagementsystemadmin.models.CourseOption
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminAddSubjectActivity : CoreBaseActivity() {

    // Views
    private lateinit var topBar: MaterialToolbar
    private lateinit var tilSubjectName: TextInputLayout
    private lateinit var tilSubjectId: TextInputLayout
    private lateinit var tilCourse: TextInputLayout
    private lateinit var tilYear: TextInputLayout
    private lateinit var tilSemester: TextInputLayout
    private lateinit var tilStatus: TextInputLayout

    private lateinit var etSubjectName: TextInputEditText
    private lateinit var etSubjectId: TextInputEditText
    private lateinit var ddCourse: AutoCompleteTextView
    private lateinit var ddYear: AutoCompleteTextView
    private lateinit var ddSemester: AutoCompleteTextView
    private lateinit var ddStatus: AutoCompleteTextView

    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var bottomButtonBar: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var formContainer: View

    // Firebase
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Course data
    private val courseOptions = mutableListOf<CourseOption>()
    private val courseDetailsMap = mutableMapOf<String, Int>() // courseId -> durationYears

    // Mode detection
    private var subjectId: String? = null
    private var isEditMode = false
    private var isViewMode = false

    // Selection tracking
    private var courseSelected = false
    private var yearSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_subject)

        // Check if we're editing/viewing an existing subject
        subjectId = intent.getStringExtra("id")
        isViewMode = subjectId != null

        bindViews()
        setupToolbar()
        setupDropdowns()
        setupButtons()
        setupBackPressed()
        loadCourses()

        // If viewing/editing, load existing data
        if (isViewMode && subjectId != null) {
            loadSubject(subjectId!!)
        }
    }

    private fun bindViews() {
        topBar = findViewById(R.id.topBar)

        tilSubjectName = findViewById(R.id.tilSubjectName)
        tilSubjectId = findViewById(R.id.tilSubjectId)
        tilCourse = findViewById(R.id.tilCourse)
        tilYear = findViewById(R.id.tilYear)
        tilSemester = findViewById(R.id.tilSemester)
        tilStatus = findViewById(R.id.tilStatus)

        etSubjectName = findViewById(R.id.etSubjectName)
        etSubjectId = findViewById(R.id.etSubjectId)
        ddCourse = findViewById(R.id.ddCourse)
        ddYear = findViewById(R.id.ddYear)
        ddSemester = findViewById(R.id.ddSemester)
        ddStatus = findViewById(R.id.ddStatus)

        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        bottomButtonBar = findViewById(R.id.bottomButtonBar)
        progress = findViewById(R.id.progressOverlay)
        formContainer = findViewById(R.id.formContainer)
    }

    private fun setupToolbar() {
        topBar.title = if (isViewMode) "View Subject" else "Add Subject"

        topBar.setNavigationOnClickListener {
            finish()
        }
    }


    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }


    private fun setupDropdowns() {
        // Status dropdown
        val statusOptions = listOf("Active", "Inactive")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, statusOptions)
        ddStatus.setAdapter(statusAdapter)
        ddStatus.setText("Active", false)

        // Completely disable dropdown in view mode
        ddStatus.setOnClickListener {
            if (!isViewMode || isEditMode) {
                ddStatus.showDropDown()
            } else {
                Toast.makeText(this, "❌ View mode - Cannot edit. Click Edit button first.", Toast.LENGTH_SHORT).show()
            }
        }

        // Course dropdown with validation
        ddCourse.setOnItemClickListener { _, _, position, _ ->
            val selectedCourse = courseOptions[position]
            courseSelected = true
            yearSelected = false
            updateYearDropdown(selectedCourse.id)
        }

        ddCourse.setOnClickListener {
            if (!isViewMode || isEditMode) {
                ddCourse.showDropDown()
            } else {
                Toast.makeText(this, "❌ View mode - Cannot edit. Click Edit button first.", Toast.LENGTH_SHORT).show()
            }
        }

        // Disable text input (only dropdown)
        ddCourse.keyListener = null

        // Year dropdown with validation
        ddYear.setOnClickListener {
            when {
                isViewMode && !isEditMode -> {
                    Toast.makeText(this, "❌ View mode - Cannot edit. Click Edit button first.", Toast.LENGTH_SHORT).show()
                }
                !courseSelected && !isViewMode -> {
                    Toast.makeText(this, "⚠️ Please select a course first", Toast.LENGTH_SHORT).show()
                    tilCourse.error = "Select course first"
                    ddCourse.requestFocus()
                }
                else -> {
                    tilCourse.error = null
                    ddYear.showDropDown()
                }
            }
        }

        ddYear.setOnItemClickListener { _, _, _, _ ->
            val selectedYear = ddYear.text.toString().toIntOrNull() ?: 1
            yearSelected = true
            updateSemesterDropdown(selectedYear)
            tilYear.error = null
        }

        ddYear.keyListener = null

        // Semester dropdown with validation
        ddSemester.setOnClickListener {
            when {
                isViewMode && !isEditMode -> {
                    Toast.makeText(this, "❌ View mode - Cannot edit. Click Edit button first.", Toast.LENGTH_SHORT).show()
                }
                !courseSelected && !isViewMode -> {
                    Toast.makeText(this, "⚠️ Please select a course first", Toast.LENGTH_SHORT).show()
                    tilCourse.error = "Select course first"
                    ddCourse.requestFocus()
                }
                !yearSelected && !isViewMode -> {
                    Toast.makeText(this, "⚠️ Please select a year first", Toast.LENGTH_SHORT).show()
                    tilYear.error = "Select year first"
                    ddYear.requestFocus()
                }
                else -> {
                    tilYear.error = null
                    ddSemester.showDropDown()
                }
            }
        }

        ddSemester.keyListener = null
    }

    private fun setupButtons() {
        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            if (isViewMode) {
                updateSubject()
            } else {
                saveSubject()
            }
        }
    }


    private fun loadCourses() {
        setLoading(true)

        db.collection("courses")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { snapshot ->
                courseOptions.clear()
                courseDetailsMap.clear()

                for (doc in snapshot.documents) {
                    val id = doc.id
                    val name = doc.getString("name") ?: ""
                    val code = doc.getString("code") ?: ""
                    val display = "$name ($code)"

                    // Get duration years
                    val duration = when (val raw = doc.get("durationYears")) {
                        is Number -> raw.toInt()
                        is String -> raw.toIntOrNull() ?: 3
                        else -> 3
                    }

                    courseOptions.add(CourseOption(id, display))
                    courseDetailsMap[id] = duration
                }

                courseOptions.sortBy { it.displayName }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    courseOptions
                )
                ddCourse.setAdapter(adapter)

                setLoading(false)

                if (courseOptions.isEmpty() && !isViewMode) {
                    Toast.makeText(
                        this,
                        "⚠️ No active courses found. Create a course first.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(
                    this,
                    "❌ Failed to load courses: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateYearDropdown(courseId: String) {
        val duration = courseDetailsMap[courseId] ?: 3

        // Generate year options (1 to duration)
        val yearOptions = (1..duration).map { it.toString() }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, yearOptions)
        ddYear.setAdapter(yearAdapter)

        // Clear current selection
        ddYear.setText("", false)
        ddSemester.setText("", false)
        yearSelected = false
    }

    private fun updateSemesterDropdown(year: Int) {
        // Calculate semester range for selected year
        val startSem = (year - 1) * 2 + 1
        val endSem = year * 2

        val semOptions = (startSem..endSem).map { it.toString() }
        val semAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, semOptions)
        ddSemester.setAdapter(semAdapter)

        // Clear current selection
        ddSemester.setText("", false)
    }

    private fun loadSubject(id: String) {
        setLoading(true)

        db.collection("subjects").document(id).get()
            .addOnSuccessListener { doc ->
                setLoading(false)

                if (!doc.exists()) {
                    Toast.makeText(this, "Subject not found", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val name = doc.getString("name").orEmpty()
                val subId = doc.getString("subjectId").orEmpty()
                val courseId = doc.getString("courseId").orEmpty()
                val courseName = doc.getString("courseName").orEmpty()
                val status = doc.getString("status").orEmpty()

                val year = when (val rawYear = doc.get("year")) {
                    is Number -> rawYear.toInt().toString()
                    is String -> rawYear.filter { it.isDigit() }
                    else -> "1"
                }

                val semester = when (val rawSem = doc.get("semester")) {
                    is Number -> rawSem.toInt().toString()
                    is String -> rawSem.filter { it.isDigit() }
                    else -> "1"
                }

                etSubjectName.setText(name)
                etSubjectId.setText(subId)
                ddCourse.setText(courseName, false)

                updateYearDropdown(courseId)
                ddYear.setText(year, false)

                updateSemesterDropdown(year.toIntOrNull() ?: 1)
                ddSemester.setText(semester, false)

                ddStatus.setText(status.ifEmpty { "Active" }, false)

                courseSelected = true
                yearSelected = true

                topBar.title = "$name ($subId)"

                // ✅ CHECK MODE FROM INTENT
                val mode = intent.getStringExtra("mode")
                when (mode) {
                    "edit" -> setEditMode(true)   // Edit mode
                    "view" -> setEditMode(false)  // View mode
                    else -> setEditMode(false)     // Default to view
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(
                    this,
                    "Failed to load subject: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }



    private fun setEditMode(editable: Boolean) {
        isEditMode = editable

        topBar.title = when {
            editable -> "Edit Subject"
            isViewMode -> "View Subject"
            else -> "Add Subject"
        }

        etSubjectName.isEnabled = editable || !isViewMode
        etSubjectId.isEnabled = !isViewMode && !editable

        if (isViewMode && !editable) {
            // View mode: Disable everything
            tilCourse.isEnabled = false
            tilYear.isEnabled = false
            tilSemester.isEnabled = false
            tilStatus.isEnabled = false
            ddCourse.isEnabled = false
            ddYear.isEnabled = false
            ddSemester.isEnabled = false
            ddStatus.isEnabled = false
        } else {
            // Edit/Add mode: Enable everything
            tilCourse.isEnabled = true
            tilYear.isEnabled = true
            tilSemester.isEnabled = true
            tilStatus.isEnabled = true
            ddCourse.isEnabled = true
            ddYear.isEnabled = true
            ddSemester.isEnabled = true
            ddStatus.isEnabled = true
        }

        // Button visibility
        if (isViewMode) {
            if (editable) {
                // Edit mode: [Cancel] [Save Changes]
                btnCancel.visibility = View.VISIBLE
                btnSave.visibility = View.VISIBLE
                btnCancel.text = "Cancel"
                btnSave.text = "Save Changes"
                bottomButtonBar.visibility = View.VISIBLE
            } else {
                // View mode: [Close] only
                btnCancel.visibility = View.VISIBLE
                btnSave.visibility = View.GONE
                btnCancel.text = "Close"
                bottomButtonBar.visibility = View.VISIBLE
            }
        } else {
            // Add mode: [Cancel] [Save Subject]
            btnCancel.visibility = View.VISIBLE
            btnSave.visibility = View.VISIBLE
            btnCancel.text = "Cancel"
            btnSave.text = "Save Subject"
            bottomButtonBar.visibility = View.VISIBLE
        }
    }



    private fun saveSubject() {
        // Clear errors
        tilSubjectName.error = null
        tilSubjectId.error = null
        tilCourse.error = null
        tilYear.error = null
        tilSemester.error = null
        tilStatus.error = null

        // Get values
        val subjectName = etSubjectName.text.toString().trim()
        val subjectIdRaw = etSubjectId.text.toString().trim().uppercase()
        val courseText = ddCourse.text.toString().trim()
        val year = ddYear.text.toString().trim()
        val semester = ddSemester.text.toString().trim()
        val status = ddStatus.text.toString().trim()

        // Validation
        var hasError = false

        if (subjectName.isEmpty()) {
            tilSubjectName.error = "Subject name is required"
            if (!hasError) {
                etSubjectName.requestFocus()
                hasError = true
            }
        }

        if (subjectIdRaw.isEmpty()) {
            tilSubjectId.error = "Subject ID is required"
            if (!hasError) {
                etSubjectId.requestFocus()
                hasError = true
            }
        } else if (!subjectIdRaw.matches(Regex("^[A-Z0-9]{2,12}$"))) {
            tilSubjectId.error = "Use 2-12 uppercase letters/digits only"
            if (!hasError) {
                etSubjectId.requestFocus()
                hasError = true
            }
        }

        if (courseText.isEmpty()) {
            tilCourse.error = "Course is required"
            if (!hasError) {
                ddCourse.requestFocus()
                hasError = true
            }
        }

        val selectedCourse = courseOptions.find { it.displayName == courseText }
        if (selectedCourse == null && courseText.isNotEmpty()) {
            tilCourse.error = "Please select a valid course from the list"
            if (!hasError) {
                ddCourse.requestFocus()
                hasError = true
            }
        }

        if (year.isEmpty()) {
            tilYear.error = "Year is required"
            if (!hasError) {
                ddYear.requestFocus()
                hasError = true
            }
        }

        if (semester.isEmpty()) {
            tilSemester.error = "Semester is required"
            if (!hasError) {
                ddSemester.requestFocus()
                hasError = true
            }
        }

        if (status.isEmpty()) {
            tilStatus.error = "Status is required"
            if (!hasError) {
                ddStatus.requestFocus()
                hasError = true
            }
        }

        if (hasError || selectedCourse == null) return

        // Prepare data
        val subjectKey = subjectIdRaw.lowercase()
        val uid = auth.currentUser?.uid.orEmpty()

        val data = hashMapOf(
            "name" to subjectName,
            "subjectId" to subjectIdRaw,
            "subjectKey" to subjectKey,
            "courseId" to selectedCourse.id,
            "courseName" to selectedCourse.displayName,
            "year" to year.toInt(),
            "semester" to semester.toInt(),
            "status" to status,
            "createdAt" to Timestamp.now(),
            "createdBy" to uid
        )

        setLoading(true)

        val docRef = db.collection("subjects").document(subjectKey)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    setLoading(false)
                    tilSubjectId.error = "Subject ID already exists"
                    etSubjectId.requestFocus()
                } else {
                    docRef.set(data)
                        .addOnSuccessListener {
                            setLoading(false)
                            Toast.makeText(
                                this,
                                "✅ Subject added successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            setLoading(false)
                            Toast.makeText(
                                this,
                                "❌ Error: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(
                    this,
                    "❌ Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateSubject() {
        tilSubjectName.error = null
        tilCourse.error = null
        tilYear.error = null
        tilSemester.error = null
        tilStatus.error = null

        val name = etSubjectName.text.toString().trim()
        val courseText = ddCourse.text.toString().trim()
        val year = ddYear.text.toString().trim()
        val semester = ddSemester.text.toString().trim()
        val status = ddStatus.text.toString().trim()

        var hasError = false

        if (name.isEmpty()) {
            tilSubjectName.error = "Subject name is required"
            etSubjectName.requestFocus()
            hasError = true
        }

        val selectedCourse = courseOptions.find { it.displayName == courseText }
        if (selectedCourse == null) {
            tilCourse.error = "Please select a valid course"
            ddCourse.requestFocus()
            hasError = true
        }

        if (hasError || selectedCourse == null) return

        setLoading(true)

        val payload = hashMapOf(
            "name" to name,
            "courseId" to selectedCourse.id,
            "courseName" to selectedCourse.displayName,
            "year" to year.toInt(),
            "semester" to semester.toInt(),
            "status" to status
        )

        db.collection("subjects").document(subjectId!!)
            .update(payload as Map<String, Any>)
            .addOnSuccessListener {
                setLoading(false)
                setEditMode(false)
                topBar.title = "$name (${etSubjectId.text})"
                Toast.makeText(this, "✅ Subject updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(
                    this,
                    "❌ Update failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        formContainer.isEnabled = !loading
        formContainer.alpha = if (loading) 0.6f else 1.0f
        btnSave.isEnabled = !loading
        btnCancel.isEnabled = !loading
    }
}
