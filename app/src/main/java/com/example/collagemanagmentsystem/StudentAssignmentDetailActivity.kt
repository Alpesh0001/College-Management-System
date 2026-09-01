package com.example.collagemanagmentsystem

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.collagemanagmentsystem.models.AssignmentModel
import com.example.collagemanagmentsystem.utils.CoreBaseActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentAssignmentDetailActivity : CoreBaseActivity() {

    // ── Views ─────────────────────────────────
    private lateinit var btnBack: ImageView
    private lateinit var tvToolbarTitle: TextView

    // Header card

    private lateinit var btnOpenFile: MaterialButton
    private var progressDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private var tvProgressStatus: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    private lateinit var tvDueBadge: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubject: TextView

    // Info card
    private lateinit var tvYear: TextView
    private lateinit var tvSemester: TextView
    private lateinit var tvDueDate: TextView
    private lateinit var tvPostedBy: TextView

    // Description card
    private lateinit var tvDescription: TextView
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // Attachment card
    private lateinit var cardAttachment: MaterialCardView
    private lateinit var tvFileName: TextView
    private lateinit var tvFileType: TextView
    private lateinit var ivFileIcon: ImageView

    private var downloadReceiver: BroadcastReceiver? = null
    private lateinit var btnDownloadFile: MaterialButton

    // ── Firebase ──────────────────────────────
    private val db = FirebaseFirestore.getInstance()
    private var assignmentId = ""
    private var fileUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_assignment_detail)

        assignmentId = intent.getStringExtra("ASSIGNMENT_ID") ?: ""

        bindViews()
        setupClickListeners()

        if (assignmentId.isNotEmpty()) {
            loadAssignment()
        } else {
            Toast.makeText(this, "Assignment not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun bindViews() {
        btnBack         = findViewById(R.id.btnBack)
        tvToolbarTitle  = findViewById(R.id.tvToolbarTitle)
        tvDueBadge      = findViewById(R.id.tvDueBadge)
        tvTitle         = findViewById(R.id.tvTitle)
        tvSubject       = findViewById(R.id.tvSubject)
        tvYear          = findViewById(R.id.tvYear)
        tvSemester      = findViewById(R.id.tvSemester)
        tvDueDate       = findViewById(R.id.tvDueDate)
        tvPostedBy      = findViewById(R.id.tvPostedBy)
        tvDescription   = findViewById(R.id.tvDescription)
        cardAttachment  = findViewById(R.id.cardAttachment)
        tvFileName      = findViewById(R.id.tvFileName)
        tvFileType      = findViewById(R.id.tvFileType)
        btnOpenFile     = findViewById(R.id.btnOpenFile)
        ivFileIcon      = findViewById(R.id.ivFileIcon)
        btnDownloadFile = findViewById(R.id.btnDownloadFile)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        // ✅ Open file in browser
        btnOpenFile.setOnClickListener {
            if (fileUrl.isNotEmpty()) {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                )
            } else {
                Toast.makeText(this, "File not available", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Download file
        btnDownloadFile.setOnClickListener {
            if (fileUrl.isNotEmpty()) {
                downloadFile(fileUrl)
            } else {
                Toast.makeText(this, "File not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Load assignment from Firestore
    // ─────────────────────────────────────────────
    private fun loadAssignment() {
        db.collection("assignments")
            .document(assignmentId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Assignment not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val a = doc.toObject(AssignmentModel::class.java) ?: return@addOnSuccessListener
                a.id = doc.id

                populateUI(a)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    // ─────────────────────────────────────────────
    // ✅ Populate UI
    // ─────────────────────────────────────────────
    private fun populateUI(a: AssignmentModel) {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // ── Title / Subject ───────────────────
        tvTitle.text   = a.title
        tvSubject.text = a.subject

        // ── Info ──────────────────────────────
        tvYear.text     = "Year ${a.year}"
        tvSemester.text = "Semester ${a.semester}"

        // ── Due Date ──────────────────────────
        if (a.dueDate > 0L) {
            val dueDateStr = sdf.format(Date(a.dueDate))
            val isOverdue  = now > a.dueDate

            tvDueBadge.text = if (isOverdue)
                "⏰ Overdue: $dueDateStr"
            else
                "Due: $dueDateStr"

            tvDueBadge.setBackgroundResource(
                if (isOverdue) R.drawable.bg_badge_red
                else           R.drawable.bg_badge_green
            )

            tvDueDate.text = dueDateStr
            tvDueDate.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isOverdue) R.color.red else R.color.card_title
                )
            )
        } else {
            tvDueBadge.text = "No due date"
            tvDueBadge.setBackgroundResource(R.drawable.bg_badge_orange)
            tvDueDate.text  = "Not set"
        }

        // ── Description ───────────────────────
        tvDescription.text = a.description.ifEmpty { "No description provided." }

        // ── Posted By (load faculty name) ─────
        if (a.createdBy.isNotEmpty()) {
            loadFacultyName(a.createdBy)
        } else {
            tvPostedBy.text = "Faculty"
        }

        // ── Attachment ────────────────────────
        if (a.fileUrl.isNotEmpty()) {
            fileUrl = a.fileUrl
            showAttachment(a.fileName, a.fileUrl)
        } else {
            cardAttachment.visibility = View.GONE
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Show attachment card
    // ─────────────────────────────────────────────
    private fun showAttachment(fileName: String, url: String) {
        cardAttachment.visibility = View.VISIBLE

        // ✅ Detect file type from URL or name
        val name = fileName.ifEmpty {
            url.substringAfterLast("/")
                .substringBefore("?")
                .ifEmpty { "attachment" }
        }

        tvFileName.text = name

        // ✅ Set icon + type label based on extension
        val ext = name.substringAfterLast(".").lowercase()
        when (ext) {
            "pdf" -> {
                tvFileType.text = "PDF Document"
                ivFileIcon.setImageResource(R.drawable.ic_assignment)
                ivFileIcon.setColorFilter(
                    ContextCompat.getColor(this, R.color.red)
                )
            }
            "doc", "docx" -> {
                tvFileType.text = "Word Document"
                ivFileIcon.setImageResource(R.drawable.ic_assignment)
                ivFileIcon.setColorFilter(
                    ContextCompat.getColor(this, R.color.deep_blue)
                )
            }
            "jpg", "jpeg", "png" -> {
                tvFileType.text = "Image File"
                ivFileIcon.setImageResource(R.drawable.ic_assignment)
                ivFileIcon.setColorFilter(
                    ContextCompat.getColor(this, R.color.green)
                )
            }
            else -> {
                tvFileType.text = "Attached File"
                ivFileIcon.setImageResource(R.drawable.ic_assignment)
                ivFileIcon.setColorFilter(
                    ContextCompat.getColor(this, R.color.orange)
                )
            }
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Load faculty name from Firestore
    // ─────────────────────────────────────────────
    private fun loadFacultyName(facultyId: String) {
        db.collection("faculty")
            .document(facultyId)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("fullName")
                    ?: doc.getString("name")
                    ?: "Faculty"
                tvPostedBy.text = name
            }
            .addOnFailureListener {
                tvPostedBy.text = "Faculty"
            }
    }

    // ─────────────────────────────────────────────
    // ✅ Download file using DownloadManager
    // ─────────────────────────────────────────────
    private fun downloadFile(url: String) {
        // ✅ Clean filename from URL
        val fileName = Uri.parse(url).lastPathSegment
            ?.substringAfterLast("/")
            ?.replace("%20", "_")
            ?.ifEmpty { "assignment_file.pdf" }
            ?: "assignment_file.pdf"

        // ✅ Show progress dialog
        showProgressDialog(fileName)

        downloadScope.launch {
            try {
                val client  = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        progressDialog?.dismiss()
                        Toast.makeText(
                            this@StudentAssignmentDetailActivity,
                            "❌ Download failed: ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val body        = response.body ?: run {
                    withContext(Dispatchers.Main) {
                        progressDialog?.dismiss()
                        Toast.makeText(
                            this@StudentAssignmentDetailActivity,
                            "❌ Empty response from server",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val totalBytes  = body.contentLength() // -1 if unknown
                var downloaded  = 0L

                // ✅ Save to Downloads/CollegeApp/
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS),
                    "CollegeApp"
                )
                if (!dir.exists()) dir.mkdirs()

                val outFile = File(dir, fileName)

                body.byteStream().use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytes: Int

                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            downloaded += bytes

                            // ✅ Update progress on main thread
                            val percent = if (totalBytes > 0)
                                ((downloaded * 100) / totalBytes).toInt()
                            else -1  // indeterminate

                            withContext(Dispatchers.Main) {
                                if (percent >= 0) {
                                    progressBar?.isIndeterminate = false
                                    progressBar?.progress        = percent
                                    tvProgressStatus?.text       =
                                        "Downloading... $percent% (${formatSize(downloaded)} / ${formatSize(totalBytes)})"
                                } else {
                                    progressBar?.isIndeterminate = true
                                    tvProgressStatus?.text       =
                                        "Downloading... ${formatSize(downloaded)}"
                                }
                            }
                        }
                    }
                }

                // ✅ Done!
                withContext(Dispatchers.Main) {
                    progressBar?.isIndeterminate = false
                    progressBar?.progress        = 100
                    tvProgressStatus?.text       = "✅ Download complete!"

                    handler.postDelayed({
                        progressDialog?.dismiss()
                        Toast.makeText(
                            this@StudentAssignmentDetailActivity,
                            "✅ Saved to Downloads/CollegeApp/$fileName",
                            Toast.LENGTH_LONG
                        ).show()
                    }, 600)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    Toast.makeText(
                        this@StudentAssignmentDetailActivity,
                        "❌ Download error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ─────────────────────────────────────────────
// ✅ Show custom progress dialog
// ─────────────────────────────────────────────
    private fun showProgressDialog(fileName: String) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_download_progress, null)

        progressBar        = dialogView.findViewById(R.id.progressBarDownload)
        tvProgressStatus   = dialogView.findViewById(R.id.tvProgressStatus)
        val tvFileName     = dialogView.findViewById<TextView>(R.id.tvDownloadFileName)

        tvFileName.text        = fileName
        tvProgressStatus?.text = "Starting download..."
        progressBar?.progress  = 0

        progressDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        progressDialog?.show()
    }


    // ─────────────────────────────────────────────
// ✅ BroadcastReceiver for download complete
// ─────────────────────────────────────────────
// ── Class level variable to track receiver ────


    // ─────────────────────────────────────────────
// ✅ Safe unregister — avoids crash if already
//    unregistered
// ─────────────────────────────────────────────

    // ─────────────────────────────────────────────
// ✅ Also update onDestroy to clean up receiver
    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()
        downloadScope.cancel()  // ✅ Cancel coroutine on exit
    }



    // ─────────────────────────────────────────────
// ✅ Format bytes → KB / MB
// ─────────────────────────────────────────────
    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024     -> String.format("%.0f KB", bytes / 1_024.0)
            else               -> "$bytes B"
        }
    }

}
