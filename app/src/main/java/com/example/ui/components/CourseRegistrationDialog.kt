package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CourseSubject
import com.example.data.model.CreditHourCatalog
import com.example.data.model.Student
import com.example.data.model.StudentCourseRegistration
import com.example.ui.theme.PharaohGold
import com.example.ui.theme.PharaohGoldDark
import com.example.ui.theme.PharaohGoldLight
import com.example.ui.theme.PharaohNavy
import com.example.ui.theme.PharaohNavyDark
import com.example.ui.theme.PharaohNavyLight
import com.example.ui.viewmodel.PharaohsViewModel
import com.example.ui.viewmodel.UiState

/**
 * شاشة / نافذة تسجيل المواد بنظام الساعات المعتمدة
 * - حد أقصى 18 ساعة معتمدة لكل فصل دراسي
 * - يدعم مواد الفرقة الثانية المعتمدة (اللوجستيات، محاسبة التكاليف، قانون تجاري، اقتصاد جزئي، تسويق)
 */
@Composable
fun CourseRegistrationDialog(
    uiState: UiState,
    viewModel: PharaohsViewModel,
    onDismiss: () -> Unit
) {
    val availableCourses by viewModel.availableCreditCourses.collectAsState()
    val studentRegistrations by viewModel.studentCourseRegistrations.collectAsState()

    val totalRegisteredHours = studentRegistrations.sumOf { it.creditHours }
    val maxHours = CreditHourCatalog.MAX_CREDIT_HOURS_PER_SEMESTER
    val progressFraction = (totalRegisteredHours.toFloat() / maxHours.toFloat()).coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("dialog_course_registration"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PharaohGold,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAddCheck,
                                    contentDescription = null,
                                    tint = PharaohNavyDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تسجيل المواد الدراسية",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PharaohNavyDark
                            )
                            Text(
                                text = "نظام الساعات المعتمدة (الحد الأقصى: 18 ساعة)",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_registration_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Student Info & Credit Hours Summary Card
                CreditHoursSummaryCard(
                    student = uiState.currentStudent,
                    totalRegisteredHours = totalRegisteredHours,
                    maxHours = maxHours,
                    registeredCoursesCount = studentRegistrations.size,
                    progressFraction = progressFraction
                )

                // Success / Error Alerts
                AnimatedVisibility(visible = uiState.registrationSuccessMessage != null) {
                    uiState.registrationSuccessMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = Color(0xFF047857),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.registrationErrorMessage != null) {
                    uiState.registrationErrorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    color = Color(0xFFB91C1C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. One-Click Register All Year 2 Courses Button
                if (uiState.selectedYear.number == 2 && studentRegistrations.isEmpty()) {
                    Button(
                        onClick = { viewModel.registerDefaultYear2CoursesForCurrentStudent() },
                        colors = ButtonDefaults.buttonColors(containerColor = PharaohNavyDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_register_all_year2")
                    ) {
                        Icon(imageVector = Icons.Default.PlaylistAddCheck, contentDescription = null, tint = PharaohGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تسجيل المواد الـ 5 الأساسية للفرقة الثانية دفعة واحدة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PharaohGoldLight
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 4. Courses List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المقررات المتاحة للتسجيل (${availableCourses.size}):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PharaohNavyDark
                    )
                    Text(
                        text = "3 ساعات معتمدة / مقرر",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableCourses, key = { it.courseCode }) { course ->
                        val isRegistered = studentRegistrations.any { it.courseCode == course.courseCode }
                        CourseItemCard(
                            course = course,
                            isRegistered = isRegistered,
                            canAdd = isRegistered || (totalRegisteredHours + course.creditHours <= maxHours),
                            onToggle = { viewModel.toggleCourseRegistration(course) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Done Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PharaohGold, contentColor = PharaohNavyDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_confirm_registration_done")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حفظ وإتمام تسجيل المقررات ($totalRegisteredHours من 18 ساعة)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditHoursSummaryCard(
    student: Student?,
    totalRegisteredHours: Int,
    maxHours: Int,
    registeredCoursesCount: Int,
    progressFraction: Float
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PharaohNavyDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = student?.fullName ?: "طالب مقيد",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "الفرقة: ${student?.academicYear ?: "الفرقة الثانية"} • كود: ${student?.studentCode ?: "MIS-2-0001"}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (totalRegisteredHours in 12..18) Color(0xFF10B981) else PharaohGold
                ) {
                    Text(
                        text = "$totalRegisteredHours / $maxHours ساعة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totalRegisteredHours in 12..18) Color.White else PharaohNavyDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    totalRegisteredHours == 18 -> Color(0xFF10B981) // الأخضر المكتمل
                    totalRegisteredHours >= 12 -> PharaohGold
                    else -> Color(0xFF38BDF8)
                },
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "المقررات المسجلة: $registeredCoursesCount مقرر",
                    fontSize = 11.sp,
                    color = PharaohGoldLight,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "الساعات المتبقية: ${maxHours - totalRegisteredHours} ساعة",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CourseItemCard(
    course: CourseSubject,
    isRegistered: Boolean,
    canAdd: Boolean,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRegistered) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isRegistered) Color(0xFF10B981) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isRegistered || canAdd, onClick = onToggle)
            .testTag("course_item_${course.courseCode}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isRegistered,
                onCheckedChange = { onToggle() },
                enabled = isRegistered || canAdd,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF10B981),
                    uncheckedColor = PharaohNavy
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = course.courseName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRegistered) Color(0xFF065F46) else PharaohNavyDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (course.isMandatory) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "إجباري",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "كود: ${course.courseCode} • الفصل الدراسي: ${course.semester}",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )

                if (course.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = course.description,
                        fontSize = 10.sp,
                        color = Color.DarkGray,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Credit hours badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isRegistered) Color(0xFF10B981).copy(alpha = 0.15f) else PharaohNavy.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (isRegistered) Color(0xFF047857) else PharaohNavy,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${course.creditHours} ساعات",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRegistered) Color(0xFF047857) else PharaohNavy
                    )
                }
            }
        }
    }
}
