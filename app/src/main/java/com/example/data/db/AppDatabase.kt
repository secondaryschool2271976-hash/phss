package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AcademicCloudFile
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.ExamSchedule
import com.example.data.model.Lecture
import com.example.data.model.Student
import com.example.data.model.VideoLecture
import com.example.data.model.Doctor
import com.example.data.security.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Query("SELECT * FROM students WHERE studentCode = :code LIMIT 1")
    suspend fun getStudentByCode(code: String): Student?

    @Query("SELECT * FROM students WHERE nationalIdHash = :hash LIMIT 1")
    suspend fun getStudentByNationalIdHash(hash: String): Student?

    @Query("SELECT * FROM students ORDER BY registrationDate DESC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE departmentCode = :deptCode AND yearGroup = :year ORDER BY registrationDate DESC")
    fun getStudentsByDeptAndYear(deptCode: String, year: Int): Flow<List<Student>>

    @Query("SELECT * FROM students")
    suspend fun getStudentsList(): List<Student>

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentCount(): Int

    @Query("DELETE FROM students WHERE rawNationalId = :nationalId OR nationalIdHash = :hash")
    suspend fun deleteStudentByNationalId(nationalId: String, hash: String)

    @Delete
    suspend fun deleteStudent(student: Student)
}

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures WHERE departmentCode = :deptCode AND yearGroup = :year ORDER BY lectureNumber ASC")
    fun getLectures(deptCode: String, year: Int): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures ORDER BY id DESC")
    fun getAllLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures")
    suspend fun getAllLecturesList(): List<Lecture>

    @Query("SELECT * FROM lectures WHERE doctorNationalId = :doctorNationalId ORDER BY id DESC")
    fun getLecturesForDoctor(doctorNationalId: String): Flow<List<Lecture>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: Lecture): Long

    @Update
    suspend fun updateLecture(lecture: Lecture)

    @Delete
    suspend fun deleteLecture(lecture: Lecture)
}

@Dao
interface VideoLectureDao {
    @Query("SELECT * FROM video_lectures WHERE departmentCode = :deptCode AND yearGroup = :year ORDER BY id DESC")
    fun getVideos(deptCode: String, year: Int): Flow<List<VideoLecture>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoLecture): Long

    @Delete
    suspend fun deleteVideo(video: VideoLecture)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exam_schedules WHERE departmentCode = :deptCode AND yearGroup = :year ORDER BY examDate ASC")
    fun getExams(deptCode: String, year: Int): Flow<List<ExamSchedule>>

    @Query("SELECT * FROM exam_schedules ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<ExamSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamSchedule): Long

    @Delete
    suspend fun deleteExam(exam: ExamSchedule)
}

@Dao
interface AcademicCloudFileDao {
    @Query("SELECT * FROM academic_cloud_files WHERE departmentCode = :deptCode AND yearGroup = :year ORDER BY id DESC")
    fun getFiles(deptCode: String, year: Int): Flow<List<AcademicCloudFile>>

    @Query("SELECT * FROM academic_cloud_files ORDER BY id DESC")
    fun getAllFiles(): Flow<List<AcademicCloudFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: AcademicCloudFile): Long

    @Delete
    suspend fun deleteFile(file: AcademicCloudFile)
}

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments WHERE departmentCode = :deptCode AND yearGroup = :year ORDER BY id DESC")
    fun getAssignments(deptCode: String, year: Int): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments ORDER BY id DESC")
    fun getAllAssignments(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments")
    suspend fun getAllAssignmentsList(): List<Assignment>

    @Query("SELECT * FROM assignments WHERE doctorNationalId = :doctorNationalId ORDER BY id DESC")
    fun getAssignmentsForDoctor(doctorNationalId: String): Flow<List<Assignment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment): Long

    @Delete
    suspend fun deleteAssignment(assignment: Assignment)
}

@Dao
interface AssignmentSubmissionDao {
    @Query("SELECT * FROM assignment_submissions WHERE assignmentId = :assignmentId ORDER BY submissionDate DESC")
    fun getSubmissionsForAssignment(assignmentId: Long): Flow<List<AssignmentSubmission>>

    @Query("SELECT * FROM assignment_submissions WHERE studentNationalId = :nationalId ORDER BY submissionDate DESC")
    fun getSubmissionsForStudent(nationalId: String): Flow<List<AssignmentSubmission>>

    @Query("SELECT * FROM assignment_submissions ORDER BY submissionDate DESC")
    fun getAllSubmissions(): Flow<List<AssignmentSubmission>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: AssignmentSubmission): Long

    @Delete
    suspend fun deleteSubmission(submission: AssignmentSubmission)

    @Query("DELETE FROM assignment_submissions WHERE id = :id")
    suspend fun deleteSubmissionById(id: Long)
}

@Dao
interface DoctorDao {
    @Query("SELECT * FROM doctors ORDER BY id DESC")
    fun getAllDoctors(): Flow<List<Doctor>>

    @Query("SELECT * FROM doctors WHERE nationalId = :nationalId LIMIT 1")
    suspend fun getDoctorByNationalId(nationalId: String): Doctor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: Doctor): Long

    @Delete
    suspend fun deleteDoctor(doctor: Doctor)
}

@Dao
interface CourseSubjectDao {
    @Query("SELECT * FROM course_subjects WHERE departmentCode = :deptCode AND yearGroup = :year ORDER BY courseCode ASC")
    fun getCoursesByDeptAndYear(deptCode: String, year: Int): Flow<List<com.example.data.model.CourseSubject>>

    @Query("SELECT * FROM course_subjects ORDER BY courseCode ASC")
    fun getAllCourses(): Flow<List<com.example.data.model.CourseSubject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: com.example.data.model.CourseSubject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<com.example.data.model.CourseSubject>)

    @Delete
    suspend fun deleteCourse(course: com.example.data.model.CourseSubject)
}

@Dao
interface StudentCourseRegistrationDao {
    @Query("SELECT * FROM student_course_registrations WHERE studentNationalId = :nationalId ORDER BY courseCode ASC")
    fun getRegistrationsForStudent(nationalId: String): Flow<List<com.example.data.model.StudentCourseRegistration>>

    @Query("SELECT * FROM student_course_registrations ORDER BY registrationDate DESC")
    fun getAllRegistrations(): Flow<List<com.example.data.model.StudentCourseRegistration>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: com.example.data.model.StudentCourseRegistration)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistrations(registrations: List<com.example.data.model.StudentCourseRegistration>)

    @Query("DELETE FROM student_course_registrations WHERE studentNationalId = :nationalId AND courseCode = :courseCode")
    suspend fun deleteRegistration(nationalId: String, courseCode: String)

    @Query("DELETE FROM student_course_registrations WHERE studentNationalId = :nationalId")
    suspend fun clearRegistrationsForStudent(nationalId: String)
}

@Database(
    entities = [
        Student::class,
        Lecture::class,
        VideoLecture::class,
        ExamSchedule::class,
        AcademicCloudFile::class,
        Assignment::class,
        AssignmentSubmission::class,
        Doctor::class,
        com.example.data.model.CourseSubject::class,
        com.example.data.model.StudentCourseRegistration::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun lectureDao(): LectureDao
    abstract fun videoLectureDao(): VideoLectureDao
    abstract fun examDao(): ExamDao
    abstract fun academicCloudFileDao(): AcademicCloudFileDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun assignmentSubmissionDao(): AssignmentSubmissionDao
    abstract fun doctorDao(): DoctorDao
    abstract fun courseSubjectDao(): CourseSubjectDao
    abstract fun studentCourseRegistrationDao(): StudentCourseRegistrationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pharaohs_university_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }
    }
}

// نظيف تماماً من البيانات الوهمية - يتم الاعتماد كلياً على Supabase والرفع الفعلي
suspend fun populateInitialData(database: AppDatabase) {
    // No mock students, no fake lectures, no fake exams
}
