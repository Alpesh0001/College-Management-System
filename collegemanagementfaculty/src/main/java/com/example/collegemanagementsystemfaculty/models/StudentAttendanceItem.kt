package com.example.collegemanagementsystemfaculty.models

data class StudentAttendanceItem(
    val studentId   : String,
    val studentName : String,
    val rollNo      : String,
    val rollNumber  : Int,      // ✅ Just number e.g. 1
    var status      : String = "Present"
)
