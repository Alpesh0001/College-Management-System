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

class FeesFilterBottomSheet(
    private val onApply: (courseId: String?, courseName: String?, year: String?, sem: String?) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var ddCourse: AutoCompleteTextView
    private lateinit var ddYear: AutoCompleteTextView
    private lateinit var ddSem: AutoCompleteTextView
    private lateinit var btnClear: Button
    private lateinit var btnApply: Button

    private val db = FirebaseFirestore.getInstance()
    private val courses = mutableListOf<CourseItem>()
    private var selectedCourse: CourseItem? = null

    data class CourseItem(
        val id: String,
        val code: String,
        val name: String,
        val durationYears: Int
    ) {
        override fun toString() = "$name ($code)"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_fees_filter, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ddCourse = view.findViewById(R.id.ddCourse)
        ddYear   = view.findViewById(R.id.ddYear)
        ddSem    = view.findViewById(R.id.ddSemester)
        btnClear = view.findViewById(R.id.btnClear)
        btnApply = view.findViewById(R.id.btnApply)

        listOf(ddCourse, ddYear, ddSem).forEach {
            it.setOnClickListener { _ -> it.showDropDown() }
        }

        loadCourses()

        // ✅ Course selected → update years
        ddCourse.setOnItemClickListener { _, _, position, _ ->
            selectedCourse = courses.getOrNull(position)
            selectedCourse?.let { updateYears(it.durationYears) }
        }

        // ✅ Year selected → update sems
        ddYear.setOnItemClickListener { _, _, _, _ ->
            val yearNo = ddYear.text.toString().toIntOrNull() ?: 1
            updateSems(yearNo)
        }

        btnClear.setOnClickListener {
            ddCourse.setText("", false)
            ddYear.setText("", false)
            ddSem.setText("", false)
            selectedCourse = null
        }

        btnApply.setOnClickListener {
            val courseId   = selectedCourse?.id
            val courseName = selectedCourse?.name
            val year       = ddYear.text.toString().trim().ifEmpty { null }
            val sem        = ddSem.text.toString().trim().ifEmpty { null }

            if (courseId.isNullOrEmpty()) {
                Toast.makeText(requireContext(),
                    "Please select a course", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (year.isNullOrEmpty()) {
                Toast.makeText(requireContext(),
                    "Please select a year", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sem.isNullOrEmpty()) {
                Toast.makeText(requireContext(),
                    "Please select a semester", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            onApply(courseId, courseName, year, sem)
            dismiss()
        }
    }

    private fun loadCourses() {
        db.collection("courses")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { snap ->
                courses.clear()
                snap.documents.forEach { doc ->
                    val years = when (val v = doc.get("durationYears")) {
                        is Number -> v.toInt()
                        is String -> v.toIntOrNull() ?: 3
                        else      -> 3
                    }
                    courses.add(
                        CourseItem(
                            id            = doc.id,
                            code          = doc.getString("code").orEmpty(),
                            name          = doc.getString("name").orEmpty(),
                            durationYears = years
                        )
                    )
                }
                ddCourse.setAdapter(
                    ArrayAdapter(requireContext(),
                        android.R.layout.simple_list_item_1, courses)
                )
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    "Failed to load courses", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateYears(durationYears: Int) {
        val years = (1..durationYears).map { it.toString() }
        ddYear.setAdapter(
            ArrayAdapter(requireContext(),
                android.R.layout.simple_list_item_1, years)
        )
        ddYear.setText("", false)
        ddSem.setText("", false)
    }

    private fun updateSems(yearNo: Int) {
        val sems = listOf(
            ((yearNo * 2) - 1).toString(),
            (yearNo * 2).toString()
        )
        ddSem.setAdapter(
            ArrayAdapter(requireContext(),
                android.R.layout.simple_list_item_1, sems)
        )
        ddSem.setText("", false)
    }
}
