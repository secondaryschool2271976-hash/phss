package com.example.data.supabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * إرسال بيانات الطالب وحفظها في جدول student على Supabase
     * الأعمدة الجديدة: Name, Id (int8), department, acd_year, phone (int8)
     */
    suspend fun insertStudent(
        fullName: String,
        nationalId: String,
        academicYear: String,
        phone: String,
        department: String = ""
    ): Result<SupabaseStudent> = withContext(Dispatchers.IO) {
        try {
            // 1. التحقق المسبق من صحة المدخلات
            val cleanName = fullName.trim()
            val cleanId = nationalId.trim()
            val cleanYear = academicYear.trim()
            val cleanPhone = phone.trim()

            if (cleanName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("يرجى إدخال اسم الطالب بالكامل"))
            }
            if (cleanId.length != 14 || !cleanId.all { it.isDigit() }) {
                return@withContext Result.failure(IllegalArgumentException("الرقم القومي يجب أن يتكون من 14 رقماً"))
            }
            if (cleanPhone.length < 10) {
                return@withContext Result.failure(IllegalArgumentException("يرجى إدخال رقم هاتف صالح (11 رقماً)"))
            }

            val idLong = cleanId.toLongOrNull() ?: 0L
            val phoneClean = cleanPhone.filter { it.isDigit() }
            val phoneLong = phoneClean.toLongOrNull() ?: 0L

            // 2. تجهيز كائن JSON بالأعمدة المطابقة تماماً لجدول student في Supabase:
            // Name, Id (int8), department, acd_year, phone (int8)
            val jsonPayload = JSONObject().apply {
                put("Name", cleanName)
                put("Id", idLong)
                put("department", if (department.isNotBlank()) department else "نظم معلومات إدارية")
                put("acd_year", cleanYear)
                put("phone", phoneLong)
            }.toString()

            val requestBody = jsonPayload.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(SupabaseConfig.REST_STUDENT_URL)
                .post(requestBody)
                .header("apikey", SupabaseConfig.PUBLISHABLE_ANON_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                // التحقق من نجاح العملية (HTTP 200 أو 201)
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(responseBody)
                    if (jsonArray.length() > 0) {
                        val item = jsonArray.getJSONObject(0)
                        val student = parseStudentSafely(item, cleanId)
                        return@withContext Result.success(student)
                    } else {
                        val student = SupabaseStudent(
                            fullName = cleanName,
                            nationalId = cleanId,
                            academicYear = cleanYear.ifBlank { "الفرقة الأولى - نظم معلومات إدارية" },
                            phone = cleanPhone,
                            department = department
                        )
                        return@withContext Result.success(student)
                    }
                }

                // معالجة قيود سياسة RLS
                if (response.code == 401 && (responseBody.contains("row-level security", ignoreCase = true) || responseBody.contains("42501"))) {
                    return@withContext Result.failure(
                        IllegalStateException("تنبيه أمني: جدول student في Supabase مفعّل به خيار RLS. يرجى تشغيل أمر SQL المرفق في Supabase SQL Editor أو تعطيل RLS للسماح بالقيد السحابي.")
                    )
                }

                // معالجة الأخطاء - خاصة تكرار الرقم القومي
                if (response.code == 409 || responseBody.contains("23505") || responseBody.contains("duplicate key", ignoreCase = true)) {
                    return@withContext Result.failure(
                        IllegalStateException("عذراً، هذا الرقم القومي ($cleanId) مسجل مسبقاً في قاعدة بيانات سوبابيز (Supabase)!")
                    )
                }

                // خطأ آخر من Supabase
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optString("message", errJson.optString("error_description", responseBody))
                } catch (_: Exception) {
                    responseBody.ifBlank { "رمز الخطأ: ${response.code}" }
                }

                return@withContext Result.failure(Exception("خطأ من سوبابيز: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * جلب قائمة الطلاب المخزنين في جدول student على Supabase
     */
    suspend fun getStudents(): Result<List<SupabaseStudent>> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.REST_STUDENT_URL}?select=*&order=id.desc"

            val request = Request.Builder()
                .url(url)
                .get()
                .header("apikey", SupabaseConfig.PUBLISHABLE_ANON_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_ANON_KEY}")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("تعذر جلب بيانات الطلاب (${response.code})"))
                }

                val jsonArray = JSONArray(body)
                val list = mutableListOf<SupabaseStudent>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    list.add(parseStudentSafely(item))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * جلب بيانات طالب من Supabase بالرقم القومي (لتسجيل الدخول الفوري)
     */
    suspend fun getStudentByNationalId(nationalId: String): Result<SupabaseStudent?> = withContext(Dispatchers.IO) {
        try {
            val cleanId = nationalId.trim()
            val url = "${SupabaseConfig.REST_STUDENT_URL}?Id=eq.$cleanId&select=*&limit=1"
            android.util.Log.d("SupabaseManager", "Querying student by nationalId from Supabase: $url")
            val request = Request.Builder()
                .url(url)
                .get()
                .header("apikey", SupabaseConfig.PUBLISHABLE_ANON_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_ANON_KEY}")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()?.trim() ?: ""
                android.util.Log.d("SupabaseManager", "Supabase response code: ${response.code}, body: $body")

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val obj = JSONObject(body)
                        obj.optString("message", obj.optString("error_description", body))
                    } catch (_: Exception) {
                        body.ifBlank { "رمز الخطأ: ${response.code}" }
                    }
                    return@withContext Result.failure(Exception("خطأ في الاتصال بقاعدة بيانات Supabase (${response.code}): $errorMsg"))
                }

                if (body.isEmpty() || body == "[]" || body == "null") {
                    return@withContext Result.success(null)
                }

                if (body.startsWith("[")) {
                    val jsonArray = JSONArray(body)
                    if (jsonArray.length() == 0) {
                        return@withContext Result.success(null)
                    }
                    val item = jsonArray.getJSONObject(0)
                    val student = parseStudentSafely(item, cleanId)
                    return@withContext Result.success(student)
                } else if (body.startsWith("{")) {
                    val item = JSONObject(body)
                    if (item.has("code") && item.has("message") && !item.has("Id") && !item.has("national_id")) {
                        return@withContext Result.failure(Exception(item.optString("message", "خطأ استجابة من Supabase")))
                    }
                    val student = parseStudentSafely(item, cleanId)
                    return@withContext Result.success(student)
                } else {
                    return@withContext Result.success(null)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseManager", "Exception in getStudentByNationalId for id: $nationalId", e)
            Result.failure(e)
        }
    }

    /**
     * تفريغ ومعالجة بيانات الطالب بأمان تام مع التعامل مع الحقول التي قد ترجع null أو مفاتيح متنوعة
     */
    private fun parseStudentSafely(item: JSONObject, fallbackId: String = ""): SupabaseStudent {
        val rawId = when {
            item.isNull("id") -> null
            item.has("id") -> item.optLong("id")
            else -> null
        }

        val rawFullName = when {
            item.has("Name") && !item.isNull("Name") -> item.optString("Name", "")
            item.has("full_name") && !item.isNull("full_name") -> item.optString("full_name", "")
            item.has("fullName") && !item.isNull("fullName") -> item.optString("fullName", "")
            item.has("name") && !item.isNull("name") -> item.optString("name", "")
            item.has("student_name") && !item.isNull("student_name") -> item.optString("student_name", "")
            else -> ""
        }

        val rawNationalId = when {
            item.has("Id") && !item.isNull("Id") -> item.opt("Id")?.toString() ?: fallbackId
            item.has("national_id") && !item.isNull("national_id") -> item.optString("national_id", fallbackId)
            item.has("nationalId") && !item.isNull("nationalId") -> item.optString("nationalId", fallbackId)
            item.has("nid") && !item.isNull("nid") -> item.optString("nid", fallbackId)
            else -> fallbackId
        }

        val rawDepartment = when {
            item.has("department") && !item.isNull("department") -> item.optString("department", "")
            item.has("dept") && !item.isNull("dept") -> item.optString("dept", "")
            else -> ""
        }

        val rawAcademicYear = when {
            item.has("acd_year") && !item.isNull("acd_year") -> item.optString("acd_year", "")
            item.has("academic_year") && !item.isNull("academic_year") -> item.optString("academic_year", "")
            item.has("academicYear") && !item.isNull("academicYear") -> item.optString("academicYear", "")
            item.has("year") && !item.isNull("year") -> item.optString("year", "")
            item.has("year_group") && !item.isNull("year_group") -> item.optString("year_group", "")
            else -> ""
        }

        val rawPhone = when {
            item.has("phone") && !item.isNull("phone") -> {
                val p = item.opt("phone")?.toString()?.trim() ?: ""
                // إذا كان الرقم مسجلاً كرقم int8 ومحذوفاً منه الصفر المبدئي المصري (10 أرقام تبدأ بـ 1)
                if (p.length == 10 && (p.startsWith("10") || p.startsWith("11") || p.startsWith("12") || p.startsWith("15"))) {
                    "0$p"
                } else {
                    p
                }
            }
            item.has("phoneNumber") && !item.isNull("phoneNumber") -> item.optString("phoneNumber", "")
            item.has("mobile") && !item.isNull("mobile") -> item.optString("mobile", "")
            else -> ""
        }

        val rawStudentCode = when {
            item.has("student_code") && !item.isNull("student_code") -> item.optString("student_code", "")
            item.has("studentCode") && !item.isNull("studentCode") -> item.optString("studentCode", "")
            item.has("code") && !item.isNull("code") -> item.optString("code", "")
            else -> null
        }

        val rawCreatedAt = if (item.isNull("created_at")) null else item.optString("created_at")

        val safeFullName = if (rawFullName == "null" || rawFullName.isBlank()) "طالب مقيد" else rawFullName.trim()
        val safeNationalId = if (rawNationalId == "null" || rawNationalId.isBlank()) fallbackId else rawNationalId.trim()
        val safeAcademicYear = if (rawAcademicYear == "null" || rawAcademicYear.isBlank()) "الفرقة الأولى - نظم معلومات إدارية" else rawAcademicYear.trim()
        val safePhone = if (rawPhone == "null") "" else rawPhone.trim()
        val safeDepartment = if (rawDepartment == "null" || rawDepartment.isBlank()) null else rawDepartment.trim()
        val safeStudentCode = if (rawStudentCode == "null" || rawStudentCode.isNullOrBlank()) null else rawStudentCode.trim()
        val safeCreatedAt = if (rawCreatedAt == "null") null else rawCreatedAt

        return SupabaseStudent(
            id = rawId,
            fullName = safeFullName,
            nationalId = safeNationalId,
            academicYear = safeAcademicYear,
            phone = safePhone,
            department = safeDepartment,
            studentCode = safeStudentCode,
            createdAt = safeCreatedAt
        )
    }

    /**
     * التحقق مما إذا كان الرقم القومي مسجلاً مسبقاً على السحابة
     */
    suspend fun checkNationalIdExists(nationalId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanId = nationalId.trim()
            val url = "${SupabaseConfig.REST_STUDENT_URL}?Id=eq.$cleanId&select=id"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("apikey", SupabaseConfig.PUBLISHABLE_ANON_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_ANON_KEY}")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(body)
                    Result.success(jsonArray.length() > 0)
                } else {
                    Result.failure(Exception("خطأ استعلام: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * حذف طالب نهائياً من قاعدة بيانات Supabase برقم المعرف id عبر PostgREST DELETE API
     */
    suspend fun deleteStudent(id: Long, apiKey: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val effectiveKey = apiKey ?: SupabaseConfig.getActiveApiKey()
            val url = "${SupabaseConfig.REST_STUDENT_URL}?id=eq.$id"
            val request = Request.Builder()
                .url(url)
                .delete()
                .header("apikey", effectiveKey)
                .header("Authorization", "Bearer $effectiveKey")
                .header("Prefer", "return=representation")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()?.trim() ?: ""
                if (response.isSuccessful) {
                    if (body == "[]" && effectiveKey == SupabaseConfig.PUBLISHABLE_ANON_KEY) {
                        Result.failure(Exception("تنبيه أمني: لم يتم حذف السجل في Supabase بسبب قيود RLS. يرجى تفعيل أمر SQL أو إدخال مفتاح Service Role"))
                    } else {
                        Result.success(true)
                    }
                } else {
                    Result.failure(Exception("فشل الحذف من Supabase (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * حذف طالب برقم الرقم القومي في حال عدم توفر معرف id
     */
    suspend fun deleteStudentByNationalId(nationalId: String, apiKey: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanId = nationalId.trim()
            val effectiveKey = apiKey ?: SupabaseConfig.getActiveApiKey()
            val encodedId = java.net.URLEncoder.encode(cleanId, "UTF-8")
            val url = "${SupabaseConfig.REST_STUDENT_URL}?Id=eq.$encodedId"
            val request = Request.Builder()
                .url(url)
                .delete()
                .header("apikey", effectiveKey)
                .header("Authorization", "Bearer $effectiveKey")
                .header("Prefer", "return=representation")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()?.trim() ?: ""
                if (response.isSuccessful) {
                    if (body == "[]" && effectiveKey == SupabaseConfig.PUBLISHABLE_ANON_KEY) {
                        Result.failure(Exception("تنبيه أمني: لم يتم حذف السجل في Supabase بسبب قيود RLS. يرجى تفعيل أمر SQL أو إدخال مفتاح Service Role"))
                    } else {
                        Result.success(true)
                    }
                } else {
                    Result.failure(Exception("فشل الحذف من Supabase (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * حذف طالب بالاسم الكامل في حال عدم تطابق الرقم القومي
     */
    suspend fun deleteStudentByName(fullName: String, apiKey: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanName = fullName.trim()
            if (cleanName.isBlank()) return@withContext Result.success(true)
            val effectiveKey = apiKey ?: SupabaseConfig.getActiveApiKey()
            val encodedName = java.net.URLEncoder.encode(cleanName, "UTF-8")
            val url = "${SupabaseConfig.REST_STUDENT_URL}?Name=eq.$encodedName"
            val request = Request.Builder()
                .url(url)
                .delete()
                .header("apikey", effectiveKey)
                .header("Authorization", "Bearer $effectiveKey")
                .header("Prefer", "return=representation")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()?.trim() ?: ""
                if (response.isSuccessful) {
                    if (body == "[]" && effectiveKey == SupabaseConfig.PUBLISHABLE_ANON_KEY) {
                        Result.failure(Exception("تنبيه أمني: لم يتم حذف السجل في Supabase بسبب قيود RLS. يرجى تفعيل أمر SQL أو إدخال مفتاح Service Role"))
                    } else {
                        Result.success(true)
                    }
                } else {
                    Result.failure(Exception("فشل الحذف من Supabase (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * حذف جميع الطلاب دفعة واحدة من Supabase (ميزة خاصة بالمسؤول)
     */
    suspend fun deleteAllStudents(apiKey: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val studentsResult = getStudents()
            if (studentsResult.isFailure) {
                return@withContext Result.failure(studentsResult.exceptionOrNull() ?: Exception("فشل جلب قائمة الطلاب"))
            }
            val list = studentsResult.getOrNull() ?: emptyList()
            var deletedCount = 0
            for (st in list) {
                val res = if (st.id != null) deleteStudent(st.id, apiKey) else deleteStudentByNationalId(st.nationalId, apiKey)
                if (res.isSuccess) {
                    deletedCount++
                }
            }
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

