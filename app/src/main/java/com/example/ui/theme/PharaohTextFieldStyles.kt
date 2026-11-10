package com.example.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ألوان حقول الإدخال فائقة التباين (High Contrast):
 * نص أسود داكن صريح #0F172A يمنع أي بهتان أو اختفاء للنصوص
 * حدود وخلفيات محددة بدقة تبرز الحقول بوضوح تام مع هوية الفراعنة الذهبية والكحلية.
 */
object PharaohTextFieldStyles {

    val DarkTextColor = Color(0xFF0F172A) // نص داكن صريح وفائق التباين
    val LabelColor = Color(0xFF1E293B) // عنوان داكن واضح جداً
    val PlaceholderColor = Color(0xFF475569) // رمادي محدد واضح
    val BorderFocused = Color(0xFF0F172A) // كحلي داكن صريح عند التركيز
    val BorderUnfocused = Color(0xFF94A3B8) // حدود محددة وواضحة عند عدم التركيز
    val CursorColor = Color(0xFFD4AF37) // ذهبي فرعوني للمؤشر

    val ContainerFocused = Color(0xFFFFFFFF) // خلفية بيضاء نقية عند التركيز
    val ContainerUnfocused = Color(0xFFF8FAFC) // خلفية رمادية فاتحة متناسقة

    @Composable
    fun colors(
        focusedTextColor: Color = DarkTextColor,
        unfocusedTextColor: Color = DarkTextColor,
        focusedContainerColor: Color = ContainerFocused,
        unfocusedContainerColor: Color = ContainerUnfocused,
        focusedBorderColor: Color = BorderFocused,
        unfocusedBorderColor: Color = BorderUnfocused,
        focusedLabelColor: Color = DarkTextColor,
        unfocusedLabelColor: Color = LabelColor,
        focusedPlaceholderColor: Color = PlaceholderColor,
        unfocusedPlaceholderColor: Color = PlaceholderColor,
        cursorColor: Color = CursorColor
    ): TextFieldColors {
        return OutlinedTextFieldDefaults.colors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            focusedContainerColor = focusedContainerColor,
            unfocusedContainerColor = unfocusedContainerColor,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unfocusedBorderColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            cursorColor = cursorColor
        )
    }
}

