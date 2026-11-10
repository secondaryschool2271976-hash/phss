package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.data.model.YearGroup
import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabaseStudent
import com.example.ui.theme.PharaohGold
import com.example.ui.theme.PharaohGoldDark
import com.example.ui.theme.PharaohGoldLight
import com.example.ui.theme.PharaohNavy
import com.example.ui.theme.PharaohNavyDark
import com.example.ui.theme.PharaohNavyLight
import com.example.ui.theme.PharaohTextFieldStyles
import com.example.ui.viewmodel.PharaohsViewModel
import com.example.ui.viewmodel.UiState

/**
 * قسم واجهة تسجيل الطالب في سحابة Supabase
 * مع التحقق الذكي للرقم القومي المصري ورقم الهاتف، وتحديد الكلية والفرقة
 * وتوليد كود طالب فريد يظهر مباشرة في الكارنيه
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupabaseRegistrationCard(
    uiState: UiState,
    viewModel: PharaohsViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("card_supabase_registration"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with Supabase Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF3ECF8E).copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "القيد السحابي للطلاب (Supabase)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = PharaohNavyDark
                        )
                        Text(
                            text = "قاعدة بيانات PostgreSQL الرسمية للجامعة",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "جدول: student",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Field 1: اسم الطالب (full_name) - رباعي إلزامي
            val nameParts = uiState.supabaseFullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val isQuadruple = nameParts.size >= 4 && nameParts.all { it.length >= 2 }

            OutlinedTextField(
                value = uiState.supabaseFullName,
                onValueChange = { viewModel.updateSupabaseFullName(it) },
                label = { Text("اسم الطالب بالكامل (رباعي إلزامي) *", fontSize = 12.sp) },
                placeholder = { Text("مثال: أحمد محمد علي إبراهيم", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PharaohNavy,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (nameParts.isNotEmpty()) {
                        Text(
                            text = "${nameParts.size}/4",
                            fontSize = 11.sp,
                            color = if (isQuadruple) Color(0xFF10B981) else Color(0xFFE11D48),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                },
                singleLine = true,
                enabled = !uiState.isSavingToSupabase,
                shape = RoundedCornerShape(12.dp),
                colors = PharaohTextFieldStyles.colors(
                    focusedBorderColor = if (isQuadruple) Color(0xFF10B981) else Color(0xFFCBD5E1),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_supabase_fullname")
            )

            if (nameParts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                if (isQuadruple) {
                    Text(
                        text = "✓ الاسم رباعي مكتمل وصحيح",
                        fontSize = 11.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    Text(
                        text = "⚠️ يُشترط إدخال الاسم رباعياً (الاسم، الأب، الجد، العائلة) - (${nameParts.size} أجزاء من 4)",
                        fontSize = 11.sp,
                        color = Color(0xFFE11D48),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Field 2: الرقم القومي (national_id) - 14 رقم مصري
            OutlinedTextField(
                value = uiState.supabaseNationalId,
                onValueChange = { viewModel.updateSupabaseNationalId(it) },
                label = { Text("الرقم القومي المصري (14 رقماً - فريد) *", fontSize = 12.sp) },
                placeholder = { Text("مثال: 30401010101234", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = PharaohNavy,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Text(
                        text = "${uiState.supabaseNationalId.length}/14",
                        fontSize = 11.sp,
                        color = if (uiState.supabaseNationalId.length == 14 && uiState.supabaseNationalIdInfo?.isValid == true) {
                            Color(0xFF10B981)
                        } else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !uiState.isSavingToSupabase,
                shape = RoundedCornerShape(12.dp),
                colors = PharaohTextFieldStyles.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_supabase_national_id")
            )

            // National ID live info or error
            uiState.supabaseNationalIdInfo?.let { info ->
                Spacer(modifier = Modifier.height(6.dp))
                if (info.isValid) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تاريخ الميلاد: ${info.birthDate} • ${info.governorate} • ${info.gender}",
                                fontSize = 11.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (info.errorMessage != null) {
                    Text(
                        text = "⚠️ ${info.errorMessage}",
                        fontSize = 11.sp,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Field 3: الكلية والقسم / التخصص
            Text(
                text = "الكلية والتخصص الأكاديمي:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PharaohNavyDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Department.entries.forEach { dept ->
                    FilterChip(
                        selected = uiState.supabaseDepartment == dept,
                        onClick = { viewModel.updateSupabaseDepartment(dept) },
                        label = { Text(dept.arabicName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        enabled = !uiState.isSavingToSupabase,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PharaohNavyDark,
                            selectedLabelColor = PharaohGoldLight,
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF0F172A)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Field 4: الفرق الدراسية الأربعة كاملة
            Text(
                text = "الفرقة الدراسية (المستوى الجامعي):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PharaohNavyDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                YearGroup.entries.forEach { year ->
                    FilterChip(
                        selected = uiState.supabaseAcademicYear == year,
                        onClick = { viewModel.updateSupabaseAcademicYear(year) },
                        label = { Text(year.arabicName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        enabled = !uiState.isSavingToSupabase,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F766E),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF0F172A)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Field 5: رقم الهاتف (phone)
            OutlinedTextField(
                value = uiState.supabasePhone,
                onValueChange = { viewModel.updateSupabasePhone(it) },
                label = { Text("رقم الهاتف المحمول (11 رقماً مصرياً) *", fontSize = 12.sp) },
                placeholder = { Text("01012345678", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = PharaohNavy,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.isPhoneVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                enabled = !uiState.isSavingToSupabase,
                shape = RoundedCornerShape(12.dp),
                colors = PharaohTextFieldStyles.colors(
                    focusedBorderColor = if (uiState.isPhoneVerified) Color(0xFF10B981) else Color(0xFF0284C7),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_supabase_phone")
            )

            // Phone Carrier badge or error
            if (uiState.supabasePhone.length == 11) {
                val prefix = uiState.supabasePhone.take(3)
                val carrier = when (prefix) {
                    "010" -> "شبكة فودافون مصر"
                    "011" -> "شبكة اتصالات مصر"
                    "012" -> "شبكة أورنج مصر"
                    "015" -> "شبكة المصرية للاتصالات (WE)"
                    else -> null
                }
                if (carrier != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📱 $carrier",
                        fontSize = 11.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            if (uiState.supabasePhoneError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ ${uiState.supabasePhoneError}",
                    fontSize = 11.sp,
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Phone verification status
            if (uiState.isPhoneVerified) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFECFDF5),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تم تأكيد ملكية رقم الهاتف عبر رسالة SMS بنجاح ✓",
                            fontSize = 11.sp,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (uiState.supabasePhone.length == 11 && uiState.supabasePhoneError == null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0F9FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "سيتم إرسال رسالة SMS للتحقق من الرقم",
                                fontSize = 11.sp,
                                color = Color(0xFF0369A1),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(
                            onClick = { viewModel.requestPhoneVerification() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("إرسال الرمز", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                    }
                }
            }

            // Student Code Generated Preview
            if (uiState.supabaseGeneratedStudentCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PharaohGold.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PharaohGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = PharaohGoldDark, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("كود الطالب الجامعي المُولد:", fontSize = 12.sp, color = PharaohNavyDark, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = uiState.supabaseGeneratedStudentCode,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PharaohNavyDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Warning or Error Box
            AnimatedVisibility(visible = uiState.supabaseErrorMessage != null) {
                uiState.supabaseErrorMessage?.let { errMsg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errMsg,
                                color = Color(0xFFB91C1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Loading Progress Bar
            AnimatedVisibility(visible = uiState.isSavingToSupabase) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = "جاري الاتصال وحفظ السجل في سحابة Supabase...",
                        fontSize = 11.sp,
                        color = Color(0xFF047857),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFFD1FAE5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save Button
            Button(
                onClick = { viewModel.registerStudentToSupabase() },
                enabled = !uiState.isSavingToSupabase,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isPhoneVerified) Color(0xFF10B981) else Color(0xFF0284C7),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_supabase_student")
            ) {
                if (uiState.isSavingToSupabase) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الحفظ في السحابة...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else if (!uiState.isPhoneVerified) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال رسالة التحقق SMS وإتمام القيد", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إتمام القيد وتوليد كود الطالب", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * حوار نجاح تسجيل الطالب في سحابة Supabase
 */
@Composable
fun SupabaseSuccessDialog(
    student: SupabaseStudent,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تم القيد بنجاح في Supabase!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = PharaohNavyDark
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "تم إدراج بيانات الطالب رسمياً في جدول (student) السحابي، وأصبح بإمكانك الدخول الفوري وعرض الكارنيه الجامعي:",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الاسم:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            Text(student.fullName, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الرقم القومي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            Text(student.nationalId, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFF0F172A))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الفرقة والتخصص:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            Text(student.academicYear, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("رقم الهاتف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            Text(student.phone, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color(0xFF0F172A))
                        }
                        student.id?.let { idVal ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("معرف السجل (ID):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                Text("#$idVal", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = PharaohGoldDark)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Text("الانتقال إلى الكارنيه الجامعي", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}

/**
 * بطاقة عرض طالب من قائمة Supabase
 */
@Composable
fun SupabaseStudentCard(
    student: SupabaseStudent,
    onQuickLogin: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text(student.academicYear, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = "ID: ${student.id ?: "-"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(student.nationalId, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(student.phone, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { onQuickLogin(student.nationalId) },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F766E))
            ) {
                Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تسجيل الدخول بهذا الحساب وعرض الكارنيه", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * نافذة التحقق التفاعلية من رقم هاتف الطالب عبر رسالة نصية قصيرة SMS
 */
@Composable
fun PhoneVerificationDialog(
    phone: String,
    otpCode: String,
    enteredCode: String,
    countdownSeconds: Int,
    errorMessage: String?,
    onCodeChange: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager: androidx.compose.ui.platform.ClipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE0F2FE),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("التحقق من رقم الهاتف", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PharaohNavyDark)
                    Text("رسالة تأكيد نصية (SMS)", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "تم إرسال رسالة تحقق تحتوي على رمز مكون من 4 أرقام لتأكيد رقم الهاتف المحمول:",
                    fontSize = 12.sp,
                    color = Color(0xFF334155),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🇪🇬 +20 $phone",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = PharaohNavyDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                // محاكاة إشعار الرسائل النصية القصيرة SMS الواردة
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("رسالة SMS جديدة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            }
                            Text("الآن", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "أكاديمية الفراعنة: رمز التحقق الخاص بقيدك الجامعي هو: [ $otpCode ] صالح لمدة 5 دقائق.",
                            fontSize = 11.sp,
                            color = Color(0xFF1E293B),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { onCodeChange(otpCode) },
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إدخال الرمز تلقائياً ($otpCode)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("أدخل رمز التحقق (4 أرقام):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PharaohNavyDark)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = enteredCode,
                    onValueChange = onCodeChange,
                    placeholder = { Text("----", fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        letterSpacing = 8.sp,
                        color = PharaohNavyDark
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_phone_otp")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚠️ $errorMessage",
                        fontSize = 11.sp,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (countdownSeconds > 0) {
                        Text(
                            text = "إعادة الإرسال بعد: ${countdownSeconds}ث",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        TextButton(onClick = onResendClick) {
                            Text("إعادة إرسال الرمز", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onVerifyClick,
                enabled = enteredCode.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تأكيد وإكمال القيد", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF64748B))
            }
        }
    )
}

/**
 * نافذة ضبط وحذف بيانات Supabase السحابية لمسؤول النظام
 */
@Composable
fun SupabaseAdminSettingsDialog(
    hasSecretKey: Boolean,
    onSaveSecretKey: (String) -> Unit,
    onPurgeAllTestStudents: () -> Unit,
    onDismiss: () -> Unit
) {
    var secretKeyInput by remember { mutableStateOf("") }
    val clipboardManager: androidx.compose.ui.platform.ClipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var copiedSql by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFECFDF5),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("إدارة قاعدة بيانات Supabase", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PharaohNavyDark)
                    Text("صلاحيات الحذف والربط السحابي", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // حالة الاتصال
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الاتصال بسحابة Supabase متصل ونشط", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                        }
                        Text("مشروع: qenuugadpzkhicimlnek • جدول: student", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // حالة مفتاح الصلاحيات الكاملة
                if (hasSecretKey) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مفتاح Service Role مفعّل: صلاحيات الحذف السحابي غير المقيدة مفعّلة للمسؤول", fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "مفتاح Service Role (اختياري لتجاوز قيود RLS):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PharaohNavyDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = secretKeyInput,
                        onValueChange = { secretKeyInput = it },
                        placeholder = { Text("ألصق مفتاح sb_secret_... أو JWT هنا", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (secretKeyInput.isNotBlank()) {
                                onSaveSecretKey(secretKeyInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PharaohNavyDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("حفظ وتفعيل مفتاح Service Role", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // كود SQL لتفعيل الحذف بنقرة واحدة
                Text("كود تفعيل الحذف في Supabase SQL Editor:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PharaohNavyDark)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = com.example.data.supabase.SupabaseConfig.SUPABASE_ENABLE_DELETE_SQL,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(com.example.data.supabase.SupabaseConfig.SUPABASE_ENABLE_DELETE_SQL))
                                copiedSql = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (copiedSql) "تم النسخ إلى الحافظة ✓" else "نسخ كود الـ SQL لتشغيله في لوحة Supabase", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // زر تصفية الطلاب التجريبيين
                Button(
                    onClick = {
                        onPurgeAllTestStudents()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حذف وتصفية كافة الطلاب التجريبيين نهائياً", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = PharaohNavyDark, fontWeight = FontWeight.Bold)
            }
        }
    )
}
