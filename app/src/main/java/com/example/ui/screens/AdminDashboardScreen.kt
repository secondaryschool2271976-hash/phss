package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.Department
import com.example.data.model.Student
import com.example.data.model.YearGroup
import com.example.data.model.Doctor
import com.example.data.model.DoctorCourse
import com.example.data.security.SecurityUtils
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ContentAddType
import com.example.ui.viewmodel.PharaohsViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data model for courses used in the local admin course manager
data class AdminCourse(
    val code: String,
    val name: String,
    val yearGroup: Int,
    val semester: Int
)

enum class AdminScreenView {
    DOCTORS,        // لوحة تحكم الدكاترة (Phone 2)
    STUDENTS,       // قائمة الطلاب (Phone 3 & 4)
    ADD_DOCTOR,     // إضافة دكتور جديد (Phone 5 & 7)
    COURSES,        // قائمة المواد الدراسية (Phone 8)
    STUDENT_DETAILS,// تفاصيل الطالب (Phone 9)
    STATISTICS,     // تقارير وإحصائيات (Phone 10 & 11)
    SETTINGS        // الإعدادات (Phone 12)
}

@Composable
fun AdminDashboardScreen(
    uiState: UiState,
    viewModel: PharaohsViewModel,
    modifier: Modifier = Modifier
) {
    val allStudents by viewModel.allStudents.collectAsState()
    val allAssignments by viewModel.allAssignments.collectAsState()
    val allSubmissions by viewModel.allSubmissions.collectAsState()
    val context = LocalContext.current

    // Screen navigation state within Admin Dashboard
    var currentScreen by remember { mutableStateOf(AdminScreenView.DOCTORS) }
    // Bottom bar tab: 0: السجل (Doctors/Students), 1: المواد (Courses), 2: الإحصائيات (Statistics), 3: الإعدادات (Settings)
    var selectedBottomNav by remember { mutableIntStateOf(0) }

    // Search and filters
    var doctorSearchQuery by remember { mutableStateOf("") }
    var studentSearchQuery by remember { mutableStateOf("") }
    var courseSearchQuery by remember { mutableStateOf("") }
    var selectedDeptFilter by remember { mutableStateOf<String?>("ALL") }
    var selectedLevelFilter by remember { mutableIntStateOf(0) } // 0: ALL, 1-4: Years
    var selectedSemesterFilter by remember { mutableIntStateOf(0) } // 0: ALL, 1: Term 1, 2: Term 2

    // Selection states
    var selectedStudentForDetails by remember { mutableStateOf<Student?>(null) }
    var doctorToDelete by remember { mutableStateOf<Doctor?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var showConfirmPurgeAllStudentsDialog by remember { mutableStateOf(false) }
    var showSupabaseAdminSettingsDialog by remember { mutableStateOf(false) }

    // Settings states
    var showPasscodeChangeDialog by remember { mutableStateOf(false) }
    var currentPasscodeInput by remember { mutableStateOf("") }
    var newPasscodeInput by remember { mutableStateOf("") }
    var passcodeChangeError by remember { mutableStateOf<String?>(null) }
    var languageArabic by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    // قائمة المواد الدراسية الرسمية المعتمدة (تتضمن مواد الفرقة الثانية المعتمدة بنظام الساعات)
    var adminCoursesList by remember {
        mutableStateOf(
            listOf(
                AdminCourse("LOG-201", "اللوجستيات و سلاسل الإمداد", 2, 1),
                AdminCourse("ACC-202", "محاسبه التكاليف", 2, 1),
                AdminCourse("LAW-203", "قانون تجاري", 2, 1),
                AdminCourse("ECO-204", "مبادئ اقتصاد جزئي", 2, 1),
                AdminCourse("MKT-205", "مبادئ تسويق", 2, 1),
                AdminCourse("MIS-206", "قواعد بيانات ونظم تشغيل", 2, 1)
            )
        )
    }
    var showAddCourseDialog by remember { mutableStateOf(false) }

    // Back handler: return to main list if in subscreen
    BackHandler(enabled = currentScreen != AdminScreenView.DOCTORS && currentScreen != AdminScreenView.STUDENTS && currentScreen != AdminScreenView.COURSES && currentScreen != AdminScreenView.STATISTICS && currentScreen != AdminScreenView.SETTINGS) {
        currentScreen = when (selectedBottomNav) {
            0 -> AdminScreenView.DOCTORS
            1 -> AdminScreenView.COURSES
            2 -> AdminScreenView.STATISTICS
            else -> AdminScreenView.SETTINGS
        }
    }

    // Filtered lists
    val filteredDoctors = uiState.approvedDoctorsList.filter { doc ->
        doctorSearchQuery.isBlank() ||
                doc.name.contains(doctorSearchQuery.trim(), ignoreCase = true) ||
                doc.nationalId.contains(doctorSearchQuery.trim())
    }

    val filteredStudents = allStudents.filter { student ->
        val matchesDept = selectedDeptFilter == "ALL" || student.departmentCode == selectedDeptFilter
        val matchesLevel = selectedLevelFilter == 0 || student.yearGroup == selectedLevelFilter
        val matchesSearch = studentSearchQuery.isBlank() ||
                student.fullName.contains(studentSearchQuery.trim(), ignoreCase = true) ||
                student.studentCode.contains(studentSearchQuery.trim(), ignoreCase = true) ||
                student.phone.contains(studentSearchQuery.trim())
        matchesDept && matchesLevel && matchesSearch
    }

    val filteredCourses = adminCoursesList.filter { course ->
        val matchesLevel = selectedLevelFilter == 0 || course.yearGroup == selectedLevelFilter
        val matchesSemester = selectedSemesterFilter == 0 || course.semester == selectedSemesterFilter
        val matchesSearch = courseSearchQuery.isBlank() ||
                course.name.contains(courseSearchQuery.trim(), ignoreCase = true) ||
                course.code.contains(courseSearchQuery.trim())
        matchesLevel && matchesSemester && matchesSearch
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // 4 Navigation Items matching Phone 2, 3, 8, 10, 12: [السجل] [المواد] [الإحصائيات] [الضبط]
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E293B),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item 1: السجل
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                selectedBottomNav = 0
                                currentScreen = AdminScreenView.DOCTORS
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "السجل",
                            tint = if (selectedBottomNav == 0) PharaohGold else Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "السجل",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBottomNav == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBottomNav == 0) PharaohGold else Color(0xFF94A3B8)
                        )
                    }

                    // Item 2: المواد
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                selectedBottomNav = 1
                                currentScreen = AdminScreenView.COURSES
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "المواد",
                            tint = if (selectedBottomNav == 1) PharaohGold else Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "المواد",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBottomNav == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBottomNav == 1) PharaohGold else Color(0xFF94A3B8)
                        )
                    }

                    // Item 3: الإحصائيات
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                selectedBottomNav = 2
                                currentScreen = AdminScreenView.STATISTICS
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "الإحصائيات",
                            tint = if (selectedBottomNav == 2) PharaohGold else Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "الإحصائيات",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBottomNav == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBottomNav == 2) PharaohGold else Color(0xFF94A3B8)
                        )
                    }

                    // Item 4: الضبط
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                selectedBottomNav = 3
                                currentScreen = AdminScreenView.SETTINGS
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الضبط",
                            tint = if (selectedBottomNav == 3) PharaohGold else Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "الضبط",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBottomNav == 3) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBottomNav == 3) PharaohGold else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9))
        ) {
            when (currentScreen) {
                AdminScreenView.DOCTORS -> {
                    // SCREEN 2: لوحة تحكم الدكاترة (Phone 2)
                    DoctorsControlScreen(
                        doctors = filteredDoctors,
                        searchQuery = doctorSearchQuery,
                        onSearchChange = { doctorSearchQuery = it },
                        onAddClick = { currentScreen = AdminScreenView.ADD_DOCTOR },
                        onDeleteClick = { doctorToDelete = it },
                        onSwitchToStudents = { currentScreen = AdminScreenView.STUDENTS },
                        isAdmin = uiState.currentRole == UserRole.ADMIN
                    )
                }

                AdminScreenView.STUDENTS -> {
                    // SCREEN 3 & 4: قائمة الطلاب (Phone 3 & 4)
                    StudentsControlScreen(
                        students = filteredStudents,
                        searchQuery = studentSearchQuery,
                        onSearchChange = { studentSearchQuery = it },
                        selectedLevel = selectedLevelFilter,
                        onLevelChange = { selectedLevelFilter = it },
                        selectedDept = selectedDeptFilter,
                        onDeptChange = { selectedDeptFilter = it },
                        onAddClick = { viewModel.setShowAddDialog(ContentAddType.SUPABASE_STUDENT) },
                        onStudentDetails = { student ->
                            selectedStudentForDetails = student
                            currentScreen = AdminScreenView.STUDENT_DETAILS
                        },
                        onDeleteClick = { studentToDelete = it },
                        onPurgeAllClick = { showConfirmPurgeAllStudentsDialog = true },
                        onOpenSupabaseSettings = { showSupabaseAdminSettingsDialog = true },
                        onSwitchToDoctors = { currentScreen = AdminScreenView.DOCTORS },
                        isAdmin = uiState.currentRole == UserRole.ADMIN
                    )
                }

                AdminScreenView.ADD_DOCTOR -> {
                    // SCREEN 5 & 7: إضافة دكتور جديد (Phone 5 & 7)
                    AddDoctorScreen(
                        onBack = { currentScreen = AdminScreenView.DOCTORS },
                        onSave = { name, nid, dept, spec, birthdate, subjectName ->
                            viewModel.adminAddDoctor(
                                name = name,
                                nationalId = nid,
                                deptCode = dept.code,
                                deptName = dept.arabicName,
                                subjectName = subjectName,
                                onResult = { success ->
                                    if (success) {
                                        currentScreen = AdminScreenView.DOCTORS
                                    }
                                }
                            )
                        }
                    )
                }

                AdminScreenView.COURSES -> {
                    // SCREEN 8: قائمة المواد الدراسية (Phone 8)
                    CoursesControlScreen(
                        courses = filteredCourses,
                        searchQuery = courseSearchQuery,
                        onSearchChange = { courseSearchQuery = it },
                        onAddCourseClick = { showAddCourseDialog = true },
                        onDeleteCourse = { course ->
                            adminCoursesList = adminCoursesList.filter { it.code != course.code }
                            viewModel.adminDeleteCourse(
                                DoctorCourse(
                                    id = course.code,
                                    subjectName = course.name,
                                    doctorName = "",
                                    departmentCode = "",
                                    yearGroup = course.yearGroup
                                )
                            )
                        }
                    )
                }

                AdminScreenView.STUDENT_DETAILS -> {
                    // SCREEN 9: تفاصيل الطالب (Phone 9)
                    if (selectedStudentForDetails != null) {
                        StudentDetailsScreen(
                            student = selectedStudentForDetails!!,
                            allAssignments = allAssignments,
                            allSubmissions = allSubmissions,
                            onBack = { currentScreen = AdminScreenView.STUDENTS }
                        )
                    } else {
                        currentScreen = AdminScreenView.STUDENTS
                    }
                }

                AdminScreenView.STATISTICS -> {
                    // SCREEN 10 & 11: تقارير وإحصائيات (Phone 10 & 11) - REAL DATA ONLY
                    StatisticsScreen(
                        allStudents = allStudents,
                        approvedDoctors = uiState.approvedDoctorsList,
                        allAssignments = allAssignments,
                        allSubmissions = allSubmissions
                    )
                }

                AdminScreenView.SETTINGS -> {
                    // SCREEN 12: الإعدادات (Phone 12)
                    SettingsControlScreen(
                        onChangePasswordClick = { showPasscodeChangeDialog = true },
                        isArabic = languageArabic,
                        onLanguageToggle = { languageArabic = !languageArabic },
                        notificationsEnabled = notificationsEnabled,
                        onNotificationsToggle = { notificationsEnabled = !notificationsEnabled },
                        onLogoutClick = { viewModel.logout() },
                        isAdmin = uiState.currentRole == UserRole.ADMIN
                    )
                }
            }

            // MODAL 6: إزالة مستخدم (Phone 6)
            if (doctorToDelete != null) {
                UserDeleteDialog(
                    userName = "د. " + doctorToDelete!!.name,
                    userRole = "الدكتور",
                    onConfirm = {
                        viewModel.adminDeleteDoctor(doctorToDelete!!)
                        doctorToDelete = null
                    },
                    onDismiss = { doctorToDelete = null }
                )
            }

            if (studentToDelete != null) {
                UserDeleteDialog(
                    userName = studentToDelete!!.fullName,
                    userRole = "الطالب",
                    onConfirm = {
                        viewModel.adminDeleteLocalStudent(studentToDelete!!)
                        studentToDelete = null
                    },
                    onDismiss = { studentToDelete = null }
                )
            }

            // Supabase student registration
            if (uiState.showAddDialog == ContentAddType.SUPABASE_STUDENT) {
                AlertDialog(
                    onDismissRequest = { viewModel.setShowAddDialog(null) },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { viewModel.setShowAddDialog(null) }) {
                            Text("إغلاق", color = PharaohNavyDark, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        SupabaseRegistrationCard(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                )
            }

            if (uiState.supabaseSuccessStudent != null) {
                SupabaseSuccessDialog(
                    student = uiState.supabaseSuccessStudent!!,
                    onDismiss = { viewModel.dismissSupabaseSuccessDialog() }
                )
            }

            // Confirm Purge All Test Students Dialog
            if (showConfirmPurgeAllStudentsDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmPurgeAllStudentsDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تصفية وحذف كافة الطلاب التجريبيين", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "هل أنت متأكد من رغبتك في حذف وتصفية كافة الطلاب التجريبيين (${allStudents.size} طالب) نهائياً من قاعدة بيانات Supabase والسجلات؟",
                                fontSize = 13.sp,
                                color = Color(0xFF334155),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• سيتم مسح بياناتهم بالكامل من السحابة والجهاز.\n• لن يتمكن أي طالب تجريبي من تسجيل الدخول مجدداً.\n• يمكنك بعدها قيد الطلاب الحقيقيين بالاسم الرباعي ورسائل التحقق.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 17.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.adminPurgeAllTestStudents()
                                showConfirmPurgeAllStudentsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("نعم، حذف وتصفية الكل نهائياً", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmPurgeAllStudentsDialog = false }) {
                            Text("إلغاء", color = Color(0xFF64748B))
                        }
                    }
                )
            }

            // Supabase Admin Settings Dialog
            if (showSupabaseAdminSettingsDialog) {
                com.example.ui.components.SupabaseAdminSettingsDialog(
                    hasSecretKey = uiState.hasSupabaseAdminKey,
                    onSaveSecretKey = { viewModel.saveSupabaseSecretKey(it) },
                    onPurgeAllTestStudents = {
                        viewModel.adminPurgeAllTestStudents()
                        showSupabaseAdminSettingsDialog = false
                    },
                    onDismiss = { showSupabaseAdminSettingsDialog = false }
                )
            }

            // Phone Verification Dialog
            if (uiState.showPhoneVerificationDialog) {
                com.example.ui.components.PhoneVerificationDialog(
                    phone = uiState.supabasePhone,
                    otpCode = uiState.generatedPhoneOtp,
                    enteredCode = uiState.phoneVerificationInput,
                    countdownSeconds = uiState.phoneOtpCountdown,
                    errorMessage = uiState.phoneVerificationError,
                    onCodeChange = { viewModel.updatePhoneVerificationInput(it) },
                    onVerifyClick = { viewModel.verifyPhoneOtpAndRegister() },
                    onResendClick = { viewModel.resendPhoneVerificationOtp() },
                    onDismiss = { viewModel.dismissPhoneVerificationDialog() }
                )
            }

            // Add Course Dialog
            if (showAddCourseDialog) {
                var cCode by remember { mutableStateOf("") }
                var cName by remember { mutableStateOf("") }
                var cYear by remember { mutableIntStateOf(3) }
                var cTerm by remember { mutableIntStateOf(2) }

                AlertDialog(
                    onDismissRequest = { showAddCourseDialog = false },
                    title = {
                        Text("إضافة مادة دراسية جديدة", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cName,
                                onValueChange = { cName = it },
                                label = { Text("اسم المادة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = cCode,
                                onValueChange = { cCode = it },
                                label = { Text("كود المادة (مثل 200001)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cYear.toString(),
                                    onValueChange = { cYear = it.toIntOrNull() ?: 1 },
                                    label = { Text("الفرقة (1-4)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = cTerm.toString(),
                                    onValueChange = { cTerm = it.toIntOrNull() ?: 1 },
                                    label = { Text("الترم (1-2)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (cName.isNotBlank() && cCode.isNotBlank()) {
                                    adminCoursesList = adminCoursesList + AdminCourse(cCode.trim(), cName.trim(), cYear, cTerm)
                                    showAddCourseDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3E62))
                        ) {
                            Text("حفظ", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddCourseDialog = false }) {
                            Text("إلغاء", color = Color.Gray)
                        }
                    }
                )
            }

            // Passcode change Dialog
            if (showPasscodeChangeDialog) {
                AlertDialog(
                    onDismissRequest = { showPasscodeChangeDialog = false },
                    title = { Text("تغيير كلمة المرور", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = currentPasscodeInput,
                                onValueChange = { currentPasscodeInput = it; passcodeChangeError = null },
                                label = { Text("كلمة المرور الحالية") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newPasscodeInput,
                                onValueChange = { newPasscodeInput = it; passcodeChangeError = null },
                                label = { Text("كلمة المرور الجديدة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (passcodeChangeError != null) {
                                Text(passcodeChangeError!!, color = Color.Red, fontSize = 11.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (currentPasscodeInput.isBlank() || newPasscodeInput.isBlank()) {
                                    passcodeChangeError = "يرجى ملء كافة الحقول أولاً"
                                    return@Button
                                }
                                showPasscodeChangeDialog = false
                                currentPasscodeInput = ""
                                newPasscodeInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3E62))
                        ) {
                            Text("تعديل", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPasscodeChangeDialog = false }) {
                            Text("إلغاء", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}

// ==============================================================================
// 1. لوحة تحكم الدكاترة (Phone 2)
// ==============================================================================
@Composable
private fun DoctorsControlScreen(
    doctors: List<Doctor>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (Doctor) -> Unit,
    onSwitchToStudents: () -> Unit,
    isAdmin: Boolean = true
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Dark top app bar exactly like Phone 2
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E293B)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لوحة تحكم الدكاترة",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Subtitle row: "إدارة الدكاترة" with yellow/gold "إضافة" button + Toggle for students
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isAdmin) {
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("إضافة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "إدارة الدكاترة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.width(10.dp))
                // Quick switch button to students
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.clickable { onSwitchToStudents() }
                ) {
                    Text(
                        text = "الطلاب >",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3E62),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Search bar: "البحث..." with search icon
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("البحث...", color = Color(0xFF94A3B8)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFFCBD5E1),
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (doctors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا يوجد دكاترة مسجلين حالياً", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(doctors, key = { it.id }) { doc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left Action buttons: "إضافة", "تعديل", "إزالة"
                            if (isAdmin) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = { },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3E62)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("إضافة", fontSize = 10.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = { },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("تعديل", fontSize = 10.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = { onDeleteClick(doc) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("إزالة", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            // Right side: Doctor Info and Avatar
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = doc.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "القسم: ${doc.departmentName}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1E293B))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 2. قائمة الطلاب وإدارة الطلاب (Phone 3 & 4)
// ==============================================================================
@Composable
private fun StudentsControlScreen(
    students: List<Student>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedLevel: Int,
    onLevelChange: (Int) -> Unit,
    selectedDept: String?,
    onDeptChange: (String?) -> Unit,
    onAddClick: () -> Unit,
    onStudentDetails: (Student) -> Unit,
    onDeleteClick: (Student) -> Unit,
    onPurgeAllClick: () -> Unit = {},
    onOpenSupabaseSettings: () -> Unit = {},
    onSwitchToDoctors: () -> Unit,
    isAdmin: Boolean = true
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Dark top app bar exactly like Phone 3 & 4
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E293B)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "قائمة الطلاب",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Subtitle row: "إدارة الطلاب" with action buttons (إضافة + تصفية التجريبيين + إعدادات السحابة)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isAdmin) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAddClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onPurgeAllClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تصفية التجريبيين", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onOpenSupabaseSettings,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = "إعدادات Supabase", tint = Color(0xFF047857), modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "إدارة الطلاب (${students.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Quick switch button to doctors
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.clickable { onSwitchToDoctors() }
                ) {
                    Text(
                        text = "الدكاترة >",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Dropdown filters for Level (المستوى) and Department (القسم) like Phone 4
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onLevelChange((selectedLevel + 1) % 5) },
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedLevel == 0) "المستوى: الكل" else "المستوى $selectedLevel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onDeptChange(
                            when (selectedDept) {
                                "ALL" -> "MIS"
                                "MIS" -> "BA"
                                "BA" -> "CS"
                                else -> "ALL"
                            }
                        )
                    },
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDept == "ALL") "القسم: الكل" else Department.fromCode(selectedDept!!).arabicName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search bar: "البحث..."
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("البحث...", color = Color(0xFF94A3B8)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFFCBD5E1),
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا يوجد طلاب مسجلين حالياً", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(students, key = { it.studentCode }) { student ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStudentDetails(student) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Action buttons: "تفاصيل", "تعديل", "إزالة"
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { onStudentDetails(student) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3E62)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("تفاصيل", fontSize = 10.sp, color = Color.White)
                                }

                                if (isAdmin) {
                                    Button(
                                        onClick = { },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("تعديل", fontSize = 10.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = { onDeleteClick(student) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("إزالة", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }

                            // Right side: Student info with avatar
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = student.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${Department.fromCode(student.departmentCode).arabicName} • ${student.studentCode}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1E293B))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 3. إضافة دكتور جديد (Phone 5 & 7)
// ==============================================================================
@Composable
private fun AddDoctorScreen(
    onBack: () -> Unit,
    onSave: (name: String, nationalId: String, dept: Department, spec: String, birthdate: String, subjectName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nid by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf(Department.MIS) }
    var spec by remember { mutableStateOf("") }
    var subjectName by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Dark top app bar with back arrow
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E293B)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إضافة دكتور جديد",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Field 1: الاسم الكامل
            Text("الاسم الكامل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                placeholder = { Text("الاسم الكامل", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = PharaohTextFieldStyles.colors(),
                shape = RoundedCornerShape(8.dp)
            )

            // Field 2: الرقم القومي (14 رقم)
            Text("الرقم القومي (14 رقماً)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            OutlinedTextField(
                value = nid,
                onValueChange = { nid = it; errorMessage = null },
                placeholder = { Text("أدخل 14 رقماً مصرياً", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = PharaohTextFieldStyles.colors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp)
            )

            // Field 3: القسم
            Text("القسم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Department.entries.forEach { d ->
                    val isSelected = dept == d
                    FilterChip(
                        selected = isSelected,
                        onClick = { dept = d },
                        label = { Text(d.arabicName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3E62),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Field 4: المادة الدراسية (اسم المادة المخصصة للدكتور)
            Text("المادة الدراسية (اسم المادة التي يدرسها الدكتور)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            OutlinedTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                placeholder = { Text("مثال: قواعد البيانات / البرمجة المتقدمة", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = PharaohTextFieldStyles.colors(),
                shape = RoundedCornerShape(8.dp)
            )

            // Field 5: التخصص
            Text("التخصص", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            OutlinedTextField(
                value = spec,
                onValueChange = { spec = it },
                placeholder = { Text("التخصص الأكاديمي", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = PharaohTextFieldStyles.colors(),
                shape = RoundedCornerShape(8.dp)
            )

            // Field 5: تاريخ الميلاد
            Text("تاريخ الميلاد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            OutlinedTextField(
                value = birthdate,
                onValueChange = { birthdate = it },
                placeholder = { Text("YYYY-MM-DD", color = Color(0xFF94A3B8)) },
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = PharaohTextFieldStyles.colors(),
                shape = RoundedCornerShape(8.dp)
            )

            // Field 6: صورة
            Text("صورة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "رفع صورة", tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                }
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "يرجى إدخال الاسم الكامل"
                        return@Button
                    }
                    if (nid.length != 14 || !nid.all { it.isDigit() }) {
                        errorMessage = "الرقم القومي يجب أن يتكون من 14 رقماً مصرياً"
                        return@Button
                    }
                    onSave(name.trim(), nid.trim(), dept, spec.trim(), birthdate.trim(), subjectName.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3E62)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("حفظ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ==============================================================================
// 4. إزالة مستخدم (Modal Dialog Phone 6)
// ==============================================================================
@Composable
private fun UserDeleteDialog(
    userName: String,
    userRole: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        text = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(40.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "إزالة مستخدم",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$userRole: $userName",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "أنت على وشك إزالة هذا الحساب بالكامل من النظام.",
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تأكيد الإزالة", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}

// ==============================================================================
// 5. قائمة المواد الدراسية (Phone 8)
// ==============================================================================
@Composable
private fun CoursesControlScreen(
    courses: List<AdminCourse>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAddCourseClick: () -> Unit,
    onDeleteCourse: (AdminCourse) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Dark top app bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E293B)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "قائمة المواد الدراسية",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Subtitle row: "إدارة المواد" with Add button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onAddCourseClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("إضافة مادة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Text(
                text = "إدارة المواد",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Table Header exactly like Phone 8: [الإجراء] [الفصل] [الفرقة] [كود المادة]
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            color = Color(0xFFE2E8F0)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الإجراء", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("الفصل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text("الفرقة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text("كود المادة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        // Table Rows exactly like Phone 8
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            items(courses) { course ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تعديل/حذف",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDeleteCourse(course) },
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = course.semester.toString(),
                            fontSize = 12.sp,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = course.yearGroup.toString(),
                            fontSize = 12.sp,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = course.code,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 6. تفاصيل الطالب الأكاديمية والواجبات (Phone 9 + Requirements)
// ==============================================================================
@Composable
private fun StudentDetailsScreen(
    student: Student,
    allAssignments: List<Assignment>,
    allSubmissions: List<AssignmentSubmission>,
    onBack: () -> Unit
) {
    val dept = Department.fromCode(student.departmentCode)
    val year = YearGroup.fromNumber(student.yearGroup)

    // Calculate ACTUAL assignments for this student's year and department
    val relevantAssignments = allAssignments.filter {
        it.yearGroup == student.yearGroup &&
                (it.departmentCode.isBlank() || it.departmentCode == "ALL" || it.departmentCode == student.departmentCode)
    }

    // Calculate ACTUAL submissions from this student
    val studentSubmissions = allSubmissions.filter {
        it.studentCode == student.studentCode ||
                (student.rawNationalId.isNotBlank() && it.studentNationalId == student.rawNationalId)
    }

    val submittedAssignmentIds = studentSubmissions.map { it.assignmentId }.toSet()
    val submittedAssignments = relevantAssignments.filter { it.id in submittedAssignmentIds }
    val missingAssignments = relevantAssignments.filter { it.id !in submittedAssignmentIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Dark top app bar with back button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E293B)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تفاصيل الطالب",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Green Banner like Phone 9
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFD1FAE5)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تفاصيل الطالب",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = student.fullName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF065F46)
                    )
                }
            }

            // Student Information table matching Phone 9
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الاسم بالكامل:", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(student.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }

                    // NATIONAL ID SHOWN ONLY TO ADMIN (As requested!)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الرقم القومي (خاص بالمسؤول فقط):", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        Text(
                            text = if (student.rawNationalId.isNotBlank()) student.rawNationalId else student.nationalIdMasked,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFDC2626)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("رقم الهاتف:", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(student.phone.ifBlank { "غير مسجل" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("القسم الأكاديمي:", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(dept.arabicName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الفرقة الدراسية:", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(year.arabicName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("كود الطالب الجامعي:", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(student.studentCode, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF0F172A))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المعدل التراكمي (GPA):", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text("4.29", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF0D9488))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الحالة الأكاديمية:", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text("منتظم / نشط", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                    }
                }
            }

            // SPECIFIC REQUIREMENT: متابعة التكليفات والواجبات المستلمة وغير المستلمة
            Text(
                text = "متابعة الواجبات والتكليفات الدراسية:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // عدد الواجبات المستلمة
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${submittedAssignments.size}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                        Text("تم استلامها / تسليمها", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                    }
                }

                // عدد الواجبات التي لم تستلم
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${missingAssignments.size}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                        Text("لم يتم تسليمها بعد", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                    }
                }

                // إجمالي الواجبات
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${relevantAssignments.size}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                        Text("إجمالي التكليفات", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    }
                }
            }

            // قائمة الواجبات المسلمة
            if (submittedAssignments.isNotEmpty()) {
                Text("الواجبات المسلمة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                submittedAssignments.forEach { ass ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCFCE7))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ass.subjectName + ": " + ass.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF22C55E)) {
                                Text("تم التسليم ✓", fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            // قائمة الواجبات غير المسلمة
            if (missingAssignments.isNotEmpty()) {
                Text("الواجبات المتبقية التي لم تسلم:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                missingAssignments.forEach { ass ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ass.subjectName + ": " + ass.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEF4444)) {
                                Text("متبقي (لم يسلم)", fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            // الدورات المسجلة
            Text("الدورات والمقررات المسجلة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("• تطبيقات الإنترنت وتطوير الويب", fontSize = 11.sp, color = Color(0xFF1E293B))
                    Text("• هندسة البرمجيات وتحليل النظم", fontSize = 11.sp, color = Color(0xFF1E293B))
                    Text("• نظم قواعد البيانات الموزعة", fontSize = 11.sp, color = Color(0xFF1E293B))
                }
            }
        }
    }
}

// ==============================================================================
// 7. تقارير وإحصائيات فعلية (Phone 10 & 11) - REAL DATA ONLY
// ==============================================================================
@Composable
private fun StatisticsScreen(
    allStudents: List<Student>,
    approvedDoctors: List<Doctor>,
    allAssignments: List<Assignment>,
    allSubmissions: List<AssignmentSubmission>
) {
    // ACTUAL STUDENTS COUNT BY DEPARTMENT
    val studentsMis = allStudents.count { it.departmentCode == "MIS" }
    val studentsBa = allStudents.count { it.departmentCode == "BA" }
    val studentsCs = allStudents.count { it.departmentCode == "CS" }
    val totalStudents = allStudents.size

    // ACTUAL DOCTORS COUNT BY DEPARTMENT
    val doctorsMis = approvedDoctors.count { it.departmentCode == "MIS" }
    val doctorsBa = approvedDoctors.count { it.departmentCode == "BA" }
    val doctorsCs = approvedDoctors.count { it.departmentCode == "CS" }
    val totalDoctors = approvedDoctors.size

    // ACTUAL TOTAL USERS COUNT
    val totalUsers = totalStudents + totalDoctors + 1 // +1 for the Admin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Dark top app bar exactly like Phone 10 & 11
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E293B)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "تقارير وإحصائيات",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chart 1: عدد الطلاب حسب القسم (REAL VERTICAL BAR CHART)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "عدد الطلاب حسب القسم (فعلي: $totalStudents طالب)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val maxStudentCount = maxOf(studentsMis, studentsBa, studentsCs, 1).toFloat()

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            ) {
                                val barWidth = 46.dp.toPx()
                                val availableWidth = size.width
                                val space = (availableWidth - (barWidth * 3)) / 4

                                // Bar 1: MIS
                                val h1 = if (studentsMis > 0) (studentsMis / maxStudentCount) * 100.dp.toPx() else 4.dp.toPx()
                                drawRoundRect(
                                    color = Color(0xFF0D9488),
                                    topLeft = Offset(space, 105.dp.toPx() - h1),
                                    size = Size(barWidth, h1),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )

                                // Bar 2: BA
                                val h2 = if (studentsBa > 0) (studentsBa / maxStudentCount) * 100.dp.toPx() else 4.dp.toPx()
                                drawRoundRect(
                                    color = Color(0xFFEAB308),
                                    topLeft = Offset(space * 2 + barWidth, 105.dp.toPx() - h2),
                                    size = Size(barWidth, h2),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )

                                // Bar 3: CS
                                val h3 = if (studentsCs > 0) (studentsCs / maxStudentCount) * 100.dp.toPx() else 4.dp.toPx()
                                drawRoundRect(
                                    color = Color(0xFF10B981),
                                    topLeft = Offset(space * 3 + barWidth * 2, 105.dp.toPx() - h3),
                                    size = Size(barWidth, h3),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("نظم معلومات", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text("$studentsMis طالب", fontSize = 10.sp, color = Color(0xFF0D9488), fontWeight = FontWeight.Black)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("إدارة أعمال", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text("$studentsBa طالب", fontSize = 10.sp, color = Color(0xFFEAB308), fontWeight = FontWeight.Black)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("علوم حاسب", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text("$studentsCs طالب", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

            // Chart 2: توزيع الدكاترة (REAL HORIZONTAL PROGRESS BARS)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "توزيع الدكاترة (فعلي: $totalDoctors دكتور)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val safeTotalDocs = if (totalDoctors == 0) 1 else totalDoctors

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("قسم نظم معلومات إدارية", fontSize = 11.sp, color = Color(0xFF0F172A))
                                Text("$doctorsMis دكاترة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3E62))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { if (totalDoctors > 0) doctorsMis.toFloat() / safeTotalDocs else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF1E3E62),
                                trackColor = Color(0xFFE2E8F0)
                            )
                        }

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("قسم إدارة أعمال", fontSize = 11.sp, color = Color(0xFF0F172A))
                                Text("$doctorsBa دكاترة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { if (totalDoctors > 0) doctorsBa.toFloat() / safeTotalDocs else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF0D9488),
                                trackColor = Color(0xFFE2E8F0)
                            )
                        }

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("قسم علوم حاسب", fontSize = 11.sp, color = Color(0xFF0F172A))
                                Text("$doctorsCs دكاترة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { if (totalDoctors > 0) doctorsCs.toFloat() / safeTotalDocs else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            // Indicator 3: عدد المستخدمين (Phone 11) - REAL TOTAL
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "عدد المستخدمين الإجمالي",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEAB308).copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$totalUsers",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "(يشمل $totalStudents طالب + $totalDoctors دكتور + 1 مسؤول نظام)",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

// ==============================================================================
// 8. الإعدادات (Phone 12)
// ==============================================================================
@Composable
private fun SettingsControlScreen(
    onChangePasswordClick: () -> Unit,
    isArabic: Boolean,
    onLanguageToggle: () -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsToggle: () -> Unit,
    onLogoutClick: () -> Unit,
    isAdmin: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Dark top app bar exactly like Phone 12
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E293B)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "الإعدادات",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Option 1: تغيير كلمة المرور
            if (isAdmin) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChangePasswordClick() },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("تغيير كلمة المرور", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Option 2: اللغة
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLanguageToggle() },
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isArabic) "العربية >" else "English >", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("اللغة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Option 3: الإشعارات
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNotificationsToggle() },
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { onNotificationsToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1E3E62))
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("الإشعارات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Big red logout button like Phone 12
            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("تسجيل الخروج", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
