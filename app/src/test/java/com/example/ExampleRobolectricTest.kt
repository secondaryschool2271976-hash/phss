package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("الفراعنة", appName)
    }

    @Test
    fun `test valid Egyptian national ID validation`() {
        // Sample valid 14-digit Egyptian National ID for birth year 2002, May 12, Cairo (01)
        val validId = "30205120101234"
        val info = SecurityUtils.validateEgyptianNationalId(validId)

        assertTrue(info.isValid)
        assertEquals("12/05/2002", info.birthDate)
        assertEquals("القاهرة", info.governorate)
    }

    @Test
    fun `test invalid Egyptian national ID validation length and format`() {
        // Less than 14 digits
        val shortId = "302051201"
        val infoShort = SecurityUtils.validateEgyptianNationalId(shortId)
        assertFalse(infoShort.isValid)

        // Invalid century (starts with 1)
        val invalidCentury = "10205120101234"
        val infoCentury = SecurityUtils.validateEgyptianNationalId(invalidCentury)
        assertFalse(infoCentury.isValid)
    }

    @Test
    fun `test national ID hashing and masking`() {
        val id = "30205120101234"
        val hash = SecurityUtils.hashNationalId(id)
        assertNotNull(hash)
        assertEquals(64, hash.length) // SHA-256 produces 64 hex characters

        val masked = SecurityUtils.maskNationalId(id)
        assertEquals("3020512******4", masked)
    }

    @Test
    fun `test student code generation`() {
        val codeMIS = SecurityUtils.generateStudentCode("MIS", 3, 10)
        assertTrue(codeMIS.startsWith("MIS-3-"))

        val codeCS = SecurityUtils.generateStudentCode("CS", 1, 5)
        assertTrue(codeCS.startsWith("CS-1-"))
    }

    @Test
    fun `test Cloudflare R2 configurations and public domain URL formatting`() {
        val r2Manager = com.example.data.cloud.CloudflareR2Manager()
        val formatted1 = r2Manager.formatBytes(1024 * 1024 * 2) // 2 MB
        assertEquals("2.00 MB", formatted1)

        val formatted2 = r2Manager.formatBytes(512 * 1024) // 512 KB
        assertEquals("512.00 KB", formatted2)

        assertEquals("lecture-data", com.example.data.cloud.CloudflareR2Config.BUCKET_NAME)
        assertEquals("https://pub-f9710a4d35b34defa42b5ea768a0c9e9.r2.dev", com.example.data.cloud.CloudflareR2Config.PUBLIC_DOMAIN)

        val sampleFile = com.example.data.model.AcademicCloudFile(
            title = "المحاضرة الأولى",
            subjectName = "نظم معلومات",
            departmentCode = "MIS",
            yearGroup = 3,
            category = "محاضرة",
            originalFileName = "lecture1.pdf",
            uniqueFileName = "unique_123.pdf",
            fileExtension = "pdf",
            fileSize = 1048576L,
            formattedSize = "1.00 MB",
            downloadUrl = "${com.example.data.cloud.CloudflareR2Config.PUBLIC_DOMAIN}/uploads/unique_123.pdf"
        )
        assertTrue(sampleFile.downloadUrl.contains("pub-f9710a4d35b34defa42b5ea768a0c9e9.r2.dev"))
        assertTrue(sampleFile.downloadUrl.endsWith("unique_123.pdf"))
    }

    @Test
    fun `test Supabase configuration and student model creation`() {
        assertEquals("https://nevabpquhmgsbhhygybn.supabase.co", com.example.data.supabase.SupabaseConfig.PROJECT_URL)
        assertEquals("student", com.example.data.supabase.SupabaseConfig.STUDENT_TABLE)
        assertEquals("sb_publishable_36VwC0IlbeW1MUDkKLwDtQ_WJgwjIfk", com.example.data.supabase.SupabaseConfig.PUBLISHABLE_ANON_KEY)

        val student = com.example.data.supabase.SupabaseStudent(
            id = 101L,
            fullName = "طالب جامعي مسجل بالسحابة",
            nationalId = "30205120101234",
            academicYear = "الفرقة الثالثة - نظم معلومات إدارية",
            phone = "01012345678"
        )

        assertEquals("طالب جامعي مسجل بالسحابة", student.fullName)
        assertEquals("30205120101234", student.nationalId)
        assertEquals("الفرقة الثالثة - نظم معلومات إدارية", student.academicYear)
        assertEquals("01012345678", student.phone)
        assertEquals(101L, student.id)

        // Validate National ID
        val nationalIdValidation = com.example.data.security.SecurityUtils.validateEgyptianNationalId("30205120101234")
        assertTrue(nationalIdValidation.isValid)
        assertEquals("12/05/2002", nationalIdValidation.birthDate)

        // Validate Phone (null return means valid)
        val phoneValidation = com.example.data.security.SecurityUtils.validateEgyptianPhone("01012345678")
        org.junit.Assert.assertNull(phoneValidation)
    }

    @Test
    fun `test that multiple lectures uploaded by a doctor remain in ONE unified hall`() {
        val docNationalId = "28501010101234"
        val docName = "د. هاني حسني"

        // 3 محاضرات بأسماء مختلفة من نفس الدكتور
        val lectures = listOf(
            com.example.data.model.Lecture(
                id = 1L,
                departmentCode = "MIS",
                yearGroup = 3,
                subjectName = "قواعد بيانات 1",
                title = "المحاضرة الأولى: مقدمة في SQL",
                doctorName = docName,
                lectureNumber = 1,
                date = "2025-10-01",
                summary = "مقدمة",
                slidesCount = 15,
                slideDetails = "",
                doctorNationalId = docNationalId
            ),
            com.example.data.model.Lecture(
                id = 2L,
                departmentCode = "MIS",
                yearGroup = 3,
                subjectName = "قواعد بيانات 2",
                title = "المحاضرة الثانية: العلاقات والمفاتيح",
                doctorName = docName,
                lectureNumber = 2,
                date = "2025-10-08",
                summary = "العلاقات",
                slidesCount = 20,
                slideDetails = "",
                doctorNationalId = docNationalId
            ),
            com.example.data.model.Lecture(
                id = 3L,
                departmentCode = "MIS",
                yearGroup = 3,
                subjectName = "المحاضرة الثالثة",
                title = "المحاضرة الثالثة: الاستعلامات المتقدمة",
                doctorName = docName,
                lectureNumber = 3,
                date = "2025-10-15",
                summary = "استعلامات",
                slidesCount = 25,
                slideDetails = "",
                doctorNationalId = docNationalId
            )
        )

        // جلب المقررات للفرقة الثالثة قسم MIS
        val courses = com.example.data.model.CourseCatalog.getCoursesFor(
            deptCode = "MIS",
            year = 3,
            existingLectures = lectures,
            existingAssignments = emptyList()
        )

        // يجب أن ينتج قاعة واحدة فقط لهذا الدكتور تضم جميع محاضراته
        assertEquals("يجب أن تكون هناك قاعة واحدة فقط للدكتور مهما تعددت محاضراته", 1, courses.size)
        val doctorCourse = courses[0]

        // التحقق من أن جميع المحاضرات الـ 3 تتبع هذه القاعة الواحدة
        val matchingLectures = lectures.filter { doctorCourse.matchesLecture(it) }
        assertEquals(3, matchingLectures.size)
    }

    @Test
    fun `test quadruple name validation requirement`() {
        // Less than 4 names
        val singleName = "أحمد"
        assertNotNull(SecurityUtils.validateQuadrupleName(singleName))

        val doubleName = "أحمد محمد"
        assertNotNull(SecurityUtils.validateQuadrupleName(doubleName))

        val tripleName = "أحمد محمد علي"
        assertNotNull(SecurityUtils.validateQuadrupleName(tripleName))

        // Valid Quadruple Name (4 names)
        val validQuadName = "أحمد محمد علي إبراهيم"
        org.junit.Assert.assertNull(SecurityUtils.validateQuadrupleName(validQuadName))

        // Valid Quintuple Name (5 names)
        val validFiveNames = "أحمد محمد علي إبراهيم الدسوقي"
        org.junit.Assert.assertNull(SecurityUtils.validateQuadrupleName(validFiveNames))

        // Invalid short part
        val shortPartName = "أ محمد علي إبراهيم"
        assertNotNull(SecurityUtils.validateQuadrupleName(shortPartName))
    }

    @Test
    fun `test DeletedStudentsManager prevents deleted students from appearing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nid = "30205120101999"
        val code = "MIS-1-9999"

        assertFalse(com.example.data.security.DeletedStudentsManager.isDeleted(context, nationalId = nid, studentCode = code))

        // Delete student
        com.example.data.security.DeletedStudentsManager.markStudentDeleted(
            context = context,
            nationalId = nid,
            studentCode = code,
            fullName = "طالب تجريبي للاختبار"
        )

        // Must be marked as deleted
        assertTrue(com.example.data.security.DeletedStudentsManager.isDeleted(context, nationalId = nid))
        assertTrue(com.example.data.security.DeletedStudentsManager.isDeleted(context, studentCode = code))
        assertTrue(com.example.data.security.DeletedStudentsManager.isDeleted(context, fullName = "طالب تجريبي للاختبار"))
    }

    @Test
    fun `test credit hour system catalog and Year 2 courses specification`() {
        val y2Courses = com.example.data.model.CreditHourCatalog.defaultYear2Courses
        assertEquals(6, y2Courses.size)

        // Check the 5 requested core courses
        val courseNames = y2Courses.map { it.courseName }
        assertTrue(courseNames.contains("اللوجستيات و سلاسل الإمداد"))
        assertTrue(courseNames.contains("محاسبه التكاليف"))
        assertTrue(courseNames.contains("قانون تجاري"))
        assertTrue(courseNames.contains("مبادئ اقتصاد جزئي"))
        assertTrue(courseNames.contains("مبادئ تسويق"))

        // Check credit hours limit
        assertEquals(18, com.example.data.model.CreditHourCatalog.MAX_CREDIT_HOURS_PER_SEMESTER)
        val totalHours = y2Courses.sumOf { it.creditHours }
        assertEquals(18, totalHours)
    }
}

