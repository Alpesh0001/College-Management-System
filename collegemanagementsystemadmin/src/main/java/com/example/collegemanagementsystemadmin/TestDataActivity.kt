package com.example.collegemanagementsystemadmin

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class TestDataActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    // ✅ YOUR DIVISION DATA
    private val DIVISION_ID    = "mx1utOs8ehtTwIIQhNAp"
    private val DIVISION_NAME  = "A"
    private val COURSE_ID      = "bca"
    private val COURSE_NAME    = "Bachlor computer"
    private val COURSE_CODE    = "BCA"
    private val YEAR           = "1"
    private val SEMESTER       = "1"
    private val ADMISSION_YEAR = "2026"

    // ✅ 30 Student Names
    private val studentNames = listOf(
        "Aarav Shah",      "Bhavna Patel",    "Chirag Mehta",
        "Disha Joshi",     "Eshan Trivedi",   "Farida Shaikh",
        "Gaurav Desai",    "Hetal Modi",      "Ishaan Rana",
        "Jinal Parmar",    "Kiran Solanki",   "Lakshmi Nair",
        "Manav Gupta",     "Nidhi Sharma",    "Om Verma",
        "Priya Singh",     "Qasim Khan",      "Riya Dave",
        "Sagar Patil",     "Tanvi Jain",      "Uday Yadav",
        "Vandana Tiwari",  "Waqar Malik",     "Xena Patel",
        "Yash Agarwal",    "Zara Bhat",       "Arjun Pillai",
        "Bindu Reddy",     "Chetan Kulkarni", "Deepa Iyer"
    )

    // ✅ Views
    private lateinit var tvStatus       : TextView
    private lateinit var progressBar    : ProgressBar
    private lateinit var btnAddStudents : MaterialButton
    private lateinit var btnClear       : MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_data)

        // ✅ Bind views
        tvStatus       = findViewById(R.id.tvStatus)
        progressBar    = findViewById(R.id.progressBar)
        btnAddStudents = findViewById(R.id.btnAddStudents)
        btnClear       = findViewById(R.id.btnClearStudents)

        // ✅ Set info card
        findViewById<TextView>(R.id.tvInfoDivision).text = DIVISION_NAME
        findViewById<TextView>(R.id.tvInfoCourse).text   =
            "$COURSE_NAME ($COURSE_CODE)"

        // ✅ Buttons
        btnAddStudents.setOnClickListener { addTestStudents() }
        btnClear.setOnClickListener       { clearTestStudents() }
    }

    // ═══════════════════════════════════
    // ✅ Add 30 Test Students
    // ═══════════════════════════════════
    private fun addTestStudents() {
        btnAddStudents.isEnabled = false
        btnClear.isEnabled       = false
        progressBar.visibility   = View.VISIBLE
        tvStatus.text            = "Adding students... please wait"

        var added  = 0
        var failed = 0
        val total  = studentNames.size

        studentNames.forEachIndexed { index, name ->
            val rollNumber = index + 1

            val rollNo = "${COURSE_CODE}${ADMISSION_YEAR.takeLast(2)}${
                rollNumber.toString().padStart(3, '0')
            }"  // BCA26001

            val grNo = "${COURSE_CODE}${ADMISSION_YEAR}${
                rollNumber.toString().padStart(3, '0')
            }"  // BCA2026001

            val firstName    = name.split(" ")[0]
            val tempPassword = "${firstName}@2005"

            val studentData = hashMapOf<String, Any>(
                "fullName"       to name,
                "email"          to "",
                "phone"          to "000000000$rollNumber".takeLast(10),
                "address"        to "Test Address $rollNumber",
                "dob"            to "01/01/2005",
                "gender"         to if (rollNumber % 2 == 0) "Female" else "Male",
                "bloodGroup"     to listOf(
                    "A+","B+","O+","AB+",
                    "A-","B-","O-","AB-"
                )[rollNumber % 8],
                "courseId"       to COURSE_ID,
                "courseName"     to COURSE_NAME,
                "courseCode"     to COURSE_CODE,
                "year"           to YEAR,
                "semester"       to SEMESTER,
                "divisionId"     to DIVISION_ID,
                "divisionName"   to DIVISION_NAME,
                "admissionYear"  to ADMISSION_YEAR,
                "rollNo"         to rollNo,
                "grNo"           to grNo,
                "photoUrl"       to "",
                "status"         to "Active",
                "passwordStatus" to "not_set",
                "tempPassword"   to tempPassword,
                "createdAt"      to Timestamp.now(),
                "updatedAt"      to Timestamp.now()
            )

            db.collection("students")
                .add(studentData)
                .addOnSuccessListener {
                    added++
                    tvStatus.text = "Adding... $added / $total"
                    if (added + failed == total) onAllDone(added, failed)
                }
                .addOnFailureListener {
                    failed++
                    if (added + failed == total) onAllDone(added, failed)
                }
        }
    }

    // ═══════════════════════════════════
    // ✅ Clear All Test Students
    // ═══════════════════════════════════
    private fun clearTestStudents() {
        btnAddStudents.isEnabled = false
        btnClear.isEnabled       = false
        progressBar.visibility   = View.VISIBLE
        tvStatus.text            = "Clearing students..."

        db.collection("students")
            .whereEqualTo("divisionId", DIVISION_ID)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    progressBar.visibility   = View.GONE
                    btnAddStudents.isEnabled = true
                    btnClear.isEnabled       = true
                    tvStatus.text            = "No students found to clear!"
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }

                batch.commit()
                    .addOnSuccessListener {
                        progressBar.visibility   = View.GONE
                        btnAddStudents.isEnabled = true
                        btnClear.isEnabled       = true
                        tvStatus.text = "✅ Cleared ${snap.size()} students!"
                        Toast.makeText(
                            this,
                            "✅ Cleared ${snap.size()} students",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility   = View.GONE
                        btnAddStudents.isEnabled = true
                        btnClear.isEnabled       = true
                        tvStatus.text = "❌ Clear failed: ${e.message}"
                    }
            }
            .addOnFailureListener { e ->
                progressBar.visibility   = View.GONE
                btnAddStudents.isEnabled = true
                btnClear.isEnabled       = true
                tvStatus.text = "❌ Failed: ${e.message}"
            }
    }

    // ═══════════════════════════════════
    // ✅ On All Done
    // ═══════════════════════════════════
    private fun onAllDone(added: Int, failed: Int) {
        progressBar.visibility   = View.GONE
        btnAddStudents.isEnabled = true
        btnClear.isEnabled       = true
        tvStatus.text = "✅ Done! Added: $added | Failed: $failed"
        Toast.makeText(
            this,
            "✅ $added students added!",
            Toast.LENGTH_LONG
        ).show()
    }
}
