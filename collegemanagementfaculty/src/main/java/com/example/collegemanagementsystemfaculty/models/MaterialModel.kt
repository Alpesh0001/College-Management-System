package com.example.collegemanagementsystemfaculty.models

data class MaterialModel(

    // Firestore document ID
    var documentId: String = "",

    // ✅ NEW: Course Reference
    val courseId: String = "",        // Firestore ID of the course
    val courseName: String = "",      // e.g. "BCA", "MCA", "B.Tech CSE"

    // Material Info
    val title: String = "",
    val description: String = "",

    // Classification
    val year: String = "",            // e.g. "1st Year", "2nd Year"
    val semester: String = "",        // e.g. "Sem 1", "Sem 2"
    val subject: String = "",         // e.g. "Data Structures"

    // File Info
    val fileUrl: String = "",         // Cloudinary PDF URL
    val fileName: String = "",        // Original file name e.g. "notes.pdf"
    val fileType: String = "pdf",     // pdf / doc / other

    // Uploader Info
    val uploadedBy: String = "",      // Firebase Auth UID
    val uploadedByName: String = "",  // Display name of HOD/Faculty

    // Timestamps
    val uploadedAt: Long = 0L,
    val updatedAt: Long = 0L
)
