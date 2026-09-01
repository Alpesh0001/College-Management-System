package com.example.collegemanagementsystemfaculty

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.adapters.MaterialAdapter
import com.example.collegemanagementsystemfaculty.models.MaterialModel
import com.example.collegemanagementsystemfaculty.utils.CoreBaseActivity
import com.example.collegemanagementsystemfaculty.utils.SessionManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ManageMaterialsActivity : CoreBaseActivity() {

    // ✅ Firestore & Session
    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    // ─── Views ───────────────────────────────
    private lateinit var btnBack          : View
    private lateinit var tvToolbarTitle   : TextView
    private lateinit var etSearch         : EditText
    private lateinit var tvMaterialCount  : TextView
    private lateinit var rvMaterials      : RecyclerView
    private lateinit var layoutEmpty      : LinearLayout
    private lateinit var fabAddMaterial   : ExtendedFloatingActionButton

    // ✅ Adapter & Data
    private var materialAdapter           : MaterialAdapter? = null
    private val allMaterials              = mutableListOf<MaterialModel>()
    private var listenerRegistration      : ListenerRegistration? = null

    companion object {
        private const val REQUEST_ADD_EDIT = 200
    }

    // ─────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_materials)

        session = SessionManager(this)

        bindViews()
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupFab()
        listenToMaterials()
    }

    // ─────────────────────────────────────────
    // ✅ BIND VIEWS
    private fun bindViews() {
        btnBack         = findViewById(R.id.btnBack)
        tvToolbarTitle  = findViewById(R.id.tvToolbarTitle)
        etSearch        = findViewById(R.id.etSearch)
        tvMaterialCount = findViewById(R.id.tvMaterialCount)
        rvMaterials     = findViewById(R.id.rvMaterials)
        layoutEmpty     = findViewById(R.id.layoutEmpty)
        fabAddMaterial  = findViewById(R.id.fabAddMaterial)
    }

    // ─────────────────────────────────────────
    // ✅ TOOLBAR
    private fun setupToolbar() {
        tvToolbarTitle.text = "Study Materials"
        btnBack.setOnClickListener { finish() }
    }

    // ─────────────────────────────────────────
    // ✅ RECYCLERVIEW SETUP
    private fun setupRecyclerView() {
        rvMaterials.layoutManager = LinearLayoutManager(this)

        materialAdapter = MaterialAdapter(
            materials     = mutableListOf(),
            isEditable    = true,  // ✅ HOD/Faculty app → always editable

            // ✅ Card click → VIEW mode
            onCardClick   = { material ->
                openMaterial(material, "view")
            },

            // ✅ Edit button click → EDIT mode
            onEditClick   = { material ->
                openMaterial(material, "edit")
            },

            // ✅ Delete button click → confirm dialog
            onDeleteClick = { material, position ->
                showDeleteConfirmDialog(material, position)
            }
        )

        rvMaterials.adapter = materialAdapter
    }

    // ─────────────────────────────────────────
    // ✅ FAB — Add new material
    private fun setupFab() {
        fabAddMaterial.setOnClickListener {
            val intent = Intent(this, AddMaterialActivity::class.java).apply {
                putExtra("mode", "add")
            }
            startActivityForResult(intent, REQUEST_ADD_EDIT)
        }
    }

    private fun listenToMaterials() {

        val facultyId = session.getFacultyId()
        if (facultyId.isEmpty()) {
            showEmptyState()
            return
        }

        // ⭐ SHOW LOADER
        showBlockingLoader("Loading materials...")

        listenerRegistration = db.collection("study_materials")
            .whereEqualTo("uploadedBy", facultyId)
            .orderBy("uploadedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->

                // ⭐ HIDE LOADER when response arrives
                hideBlockingLoader()

                if (error != null) {
                    Toast.makeText(
                        this,
                        "❌ Failed to load: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmptyState()
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    showEmptyState()
                    return@addSnapshotListener
                }

                allMaterials.clear()

                for (doc in snapshot.documents) {
                    val material = doc.toObject(MaterialModel::class.java)
                    material?.let {
                        it.documentId = doc.id
                        allMaterials.add(it)
                    }
                }

                // Apply search filter if exists
                val currentQuery = etSearch.text.toString().trim()

                if (currentQuery.isNotEmpty()) {
                    filterMaterials(currentQuery)
                } else {
                    updateUI(allMaterials)
                }
            }
    }

    // ─────────────────────────────────────────
    // ✅ UPDATE UI — show list or empty state
    private fun updateUI(list: List<MaterialModel>) {
        if (list.isEmpty()) {
            showEmptyState()
        } else {
            rvMaterials.visibility  = View.VISIBLE
            layoutEmpty.visibility  = View.GONE
            tvMaterialCount.text    = "${list.size} material${if (list.size > 1) "s" else ""} found"
            materialAdapter?.updateList(list)
        }
    }

    private fun showEmptyState() {
        rvMaterials.visibility  = View.GONE
        layoutEmpty.visibility  = View.VISIBLE
        tvMaterialCount.text    = "No materials uploaded yet"
        materialAdapter?.updateList(emptyList())
    }

    // ─────────────────────────────────────────
    // ✅ SEARCH
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterMaterials(s.toString().trim())
            }
        })
    }

    private fun filterMaterials(query: String) {
        if (query.isEmpty()) {
            updateUI(allMaterials)
            return
        }

        val filtered = allMaterials.filter { material ->
            material.title.contains(query, ignoreCase = true)   ||
                    material.subject.contains(query, ignoreCase = true) ||
                    material.year.contains(query, ignoreCase = true)    ||
                    material.semester.contains(query, ignoreCase = true)
        }

        updateUI(filtered)
    }

    // ─────────────────────────────────────────
    // ✅ OPEN AddMaterialActivity in correct mode
    private fun openMaterial(material: MaterialModel, mode: String) {
        val intent = Intent(this, AddMaterialActivity::class.java).apply {
            putExtra("mode", mode)
            putExtra("material_id", material.documentId)
        }
        startActivityForResult(intent, REQUEST_ADD_EDIT)
    }

    // ─────────────────────────────────────────
    // ✅ DELETE CONFIRM DIALOG
    private fun showDeleteConfirmDialog(material: MaterialModel, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Material")
            .setMessage("Are you sure you want to delete \"${material.title}\"?\nThis cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteMaterial(material, position)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    // ✅ DELETE FROM FIRESTORE
    private fun deleteMaterial(material: MaterialModel, position: Int) {

        showBlockingLoader("Deleting material...")

        db.collection("study_materials")
            .document(material.documentId)
            .delete()
            .addOnSuccessListener {

                hideBlockingLoader()

                materialAdapter?.removeItem(position)

                Toast.makeText(
                    this,
                    "✅ Material deleted!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->

                hideBlockingLoader()

                Toast.makeText(
                    this,
                    "❌ Delete failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ADD_EDIT && resultCode == Activity.RESULT_OK) {
            //showBlockingLoader("Refreshing list...")
        }
    }

    // ─────────────────────────────────────────
    // ✅ Remove listener to avoid memory leaks
    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}
