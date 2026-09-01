package com.example.collegemanagementsystemfaculty

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.collegemanagementsystemfaculty.models.AssignmentModel
import com.example.collegemanagementsystemfaculty.utils.CloudinaryHelper
import com.example.collegemanagementsystemfaculty.utils.CoreBaseActivity
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CreateAssignmentActivity : CoreBaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    private var currentMode = MODE_ADD
    private var assignmentId = ""
    private var existingFileUrl = ""

    private var selectedFileUri: Uri? = null
    private var selectedFileName = ""
    private var selectedDueDate: Long = 0L

    // 🔥 Course duration
    private var courseDurationYears = 3

    // Views
    private lateinit var btnBack: View
    private lateinit var tvToolbarTitle: TextView
    private lateinit var ddYear: AutoCompleteTextView
    private lateinit var ddSem: AutoCompleteTextView
    private lateinit var ddSubject: AutoCompleteTextView
    private lateinit var ddYear1: TextInputLayout
    private lateinit var ddSem1: TextInputLayout
    private lateinit var ddSubject1: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etDueDate: EditText
    private lateinit var layoutFilePicker: LinearLayout
    private lateinit var tvFileName: TextView
    private lateinit var btnSelectFile: Button
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnViewPdf: Button
    private lateinit var cardFileAttachment: View

    companion object {
        private const val FILE_PICK_REQUEST = 101
        const val MODE_ADD = "add"
        const val MODE_EDIT = "edit"
        const val MODE_VIEW = "view"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_assignment)

        session = SessionManager(this)
        currentMode = intent.getStringExtra("MODE") ?: MODE_ADD
        assignmentId = intent.getStringExtra("ASSIGNMENT_ID") ?: ""

        bindViews()
        setupToolbar()
        setupModeUI()
        setupClickListeners()

        // 🔥 Dynamic dropdown setup
        loadCourseDurationThenSetup()

        if (currentMode != MODE_ADD && assignmentId.isNotEmpty()) {
            loadAssignmentData()
        }
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle)
        ddYear = findViewById(R.id.ddYear)
        ddSem = findViewById(R.id.ddSem)
        ddSubject = findViewById(R.id.ddSubject)
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etDueDate = findViewById(R.id.etDueDate)
        layoutFilePicker = findViewById(R.id.layoutFilePicker)
        tvFileName = findViewById(R.id.tvFileName)
        btnSelectFile = findViewById(R.id.btnSelectFile)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)
        btnViewPdf = findViewById(R.id.btnViewPdf)
        cardFileAttachment = findViewById(R.id.cardFileAttachment)
        ddYear1 = findViewById(R.id.ddyear1)
        ddSem1 = findViewById(R.id.ddSem1)
        ddSubject1 = findViewById(R.id.ddSubject1)
    }

    private fun setupToolbar() {
        tvToolbarTitle.text = when (currentMode) {
            MODE_ADD -> "Add Assignment"
            MODE_EDIT -> "Edit Assignment"
            MODE_VIEW -> "View Assignment"
            else -> "Assignment"
        }
        btnBack.setOnClickListener { finish() }
    }

    private fun setupModeUI() {
        when (currentMode) {
            MODE_ADD -> {
                btnSave.text = "Create Assignment"
                btnSave.visibility = View.VISIBLE
                btnDelete.visibility = View.GONE
                btnViewPdf.visibility = View.GONE
                btnSelectFile.visibility = View.VISIBLE
                setFieldsEditable(true)
            }
            MODE_EDIT -> {
                btnSave.text = "Save Changes"
                btnSave.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
                btnViewPdf.visibility = View.GONE
                btnSelectFile.visibility = View.VISIBLE
                setFieldsEditable(true)
            }
            MODE_VIEW -> {
                btnSave.visibility = View.GONE
                btnDelete.visibility = View.GONE
                btnViewPdf.visibility = View.VISIBLE
                btnSelectFile.visibility = View.GONE
                setFieldsEditable(false)
            }
        }
    }

    private fun setFieldsEditable(editable: Boolean) {

        etTitle.isEnabled = editable
        etDescription.isEnabled = editable
        ddYear1.isEnabled = editable
        ddSem1.isEnabled = editable
        ddSubject1.isEnabled = editable
        ddYear.isEnabled = editable
        ddSem.isEnabled = editable
        ddSubject.isEnabled = editable
        etDueDate.isEnabled = editable
        btnSelectFile.isEnabled = editable
        layoutFilePicker.isEnabled = editable
    }

    // =========================================================
    // 🔥 DYNAMIC DROPDOWNS
    // =========================================================

    private fun loadCourseDurationThenSetup() {
        val courseId = session.getCourseId()
        if (courseId.isEmpty()) {
            setupYearDropdown(3)
            return
        }

        db.collection("courses")
            .document(courseId)
            .get()
            .addOnSuccessListener { doc ->
                courseDurationYears = when (val v = doc.get("durationYears")) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: 3
                    else -> 3
                }
                setupYearDropdown(courseDurationYears)
            }
            .addOnFailureListener {
                setupYearDropdown(3)
            }
    }

    private fun setupYearDropdown(durationYears: Int) {
        val yearLabels = (1..durationYears).map {
            when (it) {
                1 -> "1"
                2 -> "2"
                3 -> "3"
                4 -> "4"
                else -> "${it}"
            }
        }

        ddYear.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, yearLabels)
        )

        ddYear.setOnItemClickListener { _, _, position, _ ->
            val yearNo = position + 1
            ddSem.setText("", false)
            ddSubject.setText("", false)
            updateSemesterOptions(yearNo)
        }

        ddYear.setOnClickListener {
            if (currentMode != MODE_VIEW) ddYear.showDropDown()
        }
    }

    private fun updateSemesterOptions(yearNo: Int) {
        val sem1 = (yearNo * 2) - 1
        val sem2 = yearNo * 2
        val sems = listOf("$sem1", "$sem2")

        ddSem.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sems)
        )

        ddSem.setOnItemClickListener { _, _, position, _ ->
            val semNo = if (position == 0) sem1 else sem2
            ddSubject.setText("", false)
            loadSubjectsForYearSem(yearNo, semNo)
        }

        ddSem.setOnClickListener {
            if (currentMode != MODE_VIEW) ddSem.showDropDown()
        }
    }

    private fun loadSubjectsForYearSem(yearNo: Int, semNo: Int) {
        val courseId = session.getCourseId()
        if (courseId.isEmpty()) return

        db.collection("subjects")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("year", yearNo)
            .whereEqualTo("semester", semNo)
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { snapshot ->
                val subjectNames = snapshot.documents
                    .mapNotNull { it.getString("name") }
                    .sorted()

                if (subjectNames.isEmpty()) {
                    Toast.makeText(this, "No subjects found for this selection", Toast.LENGTH_SHORT).show()
                    val placeholder = listOf("No subjects available")
                    ddSubject.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, placeholder))
                    ddSubject.setText("", false)
                    return@addOnSuccessListener
                }

                ddSubject.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, subjectNames))
                ddSubject.setOnClickListener {
                    if (currentMode != MODE_VIEW) ddSubject.showDropDown()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Failed to load subjects", Toast.LENGTH_SHORT).show()
            }
    }

    // =========================================================

    private fun setupClickListeners() {
        btnSelectFile.setOnClickListener { if (currentMode != MODE_VIEW) openFilePicker() }
        layoutFilePicker.setOnClickListener { if (currentMode != MODE_VIEW) openFilePicker() }
        etDueDate.setOnClickListener { if (currentMode != MODE_VIEW) showDatePicker() }

        btnSave.setOnClickListener {
            if (validateForm()) uploadFileAndSave()
        }

        btnDelete.setOnClickListener { showDeleteConfirmDialog() }

        btnViewPdf.setOnClickListener {
            if (existingFileUrl.isNotEmpty()) {
                openPdfInBrowser(existingFileUrl)
            } else {
                Toast.makeText(this, "No file available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openPdfInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val picker = DatePickerDialog(
            this,
            { _, y, m, d ->
                calendar.set(y, m, d)
                selectedDueDate = calendar.timeInMillis
                etDueDate.setText(
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(calendar.time)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        picker.datePicker.minDate = System.currentTimeMillis()
        picker.show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
        }
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), FILE_PICK_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                selectedFileUri = it
                selectedFileName = getFileNameFromUri(it)
                tvFileName.text = selectedFileName
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "assignment.pdf"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) {
                name = cursor.getString(index)
            }
        }
        return name
    }

    private fun validateForm(): Boolean {
        return etTitle.text.toString().trim().isNotEmpty()
                && ddYear.text.toString().isNotEmpty()
                && ddSem.text.toString().isNotEmpty()
                && ddSubject.text.toString().isNotEmpty()
    }

    private fun uploadFileAndSave() {
        btnSave.isEnabled = false
        btnSave.text = "Uploading... 0%"

        if (selectedFileUri != null) {
            CloudinaryHelper.uploadAssignmentFile(
                context = this,
                fileUri = selectedFileUri!!,
                fileName = selectedFileName,
                onProgress = { progress ->
                    runOnUiThread { btnSave.text = "Uploading... $progress%" }
                },
                onSuccess = { url ->
                    runOnUiThread {
                        btnSave.text = "Saving..."
                        saveToFirestore(url)
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        btnSave.isEnabled = true
                        btnSave.text = if (currentMode == MODE_ADD) "Create Assignment" else "Save Changes"
                        Toast.makeText(this, "❌ Upload failed: $error", Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else if (currentMode == MODE_EDIT && existingFileUrl.isNotEmpty()) {
            saveToFirestore(existingFileUrl)
        } else {
            saveToFirestore("")
        }
    }

    private fun saveToFirestore(url: String) {

        val now = System.currentTimeMillis()

        // ✅ Get session data safely
        val facultyId = session.getFacultyId()
        val courseId  = session.getCourseId()

        if (facultyId.isEmpty() || courseId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            return
        }

        val assignment = AssignmentModel(
            title = etTitle.text.toString().trim(),
            description = etDescription.text.toString().trim(),
            year = ddYear.text.toString(),
            semester = ddSem.text.toString(),
            subject = ddSubject.text.toString(),
            fileUrl = url,
            fileName = selectedFileName,
            dueDate = selectedDueDate,

            createdAt = if (currentMode == MODE_ADD) now else 0,
            updatedAt = now,

            createdBy = facultyId,
            courseId = courseId   // ✅ NEW FIELD
        )

        if (currentMode == MODE_ADD) {

            db.collection("assignments")
                .add(assignment)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Assignment created!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "❌ Failed to create assignment", Toast.LENGTH_SHORT).show()
                }

        } else {

            db.collection("assignments")
                .document(assignmentId)
                .set(assignment)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Assignment updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "❌ Failed to update assignment", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadAssignmentData() {
        showBlockingLoader("Loading...")
        db.collection("assignments")
            .document(assignmentId)
            .get()
            .addOnSuccessListener { doc ->
                hideBlockingLoader()
                if (doc.exists()) {
                    val assignment = doc.toObject(AssignmentModel::class.java) ?: return@addOnSuccessListener
                    assignment.id = doc.id

                    etTitle.setText(assignment.title)
                    etDescription.setText(assignment.description)
                    ddYear.setText(assignment.year, false)
                    ddSem.setText(assignment.semester, false)
                    ddSubject.setText(assignment.subject, false)

                    selectedDueDate = assignment.dueDate
                    if (assignment.dueDate > 0L) {
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        etDueDate.setText(sdf.format(Date(assignment.dueDate)))
                    }

                    if (assignment.fileUrl.isNotEmpty()) {
                        existingFileUrl = assignment.fileUrl
                        selectedFileName = assignment.fileName
                        tvFileName.text = assignment.fileName
                    } else if (currentMode == MODE_VIEW) {
                        cardFileAttachment.visibility = View.GONE
                        btnViewPdf.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener {
                hideBlockingLoader()
                Toast.makeText(this, "❌ Failed to load assignment", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Assignment")
            .setMessage("Delete \"${etTitle.text}\"?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("assignments").document(assignmentId).delete().addOnSuccessListener { finish() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
