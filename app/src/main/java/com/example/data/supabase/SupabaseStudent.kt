package com.example.data.supabase

/**
 * نموذج بيانات الطالب في جدول student على Supabase
 * الأعمدة:
 * - id (تلقائي)
 * - full_name (اسم الطالب)
 * - national_id (الرقم القومي - فريد Unique)
 * - academic_year (الفرقة الدراسية)
 * - phone (رقم الهاتف)
 */
data class SupabaseStudent(
    val id: Long? = null,
    val fullName: String,
    val nationalId: String,
    val academicYear: String,
    val phone: String,
    val department: String? = null,
    val studentCode: String? = null,
    val createdAt: String? = null
)
