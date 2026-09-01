package com.example.collegemanagementsystemadmin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore

class FacultyFilterBottomSheetDialog(
    private val onApplyFilters: (
        course: String?,
        role: String?,
        designation: String?,
        status: String?,
        sort: String
    ) -> Unit
) : BottomSheetDialogFragment() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var ddCourse: AutoCompleteTextView
    private lateinit var ddRole: AutoCompleteTextView
    private lateinit var ddDesignation: AutoCompleteTextView
    private lateinit var ddStatus: AutoCompleteTextView
    private lateinit var ddSort: AutoCompleteTextView
    private lateinit var btnApply: Button
    private lateinit var btnClear: Button

    private val courses = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_filter_faculty, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        loadCourses()
        setupStaticDropdowns()
        setupButtons()
    }

    private fun bindViews(view: View) {
        ddCourse = view.findViewById(R.id.ddCourse)
        ddRole = view.findViewById(R.id.ddRole)
        ddDesignation = view.findViewById(R.id.ddDesignation)
        ddStatus = view.findViewById(R.id.ddStatus)
        ddSort = view.findViewById(R.id.ddSort)
        btnApply = view.findViewById(R.id.btnApply)
        btnClear = view.findViewById(R.id.btnClear)

        listOf(ddCourse, ddRole, ddDesignation, ddStatus, ddSort).forEach { v ->
            v.setOnClickListener { v.showDropDown() }
        }
    }

    private fun loadCourses() {
        courses.clear()
        courses.add("All")

        db.collection("courses")
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { d ->
                    val status = d.getString("status") ?: "Active"
                    if (status == "Active") {
                        val code = d.getString("code").orEmpty()
                        courses.add(code)
                    }
                }
                ddCourse.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, courses))
                ddCourse.setText("All", false)
            }
    }

    private fun setupStaticDropdowns() {
        // Role
        val roles = listOf("All", "Faculty", "HOD")
        ddRole.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, roles))
        ddRole.setText("All", false)

        // Designation
        val designations = listOf(
            "All",
            "Professor",
            "Associate Professor",
            "Assistant Professor",
            "Lecturer",
            "Senior Lecturer",
            "Guest Lecturer"
        )
        ddDesignation.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, designations))
        ddDesignation.setText("All", false)

        // Status
        val statuses = listOf("All", "Active", "Inactive", "On Leave", "Resigned")
        ddStatus.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statuses))
        ddStatus.setText("All", false)

        // Sort
        val sortOptions = listOf(
            "Name (A-Z)",
            "Name (Z-A)",
            "Employee ID",
            "Recently Added"
        )
        ddSort.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortOptions))
        ddSort.setText("Name (A-Z)", false)
    }

    private fun setupButtons() {
        btnApply.setOnClickListener {
            val course = ddCourse.text.toString().trim().let { if (it == "All") null else it }
            val role = ddRole.text.toString().trim().let { if (it == "All") null else it }
            val designation = ddDesignation.text.toString().trim().let { if (it == "All") null else it }
            val status = ddStatus.text.toString().trim().let { if (it == "All") null else it }
            val sort = ddSort.text.toString().trim()

            onApplyFilters(course, role, designation, status, sort)
            dismiss()
        }

        btnClear.setOnClickListener {
            ddCourse.setText("All", false)
            ddRole.setText("All", false)
            ddDesignation.setText("All", false)
            ddStatus.setText("All", false)
            ddSort.setText("Name (A-Z)", false)
        }
    }
}
