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

class AdminAddFacultyActivity : CoreBaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var mode: String = "add"
    private var facultyId: String? = null

    private lateinit var topBar: MaterialToolbar
    private lateinit var scroll: ScrollView
    private lateinit var imgProfile: CircleImageView
    private lateinit var btnChangePhoto: Button

    // Personal Info
    private lateinit var etEmployeeId: EditText
    private lateinit var btnGenEmployeeId: Button
    private lateinit var etFullName: EditText
    private lateinit var etDob: EditText
    private lateinit var ddGender: AutoCompleteTextView
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText

    // Professional Info
    private lateinit var etQualification: EditText
    private lateinit var etSpecialization: EditText
    private lateinit var etExperience: EditText
    private lateinit var etJoiningDate: EditText
    private lateinit var ddDesignation: AutoCompleteTextView

    // Course & Role
    private lateinit var ddCourse: AutoCompleteTextView
    private lateinit var ddRole: AutoCompleteTextView
    private lateinit var tvHodWarning: TextView
    private lateinit var etSalary: EditText
    private lateinit var ddStatus: AutoCompleteTextView

    // Login Credentials
    private lateinit var etTempPassword: EditText
    private lateinit var btnGenPassword: Button
    // ========== GENERATE EMPLOYEE ID ==========
    private var generatedEmployeeId: String? = null // Track generated ID
    // Action Buttons
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    data class CourseItem(val id: String, val name: String, val code: String, val hodId: String?, val hodName: String?) {
        override fun toString(): String = "$name ($code)"
    }

    private val courses = mutableListOf<CourseItem>()
    private var selectedCourse: CourseItem? = null
    private var photoUrl: String? = null
    private var pendingFaculty: Map<String, Any?>? = null
    private var croppedImageUri: Uri? = null
    private var isSaving = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Storage permission required to upload photo", Toast.LENGTH_LONG).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { startCrop(it) }
    }

    private val cropImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                croppedImageUri = it
                displayCroppedImage(it)
                Toast.makeText(this, "✅ Photo selected. Click Save to upload.", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(this, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_faculty)

        mode = intent.getStringExtra("mode") ?: "add"
        facultyId = intent.getStringExtra("facultyId")

        bindViews()
        setupToolbar()
        setupStaticDropdowns()
        setupPickers()
        setupKeyboardScroll()
        setupButtons()
        loadCourses()

        if (mode != "add") {
            topBar.title = if (mode == "view") "View Faculty" else "Edit Faculty"
            loadFacultyById()
        }

        applyModeLocks()
        wireChangeInvalidators()
    }

    private fun bindViews() {
        topBar = findViewById(R.id.topBar)
        scroll = findViewById(R.id.scrollViewRoot)
        imgProfile = findViewById(R.id.imgProfile)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)

        etEmployeeId = findViewById(R.id.etEmployeeId)
        btnGenEmployeeId = findViewById(R.id.btnGenEmployeeId)
        etFullName = findViewById(R.id.etFullName)
        etDob = findViewById(R.id.etDob)
        ddGender = findViewById(R.id.ddGender)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etAddress = findViewById(R.id.etAddress)

        etQualification = findViewById(R.id.etQualification)
        etSpecialization = findViewById(R.id.etSpecialization)
        etExperience = findViewById(R.id.etExperience)
        etJoiningDate = findViewById(R.id.etJoiningDate)
        ddDesignation = findViewById(R.id.ddDesignation)

        ddCourse = findViewById(R.id.ddCourse)
        ddRole = findViewById(R.id.ddRole)
        tvHodWarning = findViewById(R.id.tvHodWarning)
        etSalary = findViewById(R.id.etSalary)
        ddStatus = findViewById(R.id.ddStatus)

        etTempPassword = findViewById(R.id.etTempPassword)
        btnGenPassword = findViewById(R.id.btnGenPassword)

        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        listOf(ddGender, ddDesignation, ddCourse, ddRole, ddStatus).forEach { v ->
            v.setOnClickListener { v.showDropDown() }
        }
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupStaticDropdowns() {
        val genderOptions = listOf("Male", "Female", "Other")
        ddGender.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, genderOptions))

        val designations = listOf(
            "Professor",
            "Associate Professor",
            "Assistant Professor",
            "Lecturer",
            "Senior Lecturer",
            "Guest Lecturer"
        )
        ddDesignation.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, designations))

        val roles = listOf("Faculty", "HOD")
        ddRole.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, roles))
        ddRole.setText("Faculty", false)

        val statusOptions = listOf("Active", "Inactive", "On Leave", "Resigned")
        ddStatus.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, statusOptions))
        ddStatus.setText("Active", false)
    }

    private fun applyModeLocks() {
        val isView = mode == "view"
        val isEdit = mode == "edit"

        if (isView) {
            val allFields = listOf<View>(
                etEmployeeId, etFullName, etDob, ddGender, etPhone, etEmail, etAddress,
                etQualification, etSpecialization, etExperience, etJoiningDate, ddDesignation,
                ddCourse, ddRole, etSalary, ddStatus, etTempPassword
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

            findViewById<TextInputLayout>(R.id.tilGender).isEnabled = false
            findViewById<TextInputLayout>(R.id.tilDesignation).isEnabled = false
            findViewById<TextInputLayout>(R.id.tilCourse).isEnabled = false
            findViewById<TextInputLayout>(R.id.tilRole).isEnabled = false
            findViewById<TextInputLayout>(R.id.tilStatus).isEnabled = false

            btnChangePhoto.isEnabled = false
            btnGenEmployeeId.isEnabled = false
            btnGenPassword.isEnabled = false
            btnSave.visibility = View.GONE
            btnCancel.text = "Close"
        } else if (isEdit) {
            etEmployeeId.isEnabled = false
            etTempPassword.isEnabled = false
            etJoiningDate.isEnabled = false

            findViewById<TextInputLayout>(R.id.tilEmployeeId).isEnabled = false
            findViewById<TextInputLayout>(R.id.tilTempPassword).isEnabled = false
            findViewById<TextInputLayout>(R.id.tilJoiningDate).isEnabled = false

            btnGenEmployeeId.isEnabled = false
            btnGenPassword.isEnabled = false
        }
    }

    private fun setupPickers() {
        etDob.setOnClickListener { showDatePicker { date -> etDob.setText(date) } }
        etJoiningDate.setOnClickListener { showDatePicker { date -> etJoiningDate.setText(date) } }
    }

    private fun showDatePicker(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                val day = "%02d".format(Locale.US, d)
                val mon = "%02d".format(Locale.US, m + 1)
                onPicked("$day/$mon/$y")
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupKeyboardScroll() {
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.post {
                    // Only scroll if the view is not fully visible
                    val scrollBounds = android.graphics.Rect()
                    scroll.getHitRect(scrollBounds)

                    if (!v.getLocalVisibleRect(scrollBounds)) {
                        // View is not fully visible, scroll to it
                        scroll.smoothScrollTo(0, (v.top - 100).coerceAtLeast(0))
                    }
                }
            }
        }

        listOf(
            etEmployeeId, etFullName, etDob, etPhone, etEmail, etAddress,
            etQualification, etSpecialization, etExperience, etJoiningDate, etSalary
        ).forEach { it.onFocusChangeListener = focusListener }
    }


    private fun setupButtons() {
        btnChangePhoto.setOnClickListener {
            if (mode == "view") {
                Toast.makeText(this, "Cannot change photo in view mode", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkPermissionsAndShowDialog()
        }

        btnGenEmployeeId.setOnClickListener {
            generateEmployeeId()
        }

        btnGenPassword.setOnClickListener {
            if (!validateRequired(listOf(etFullName to "Full name", etDob to "DOB"))) return@setOnClickListener
            generateTempPassword()
        }

        btnSave.setOnClickListener {
            if (isSaving) {
                Toast.makeText(this, "Please wait, saving...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onSave()
        }

        btnCancel.setOnClickListener { finish() }
    }

    private fun validateRequired(pairs: List<Pair<View, String>>): Boolean {
        val missing = pairs.filter { (v, _) ->
            val value = when (v) {
                is EditText -> v.text?.toString()?.trim().orEmpty()
                is AutoCompleteTextView -> v.text?.toString()?.trim().orEmpty()
                else -> ""
            }
            value.isEmpty()
        }
        if (missing.isEmpty()) return true
        val firstView = missing.first().first
        val message = missing.joinToString(", ") { it.second } + " required"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        focusStable(firstView)
        return false
    }

    private fun focusStable(view: View) {
        view.requestFocus()
        view.post { scroll.smoothScrollTo(0, (view.top - 48).coerceAtLeast(0)) }
    }

    private fun loadCourses() {
        db.collection("courses")
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snap ->
                courses.clear()
                snap.documents.forEach { d ->
                    val status = d.getString("status") ?: "Active"
                    if (status == "Active") {
                        val id = d.id
                        val name = d.getString("name").orEmpty()
                        val code = d.getString("code").orEmpty()
                        val hodId = d.getString("hodId")
                        val hodName = d.getString("hodName")
                        courses.add(CourseItem(id, name, code, hodId, hodName))
                    }
                }
                ddCourse.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, courses))

                pendingFaculty?.let { fillFromDoc(it); pendingFaculty = null }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load courses: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun wireChangeInvalidators() {
        ddCourse.setOnItemClickListener { _, _, pos, _ ->
            selectedCourse = courses.getOrNull(pos)
        }

        ddRole.setOnItemClickListener { _, _, _, _ ->
            val role = ddRole.text.toString().trim()
            if (role == "HOD" && selectedCourse != null) {
                val currentHod = selectedCourse!!.hodName
                if (!currentHod.isNullOrEmpty()) {
                    tvHodWarning.text = "⚠️ Note: Current HOD is $currentHod. Selecting HOD will replace them."
                    tvHodWarning.visibility = View.VISIBLE
                } else {
                    tvHodWarning.visibility = View.VISIBLE
                }
            } else {
                tvHodWarning.visibility = View.GONE
            }
        }
    }

    private fun loadFacultyById() {
        val id = facultyId ?: return
        db.collection("faculties").document(id).get()
            .addOnSuccessListener { d ->
                if (!d.exists()) {
                    Toast.makeText(this, "Faculty not found", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }
                val doc = d.data ?: emptyMap()
                if (courses.isEmpty()) {
                    pendingFaculty = doc
                } else {
                    fillFromDoc(doc)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Load failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun fillFromDoc(doc: Map<String, Any?>) {
        fun gs(k: String) = (doc[k] as? String).orEmpty()

        etEmployeeId.setText(gs("employeeId"))
        etFullName.setText(gs("fullName"))
        etDob.setText(gs("dateOfBirth"))
        etPhone.setText(gs("phone"))
        etEmail.setText(gs("email"))
        etAddress.setText(gs("address"))

        etQualification.setText(gs("qualification"))
        etSpecialization.setText(gs("specialization"))
        etExperience.setText(gs("experience"))
        etJoiningDate.setText(gs("joiningDate"))

        val salary = doc["salary"]
        if (salary != null) {
            etSalary.setText(salary.toString())
        }

        etTempPassword.setText(gs("tempPassword"))

        ddGender.setText(gs("gender"), false)
        ddDesignation.setText(gs("designation"), false)
        ddRole.setText(gs("role").ifEmpty { "Faculty" }, false)
        ddStatus.setText(gs("status").ifEmpty { "Active" }, false)

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
        val idx = courses.indexOfFirst { it.code.equals(courseCode, true) }
        if (idx >= 0) {
            selectedCourse = courses[idx]
            ddCourse.setText(courses[idx].toString(), false)
        }
    }

    private fun generateEmployeeId() {
        // If already generated in this session, don't generate again
        if (!etEmployeeId.text.isNullOrEmpty()) {
            Toast.makeText(this, "Employee ID already generated", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Generating Employee ID...", Toast.LENGTH_SHORT).show()

        val counterRef = db.collection("meta").document("employee_counters")

        // Just READ the counter, don't increment yet
        counterRef.get()
            .addOnSuccessListener { snap ->
                val lastCounter = snap.getLong("last") ?: 0L
                val nextCounter = lastCounter.toInt() + 1

                Log.d("EMP_GENERATION", "Current counter: $lastCounter, trying: $nextCounter")

                // Find next available ID starting from nextCounter
                findNextAvailableEmployeeId(nextCounter)
            }
            .addOnFailureListener { e ->
                Log.e("EMP_GENERATION", "Failed to read counter: ${e.message}")
                Toast.makeText(this, "Error generating Employee ID", Toast.LENGTH_LONG).show()
            }
    }

    private fun findNextAvailableEmployeeId(start: Int) {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val candidate = "EMP$year${start.toString().padStart(3, '0')}"

        Log.d("EMP_GENERATION", "Checking if $candidate is free...")

        db.collection("faculties").whereEqualTo("employeeId", candidate).limit(1).get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    Log.d("EMP_GENERATION", "$candidate already exists, trying next...")
                    findNextAvailableEmployeeId(start + 1)
                } else {
                    Log.d("EMP_GENERATION", "$candidate is free! Using it.")
                    generatedEmployeeId = candidate
                    etEmployeeId.setText(candidate)
                    btnGenEmployeeId.isEnabled = false
                    Toast.makeText(this, "✅ Employee ID generated: $candidate", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("EMP_GENERATION", "Check failed: ${e.message}")
                Toast.makeText(this, "Error checking Employee ID availability", Toast.LENGTH_LONG).show()
            }
    }


    // ========== GENERATE TEMP PASSWORD ==========
    private fun generateTempPassword() {
        val fullName = etFullName.text?.toString()?.trim().orEmpty()
        val dob = etDob.text?.toString()?.trim().orEmpty()
        val year = dob.takeLast(4).takeIf { it.length == 4 } ?: "0000"

        val namePrefix = fullName.replace(" ", "").take(4).uppercase()
        val password = "$namePrefix@$year"

        etTempPassword.setText(password)
        Toast.makeText(this, "✅ Password generated", Toast.LENGTH_SHORT).show()
    }

    // ========== IMAGE UPLOAD METHODS ==========
    private fun checkPermissionsAndShowDialog() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val permissionStatus = ContextCompat.checkSelfPermission(this, permission)

        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, destinationFileName))

        val options = UCrop.Options().apply {
            setCompressionQuality(80)
            setCircleDimmedLayer(true)
            setShowCropFrame(false)
            setShowCropGrid(false)
            setFreeStyleCropEnabled(false)
        }

        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800)
            .withOptions(options)

        cropImageLauncher.launch(uCrop.getIntent(this))
    }

    private fun displayCroppedImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(imgProfile)
    }

    // ========== SAVE FACULTY ==========
    private fun onSave() {
        if (!validateRequired(
                listOf(
                    etEmployeeId to "Employee ID",
                    etFullName to "Full name",
                    etDob to "DOB",
                    ddGender to "Gender",
                    etPhone to "Phone",
                    etEmail to "Email",
                    etAddress to "Address",
                    etQualification to "Qualification",
                    etExperience to "Experience",
                    etJoiningDate to "Joining date",
                    ddDesignation to "Designation",
                    ddCourse to "Course",
                    ddRole to "Role",
                    ddStatus to "Status",
                    etTempPassword to "Temp password"
                )
            )
        ) {
            return
        }

        val employeeId = etEmployeeId.text.toString().trim()
        val fullName = etFullName.text.toString().trim()
        val dob = etDob.text.toString().trim()
        val gender = ddGender.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val qualification = etQualification.text.toString().trim()
        val specialization = etSpecialization.text.toString().trim()
        val experience = etExperience.text.toString().trim()
        val joiningDate = etJoiningDate.text.toString().trim()
        val designation = ddDesignation.text.toString().trim()
        val course = selectedCourse!!
        val role = ddRole.text.toString().trim()
        val salaryText = etSalary.text.toString().trim()
        val salary = salaryText.toIntOrNull()
        val status = ddStatus.text.toString().trim()
        val tmpPwd = etTempPassword.text.toString().trim()

        if (phone.length != 10) {
            Toast.makeText(this, "Phone must be 10 digits", Toast.LENGTH_SHORT).show()
            focusStable(etPhone)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            focusStable(etEmail)
            return
        }

        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            sdf.parse(dob)
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
            focusStable(etDob)
            return
        }

        if (croppedImageUri != null) {
            showCustomLoading("Preparing image...")

            lifecycleScope.launch {
                try {
                    updateLoadingMessage("Uploading photo \n(this may take 20-30 seconds)...")

                    val result = ImgBBUploader.uploadImage(this@AdminAddFacultyActivity, croppedImageUri!!)

                    result.onSuccess { url ->
                        photoUrl = url
                        updateLoadingMessage("Photo uploaded! \n Saving faculty...")

                        saveFacultyData(employeeId, fullName, dob, gender, phone, email, address,
                            qualification, specialization, experience, joiningDate, designation,
                            course, role, salary, status, tmpPwd)
                    }

                    result.onFailure { error ->
                        hideCustomLoading()
                        Toast.makeText(
                            this@AdminAddFacultyActivity,
                            "❌ Photo upload failed: ${error.message}. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    hideCustomLoading()
                    Toast.makeText(
                        this@AdminAddFacultyActivity,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            showCustomLoading("Saving faculty...")
            saveFacultyData(employeeId, fullName, dob, gender, phone, email, address,
                qualification, specialization, experience, joiningDate, designation,
                course, role, salary, status, tmpPwd)
        }
    }

    private fun updateLoadingMessage(message: String) {
        val overlay = findViewById<View>(R.id.blockingOverlay)
        val statusText = overlay?.findViewById<TextView>(R.id.tvStatus)
        statusText?.text = message
    }

    // ========== SAVE FACULTY DATA TO FIRESTORE ==========
    private fun saveFacultyData(
        employeeId: String,
        fullName: String,
        dob: String,
        gender: String,
        phone: String,
        email: String,
        address: String,
        qualification: String,
        specialization: String,
        experience: String,
        joiningDate: String,
        designation: String,
        course: CourseItem,
        role: String,
        salary: Int?,
        status: String,
        tmpPwd: String
    ) {
        val overlay = findViewById<View>(R.id.blockingOverlay)
        val statusText = overlay?.findViewById<TextView>(R.id.tvStatus)
        statusText?.text = "Saving faculty data..."

        val data: HashMap<String, Any?> = hashMapOf(
            "employeeId" to employeeId,
            "fullName" to fullName,
            "dateOfBirth" to dob,
            "gender" to gender,
            "phone" to phone,
            "email" to email,
            "address" to address,
            "qualification" to qualification,
            "specialization" to specialization,
            "experience" to experience,
            "joiningDate" to joiningDate,
            "designation" to designation,
            "role" to role,
            "courseCode" to course.code,
            "courseName" to course.name,
            "courseId" to course.id,
            "salary" to salary,
            "status" to status,
            "tempPassword" to tmpPwd,
            "passwordStatus" to "not_set",
            "photoUrl" to (photoUrl ?: ""),
            "updatedAt" to Timestamp.now()
        )

        if (mode == "add") {
            data["createdAt"] = Timestamp.now()
            data["createdBy"] = "admin123" // Replace with actual admin ID
            checkUniqueAndSave(email, phone, employeeId, data, course, role)
        } else {
            val id = facultyId ?: run {
                Toast.makeText(this, "Missing faculty ID", Toast.LENGTH_SHORT).show()
                hideCustomLoading()
                return
            }

            // If role changed to HOD, update course
            if (role == "HOD") {
                updateCourseHOD(course.id, id, fullName, email, phone)
            }

            db.collection("faculties").document(id).set(data, SetOptions.merge())
                .addOnSuccessListener {
                    hideCustomLoading()
                    Toast.makeText(this, "✅ Faculty updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener { e ->
                    hideCustomLoading()
                    Toast.makeText(this, "❌ Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun checkUniqueAndSave(
        email: String,
        phone: String,
        employeeId: String,
        data: HashMap<String, Any?>,
        course: CourseItem,
        role: String
    ) {
        db.collection("faculties").whereEqualTo("email", email).limit(1).get()
            .addOnSuccessListener { emailSnap ->
                if (!emailSnap.isEmpty) {
                    hideCustomLoading()
                    Toast.makeText(this, "❌ Email already exists", Toast.LENGTH_SHORT).show()
                    focusStable(etEmail)
                    return@addOnSuccessListener
                }

                db.collection("faculties").whereEqualTo("phone", phone).limit(1).get()
                    .addOnSuccessListener { phoneSnap ->
                        if (!phoneSnap.isEmpty) {
                            hideCustomLoading()
                            Toast.makeText(this, "❌ Phone already exists", Toast.LENGTH_SHORT).show()
                            focusStable(etPhone)
                            return@addOnSuccessListener
                        }

                        db.collection("faculties").whereEqualTo("employeeId", employeeId).limit(1).get()
                            .addOnSuccessListener { empSnap ->
                                if (!empSnap.isEmpty) {
                                    hideCustomLoading()
                                    Toast.makeText(this, "❌ Employee ID already exists", Toast.LENGTH_SHORT).show()
                                    focusStable(etEmployeeId)
                                    return@addOnSuccessListener
                                }

                                // ✅ NOW increment the counter (only when actually saving)
                                if (generatedEmployeeId == employeeId) {
                                    updateEmployeeCounter(employeeId) {
                                        saveFacultyToFirestore(data, course, role, email, phone)
                                    }
                                } else {
                                    saveFacultyToFirestore(data, course, role, email, phone)
                                }
                            }
                    }
            }
            .addOnFailureListener { e ->
                hideCustomLoading()
                Toast.makeText(this, "❌ Validation failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // NEW method to update counter
    private fun updateEmployeeCounter(employeeId: String, onSuccess: () -> Unit) {
        val counterRef = db.collection("meta").document("employee_counters")

        // Extract number from employeeId (e.g., "EMP2026001" -> 1)
        val numberPart = employeeId.substringAfterLast("EMP").takeLast(3).toIntOrNull() ?: 1

        counterRef.get()
            .addOnSuccessListener { snap ->
                val currentCounter = snap.getLong("last") ?: 0L

                // Update counter to the number we just used
                if (numberPart > currentCounter) {
                    counterRef.update("last", numberPart.toLong())
                        .addOnSuccessListener {
                            Log.d("EMP_GENERATION", "Counter updated to $numberPart")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            Log.e("EMP_GENERATION", "Failed to update counter: ${e.message}")
                            onSuccess() // Continue anyway
                        }
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { e ->
                Log.e("EMP_GENERATION", "Failed to read counter: ${e.message}")
                onSuccess() // Continue anyway
            }
    }

    // NEW method to save faculty
    private fun saveFacultyToFirestore(
        data: HashMap<String, Any?>,
        course: CourseItem,
        role: String,
        email: String,
        phone: String
    ) {
        db.collection("faculties").add(data)
            .addOnSuccessListener { docRef ->
                // If HOD, update course
                if (role == "HOD") {
                    updateCourseHOD(course.id, docRef.id, data["fullName"] as String, email, phone)
                } else {
                    hideCustomLoading()
                    Toast.makeText(this, "✅ Faculty saved successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                hideCustomLoading()
                Toast.makeText(this, "❌ Save failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun updateCourseHOD(courseId: String, facultyId: String, facultyName: String, email: String, phone: String) {
        val courseData = mapOf(
            "hodId" to facultyId,
            "hodName" to facultyName,
            "hodEmail" to email,
            "hodPhone" to phone,
            "updatedAt" to Timestamp.now()
        )

        db.collection("courses").document(courseId).update(courseData)
            .addOnSuccessListener {
                hideCustomLoading()
                Toast.makeText(this, "✅ Faculty saved and assigned as HOD", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                hideCustomLoading()
                Toast.makeText(this, "⚠️ Faculty saved but HOD update failed: ${e.message}", Toast.LENGTH_LONG).show()
                setResult(RESULT_OK)
                finish()
            }
    }

    // ========== CUSTOM LOADING HELPERS ==========
    private fun showCustomLoading(message: String = "Saving faculty...") {
        isSaving = true
        btnSave.isEnabled = false

        val overlay = findViewById<View>(R.id.blockingOverlay)
        val loaderView = overlay?.findViewById<ImageView>(R.id.imgLoader)
        val statusText = overlay?.findViewById<TextView>(R.id.tvStatus)

        statusText?.text = message

        loaderView?.let {
            val anim = AnimationUtils.loadAnimation(
                this,
                R.anim.progress_ring_rotate
            )
            it.startAnimation(anim)
        }

        overlay?.visibility = View.VISIBLE
    }

    private fun hideCustomLoading() {
        isSaving = false
        btnSave.isEnabled = true

        val overlay = findViewById<View>(R.id.blockingOverlay)
        val loaderView = overlay?.findViewById<ImageView>(R.id.imgLoader)

        loaderView?.clearAnimation()

        overlay?.visibility = View.GONE
    }
}
