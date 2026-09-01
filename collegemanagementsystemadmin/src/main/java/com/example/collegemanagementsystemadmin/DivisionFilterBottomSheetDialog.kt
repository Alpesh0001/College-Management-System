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

class DivisionFilterBottomSheetDialog(
    private val onApplyFilters: (
        course: String?,
        year: String?,
        semester: String?,
        sort: String
    ) -> Unit
) : BottomSheetDialogFragment() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var ddCourse: AutoCompleteTextView
    private lateinit var ddYear: AutoCompleteTextView
    private lateinit var ddSemester: AutoCompleteTextView
    private lateinit var ddSort: AutoCompleteTextView
    private lateinit var btnApply: Button
    private lateinit var btnClear: Button

    data class CourseItem(val code: String, val name: String, val durationYears: Int) {
        override fun toString(): String = "$name ($code)"
    }

    private val courses = mutableListOf<CourseItem>()
    private var selectedCourse: CourseItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_filter_division, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupStaticDropdowns()
        loadCourses()
        setupButtons()
    }

    private fun bindViews(view: View) {
        ddCourse = view.findViewById(R.id.ddCourse)
        ddYear = view.findViewById(R.id.ddYear)
        ddSemester = view.findViewById(R.id.ddSemester)
        ddSort = view.findViewById(R.id.ddSort)
        btnApply = view.findViewById(R.id.btnApply)
        btnClear = view.findViewById(R.id.btnClear)

        listOf(ddCourse, ddYear, ddSemester, ddSort).forEach { v ->
            v.setOnClickListener { v.showDropDown() }
        }
    }

    private fun loadCourses() {
        courses.clear()
        courses.add(CourseItem("All", "All Courses", 4))  // Default "All" option

        db.collection("courses")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { snap ->
                snap.documents
                    .sortedBy { it.getString("name").orEmpty() }
                    .forEach { d ->
                        val code = d.getString("code").orEmpty()
                        val name = d.getString("name").orEmpty()
                        val durationYears = when (val v = d.get("durationYears")) {
                            is Number -> v.toInt()
                            is String -> v.toIntOrNull() ?: 3
                            else -> 3
                        }
                        if (code.isNotEmpty()) {
                            courses.add(CourseItem(code, name, durationYears))
                        }
                    }

                ddCourse.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        courses
                    )
                )
                ddCourse.setText("All Courses (All)", false)
            }

        // ✅ FIX: Update year/semester when course is selected
        ddCourse.setOnItemClickListener { _, _, position, _ ->
            selectedCourse = courses.getOrNull(position)
            selectedCourse?.let { updateYearSemForCourse(it) }
        }
    }

    // ✅ FIX: Update year/semester based on course selection
    private fun updateYearSemForCourse(course: CourseItem) {
        if (course.code == "All") {
            // Show all options when "All" is selected
            setupYearSemesterDropdowns(
                years = listOf("All", "1", "2", "3", "4"),
                semesters = listOf("All", "1", "2", "3", "4", "5", "6", "7", "8")
            )
            return
        }

        val durationYears = course.durationYears

        // Generate year options based on course duration
        val years = mutableListOf("All")
        for (y in 1..durationYears) years.add(y.toString())

        // Generate all possible semesters for this course
        val sems = mutableListOf("All")
        for (s in 1..(durationYears * 2)) sems.add(s.toString())

        setupYearSemesterDropdowns(years, sems)

        // ✅ FIX: Set up year listener to update semester
        ddYear.setOnItemClickListener { _, _, _, _ ->
            val y = ddYear.text?.toString()?.trim().orEmpty()

            if (y == "All") {
                // Show all semesters for this course
                val allSems = mutableListOf("All")
                for (s in 1..(durationYears * 2)) allSems.add(s.toString())

                ddSemester.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        allSems
                    )
                )
                ddSemester.setText("All", false)
            } else {
                val yearNo = y.toIntOrNull() ?: 1

                // Generate semesters for selected year (e.g., Year 1 → Sem 1,2; Year 2 → Sem 3,4)
                val yearSems = mutableListOf("All")
                yearSems.add(((yearNo * 2) - 1).toString())
                yearSems.add((yearNo * 2).toString())

                ddSemester.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        yearSems
                    )
                )
                ddSemester.setText("All", false)
            }
        }
    }

    private fun setupYearSemesterDropdowns(years: List<String>, semesters: List<String>) {
        ddYear.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                years
            )
        )
        ddSemester.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                semesters
            )
        )

        ddYear.setText("All", false)
        ddSemester.setText("All", false)
    }

    private fun setupStaticDropdowns() {
        // Initial generic values (before course is selected)
        setupYearSemesterDropdowns(
            years = listOf("All", "1", "2", "3", "4"),
            semesters = listOf("All", "1", "2", "3", "4", "5", "6", "7", "8")
        )

        val sortOptions = listOf(
            "Division Name",
            "Course",
            "Year",
            "Recently Added"
        )
        ddSort.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                sortOptions
            )
        )
        ddSort.setText("Division Name", false)
    }

    private fun setupButtons() {
        btnApply.setOnClickListener {
            val course = if (selectedCourse?.code == "All") null else selectedCourse?.code
            val year = ddYear.text.toString().trim().let { if (it == "All") null else it }
            val semester = ddSemester.text.toString().trim().let { if (it == "All") null else it }
            val sort = ddSort.text.toString().trim()

            onApplyFilters(course, year, semester, sort)
            dismiss()
        }

        btnClear.setOnClickListener {
            ddCourse.setText("All Courses (All)", false)
            selectedCourse = courses.firstOrNull()
            setupYearSemesterDropdowns(
                years = listOf("All", "1", "2", "3", "4"),
                semesters = listOf("All", "1", "2", "3", "4", "5", "6", "7", "8")
            )
            ddSort.setText("Division Name", false)
        }
    }
}
