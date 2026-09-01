package com.example.collegemanagementsystemadmin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SubjectFilterBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var ddCourse: AutoCompleteTextView
    private lateinit var ddYear: AutoCompleteTextView
    private lateinit var ddSem: AutoCompleteTextView
    private lateinit var ddStatus: AutoCompleteTextView
    private lateinit var btnClear: Button
    private lateinit var btnApply: Button

    var courseOptions: List<String> = emptyList()
    var courseDetailsMap: Map<String, Pair<String, Int>> = emptyMap() // courseName -> (courseId, duration)
    var onApply: ((String, String, String, String) -> Unit)? = null
    var onClear: (() -> Unit)? = null

    private var selectedCourseName = ""
    private var selectedYear = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_filter_subjects, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ddCourse = view.findViewById(R.id.ddCourse)
        ddYear = view.findViewById(R.id.ddYear)
        ddSem = view.findViewById(R.id.ddSem)
        ddStatus = view.findViewById(R.id.ddStatus)
        btnClear = view.findViewById(R.id.btnClearFilters)
        btnApply = view.findViewById(R.id.btnApplyFilters)

        setupDropdowns()
        setupButtons()
    }

    private fun setupDropdowns() {
        // Course dropdown
        val courseAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            courseOptions
        )
        ddCourse.setAdapter(courseAdapter)
        ddCourse.setText("All Courses", false)

        ddCourse.setOnItemClickListener { _, _, position, _ ->
            selectedCourseName = courseOptions[position]
            if (selectedCourseName != "All Courses") {
                updateYearDropdown(selectedCourseName)
            } else {
                setupDefaultYearDropdown()
                setupDefaultSemesterDropdown()
            }
        }

        ddCourse.setOnClickListener { ddCourse.showDropDown() }

        // Year dropdown
        ddYear.setOnItemClickListener { _, _, _, _ ->
            val yearText = ddYear.text.toString()
            if (yearText != "All Years") {
                val year = yearText.replace("Year ", "").toIntOrNull() ?: 1
                selectedYear = year
                updateSemesterDropdown(year)
            } else {
                setupDefaultSemesterDropdown()
            }
        }
        ddYear.setOnClickListener { ddYear.showDropDown() }

        // Semester dropdown
        ddSem.setOnClickListener { ddSem.showDropDown() }

        // Status dropdown
        val statusOptions = listOf("All Status", "Active", "Inactive")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusOptions)
        ddStatus.setAdapter(statusAdapter)
        ddStatus.setText("All Status", false)
        ddStatus.setOnClickListener { ddStatus.showDropDown() }

        // Setup defaults
        setupDefaultYearDropdown()
        setupDefaultSemesterDropdown()
    }

    private fun setupDefaultYearDropdown() {
        val yearOptions = listOf("All Years", "Year 1", "Year 2", "Year 3", "Year 4", "Year 5", "Year 6")
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, yearOptions)
        ddYear.setAdapter(yearAdapter)
        ddYear.setText("All Years", false)
    }

    private fun setupDefaultSemesterDropdown() {
        val semOptions = (1..12).map { "Semester $it" }.toMutableList()
        semOptions.add(0, "All Semesters")
        val semAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, semOptions)
        ddSem.setAdapter(semAdapter)
        ddSem.setText("All Semesters", false)
    }

    private fun updateYearDropdown(courseName: String) {
        val duration = courseDetailsMap[courseName]?.second ?: 3

        val yearOptions = mutableListOf("All Years")
        yearOptions.addAll((1..duration).map { "Year $it" })

        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, yearOptions)
        ddYear.setAdapter(yearAdapter)
        ddYear.setText("All Years", false)

        setupDefaultSemesterDropdown()
    }

    private fun updateSemesterDropdown(year: Int) {
        val startSem = (year - 1) * 2 + 1
        val endSem = year * 2

        val semOptions = mutableListOf("All Semesters")
        semOptions.addAll((startSem..endSem).map { "Semester $it" })

        val semAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, semOptions)
        ddSem.setAdapter(semAdapter)
        ddSem.setText("All Semesters", false)
    }

    private fun setupButtons() {
        btnClear.setOnClickListener {
            onClear?.invoke()
            dismiss()
        }

        btnApply.setOnClickListener {
            val course = ddCourse.text.toString()
            val year = ddYear.text.toString()
            val sem = ddSem.text.toString()
            val status = ddStatus.text.toString()

            onApply?.invoke(course, year, sem, status)
            dismiss()
        }
    }
}
