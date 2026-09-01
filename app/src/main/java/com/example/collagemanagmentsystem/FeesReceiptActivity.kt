package com.example.collagemanagmentsystem

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.collagemanagmentsystem.R
import com.example.collagemanagmentsystem.utils.CoreBaseActivity
import com.example.collagemanagmentsystem.utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FeesReceiptActivity : CoreBaseActivity() {

    private val TAG = "FeesReceiptActivity"

    // Views
    private lateinit var topBar: MaterialToolbar
    private lateinit var tvCollegeName: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvReceiptNo: TextView
    private lateinit var tvPaymentDate: TextView
    private lateinit var tvStudentName: TextView
    private lateinit var tvRollNo: TextView
    private lateinit var tvCourse: TextView
    private lateinit var tvSemester: TextView
    private lateinit var tvTotalFees: TextView
    private lateinit var tvPaidAmount: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var tvFinalAmount: TextView
    private lateinit var btnDownloadPdf: Button
    private lateinit var progressBar: ProgressBar

    // Data from Intent
    private var studentName  = ""
    private var rollNo       = ""
    private var courseName   = ""
    private var semNumber    = 0
    private var totalAmount  = 0L
    private var paidAmount   = 0L
    private var status       = ""
    private var paidDate     = 0L
    private var collegeNameStr = "College Management System"

    private lateinit var session: SessionManager

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) downloadPDF()
        else Toast.makeText(this,
            "Storage permission needed to download", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fees_receipt)

        session = SessionManager(this)
        readIntent()
        bindViews()
        setupToolbar()
        populateReceipt()
        setupDownload()
    }

    private fun readIntent() {
        studentName = intent.getStringExtra("studentName") ?: session.getFullName() ?: ""
        rollNo      = intent.getStringExtra("rollNo")      ?: session.getRollNo()      ?: ""
        courseName  = intent.getStringExtra("courseName")  ?: session.getCourseName()  ?: ""
        semNumber   = intent.getIntExtra("semNumber", 1)
        totalAmount = intent.getLongExtra("totalAmount", 0L)
        paidAmount  = intent.getLongExtra("paidAmount", 0L)
        status      = intent.getStringExtra("status")      ?: "pending"
        paidDate    = intent.getLongExtra("paidDate", 0L)
    }

    private fun bindViews() {
        topBar          = findViewById(R.id.topBar)
        tvCollegeName   = findViewById(R.id.tvCollegeName)
        tvStatusBadge   = findViewById(R.id.tvStatusBadge)
        tvReceiptNo     = findViewById(R.id.tvReceiptNo)
        tvPaymentDate   = findViewById(R.id.tvPaymentDate)
        tvStudentName   = findViewById(R.id.tvStudentName)
        tvRollNo        = findViewById(R.id.tvRollNo)
        tvCourse        = findViewById(R.id.tvCourse)
        tvSemester      = findViewById(R.id.tvSemester)
        tvTotalFees     = findViewById(R.id.tvTotalFees)
        tvPaidAmount    = findViewById(R.id.tvPaidAmount)
        tvRemaining     = findViewById(R.id.tvRemaining)
        tvFinalAmount   = findViewById(R.id.tvFinalAmount)
        btnDownloadPdf  = findViewById(R.id.btnDownloadPdf)
        progressBar     = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        topBar.setNavigationOnClickListener { finish() }
    }

    private fun populateReceipt() {
        val remaining = totalAmount - paidAmount

        // Receipt number: REC-YEAR-STUDENTID-SEM
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val studentId = session.getStudentId() ?: "000"
        tvReceiptNo.text = "REC-$year-${studentId.takeLast(4)}-SEM$semNumber"

        // Payment date
        tvPaymentDate.text = if (paidDate > 0L)
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(paidDate))
        else "N/A"

        // Student details
        tvCollegeName.text  = collegeNameStr
        tvStudentName.text  = studentName
        tvRollNo.text       = rollNo
        tvCourse.text       = courseName
        tvSemester.text     = "Semester $semNumber"

        // Fee details
        tvTotalFees.text    = "₹${String.format("%,d", totalAmount)}"
        tvPaidAmount.text   = "₹${String.format("%,d", paidAmount)}"
        tvRemaining.text    = "₹${String.format("%,d", remaining)}"
        tvFinalAmount.text  = "₹${String.format("%,d", paidAmount)}"

        // Status badge
        val (badgeText, badgeDrawable) = when (status.lowercase()) {
            "paid"    -> Pair("PAID",    R.drawable.bg_badge_green)
            "partial" -> Pair("PARTIAL", R.drawable.bg_badge_orange)
            else      -> Pair("PENDING", R.drawable.bg_badge_red)
        }
        tvStatusBadge.text = badgeText
        tvStatusBadge.setBackgroundResource(badgeDrawable)

        Log.d(TAG, "Receipt populated: sem=$semNumber | paid=$paidAmount | total=$totalAmount")
    }

    // ─────────────────────────────────────────────
    // Download PDF
    // ─────────────────────────────────────────────
    private fun setupDownload() {
        btnDownloadPdf.setOnClickListener {
            checkPermissionAndDownload()
        }
    }

    private fun checkPermissionAndDownload() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ no permission needed
                downloadPDF()
            }
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                downloadPDF()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun downloadPDF() {
        btnDownloadPdf.isEnabled = false
        btnDownloadPdf.text      = "Generating PDF..."
        progressBar.visibility   = View.VISIBLE

        try {
            val pdfFile = generateReceiptPDF()

            if (pdfFile != null && pdfFile.exists()) {
                progressBar.visibility   = View.GONE
                btnDownloadPdf.isEnabled = true
                btnDownloadPdf.text      = "📥 Download Receipt PDF"

                Toast.makeText(
                    this,
                    "✅ Saved to Downloads/$collegeNameStr/",
                    Toast.LENGTH_LONG
                ).show()

                // ✅ Open PDF
                openPDF(pdfFile)
            } else {
                throw Exception("PDF file not created")
            }

        } catch (e: Exception) {
            progressBar.visibility   = View.GONE
            btnDownloadPdf.isEnabled = true
            btnDownloadPdf.text      = "📥 Download Receipt PDF"
            Log.e(TAG, "PDF error: ${e.message}")
            Toast.makeText(this, "❌ Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Generate PDF
    // ─────────────────────────────────────────────
    private fun generateReceiptPDF(): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawReceiptOnCanvas(canvas)

            pdfDocument.finishPage(page)

            // ✅ Save to Downloads/CollegeName/BCA_SEM1_Receipt.pdf
            val downloadsDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            val collegeFolder = File(downloadsDir, collegeNameStr)
            if (!collegeFolder.exists()) collegeFolder.mkdirs()

            val fileName = "${courseName.uppercase()}_SEM${semNumber}_Receipt.pdf"
            val file = File(collegeFolder, fileName)

            val out = FileOutputStream(file)
            pdfDocument.writeTo(out)
            pdfDocument.close()
            out.close()

            Log.d(TAG, "PDF saved: ${file.absolutePath}")
            file

        } catch (e: Exception) {
            Log.e(TAG, "generateReceiptPDF error: ${e.message}")
            null
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Draw receipt content on PDF canvas
    // ─────────────────────────────────────────────
    private fun drawReceiptOnCanvas(canvas: Canvas) {
        val centerX = 297.5f
        val leftM   = 60f
        val rightM  = 535f
        var y       = 0f

        // ── Header Background ────────────────────
        val headerPaint = Paint().apply {
            color = Color.rgb(13, 71, 161) // deep blue
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 595f, 160f, headerPaint)

        // College Name
        val collegePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(collegeNameStr, centerX, 70f, collegePaint)

        // FEES RECEIPT
        val subPaint = Paint().apply {
            color = Color.parseColor("#B3E5FC")
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FEES RECEIPT", centerX, 100f, subPaint)

        // Status badge
        val badgeColor = when (status.lowercase()) {
            "paid"    -> Color.rgb(76, 175, 80)
            "partial" -> Color.rgb(255, 152, 0)
            else      -> Color.rgb(244, 67, 54)
        }
        val badgePaint = Paint().apply {
            color = badgeColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            centerX - 50f, 112f, centerX + 50f, 140f,
            12f, 12f, badgePaint
        )
        val badgeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(status.uppercase(), centerX, 131f, badgeTextPaint)

        y = 180f

        // ── Receipt No + Date ────────────────────
        val labelPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 13f
        }
        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        val year       = Calendar.getInstance().get(Calendar.YEAR)
        val studentId  = session.getStudentId() ?: "000"
        val receiptNo  = "REC-$year-${studentId.takeLast(4)}-SEM$semNumber"
        val dateStr    = if (paidDate > 0L)
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(paidDate))
        else "N/A"

        canvas.drawText("Receipt No: $receiptNo", leftM, y, labelPaint)
        canvas.drawText("Date: $dateStr", rightM, y, valuePaint)
        y += 20f

        // Divider
        val divPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        canvas.drawLine(leftM, y, rightM, y, divPaint)
        y += 20f

        // ── Section: Student Details ─────────────
        fun drawSection(title: String) {
            val sPaint = Paint().apply {
                color = Color.rgb(13, 71, 161)
                textSize = 15f
                isFakeBoldText = true
            }
            canvas.drawText(title, leftM, y, sPaint)
            y += 8f
            canvas.drawLine(leftM, y, rightM, y, divPaint)
            y += 20f
        }

        fun drawRow(label: String, value: String, valueColor: Int = Color.BLACK) {
            canvas.drawText(label, leftM, y, labelPaint)
            val vp = Paint().apply {
                color = valueColor
                textSize = 13f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(value, rightM, y, vp)
            y += 30f
        }

        drawSection("Student Details")
        drawRow("Student Name", studentName)
        drawRow("Roll Number", rollNo)
        drawRow("Course", courseName)
        y += 10f

        drawSection("Fee Details")
        drawRow("Semester", "Semester $semNumber")
        drawRow("Total Fees", "₹${String.format("%,d", totalAmount)}")
        drawRow("Paid Amount", "₹${String.format("%,d", paidAmount)}",
            Color.rgb(76, 175, 80))
        drawRow("Remaining", "₹${String.format("%,d", totalAmount - paidAmount)}",
            Color.rgb(244, 67, 54))
        y += 10f

        // ── Total Box ────────────────────────────
        val totalBoxPaint = Paint().apply {
            color = Color.rgb(76, 175, 80)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(leftM, y, rightM, y + 50f, 8f, 8f, totalBoxPaint)

        val totalLPaint = Paint().apply {
            color = Color.WHITE; textSize = 15f; isFakeBoldText = true
        }
        val totalVPaint = Paint().apply {
            color = Color.WHITE; textSize = 18f; isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Amount Paid", leftM + 15f, y + 32f, totalLPaint)
        canvas.drawText("₹${String.format("%,d", paidAmount)}", rightM - 15f, y + 32f, totalVPaint)
        y += 70f

        // ── Footer ───────────────────────────────
        val footerPaint = Paint().apply {
            color = Color.GRAY; textSize = 11f; textAlign = Paint.Align.CENTER
        }
        canvas.drawText("This is a computer generated receipt. No signature required.",
            centerX, y, footerPaint)
    }

    // ─────────────────────────────────────────────
    // Open PDF
    // ─────────────────────────────────────────────
    private fun openPDF(file: File) {
        try {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.provider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "openPDF error: ${e.message}")
        }
    }
}
