package com.example.collegemanagementsystemfaculty.models

data class Division(
    val id: String = "",
    val divisionName: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val courseCode: String = "",
    val year: String = "",
    val semester: String = "",
    val capacity: Int = 0,
    val currentStrength: Int = 0,
    val classTeacherId: String? = null,
    val classTeacherName: String? = null,
    val status: String = "Active",
    val hasTimetable: Boolean = false
)
