package com.example.collegemanagementsystemadmin.models

data class CourseOption(
    val id: String,
    val displayName: String
) {
    override fun toString(): String = displayName
}