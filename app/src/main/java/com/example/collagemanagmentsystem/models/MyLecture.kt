package com.example.collagemanagmentsystem.models

data class MyLecture(
    val slotId       : String = "",
    val divisionId   : String = "",
    val divisionName : String = "",
    val day          : String = "",
    val timeFrom     : String = "",
    val timeTo       : String = "",
    val subjectName  : String = "",
    val subjectCode  : String = "",
    val roomNo       : String = "",
    val slotType     : String = "lecture"
)
