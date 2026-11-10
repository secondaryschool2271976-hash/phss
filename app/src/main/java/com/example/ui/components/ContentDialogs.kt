package com.example.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Department
import com.example.data.model.ExamSchedule
import com.example.data.model.Lecture
import com.example.data.model.VideoLecture
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

@Composable
fun AddLectureDialog(
    department: Department,
    year: YearGroup,
    onDismiss: () -> Unit,
    onConfirm: (subject: String, title: String, doctor: String, lectureNum: Int, date: String, summary: String, slidesCount: Int, slideDetails: String, allowSubmissions: Boolean) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onConfirmWithFile: ((subject: String, title: String, doctor: String, lectureNum: Int, date: String, summary: String, slidesCount: Int, slideDetails: String, fileUri: Uri?, fileName: String, fileSize: String, allowSubmissions: Boolean) -> Unit)? = null,
    doctorName: String = "",
    defaultSubject: String = "",
    existingSubjects: List<String> = emptyList()
) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf(defaultSubject.ifBlank { existingSubjects.firstOrNull().orEmpty() }) }
    var title by remember { mutableStateOf("") }
    var doctor by remember { mutableStateOf(doctorName) }
    var lectureNum by remember { mutableStateOf("1") }
    var date by remember { mutableStateOf("2025-10-25") }
    var summary by remember { mutableStateOf("") }
    var slidesCount by remember { mutableStateOf("25") }
    var slideDetails by remember { mutableStateOf("") }
    var allowSubmissions by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            var name = "lecture_file"
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
            if (title.isBlank()) {
                title = name.substringBeforeLast('.')
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
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
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = PharaohNavyDark)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "إضافة محاضرة ورفعها إلى السحابة",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Real File Picker Button (Cloudflare R2)
                Text(
                    text = "ملف المحاضرة (PDF / PPTX / MP4 / DOCX / ZIP) *:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PharaohNavyDark
                )

                if (selectedUri == null) {
                    OutlinedButton(
                        onClick = { filePicker.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختر ملف المحاضرة من ذاكرة الهاتف", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
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
                                        color = PharaohNavyDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(text = "الحجم: $selectedFileSize", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            IconButton(onClick = {
                                selectedUri = null
                                selectedFileName = ""
                                selectedFileSize = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "إزالة", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Progress Bar
                AnimatedVisibility(visible = isUploading) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "جاري رفع الملف إلى Cloudflare R2...", fontSize = 11.sp, color = Color(0xFF047857))
                            Text(text = "${(uploadProgress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF10B981)
                        )
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it; error = null },
                    label = { Text("اسم المادة") },
                    placeholder = { Text("مثال: قواعد البيانات 2") },
                    colors = PharaohTextFieldStyles.colors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lecture_subject"),
                    singleLine = true
                )

                if (existingSubjects.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "اختر من المواد المتاحة لربط المحاضرات في مقرر واحد:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PharaohNavyLight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(existingSubjects) { subName ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (subject.trim().equals(subName.trim(), ignoreCase = true)) PharaohGoldContainer else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (subject.trim().equals(subName.trim(), ignoreCase = true)) PharaohGold else Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable { subject = subName }
                                ) {
                                    Text(
                                        text = subName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PharaohNavyDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("عنوان المحاضرة") },
                    placeholder = { Text("مثال: المحاضرة 3: العلاقات والمفاتيح الأجنبية") },
                    colors = PharaohTextFieldStyles.colors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lecture_title"),
                    singleLine = true
                )

                if (doctorName.isBlank()) {
                    OutlinedTextField(
                        value = doctor,
                        onValueChange = { doctor = it; error = null },
                        label = { Text("اسم الدكتور / المحاضر") },
                        placeholder = { Text("مثال: د. هيئة التدريس") },
                        colors = PharaohTextFieldStyles.colors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_lecture_doctor"),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lectureNum,
                        onValueChange = { lectureNum = it.filter { c -> c.isDigit() } },
                        label = { Text("رقم المحاضرة") },
                        colors = PharaohTextFieldStyles.colors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_lecture_number"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = slidesCount,
                        onValueChange = { slidesCount = it.filter { c -> c.isDigit() } },
                        label = { Text("عدد الشرائح") },
                        colors = PharaohTextFieldStyles.colors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_lecture_slides_count"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("تاريخ المحاضرة") },
                    colors = PharaohTextFieldStyles.colors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("ملخص المحاضرة والنقاط الأساسية") },
                    colors = PharaohTextFieldStyles.colors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .testTag("input_lecture_summary"),
                    maxLines = 3
                )

                // مفتاح تحكم (Switch): السماح للطلاب بتسليم ملفات/واجبات لهذا الدرس
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (allowSubmissions) Color(0xFFFEF3C7) else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (allowSubmissions) PharaohGold else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "السماح للطلاب بتسليم ملفات/واجبات لهذا الدرس",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PharaohNavyDark
                            )
                            Text(
                                text = if (allowSubmissions)
                                    "✓ مفعّل: سيتاح زر 'رفع الحل / تسليم التكليف' للطلاب كواجب رسمي"
                                else
                                    "✕ معطّل: تظهر المحاضرة كمحتوى للقراءة والتحميل فقط",
                                fontSize = 11.sp,
                                color = if (allowSubmissions) Color(0xFF92400E) else Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(
                            checked = allowSubmissions,
                            onCheckedChange = { allowSubmissions = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PharaohGoldDark,
                                checkedTrackColor = PharaohGoldLight
                            ),
                            modifier = Modifier.testTag("switch_lecture_allow_submissions")
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
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error!!,
                                color = Color(0xFFB91C1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isUploading) return@Button
                    if (subject.isBlank() || title.isBlank() || doctor.isBlank()) {
                        error = "يرجى ملء جميع الحقول الإلزامية (المادة، العنوان، اسم الدكتور)"
                    } else {
                        // فحص حالة الاتصال بالإنترنت لمنع الانهيار
                        if (!com.example.data.security.SecurityUtils.isNetworkAvailable(context)) {
                            error = "تعذر الرفع: يرجى التحقق من اتصال الإنترنت والمحاولة مجدداً"
                            return@Button
                        }
                        isUploading = true
                        error = null
                        if (onConfirmWithFile != null) {
                            onConfirmWithFile(
                                subject.trim(),
                                title.trim(),
                                doctor.trim(),
                                lectureNum.toIntOrNull() ?: 1,
                                date.trim(),
                                summary.trim().ifEmpty { "لا يوجد ملخص متاح حالياً" },
                                slidesCount.toIntOrNull() ?: 20,
                                slideDetails.trim().ifEmpty { "شريحة تمهيدية | المفاهيم الأساسية | التطبيقات العملية | الخاتمة" },
                                selectedUri,
                                selectedFileName,
                                selectedFileSize,
                                allowSubmissions
                            )
                        } else {
                            onConfirm(
                                subject.trim(),
                                title.trim(),
                                doctor.trim(),
                                lectureNum.toIntOrNull() ?: 1,
                                date.trim(),
                                summary.trim().ifEmpty { "لا يوجد ملخص متاح حالياً" },
                                slidesCount.toIntOrNull() ?: 20,
                                slideDetails.trim().ifEmpty { "شريحة تمهيدية | المفاهيم الأساسية | التطبيقات العملية | الخاتمة" },
                                allowSubmissions
                            )
                        }
                    }
                },
                enabled = !isUploading,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp,
                    disabledElevation = 0.dp
                ),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PharaohNavyDark,
                    contentColor = Color.White,
                    disabledContainerColor = PharaohNavyDark.copy(alpha = 0.7f),
                    disabledContentColor = Color.White.copy(alpha = 0.9f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_confirm_add_lecture")
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        color = PharaohGoldLight,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "جاري الرفع والسحب إلى السحابة...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = PharaohGoldLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "رفع المحاضرة / نشر المحتوى",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("إلغاء", color = if (isUploading) Color.LightGray else Color.Gray)
            }
        }
    )
}

@Composable
fun AddVideoDialog(
    department: Department,
    year: YearGroup,
    onDismiss: () -> Unit,
    onConfirm: (subject: String, title: String, doctor: String, duration: Int, url: String, description: String) -> Unit,
    doctorName: String = ""
) {
    var subject by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var doctor by remember { mutableStateOf(doctorName) }
    var duration by remember { mutableStateOf("45") }
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "رفع فيديو محاضرة (${department.arabicName} - ${year.arabicName})",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = PharaohNavyDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it; error = null },
                    label = { Text("اسم المادة") },
                    placeholder = { Text("مثال: هياكل البيانات") },
                    colors = PharaohTextFieldStyles.colors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("عنوان الفيديو") },
                    placeholder = { Text("مثال: شرح عملي لخوارزميات الترتيب") },
                    colors = PharaohTextFieldStyles.colors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (doctorName.isBlank()) {
                    OutlinedTextField(
                        value = doctor,
                        onValueChange = { doctor = it; error = null },
                        label = { Text("اسم المحاضر") },
                        colors = PharaohTextFieldStyles.colors(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { c -> c.isDigit() } },
                    label = { Text("المدة بالدقائق") },
                    colors = PharaohTextFieldStyles.colors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("رابط الفيديو أو الملف (اختياري)") },
                    placeholder = { Text("اتركه فارغاً لاستخدام المشغل السحابي التلقائي") },
                    colors = PharaohTextFieldStyles.colors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف الفيديو والنقاط المشروحة") },
                    colors = PharaohTextFieldStyles.colors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    maxLines = 3
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isBlank() || title.isBlank() || doctor.isBlank()) {
                        error = "يرجى استيفاء الحقول الأساسية"
                    } else {
                        onConfirm(
                            subject.trim(),
                            title.trim(),
                            doctor.trim(),
                            duration.toIntOrNull() ?: 45,
                            url.trim(),
                            description.trim().ifEmpty { "فيديو تعليمي تفاعلي يغطي الجوانب التطبيقية والنظرية للمقرر." }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PharaohNavy)
            ) {
                Text("نشر الفيديو")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AddExamDialog(
    department: Department,
    year: YearGroup,
    onDismiss: () -> Unit,
    onConfirm: (subject: String, date: String, startTime: String, durationHours: Int, hall: String, notes: String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2025-06-15") }
    var startTime by remember { mutableStateOf("09:00 ص") }
    var durationHours by remember { mutableStateOf("2") }
    var hall by remember { mutableStateOf("مدرج الفراعنة رقم (1)") }
    var notes by remember { mutableStateOf("إحضار الكارنيه الجامعي والأدوات الشخصية") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة موعد امتحان (${department.arabicName} - ${year.arabicName})",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = PharaohNavyDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it; error = null },
                    label = { Text("اسم المقرر / المادة") },
                    colors = PharaohTextFieldStyles.colors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("تاريخ الامتحان") },
                        placeholder = { Text("YYYY-MM-DD") },
                        colors = PharaohTextFieldStyles.colors(),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("توقيت البدء") },
                        colors = PharaohTextFieldStyles.colors(),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = durationHours,
                        onValueChange = { durationHours = it.filter { c -> c.isDigit() } },
                        label = { Text("المدة (ساعات)") },
                        colors = PharaohTextFieldStyles.colors(),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = hall,
                        onValueChange = { hall = it },
                        label = { Text("القاعة / المدرج") },
                        colors = PharaohTextFieldStyles.colors(),
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("تعليمات وملاحظات اللجنة") },
                    colors = PharaohTextFieldStyles.colors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp),
                    maxLines = 3
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isBlank() || date.isBlank()) {
                        error = "يرجى إدخال اسم المادة وتاريخ الامتحان"
                    } else {
                        onConfirm(
                            subject.trim(),
                            date.trim(),
                            startTime.trim(),
                            durationHours.toIntOrNull() ?: 2,
                            hall.trim(),
                            notes.trim()
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PharaohNavy)
            ) {
                Text("إدراج بالجدول")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun SlideViewerDialog(
    lecture: Lecture,
    onDismiss: () -> Unit
) {
    var currentSlideIndex by remember { mutableIntStateOf(1) }
    val totalSlides = lecture.slidesCount.coerceAtLeast(1)

    // Generate descriptive slide content simulation
    val slideTitles = listOf(
        "مقدمة وأهداف المحاضرة",
        "المفاهيم النظرية والأطر العامة",
        "البنية الهيكلية والمكونات الأساسية",
        "حالات دراسية وأمثلة تطبيقية واقعية",
        "تحليل الخوارزميات والمعايير التشغيلية",
        "الاستنتاجات والتوصيات وأسئلة المراجعة"
    )
    val currentTitle = slideTitles[(currentSlideIndex - 1) % slideTitles.size]

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
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lecture.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PharaohNavyDark
                        )
                        Text(
                            text = "${lecture.subjectName} | ${lecture.doctorName}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_slide_viewer")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Slide Display Card (Presentation Canvas simulation)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PharaohNavyDark),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(PharaohNavyDark, PharaohNavy)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Slide Number & Tag
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PharaohGold.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "جامعة الفراعنة - الشريحة $currentSlideIndex / $totalSlides",
                                        color = PharaohGoldLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = lecture.date,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = currentTitle,
                                color = PharaohGoldLight,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Key points on this slide
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "• النقطة المحورية: ${lecture.summary.take(80)}...",
                                    "• الربط المفاهيمي بين النظريات الأكاديمية والتطبيق بسوق العمل في مصر والشرق الأوسط.",
                                    "• التحديات الشائعة في التطبيق وكيفية التغلب عليها بطرق هندسية وإدارية حديثة.",
                                    "• مرجع المحاضرة: المقرر الأكاديمي المعتمد - معهد الفراعنة العالي."
                                ).forEach { point ->
                                    Text(
                                        text = point,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Diagram box simulation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, PharaohGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "رسم توضيحي",
                                        tint = PharaohGoldLight,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "[شكل بياني ومخطط تدفق مفاهيم الشريحة رقم $currentSlideIndex]",
                                        color = PharaohGoldLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Controls for slides
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (currentSlideIndex > 1) currentSlideIndex--
                        },
                        enabled = currentSlideIndex > 1,
                        colors = ButtonDefaults.buttonColors(containerColor = PharaohNavy),
                        modifier = Modifier.testTag("btn_prev_slide")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "السابق")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("السابق")
                    }

                    Text(
                        text = "$currentSlideIndex / $totalSlides",
                        fontWeight = FontWeight.Bold,
                        color = PharaohNavyDark,
                        fontSize = 14.sp
                    )

                    Button(
                        onClick = {
                            if (currentSlideIndex < totalSlides) currentSlideIndex++
                        },
                        enabled = currentSlideIndex < totalSlides,
                        colors = ButtonDefaults.buttonColors(containerColor = PharaohNavy),
                        modifier = Modifier.testTag("btn_next_slide")
                    ) {
                        Text("التالي")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "التالي")
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerDialog(
    video: VideoLecture,
    onDismiss: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableFloatStateOf(0.18f) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    val totalDurationSeconds = video.durationMinutes * 60
    val currentSeconds = (currentProgress * totalDurationSeconds).toInt()
    val formattedCurrent = "%02d:%02d".format(currentSeconds / 60, currentSeconds % 60)
    val formattedTotal = "%02d:%02d".format(totalDurationSeconds / 60, totalDurationSeconds % 60)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PharaohNavyDark
                        )
                        Text(
                            text = "${video.subjectName} | المحاضر: ${video.doctorName}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Video Screen Canvas simulation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Educational stream backdrop
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PharaohGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                IconButton(onClick = { isPlaying = !isPlaying }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                        tint = PharaohGoldLight,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPlaying) "جاري تشغيل الفيديو بدقة عالية (1080p)" else "الفيديو متوقف مؤقتاً",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "سرعة العرض: ${playbackSpeed}x",
                            color = PharaohGoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Player Slider
                Slider(
                    value = currentProgress,
                    onValueChange = { currentProgress = it },
                    colors = SliderDefaults.colors(
                        thumbColor = PharaohGold,
                        activeTrackColor = PharaohGold,
                        inactiveTrackColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Time counters & speed controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$formattedCurrent / $formattedTotal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PharaohNavyDark
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (playbackSpeed == speed) PharaohGold else Color.LightGray.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { playbackSpeed = speed }
                            ) {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (playbackSpeed == speed) PharaohNavyDark else Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description and notes
                Text(
                    text = "نبذة عن محتوى الفيديو التعليمي:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PharaohNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.description,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    lineHeight = 18.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
    }
}
