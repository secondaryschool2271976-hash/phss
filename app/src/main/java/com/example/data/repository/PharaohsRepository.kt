package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.AcademicCloudFile
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.ExamSchedule
import com.example.data.model.Lecture
import com.example.data.model.Student
import com.example.data.model.VideoLecture
import com.example.data.model.Doctor
import com.example.data.security.SecurityUtils
import kotlinx.coroutines.flow.Flow

class PharaohsRepository(private val database: AppDatabase) {

    private val studentDao = database.studentDao()
    private val lectureDao = database.lectureDao()
    private val videoDao = database.videoLectureDao()
    private val examDao = database.examDao()
    private val cloudFileDao = database.academicCloudFileDao()
    private val assignmentDao = database.assignmentDao()
    private val submissionDao = database.assignmentSubmissionDao()
    private val doctorDao = database.doctorDao()

    suspend fun registerStudent(
        firstName: String,
        fatherName: String,
        grandFatherName: String,
        familyName: String,
        nationalId: String,
        departmentCode: String,
        yearGroup: Int
    ): Result<Student> {
        val fName = firstName.trim()
        val faName = fatherName.trim()
        val gName = grandFatherName.trim()
        val famName = familyName.trim()
        val cleanNationalId = nationalId.trim()

        // 1. Validate 4 parts of the name
        val errorFirst = SecurityUtils.validateArabicNamePart(fName, "الاسم الأول")
        if (errorFirst != null) return Result.failure(IllegalArgumentException(errorFirst))

        val errorFather = SecurityUtils.validateArabicNamePart(faName, "اسم الأب")
        if (errorFather != null) return Result.failure(IllegalArgumentException(errorFather))

        val errorGrand = SecurityUtils.validateArabicNamePart(gName, "اسم الجد")
        if (errorGrand != null) return Result.failure(IllegalArgumentException(errorGrand))

        val errorFamily = SecurityUtils.validateArabicNamePart(famName, "اللقب / العائلة")
        if (errorFamily != null) return Result.failure(IllegalArgumentException(errorFamily))

        // 2. Validate National ID
        val idValidation = SecurityUtils.validateEgyptianNationalId(cleanNationalId)
        if (!idValidation.isValid) {
            return Result.failure(IllegalArgumentException(idValidation.errorMessage ?: "الرقم القومي غير صالح"))
        }

        // 3. Security: Hash national ID with SHA-256
        val nationalIdHash = SecurityUtils.hashNationalId(cleanNationalId)

        // 4. Check for duplicate National ID
        val existingStudent = studentDao.getStudentByNationalIdHash(nationalIdHash)
        if (existingStudent != null) {
            return Result.failure(
                IllegalStateException("الرقم القومي مسجل مسبقاً في النظام باسم: ${existingStudent.fullName}. كود الطالب المسجل: ${existingStudent.studentCode}")
            )
        }

        // 5. Generate Unique Student ID
        val count = studentDao.getStudentCount()
        val studentCode = SecurityUtils.generateStudentCode(departmentCode, yearGroup, count)

        val fullName = "$fName $faName $gName $famName"
        val maskedId = SecurityUtils.maskNationalId(cleanNationalId)

        val newStudent = Student(
            studentCode = studentCode,
            fullName = fullName,
            firstName = fName,
            fatherName = faName,
            grandFatherName = gName,
            familyName = famName,
            nationalIdHash = nationalIdHash,
            nationalIdMasked = maskedId,
            departmentCode = departmentCode,
            yearGroup = yearGroup,
            registrationDate = System.currentTimeMillis()
        )

        studentDao.insertStudent(newStudent)
        return Result.success(newStudent)
    }

    suspend fun loginWithStudentCode(code: String): Result<Student> {
        val cleanCode = code.trim().uppercase()
        val student = studentDao.getStudentByCode(cleanCode)
        return if (student != null) {
            Result.success(student)
        } else {
            Result.failure(IllegalArgumentException("لم يتم العثور على طالب مسجل بهذا الكود ($cleanCode). يرجى التأكد من كتابة الكود بشكل صحيح."))
        }
    }

    suspend fun loginWithNationalId(nationalId: String): Result<Student> {
        val hash = SecurityUtils.hashNationalId(nationalId.trim())
        val student = studentDao.getStudentByNationalIdHash(hash)
        return if (student != null) {
            Result.success(student)
        } else {
            Result.failure(IllegalArgumentException("لم يتم العثور على طالب مسجل بهذا الرقم القومي."))
        }
    }

    suspend fun deleteStudentByNationalId(nationalId: String) {
        val clean = nationalId.trim()
        val hash = SecurityUtils.hashNationalId(clean)
        studentDao.deleteStudentByNationalId(clean, hash)
    }

    fun getLectures(deptCode: String, year: Int): Flow<List<Lecture>> =
        lectureDao.getLectures(deptCode, year)

    fun getAllLectures(): Flow<List<Lecture>> =
        lectureDao.getAllLectures()

    suspend fun getAllLecturesList(): List<Lecture> =
        lectureDao.getAllLecturesList()

    fun getLecturesForDoctor(doctorNationalId: String): Flow<List<Lecture>> =
        lectureDao.getLecturesForDoctor(doctorNationalId)

    suspend fun addLecture(lecture: Lecture): Long =
        lectureDao.insertLecture(lecture)

    suspend fun deleteLecture(lecture: Lecture) =
        lectureDao.deleteLecture(lecture)

    fun getVideoLectures(deptCode: String, year: Int): Flow<List<VideoLecture>> =
        videoDao.getVideos(deptCode, year)

    suspend fun addVideoLecture(video: VideoLecture): Long =
        videoDao.insertVideo(video)

    suspend fun deleteVideoLecture(video: VideoLecture) =
        videoDao.deleteVideo(video)

    fun getExamSchedules(deptCode: String, year: Int): Flow<List<ExamSchedule>> =
        examDao.getExams(deptCode, year)

    fun getAllExams(): Flow<List<ExamSchedule>> =
        examDao.getAllExams()

    suspend fun addExamSchedule(exam: ExamSchedule): Long =
        examDao.insertExam(exam)

    suspend fun deleteExamSchedule(exam: ExamSchedule) =
        examDao.deleteExam(exam)

    fun getAllStudents(): Flow<List<Student>> =
        studentDao.getAllStudents()

    suspend fun getAllStudentsList(): List<Student> =
        studentDao.getStudentsList()

    fun getStudentsByDeptAndYear(deptCode: String, year: Int): Flow<List<Student>> =
        studentDao.getStudentsByDeptAndYear(deptCode, year)

    fun getCloudFiles(deptCode: String, year: Int): Flow<List<AcademicCloudFile>> =
        cloudFileDao.getFiles(deptCode, year)

    fun getAllCloudFiles(): Flow<List<AcademicCloudFile>> =
        cloudFileDao.getAllFiles()

    suspend fun saveCloudFile(file: AcademicCloudFile): Long =
        cloudFileDao.insertFile(file)

    suspend fun deleteCloudFile(file: AcademicCloudFile) =
        cloudFileDao.deleteFile(file)

    // Assignments (Google Classroom-like)
    fun getAssignments(deptCode: String, year: Int): Flow<List<Assignment>> =
        assignmentDao.getAssignments(deptCode, year)

    fun getAllAssignments(): Flow<List<Assignment>> =
        assignmentDao.getAllAssignments()

    suspend fun getAllAssignmentsList(): List<Assignment> =
        assignmentDao.getAllAssignmentsList()

    fun getAssignmentsForDoctor(doctorNationalId: String): Flow<List<Assignment>> =
        assignmentDao.getAssignmentsForDoctor(doctorNationalId)

    suspend fun addAssignment(assignment: Assignment): Long =
        assignmentDao.insertAssignment(assignment)

    suspend fun deleteAssignment(assignment: Assignment) =
        assignmentDao.deleteAssignment(assignment)

    fun getSubmissionsForAssignment(assignmentId: Long): Flow<List<AssignmentSubmission>> =
        submissionDao.getSubmissionsForAssignment(assignmentId)

    fun getAllSubmissions(): Flow<List<AssignmentSubmission>> =
        submissionDao.getAllSubmissions()

    fun getSubmissionsForStudent(nationalId: String): Flow<List<AssignmentSubmission>> =
        submissionDao.getSubmissionsForStudent(nationalId)

    suspend fun submitAssignment(submission: AssignmentSubmission): Long =
        submissionDao.insertSubmission(submission)

    suspend fun deleteSubmission(submission: AssignmentSubmission) =
        submissionDao.deleteSubmission(submission)

    suspend fun saveStudent(student: Student) {
        studentDao.insertStudent(student)
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.deleteStudent(student)
    }

    // Doctor Management (Approved Doctors)
    fun getAllDoctors(): Flow<List<Doctor>> =
        doctorDao.getAllDoctors()

    suspend fun getDoctorByNationalId(nationalId: String): Doctor? =
        doctorDao.getDoctorByNationalId(nationalId)

    suspend fun addDoctor(doctor: Doctor): Long =
        doctorDao.insertDoctor(doctor)

    suspend fun deleteDoctor(doctor: Doctor) {
        doctorDao.deleteDoctor(doctor)
    }

    private val courseSubjectDao = database.courseSubjectDao()
    private val registrationDao = database.studentCourseRegistrationDao()

    // Course Subjects (Credit Hour System)
    fun getAvailableCourses(deptCode: String, year: Int): Flow<List<com.example.data.model.CourseSubject>> =
        courseSubjectDao.getCoursesByDeptAndYear(deptCode, year)

    fun getAllCourseSubjects(): Flow<List<com.example.data.model.CourseSubject>> =
        courseSubjectDao.getAllCourses()

    suspend fun saveCourseSubject(subject: com.example.data.model.CourseSubject) {
        courseSubjectDao.insertCourse(subject)
    }

    suspend fun saveCourseSubjects(subjects: List<com.example.data.model.CourseSubject>) {
        courseSubjectDao.insertCourses(subjects)
    }

    suspend fun deleteCourseSubject(subject: com.example.data.model.CourseSubject) {
        courseSubjectDao.deleteCourse(subject)
    }

    // Student Course Registrations
    fun getRegistrationsForStudent(nationalId: String): Flow<List<com.example.data.model.StudentCourseRegistration>> =
        registrationDao.getRegistrationsForStudent(nationalId)

    fun getAllRegistrations(): Flow<List<com.example.data.model.StudentCourseRegistration>> =
        registrationDao.getAllRegistrations()

    suspend fun registerCourse(registration: com.example.data.model.StudentCourseRegistration) {
        registrationDao.insertRegistration(registration)
    }

    suspend fun registerCourses(registrations: List<com.example.data.model.StudentCourseRegistration>) {
        registrationDao.insertRegistrations(registrations)
    }

    suspend fun unregisterCourse(nationalId: String, courseCode: String) {
        registrationDao.deleteRegistration(nationalId, courseCode)
    }

    suspend fun clearStudentRegistrations(nationalId: String) {
        registrationDao.clearRegistrationsForStudent(nationalId)
    }
}


