package com.example.collagemanagmentsystem.models

data class StudentSubjectAttendance(
    val subjectName: String = "",
    val subjectCode: String = "",
    val present: Int = 0,
    val absent: Int = 0,
    val total: Int = 0
) {
    val percentage: Int
        get() = if (total > 0) (present * 100) / total else 0
}
