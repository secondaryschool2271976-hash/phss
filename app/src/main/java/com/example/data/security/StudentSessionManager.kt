package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Student
import com.example.data.model.DoctorProfile

/**
 * مدير الجلسة المحلية للطالب (Local Session Manager)
 * يحفظ بيانات الطالب المسجل محلياً في الهاتف فقط عبر SharedPreferences
 * بحيث يفتح الهاتف مباشرة على كارنيه هذا الطالب دون مشاركة بياناته مع أي مستخدم آخر على جهاز مختلف.
 * يدعم أيضاً حفظ بيانات الدكاترة والمسؤولين لضمان تسجيل الدخول التلقائي مرة واحدة فقط.
 */
object StudentSessionManager {

    private const val PREF_NAME = "pharaohs_student_local_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_CURRENT_ROLE = "current_role" // STUDENT, DOCTOR, ADMIN

    // Student Keys
    private const val KEY_STUDENT_CODE = "student_code"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_FATHER_NAME = "father_name"
    private const val KEY_GRAND_FATHER_NAME = "grand_father_name"
    private const val KEY_FAMILY_NAME = "family_name"
    private const val KEY_NATIONAL_ID_HASH = "national_id_hash"
    private const val KEY_NATIONAL_ID_MASKED = "national_id_masked"
    private const val KEY_RAW_NATIONAL_ID = "raw_national_id"
    private const val KEY_DEPARTMENT_CODE = "department_code"
    private const val KEY_YEAR_GROUP = "year_group"
    private const val KEY_ACADEMIC_YEAR = "academic_year"
    private const val KEY_PHONE = "phone"
    private const val KEY_REGISTRATION_DATE = "registration_date"

    // Doctor Keys
    private const val KEY_DOCTOR_NAME = "doctor_name"
    private const val KEY_DOCTOR_NATIONAL_ID = "doctor_national_id"
    private const val KEY_DOCTOR_DEPT_CODE = "doctor_dept_code"
    private const val KEY_DOCTOR_DEPT_NAME = "doctor_dept_name"
    private const val KEY_DOCTOR_SUBJECT_NAME = "doctor_subject_name"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * حفظ جلسة الطالب محلياً على هذا الجهاز فقط
     */
    fun saveStudentSession(context: Context, student: Student) {
        try {
            getPrefs(context).edit().apply {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putString(KEY_CURRENT_ROLE, "STUDENT")
                putString(KEY_STUDENT_CODE, student.studentCode)
                putString(KEY_FULL_NAME, student.fullName)
                putString(KEY_FIRST_NAME, student.firstName)
                putString(KEY_FATHER_NAME, student.fatherName)
                putString(KEY_GRAND_FATHER_NAME, student.grandFatherName)
                putString(KEY_FAMILY_NAME, student.familyName)
                putString(KEY_NATIONAL_ID_HASH, student.nationalIdHash)
                putString(KEY_NATIONAL_ID_MASKED, student.nationalIdMasked)
                putString(KEY_RAW_NATIONAL_ID, student.rawNationalId)
                putString(KEY_DEPARTMENT_CODE, student.departmentCode)
                putInt(KEY_YEAR_GROUP, student.yearGroup)
                putString(KEY_ACADEMIC_YEAR, student.academicYear)
                putString(KEY_PHONE, student.phone)
                putLong(KEY_REGISTRATION_DATE, student.registrationDate)
                apply()
            }
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    /**
     * حفظ جلسة عضو هيئة التدريس (الدكتور)
     */
    fun saveDoctorSession(context: Context, name: String, nationalId: String, deptCode: String, deptName: String, subjectName: String = "المادة الدراسية") {
        try {
            getPrefs(context).edit().apply {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putString(KEY_CURRENT_ROLE, "DOCTOR")
                putString(KEY_DOCTOR_NAME, name)
                putString(KEY_DOCTOR_NATIONAL_ID, nationalId)
                putString(KEY_DOCTOR_DEPT_CODE, deptCode)
                putString(KEY_DOCTOR_DEPT_NAME, deptName)
                putString(KEY_DOCTOR_SUBJECT_NAME, subjectName)
                apply()
            }
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    /**
     * حفظ جلسة مسؤول النظام (Admin)
     */
    fun saveAdminSession(context: Context) {
        try {
            getPrefs(context).edit().apply {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putString(KEY_CURRENT_ROLE, "ADMIN")
                apply()
            }
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    /**
     * جلب نوع الجلسة النشطة حالياً (STUDENT, DOCTOR, ADMIN) إن وجدت
     */
    fun getSavedRole(context: Context): String? {
        val prefs = getPrefs(context)
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return null
        }
        return prefs.getString(KEY_CURRENT_ROLE, "STUDENT")
    }

    /**
     * جلب بيانات الدكتور المخزن محلياً كجلسة مستمرة
     */
    fun getSavedDoctorSession(context: Context): DoctorProfile? {
        return try {
            val prefs = getPrefs(context)
            if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
                return null
            }
            if (prefs.getString(KEY_CURRENT_ROLE, "") != "DOCTOR") {
                return null
            }
            val name = prefs.getString(KEY_DOCTOR_NAME, null) ?: return null
            val nationalId = prefs.getString(KEY_DOCTOR_NATIONAL_ID, "") ?: ""
            val deptCode = prefs.getString(KEY_DOCTOR_DEPT_CODE, "MIS") ?: "MIS"
            val deptName = prefs.getString(KEY_DOCTOR_DEPT_NAME, "") ?: ""
            val subjectName = prefs.getString(KEY_DOCTOR_SUBJECT_NAME, "المادة الدراسية") ?: "المادة الدراسية"
            DoctorProfile(name, nationalId, deptCode, deptName, subjectName)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * جلب بيانات الطالب المخزن محلياً كجلسة مستمرة
     */
    fun getSavedStudentSession(context: Context): Student? {
        return try {
            val prefs = getPrefs(context)
            if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
                return null
            }
            if (prefs.getString(KEY_CURRENT_ROLE, "STUDENT") != "STUDENT") {
                return null
            }

            val code = prefs.getString(KEY_STUDENT_CODE, null) ?: return null
            val fullName = prefs.getString(KEY_FULL_NAME, "طالب") ?: "طالب"
            val rawId = prefs.getString(KEY_RAW_NATIONAL_ID, "") ?: ""
            val maskedId = prefs.getString(KEY_NATIONAL_ID_MASKED, "") ?: SecurityUtils.maskNationalId(rawId)
            val hashId = prefs.getString(KEY_NATIONAL_ID_HASH, "") ?: SecurityUtils.hashNationalId(rawId)
            val deptCode = prefs.getString(KEY_DEPARTMENT_CODE, "MIS") ?: "MIS"
            val yearGroup = prefs.getInt(KEY_YEAR_GROUP, 1).coerceIn(1, 4)
            val academicYear = prefs.getString(KEY_ACADEMIC_YEAR, "الفرقة الأولى - نظم معلومات إدارية") ?: "الفرقة الأولى"
            val phone = prefs.getString(KEY_PHONE, "") ?: ""
            val fName = prefs.getString(KEY_FIRST_NAME, fullName.split(" ").firstOrNull() ?: "طالب") ?: "طالب"
            val faName = prefs.getString(KEY_FATHER_NAME, "") ?: ""
            val gName = prefs.getString(KEY_GRAND_FATHER_NAME, "") ?: ""
            val famName = prefs.getString(KEY_FAMILY_NAME, "") ?: ""
            val regDate = prefs.getLong(KEY_REGISTRATION_DATE, System.currentTimeMillis())

            Student(
                studentCode = code,
                fullName = fullName,
                firstName = fName,
                fatherName = faName,
                grandFatherName = gName,
                familyName = famName,
                nationalIdHash = hashId,
                nationalIdMasked = maskedId,
                departmentCode = deptCode,
                yearGroup = yearGroup,
                registrationDate = regDate,
                academicYear = academicYear,
                phone = phone,
                rawNationalId = rawId
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * تسجيل خروج وحذف جميع بيانات الجلسة المحلية من الهاتف
     */
    fun clearStudentSession(context: Context) {
        try {
            getPrefs(context).edit().clear().apply()
        } catch (_: Exception) {
            // Ignore
        }
    }

    /**
     * التحقق مما إذا كانت هناك جلسة نشطة
     */
    fun hasActiveStudentSession(context: Context): Boolean {
        return try {
            getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
        } catch (_: Exception) {
            false
        }
    }

    // ==========================================
    // Supabase Admin Secret / Service Role Key Storage
    // ==========================================
    private const val KEY_SUPABASE_SERVICE_ROLE_KEY = "supabase_service_role_key"

    fun saveSupabaseSecretKey(context: Context, key: String) {
        try {
            getPrefs(context).edit().putString(KEY_SUPABASE_SERVICE_ROLE_KEY, key.trim()).apply()
        } catch (_: Exception) {}
    }

    fun getSupabaseSecretKey(context: Context): String? {
        return try {
            val key = getPrefs(context).getString(KEY_SUPABASE_SERVICE_ROLE_KEY, null)
            if (key.isNullOrBlank()) null else key.trim()
        } catch (_: Exception) {
            null
        }
    }

    fun clearSupabaseSecretKey(context: Context) {
        try {
            getPrefs(context).edit().remove(KEY_SUPABASE_SERVICE_ROLE_KEY).apply()
        } catch (_: Exception) {}
    }
}

/**
 * مدير شامل لإدارة قائمة الطلاب المحذوفين لضمان عدم ظهورهم أو مزامنتهم مجدداً نهائياً
 */
object DeletedStudentsManager {
    private const val PREFS_NAME = "deleted_students_prefs"
    private const val KEY_DELETED_NIDS = "deleted_national_ids"
    private const val KEY_DELETED_IDS = "deleted_student_ids"
    private const val KEY_DELETED_CODES = "deleted_student_codes"
    private const val KEY_DELETED_NAMES = "deleted_student_names"

    fun addDeletedStudent(context: Context, nationalId: String) {
        markStudentDeleted(context, nationalId = nationalId)
    }

    fun markStudentDeleted(
        context: Context,
        nationalId: String = "",
        studentId: Long? = null,
        studentCode: String? = null,
        fullName: String? = null
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            if (nationalId.isNotBlank()) {
                val cleanNid = nationalId.trim()
                val nids = HashSet(prefs.getStringSet(KEY_DELETED_NIDS, emptySet()) ?: emptySet())
                nids.add(cleanNid)
                editor.putStringSet(KEY_DELETED_NIDS, nids)

                val strBackup = prefs.getString("backup_nids", "") ?: ""
                editor.putString("backup_nids", "$strBackup,$cleanNid")
            }

            if (studentId != null) {
                val idStr = studentId.toString()
                val ids = HashSet(prefs.getStringSet(KEY_DELETED_IDS, emptySet()) ?: emptySet())
                ids.add(idStr)
                editor.putStringSet(KEY_DELETED_IDS, ids)

                val strBackup = prefs.getString("backup_ids", "") ?: ""
                editor.putString("backup_ids", "$strBackup,$idStr")
            }

            if (!studentCode.isNullOrBlank()) {
                val cleanCode = studentCode.trim().lowercase()
                val codes = HashSet(prefs.getStringSet(KEY_DELETED_CODES, emptySet()) ?: emptySet())
                codes.add(cleanCode)
                editor.putStringSet(KEY_DELETED_CODES, codes)

                val strBackup = prefs.getString("backup_codes", "") ?: ""
                editor.putString("backup_codes", "$strBackup,$cleanCode")
            }

            if (!fullName.isNullOrBlank()) {
                val cleanName = com.example.data.model.CourseCatalog.normalizeArabic(fullName)
                val names = HashSet(prefs.getStringSet(KEY_DELETED_NAMES, emptySet()) ?: emptySet())
                names.add(cleanName)
                editor.putStringSet(KEY_DELETED_NAMES, names)

                val strBackup = prefs.getString("backup_names", "") ?: ""
                editor.putString("backup_names", "$strBackup,$cleanName")
            }

            editor.commit() // Commit immediately to disk!
        } catch (_: Exception) {}
    }

    fun isDeleted(
        context: Context,
        nationalId: String = "",
        studentId: Long? = null,
        studentCode: String? = null,
        fullName: String? = null
    ): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            if (nationalId.isNotBlank()) {
                val cleanNid = nationalId.trim()
                val nids = prefs.getStringSet(KEY_DELETED_NIDS, emptySet()) ?: emptySet()
                if (nids.contains(cleanNid)) return true
                val backupNids = prefs.getString("backup_nids", "") ?: ""
                if (backupNids.contains(cleanNid)) return true
            }

            if (studentId != null) {
                val idStr = studentId.toString()
                val ids = prefs.getStringSet(KEY_DELETED_IDS, emptySet()) ?: emptySet()
                if (ids.contains(idStr)) return true
                val backupIds = prefs.getString("backup_ids", "") ?: ""
                if (backupIds.contains(idStr)) return true
            }

            if (!studentCode.isNullOrBlank()) {
                val cleanCode = studentCode.trim().lowercase()
                val codes = prefs.getStringSet(KEY_DELETED_CODES, emptySet()) ?: emptySet()
                if (codes.contains(cleanCode)) return true
                val backupCodes = prefs.getString("backup_codes", "") ?: ""
                if (backupCodes.contains(cleanCode)) return true
            }

            if (!fullName.isNullOrBlank()) {
                val normName = com.example.data.model.CourseCatalog.normalizeArabic(fullName)
                val names = prefs.getStringSet(KEY_DELETED_NAMES, emptySet()) ?: emptySet()
                if (names.contains(normName)) return true
                val backupNames = prefs.getString("backup_names", "") ?: ""
                if (backupNames.contains(normName)) return true
            }

            false
        } catch (_: Exception) {
            false
        }
    }

    fun clearAll(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (_: Exception) {}
    }
}
