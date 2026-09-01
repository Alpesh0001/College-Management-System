package com.example.collegemanagementsystemadmin.models

data class Student(
    val id: String = "",
    val fullName: String = "",
    val grNo: String = "",
    val rollNo: String = "",
    val dob: String = "",
    val gender: String = "",
    val bloodGroup: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val courseCode: String = "",
    val year: String = "",
    val semester: String = "",
    val admissionYear: String = "",
    val status: String = "",
    val tempPassword: String = "",
    val passwordStatus: String = "not_set",
    val photoUrl: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
    val updatedAt: com.google.firebase.Timestamp? = null
) {
    fun getInitial(): String {
        return fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }

    fun getYearSemesterLabel(): String {
        return "Y$year S$semester"
    }
}
