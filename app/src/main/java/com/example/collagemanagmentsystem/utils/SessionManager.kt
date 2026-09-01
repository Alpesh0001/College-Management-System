package com.example.collagemanagmentsystem.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(private val context: Context) {

    companion object {
        private const val PREF_NAME           = "StudentSession"
        private const val KEY_IS_LOGGED_IN    = "isLoggedIn"
        private const val KEY_STUDENT_ID      = "studentId"
        private const val KEY_FULL_NAME       = "fullName"
        private const val KEY_EMAIL           = "email"
        private const val KEY_PHONE           = "phone"
        private const val KEY_GR_NO           = "grNo"
        private const val KEY_ROLL_NO         = "rollNo"
        private const val KEY_COURSE_ID       = "courseId"
        private const val KEY_COURSE_NAME     = "courseName"
        private const val KEY_COURSE_CODE     = "courseCode"
        private const val KEY_DIVISION_ID     = "divisionId"
        private const val KEY_DIVISION_NAME   = "divisionName"
        private const val KEY_SEMESTER        = "semester"
        private const val KEY_YEAR            = "year"
        private const val KEY_GENDER          = "gender"
        private const val KEY_DOB             = "dob"
        private const val KEY_BLOOD_GROUP     = "bloodGroup"
        private const val KEY_ADDRESS         = "address"
        private const val KEY_ADMISSION_YEAR  = "admissionYear"
        private const val KEY_PHOTO_URL       = "photoUrl"
        private const val KEY_STATUS          = "status"
        private const val KEY_PASS_STATUS     = "passwordStatus"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════
    // ✅ Save Full Session
    // ═══════════════════════════════════════
    fun saveSession(
        studentId: String,
        fullName: String,
        email: String,
        phone: String,
        grNo: String,
        rollNo: String,
        courseId: String,
        courseName: String,
        courseCode: String,
        divisionId: String,
        divisionName: String,
        semester: String,
        year: String,
        gender: String,
        dob: String,
        bloodGroup: String,
        address: String,
        admissionYear: String,
        photoUrl: String,
        status: String,
        passwordStatus: String
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN,   true)
            putString(KEY_STUDENT_ID,      studentId)
            putString(KEY_FULL_NAME,       fullName)
            putString(KEY_EMAIL,           email)
            putString(KEY_PHONE,           phone)
            putString(KEY_GR_NO,           grNo)
            putString(KEY_ROLL_NO,         rollNo)
            putString(KEY_COURSE_ID,       courseId)
            putString(KEY_COURSE_NAME,     courseName)
            putString(KEY_COURSE_CODE,     courseCode)
            putString(KEY_DIVISION_ID,     divisionId)
            putString(KEY_DIVISION_NAME,   divisionName)
            putString(KEY_SEMESTER,        semester)
            putString(KEY_YEAR,            year)
            putString(KEY_GENDER,          gender)
            putString(KEY_DOB,             dob)
            putString(KEY_BLOOD_GROUP,     bloodGroup)
            putString(KEY_ADDRESS,         address)
            putString(KEY_ADMISSION_YEAR,  admissionYear)
            putString(KEY_PHOTO_URL,       photoUrl)
            putString(KEY_STATUS,          status)
            putString(KEY_PASS_STATUS,     passwordStatus)
            apply()
        }
    }

    // ═══════════════════════════════════════
    // ✅ Getters
    // ═══════════════════════════════════════
    fun isLoggedIn(): Boolean =
        prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getStudentId(): String =
        prefs.getString(KEY_STUDENT_ID, "").orEmpty()


    fun getFullName(): String =
        prefs.getString(KEY_FULL_NAME, "").orEmpty()

    fun getEmail(): String =
        prefs.getString(KEY_EMAIL, "").orEmpty()

    fun getPhone(): String =
        prefs.getString(KEY_PHONE, "").orEmpty()

    fun getGrNo(): String =
        prefs.getString(KEY_GR_NO, "").orEmpty()

    fun getRollNo(): String =
        prefs.getString(KEY_ROLL_NO, "").orEmpty()

    fun getCourseId(): String =
        prefs.getString(KEY_COURSE_ID, "").orEmpty()

    fun getCourseName(): String =
        prefs.getString(KEY_COURSE_NAME, "").orEmpty()

    fun getCourseCode(): String =
        prefs.getString(KEY_COURSE_CODE, "").orEmpty()

    fun getDivisionId(): String =
        prefs.getString(KEY_DIVISION_ID, "").orEmpty()

    fun getDivisionName(): String =
        prefs.getString(KEY_DIVISION_NAME, "").orEmpty()

    fun getSemester(): String =
        prefs.getString(KEY_SEMESTER, "").orEmpty()

    fun getYear(): String =
        prefs.getString(KEY_YEAR, "").orEmpty()

    fun getGender(): String =
        prefs.getString(KEY_GENDER, "").orEmpty()

    fun getDob(): String =
        prefs.getString(KEY_DOB, "").orEmpty()

    fun getBloodGroup(): String =
        prefs.getString(KEY_BLOOD_GROUP, "").orEmpty()

    fun getAddress(): String =
        prefs.getString(KEY_ADDRESS, "").orEmpty()

    fun getAdmissionYear(): String =
        prefs.getString(KEY_ADMISSION_YEAR, "").orEmpty()

    fun getPhotoUrl(): String =
        prefs.getString(KEY_PHOTO_URL, "").orEmpty()

    fun getStatus(): String =
        prefs.getString(KEY_STATUS, "").orEmpty()

    fun getPasswordStatus(): String =
        prefs.getString(KEY_PASS_STATUS, "").orEmpty().lowercase()

    // ═══════════════════════════════════════
    // ✅ Update Helpers
    // ═══════════════════════════════════════
    fun updatePasswordStatus(status: String) {
        prefs.edit().putString(KEY_PASS_STATUS, status).apply()
    }

    fun updatePhotoUrl(url: String) {
        prefs.edit().putString(KEY_PHOTO_URL, url).apply()
    }

    fun updateSemesterAndYear(semester: String, year: String) {
        prefs.edit()
            .putString(KEY_SEMESTER, semester)
            .putString(KEY_YEAR, year)
            .apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
