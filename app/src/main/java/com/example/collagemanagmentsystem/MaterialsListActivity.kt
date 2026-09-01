package com.example.collagemanagmentsystem

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collagemanagmentsystem.R
import com.example.collagemanagmentsystem.adapters.SubjectAdapter
import com.example.collagemanagmentsystem.models.MaterialModel
import com.example.collagemanagmentsystem.utils.CoreBaseActivity
import com.example.collagemanagmentsystem.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class MaterialsListActivity : CoreBaseActivity() {

    // ── Views ─────────────────────────────────
    private lateinit var btnBack: ImageView
    private lateinit var tvSemYear: TextView
    private lateinit var etSearch: EditText
    private lateinit var tvSubjectCount: TextView
    private lateinit var rvSubjects: RecyclerView
    private lateinit var layoutEmpty: LinearLayout

    // ── Data ──────────────────────────────────
    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private lateinit var adapter: SubjectAdapter

    // all subjects with their material counts
    private val allSubjects = mutableListOf<Pair<String, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_materials_list)

        session = SessionManager(this)
        bindViews()
        setupRecyclerView()
        setupSearch()
        setupHeader()
        loadSubjectsWithMaterials()
    }

    private fun bindViews() {
        btnBack         = findViewById(R.id.btnBack)
        tvSemYear       = findViewById(R.id.tvSemYear)
        etSearch        = findViewById(R.id.etSearch)
        tvSubjectCount  = findViewById(R.id.tvSubjectCount)
        rvSubjects      = findViewById(R.id.rvSubjects)
        layoutEmpty     = findViewById(R.id.layoutEmpty)
        btnBack.setOnClickListener { finish() }
    }

    private fun setupHeader() {
        val sem  = session.getSemester()
        val year = session.getYear()
        tvSemYear.text = "Semester $sem • Year $year"
    }

    private fun setupRecyclerView() {
        adapter = SubjectAdapter(
            subjects = mutableListOf(),
            onClick  = { subjectName -> openSubjectMaterials(subjectName) }
        )
        rvSubjects.layoutManager = LinearLayoutManager(this)
        rvSubjects.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { filterSubjects() }
        })
    }

    // ─────────────────────────────────────────────
    // ✅ Load all materials for current sem
    //    Group by subject → count per subject
    // ─────────────────────────────────────────────
    private fun loadSubjectsWithMaterials() {
        val courseId = session.getCourseId()
        val sem      = session.getSemester()

        tvSubjectCount.text = "Loading..."

        val query = db.collection("study_materials")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("semester", sem)

        // ✅ Cache first → then server refresh
        query.get(Source.CACHE)
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) processSnapshot(snap.documents
                    .mapNotNull { it.toObject(MaterialModel::class.java) })

                query.get(Source.SERVER)
                    .addOnSuccessListener { serverSnap ->
                        processSnapshot(serverSnap.documents
                            .mapNotNull { it.toObject(MaterialModel::class.java) })
                    }
                    .addOnFailureListener {
                        if (allSubjects.isEmpty()) showEmpty()
                    }
            }
            .addOnFailureListener {
                query.get(Source.SERVER)
                    .addOnSuccessListener { snap ->
                        processSnapshot(snap.documents
                            .mapNotNull { it.toObject(MaterialModel::class.java) })
                    }
                    .addOnFailureListener { showEmpty() }
            }
    }

    private fun processSnapshot(materials: List<MaterialModel>) {
        // ✅ Group by subject → count
        val grouped = materials
            .groupBy { it.subject }
            .map { (subject, items) -> Pair(subject, items.size) }
            .sortedBy { it.first }

        allSubjects.clear()
        allSubjects.addAll(grouped)
        filterSubjects()
    }

    private fun filterSubjects() {
        val query    = etSearch.text.toString().trim()
        val filtered = if (query.isEmpty()) allSubjects.toList()
        else allSubjects.filter {
            it.first.contains(query, ignoreCase = true)
        }

        adapter.updateList(filtered)

        tvSubjectCount.text =
            "${filtered.size} subject${if (filtered.size == 1) "" else "s"}"

        if (filtered.isEmpty()) showEmpty() else showList()
    }

    private fun openSubjectMaterials(subject: String) {
        startActivity(
            Intent(this, SubjectMaterialsActivity::class.java).apply {
                putExtra("SUBJECT_NAME", subject)
                putExtra("SEMESTER", session.getSemester())
                putExtra("COURSE_ID", session.getCourseId())
            }
        )
    }

    private fun showEmpty() {
        rvSubjects.visibility   = View.GONE
        layoutEmpty.visibility  = View.VISIBLE
        tvSubjectCount.text     = "0 subjects"
    }

    private fun showList() {
        rvSubjects.visibility   = View.VISIBLE
        layoutEmpty.visibility  = View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadSubjectsWithMaterials()
    }
}
