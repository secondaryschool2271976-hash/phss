package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.ui.components.AddExamDialog
import com.example.ui.theme.PharaohGold
import com.example.ui.theme.PharaohGoldContainer
import com.example.ui.theme.PharaohGoldDark
import com.example.ui.theme.PharaohGoldLight
import com.example.ui.theme.PharaohNavy
import com.example.ui.theme.PharaohNavyDark
import com.example.ui.theme.PharaohNavyLight
import com.example.ui.theme.PharaohTextFieldStyles
import com.example.ui.viewmodel.ContentAddType
import com.example.ui.viewmodel.PharaohsViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.UserRole

@Composable
fun ExamScheduleScreen(
    uiState: UiState,
    viewModel: PharaohsViewModel,
    modifier: Modifier = Modifier
) {
    val allExams by viewModel.allExams.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedDeptFilter by remember { mutableStateOf<String?>("ALL") }
    var showAddExamDialog by remember { mutableStateOf(false) }

    val filteredExams = allExams.filter { exam ->
        val matchesDept = selectedDeptFilter == "ALL" || exam.departmentCode == selectedDeptFilter
        val matchesSearch = searchQuery.isBlank() || exam.subjectName.contains(searchQuery.trim(), ignoreCase = true) || exam.hall.contains(searchQuery.trim(), ignoreCase = true)
        matchesDept && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Screen Header Title Bar (شريط علوي بارز لعناوين الشاشات)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PharaohNavyDark,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("exam_schedule_top_app_bar")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = PharaohGold,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = PharaohNavyDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "جدول الامتحانات الرسمية واللجان",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = PharaohGoldLight
                        )
                        Text(
                            text = "الفصل الدراسي الحالي - العام الجامعي 2025/2026",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                if (uiState.currentRole == UserRole.DOCTOR || uiState.currentRole == UserRole.ADMIN) {
                    Button(
                        onClick = { showAddExamDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PharaohGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PharaohNavyDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة موعد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PharaohNavyDark)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث باسم المقرر، القاعة، أو التاريخ...") },
            colors = PharaohTextFieldStyles.colors(),
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PharaohNavy)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_exams"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Department Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedDeptFilter == "ALL",
                onClick = { selectedDeptFilter = "ALL" },
                label = { Text("جميع الأقسام", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PharaohNavy,
                    selectedLabelColor = PharaohGoldLight
                )
            )

            Department.entries.forEach { dept ->
                FilterChip(
                    selected = selectedDeptFilter == dept.code,
                    onClick = { selectedDeptFilter = dept.code },
                    label = { Text(dept.code, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PharaohNavy,
                        selectedLabelColor = PharaohGoldLight
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Examination Regulations Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PharaohGoldContainer)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PharaohGoldDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تنبيه هام: يُحظر تماماً اصطحاب الهواتف المحمولة داخل اللجان، ويلزم إبراز كارنيه الطالب الصادر من التطبيق للدخول.",
                    fontSize = 11.sp,
                    color = PharaohNavyDark,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Exam Cards
        if (filteredExams.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لم يتم العثور على امتحانات مطابقة للبحث",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredExams, key = { it.id }) { exam ->
                    ExamScheduleCard(
                        exam = exam,
                        isDoctorAdmin = uiState.currentRole == UserRole.DOCTOR || uiState.currentRole == UserRole.ADMIN,
                        onDelete = { viewModel.deleteExam(exam) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }

    if (showAddExamDialog) {
        AddExamDialog(
            department = uiState.selectedDepartment,
            year = uiState.selectedYear,
            onDismiss = { showAddExamDialog = false },
            onConfirm = { subj, dt, time, dur, hall, notes ->
                viewModel.addExamSchedule(subj, dt, time, dur, hall, notes)
                showAddExamDialog = false
            }
        )
    }
}
