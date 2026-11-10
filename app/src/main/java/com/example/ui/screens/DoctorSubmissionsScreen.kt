package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.Department
import com.example.data.model.YearGroup
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharaohsViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorSubmissionsScreen(
    uiState: UiState,
    viewModel: PharaohsViewModel,
    modifier: Modifier = Modifier
) {
    val allAssignments by viewModel.allAssignments.collectAsState()
    val allSubmissions by viewModel.allSubmissions.collectAsState()
    val context = LocalContext.current

    val doctorName = uiState.currentDoctorProfile?.name ?: ""
    val doctorId = uiState.currentDoctorProfile?.nationalId ?: ""

    // Filter assignments that belong to this doctor
    val doctorAssignments = remember(allAssignments, doctorName, doctorId) {
        allAssignments.filter { assignment ->
            (doctorId.isNotBlank() && assignment.doctorNationalId == doctorId) ||
                    (doctorName.isNotBlank() && assignment.doctorName.contains(doctorName, ignoreCase = true)) ||
                    doctorName.isBlank() // If no doctor profile, show all as fallback
        }
    }

    val doctorAssignmentIds = remember(doctorAssignments) {
        doctorAssignments.map { it.id }.toSet()
    }

    // Filter submissions that belong to this doctor's assignments
    val doctorSubmissions = remember(allSubmissions, doctorAssignmentIds) {
        allSubmissions.filter { submission ->
            doctorAssignmentIds.contains(submission.assignmentId) || doctorAssignmentIds.isEmpty()
        }
    }

    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedAssignmentFilter by remember { mutableStateOf<Long?>(null) }
    var showFilterDropdown by remember { mutableStateOf(false) }

    // Filter list
    val filteredSubmissions = remember(doctorSubmissions, searchQuery, selectedAssignmentFilter) {
        doctorSubmissions.filter { sub ->
            val matchesSearch = searchQuery.isBlank() ||
                    sub.studentName.contains(searchQuery.trim(), ignoreCase = true) ||
                    sub.studentCode.contains(searchQuery.trim(), ignoreCase = true)
            
            val matchesAssignment = selectedAssignmentFilter == null || sub.assignmentId == selectedAssignmentFilter
            
            matchesSearch && matchesAssignment
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Hero Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            color = PharaohNavyDark,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PharaohGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = PharaohGoldLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "تسليمات الطلاب المستلمة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "متابعة الواجبات المرفوعة من الطلاب ومراجعتها",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Search & Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم الطالب أو الكود...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("submission_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PharaohNavy,
                    unfocusedBorderColor = Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Filter button
            Box {
                Button(
                    onClick = { showFilterDropdown = true },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("btn_filter_submissions"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedAssignmentFilter != null) PharaohGold else PharaohNavyDark,
                        contentColor = if (selectedAssignmentFilter != null) PharaohNavyDark else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "تصفية")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedAssignmentFilter != null) "مصفى" else "الكل",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = showFilterDropdown,
                    onDismissRequest = { showFilterDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("عرض كل التسليمات", fontWeight = FontWeight.Bold) },
                        onClick = {
                            selectedAssignmentFilter = null
                            showFilterDropdown = false
                        }
                    )
                    Divider()
                    doctorAssignments.forEach { assignment ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = "${assignment.subjectName} • ${assignment.title}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                ) 
                            },
                            onClick = {
                                selectedAssignmentFilter = assignment.id
                                showFilterDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // Selected filter active badge
        if (selectedAssignmentFilter != null) {
            val activeAssig = doctorAssignments.firstOrNull { it.id == selectedAssignmentFilter }
            if (activeAssig != null) {
                Surface(
                    color = PharaohGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, PharaohGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "عرض تصفية: ${activeAssig.subjectName} • ${activeAssig.title}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PharaohNavyDark,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "إلغاء",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Red,
                            modifier = Modifier
                                .clickable { selectedAssignmentFilter = null }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        // Submissions Count
        Text(
            text = "إجمالي التسليمات: ${filteredSubmissions.size}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PharaohNavyLight,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // List of submissions
        if (filteredSubmissions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد أي تسليمات مستلمة حالياً",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSubmissions) { sub ->
                    val correspondingAssignment = allAssignments.firstOrNull { it.id == sub.assignmentId }
                    SubmissionRowItem(
                        submission = sub,
                        assignment = correspondingAssignment,
                        onDownloadClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sub.fileUrl))
                            context.startActivity(intent)
                        },
                        onDeleteClick = {
                            viewModel.deleteSubmission(sub)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SubmissionRowItem(
    submission: AssignmentSubmission,
    assignment: Assignment?,
    onDownloadClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Student Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PharaohNavyDark.copy(alpha = 0.08f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PharaohNavyDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = submission.studentName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PharaohNavyDark
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "كود الطالب: ${submission.studentCode}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        if (assignment != null) {
                            val dept = Department.fromCode(assignment.departmentCode).arabicName
                            val year = YearGroup.fromNumber(assignment.yearGroup).arabicName
                            Text(
                                text = " • $dept • $year",
                                fontSize = 10.sp,
                                color = PharaohNavyLight
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.LightGray.copy(alpha = 0.5f))

            // Assignment details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = PharaohGoldDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = assignment?.subjectName ?: "مقرر دراسي",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PharaohNavyDark
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "التكليف: ${assignment?.title ?: "بدون عنوان"}",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(start = 22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Student Notes if any
            if (submission.notes.isNotBlank()) {
                Text(
                    text = "ملاحظات الطالب:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Text(
                    text = submission.notes,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
            }

            // File Attachment Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = PharaohNavy,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = submission.fileName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PharaohNavyDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = submission.formattedSize.ifBlank { "غير محدد" },
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier
                                .size(36.dp)
                                .background(PharaohGold, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "عرض وتحميل",
                                tint = PharaohNavyDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (onDeleteClick != null) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFEF2F2), CircleShape)
                                    .border(1.dp, Color(0xFFFECACA), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الطلب",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Submission Date
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                val sdf = remember { SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")) }
                val dateStr = remember(submission.submissionDate) { sdf.format(Date(submission.submissionDate)) }
                Text(
                    text = "تاريخ التسليم: $dateStr",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
