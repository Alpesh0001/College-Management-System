package com.example.collegemanagementsystemadmin

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity
import com.example.collegemanagementsystemadmin.utils.ImgBBUploader
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.yalantis.ucrop.UCrop
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.mapOf

class AdminAddStudentActivity : CoreBaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var mode: String = "add"
    private var studentId: String? = null

    private lateinit var topBar          : MaterialToolbar
    private lateinit var scroll          : ScrollView
    private lateinit var imgProfile      : CircleImageView
    private lateinit var btnChangePhoto  : Button
    private lateinit var etFullName      : EditText
    private lateinit var etDob           : EditText
    private lateinit var ddGender        : AutoCompleteTextView
    private lateinit var ddBloodGroup    : AutoCompleteTextView
    private lateinit var etPhone         : EditText
    private lateinit var etEmail         : EditText
    private lateinit var etAddress       : EditText
    private lateinit var ddCourse        : AutoCompleteTextView
    private lateinit var ddYear          : AutoCompleteTextView
    private lateinit var ddSem           : AutoCompleteTextView
    private lateinit var etAdmissionYear : EditText
    private lateinit var ddStatus        : AutoCompleteTextView
    private lateinit var etGrNo          : EditText
    private var pendingRollNumber = 0
    private var pendingRollText   = ""
    private var pendingGrNumber   = 0
    private lateinit var etRoll          : EditText
    private lateinit var etTempPassword  : EditText
    private lateinit var tvPasswordStatus: TextView
    private lateinit var btnGenGr        : Button
    private lateinit var btnGenRoll      : Button

    private lateinit var btnGenPwd       : Button
    private lateinit var btnSave         : Button
    private lateinit var btnCancel       : Button
    private lateinit var etDivision      : EditText
    private var btnManageCourse          : TextView? = null

    // ✅ Division assignment
    private var assignedDivisionId   = ""
    private var assignedDivisionName = ""

    data class CourseItem(
        val id: String,
        val name: String,
        val code: String,
        val durationYears: Int
    ) {
        override fun toString(): String = "$name ($code)"
    }

    private val courses = mutableListOf<CourseItem>()
    private var selectedCourse    : CourseItem? = null
    private var photoUrl          : String?     = null
    private var pendingStudent    : Map<String, Any?>? = null
    private var croppedImageUri   : Uri?        = null
    private var isSaving          = false

    // ═══════════════════════════════════
    // ✅ Activity Result Launchers
    // ═══════════════════════════════════
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openGallery()
        else Toast.makeText(
            this,
            "Storage permission required to upload photo",
            Toast.LENGTH_LONG
        ).show()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { startCrop(it) } }

    private val cropImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                croppedImageUri = it
                displayCroppedImage(it)
                Toast.makeText(
                    this,
                    "✅ Photo selected. Click Save to upload.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(
                this,
                "Crop error: ${cropError?.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val courseActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadCourses()
            Toast.makeText(this, "Courses reloaded", Toast.LENGTH_SHORT).show()
        }
    }

    // ═══════════════════════════════════
    // ✅ onCreate
    // ═══════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_student)

        mode      = intent.getStringExtra("mode")      ?: "add"
        studentId = intent.getStringExtra("studentId")

        bindViews()
        setupToolbar()
        setupStaticDropdowns()
        setupPickers()
        setupAdmissionYearPicker()
        setupKeyboardScroll()
        setupButtons()
        loadCourses()

        if (mode != "add") {
            topBar.title = if (mode == "view") "View Student" else "Edit Student"
            loadStudentById()
        }

        applyModeLocks()
        wireChangeInvalidators()
    }

    // ═══════════════════════════════════
    // ✅ Bind Views
    // ═══════════════════════════════════
    private fun bindViews() {
        topBar           = findViewById(R.id.topBar)
        scroll           = findViewById(R.id.scrollViewRoot)
        imgProfile       = findViewById(R.id.imgProfile)
        btnChangePhoto   = findViewById(R.id.btnChangePhoto)
        etFullName       = findViewById(R.id.etFullName)
        etDob            = findViewById(R.id.etDob)
        ddGender         = findViewById(R.id.ddGender)
        ddBloodGroup     = findViewById(R.id.ddBloodGroup)
        etPhone          = findViewById(R.id.etPhone)
        etEmail          = findViewById(R.id.etEmail)
        etAddress        = findViewById(R.id.etAddress)
        ddCourse         = findViewById(R.id.ddCourse)
        ddYear           = findViewById(R.id.ddYear)
        ddSem            = findViewById(R.id.ddSem)
        etAdmissionYear  = findViewById(R.id.etAdmissionYear)
        ddStatus         = findViewById(R.id.ddStatus)
        etGrNo           = findViewById(R.id.etGrNo)
        etRoll           = findViewById(R.id.etRoll)
        etTempPassword   = findViewById(R.id.etTempPassword)
        tvPasswordStatus = findViewById(R.id.tvPasswordStatus)
        btnGenGr         = findViewById(R.id.btnGenGr)
        btnGenRoll       = findViewById(R.id.btnGenRoll)
        btnGenPwd        = findViewById(R.id.btnGenPwd)
        btnSave          = findViewById(R.id.btnSave)
        btnCancel        = findViewById(R.id.btnCancel)
        etDivision       = findViewById(R.id.etDivision)
        btnManageCourse  = findViewById(R.id.btnManageCourse)

        listOf(ddCourse, ddYear, ddSem, ddGender, ddBloodGroup, ddStatus).forEach { v ->
            v.setOnClickListener { v.showDropDown() }
        }
    }

    // ═══════════════════════════════════
    // ✅ Setup Toolbar
    // ═══════════════════════════════════
    private fun setupToolbar() {
        topBar.setNavigationOnClickListener { finish() }
    }

    // ═══════════════════════════════════
    // ✅ Static Dropdowns
    // ═══════════════════════════════════
    private fun setupStaticDropdowns() {
        ddGender.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf("Male", "Female", "Other")
            )
        )
        ddBloodGroup.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
            )
        )
        ddStatus.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf("Active", "Inactive", "Graduated", "Suspended")
            )
        )
        ddStatus.setText("Active", false)
    }

    // ═══════════════════════════════════
    // ✅ Apply Mode Locks
    // ═══════════════════════════════════
    private fun applyModeLocks() {
        val isView = mode == "view"
        val isEdit = mode == "edit"

        if (isView) {
            listOf<View>(
                etFullName, etDob, ddGender, ddBloodGroup,
                etPhone, etEmail, etAddress, ddCourse,
                ddYear, ddSem, etAdmissionYear, ddStatus,
                etGrNo, etRoll, etTempPassword, etDivision
            ).forEach { v ->
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
                R.id.tilGender, R.id.tilBloodGroup, R.id.tilCourse,
                R.id.tilYear, R.id.tilSem, R.id.tilStatus, R.id.tilDivision
            ).forEach { id ->
                findViewById<TextInputLayout>(id).isEnabled = false
            }

            btnChangePhoto.isEnabled = false
            btnGenGr.isEnabled       = false
            btnGenRoll.isEnabled     = false
            btnGenPwd.isEnabled      = false
            btnSave.visibility       = View.GONE
            btnCancel.text           = "Close"

        } else if (isEdit) {
            listOf(etGrNo, etRoll, etTempPassword, etAdmissionYear, etDivision)
                .forEach { it.isEnabled = false }

            listOf(
                R.id.tilGrNo, R.id.tilRollNo,
                R.id.tilTempPassword, R.id.tilAdmissionYear,
                R.id.tilDivision
            ).forEach { id ->
                findViewById<TextInputLayout>(id).isEnabled = false
            }

            btnGenGr.isEnabled   = false
            btnGenRoll.isEnabled = false
            btnGenPwd.isEnabled  = false
        }
    }

    // ═══════════════════════════════════
    // ✅ Date Pickers
    // ═══════════════════════════════════
    private fun setupPickers() {
        etDob.setOnClickListener {
            showDatePicker { date -> etDob.setText(date) }
        }
    }

    private fun showDatePicker(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                onPicked(
                    "%02d/%02d/$y".format(Locale.US, d, m + 1)
                )
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupAdmissionYearPicker() {
        etAdmissionYear.isFocusable = false
        etAdmissionYear.isClickable = true
        etAdmissionYear.setOnClickListener {
            val cy = Calendar.getInstance().get(Calendar.YEAR)
            showYearPicker(cy) { y ->
                etAdmissionYear.setText(y.toString())
                clearGeneratedIds()
            }
        }
    }

    private fun showYearPicker(defaultYear: Int, onPicked: (Int) -> Unit) {
        val cal = Calendar.getInstance()
        val dlg = DatePickerDialog(
            this,
            { _, y, _, _ -> onPicked(y) },
            defaultYear,
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        try {
            val dp      = dlg.datePicker
            val dayId   = resources.getIdentifier("day", "id", "android")
            val monthId = resources.getIdentifier("month", "id", "android")
            dp.findViewById<View?>(dayId)?.visibility   = View.GONE
            dp.findViewById<View?>(monthId)?.visibility = View.GONE
        } catch (_: Exception) {}
        dlg.show()
    }

    // ═══════════════════════════════════
    // ✅ Keyboard Scroll
    // ═══════════════════════════════════
    private fun setupKeyboardScroll() {
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) v.post {
                scroll.smoothScrollTo(0, (v.top - 48).coerceAtLeast(0))
            }
        }
        listOf(
            etFullName, etDob, etPhone, etEmail,
            etAddress, etAdmissionYear, etGrNo, etRoll
        ).forEach { it.onFocusChangeListener = focusListener }
    }

    // ═══════════════════════════════════
    // ✅ Setup Buttons
    // ═══════════════════════════════════
    private fun setupButtons() {
        btnManageCourse?.setOnClickListener {
            courseActivityLauncher.launch(
                Intent(this, AdminCourseActivity::class.java)
            )
        }

        btnChangePhoto.setOnClickListener {
            if (mode == "view") {
                Toast.makeText(
                    this,
                    "Cannot change photo in view mode",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            checkPermissionsAndShowDialog()
        }

        btnGenGr.setOnClickListener {
            if (courses.isEmpty()) {
                Toast.makeText(
                    this, "Please wait, loading courses...",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (!validateRequired(
                    listOf(ddCourse to "Course",
                        etAdmissionYear to "Admission year")
                )
            ) return@setOnClickListener
            generateGrWithCounterAndCheck()
        }

        btnGenRoll.setOnClickListener {
            if (courses.isEmpty()) {
                Toast.makeText(
                    this, "Please wait, loading courses...",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (!validateRequired(
                    listOf(ddCourse to "Course",
                        etAdmissionYear to "Admission year")
                )
            ) return@setOnClickListener
            generateRollWithCounterAndCheck()
        }

        btnGenPwd.setOnClickListener {
            if (!validateRequired(
                    listOf(etFullName to "Full name", etDob to "DOB")
                )
            ) return@setOnClickListener
            generateTempPassword()
        }

        btnSave.setOnClickListener {
            if (isSaving) {
                Toast.makeText(
                    this, "Please wait, saving...",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            onSave()
        }

        btnCancel.setOnClickListener { finish() }
    }

    // ═══════════════════════════════════
    // ✅ Validate Required Fields
    // ═══════════════════════════════════
    private fun validateRequired(pairs: List<Pair<View, String>>): Boolean {
        val missing = pairs.filter { (v, _) ->
            val value = when (v) {
                is EditText             -> v.text?.toString()?.trim().orEmpty()
                is AutoCompleteTextView -> v.text?.toString()?.trim().orEmpty()
                else                    -> ""
            }
            value.isEmpty()
        }
        if (missing.isEmpty()) return true
        Toast.makeText(
            this,
            missing.joinToString(", ") { it.second } + " required",
            Toast.LENGTH_SHORT
        ).show()
        focusStable(missing.first().first)
        return false
    }

    private fun focusStable(view: View) {
        view.requestFocus()
        view.post { scroll.smoothScrollTo(0, (view.top - 48).coerceAtLeast(0)) }
    }

    // ═══════════════════════════════════
    // ✅ Load Courses
    // ═══════════════════════════════════
    private fun loadCourses() {
        db.collection("courses")
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snap ->
                courses.clear()
                snap.documents.forEach { d ->
                    if (d.getString("status") == "Active") {
                        courses.add(
                            CourseItem(
                                id            = d.id,
                                name          = d.getString("name").orEmpty(),
                                code          = d.getString("code").orEmpty(),
                                durationYears = when (val v = d.get("durationYears")) {
                                    is Number -> v.toInt()
                                    is String -> v.toIntOrNull() ?: 3
                                    else      -> 3
                                }
                            )
                        )
                    }
                }
                ddCourse.setAdapter(
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        courses
                    )
                )
                pendingStudent?.let { fillFromDoc(it); pendingStudent = null }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to load courses: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun applyYearOptions(durationYears: Int) {
        val years = (1..durationYears.coerceAtLeast(1)).map { it.toString() }
        ddYear.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, years)
        )
        ddYear.setText("", false)
        ddSem.setText("", false)
    }

    // ═══════════════════════════════════
    // ✅ Wire Change Invalidators
    // ═══════════════════════════════════
    private fun wireChangeInvalidators() {
        ddCourse.setOnItemClickListener { _, _, pos, _ ->
            selectedCourse = courses.getOrNull(pos)
            applyYearOptions(selectedCourse?.durationYears ?: 1)
            clearGeneratedIds()
        }

        ddYear.setOnItemClickListener { _, _, _, _ ->
            val y     = ddYear.text?.toString()?.trim().orEmpty()
            val yearNo = y.toIntOrNull() ?: 1
            ddSem.setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    listOf(((yearNo * 2) - 1).toString(), (yearNo * 2).toString())
                )
            )
            ddSem.setText("", false)
            clearGeneratedIds()
        }

        ddSem.setOnItemClickListener { _, _, _, _ -> clearGeneratedIds() }
    }

    private fun clearGeneratedIds() {
        if (mode == "add") {
            etGrNo.setText("")
            etRoll.setText("")
            etDivision.setText("")
            assignedDivisionId   = ""
            assignedDivisionName = ""
            pendingRollNumber    = 0
            pendingRollText      = ""
            pendingGrNumber      = 0
            btnGenGr.isEnabled   = true
            btnGenRoll.isEnabled = true
        }
    }


    // ═══════════════════════════════════
    // ✅ Load Student (Edit/View)
    // ═══════════════════════════════════
    private fun loadStudentById() {
        val id = studentId ?: return
        db.collection("students").document(id).get()
            .addOnSuccessListener { d ->
                if (!d.exists()) {
                    Toast.makeText(this, "Student not found", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }
                val doc = d.data ?: emptyMap()
                if (courses.isEmpty()) pendingStudent = doc
                else fillFromDoc(doc)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Load failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ═══════════════════════════════════
    // ✅ Fill Form from Firestore Doc
    // ═══════════════════════════════════
    private fun fillFromDoc(doc: Map<String, Any?>) {
        fun gs(k: String) = (doc[k] as? String).orEmpty()

        etFullName.setText(gs("fullName"))
        etGrNo.setText(gs("grNo"))
        etRoll.setText(gs("rollNo"))
        etTempPassword.setText(gs("tempPassword"))
        etDob.setText(gs("dob"))
        etPhone.setText(gs("phone"))
        etEmail.setText(gs("email"))
        etAddress.setText(gs("address"))
        etAdmissionYear.setText(gs("admissionYear"))

        ddGender.setText(gs("gender"), false)
        ddBloodGroup.setText(gs("bloodGroup"), false)
        ddStatus.setText(gs("status").ifEmpty { "Active" }, false)

        // ✅ Fill division
        assignedDivisionId   = gs("divisionId")
        assignedDivisionName = gs("divisionName")
        etDivision.setText(
            assignedDivisionName.ifEmpty { "Not Assigned" }
        )

        tvPasswordStatus.text = "Password Status: " +
                if (gs("passwordStatus") == "active") "Active" else "Not Set"

        photoUrl = gs("photoUrl").ifEmpty { null }
        photoUrl?.let { url ->
            if (url.isNotEmpty()) {
                Glide.with(this)
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(imgProfile)
            }
        }

        val courseCode = gs("courseCode")
        val idx        = courses.indexOfFirst { it.code.equals(courseCode, true) }
        if (idx >= 0) {
            selectedCourse = courses[idx]
            ddCourse.setText(courses[idx].toString(), false)
            applyYearOptions(selectedCourse!!.durationYears)
        }

        val year = gs("year")
        val sem  = gs("semester")
        if (year.isNotBlank()) {
            ddYear.setText(year, false)
            val yNum = year.toIntOrNull() ?: 1
            ddSem.setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    listOf(((yNum * 2) - 1).toString(), (yNum * 2).toString())
                )
            )
        }
        if (sem.isNotBlank()) ddSem.setText(sem, false)
    }

    // ═══════════════════════════════════
    // ✅ Generate GR Number
    // ═══════════════════════════════════
    // ✅ FIXED: GR counter increments ONLY on Save
    private fun generateGrWithCounterAndCheck() {
        val course = selectedCourse ?: run { focusStable(ddCourse); return }
        val admissionYear = etAdmissionYear.text?.toString()?.trim().orEmpty()
        if (admissionYear.isEmpty()) { focusStable(etAdmissionYear); return }

        showCustomLoading("Generating GR number...")

        // ✅ READ counter without incrementing
        val counterRef = db.collection("meta")
            .document("gr_counters")
            .collection("byCourseYear")
            .document("${course.code}_$admissionYear")

        counterRef.get()
            .addOnSuccessListener { snap ->
                val lastUsed  = snap.getLong("last")?.toInt() ?: 0
                val startFrom = lastUsed + 1
                findFreeGrNumber(course, admissionYear, startFrom)
            }
            .addOnFailureListener {
                findFreeGrNumber(course, admissionYear, 1)
            }
    }

    private fun findFreeGrNumber(
        course       : CourseItem,
        admissionYear: String,
        start        : Int
    ) {
        val candidate = "${course.code}$admissionYear" +
                start.toString().padStart(3, '0')

        db.collection("students")
            .whereEqualTo("grNo", candidate)
            .limit(1).get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    findFreeGrNumber(course, admissionYear, start + 1)
                } else {
                    hideCustomLoading()
                    // ✅ Store pending GR - counter increments on Save
                    pendingGrNumber = start
                    etGrNo.setText(candidate)
                    btnGenGr.isEnabled = false
                    Toast.makeText(
                        this,
                        "✅ GR generated: $candidate",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                hideCustomLoading()
                pendingGrNumber = start
                etGrNo.setText(candidate)
                btnGenGr.isEnabled = false
            }
    }


    // ═══════════════════════════════════════════════════
    // ✅ UPDATED: Generate Roll + Auto Assign Division
    // ═══════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════
// ✅ FIXED: Generate Roll + Auto Assign Division
//           Checks course + year + semester + rollRange
//           Counter increments ONLY on actual Save
// ═══════════════════════════════════════════════════
    private fun generateRollWithCounterAndCheck() {
        val course = selectedCourse ?: run { focusStable(ddCourse); return }
        val year   = ddYear.text?.toString()?.trim().orEmpty()
        val sem    = ddSem.text?.toString()?.trim().orEmpty()
        val admissionYear = etAdmissionYear.text?.toString()?.trim().orEmpty()

        if (admissionYear.isEmpty()) { focusStable(etAdmissionYear); return }
        if (year.isEmpty()) {
            Toast.makeText(this, "⚠️ Select Year first", Toast.LENGTH_SHORT).show()
            focusStable(ddYear); return
        }
        if (sem.isEmpty()) {
            Toast.makeText(this, "⚠️ Select Semester first", Toast.LENGTH_SHORT).show()
            focusStable(ddSem); return
        }

        showCustomLoading("Checking divisions...")

        // ✅ FIRST: Check if ANY division exists for course+year+sem
        db.collection("divisions")
            .whereEqualTo("courseId", course.id)
            .whereEqualTo("year", year)
            .whereEqualTo("semester", sem)
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { divSnap ->
                hideCustomLoading()

                if (divSnap.isEmpty) {
                    // ✅ No division found for this course+year+sem
                    AlertDialog.Builder(this)
                        .setTitle("⚠️ No Division Found!")
                        .setMessage(
                            "No division exists for:\n" +
                                    "Course : ${course.name}\n" +
                                    "Year   : $year\n" +
                                    "Sem    : $sem\n\n" +
                                    "Please add a division first\n" +
                                    "before adding students!"
                        )
                        .setPositiveButton("Manage Divisions") { _, _ ->
                            // ✅ Navigate to Division management
                            finish()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    return@addOnSuccessListener
                }

                // ✅ Division(s) exist → now find free roll number
                // WITHOUT incrementing counter yet
                findFreeRollNumber(course, admissionYear, year, sem, divSnap.documents)
            }
            .addOnFailureListener { e ->
                hideCustomLoading()
                Toast.makeText(
                    this,
                    "❌ Failed to check divisions: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // ═══════════════════════════════════
    // ✅ Generate Temp Password
    // ═══════════════════════════════════
    private fun generateTempPassword() {
        val fullName   = etFullName.text?.toString()?.trim().orEmpty()
        val dob        = etDob.text?.toString()?.trim().orEmpty()
        val year       = dob.takeLast(4).takeIf { it.length == 4 } ?: "0000"
        val namePrefix = fullName.replace(" ", "").take(4).uppercase()
        etTempPassword.setText("$namePrefix@$year")
    }

    // ═══════════════════════════════════
    // ✅ Image Upload
    // ═══════════════════════════════════
    private fun checkPermissionsAndShowDialog() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission)
            == PackageManager.PERMISSION_GRANTED
        ) openGallery()
        else requestPermissionLauncher.launch(permission)
    }

    private fun openGallery() { pickImageLauncher.launch("image/*") }

    private fun startCrop(sourceUri: Uri) {
        val destUri = Uri.fromFile(
            File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )
        cropImageLauncher.launch(
            UCrop.of(sourceUri, destUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(800, 800)
                .withOptions(UCrop.Options().apply {
                    setCompressionQuality(80)
                    setCircleDimmedLayer(true)
                    setShowCropFrame(false)
                    setShowCropGrid(false)
                    setFreeStyleCropEnabled(false)
                })
                .getIntent(this)
        )
    }

    private fun displayCroppedImage(uri: Uri) {
        Glide.with(this).load(uri).circleCrop().into(imgProfile)
    }

    // ═══════════════════════════════════
    // ✅ On Save
    // ═══════════════════════════════════
    private fun onSave() {
        if (!validateRequired(
                listOf(
                    etFullName      to "Full name",
                    etDob           to "DOB",
                    ddGender        to "Gender",
                    etPhone         to "Phone",
                    etAddress       to "Address",
                    ddCourse        to "Course",
                    ddYear          to "Year",
                    ddSem           to "Semester",
                    etAdmissionYear to "Admission year",
                    ddStatus        to "Status",
                    etGrNo          to "GR number",
                    etRoll          to "Roll number",
                    etTempPassword  to "Temp password"
                )
            )
        ) return

        val fullName      = etFullName.text.toString().trim()
        val dob           = etDob.text.toString().trim()
        val gender        = ddGender.text.toString().trim()
        val bloodGroup    = ddBloodGroup.text.toString().trim()
        val phone         = etPhone.text.toString().trim()
        val email         = etEmail.text.toString().trim()
        val address       = etAddress.text.toString().trim()
        val course        = selectedCourse!!
        val year          = ddYear.text.toString().trim()
        val sem           = ddSem.text.toString().trim()
        val admissionYear = etAdmissionYear.text.toString().trim()
        val status        = ddStatus.text.toString().trim()
        val gr            = etGrNo.text.toString().trim()
        val roll          = etRoll.text.toString().trim()
        val tmpPwd        = etTempPassword.text.toString().trim()

        // ✅ Phone validation
        if (phone.length != 10) {
            Toast.makeText(this, "Phone must be 10 digits", Toast.LENGTH_SHORT).show()
            focusStable(etPhone)
            return
        }

        // ✅ Email validation
        if (email.isNotEmpty() &&
            !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        ) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            focusStable(etEmail)
            return
        }

        // ✅ Age validation
        try {
            val sdf       = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val birthDate = sdf.parse(dob)
            val cal       = Calendar.getInstance()
            cal.time      = birthDate!!
            val age       = Calendar.getInstance().get(Calendar.YEAR) -
                    cal.get(Calendar.YEAR)
            if (age < 15) {
                Toast.makeText(
                    this,
                    "Student must be at least 15 years old",
                    Toast.LENGTH_SHORT
                ).show()
                focusStable(etDob)
                return
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
            focusStable(etDob)
            return
        }

        // ✅ Division validation (only for ADD mode)
        if (mode == "add" && assignedDivisionId.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("⚠️ No Division Assigned!")
                .setMessage(
                    "Student has no division assigned.\n\n" +
                            "Please generate Roll Number first.\n" +
                            "If no division found, add a division " +
                            "with matching roll range!"
                )
                .setPositiveButton("OK", null)
                .show()
            return
        }

        if (croppedImageUri != null) {
            showCustomLoading("Preparing image...")
            lifecycleScope.launch {
                try {
                    updateLoadingMessage(
                        "Uploading photo\n(this may take 20-30 seconds)..."
                    )
                    val result = ImgBBUploader.uploadImage(
                        this@AdminAddStudentActivity, croppedImageUri!!
                    )
                    result.onSuccess { url ->
                        photoUrl = url
                        updateLoadingMessage("Photo uploaded! Saving student...")
                        saveStudentData(
                            fullName, dob, gender, bloodGroup, phone,
                            email, address, course, year, sem,
                            admissionYear, status, gr, roll, tmpPwd
                        )
                    }
                    result.onFailure { error ->
                        hideCustomLoading()
                        Toast.makeText(
                            this@AdminAddStudentActivity,
                            "❌ Photo upload failed: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    hideCustomLoading()
                    Toast.makeText(
                        this@AdminAddStudentActivity,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            showCustomLoading("Saving student...")
            saveStudentData(
                fullName, dob, gender, bloodGroup, phone,
                email, address, course, year, sem,
                admissionYear, status, gr, roll, tmpPwd
            )
        }
    }

    private fun updateLoadingMessage(message: String) {
        findViewById<View>(R.id.blockingOverlay)
            ?.findViewById<TextView>(R.id.tvStatus)
            ?.text = message
    }

    // ═══════════════════════════════════
    // ✅ Save Student Data
    // ═══════════════════════════════════
    private fun saveStudentData(
        fullName: String, dob: String, gender: String,
        bloodGroup: String, phone: String, email: String,
        address: String, course: CourseItem, year: String,
        sem: String, admissionYear: String, status: String,
        gr: String, roll: String, tmpPwd: String
    ) {
        findViewById<View>(R.id.blockingOverlay)
            ?.findViewById<TextView>(R.id.tvStatus)
            ?.text = "Saving student data..."

        val data = hashMapOf<String, Any>(
            "fullName"       to fullName,
            "dob"            to dob,
            "gender"         to gender,
            "bloodGroup"     to bloodGroup,
            "phone"          to phone,
            "email"          to email,
            "address"        to address,
            "courseId"       to course.id,
            "courseName"     to course.name,
            "courseCode"     to course.code,
            "year"           to year,
            "semester"       to sem,
            "admissionYear"  to admissionYear,
            "status"         to status,
            "grNo"           to gr,
            "rollNo"         to roll,
            "tempPassword"   to tmpPwd,
            "passwordStatus" to "not_set",
            "photoUrl"       to (photoUrl ?: ""),
            "divisionId"     to assignedDivisionId,    // ✅ NEW
            "divisionName"   to assignedDivisionName,  // ✅ NEW
            "updatedAt"      to Timestamp.now()
        )

        if (mode == "add") {
            data["createdAt"] = Timestamp.now()
            checkUniqueAndSave(email, phone, gr, roll, data)
        } else {
            val id = studentId ?: run {
                Toast.makeText(this, "Missing student ID", Toast.LENGTH_SHORT).show()
                hideCustomLoading()
                return
            }
            db.collection("students").document(id)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    hideCustomLoading()
                    Toast.makeText(
                        this, "✅ Student updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener { e ->
                    hideCustomLoading()
                    Toast.makeText(
                        this, "❌ Update failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    // ═══════════════════════════════════
    // ✅ Unique Check Before Save
    // ═══════════════════════════════════
    private fun checkUniqueAndSave(
        email: String,
        phone: String,
        gr   : String,
        roll : String,
        data : HashMap<String, Any>
    ) {
        // ✅ Step 1: Check Email
        db.collection("students")
            .whereEqualTo("email", email).limit(1).get()
            .addOnSuccessListener { emailSnap ->
                if (email.isNotEmpty() && !emailSnap.isEmpty) {
                    hideCustomLoading()
                    Toast.makeText(this, "❌ Email already exists", Toast.LENGTH_SHORT).show()
                    focusStable(etEmail)
                    return@addOnSuccessListener
                }

                // ✅ Step 2: Check Phone
                db.collection("students")
                    .whereEqualTo("phone", phone).limit(1).get()
                    .addOnSuccessListener { phoneSnap ->
                        if (!phoneSnap.isEmpty) {
                            hideCustomLoading()
                            Toast.makeText(this, "❌ Phone already exists", Toast.LENGTH_SHORT).show()
                            focusStable(etPhone)
                            return@addOnSuccessListener
                        }

                        // ✅ Step 3: Check GR Number
                        db.collection("students")
                            .whereEqualTo("grNo", gr).limit(1).get()
                            .addOnSuccessListener { grSnap ->
                                if (!grSnap.isEmpty) {
                                    hideCustomLoading()
                                    Toast.makeText(this, "❌ GR Number already exists", Toast.LENGTH_SHORT).show()
                                    focusStable(etGrNo)
                                    return@addOnSuccessListener
                                }

                                // ✅ Step 4: Check Roll Number
                                db.collection("students")
                                    .whereEqualTo("rollNo", roll).limit(1).get()
                                    .addOnSuccessListener { rollSnap ->
                                        if (!rollSnap.isEmpty) {
                                            hideCustomLoading()
                                            Toast.makeText(this, "❌ Roll Number already exists", Toast.LENGTH_SHORT).show()
                                            focusStable(etRoll)
                                            return@addOnSuccessListener
                                        }

                                        // ✅ Step 5: All checks passed → Save Student
                                        db.collection("students").add(data)
                                            .addOnSuccessListener { _ ->

                                                // ✅ Step 6: Increment counters ONLY after save
                                                val course        = selectedCourse
                                                val admissionYear = etAdmissionYear
                                                    .text.toString().trim()

                                                if (course != null && admissionYear.isNotEmpty()) {

                                                    // ✅ Increment GR counter
                                                    if (pendingGrNumber > 0) {
                                                        db.collection("meta")
                                                            .document("gr_counters")
                                                            .collection("byCourseYear")
                                                            .document("${course.code}_$admissionYear")
                                                            .set(
                                                                mapOf("last" to pendingGrNumber),
                                                                SetOptions.merge()
                                                            )
                                                    }

                                                    // ✅ Increment Roll counter
                                                    if (pendingRollNumber > 0) {
                                                        db.collection("meta")
                                                            .document("roll_counters")
                                                            .collection("byCourseYear")
                                                            .document("${course.code}_$admissionYear")
                                                            .set(
                                                                mapOf("last" to pendingRollNumber),
                                                                SetOptions.merge()
                                                            )
                                                    }
                                                }

                                                hideCustomLoading()
                                                Toast.makeText(
                                                    this,
                                                    "✅ Student saved!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                setResult(RESULT_OK)
                                                finish()
                                            }
                                            .addOnFailureListener { e ->
                                                hideCustomLoading()
                                                Toast.makeText(
                                                    this,
                                                    "❌ Save failed: ${e.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        hideCustomLoading()
                                        Toast.makeText(
                                            this,
                                            "❌ Roll check failed: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                hideCustomLoading()
                                Toast.makeText(
                                    this,
                                    "❌ GR check failed: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        hideCustomLoading()
                        Toast.makeText(
                            this,
                            "❌ Phone check failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                hideCustomLoading()
                Toast.makeText(
                    this,
                    "❌ Validation failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // ═══════════════════════════════════
    // ✅ Loading Helpers
    // ═══════════════════════════════════
    private fun showCustomLoading(message: String = "Saving student...") {
        isSaving          = true
        btnSave.isEnabled = false
        val overlay    = findViewById<View>(R.id.blockingOverlay)
        val loaderView = overlay?.findViewById<ImageView>(R.id.imgLoader)
        overlay?.findViewById<TextView>(R.id.tvStatus)?.text = message
        loaderView?.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.progress_ring_rotate)
        )
        overlay?.visibility = View.VISIBLE
    }

    private fun hideCustomLoading() {
        isSaving          = false
        btnSave.isEnabled = true
        val overlay    = findViewById<View>(R.id.blockingOverlay)
        overlay?.findViewById<ImageView>(R.id.imgLoader)?.clearAnimation()
        overlay?.visibility = View.GONE
    }
    // ✅ NEW: Find free roll number WITHOUT touching counter
// Counter increments ONLY when student is actually saved
    private fun findFreeRollNumber(
        course    : CourseItem,
        admissionYear: String,
        year      : String,
        sem       : String,
        divDocs   : List<com.google.firebase.firestore.DocumentSnapshot>
    ) {
        val yearShort = admissionYear.takeLast(2)

        // ✅ Build full roll set from ALL divisions
        // for this course+year+sem
        val allRollSets = mutableListOf<Triple<String, String, IntRange>>()
        // Triple = (divisionId, divisionName, rollRange)

        divDocs.forEach { doc ->
            val divId   = doc.id
            val divName = doc.getString("divisionName") ?: ""
            val ranges  = doc.get("rollNumberRanges")
                    as? List<Map<String, Any>> ?: emptyList()
            ranges.forEach { rm ->
                val s = (rm["start"] as? Long)?.toInt() ?: 0
                val e = (rm["end"]   as? Long)?.toInt() ?: 0
                allRollSets.add(Triple(divId, divName, s..e))
            }
        }

        if (allRollSets.isEmpty()) {
            Toast.makeText(
                this,
                "⚠️ Divisions exist but have no roll ranges!\n" +
                        "Please edit division and add roll ranges.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        showCustomLoading("Finding free roll number...")

        // ✅ Get current counter value (READ ONLY - no increment)
        val counterRef = db.collection("meta")
            .document("roll_counters")
            .collection("byCourseYear")
            .document("${course.code}_$admissionYear")

        counterRef.get()
            .addOnSuccessListener { snap ->
                val lastUsed = snap.getLong("last")?.toInt() ?: 0
                val startFrom = lastUsed + 1

                // ✅ Find next free roll starting from lastUsed+1
                findNextFreeRoll(
                    course, admissionYear, yearShort,
                    year, sem, startFrom, allRollSets
                )
            }
            .addOnFailureListener {
                // ✅ Counter doesn't exist yet → start from 1
                findNextFreeRoll(
                    course, admissionYear, yearShort,
                    year, sem, 1, allRollSets
                )
            }
    }
    // ✅ NEW: Recursively find free roll number
// Assigns division based on course+year+sem+rollRange
    private fun findNextFreeRoll(
        course       : CourseItem,
        admissionYear: String,
        yearShort    : String,
        year         : String,
        sem          : String,
        rollNum      : Int,
        allRollSets  : List<Triple<String, String, IntRange>>
    ) {
        // ✅ Find which division this rollNum belongs to
        val matchedDiv = allRollSets.firstOrNull { (_, _, range) ->
            rollNum in range
        }

        if (matchedDiv == null) {
            // ✅ Roll number exceeds all division ranges
            hideCustomLoading()
            AlertDialog.Builder(this)
                .setTitle("⚠️ Roll Range Full!")
                .setMessage(
                    "Roll number $rollNum exceeds all\n" +
                            "division roll ranges for:\n" +
                            "Course : ${course.name}\n" +
                            "Year   : $year  |  Sem: $sem\n\n" +
                            "Please edit division and\n" +
                            "increase the roll range!"
                )
                .setPositiveButton("Manage Divisions") { _, _ ->
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val candidate = "${course.code}$yearShort" +
                rollNum.toString().padStart(3, '0')

        // ✅ Check if this roll already exists in students
        db.collection("students")
            .whereEqualTo("rollNo", candidate)
            .limit(1).get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    // ✅ Roll taken → try next
                    findNextFreeRoll(
                        course, admissionYear, yearShort,
                        year, sem, rollNum + 1, allRollSets
                    )
                } else {
                    // ✅ Roll is FREE!
                    hideCustomLoading()

                    // ✅ Store pending roll number
                    // Counter will increment ONLY on actual Save
                    pendingRollNumber = rollNum
                    pendingRollText   = candidate

                    val divId   = matchedDiv.first
                    val divName = matchedDiv.second

                    assignedDivisionId   = divId
                    assignedDivisionName = divName
                    etRoll.setText(candidate)
                    etDivision.setText(divName)
                    btnGenRoll.isEnabled = false

                    Toast.makeText(
                        this,
                        "✅ Roll: $candidate → Division: $divName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                hideCustomLoading()
                // ✅ On error, use candidate anyway
                pendingRollNumber = rollNum
                pendingRollText   = candidate
                assignedDivisionId   = matchedDiv.first
                assignedDivisionName = matchedDiv.second
                etRoll.setText(candidate)
                etDivision.setText(matchedDiv.second)
                btnGenRoll.isEnabled = false
            }
    }

}
