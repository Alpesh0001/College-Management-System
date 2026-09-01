package com.example.collegemanagementsystemadmin.models

import com.google.firebase.Timestamp

data class Faculty(
    val id: String = "",
    val employeeId: String = "",
    val fullName: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val photoUrl: String = "",

    // Professional Info
    val qualification: String = "",
    val specialization: String = "",
    val experience: String = "",
    val joiningDate: String = "",
    val designation: String = "",

    // Course & Role
    val role: String = "Faculty", // Faculty or HOD
    val courseCode: String = "",
    val courseName: String = "",
    val courseId: String = "",

    // Other
    val salary: Int? = null,
    val status: String = "Active",
    val tempPassword: String = "",
    val passwordStatus: String = "not_set",

    // Timestamps
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
