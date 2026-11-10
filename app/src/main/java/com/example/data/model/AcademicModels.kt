package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Department(
    val code: String,
    val arabicName: String,
    val englishName: String,
    val description: String
) {
    MIS(
        code = "MIS",
        arabicName = "نظم معلومات إدارية",
        englishName = "Management Information Systems",
        description = "دمج بين تكنولوجيا المعلومات والحلول الإدارية وذكاء الأعمال"
    ),
    BUSINESS_ADMIN(
        code = "BA",
        arabicName = "إدارة أعمال",
        englishName = "Business Administration",
        description = "القيادة الاستراتيجية، التسويق، الإدارة المالية، وريادة الأعمال"
    ),
    COMPUTER_SCIENCE(
        code = "CS",
        arabicName = "علوم حاسب",
        englishName = "Computer Science",
        description = "هندسة البرمجيات، هياكل البيانات، الذكاء الاصطناعي، والشبكات"
    );

    companion object {
        fun fromCode(code: String): Department {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: MIS
        }
    }
}

enum class YearGroup(val number: Int, val arabicName: String, val levelDescription: String) {
    YEAR_1(1, "الفرقة الأولى", "المستوى التأسيسي والمفاهيم العامة"),
    YEAR_2(2, "الفرقة الثانية", "المستوى المتوسط والتطبيقات المتخصصة"),
    YEAR_3(3, "الفرقة الثالثة", "المستوى المتقدم والأنظمة المتكاملة"),
    YEAR_4(4, "الفرقة الرابعة", "مستوى التخرج ومشاريع التخرج والجاهزية لسوق العمل");

    companion object {
        fun fromNumber(num: Int): YearGroup {
            return entries.firstOrNull { it.number == num } ?: YEAR_1
        }
    }
}

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["nationalIdHash"], unique = true)
    ]
)
data class Student(
    @PrimaryKey
    val studentCode: String, // e.g. "MIS-3-0451"
    val fullName: String,
    val firstName: String,
    val fatherName: String,
    val grandFatherName: String,
    val familyName: String,
    val nationalIdHash: String, // SHA-256 for secure duplicate prevention
    val nationalIdMasked: String, // e.g. "3020512******4"
    val departmentCode: String,
    val yearGroup: Int,
    val registrationDate: Long = System.currentTimeMillis(),
    val academicYear: String = "2025 / 2026",
    val phone: String = "",
    val rawNationalId: String = ""
)

@Entity(tableName = "lectures")
data class Lecture(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val departmentCode: String,
    val yearGroup: Int,
    val subjectName: String,
    val title: String,
    val doctorName: String,
    val lectureNumber: Int,
    val date: String,
    val summary: String,
    val slidesCount: Int,
    val slideDetails: String, // JSON or descriptive list of slides content
    val fileType: String = "PDF / شرائح عرض",
    val fileUrl: String = "",
    val fileName: String = "",
    val fileSize: String = "",
    val allowSubmissions: Boolean = false, // مفتاح تحكم: السماح للطلاب بتسليم ملفات/واجبات لهذا الدرس
    val doctorNationalId: String = "" // الرقم القومي للدكتور لعزل المحتوى في لوحة التحكم
)

@Entity(tableName = "video_lectures")
data class VideoLecture(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val departmentCode: String,
    val yearGroup: Int,
    val subjectName: String,
    val title: String,
    val doctorName: String,
    val durationMinutes: Int,
    val videoUrl: String,
    val description: String,
    val addedDate: String
)

@Entity(tableName = "exam_schedules")
data class ExamSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val departmentCode: String,
    val yearGroup: Int,
    val subjectName: String,
    val examDate: String, // YYYY-MM-DD
    val startTime: String, // e.g. "09:00 ص"
    val durationHours: Int = 2,
    val hall: String, // e.g. "مدرج الفراعنة رقم (1)"
    val notes: String = "الالتزام بالزي الرسمي وإحضار الكارنيه الجامعي والأدوات الشخصية"
)

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val departmentCode: String,
    val yearGroup: Int,
    val subjectName: String,
    val title: String,
    val doctorName: String,
    val description: String,
    val dueDate: String,
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val doctorNationalId: String = ""
)

@Entity(tableName = "assignment_submissions")
data class AssignmentSubmission(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assignmentId: Long,
    val studentName: String,
    val studentNationalId: String,
    val studentCode: String,
    val fileUrl: String,
    val fileName: String,
    val formattedSize: String = "",
    val submissionDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class DoctorProfile(
    val name: String,
    val nationalId: String,
    val departmentCode: String,
    val departmentName: String,
    val subjectName: String = "المادة الدراسية"
)

@Entity(tableName = "doctors")
data class Doctor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val nationalId: String,
    val departmentCode: String,
    val departmentName: String,
    val subjectName: String = "المادة الدراسية"
)


@Entity(tableName = "academic_cloud_files")
data class AcademicCloudFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subjectName: String,
    val departmentCode: String,
    val yearGroup: Int,
    val category: String, // "محاضرة", "واجب", "شيت عملي", "ملخص"
    val originalFileName: String,
    val uniqueFileName: String,
    val fileExtension: String,
    val fileSize: Long,
    val formattedSize: String,
    val downloadUrl: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

/**
 * نموذج المادة الدراسية في نظام الساعات المعتمدة
 */
@Entity(tableName = "course_subjects")
data class CourseSubject(
    @PrimaryKey
    val courseCode: String, // e.g. "MIS201"
    val courseName: String, // e.g. "اللوجستيات و سلاسل الإمداد"
    val departmentCode: String, // e.g. "MIS"
    val yearGroup: Int, // e.g. 2
    val creditHours: Int = 3, // عدد الساعات المعتمدة (غالباً 3 أو 2 أو 4)
    val semester: Int = 1, // الفصل الدراسي (1 أو 2)
    val isMandatory: Boolean = true, // مادة إجبارية أو اختيارية
    val prerequisiteCode: String? = null, // المتطلب السابق إن وجد
    val description: String = ""
)

/**
 * تسجيل الطالب للمقررات الدراسية في نظام الساعات المعتمدة
 */
@Entity(
    tableName = "student_course_registrations",
    primaryKeys = ["studentNationalId", "courseCode"]
)
data class StudentCourseRegistration(
    val studentNationalId: String,
    val studentCode: String,
    val studentName: String,
    val courseCode: String,
    val courseName: String,
    val creditHours: Int,
    val departmentCode: String,
    val yearGroup: Int,
    val semester: Int,
    val registrationDate: Long = System.currentTimeMillis(),
    val academicTerm: String = "الفصل الدراسي الأول 2025 / 2026"
)

/**
 * الكتالوج الافتراضي لمقررات نظام الساعات المعتمدة (الحد الأقصى للتسجيل: 18 ساعة معتمدة)
 */
object CreditHourCatalog {
    const val MAX_CREDIT_HOURS_PER_SEMESTER = 18
    const val MIN_CREDIT_HOURS_PER_SEMESTER = 12

    /**
     * مواد الفرقة الثانية المعتمدة
     * 1/ اللوجستيات و سلاسل الإمداد
     * 2/ محاسبه التكاليف
     * 3/ قانون تجاري
     * 4/ مبادئ اقتصاد جزئي
     * 5/ مبادئ تسويق
     * بالإضافة لمقررات تكميلية للوصول لسقف الـ 18 ساعة
     */
    val defaultYear2Courses = listOf(
        CourseSubject(
            courseCode = "LOG-201",
            courseName = "اللوجستيات و سلاسل الإمداد",
            departmentCode = "MIS",
            yearGroup = 2,
            creditHours = 3,
            semester = 1,
            isMandatory = true,
            description = "مفاهيم إدارة سلاسل الإمداد، التوزيع، وإدارة المخزون والعمليات اللوجستية الحديثة"
        ),
        CourseSubject(
            courseCode = "ACC-202",
            courseName = "محاسبه التكاليف",
            departmentCode = "MIS",
            yearGroup = 2,
            creditHours = 3,
            semester = 1,
            isMandatory = true,
            description = "تحليل عناصر التكاليف، قياس تكلفة الإنتاج وتوزيع الأعباء غير المباشرة لاتخاذ القرارات"
        ),
        CourseSubject(
            courseCode = "LAW-203",
            courseName = "قانون تجاري",
            departmentCode = "MIS",
            yearGroup = 2,
            creditHours = 3,
            semester = 1,
            isMandatory = true,
            description = "الأعمال التجارية، التاجر والتزاماته، الشركات التجارية والعقود والأوراق التجارية"
        ),
        CourseSubject(
            courseCode = "ECO-204",
            courseName = "مبادئ اقتصاد جزئي",
            departmentCode = "MIS",
            yearGroup = 2,
            creditHours = 3,
            semester = 1,
            isMandatory = true,
            description = "نظرية العرض والطلب، توازن السوق، سلوك المستهلك وتكاليف المنشأة في الأسواق المختلفة"
        ),
        CourseSubject(
            courseCode = "MKT-205",
            courseName = "مبادئ تسويق",
            departmentCode = "MIS",
            yearGroup = 2,
            creditHours = 3,
            semester = 1,
            isMandatory = true,
            description = "المزيج التسويقي 4Ps، أبحاث السوق، دراسة سلوك العميل والتسويق الرقمي الحديث"
        ),
        CourseSubject(
            courseCode = "MIS-206",
            courseName = "قواعد بيانات ونظم تشغيل",
            departmentCode = "MIS",
            yearGroup = 2,
            creditHours = 3,
            semester = 1,
            isMandatory = false,
            description = "مقرر اختياري تخصصي - تطبيقات قواعد البيانات وإدارة نظم المعلومات ونظم التشغيل"
        )
    )

    fun getAvailableCourses(deptCode: String, year: Int): List<CourseSubject> {
        return if (year == 2) {
            defaultYear2Courses
        } else {
            // مقررات نموذجية استرشادية لباقي الفرق بنظام الـ 3 ساعات
            listOf(
                CourseSubject("${deptCode}${year}01", "مقرر تخصصي رئيسي (1)", deptCode, year, 3, 1),
                CourseSubject("${deptCode}${year}02", "مقرر تخصصي رئيسي (2)", deptCode, year, 3, 1),
                CourseSubject("${deptCode}${year}03", "تحليل وتصميم النظم", deptCode, year, 3, 1),
                CourseSubject("${deptCode}${year}04", "إدارة الأعمال الإلكترونية", deptCode, year, 3, 1),
                CourseSubject("${deptCode}${year}05", "مهارات الاتصال والتفاوض", deptCode, year, 3, 1),
                CourseSubject("${deptCode}${year}06", "مقرر اختياري حر", deptCode, year, 3, 1, isMandatory = false)
            )
        }
    }
}


