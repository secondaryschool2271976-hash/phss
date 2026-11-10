package com.example.data.supabase

/**
 * إعدادات الاتصال بقاعدة بيانات Supabase السحابية
 */
object SupabaseConfig {
    const val PROJECT_URL = "https://nevabpquhmgsbhhygybn.supabase.co"
    const val PUBLISHABLE_ANON_KEY = "sb_publishable_36VwC0IlbeW1MUDkKLwDtQ_WJgwjIfk"
    const val STUDENT_TABLE = "student"
    const val REST_STUDENT_URL = "$PROJECT_URL/rest/v1/$STUDENT_TABLE"

    // مفتاح الصلاحيات الكاملة (Service Role / Secret Key) إن وُجد في الذاكرة أو الإعدادات
    @Volatile
    var customServiceRoleKey: String? = null

    /**
     * كود SQL لتفعيل الصلاحيات الكاملة والحذف في Supabase SQL Editor
     */
    const val SUPABASE_ENABLE_DELETE_SQL = 
        "-- تفعيل الصلاحيات الكاملة وإلغاء قيود RLS لجدول student:\n" +
        "ALTER TABLE public.student DISABLE ROW LEVEL SECURITY;\n\n" +
        "-- أو إضافة سياسة شاملة تسمح بالإدخال والقراءة والحذف والتعديل:\n" +
        "CREATE POLICY IF NOT EXISTS \"Allow full access\" ON public.student FOR ALL USING (true) WITH CHECK (true);"

    fun getActiveApiKey(context: android.content.Context? = null): String {
        return customServiceRoleKey
            ?: (context?.let { com.example.data.security.StudentSessionManager.getSupabaseSecretKey(it) })
            ?: PUBLISHABLE_ANON_KEY
    }

    fun hasAdminSecretKey(context: android.content.Context? = null): Boolean {
        return !customServiceRoleKey.isNullOrBlank() ||
                (context?.let { !com.example.data.security.StudentSessionManager.getSupabaseSecretKey(it).isNullOrBlank() } == true)
    }
}
