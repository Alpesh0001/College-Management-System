package com.example.collegemanagementsystemadmin.models

import com.google.firebase.Timestamp

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
    val rollNumberRanges: List<RollRange> = emptyList(),
    val classTeacherId: String? = null,
    val classTeacherName: String? = null,
    val classTeacherEmail: String? = null,
    val status: String = "Active",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class RollRange(
    val start: Int = 0,
    val end: Int = 0
) {
    fun toDisplayString(): String {
        return if (start == end) {
            "$start"
        } else {
            "$start-$end"
        }
    }

    fun containsRoll(roll: Int): Boolean {
        return roll in start..end
    }
}
