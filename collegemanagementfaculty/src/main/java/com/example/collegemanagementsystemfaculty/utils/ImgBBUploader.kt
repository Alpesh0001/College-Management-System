package com.example.collegemanagementsystemfaculty.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

import com.example.collegemanagementsystemfaculty.BuildConfig

object ImgBBUploader {

    private val API_KEY = BuildConfig.IMGBB_API_KEY
    private const val UPLOAD_URL = "https://api.imgbb.com/1/upload"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadImage(context: Context, imageUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val compressedFile = compressImage(context, imageUri)
                val base64Image = encodeImageToBase64(compressedFile)

                val requestBody = FormBody.Builder()
                    .add("key", API_KEY)
                    .add("image", base64Image)
                    .build()

                val request = Request.Builder()
                    .url(UPLOAD_URL)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val imageUrl = jsonResponse.getJSONObject("data").getString("url")
                    compressedFile.delete()
                    Result.success(imageUrl)
                } else {
                    Result.failure(Exception("Upload failed: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun compressImage(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val maxSize = 1024
        val width = bitmap.width
        val height = bitmap.height

        val scale = if (width > height) maxSize.toFloat() / width
        else maxSize.toFloat() / height

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

        val tempFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        val fileOutputStream = FileOutputStream(tempFile)
        fileOutputStream.write(outputStream.toByteArray())
        fileOutputStream.close()

        bitmap.recycle()
        resizedBitmap.recycle()

        return tempFile
    }

    private fun encodeImageToBase64(file: File): String {
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
