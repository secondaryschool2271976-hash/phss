package com.example.data.model

data class DoctorCourse(
    val id: String,
    val subjectName: String,
    val doctorName: String,
    val departmentCode: String,
    val yearGroup: Int,
    val doctorNationalId: String = "",
    val description: String = ""
) {
    /**
     * التحقق مما إذا كانت المحاضرة تتبع لهذه القاعة / الدكتور
     */
    fun matchesLecture(lecture: Lecture): Boolean {
        // 1. الفحص بالرقم القومي للدكتور
        if (doctorNationalId.isNotBlank() && lecture.doctorNationalId.isNotBlank()) {
            if (doctorNationalId.trim() == lecture.doctorNationalId.trim()) return true
        }
        // 2. الفحص باسم الدكتور
        if (CourseCatalog.isDoctorMatch(doctorName, lecture.doctorName)) {
            return true
        }
        // 3. الفحص باسم المادة في حال تطابقها التام
        if (subjectName.isNotBlank() && lecture.subjectName.isNotBlank()) {
            if (CourseCatalog.normalizeArabic(subjectName) == CourseCatalog.normalizeArabic(lecture.subjectName)) {
                return true
            }
        }
        return false
    }

    /**
     * التحقق من تبعية التكليف الدراسي
     */
    fun matchesAssignment(assignment: Assignment): Boolean {
        if (doctorNationalId.isNotBlank() && assignment.doctorNationalId.isNotBlank()) {
            if (doctorNationalId.trim() == assignment.doctorNationalId.trim()) return true
        }
        if (CourseCatalog.isDoctorMatch(doctorName, assignment.doctorName)) {
            return true
        }
        if (subjectName.isNotBlank() && assignment.subjectName.isNotBlank()) {
            if (CourseCatalog.normalizeArabic(subjectName) == CourseCatalog.normalizeArabic(assignment.subjectName)) {
                return true
            }
        }
        return false
    }

    /**
     * التحقق من تبعية الملف السحابي في Cloudflare R2
     */
    fun matchesCloudFile(file: AcademicCloudFile): Boolean {
        if (subjectName.isNotBlank() && file.subjectName.isNotBlank()) {
            if (CourseCatalog.normalizeArabic(subjectName) == CourseCatalog.normalizeArabic(file.subjectName)) {
                return true
            }
        }
        if (CourseCatalog.isDoctorMatch(doctorName, file.title) || CourseCatalog.isDoctorMatch(doctorName, file.subjectName)) {
            return true
        }
        return false
    }

    /**
     * التحقق من تبعية الفيديو المسجل
     */
    fun matchesVideo(video: VideoLecture): Boolean {
        if (CourseCatalog.isDoctorMatch(doctorName, video.doctorName)) return true
        if (subjectName.isNotBlank() && video.subjectName.isNotBlank()) {
            if (CourseCatalog.normalizeArabic(subjectName) == CourseCatalog.normalizeArabic(video.subjectName)) {
                return true
            }
        }
        return false
    }
}

object CourseCatalog {
    private val deletedCourseIds = mutableSetOf<String>()

    fun deleteCourse(courseId: String) {
        deletedCourseIds.add(courseId)
    }

    fun isCourseDeleted(courseId: String): Boolean = deletedCourseIds.contains(courseId)

    val defaultCourses = emptyList<DoctorCourse>()

    fun normalizeArabic(text: String): String {
        if (text.isBlank()) return ""
        var clean = text.trim()
        clean = clean.replace("\\s+".toRegex(), " ")
        clean = clean
            .replace("[أإآ]".toRegex(), "ا")
            .replace("ة\\b".toRegex(), "ه")
            .replace("ى\\b".toRegex(), "ي")
            .replace("[\\u064B-\\u065F]".toRegex(), "") // إزالة التشكيل
        return clean.lowercase()
    }

    fun normalizeArabicSubjectName(name: String): String = normalizeArabic(name)

    fun cleanDoctorName(name: String): String {
        var clean = name.trim()
        val prefixes = listOf(
            "الدكتورة", "الدكتور", "دكتورة", "دكتور",
            "أ.د.", "أ.د", "أستاذ دكتور", "استاذ دكتور", "أستاذ", "استاذ",
            "د.", "د/", "د /", "د ", "م.", "م/", "مهندس", "مهندسة"
        )
        for (p in prefixes) {
            if (clean.startsWith(p, ignoreCase = true)) {
                clean = clean.substring(p.length).trim()
            }
        }
        return normalizeArabic(clean)
    }

    /**
     * مقارنة ذكية لأسماء الدكاترة لضمان عدم تكرار القاعة مهما اختلفت صيغة الاسم
     */
    fun isDoctorMatch(doc1: String, doc2: String): Boolean {
        val c1 = cleanDoctorName(doc1)
        val c2 = cleanDoctorName(doc2)
        if (c1.isBlank() || c2.isBlank()) return false
        if (c1 == c2) return true
        if (c1.contains(c2) || c2.contains(c1)) return true
        val words1 = c1.split(" ").filter { it.isNotBlank() }
        val words2 = c2.split(" ").filter { it.isNotBlank() }
        if (words1.isNotEmpty() && words2.isNotEmpty()) {
            if (words1[0] == words2[0]) {
                if (words1.size >= 2 && words2.size >= 2) {
                    if (words1[1] == words2[1]) return true
                } else {
                    return true
                }
            }
        }
        return false
    }

    /**
     * النظام الصارم: قاعة واحدة فقط لكل دكتور!
     * جميع المحاضرات التي يرفعها الدكتور تتجمع تلقائياً في قاعته الواحدة
     * ولا يتم إطلاقاً إنشاء قاعة جديدة أو صفحة جديدة لكل محاضرة.
     */
    fun getCoursesFor(
        deptCode: String,
        year: Int,
        existingLectures: List<Lecture>,
        existingAssignments: List<Assignment>,
        approvedDoctors: List<Doctor> = emptyList(),
        currentDoctorProfile: DoctorProfile? = null
    ): List<DoctorCourse> {
        val result = mutableListOf<DoctorCourse>()
        val processedDoctorKeys = mutableSetOf<String>()

        // 1. الدكاترة المعتمدون في هذا القسم
        val relevantDoctors = approvedDoctors.filter {
            it.departmentCode.equals(deptCode, ignoreCase = true) || it.departmentCode.isBlank()
        }.toMutableList()

        // إضافة الدكتور الحالي المسجل إن وجد ولم يكن في القائمة
        if (currentDoctorProfile != null && (currentDoctorProfile.departmentCode.equals(deptCode, ignoreCase = true) || currentDoctorProfile.departmentCode.isBlank())) {
            val alreadyPresent = relevantDoctors.any {
                (it.nationalId.isNotBlank() && it.nationalId == currentDoctorProfile.nationalId) ||
                isDoctorMatch(it.name, currentDoctorProfile.name)
            }
            if (!alreadyPresent) {
                relevantDoctors.add(
                    Doctor(
                        name = currentDoctorProfile.name,
                        nationalId = currentDoctorProfile.nationalId,
                        departmentCode = currentDoctorProfile.departmentCode,
                        departmentName = currentDoctorProfile.departmentName,
                        subjectName = currentDoctorProfile.subjectName
                    )
                )
            }
        }

        // إنشاء قاعة واحدة لكل دكتور معتمد
        for (doc in relevantDoctors) {
            val key = if (doc.nationalId.isNotBlank()) doc.nationalId.trim() else cleanDoctorName(doc.name)
            if (key in processedDoctorKeys || key.isBlank()) continue
            processedDoctorKeys.add(key)

            val courseId = "doc_course_${key.hashCode()}_$year"
            if (deletedCourseIds.contains(courseId)) continue

            val formattedDocName = if (doc.name.startsWith("د.") || doc.name.startsWith("د/")) doc.name.trim() else "د. ${doc.name.replace("د. ", "").trim()}"

            // البحث عن محاضرات وتكليفات هذا الدكتور لاستنتاج أفضل مسمى لمادته
            val docLectures = existingLectures.filter { lec ->
                (doc.nationalId.isNotBlank() && lec.doctorNationalId == doc.nationalId) ||
                isDoctorMatch(doc.name, lec.doctorName)
            }
            val bestSubject = docLectures
                .map { it.subjectName.trim() }
                .firstOrNull { it.isNotBlank() && !it.startsWith("محاضرة", ignoreCase = true) && !it.startsWith("المحاضرة", ignoreCase = true) }
                ?: docLectures.firstOrNull()?.subjectName?.trim()

            val subjName = when {
                doc.subjectName.isNotBlank() && doc.subjectName != "المادة الدراسية" -> doc.subjectName.trim()
                !bestSubject.isNullOrBlank() -> bestSubject
                else -> "مقرر $formattedDocName"
            }

            result.add(
                DoctorCourse(
                    id = courseId,
                    subjectName = subjName,
                    doctorName = formattedDocName,
                    departmentCode = deptCode,
                    yearGroup = year,
                    doctorNationalId = doc.nationalId,
                    description = "قاعة دراسية موحدة لجميع المحاضرات والتكليفات"
                )
            )
        }

        // 2. تجميع المحاضرات والتكليفات المرفوعة لدكاترة آخرين لم يُسجلوا مسبقاً
        // يتم التجميع حصراً في قاعة موحدة واحدة لكل دكتور (وليس قاعة لكل محاضرة)
        val unassignedContent = (existingLectures.map { Triple(it.doctorName, it.doctorNationalId, it.subjectName) } + 
                existingAssignments.map { Triple(it.doctorName, it.doctorNationalId, it.subjectName) })

        for (item in unassignedContent) {
            val rawDocName = item.first.trim()
            val docNatId = item.second.trim()
            val subj = item.third.trim()
            if (rawDocName.isBlank() && docNatId.isBlank()) continue

            // هل المحاضرة/التكليف تتبع لقاعة دكتور موجودة بالفعل؟
            val existingCourse = result.firstOrNull { course ->
                (course.doctorNationalId.isNotBlank() && docNatId.isNotBlank() && course.doctorNationalId == docNatId) ||
                isDoctorMatch(course.doctorName, rawDocName)
            }

            if (existingCourse == null) {
                // إنشاء قاعة موحدة واحدة لهذا الدكتور تضم كل محاضراته ومواده
                val formattedDocName = if (rawDocName.startsWith("د.") || rawDocName.startsWith("د/")) rawDocName else "د. ${rawDocName.replace("د. ", "").trim()}"
                val key = if (docNatId.isNotBlank()) docNatId else cleanDoctorName(rawDocName)
                if (key !in processedDoctorKeys && key.isNotBlank()) {
                    processedDoctorKeys.add(key)
                    val courseId = "doc_group_${key.hashCode()}_$year"
                    if (!deletedCourseIds.contains(courseId)) {
                        val bestSubj = if (subj.isNotBlank() && !subj.startsWith("محاضرة", ignoreCase = true) && !subj.startsWith("المحاضرة", ignoreCase = true)) {
                            subj
                        } else "مقرر $formattedDocName"

                        result.add(
                            DoctorCourse(
                                id = courseId,
                                subjectName = bestSubj,
                                doctorName = formattedDocName,
                                departmentCode = deptCode,
                                yearGroup = year,
                                doctorNationalId = docNatId,
                                description = "قاعة دراسية موحدة لجميع المحاضرات والتكليفات"
                            )
                        )
                    }
                }
            }
        }

        return result
    }
}
