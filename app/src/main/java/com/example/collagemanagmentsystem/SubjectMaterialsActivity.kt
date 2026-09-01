package com.example.collagemanagmentsystem

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collagemanagmentsystem.R
import com.example.collagemanagmentsystem.adapters.MaterialsAdapter
import com.example.collagemanagmentsystem.models.MaterialModel
import com.example.collagemanagmentsystem.utils.CoreBaseActivity
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class SubjectMaterialsActivity : CoreBaseActivity() {

    // ── Views ─────────────────────────────────
    private lateinit var btnBack: ImageView
    private lateinit var tvToolbarTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var tvMaterialCount: TextView
    private lateinit var rvMaterials: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var chipAll: Chip
    private lateinit var chipPdf: Chip
    private lateinit var chipDoc: Chip
    private lateinit var chipImage: Chip

    // ── Data ──────────────────────────────────
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: MaterialsAdapter
    private val allMaterials = mutableListOf<MaterialModel>()
    private var activeFilter = "all"

    // ── Download ──────────────────────────────
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler       = Handler(Looper.getMainLooper())
    private var progressDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private var tvProgressStatus: TextView? = null

    // ── Extras ────────────────────────────────
    private var subjectName = ""
    private var semester    = ""
    private var courseId    = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject_materials)

        subjectName = intent.getStringExtra("SUBJECT_NAME") ?: ""
        semester    = intent.getStringExtra("SEMESTER") ?: ""
        courseId    = intent.getStringExtra("COURSE_ID") ?: ""

        bindViews()
        setupRecyclerView()
        setupSearch()
        setupChips()
        loadMaterials()
    }

    private fun bindViews() {
        btnBack          = findViewById(R.id.btnBack)
        tvToolbarTitle   = findViewById(R.id.tvToolbarTitle)
        etSearch         = findViewById(R.id.etSearch)
        tvMaterialCount  = findViewById(R.id.tvMaterialCount)
        rvMaterials      = findViewById(R.id.rvMaterials)
        layoutEmpty      = findViewById(R.id.layoutEmpty)
        chipAll          = findViewById(R.id.chipAll)
        chipPdf          = findViewById(R.id.chipPdf)
        chipDoc          = findViewById(R.id.chipDoc)
        chipImage        = findViewById(R.id.chipImage)

        tvToolbarTitle.text = subjectName
        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = MaterialsAdapter(
            materials       = mutableListOf(),
            onCardClick     = { material -> openFileInBrowser(material) }, // ✅ NEW
            onDownloadClick = { material -> downloadFile(material) }
        )
        rvMaterials.layoutManager = LinearLayoutManager(this)
        rvMaterials.adapter = adapter
    }

    // ✅ Card Click → Open PDF/file URL directly in browser or PDF viewer
    private fun openFileInBrowser(material: MaterialModel) {
        if (material.fileUrl.isEmpty()) {
            Toast.makeText(this, "No file attached", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(material.fileUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilterAndSearch() }
        })
    }

    private fun setupChips() {
        chipAll.setOnClickListener   { activeFilter = "all";   applyFilterAndSearch() }
        chipPdf.setOnClickListener   { activeFilter = "pdf";   applyFilterAndSearch() }
        chipDoc.setOnClickListener   { activeFilter = "doc";   applyFilterAndSearch() }
        chipImage.setOnClickListener { activeFilter = "image"; applyFilterAndSearch() }
    }

    // ─────────────────────────────────────────────
    // ✅ Load materials for this subject
    // ─────────────────────────────────────────────
    private fun loadMaterials() {
        tvMaterialCount.text = "Loading..."

        val query = db.collection("study_materials")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("semester", semester)
            .whereEqualTo("subject", subjectName)

        query.get(Source.CACHE)
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    processSnapshot(snap.documents.mapNotNull { doc ->
                        doc.toObject(MaterialModel::class.java)?.also { it.id = doc.id }
                    })
                }
                query.get(Source.SERVER)
                    .addOnSuccessListener { serverSnap ->
                        processSnapshot(serverSnap.documents.mapNotNull { doc ->
                            doc.toObject(MaterialModel::class.java)?.also { it.id = doc.id }
                        })
                    }
                    .addOnFailureListener {
                        if (allMaterials.isEmpty()) showEmpty()
                    }
            }
            .addOnFailureListener {
                query.get(Source.SERVER)
                    .addOnSuccessListener { snap ->
                        processSnapshot(snap.documents.mapNotNull { doc ->
                            doc.toObject(MaterialModel::class.java)?.also { it.id = doc.id }
                        })
                    }
                    .addOnFailureListener { showEmpty() }
            }
    }

    private fun processSnapshot(list: List<MaterialModel>) {
        allMaterials.clear()
        allMaterials.addAll(list.sortedByDescending { it.uploadedAt })
        applyFilterAndSearch()
    }

    // ─────────────────────────────────────────────
    // ✅ Filter + Search
    // ─────────────────────────────────────────────
    private fun applyFilterAndSearch() {
        val query = etSearch.text.toString().trim()

        var filtered = when (activeFilter) {
            "pdf"   -> allMaterials.filter {
                it.fileName.endsWith(".pdf", true) || it.fileType == "pdf"
            }
            "doc"   -> allMaterials.filter {
                it.fileName.endsWith(".doc", true) ||
                        it.fileName.endsWith(".docx", true) || it.fileType == "doc"
            }
            "image" -> allMaterials.filter {
                it.fileName.endsWith(".jpg", true) ||
                        it.fileName.endsWith(".jpeg", true) ||
                        it.fileName.endsWith(".png", true)
            }
            else -> allMaterials.toList()
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(query, true) ||
                        it.description.contains(query, true)
            }
        }

        adapter.updateList(filtered)
        val count = filtered.size
        tvMaterialCount.text = "$count material${if (count == 1) "" else "s"}"

        if (count == 0) showEmpty() else showList()
    }

    // ─────────────────────────────────────────────
    // ✅ Download — same OkHttp approach as Assignment
    // ─────────────────────────────────────────────
    private fun downloadFile(material: MaterialModel) {
        if (material.fileUrl.isEmpty()) {
            Toast.makeText(this, "No file attached", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = material.fileName.ifEmpty {
            material.fileUrl
                .substringAfterLast("/")
                .substringBefore("?")
                .replace("%20", "_")
                .ifEmpty { "material_file.pdf" }
        }

        showProgressDialog(fileName)

        downloadScope.launch {
            try {
                val client   = OkHttpClient()
                val request  = Request.Builder()
                    .url(material.fileUrl)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        progressDialog?.dismiss()
                        Toast.makeText(
                            this@SubjectMaterialsActivity,
                            "❌ Download failed: ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val body       = response.body ?: return@launch
                val totalBytes = body.contentLength()
                var downloaded = 0L

                val dir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS),
                    "CollegeApp/Materials"
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
                            val percent = if (totalBytes > 0)
                                ((downloaded * 100) / totalBytes).toInt() else -1

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

                withContext(Dispatchers.Main) {
                    progressBar?.isIndeterminate = false
                    progressBar?.progress        = 100
                    tvProgressStatus?.text       = "✅ Download complete!"
                    handler.postDelayed({
                        progressDialog?.dismiss()
                        Toast.makeText(
                            this@SubjectMaterialsActivity,
                            "✅ Saved to Downloads/CollegeApp/Materials/$fileName",
                            Toast.LENGTH_LONG
                        ).show()
                    }, 600)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    Toast.makeText(
                        this@SubjectMaterialsActivity,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showProgressDialog(fileName: String) {
        val dialogView     = LayoutInflater.from(this)
            .inflate(R.layout.dialog_download_progress, null)
        progressBar        = dialogView.findViewById(R.id.progressBarDownload)
        tvProgressStatus   = dialogView.findViewById(R.id.tvProgressStatus)
        val tvFileName     = dialogView.findViewById<TextView>(R.id.tvDownloadFileName)
        tvFileName.text    = fileName
        tvProgressStatus?.text = "Starting download..."
        progressBar?.progress  = 0
        progressDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024     -> String.format("%.0f KB", bytes / 1_024.0)
            else               -> "$bytes B"
        }
    }

    private fun showEmpty() {
        rvMaterials.visibility  = View.GONE
        layoutEmpty.visibility  = View.VISIBLE
    }

    private fun showList() {
        rvMaterials.visibility  = View.VISIBLE
        layoutEmpty.visibility  = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()
        downloadScope.cancel()
    }

    override fun onResume() {
        super.onResume()
        loadMaterials()
    }
}
