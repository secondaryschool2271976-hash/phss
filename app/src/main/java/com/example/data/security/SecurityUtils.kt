package com.example.data.security

import java.security.MessageDigest

data class NationalIdInfo(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val birthDate: String? = null,
    val governorate: String? = null,
    val gender: String? = null
)

object SecurityUtils {

    private val governorateMap = mapOf(
        "01" to "القاهرة",
        "02" to "الإسكندرية",
        "03" to "بورسعيد",
        "04" to "السويس",
        "11" to "دمياط",
        "12" to "الدقهلية",
        "13" to "الشرقية",
        "14" to "القليوبية",
        "15" to "كفر الشيخ",
        "16" to "الغربية",
        "17" to "المنوفية",
        "18" to "البحيرة",
        "19" to "الإسماعيلية",
        "21" to "الجيزة",
        "22" to "بني سويف",
        "23" to "الفيوم",
        "24" to "المنيا",
        "25" to "أسيوط",
        "26" to "سوهاج",
        "27" to "قنا",
        "28" to "أسوان",
        "29" to "الأقصر",
        "31" to "البحر الأحمر",
        "32" to "الوادي الجديد",
        "33" to "مطروح",
        "34" to "شمال سيناء",
        "35" to "جنوب سيناء",
        "88" to "خارج الجمهورية"
    )

    fun hashNationalId(nationalId: String): String {
        val cleanId = nationalId.trim()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(cleanId.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun maskNationalId(nationalId: String): String {
        val clean = nationalId.trim()
        if (clean.length < 14) return "**************"
        return "${clean.take(7)}******${clean.takeLast(1)}"
    }

    fun validateEgyptianNationalId(nationalId: String): NationalIdInfo {
        val clean = nationalId.trim()
        if (clean.length != 14) {
            return NationalIdInfo(
                isValid = false,
                errorMessage = "الرقم القومي يجب أن يتكون من 14 رقماً بالضبط (المُدخل: ${clean.length})"
            )
        }

        if (!clean.all { it.isDigit() }) {
            return NationalIdInfo(
                isValid = false,
                errorMessage = "الرقم القومي يجب أن يحتوي على أرقام فقط"
            )
        }

        val centuryChar = clean[0]
        val century = when (centuryChar) {
            '2' -> 1900
            '3' -> 2000
            else -> {
                return NationalIdInfo(
                    isValid = false,
                    errorMessage = "الرقم الأول غير صحيح (يجب أن يبدأ بـ 2 لمواليد القرن الماضي أو 3 لمواليد القرن الحالي)"
                )
            }
        }

        val yearDigits = clean.substring(1, 3).toIntOrNull() ?: return NationalIdInfo(isValid = false, errorMessage = "سنة الميلاد غير صحيحة")
        val month = clean.substring(3, 5).toIntOrNull() ?: return NationalIdInfo(isValid = false, errorMessage = "شهر الميلاد غير صحيح")
        val day = clean.substring(5, 7).toIntOrNull() ?: return NationalIdInfo(isValid = false, errorMessage = "يوم الميلاد غير صحيح")

        if (month !in 1..12) {
            return NationalIdInfo(isValid = false, errorMessage = "شهر الميلاد غير صالح ($month)")
        }

        if (day !in 1..31) {
            return NationalIdInfo(isValid = false, errorMessage = "يوم الميلاد غير صالح ($day)")
        }

        val fullYear = century + yearDigits
        val govCode = clean.substring(7, 9)
        val govName = governorateMap[govCode] ?: "محافظة أخرى (${govCode})"

        val sequenceNum = clean.substring(9, 13).toIntOrNull() ?: 0
        val gender = if (sequenceNum % 2 != 0) "ذكر" else "أنثى"

        val formattedDate = "%02d/%02d/%04d".format(day, month, fullYear)

        return NationalIdInfo(
            isValid = true,
            birthDate = formattedDate,
            governorate = govName,
            gender = gender
        )
    }

    fun validateArabicNamePart(part: String, partNameArabic: String): String? {
        val clean = part.trim()
        if (clean.isEmpty()) {
            return "يرجى إدخال $partNameArabic"
        }
        if (clean.length < 2) {
            return "$partNameArabic يجب ألا يقل عن حرفين"
        }
        val arabicRegex = Regex("^[\\u0621-\\u064A\\s]+$")
        if (!arabicRegex.matches(clean)) {
            return "$partNameArabic يجب أن يحتوي على حروف عربية فقط"
        }
        return null
    }

    /**
     * التحقق من أن اسم الطالب رباعي على الأقل وفقاً للمتطلبات الأكاديمية
     */
    fun validateQuadrupleName(fullName: String): String? {
        val clean = fullName.trim()
        if (clean.isBlank()) {
            return "يرجى كتابة اسم الطالب بالكامل"
        }
        val parts = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 4) {
            return "يجب أن يكون اسم الطالب رباعياً على الأقل (الاسم الأول واسم الأب والجد واللقب/العائلة) - المُدخل: ${parts.size} أجزاء"
        }
        for ((index, part) in parts.withIndex()) {
            val partTitle = when (index) {
                0 -> "الاسم الأول"
                1 -> "اسم الأب"
                2 -> "اسم الجد"
                3 -> "اللقب أو اسم العائلة"
                else -> "الجزء ${index + 1}"
            }
            if (part.length < 2) {
                return "$partTitle ($part) قصير جداً، يجب ألا يقل عن حرفين"
            }
        }
        return null
    }

    fun generateStudentCode(departmentCode: String, yearNumber: Int, studentCount: Int): String {
        // Suggested format: Department Code + Year + 4 digits (e.g. MIS-3-0451)
        val seed = (studentCount + 1) * 37 + (System.currentTimeMillis() % 899).toInt()
        val serial = (1000 + (seed % 9000)).toString()
        return "$departmentCode-$yearNumber-$serial"
    }

    fun validateEgyptianPhone(phone: String): String? {
        val clean = phone.trim()
        if (clean.length != 11) {
            return "رقم الهاتف يجب أن يتكون من 11 رقماً بالضبط (المُدخل: ${clean.length})"
        }
        if (!clean.all { it.isDigit() }) {
            return "رقم الهاتف يجب أن يحتوي على أرقام فقط"
        }
        val prefix = clean.take(3)
        if (prefix !in listOf("010", "011", "012", "015")) {
            return "رقم الهاتف يجب أن يبدأ بأحد الشبكات المصرية: 010 أو 011 أو 012 أو 015"
        }
        return null
    }

    fun isNetworkAvailable(context: android.content.Context): Boolean {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

