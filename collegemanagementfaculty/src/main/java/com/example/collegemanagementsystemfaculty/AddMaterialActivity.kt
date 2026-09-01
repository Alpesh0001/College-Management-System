package com.example.collegemanagementsystemfaculty

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.collegemanagementsystemfaculty.models.MaterialModel
import com.example.collegemanagementsystemfaculty.utils.CloudinaryHelper
import com.example.collegemanagementsystemfaculty.utils.CoreBaseActivity
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class AddMaterialActivity : CoreBaseActivity() {

    // ✅ Firestore & Session
    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    // ✅ Mode & IDs
    private var currentMode     = "add"
    private var materialId      = ""
    private var existingFileUrl = ""

    // ✅ File
    private var selectedFileUri  : Uri?   = null
    private var selectedFileName : String = ""

    // ✅ Course data from Firestore
    private var courseDurationYears = 3  // default, will be loaded from Firestore

    // ─── Views ───────────────────────────────
    private lateinit var btnBack           : View
    private lateinit var tvToolbarTitle    : TextView
    private lateinit var ddYear            : AutoCompleteTextView
    private lateinit var ddSem             : AutoCompleteTextView
    private lateinit var ddSubject         : AutoCompleteTextView
    private lateinit var etTitle           : TextInputEditText
    private lateinit var etDescription     : TextInputEditText
    private lateinit var layoutFilePicker  : LinearLayout
    private lateinit var tvFileName        : TextView
    private lateinit var imgFilePreview    : ImageView
    private lateinit var btnSelectFile     : Button
    private lateinit var btnSave           : Button
    private lateinit var btnDeleteMaterial : Button
    private lateinit var btnViewPdf        : Button
    private lateinit var cardFileAttachment: View
    private lateinit var ddYear1: TextInputLayout
    private lateinit var ddSem1: TextInputLayout
    private lateinit var ddSubject1: TextInputLayout

    companion object {
        private const val FILE_PICK_REQUEST = 101
    }

    // ─────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_material)

        session     = SessionManager(this)
        currentMode = intent.getStringExtra("mode") ?: "add"
        materialId  = intent.getStringExtra("material_id") ?: ""

        bindViews()
        setupToolbar()
        setupModeUI()
        setupClickListeners()

        // ✅ First load course duration, then setup dropdowns
        loadCourseDurationThenSetup()

        if (currentMode != "add" && materialId.isNotEmpty()) {
            loadMaterialData()
        }
    }
    // ─────────────────────────────────────────
    // ✅ BIND ALL VIEWS
    private fun bindViews() {
        btnBack            = findViewById(R.id.btnBack)
        tvToolbarTitle     = findViewById(R.id.tvToolbarTitle)
        ddYear             = findViewById(R.id.ddYear)
        ddSem              = findViewById(R.id.ddSem)
        ddSubject          = findViewById(R.id.ddSubject)
        etTitle            = findViewById(R.id.etTitle)
        etDescription      = findViewById(R.id.etDescription)
        layoutFilePicker   = findViewById(R.id.layoutFilePicker)
        tvFileName         = findViewById(R.id.tvFileName)
        imgFilePreview     = findViewById(R.id.imgFilePreview)
        btnSelectFile      = findViewById(R.id.btnSelectFile)
        btnSave            = findViewById(R.id.btnSave)
        btnDeleteMaterial  = findViewById(R.id.btnDeleteMaterial)
        btnViewPdf         = findViewById(R.id.btnViewPdf)
        cardFileAttachment = findViewById(R.id.cardFileAttachment)
        ddYear1 = findViewById(R.id.ddYear1)
        ddSem1 = findViewById(R.id.ddSem1)
        ddSubject1 = findViewById(R.id.ddSubject1)
    }

    // ─────────────────────────────────────────
    // ✅ TOOLBAR
    private fun setupToolbar() {
        tvToolbarTitle.text = when (currentMode) {
            "add"  -> "Add Material"
            "edit" -> "Edit Material"
            "view" -> "View Material"
            else   -> "Material"
        }
        btnBack.setOnClickListener { finish() }
    }

    // ─────────────────────────────────────────
    // ✅ MODE UI
    private fun setupModeUI() {
        when (currentMode) {

            "add" -> {
                btnSave.text                   = "Upload Material"
                btnSave.visibility             = View.VISIBLE
                btnDeleteMaterial.visibility   = View.GONE
                btnViewPdf.visibility          = View.GONE
                btnSelectFile.visibility       = View.VISIBLE
                cardFileAttachment.visibility  = View.VISIBLE
                setFieldsEditable(true)
            }

            "edit" -> {
                btnSave.text                   = "Save Changes"
                btnSave.visibility             = View.VISIBLE
                btnDeleteMaterial.visibility   = View.VISIBLE
                btnViewPdf.visibility          = View.GONE
                btnSelectFile.visibility       = View.VISIBLE
                cardFileAttachment.visibility  = View.VISIBLE
                setFieldsEditable(true)
            }

            "view" -> {
                btnSave.visibility             = View.GONE
                btnDeleteMaterial.visibility   = View.GONE
                btnViewPdf.visibility          = View.VISIBLE
                btnSelectFile.visibility       = View.GONE
                cardFileAttachment.visibility  = View.VISIBLE
                setFieldsEditable(false)
            }
        }
    }

    // ─────────────────────────────────────────
    // ✅ ENABLE / DISABLE ALL FIELDS
    private fun setFieldsEditable(editable: Boolean) {

        etTitle.isEnabled = editable
        etDescription.isEnabled = editable

        ddYear.isEnabled = editable
        ddSem.isEnabled = editable
        ddSubject.isEnabled = editable

        layoutFilePicker.isEnabled = editable

        // ⭐ IMPORTANT — Disable dropdown arrows
        ddYear1.isEnabled = editable
        ddSem1.isEnabled = editable
        ddSubject1.isEnabled = editable

        // ⭐ Prevent dropdown from opening
        if (!editable) {
            ddYear.keyListener = null
            ddSem.keyListener = null
            ddSubject.keyListener = null

            ddYear.isFocusable = false
            ddSem.isFocusable = false
            ddSubject.isFocusable = false
        }
    }



    private fun loadCourseDurationThenSetup() {

        showBlockingLoader("Preparing form...")

        val courseId = session.getCourseId()
        if (courseId.isEmpty()) {
            hideBlockingLoader()
            setupYearDropdown(3)
            return
        }

        db.collection("courses")
            .document(courseId)
            .get()
            .addOnSuccessListener { doc ->
                hideBlockingLoader()

                courseDurationYears = when (val v = doc.get("durationYears")) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: 3
                    else -> 3
                }

                setupYearDropdown(courseDurationYears)
            }
            .addOnFailureListener {
                hideBlockingLoader()
                setupYearDropdown(3)
            }
    }

    // ─────────────────────────────────────────
    // ✅ STEP 2: Setup Year dropdown based on duration
    private fun setupYearDropdown(durationYears: Int) {
        val yearLabels = (1..durationYears).map { year ->
            when (year) {
                1 -> "1"
                2 -> "2"
                3 -> "3"
                4 -> "4"
                else -> "${year}"
            }
        }

        ddYear.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, yearLabels)
        )

        // ✅ When year selected → update semester dropdown
        ddYear.setOnItemClickListener { _, _, position, _ ->
            val yearNo = position + 1  // position 0 = 1st Year
            ddSem.setText("", false)   // clear previous sem
            ddSubject.setText("", false) // clear previous subject
            updateSemesterOptions(yearNo)
        }

        ddYear.setOnClickListener { ddYear.showDropDown() }
    }

    // ─────────────────────────────────────────
    // ✅ STEP 3: Semester options based on selected year
    private fun updateSemesterOptions(yearNo: Int) {
        val sem1 = (yearNo * 2) - 1
        val sem2 = yearNo * 2
        val sems = listOf("$sem1", "$sem2")

        ddSem.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sems)
        )

        // ✅ When sem selected → load subjects
        ddSem.setOnItemClickListener { _, _, position, _ ->
            val selectedSemNo = if (position == 0) sem1 else sem2
            val selectedYearNo = yearNo
            ddSubject.setText("", false) // clear previous subject
            loadSubjectsForYearSem(selectedYearNo, selectedSemNo)
        }

        ddSem.setOnClickListener { ddSem.showDropDown() }
    }

    // ─────────────────────────────────────────
    // ✅ STEP 4: Load subjects from Firestore based on year + sem
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
                }

                ddSubject.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, subjectNames)
                )
                ddSubject.setOnClickListener { ddSubject.showDropDown() }
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Failed to load subjects", Toast.LENGTH_SHORT).show()
            }
    }

    // ─────────────────────────────────────────
    // ✅ CLICK LISTENERS
    private fun setupClickListeners() {

        btnSelectFile.setOnClickListener { openFilePicker() }
        layoutFilePicker.setOnClickListener { openFilePicker() }

        btnSave.setOnClickListener {
            if (validateForm()) {
                if (selectedFileUri != null) {
                    uploadFileAndSave()
                } else if (currentMode == "edit" && existingFileUrl.isNotEmpty()) {
                    saveToFirestore(existingFileUrl)
                } else {
                    Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnDeleteMaterial.setOnClickListener { showDeleteConfirmDialog() }

        btnViewPdf.setOnClickListener {
            if (existingFileUrl.isNotEmpty()) {
                openPdfInBrowser(existingFileUrl)
            } else {
                Toast.makeText(this, "No file available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────────────────────────────────────
    // ✅ FILE PICKER
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ))
        }
        startActivityForResult(Intent.createChooser(intent, "Select File"), FILE_PICK_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedFileUri  = uri
                selectedFileName = getFileNameFromUri(uri)
                tvFileName.text  = selectedFileName
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "selected_file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    // ─────────────────────────────────────────
    // ✅ VALIDATION
    private fun validateForm(): Boolean {
        val title   = etTitle.text.toString().trim()
        val year    = ddYear.text.toString().trim()
        val sem     = ddSem.text.toString().trim()
        val subject = ddSubject.text.toString().trim()

        return when {
            title.isEmpty()   -> { Toast.makeText(this, "Please enter material title", Toast.LENGTH_SHORT).show(); false }
            year.isEmpty()    -> { Toast.makeText(this, "Please select year", Toast.LENGTH_SHORT).show(); false }
            sem.isEmpty()     -> { Toast.makeText(this, "Please select semester", Toast.LENGTH_SHORT).show(); false }
            subject.isEmpty() -> { Toast.makeText(this, "Please select subject", Toast.LENGTH_SHORT).show(); false }
            else              -> true
        }
    }

    private fun uploadFileAndSave() {

        showBlockingLoader("Uploading material...")

        btnSave.isEnabled = false
        btnSave.text = "Uploading... 0%"

        CloudinaryHelper.uploadFile(
            context = this,
            fileUri = selectedFileUri!!,
            fileName = selectedFileName,

            onProgress = { progress ->
                runOnUiThread {
                    btnSave.text = "Uploading... $progress%"
                }
            },

            onSuccess = { url ->
                runOnUiThread {
                    btnSave.text = "Saving..."
                    saveToFirestore(url)
                }
            },

            onError = { error ->
                runOnUiThread {
                    hideBlockingLoader()

                    btnSave.isEnabled = true
                    btnSave.text =
                        if (currentMode == "add") "Upload Material"
                        else "Save Changes"

                    Toast.makeText(
                        this,
                        "❌ Upload failed: $error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    // ─────────────────────────────────────────
    // ✅ SAVE TO FIRESTORE
    private fun saveToFirestore(fileUrl: String) {
        val now = System.currentTimeMillis()
        showBlockingLoader("Saving material...")
        val material = MaterialModel(
            courseId       = session.getCourseId(),
            courseName     = session.getCourseName(),
            title          = etTitle.text.toString().trim(),
            description    = etDescription.text.toString().trim(),
            year           = ddYear.text.toString().trim(),
            semester       = ddSem.text.toString().trim(),
            subject        = ddSubject.text.toString().trim(),
            fileUrl        = fileUrl,
            fileName       = selectedFileName.ifEmpty { "file" },
            fileType = when {
                fileUrl.endsWith(".pdf", true) -> "pdf"
                fileUrl.endsWith(".doc", true) -> "doc"
                fileUrl.endsWith(".docx", true) -> "docx"
                else -> "file"
            },
            uploadedBy     = session.getFacultyId(),
            uploadedByName = session.getFullName(),
            uploadedAt     = if (currentMode == "add") now else 0L,
            updatedAt      = now
        )

        if (currentMode == "add") {
            db.collection("study_materials")
                .add(material)
                .addOnSuccessListener {
                    hideBlockingLoader()
                    Toast.makeText(this, "✅ Material uploaded successfully!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)

                    finish()
                }
                .addOnFailureListener { e ->
                    hideBlockingLoader()
                    btnSave.isEnabled = true
                    btnSave.text      = "Upload Material"
                    Toast.makeText(this, "❌ Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

        } else {
            val updates = hashMapOf<String, Any>(
                "title"       to material.title,
                "description" to material.description,
                "year"        to material.year,
                "semester"    to material.semester,
                "subject"     to material.subject,
                "fileUrl"     to fileUrl,
                "fileName"    to material.fileName,
                "fileType"    to material.fileType,
                "updatedAt"   to now
            )
            db.collection("study_materials")
                .document(materialId)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Material updated successfully!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                .addOnFailureListener { e ->
                    btnSave.isEnabled = true
                    btnSave.text      = "Save Changes"
                    Toast.makeText(this, "❌ Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // ─────────────────────────────────────────
    // ✅ LOAD EXISTING DATA (EDIT / VIEW)
    private fun loadMaterialData() {
        showBlockingLoader("Loading...")

        db.collection("study_materials")
            .document(materialId)
            .get()
            .addOnSuccessListener { doc ->
                hideBlockingLoader()
                if (doc.exists()) {
                    val material = doc.toObject(MaterialModel::class.java)
                        ?: return@addOnSuccessListener
                    material.documentId = doc.id

                    etTitle.setText(material.title)
                    etDescription.setText(material.description)
                    ddYear.setText(material.year, false)
                    ddSem.setText(material.semester, false)
                    ddSubject.setText(material.subject, false)
                    tvFileName.text = material.fileName

                    existingFileUrl  = material.fileUrl
                    selectedFileName = material.fileName
                }
            }
            .addOnFailureListener {
                hideBlockingLoader()
                Toast.makeText(this, "❌ Failed to load material", Toast.LENGTH_SHORT).show()
            }
    }

    // ─────────────────────────────────────────
    // ✅ DELETE CONFIRM DIALOG
    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Material")
            .setMessage("Are you sure you want to delete \"${etTitle.text}\"?\nThis cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteMaterial() }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    // ✅ DELETE FROM FIRESTORE
    private fun deleteMaterial() {
        btnDeleteMaterial.isEnabled = false
        btnDeleteMaterial.text      = "Deleting..."
        showBlockingLoader("Deleting...")

        db.collection("study_materials")
            .document(materialId)
            .delete()
            .addOnSuccessListener {
                hideBlockingLoader()
                Toast.makeText(this, "✅ Material deleted!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                hideBlockingLoader()
                btnDeleteMaterial.isEnabled = true
                btnDeleteMaterial.text      = "Delete Material"
                Toast.makeText(this, "❌ Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ─────────────────────────────────────────
    // ✅ OPEN PDF IN BROWSER
    private fun openPdfInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
