package com.example.collegemanagementsystemfaculty.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(private val context: Context) {

    companion object {
        private const val PREF_NAME         = "FacultySession"
        private const val KEY_IS_LOGGED_IN  = "isLoggedIn"
        private const val KEY_FACULTY_ID    = "facultyId"
        private const val KEY_EMPLOYEE_ID   = "employeeId"
        private const val KEY_FULL_NAME     = "fullName"
        private const val KEY_EMAIL         = "email"
        private const val KEY_PHONE         = "phone"
        private const val KEY_ROLE          = "role"
        private const val KEY_COURSE_ID     = "courseId"
        private const val KEY_COURSE_NAME   = "courseName"
        private const val KEY_COURSE_CODE   = "courseCode"
        private const val KEY_PHOTO_URL     = "photoUrl"
        private const val KEY_DESIGNATION   = "designation"
        private const val KEY_PASS_STATUS   = "passwordStatus"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════
    // ✅ Save Session
    // ═══════════════════════════════════════
    fun saveSession(
        facultyId: String,
        employeeId: String,
        fullName: String,
        email: String,
        phone: String,
        role: String,
        courseId: String,
        courseName: String,
        courseCode: String,
        photoUrl: String,
        designation: String,
        passwordStatus: String
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_FACULTY_ID,   facultyId)
            putString(KEY_EMPLOYEE_ID,  employeeId)
            putString(KEY_FULL_NAME,    fullName)
            putString(KEY_EMAIL,        email)
            putString(KEY_PHONE,        phone)
            putString(KEY_ROLE,         role)
            putString(KEY_COURSE_ID,    courseId)
            putString(KEY_COURSE_NAME,  courseName)
            putString(KEY_COURSE_CODE,  courseCode)
            putString(KEY_PHOTO_URL,    photoUrl)
            putString(KEY_DESIGNATION,  designation)
            putString(KEY_PASS_STATUS,  passwordStatus)
            apply()
        }
    }

    // ═══════════════════════════════════════
    // ✅ Getters
    // ═══════════════════════════════════════
    fun isLoggedIn(): Boolean =
        prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getFacultyId(): String =
        prefs.getString(KEY_FACULTY_ID, "").orEmpty()

    fun getEmployeeId(): String =
        prefs.getString(KEY_EMPLOYEE_ID, "").orEmpty()

    fun getFullName(): String =
        prefs.getString(KEY_FULL_NAME, "").orEmpty()

    fun getEmail(): String =
        prefs.getString(KEY_EMAIL, "").orEmpty()

    fun getPhone(): String =
        prefs.getString(KEY_PHONE, "").orEmpty()

    fun getRole(): String =
        prefs.getString(KEY_ROLE, "").orEmpty()

    fun getCourseId(): String =
        prefs.getString(KEY_COURSE_ID, "").orEmpty()

    fun getCourseName(): String =
        prefs.getString(KEY_COURSE_NAME, "").orEmpty()

    fun getCourseCode(): String =
        prefs.getString(KEY_COURSE_CODE, "").orEmpty()

    fun getPhotoUrl(): String =
        prefs.getString(KEY_PHOTO_URL, "").orEmpty()

    fun getDesignation(): String =
        prefs.getString(KEY_DESIGNATION, "").orEmpty()

    fun getPasswordStatus(): String =
        prefs.getString(KEY_PASS_STATUS, "").orEmpty().lowercase()

    // ✅ Role Helpers
    fun isHOD(): Boolean     = getRole() == "HOD"
    fun isFaculty(): Boolean = getRole() == "Faculty"

    // ═══════════════════════════════════════
    // ✅ Update Password Status
    // ═══════════════════════════════════════
    fun updatePasswordStatus(status: String) {
        prefs.edit()
            .putString(KEY_PASS_STATUS, status)
            .apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
