package com.example.collagemanagmentsystem.models

data class MaterialModel(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var subject: String = "",
    var year: String = "",
    var semester: String = "",
    var fileUrl: String = "",
    var fileName: String = "",
    var fileType: String = "",  // pdf / doc / image
    var courseId: String = "",
    var createdBy: String = "",
    var uploadedAt: Long = 0L
)
