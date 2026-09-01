package com.example.collagemanagmentsystem

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collagemanagmentsystem.R
import com.example.collagemanagmentsystem.adapters.FeesAdapter
import com.example.collagemanagmentsystem.utils.CoreBaseActivity
import com.example.collagemanagmentsystem.utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class FeesActivity : CoreBaseActivity(), PaymentResultListener {

    private val TAG = "FeesActivity"

    // ── Views ─────────────────────────────────
    private lateinit var topBar: MaterialToolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var rvSemesters: RecyclerView
    private lateinit var layoutEmpty: View

    // ── Current Sem Card Views ─────────────────
    private lateinit var tvCurrentSemLabel: TextView
    private lateinit var tvCurrentSemBadge: TextView
    private lateinit var tvCurrentPaid: TextView
    private lateinit var tvCurrentTotal: TextView
    private lateinit var tvCurrentRemaining: TextView
    private lateinit var currentSemCard: MaterialCardView

    // ── Firebase + Session ─────────────────────
    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    // ── Adapter ───────────────────────────────
    private lateinit var adapter: FeesAdapter

    // ── Student Data ──────────────────────────
    private var studentId  = ""
    private var studentName = ""
    private var rollNo     = ""
    private var courseName = ""
    private var courseId   = ""
    private var currentSem = 1

    // ── Current Sem Data ──────────────────────
    private var currentSemTotal: Long = 0L
    private var currentSemPaid: Long  = 0L
    private var currentSemStatus      = "pending"

    // ── Razorpay ──────────────────────────────
    private val RAZORPAY_KEY = BuildConfig.RAZORPAY_KEY_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fees)

        session    = SessionManager(this)
        studentId  = session.getStudentId() ?: ""
        studentName = session.getFullName() ?: ""
        rollNo     = session.getRollNo() ?: ""
        courseName = session.getCourseName() ?: ""
        courseId   = session.getCourseId() ?: ""
        currentSem  = session.getSemester().toIntOrNull() ?: 1

        // ✅ Preload Razorpay (faster opening)
        Checkout.preload(applicationContext)

        bindViews()
        setupToolbar()
        setupRecyclerView()
        loadFeesData()
    }

    private fun bindViews() {
        topBar             = findViewById(R.id.topBar)
        progressBar        = findViewById(R.id.progressBar)
        rvSemesters        = findViewById(R.id.rvSemesters)
        layoutEmpty        = findViewById(R.id.layoutEmpty)
        tvCurrentSemLabel  = findViewById(R.id.tvCurrentSemLabel)
        tvCurrentSemBadge  = findViewById(R.id.tvCurrentSemBadge)
        tvCurrentPaid      = findViewById(R.id.tvCurrentPaid)
        tvCurrentTotal     = findViewById(R.id.tvCurrentTotal)
        tvCurrentRemaining = findViewById(R.id.tvCurrentRemaining)
        currentSemCard     = findViewById(R.id.cardCurrentSem) // ✅ add id to XML
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = FeesAdapter(
            semesters = mutableListOf(),
            onReceiptClick = { sem ->
                openReceipt(sem)
            }
        )
        rvSemesters.layoutManager = LinearLayoutManager(this)
        rvSemesters.adapter = adapter
    }

    // ─────────────────────────────────────────────
    // ✅ Load: semesterFees from course + student fees doc
    // ─────────────────────────────────────────────
    private fun loadFeesData() {
        showLoading(true)

        // Step 1: get semesterFees map from course
        db.collection("courses").document(courseId).get()
            .addOnSuccessListener { courseDoc ->
                @Suppress("UNCHECKED_CAST")
                val rawFees = courseDoc.get("semesterFees") as? Map<String, Any> ?: emptyMap()
                val semFeesMap = rawFees.mapValues { entry ->
                    when (val v = entry.value) {
                        is Long   -> v
                        is Number -> v.toLong()
                        else      -> 0L
                    }
                }

                // Step 2: get student fees doc
                loadStudentFees(semFeesMap)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadStudentFees(semFeesMap: Map<String, Long>) {
        db.collection("fees").document(studentId).get()
            .addOnSuccessListener { feeDoc ->
                showLoading(false)

                @Suppress("UNCHECKED_CAST")
                val sems = feeDoc.get("semesters")
                        as? List<Map<String, Any>> ?: emptyList()

                // ── Current sem data ─────────────────
                val currentTotal   = semFeesMap[currentSem.toString()] ?: 0L
                val currentSemData = sems.find { map ->
                    (map["semNumber"] as? Long)?.toInt() == currentSem
                }
                val cPaid   = currentSemData?.get("paidAmount") as? Long ?: 0L
                val cStatus = currentSemData?.get("status") as? String ?: "pending"
                val cDate   = currentSemData?.get("paidDate") as? Long ?: 0L

                currentSemTotal  = currentTotal
                currentSemPaid   = cPaid
                currentSemStatus = cStatus

                updateCurrentSemCard(currentTotal, cPaid, cStatus)

                // ── Build history list ───────────────
                // ✅ Rule:
                //   - Always show sems BEFORE current sem (1..currentSem-1)
                //   - ALSO show current sem IF it is paid
                buildHistoryList(semFeesMap, sems, cStatus, cDate, cPaid)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ─────────────────────────────────────────────
// ✅ Build previous/history semesters list
// ─────────────────────────────────────────────
    private fun buildHistoryList(
        semFeesMap: Map<String, Long>,
        sems: List<Map<String, Any>>,
        currentStatus: String,
        currentPaidDate: Long,
        currentPaid: Long
    ) {
        val historyList = mutableListOf<FeesAdapter.FeeSemester>()

        // ✅ Sort ALL sems by number
        val sortedKeys = semFeesMap.keys
            .sortedBy { it.toIntOrNull() ?: 0 }

        for (semKey in sortedKeys) {
            val semNo    = semKey.toIntOrNull() ?: continue
            val totalAmt = semFeesMap[semKey] ?: 0L

            if (semNo < currentSem) {
                // ✅ Always show sems BEFORE current
                val semData = sems.find { map ->
                    (map["semNumber"] as? Long)?.toInt() == semNo
                }
                historyList.add(
                    FeesAdapter.FeeSemester(
                        semNumber   = semNo,
                        totalAmount = totalAmt,
                        paidAmount  = semData?.get("paidAmount") as? Long ?: 0L,
                        status      = semData?.get("status") as? String ?: "pending",
                        receiptUrl  = semData?.get("receiptUrl") as? String,
                        paidDate    = semData?.get("paidDate") as? Long ?: 0L
                    )
                )
            } else if (semNo == currentSem && currentStatus.lowercase() == "paid") {
                // ✅ Show current sem ONLY if paid
                historyList.add(
                    FeesAdapter.FeeSemester(
                        semNumber   = currentSem,
                        totalAmount = totalAmt,
                        paidAmount  = currentPaid,
                        status      = currentStatus,
                        receiptUrl  = null,
                        paidDate    = currentPaidDate
                    )
                )
            }
            // ✅ Sems AFTER current → never show
        }

        // ── Update UI ────────────────────────────
        if (historyList.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rvSemesters.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            rvSemesters.visibility = View.VISIBLE
            adapter.updateList(historyList)
        }
    }


    // ─────────────────────────────────────────────
    // ✅ Update current sem card UI
    // ─────────────────────────────────────────────
    private fun updateCurrentSemCard(
        total: Long,
        paid: Long,
        status: String
    ) {
        val remaining = total - paid

        tvCurrentSemLabel.text  = "Semester $currentSem"
        tvCurrentPaid.text      = "₹${String.format("%,d", paid)}"
        tvCurrentTotal.text     = "₹${String.format("%,d", total)}"
        tvCurrentRemaining.text = "₹${String.format("%,d", remaining)}"

        val (badgeText, badgeDrawable) = when (status.lowercase()) {
            "paid"    -> Pair("PAID",    R.drawable.bg_badge_green)
            "partial" -> Pair("PARTIAL", R.drawable.bg_badge_orange)
            else      -> Pair("PENDING", R.drawable.bg_badge_red)
        }
        tvCurrentSemBadge.text = badgeText
        tvCurrentSemBadge.setBackgroundResource(badgeDrawable)

        // ✅ Card click → pay if not fully paid
        if (status.lowercase() == "paid") {
            // ✅ PAID → disable click, open receipt
            currentSemCard.isClickable = false
            currentSemCard.alpha = 0.85f
            currentSemCard.setOnClickListener {
                openReceipt(
                    FeesAdapter.FeeSemester(
                        semNumber   = currentSem,
                        totalAmount = total,
                        paidAmount  = paid,
                        status      = status,
                        receiptUrl  = null,
                        paidDate    = 0L
                    )
                )
            }
        } else {
            // ✅ PENDING/PARTIAL → click to pay
            currentSemCard.isClickable = true
            currentSemCard.alpha = 1.0f
            currentSemCard.setOnClickListener {
                startRazorpayPayment(remaining)
            }
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Open Razorpay
    // ─────────────────────────────────────────────
    private fun startRazorpayPayment(remainingAmount: Long) {
        if (remainingAmount <= 0) {
            Toast.makeText(this, "No dues remaining!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY)
            checkout.setImage(R.mipmap.ic_launcher)

            val options = JSONObject()
            options.put("name", "College Management")
            options.put("description",
                "Semester $currentSem Fees | $courseName | $studentName")
            options.put("theme.color", "#0D47A1")   // deep blue
            options.put("currency", "INR")
            options.put("amount", remainingAmount * 100) // ✅ paise

            // ✅ Prefill student details
            val prefill = JSONObject()
            prefill.put("contact", session.getPhone() ?: "")
            prefill.put("email",   session.getEmail() ?: "")
            options.put("prefill", prefill)

            checkout.open(this, options)

        } catch (e: Exception) {
            Log.e(TAG, "Razorpay error: ${e.message}")
            Toast.makeText(this, "❌ Payment error: ${e.message}",
                Toast.LENGTH_LONG).show()
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Razorpay: Payment SUCCESS
    // ─────────────────────────────────────────────
    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Log.d(TAG, "✅ Payment success: $razorpayPaymentId")

        val newPaid   = currentSemTotal // ✅ full amount paid
        val newStatus = "paid"
        val paidDate  = System.currentTimeMillis()

        // ✅ Save to Firestore fees/{studentId}
        savePaymentToFirestore(
            paidAmount  = newPaid,
            status      = newStatus,
            paidDate    = paidDate,
            paymentId   = razorpayPaymentId ?: ""
        )
    }

    // ─────────────────────────────────────────────
    // ✅ Razorpay: Payment FAILED
    // ─────────────────────────────────────────────
    override fun onPaymentError(errorCode: Int, errorDescription: String?) {
        Log.e(TAG, "❌ Payment failed: $errorCode | $errorDescription")
        Toast.makeText(
            this,
            "❌ Payment failed: $errorDescription",
            Toast.LENGTH_LONG
        ).show()
    }

    // ─────────────────────────────────────────────
    // ✅ Save successful payment to Firestore
    // ─────────────────────────────────────────────
    private fun savePaymentToFirestore(
        paidAmount: Long,
        status: String,
        paidDate: Long,
        paymentId: String
    ) {
        showLoading(true)

        val newSemEntry = mapOf(
            "semNumber"   to currentSem.toLong(),
            "totalAmount" to currentSemTotal,
            "paidAmount"  to paidAmount,
            "status"      to status,
            "paidDate"    to paidDate,
            "paymentId"   to paymentId,
            "receiptUrl"  to null
        )

        db.collection("fees").document(studentId).get()
            .addOnSuccessListener { doc ->
                val updatedSems: MutableList<Map<String, Any?>>

                if (doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val existingSems = (doc.get("semesters")
                            as? List<Map<String, Any?>>)
                        ?.toMutableList() ?: mutableListOf()

                    val idx = existingSems.indexOfFirst { map ->
                        (map["semNumber"] as? Long)?.toInt() == currentSem
                    }
                    if (idx >= 0) existingSems[idx] = newSemEntry
                    else existingSems.add(newSemEntry)

                    updatedSems = existingSems
                } else {
                    updatedSems = mutableListOf(newSemEntry)
                }

                db.collection("fees").document(studentId)
                    .set(mapOf(
                        "studentId" to studentId,
                        "semesters" to updatedSems
                    ))
                    .addOnSuccessListener {
                        showLoading(false)

                        // ✅ 1. Update current sem card UI
                        currentSemPaid   = paidAmount
                        currentSemStatus = status
                        updateCurrentSemCard(currentSemTotal, paidAmount, status)

                        // ✅ 2. Refresh history list with updated sems
                        @Suppress("UNCHECKED_CAST")
                        val refreshedSems = updatedSems
                            .map { it as Map<String, Any> }

                        // ✅ 3. Reload full semFeesMap from course → rebuild list
                        db.collection("courses").document(courseId).get()
                            .addOnSuccessListener { courseDoc ->
                                @Suppress("UNCHECKED_CAST")
                                val rawFees = courseDoc.get("semesterFees")
                                        as? Map<String, Any> ?: emptyMap()

                                val semFeesMap = rawFees.mapValues { entry ->
                                    when (val v = entry.value) {
                                        is Long   -> v
                                        is Number -> v.toLong()
                                        else      -> 0L
                                    }
                                }

                                // ✅ Rebuild history list
                                // (current sem now shows in list since status = paid)
                                buildHistoryList(
                                    semFeesMap      = semFeesMap,
                                    sems            = refreshedSems,
                                    currentStatus   = status,
                                    currentPaidDate = paidDate,
                                    currentPaid     = paidAmount
                                )
                            }

                        // ✅ 4. Toast
                        Toast.makeText(
                            this,
                            "✅ Payment Successful! Fees paid for Semester $currentSem",
                            Toast.LENGTH_LONG
                        ).show()

                        // ✅ 5. Open receipt automatically
                        openReceipt(
                            FeesAdapter.FeeSemester(
                                semNumber   = currentSem,
                                totalAmount = currentSemTotal,
                                paidAmount  = paidAmount,
                                status      = status,
                                receiptUrl  = null,
                                paidDate    = paidDate
                            )
                        )
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    // ─────────────────────────────────────────────
    // ✅ Open receipt screen
    // ─────────────────────────────────────────────
    private fun openReceipt(sem: FeesAdapter.FeeSemester) {
        startActivity(
            Intent(this, FeesReceiptActivity::class.java).apply {
                putExtra("semNumber",   sem.semNumber)
                putExtra("totalAmount", sem.totalAmount)
                putExtra("paidAmount",  sem.paidAmount)
                putExtra("status",      sem.status)
                putExtra("paidDate",    sem.paidDate)
                putExtra("studentName", studentName)
                putExtra("rollNo",      rollNo)
                putExtra("courseName",  courseName)
            }
        )
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}
