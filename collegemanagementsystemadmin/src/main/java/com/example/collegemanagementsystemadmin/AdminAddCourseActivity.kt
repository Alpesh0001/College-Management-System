package com.example.collegemanagementsystemadmin

import android.os.Bundle
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminAddCourseActivity : CoreBaseActivity() {

    // Views
    private lateinit var topBar: MaterialToolbar
    private lateinit var tilCourseName: TextInputLayout
    private lateinit var tilCourseCode: TextInputLayout
    private lateinit var tilDurationYears: TextInputLayout
    private lateinit var tilStatus: TextInputLayout

    private lateinit var etName: TextInputEditText
    private lateinit var etCode: TextInputEditText
    private lateinit var ddYears: AutoCompleteTextView
    private lateinit var ddStatus: AutoCompleteTextView

    // ✅ Sem fees views
    private lateinit var layoutSemFees: LinearLayout
    private lateinit var containerSemFees: LinearLayout

    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var progress: ProgressBar
    private lateinit var formContainer: View

    // ✅ Store dynamic fee inputs
    private val semFeeInputs = mutableMapOf<Int, TextInputEditText>()

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_course)

        bindViews()
        setupToolbar()
        setupDropdowns()
        setupButtons()
    }

    private fun bindViews() {
        topBar           = findViewById(R.id.topBar)
        tilCourseName    = findViewById(R.id.tilCourseName)
        tilCourseCode    = findViewById(R.id.tilCourseCode)
        tilDurationYears = findViewById(R.id.tilDurationYears)
        tilStatus        = findViewById(R.id.tilStatus)
        etName           = findViewById(R.id.etNamePlain)
        etCode           = findViewById(R.id.etCodePlain)
        ddYears          = findViewById(R.id.etYearsPlain)
        ddStatus         = findViewById(R.id.ddStatusPlain)
        layoutSemFees    = findViewById(R.id.layoutSemFees)    // ✅
        containerSemFees = findViewById(R.id.containerSemFees) // ✅
        btnSave          = findViewById(R.id.btnSavePlain)
        btnCancel        = findViewById(R.id.btnCancelPlain)
        progress         = findViewById(R.id.progressOverlay)
        formContainer    = findViewById(R.id.formContainer)
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener { finish() }
    }

    private fun setupDropdowns() {
        // Duration Years Dropdown
        val yearOptions = listOf("1", "2", "3", "4", "5", "6")
        ddYears.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, yearOptions)
        )
        ddYears.setOnClickListener { ddYears.showDropDown() }

        // ✅ When year selected → show sem fee fields
        ddYears.setOnItemClickListener { _, _, position, _ ->
            val selectedYears = (position + 1)
            buildSemFeeFields(selectedYears)
        }

        // Status Dropdown
        ddStatus.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("Active", "Inactive"))
        )
        ddStatus.setText("Active", false)
        ddStatus.setOnClickListener { ddStatus.showDropDown() }
    }

    // ─────────────────────────────────────────────
    // ✅ Build dynamic sem fee input fields
    // ─────────────────────────────────────────────
    private fun buildSemFeeFields(durationYears: Int) {
        containerSemFees.removeAllViews()
        semFeeInputs.clear()

        val totalSems = durationYears * 2

        for (sem in 1..totalSems) {
            // Row: "Semester X" label + fee input
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 12.dpToPx()
                layoutParams = params
            }

            // Sem label
            val label = TextView(this).apply {
                text = "Semester $sem"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Fee input layout
            val til = com.google.android.material.textfield.TextInputLayout(
                this,
                null,
                com.google.android.material.R.attr.textInputOutlinedStyle
            ).apply {
                hint = "Amount (₹)"
                prefixText = "₹"
                layoutParams = LinearLayout.LayoutParams(
                    160.dpToPx(),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val etFee = TextInputEditText(til.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                maxLines = 1
            }

            til.addView(etFee)
            row.addView(label)
            row.addView(til)
            containerSemFees.addView(row)

            // Store reference
            semFeeInputs[sem] = etFee
        }

        // ✅ Show the section
        layoutSemFees.visibility = View.VISIBLE
    }

    private fun setupButtons() {
        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveCourse() }
    }

    // ─────────────────────────────────────────────
    // Save Course + Semester Fees
    // ─────────────────────────────────────────────
    private fun saveCourse() {
        tilCourseName.error = null
        tilCourseCode.error = null
        tilDurationYears.error = null
        tilStatus.error = null

        val name   = etName.text.toString().trim()
        val code   = etCode.text.toString().trim().uppercase()
        val years  = ddYears.text.toString().trim()
        val status = ddStatus.text.toString().trim()

        var hasError = false

        if (name.isEmpty()) {
            tilCourseName.error = "Course name is required"
            hasError = true
        }
        if (code.isEmpty()) {
            tilCourseCode.error = "Course code is required"
            hasError = true
        } else if (!code.matches(Regex("^[A-Z0-9]{2,8}$"))) {
            tilCourseCode.error = "Use 2-8 uppercase letters/digits only"
            hasError = true
        }
        if (years.isEmpty()) {
            tilDurationYears.error = "Duration is required"
            hasError = true
        }
        if (status.isEmpty()) {
            tilStatus.error = "Status is required"
            hasError = true
        }

        if (hasError) return

        val yearsInt = years.toIntOrNull() ?: 0

        // ✅ Validate + collect sem fees
        val semesterFees = mutableMapOf<String, Long>()
        for ((sem, input) in semFeeInputs) {
            val value = input.text.toString().trim()
            if (value.isEmpty()) {
                input.error = "Required"
                Toast.makeText(this, "Enter fees for Semester $sem", Toast.LENGTH_SHORT).show()
                return
            }
            semesterFees[sem.toString()] = value.toLongOrNull() ?: 0L
        }

        val courseKey = code.lowercase()
        val uid = auth.currentUser?.uid.orEmpty()

        val data = hashMapOf(
            "name"          to name,
            "code"          to code,
            "courseKey"     to courseKey,
            "durationYears" to yearsInt,
            "status"        to status,
            "semesterFees"  to semesterFees, // ✅ Save fees map
            "createdAt"     to Timestamp.now(),
            "createdBy"     to uid
        )

        setLoading(true)

        val docRef = db.collection("courses").document(courseKey)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    setLoading(false)
                    tilCourseCode.error = "Course code already exists"
                } else {
                    docRef.set(data)
                        .addOnSuccessListener {
                            setLoading(false)
                            Toast.makeText(this, "✅ Course added!", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            setLoading(false)
                            Toast.makeText(this, "❌ ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "❌ ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        formContainer.isEnabled = !loading
        formContainer.alpha = if (loading) 0.6f else 1.0f
        btnSave.isEnabled = !loading
        btnCancel.isEnabled = !loading
        etName.isEnabled = !loading
        etCode.isEnabled = !loading
        ddYears.isEnabled = !loading
        ddStatus.isEnabled = !loading
    }

    // ✅ Extension: Int to pixels
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
