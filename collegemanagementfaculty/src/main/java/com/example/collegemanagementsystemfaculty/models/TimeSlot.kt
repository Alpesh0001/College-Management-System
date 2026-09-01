package com.example.collegemanagementsystemfaculty.models

data class TimeSlot(
    val id: String = "",
    val day: String = "",
    val timeFrom: String = "",
    val timeTo: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val facultyName: String = "",
    val facultyId: String = "",
    val roomNo: String = "",
    val slotType: String = "lecture" // lecture / lab / break
)
