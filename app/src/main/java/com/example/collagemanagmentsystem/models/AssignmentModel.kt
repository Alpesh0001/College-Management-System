package com.example.collagemanagmentsystem.models

data class AssignmentModel(
    var id: String = "",

    var title: String = "",
    var description: String = "",
    var year: String = "",
    var semester: String = "",
    var subject: String = "",

    var fileUrl: String = "",
    var fileName: String = "",

    var dueDate: Long = 0L,
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L,

    var createdBy: String = "",
    var courseId: String = ""
)
