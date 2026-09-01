package com.example.collegemanagementsystemfaculty.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

import com.example.collegemanagementsystemfaculty.BuildConfig

object CloudinaryHelper {

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key"    to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
            )
            MediaManager.init(context, config)
            isInitialized = true
        }
    }

    // ✅ Upload PDF/File to Cloudinary
    fun uploadFile(
        context: Context,
        fileUri: Uri,
        fileName: String,
        onProgress: (Int) -> Unit,       // progress 0–100
        onSuccess: (String) -> Unit,     // returns Cloudinary URL
        onError: (String) -> Unit        // returns error message
    ) {
        MediaManager.get()
            .upload(fileUri)
            .option("folder", "study_materials/")   // saves in study_materials folder
            .option("resource_type", "raw")
            .option("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)  // raw = for PDF/docs (not image/video)
            .option("public_id", "material_${System.currentTimeMillis()}_$fileName")
            .callback(object : UploadCallback {

                override fun onStart(requestId: String) {
                    onProgress(0)
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = ((bytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
                    onProgress(progress)
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"].toString()
                    onSuccess(url)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onError(error.description)
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    onError("Upload rescheduled: ${error.description}")
                }
            })
            .dispatch(context)
    }


    // ✅ Delete file from Cloudinary by public ID

    fun extractPublicId(cloudinaryUrl: String): String {
        // Extract public_id from URL for reference
        // e.g. https://res.cloudinary.com/yourcloud/raw/upload/v123/study_materials/file.pdf
        return try {
            val parts = cloudinaryUrl.split("/upload/")
            if (parts.size > 1) {
                val afterUpload = parts[1]
                // Remove version number (v1234567/) if present
                val withoutVersion = if (afterUpload.startsWith("v")) {
                    afterUpload.substringAfter("/")
                } else {
                    afterUpload
                }
                withoutVersion
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }


    // ✅ NEW: Upload Assignment PDF (saves in assignments/ folder)
    fun uploadAssignmentFile(
        context: Context,
        fileUri: Uri,
        fileName: String,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        MediaManager.get()
            .upload(fileUri)
            .option("folder", "assignments/")           // ← NEW: assignments folder
            .option("resource_type", "raw")
            .option("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .option("public_id", "assignment_${System.currentTimeMillis()}_$fileName")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) { onProgress(0) }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = ((bytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
                    onProgress(progress)
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"].toString()
                    onSuccess(url)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onError(error.description)
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    onError("Upload rescheduled: ${error.description}")
                }
            })
            .dispatch(context)
    }

}
