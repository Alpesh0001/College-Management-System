package com.example.collegemanagementsystemfaculty.models

data class StudentModel(
    var id: String = "",
    var fullName: String = "",
    var rollNo: String = "",
    var courseName: String = "",
    var year: String = "",
    var semester: String = "",
    var divisionId: String = "",
    var divisionName: String = "",
    var photoUrl: String = ""
)