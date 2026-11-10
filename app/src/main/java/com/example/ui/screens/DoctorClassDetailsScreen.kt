package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicCloudFile
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.Department
import com.example.data.model.DoctorCourse
import com.example.data.model.Lecture
import com.example.data.model.VideoLecture
import com.example.data.model.YearGroup
import com.example.ui.components.AddAssignmentDialog
import com.example.ui.components.AddLectureDialog
import com.example.ui.components.AssignmentCard
import com.example.ui.components.AssignmentSubmissionsDialog
import com.example.ui.components.CloudFileCard
import com.example.ui.components.CloudUploadDialog
import com.example.ui.components.SubmitAssignmentDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import com.example.ui.theme.PharaohGold
import com.example.ui.theme.PharaohGoldContainer
import com.example.ui.theme.PharaohGoldDark
import com.example.ui.theme.PharaohGoldLight
import com.example.ui.theme.PharaohNavy
import com.example.ui.theme.PharaohNavyDark
import com.example.ui.theme.PharaohNavyLight
import com.example.ui.viewmodel.ContentAddType
import com.example.ui.viewmodel.PharaohsViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.UserRole

/**
 * شاشة تفاصيل المادة / الفصل الدراسي للدكتور (DoctorClassDetailsScreen)
 * تعرض حصرياً:
 * 1. المحاضرات والملفات الخاصة بهذا المقرر فقط.
 * 2. التكليفات والواجبات المطروحة من هذا الدكتور فقط.
 */
@Composable
fun DoctorClassDetailsScreen(
    course: DoctorCourse,
    department: Department,
    year: YearGroup,
    lectures: List<Lecture>,
    assignments: List<Assignment>,
    studentSubmissions: List<AssignmentSubmission>,
    cloudFiles: List<AcademicCloudFile>,
    videos: List<VideoLecture>,
    uiState: UiState,
    viewModel: PharaohsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    // 0: المحاضرات, 1: التكليفات, 2: المرفقات والفيديوهات
    var selectedSubTab by remember { mutableIntStateOf(0) }

    // تصفية المحتوى حصرياً لقاعة هذا الدكتور الموحدة
    val courseLectures = lectures.filter { course.matchesLecture(it) }
    val courseAssignments = assignments.filter { course.matchesAssignment(it) }
    val courseCloudFiles = cloudFiles.filter { course.matchesCloudFile(it) }
    val courseVideos = videos.filter { course.matchesVideo(it) }

    val isDoctorAdmin = uiState.currentRole == UserRole.DOCTOR || uiState.currentRole == UserRole.ADMIN

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Class Header with Back Button
        Surface(
            color = PharaohNavyDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_from_class_details")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع للمقررات",
                            tint = PharaohGoldLight
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = course.subjectName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = course.doctorName,
                                fontSize = 12.sp,
                                color = PharaohGoldLight,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " • ${department.arabicName} • ${year.arabicName}",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Banner for this course
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClassStatChip(
                        title = "المحاضرات",
                        count = courseLectures.size.toString(),
                        icon = Icons.Default.Slideshow,
                        modifier = Modifier.weight(1f)
                    )
                    ClassStatChip(
                        title = "التكليفات",
                        count = courseAssignments.size.toString(),
                        icon = Icons.Default.Assignment,
                        modifier = Modifier.weight(1f)
                    )
                    ClassStatChip(
                        title = "الملفات وR2",
                        count = (courseCloudFiles.size + courseVideos.size).toString(),
                        icon = Icons.Default.CloudUpload,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sub Tabs
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = PharaohNavyDark,
            contentColor = PharaohGoldLight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                selectedContentColor = PharaohGoldLight,
                unselectedContentColor = Color.White,
                text = {
                    Text(
                        text = "المحاضرات (${courseLectures.size})",
                        fontWeight = if (selectedSubTab == 0) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (selectedSubTab == 0) PharaohGoldLight else Color.White
                    )
                },
                icon = { Icon(Icons.Default.Slideshow, contentDescription = null, tint = if (selectedSubTab == 0) PharaohGoldLight else Color.White, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_course_lectures")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                selectedContentColor = PharaohGoldLight,
                unselectedContentColor = Color.White,
                text = {
                    Text(
                        text = "التكليفات والواجبات (${courseAssignments.size})",
                        fontWeight = if (selectedSubTab == 1) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (selectedSubTab == 1) PharaohGoldLight else Color.White
                    )
                },
                icon = { Icon(Icons.Default.Assignment, contentDescription = null, tint = if (selectedSubTab == 1) PharaohGoldLight else Color.White, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_course_assignments")
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                selectedContentColor = PharaohGoldLight,
                unselectedContentColor = Color.White,
                text = {
                    Text(
                        text = "السحابة والفيديو (${courseCloudFiles.size + courseVideos.size})",
                        fontWeight = if (selectedSubTab == 2) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (selectedSubTab == 2) PharaohGoldLight else Color.White
                    )
                },
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = if (selectedSubTab == 2) PharaohGoldLight else Color.White, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_course_cloud_videos")
            )
        }

        // Content Display
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            when (selectedSubTab) {
                // 1. المحاضرات الخاصة بهذا المقرر فقط
                0 -> {
                    if (courseLectures.isEmpty()) {
                        EmptyCourseSection(
                            icon = Icons.Default.Slideshow,
                            title = "لا توجد محاضرات منشورة بعد لهذا المقرر",
                            subtitle = "سيقوم د. ${course.doctorName} برفع ملفات المحاضرات والشروحات قريباً",
                            actionText = if (isDoctorAdmin) "رفع محاضرة جديدة" else null,
                            onAction = { viewModel.setShowAddDialog(ContentAddType.LECTURE) }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(courseLectures, key = { it.id }) { lecture ->
                                CourseLectureCard(
                                    lecture = lecture,
                                    isDoctorAdmin = isDoctorAdmin,
                                    onOpenViewer = { viewModel.openLectureViewer(lecture) },
                                    onSubmitAssignment = { viewModel.openSubmitLectureDialog(lecture) },
                                    onDelete = { viewModel.deleteLecture(lecture) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(30.dp)) }
                        }
                    }
                }

                // 2. التكليفات والواجبات المطروحة من هذا الدكتور فقط
                1 -> {
                    if (courseAssignments.isEmpty()) {
                        EmptyCourseSection(
                            icon = Icons.Default.Assignment,
                            title = "لا توجد تكليفات أو واجبات دراسية مطلوبة حالياً",
                            subtitle = "عندما يطرح د. ${course.doctorName} شيتات أو تكليفات ستظهر هنا للتسليم المباشر",
                            actionText = if (isDoctorAdmin) "نشر تكليف جديد" else null,
                            onAction = { viewModel.setShowAddDialog(ContentAddType.ASSIGNMENT) }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isDoctorAdmin) {
                                item {
                                    Button(
                                        onClick = { viewModel.setShowAddDialog(ContentAddType.ASSIGNMENT) },
                                        modifier = Modifier.fillMaxWidth().testTag("btn_add_assignment_direct"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PharaohNavyDark,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Assignment,
                                            contentDescription = null,
                                            tint = PharaohGoldLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("إضافة تكليف جديد مباشر", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            items(courseAssignments, key = { it.id }) { assignment ->
                                val userSubmission = studentSubmissions.firstOrNull { it.assignmentId == assignment.id }
                                AssignmentCard(
                                    assignment = assignment,
                                    submission = userSubmission,
                                    isDoctor = isDoctorAdmin,
                                    onOpenSubmit = { viewModel.openSubmitAssignmentDialog(it) },
                                    onViewSubmissions = { viewModel.openAssignmentDetails(it) },
                                    onDelete = { viewModel.deleteAssignment(it) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(30.dp)) }
                        }
                    }
                }

                // 3. المرفقات السحابية والفيديوهات الخاصة بالمقرر
                2 -> {
                    if (courseCloudFiles.isEmpty() && courseVideos.isEmpty()) {
                        EmptyCourseSection(
                            icon = Icons.Default.CloudUpload,
                            title = "لا توجد ملفات سحابية أو فيديوهات مسجلة لهذا المقرر",
                            subtitle = "يمكن لأستاذ المادة رفع ملفات R2 أو روابط الشروحات المسجلة",
                            actionText = if (isDoctorAdmin) "رفع ملف R2" else null,
                            onAction = { viewModel.setShowAddDialog(ContentAddType.CLOUD_FILE) }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (courseCloudFiles.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "الملفات والشيتات في سحابة Cloudflare R2:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PharaohNavyDark
                                    )
                                }
                                items(courseCloudFiles, key = { it.id }) { file ->
                                    CloudFileCard(
                                        file = file,
                                        canDelete = isDoctorAdmin,
                                        onDelete = { viewModel.deleteCloudFile(file) }
                                    )
                                }
                            }

                            if (courseVideos.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "المحاضرات المصورة بالفيديو:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PharaohNavyDark
                                    )
                                }
                                items(courseVideos, key = { it.id }) { video ->
                                    CourseVideoCard(
                                        video = video,
                                        onPlay = { viewModel.openVideoPlayer(video) },
                                        onDelete = if (isDoctorAdmin) { { viewModel.deleteVideo(video) } } else null
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(70.dp)) }
                        }
                    }
                }
            }

            // الزر العائم البارز للدكتور لإضافة ونشر المحتوى
            if (isDoctorAdmin) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when (selectedSubTab) {
                            0 -> viewModel.setShowAddDialog(ContentAddType.LECTURE)
                            1 -> viewModel.setShowAddDialog(ContentAddType.ASSIGNMENT)
                            2 -> viewModel.setShowAddDialog(ContentAddType.CLOUD_FILE)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = PharaohNavyDark,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = when (selectedSubTab) {
                            1 -> Icons.Default.Assignment
                            2 -> Icons.Default.CloudUpload
                            else -> Icons.Default.CloudUpload
                        },
                        contentDescription = null,
                        tint = PharaohGoldLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedSubTab) {
                            1 -> "نشر تكليف جديد"
                            2 -> "رفع ملف R2"
                            else -> "رفع المحاضرة / نشر المحتوى"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Dialog for adding a lecture
    if (uiState.showAddDialog == ContentAddType.LECTURE) {
        val resolvedDoctorName = uiState.currentDoctorProfile?.name ?: course.doctorName
        AddLectureDialog(
            department = department,
            year = year,
            onDismiss = { viewModel.setShowAddDialog(null) },
            doctorName = resolvedDoctorName,
            defaultSubject = course.subjectName,
            onConfirmWithFile = { subj, title, doc, num, dt, sum, sCount, sDet, fileUri, fileName, fileSize, allowSubmissions ->
                if (fileUri != null) {
                    viewModel.selectLectureFile(fileUri, fileName, fileSize)
                }
                viewModel.addLectureWithCloudFile(
                    context = context,
                    subject = course.subjectName,
                    title = title,
                    doctor = resolvedDoctorName,
                    lectureNum = num,
                    date = dt,
                    summary = sum,
                    slidesCount = sCount,
                    slideDetails = sDet,
                    allowSubmissions = allowSubmissions
                )
            }
        )
    }

    // Dialog for adding an assignment
    if (uiState.showAddDialog == ContentAddType.ASSIGNMENT) {
        val resolvedDoctorName = uiState.currentDoctorProfile?.name ?: course.doctorName
        AddAssignmentDialog(
            department = department,
            year = year,
            doctorName = resolvedDoctorName,
            onDismiss = { viewModel.setShowAddDialog(null) },
            onSubmit = { subj, title, desc, due, uri, name, size ->
                if (uri != null) {
                    viewModel.selectAssignmentAttachment(uri, name, size)
                }
                viewModel.addAssignment(
                    context = context,
                    subject = if (subj.isNotBlank()) subj else course.subjectName,
                    title = title,
                    doctor = resolvedDoctorName,
                    description = desc,
                    dueDate = due
                )
            }
        )
    }

    // Dialog for uploading cloud file to R2
    if (uiState.showAddDialog == ContentAddType.CLOUD_FILE) {
        CloudUploadDialog(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.setShowAddDialog(null) }
        )
    }

    // Dialog for viewing submissions for an assignment
    if (uiState.activeAssignmentForDetails != null) {
        AssignmentSubmissionsDialog(
            assignment = uiState.activeAssignmentForDetails!!,
            submissions = uiState.activeAssignmentSubmissions,
            enrolledStudents = uiState.activeAssignmentEnrolledStudents,
            onDeleteSubmission = { viewModel.deleteSubmission(it) },
            onDismiss = { viewModel.openAssignmentDetails(null) }
        )
    }

    // Dialog for student submitting an assignment
    if (uiState.showAddDialog == ContentAddType.SUBMIT_ASSIGNMENT && uiState.activeAssignmentToSubmit != null) {
        SubmitAssignmentDialog(
            assignment = uiState.activeAssignmentToSubmit!!,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.setShowAddDialog(null) }
        )
    }
}

@Composable
private fun ClassStatChip(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PharaohGoldLight, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = count, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = title, fontSize = 10.sp, color = Color(0xFFE2E8F0))
        }
    }
}

@Composable
private fun EmptyCourseSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = PharaohGold.copy(alpha = 0.15f),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = PharaohNavy, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PharaohNavyDark,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PharaohNavyDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CourseLectureCard(
    lecture: Lecture,
    isDoctorAdmin: Boolean,
    onOpenViewer: () -> Unit,
    onSubmitAssignment: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lecture_item_${lecture.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PharaohNavyDark
                    ) {
                        Text(
                            text = "محاضرة #${lecture.lectureNumber}",
                            color = PharaohGoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = lecture.date, fontSize = 11.sp, color = Color.Gray)
                }

                if (isDoctorAdmin) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = lecture.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PharaohNavyDark
            )

            if (lecture.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lecture.summary,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenViewer,
                    colors = ButtonDefaults.buttonColors(containerColor = PharaohNavyDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(imageVector = Icons.Default.Slideshow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("عرض الشرائح والملخص (${lecture.slidesCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (lecture.fileUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lecture.fileUrl))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحميل PDF", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseVideoCard(
    video: VideoLecture,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.PlayCircleFilled, contentDescription = null, tint = Color(0xFFD97706))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = video.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PharaohNavyDark)
                    Text(text = "مدة: ${video.durationMinutes} دقيقة • ${video.addedDate}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = PharaohNavy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("مشاهدة", fontSize = 11.sp)
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
