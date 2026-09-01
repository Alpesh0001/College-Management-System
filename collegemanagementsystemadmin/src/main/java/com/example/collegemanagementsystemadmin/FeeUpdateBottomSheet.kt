package com.example.collegemanagementsystemadmin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class FeeUpdateBottomSheet(
    private val student: StudentFeesAdapter.StudentFeeItem,
    private val semNumber: String,
    private val totalAmount: Long,
    private val onSaved: () -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var tvAvatar: TextView
    private lateinit var tvStudentName: TextView
    private lateinit var tvSemInfo: TextView
    private lateinit var tvCurrentBadge: TextView
    private lateinit var btnStatusPaid: Button
    private lateinit var btnStatusPartial: Button
    private lateinit var btnStatusPending: Button
    private lateinit var tilPaidAmount: TextInputLayout
    private lateinit var etPaidAmount: TextInputEditText
    private lateinit var tilPaidDate: TextInputLayout
    private lateinit var etPaidDate: TextInputEditText
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance()
    private var selectedStatus = student.status
    private var selectedDateMillis = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_fee_update, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        populateStudentInfo()
        setupStatusButtons()
        setupDatePicker()
        setupSaveCancel()

        // ✅ Pre-select current status
        selectStatus(student.status)

        // ✅ Pre-fill paid amount
        if (student.paidAmount > 0) {
            etPaidAmount.setText(student.paidAmount.toString())
        }
    }

    private fun bindViews(view: View) {
        tvAvatar         = view.findViewById(R.id.tvAvatar)
        tvStudentName    = view.findViewById(R.id.tvStudentName)
        tvSemInfo        = view.findViewById(R.id.tvSemInfo)
        tvCurrentBadge   = view.findViewById(R.id.tvCurrentBadge)
        btnStatusPaid    = view.findViewById(R.id.btnStatusPaid)
        btnStatusPartial = view.findViewById(R.id.btnStatusPartial)
        btnStatusPending = view.findViewById(R.id.btnStatusPending)
        tilPaidAmount    = view.findViewById(R.id.tilPaidAmount)
        etPaidAmount     = view.findViewById(R.id.etPaidAmount)
        tilPaidDate      = view.findViewById(R.id.tilPaidDate)
        etPaidDate       = view.findViewById(R.id.etPaidDate)
        btnCancel        = view.findViewById(R.id.btnCancel)
        btnSave          = view.findViewById(R.id.btnSave)
    }

    // ─────────────────────────────────────────────
    // Populate student info
    // ─────────────────────────────────────────────
    private fun populateStudentInfo() {
        // Avatar initials
        tvAvatar.text = student.name
            .trim()
            .split(" ")
            .take(2)
            .joinToString("") { it.first().uppercase() }

        tvStudentName.text = student.name
        tvSemInfo.text = "Semester $semNumber • Total: ₹${String.format("%,d", totalAmount)}"

        // Current badge
        updateBadge(student.status)
    }

    // ─────────────────────────────────────────────
    // Status button selection
    // ─────────────────────────────────────────────
    private fun setupStatusButtons() {
        btnStatusPaid.setOnClickListener    { selectStatus("paid") }
        btnStatusPartial.setOnClickListener { selectStatus("partial") }
        btnStatusPending.setOnClickListener { selectStatus("pending") }
    }

    private fun selectStatus(status: String) {
        selectedStatus = status

        // Reset all to dim
        btnStatusPaid.alpha    = 0.45f
        btnStatusPartial.alpha = 0.45f
        btnStatusPending.alpha = 0.45f

        // Highlight selected
        when (status.lowercase()) {
            "paid"    -> {
                btnStatusPaid.alpha = 1.0f
                // ✅ Auto-fill total amount when paid
                etPaidAmount.setText(totalAmount.toString())
                setDateToToday()
            }
            "partial" -> {
                btnStatusPartial.alpha = 1.0f
                // Clear for manual entry
                if (etPaidAmount.text.toString() == totalAmount.toString()) {
                    etPaidAmount.setText("")
                }
                setDateToToday()
            }
            "pending" -> {
                btnStatusPending.alpha = 1.0f
                etPaidAmount.setText("0")
            }
        }

        updateBadge(status)
    }

    private fun updateBadge(status: String) {
        val (text, drawable) = when (status.lowercase()) {
            "paid"    -> Pair("PAID",    R.drawable.bg_badge_green)
            "partial" -> Pair("PARTIAL", R.drawable.bg_badge_orange)
            else      -> Pair("PENDING", R.drawable.bg_badge_red)
        }
        tvCurrentBadge.text = text
        tvCurrentBadge.setBackgroundResource(drawable)
    }

    // ─────────────────────────────────────────────
    // Date Picker
    // ─────────────────────────────────────────────
    private fun setupDatePicker() {
        setDateToToday()
        etPaidDate.setOnClickListener { showDatePicker() }
    }

    private fun setDateToToday() {
        selectedDateMillis = System.currentTimeMillis()
        etPaidDate.setText(
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Date(selectedDateMillis))
        )
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                cal.set(year, month, day)
                selectedDateMillis = cal.timeInMillis
                etPaidDate.setText(
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(Date(selectedDateMillis))
                )
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ─────────────────────────────────────────────
    // Save + Cancel
    // ─────────────────────────────────────────────
    private fun setupSaveCancel() {
        btnCancel.setOnClickListener { dismiss() }
        btnSave.setOnClickListener   { saveFees() }
    }

    // ─────────────────────────────────────────────
    // ✅ Save fees to Firestore
    // ─────────────────────────────────────────────
    private fun saveFees() {
        val paidStr = etPaidAmount.text.toString().trim()

        if (paidStr.isEmpty()) {
            tilPaidAmount.error = "Enter paid amount"
            return
        }

        val paidAmount = paidStr.toLongOrNull() ?: 0L

        // ✅ Validate paid amount
        if (paidAmount > totalAmount) {
            tilPaidAmount.error = "Cannot exceed total ₹${String.format("%,d", totalAmount)}"
            return
        }

        tilPaidAmount.error = null
        btnSave.isEnabled   = false
        btnSave.text        = "Saving..."

        val semNum = semNumber.toIntOrNull() ?: 1

        // ✅ New semester entry
        val newSemEntry = mapOf(
            "semNumber"   to semNum.toLong(),
            "totalAmount" to totalAmount,
            "paidAmount"  to paidAmount,
            "status"      to selectedStatus,
            "paidDate"    to if (selectedStatus == "pending") 0L else selectedDateMillis,
            "receiptUrl"  to null
        )

        // ✅ Read existing fees doc first
        db.collection("fees")
            .document(student.studentId)
            .get()
            .addOnSuccessListener { doc ->
                val updatedSems: MutableList<Map<String, Any?>>

                if (doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val existingSems = (doc.get("semesters")
                            as? List<Map<String, Any?>>)
                        ?.toMutableList() ?: mutableListOf()

                    // ✅ Replace if same semNumber exists, else add
                    val idx = existingSems.indexOfFirst { map ->
                        (map["semNumber"] as? Long)?.toInt() == semNum
                    }

                    if (idx >= 0) existingSems[idx] = newSemEntry
                    else existingSems.add(newSemEntry)

                    updatedSems = existingSems
                } else {
                    // ✅ First time → create new doc
                    updatedSems = mutableListOf(newSemEntry)
                }

                // ✅ Save back
                db.collection("fees")
                    .document(student.studentId)
                    .set(mapOf(
                        "studentId" to student.studentId,
                        "semesters" to updatedSems
                    ))
                    .addOnSuccessListener {
                        btnSave.isEnabled = true
                        btnSave.text      = "Save"
                        Toast.makeText(
                            requireContext(),
                            "✅ Fees updated for ${student.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                        onSaved() // ✅ Refresh parent list
                        dismiss()
                    }
                    .addOnFailureListener { e ->
                        btnSave.isEnabled = true
                        btnSave.text      = "Save"
                        Toast.makeText(
                            requireContext(),
                            "❌ Failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                btnSave.isEnabled = true
                btnSave.text      = "Save"
                Toast.makeText(
                    requireContext(),
                    "❌ Failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
