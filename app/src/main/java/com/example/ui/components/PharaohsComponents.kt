package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
import com.example.ui.theme.PharaohTerracotta

@Composable
fun PharaohsHeader(
    modifier: Modifier = Modifier,
    title: String = "الفراعنة",
    subtitle: String = "المنصة التعليمية لطلاب الجامعة"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pharaohs_header_card"),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = PharaohNavyDark),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PharaohNavyDark,
                            PharaohNavy
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_pharaohs_logo),
                            contentDescription = "شعار الفراعنة",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = PharaohGoldLight,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Decorative Hieroglyphic-inspired badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PharaohGold.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PharaohGold, PharaohGoldDark)))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "التميز الأكاديمي",
                            tint = PharaohGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "2025/2026",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PharaohGoldLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentIdCard(
    student: Student,
    modifier: Modifier = Modifier,
    onCopyCode: (() -> Unit)? = null
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val dept = Department.fromCode(student.departmentCode)
    val year = YearGroup.fromNumber(student.yearGroup)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("student_id_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PharaohGoldDark, PharaohNavyLight)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            PharaohPapyrus.copy(alpha = 0.35f)
                        )
                    )
                )
        ) {
            // Header: Republic & Institute
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PharaohNavyDark)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "جمهورية مصر العربية",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "وزارة التعليم العالي - معهد الفراعنة العالي",
                            color = PharaohGoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.ic_pharaohs_logo),
                        contentDescription = "ختم معتمد",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            // Body
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo simulation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PharaohNavyLight.copy(alpha = 0.15f))
                            .border(2.dp, PharaohGold, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "صورة الطالب",
                            tint = PharaohNavy,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "طالب نظامي",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PharaohNavy
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Info details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PharaohNavyDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PharaohNavy.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = dept.arabicName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PharaohNavy,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PharaohGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = year.arabicName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PharaohGoldDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Unique Student Code
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PharaohGoldContainer)
                            .border(1.dp, PharaohGold, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "كود الطالب: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PharaohNavyDark
                        )
                        Text(
                            text = student.studentCode,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PharaohNavy,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(student.studentCode))
                                onCopyCode?.invoke()
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("copy_student_code_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ كود الطالب",
                                tint = PharaohNavy,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Footer with barcode simulation and security stamp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "بيانات مشفرة",
                                tint = Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الرقم القومي: ${student.nationalIdMasked}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "العام الجامعي: ${student.academicYear}",
                            fontSize = 10.sp,
                            color = PharaohNavyLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Barcode graphic bars simulation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf(16, 22, 14, 26, 18, 28, 12, 24, 16, 26, 20, 14, 28, 18, 22).forEach { h ->
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(h.dp)
                                    .background(PharaohNavyDark)
                            )
                        }
                    }
                }
            }
        }
    }
}
