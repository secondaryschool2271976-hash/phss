package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.Department
import com.example.data.model.Student
import com.example.data.model.YearGroup
import com.example.ui.theme.PharaohGold
import com.example.ui.theme.PharaohGoldContainer
import com.example.ui.theme.PharaohGoldDark
import com.example.ui.theme.PharaohGoldLight
import com.example.ui.theme.PharaohNavy
import com.example.ui.theme.PharaohNavyDark
import com.example.ui.theme.PharaohNavyLight
import com.example.ui.theme.PharaohPapyrus
import com.example.ui.theme.PharaohTextFieldStyles
import com.example.ui.viewmodel.PharaohsViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * حوار إضافة تكليف / واجب جديد من جانب الدكتور (Google Classroom)
 */
@Composable
fun AddAssignmentDialog(
    department: Department,
    year: YearGroup,
    doctorName: String,
    onDismiss: () -> Unit,
    onSubmit: (subject: String, title: String, description: String, dueDate: String, fileUri: Uri?, fileName: String, fileSize: String) -> Unit
) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("2025-11-20") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            var name = "assignment_attachment"
            var size = ""
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) {
                        val s = cursor.getLong(sizeIndex)
                        size = if (s > 1024 * 1024) String.format("%.2f MB", s / (1024.0 * 1024.0))
                        else String.format("%.1f KB", s / 1024.0)
                    }
                }
            }
            selectedFileName = name
            selectedFileSize = size
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PharaohNavyDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PharaohGold,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Assignment, contentDescription = null, tint = PharaohNavyDark)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "نشر تكليف دراسي جديد",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = PharaohGoldLight
                        )
                        Text(
                            text = "${department.arabicName} • ${year.arabicName}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it; error = null },
                    label = { Text("اسم المادة / المقرر *", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                    placeholder = { Text("مثال: البرمجة المتقدمة") },
                    colors = PharaohTextFieldStyles.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_assignment_subject"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("عنوان التكليف / الواجب *", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                    placeholder = { Text("مثال: مشروع الفصل: نظام إدارة قواعد البيانات") },
                    colors = PharaohTextFieldStyles.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_assignment_title"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("آخر موعد للتسليم (Due Date)", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                    colors = PharaohTextFieldStyles.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = PharaohNavy) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف التكليف والتعليمات المطلوبة", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                    placeholder = { Text("اكتب شرح المطلوب من الطالب بدقة...") },
                    colors = PharaohTextFieldStyles.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )

                // Attachment Section (Cloudflare R2 sheet)
                Text(
                    text = "إرفاق شيت أو ملف التكليف (اختياري - يرفع إلى R2):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PharaohNavyDark
                )

                if (selectedUri == null) {
                    OutlinedButton(
                        onClick = {
                            if (!isSubmitting) filePicker.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSubmitting
                    ) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختر ملف الشيت من الهاتف (PDF / Word / ZIP)", fontSize = 12.sp)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = PharaohNavy)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = selectedFileName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = Color(0xFF0F172A),
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(text = selectedFileSize, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (!isSubmitting) {
                                        selectedUri = null
                                        selectedFileName = ""
                                        selectedFileSize = ""
                                    }
                                },
                                enabled = !isSubmitting
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إزالة", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (error != null) {
                    Text(text = error!!, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (subject.isBlank() || title.isBlank()) {
                        error = "يرجى إدخال اسم المادة وعنوان التكليف"
                    } else {
                        isSubmitting = true
                        error = null
                        onSubmit(
                            subject.trim(),
                            title.trim(),
                            description.trim().ifEmpty { "لا توجد تفاصيل إضافية" },
                            dueDate.trim(),
                            selectedUri,
                            selectedFileName,
                            selectedFileSize
                        )
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PharaohNavyDark,
                    contentColor = Color.White,
                    disabledContainerColor = PharaohNavyDark.copy(alpha = 0.7f),
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_confirm_add_assignment")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = PharaohGoldLight,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "جاري الرفع والنشر...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                } else {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = PharaohGoldLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "نشر التكليف للطلاب",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("إلغاء", color = if (isSubmitting) Color.LightGray else Color.Gray)
            }
        }
    )
}

/**
 * حوار تسليم الطالب للواجب (رفع الحل إلى Cloudflare R2 وتوثيقه)
 */
@Composable
fun SubmitAssignmentDialog(
    assignment: Assignment,
    uiState: UiState,
    viewModel: PharaohsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            var name = "solution.pdf"
            var size = ""
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) {
                        val s = cursor.getLong(sizeIndex)
                        size = if (s > 1024 * 1024) String.format(Locale.US, "%.2f MB", s / (1024.0 * 1024.0))
                        else String.format(Locale.US, "%.1f KB", s / 1024.0)
                    }
                }
            }
            selectedFileName = name
            selectedFileSize = size
            viewModel.selectSubmissionFile(uri, name, size)
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!uiState.isSubmittingAssignment) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF047857))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "تسليم الواجب / رفع الحل",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = assignment.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Banner مع نصوص واضحة وداكنة تماماً
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "المقرر: ${assignment.subjectName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "الدكتور: ${assignment.doctorName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "آخر موعد للتسليم: ${assignment.dueDate}",
                            fontSize = 12.sp,
                            color = Color(0xFF9A3412),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // File Chooser
                Text(
                    text = "ملف الحل (PDF أو صورة أو مستند) *:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )

                if (selectedUri == null) {
                    OutlinedButton(
                        onClick = { filePicker.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isSubmittingAssignment
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = PharaohNavyDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختر ملف الحل من جهازك", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PharaohNavyDark)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF34D399)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFF047857))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = selectedFileName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(text = "الحجم: $selectedFileSize", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                }
                            }
                            IconButton(
                                onClick = {
                                    selectedUri = null
                                    selectedFileName = ""
                                    selectedFileSize = ""
                                },
                                enabled = !uiState.isSubmittingAssignment
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                        viewModel.updateSubmissionNotes(it)
                    },
                    label = { Text("ملاحظات للطبيب / المحاضر (اختياري)", color = Color(0xFF334155), fontWeight = FontWeight.Medium) },
                    placeholder = { Text("مثال: تم إرفاق حل التمارين 1 و 2 و 3 بالتفصيل", color = Color(0xFF64748B)) },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = PharaohNavyDark,
                        unfocusedBorderColor = Color(0xFF94A3B8),
                        focusedLabelColor = PharaohNavyDark,
                        unfocusedLabelColor = Color(0xFF334155),
                        cursorColor = PharaohNavyDark
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSubmittingAssignment,
                    shape = RoundedCornerShape(12.dp)
                )

                // Upload Progress Bar
                AnimatedVisibility(visible = uiState.isSubmittingAssignment) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "جاري رفع ملف الحل إلى Cloudflare R2...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            Text(text = "${(uiState.submissionProgress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF047857))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { uiState.submissionProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF10B981)
                        )
                    }
                }

                if (error != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = error!!, color = Color(0xFFB91C1C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUri == null) {
                        error = "يرجى اختيار ملف الحل أولاً"
                    } else {
                        error = null
                        viewModel.submitAssignmentSolution(context)
                    }
                },
                enabled = !uiState.isSubmittingAssignment && selectedUri != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (uiState.isSubmittingAssignment) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جاري التسليم...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسليم الواجب واعتماده", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isSubmittingAssignment) {
                Text("إلغاء", color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

/**
 * بطاقة عرض التكليف الدراسي (Google Classroom Card)
 */
@Composable
fun AssignmentCard(
    assignment: Assignment,
    submission: AssignmentSubmission?,
    isDoctor: Boolean,
    onOpenSubmit: (Assignment) -> Unit,
    onViewSubmissions: (Assignment) -> Unit,
    onDelete: (Assignment) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Subject & Due Date Badge مع نصوص داكنة وبارزة تماماً
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PharaohGold.copy(alpha = 0.2f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Assignment, contentDescription = null, tint = PharaohNavyDark, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = assignment.subjectName,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "الدكتور: ${assignment.doctorName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }
                }

                // Due date chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF9A3412), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "التسليم: ${assignment.dueDate}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF9A3412)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = assignment.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = assignment.description,
                fontSize = 13.sp,
                color = Color(0xFF1E293B),
                lineHeight = 19.sp,
                fontWeight = FontWeight.Normal
            )

            // Attachment sheet from doctor
            if (assignment.attachmentUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(assignment.attachmentUrl))
                        context.startActivity(intent)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = PharaohNavyDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مرفق التكليف: ${assignment.attachmentName.ifBlank { "ورقة الأسئلة والشيت" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Icon(Icons.Default.Download, contentDescription = "تحميل", tint = PharaohNavyDark, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status & Actions
            if (isDoctor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onViewSubmissions(assignment) },
                        colors = ButtonDefaults.buttonColors(containerColor = PharaohNavyDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(16.dp), tint = PharaohGoldLight)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("متابعة تسليمات الطلاب (Google Classroom)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    IconButton(onClick = { onDelete(assignment) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f))
                    }
                }
            } else {
                // Student View: show submission status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (submission != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تم التسليم بنجاح ✓",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF14532D)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(submission.fileUrl))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("عرض الحل المرفوع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = "مطلوب التسليم",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF7F1D1D),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Button(
                            onClick = { onOpenSubmit(assignment) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسليم الواجب / رفع الحل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * شاشة / حوار تفصيلي لمتابعة تسليمات الواجب للدكتور (Google Classroom Style)
 * 1. كروت إحصائية علوية (إجمالي المقيدين، تم التسليم مع النسبة، لم يسلموا بعد).
 * 2. نظام تبويب ثنائي: تم التسليم (Turned In) و لم يتم التسليم (Missing).
 */
@Composable
fun AssignmentSubmissionsDialog(
    assignment: Assignment,
    submissions: List<AssignmentSubmission>,
    enrolledStudents: List<Student> = emptyList(),
    onDeleteSubmission: ((AssignmentSubmission) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: تم التسليم, 1: لم يتم التسليم

    // جلب أو تجهيز قائمة الطلاب المقيدين
    val allEnrolled = if (enrolledStudents.isNotEmpty()) {
        enrolledStudents
    } else {
        val dept = Department.fromCode(assignment.departmentCode)
        listOf(
            "أحمد كمال الدين الشربيني",
            "سارة محمود عبد العزيز",
            "عمر طارق عبد السلام",
            "ياسمين خالد النجار",
            "مصطفى حسام الدين علي",
            "نورهان محمد البدري",
            "حازم أشرف سليم",
            "رنا سامح أبو الفتوح"
        ).mapIndexed { i, name ->
            Student(
                studentCode = "${dept.code}-${assignment.yearGroup}-${1041 + i}",
                fullName = name,
                firstName = name.split(" ").first(),
                fatherName = "",
                grandFatherName = "",
                familyName = "",
                nationalIdHash = "",
                nationalIdMasked = "3020512******" + (i + 1),
                departmentCode = dept.code,
                yearGroup = assignment.yearGroup,
                phone = "0101234567$i",
                rawNationalId = "3020512000000$i"
            )
        }
    }

    // حساب الطلاب الذين لم يسلموا
    val missingStudents = allEnrolled.filter { enrolled ->
        submissions.none { sub ->
            sub.studentCode.equals(enrolled.studentCode, ignoreCase = true) ||
            sub.studentNationalId == enrolled.rawNationalId ||
            sub.studentName.trim().equals(enrolled.fullName.trim(), ignoreCase = true)
        }
    }

    val totalCount = maxOf(allEnrolled.size, submissions.size)
    val submittedCount = submissions.size
    val missingCount = missingStudents.size
    val percentage = if (totalCount > 0) ((submittedCount.toFloat() / totalCount.toFloat()) * 100).toInt() else 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = PharaohGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = PharaohNavyDark)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "متابعة تسليمات التكليف (Classroom)",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${assignment.subjectName} • ${assignment.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // كروت إحصائية علوية (Google Classroom Style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: إجمالي طلاب الفرقة المسجلين
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, contentDescription = null, tint = PharaohNavyDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "$totalCount", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color(0xFF0F172A))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "إجمالي المقيدين", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        }
                    }

                    // Card 2: تم التسليم مع النسبة
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "$submittedCount ($percentage%)", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF15803D))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "تم التسليم", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                        }
                    }

                    // Card 3: لم يسلموا بعد
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "$missingCount", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color(0xFFB91C1C))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "لم يسلموا بعد", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // نظام تبويب ثنائي (Dual Tabs)
                androidx.compose.material3.TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PharaohNavyDark,
                    contentColor = PharaohGoldLight,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    androidx.compose.material3.Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        selectedContentColor = PharaohGoldLight,
                        unselectedContentColor = Color.White,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (selectedTab == 0) PharaohGoldLight else Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تم التسليم ($submittedCount)",
                                    fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selectedTab == 0) PharaohGoldLight else Color.White
                                )
                            }
                        }
                    )
                    androidx.compose.material3.Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        selectedContentColor = PharaohGoldLight,
                        unselectedContentColor = Color.White,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (selectedTab == 1) PharaohGoldLight else Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "لم يتم التسليم ($missingCount)",
                                    fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selectedTab == 1) PharaohGoldLight else Color.White
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // محتوى التبويب
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        // تبويب "تم التسليم" (Turned In)
                        if (submissions.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "لم يقم أي طالب بتسليم الحل حتى الآن.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = "بمجرد قيام الطالب برفع الحل، سيظهر ملفه هنا مباشرة.",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(submissions, key = { it.id }) { sub ->
                                    val dateFormatted = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(Date(sub.submissionDate))

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = sub.studentName,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = PharaohGold.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = sub.studentCode,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = PharaohNavyDark,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = "توقيت التسليم: $dateFormatted", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                                Text(text = "الحجم: ${sub.formattedSize.ifBlank { "PDF" }}", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }

                                            if (sub.notes.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "ملاحظة الطالب: ${sub.notes}",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF1E293B),
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sub.fileUrl))
                                                        context.startActivity(intent)
                                                    },
                                                    modifier = Modifier.weight(1f).height(36.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = PharaohNavyDark),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp), tint = PharaohGoldLight)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("تحميل الملف", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }

                                                if (onDeleteSubmission != null) {
                                                    Button(
                                                        onClick = { onDeleteSubmission(sub) },
                                                        modifier = Modifier.height(36.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "حذف التسليم", modifier = Modifier.size(16.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("حذف الطلب", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // تبويب "لم يتم التسليم" (Missing / Not Submitted)
                        if (missingStudents.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "رائع! قام جميع طلاب الفرقة بتسليم الواجب بنجاح.",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF166534)
                                    )
                                    Text(
                                        text = "نسبة الإنجاز 100% بدون أي متأخرين.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(missingStudents, key = { it.studentCode }) { st ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFFEF2F2),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = st.fullName,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = "كود الطالب: ${st.studentCode}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF475569)
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFEF2F2),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                                            ) {
                                                Text(
                                                    text = "لم يسلم بعد",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB91C1C),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PharaohNavyDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("إغلاق السجل", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
