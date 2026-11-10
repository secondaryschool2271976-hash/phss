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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cloud.CloudflareR2Config
import com.example.data.model.AcademicCloudFile
import com.example.data.model.YearGroup
import com.example.ui.theme.PharaohGold
import com.example.ui.theme.PharaohGoldDark
import com.example.ui.theme.PharaohGoldLight
import com.example.ui.theme.PharaohNavy
import com.example.ui.theme.PharaohNavyDark
import com.example.ui.theme.PharaohNavyLight
import com.example.ui.theme.PharaohTextFieldStyles
import com.example.ui.viewmodel.PharaohsViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.UserRole
import java.util.Locale

/**
 * حوار رفع ملفات المحاضرات والواجبات إلى Cloudflare R2
 */
@Composable
fun CloudUploadDialog(
    uiState: UiState,
    viewModel: PharaohsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Launcher لاختيار الملفات من الهاتف بجميع الصيغ المدعومة
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "file.pdf"
            var fileSize = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
            val ext = fileName.substringAfterLast('.', "pdf").lowercase(Locale.ROOT)
            val sizeStr = if (fileSize > 1024 * 1024) {
                String.format(Locale.US, "%.2f MB", fileSize / (1024.0 * 1024.0))
            } else {
                String.format(Locale.US, "%.1f KB", fileSize / 1024.0)
            }
            viewModel.selectFileForUpload(uri, fileName, sizeStr, ext)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.uploadProgressPercent,
        label = "uploadProgress"
    )

    AlertDialog(
        onDismissRequest = {
            if (!uiState.isUploadingToCloud) {
                onDismiss()
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (uiState.isUploadingToCloud) return@Button
                    if (!com.example.data.security.SecurityUtils.isNetworkAvailable(context)) {
                        viewModel.clearSelectedUploadFile()
                        return@Button
                    }
                    viewModel.uploadCurrentFile(context)
                },
                enabled = !uiState.isUploadingToCloud && uiState.uploadSelectedUri != null,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp,
                    disabledElevation = 0.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PharaohNavyDark,
                    contentColor = Color.White,
                    disabledContainerColor = PharaohNavyDark.copy(alpha = 0.6f),
                    disabledContentColor = Color.White.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_confirm_upload")
            ) {
                if (uiState.isUploadingToCloud) {
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
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isUploadingToCloud
            ) {
                Text("إلغاء", color = Color.Gray, fontSize = 13.sp)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = PharaohGold.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = PharaohGoldDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "رفع ملف إلى Cloudflare R2",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PharaohNavyDark
                    )
                    Text(
                        text = "Bucket: ${CloudflareR2Config.BUCKET_NAME}",
                        fontSize = 11.sp,
                        color = Color.Gray
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
                // Drag & Drop / File Picker Area
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (uiState.uploadSelectedUri != null) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = if (uiState.uploadSelectedUri != null) Color(0xFF22C55E) else PharaohGold.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !uiState.isUploadingToCloud) {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "video/mp4",
                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/zip",
                                    "*/*"
                                )
                            )
                        }
                        .testTag("file_picker_area")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.uploadSelectedUri == null) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = PharaohGoldDark,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "اضغط لاختيار ملف من جهازك",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = PharaohNavyDark
                            )
                            Text(
                                text = "يدعم: PDF, MP4, PPTX, DOCX, ZIP",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = getFileTypeColor(uiState.uploadSelectedExtension),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = uiState.uploadSelectedExtension.uppercase(Locale.ROOT),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.uploadSelectedFileName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = uiState.uploadSelectedFileSize,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearSelectedUploadFile() },
                                    enabled = !uiState.isUploadingToCloud
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "إلغاء الملف",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Field: اسم المحاضرة أو الواجب
                OutlinedTextField(
                    value = uiState.uploadTitleInput,
                    onValueChange = { viewModel.updateUploadTitle(it) },
                    label = { Text("اسم المحاضرة أو الواجب *", fontSize = 12.sp) },
                    placeholder = { Text("مثال: المحاضرة 4 - تخطيط موارد المؤسسات", fontSize = 12.sp) },
                    colors = PharaohTextFieldStyles.colors(),
                    singleLine = true,
                    enabled = !uiState.isUploadingToCloud,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_upload_title"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Field: اسم المادة الدراسية
                OutlinedTextField(
                    value = uiState.uploadSubjectInput,
                    onValueChange = { viewModel.updateUploadSubject(it) },
                    label = { Text("اسم المادة الدراسية *", fontSize = 12.sp) },
                    placeholder = { Text("مثال: تخطيط موارد المؤسسات (ERP)", fontSize = 12.sp) },
                    colors = PharaohTextFieldStyles.colors(),
                    singleLine = true,
                    enabled = !uiState.isUploadingToCloud,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_upload_subject"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Selection Chips
                Text(
                    text = "نوع المحتوى:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PharaohNavyDark
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("محاضرة", "واجب / تكليف", "شيت وسكاشن", "ملخص ومراجعة").forEach { cat ->
                        FilterChip(
                            selected = uiState.uploadCategoryInput == cat,
                            onClick = { viewModel.updateUploadCategory(cat) },
                            label = { Text(cat, fontSize = 10.sp) },
                            enabled = !uiState.isUploadingToCloud,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PharaohGold,
                                selectedLabelColor = PharaohNavyDark
                            )
                        )
                    }
                }

                // Target Year info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الوجهة: ${uiState.selectedDepartment.arabicName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PharaohNavyDark
                    )
                    Text(
                        text = uiState.selectedYear.arabicName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PharaohGoldDark
                    )
                }

                // Upload Progress Bar
                AnimatedVisibility(visible = uiState.isUploadingToCloud) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF9C3), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "جاري الرفع إلى سحابة R2...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PharaohNavyDark
                            )
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = PharaohGoldDark
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PharaohGold,
                            trackColor = Color.White
                        )
                    }
                }

                // Error message
                uiState.uploadErrorMessage?.let { err ->
                    Text(
                        text = err,
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    )
}

/**
 * كارت نجاح الرفع مع زر نسخ الرابط وزر التحميل المباشر
 */
@Composable
fun UploadedFileSuccessCard(
    file: AcademicCloudFile,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("uploaded_success_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0FDF4)
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF86EFAC))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تم الرفع بنجاح إلى Cloudflare R2!",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color(0xFF166534)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Text("✕", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File Details
            Text(
                text = file.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PharaohNavyDark
            )
            Text(
                text = "${file.subjectName} | ${YearGroup.fromNumber(file.yearGroup).arabicName} | ${file.formattedSize}",
                fontSize = 11.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Download URL Field
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = file.downloadUrl,
                        fontSize = 11.sp,
                        color = Color(0xFF0369A1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Copy Link & Direct Download
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button: نسخ الرابط
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Cloudflare R2 Link", file.downloadUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ رابط التحميل المباشر بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_copy_public_url"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PharaohNavyDark,
                        contentColor = PharaohGoldLight
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ الرابط", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Button: تحميل الملف مباشرة
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(file.downloadUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح الرابط: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_direct_download"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تحميل الملف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * كارت عرض كل ملف في قائمة السحابة
 */
@Composable
fun CloudFileCard(
    file: AcademicCloudFile,
    canDelete: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_cloud_file_${file.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Type Badge Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getFileTypeColor(file.fileExtension),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = getFileTypeIcon(file.fileExtension),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = file.fileExtension.uppercase(Locale.ROOT),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PharaohGold.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = file.category,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = file.subjectName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = file.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${file.formattedSize} • ${YearGroup.fromNumber(file.yearGroup).arabicName}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                }

                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الملف",
                            tint = Color.Red.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy Link Button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Cloudflare R2 Link", file.downloadUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ الرابط المباشر للمحاضرة", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ الرابط", fontSize = 11.sp)
                }

                // Direct Download Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(file.downloadUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح الرابط: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PharaohNavyDark,
                        contentColor = PharaohGoldLight
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تحميل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun getFileTypeColor(extension: String): Color {
    return when (extension.lowercase(Locale.ROOT)) {
        "pdf" -> Color(0xFFDC2626) // Red
        "mp4" -> Color(0xFF7C3AED) // Purple
        "pptx" -> Color(0xFFEA580C) // Orange
        "docx" -> Color(0xFF2563EB) // Blue
        "zip" -> Color(0xFF059669) // Emerald
        else -> Color(0xFF475569) // Slate
    }
}

fun getFileTypeIcon(extension: String): ImageVector {
    return when (extension.lowercase(Locale.ROOT)) {
        "pdf" -> Icons.Default.PictureAsPdf
        "mp4" -> Icons.Default.VideoLibrary
        "pptx" -> Icons.Default.Slideshow
        "docx" -> Icons.Default.Description
        "zip" -> Icons.Default.FolderZip
        else -> Icons.Default.Description
    }
}
