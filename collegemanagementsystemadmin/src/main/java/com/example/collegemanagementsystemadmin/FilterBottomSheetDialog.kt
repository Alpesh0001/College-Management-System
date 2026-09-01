package com.example.collegemanagementsystemadmin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FilterBottomSheetDialog(
    private val onApplyFilters: (String?, String?, String?, String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var ddCourse: AutoCompleteTextView
    private lateinit var ddYear: AutoCompleteTextView
    private lateinit var ddSem: AutoCompleteTextView
    private lateinit var ddSort: AutoCompleteTextView
    private lateinit var btnClearFilters: Button
    private lateinit var btnApplyFilters: Button

    private val db = FirebaseFirestore.getInstance()
    private val courses = mutableListOf<CourseItem>()
    private var selectedCourse: CourseItem? = null

    // ✅ Updated to include durationYears
    data class CourseItem(
        val id: String,
        val code: String,
        val name: String,
        val durationYears: Int
    ) {
        override fun toString(): String = "$name ($code)"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_filter_students, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ddCourse = view.findViewById(R.id.ddCourse)
        ddYear = view.findViewById(R.id.ddYear)
        ddSem = view.findViewById(R.id.ddSem)
        ddSort = view.findViewById(R.id.ddSort)
        btnClearFilters = view.findViewById(R.id.btnClearFilters)
        btnApplyFilters = view.findViewById(R.id.btnApplyFilters)

        setupDropdowns()
        loadCourses()
        setupListeners()

        btnClearFilters.setOnClickListener {
            clearAllFilters()
        }

        btnApplyFilters.setOnClickListener {
            val course = ddCourse.text.toString().trim().ifEmpty { null }
            val year = ddYear.text.toString().trim().ifEmpty { null }
            val sem = ddSem.text.toString().trim().ifEmpty { null }
            val sort = ddSort.text.toString().trim()

            onApplyFilters(course, year, sem, sort)
            dismiss()
        }
    }

    private fun setupDropdowns() {
        // Sort options
        val sortOptions = listOf("Name (A-Z)", "Name (Z-A)", "Roll Number", "Recently Added")
        ddSort.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortOptions))
        ddSort.setText("Name (A-Z)", false)

        // Enable dropdown clicks
        listOf(ddCourse, ddYear, ddSem, ddSort).forEach { v ->
            v.setOnClickListener { v.showDropDown() }
        }
    }

    private fun setupListeners() {
        // ✅ When course is selected, update year options
        ddCourse.setOnItemClickListener { _, _, position, _ ->
            selectedCourse = courses.getOrNull(position)
            selectedCourse?.let { course ->
                updateYearOptions(course.durationYears)
            }
        }

        // ✅ When year is selected, update semester options
        ddYear.setOnItemClickListener { _, _, _, _ ->
            val y = ddYear.text.toString().trim()
            val yearNo = y.toIntOrNull() ?: 1
            updateSemesterOptions(yearNo)
        }
    }

    private fun loadCourses() {
        db.collection("courses")
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snap ->
                courses.clear()
                snap.documents.forEach { d ->
                    val status = d.getString("status") ?: "Active"
                    if (status == "Active") {
                        val id = d.id
                        val code = d.getString("code").orEmpty()
                        val name = d.getString("name").orEmpty()
                        val durationYears = when (val v = d.get("durationYears")) {
                            is Number -> v.toInt()
                            is String -> v.toIntOrNull() ?: 3
                            else -> 3
                        }
                        courses.add(CourseItem(id, code, name, durationYears))
                    }
                }
                ddCourse.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, courses))
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load courses", Toast.LENGTH_SHORT).show()
            }
    }

    // ✅ Update year dropdown based on course duration
    private fun updateYearOptions(durationYears: Int) {
        val years = (1..durationYears.coerceAtLeast(1)).map { it.toString() }
        ddYear.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, years))
        ddYear.setText("", false)  // Clear previous selection
        ddSem.setText("", false)   // Clear semester too
    }

    // ✅ Update semester options based on year
    private fun updateSemesterOptions(yearNo: Int) {
        val sems = listOf(((yearNo * 2) - 1).toString(), (yearNo * 2).toString())
        ddSem.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sems))
        ddSem.setText("", false)  // Clear previous selection
    }

    private fun clearAllFilters() {
        ddCourse.setText("", false)
        ddYear.setText("", false)
        ddSem.setText("", false)
        ddSort.setText("Name (A-Z)", false)
        selectedCourse = null
    }
}
