package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.toPersianDigits
import com.example.ui.theme.SchoolBluePrimary
import kotlin.math.roundToInt

data class PresetPaletteColor(
    val hex: String,
    val name: String,
    val r: Int,
    val g: Int,
    val b: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RgbColorMixerDialog(
    initialColorHex: String = "#8B5CF6",
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse initial RGB
    val parsedColor = remember(initialColorHex) {
        try {
            val clean = if (initialColorHex.startsWith("#")) initialColorHex else "#$initialColorHex"
            val colorInt = android.graphics.Color.parseColor(clean)
            Triple(
                android.graphics.Color.red(colorInt),
                android.graphics.Color.green(colorInt),
                android.graphics.Color.blue(colorInt)
            )
        } catch (_: Exception) {
            Triple(139, 92, 246)
        }
    }

    var redValue by remember { mutableFloatStateOf(parsedColor.first.toFloat()) }
    var greenValue by remember { mutableFloatStateOf(parsedColor.second.toFloat()) }
    var blueValue by remember { mutableFloatStateOf(parsedColor.third.toFloat()) }

    // Multi-color selected list for blending
    val selectedPaletteColors = remember { mutableStateListOf<PresetPaletteColor>() }

    val presetColors = remember {
        listOf(
            PresetPaletteColor("#EF4444", "قرمز آتشی", 239, 68, 68),
            PresetPaletteColor("#F97316", "نارنجی نئون", 249, 115, 22),
            PresetPaletteColor("#F59E0B", "طلایی شاهانه", 245, 158, 11),
            PresetPaletteColor("#10B981", "سبز زمردی", 16, 185, 129),
            PresetPaletteColor("#06B6D4", "فیروزه‌ای اقیانوسی", 6, 182, 212),
            PresetPaletteColor("#3B82F6", "آبی دیپلمات", 59, 130, 246),
            PresetPaletteColor("#1D4ED8", "آبی کاربنی", 29, 78, 216),
            PresetPaletteColor("#8B5CF6", "بنفش رویال", 139, 92, 246),
            PresetPaletteColor("#D946EF", "ارغوانی درخشان", 217, 70, 239),
            PresetPaletteColor("#EC4899", "صورتی کریستالی", 236, 72, 153),
            PresetPaletteColor("#0F172A", "سیاه کهکشانی", 15, 23, 42),
            PresetPaletteColor("#64748B", "طوسی متالیک", 100, 116, 139)
        )
    }

    val rInt = redValue.roundToInt().coerceIn(0, 255)
    val gInt = greenValue.roundToInt().coerceIn(0, 255)
    val bInt = blueValue.roundToInt().coerceIn(0, 255)

    val currentHex = String.format("#%02X%02X%02X", rInt, gInt, bInt)
    val currentColor = Color(rInt, gInt, bInt)

    // Blend selected colors helper
    fun blendSelectedColors() {
        if (selectedPaletteColors.isNotEmpty()) {
            val avgR = selectedPaletteColors.map { it.r }.average().roundToInt().toFloat()
            val avgG = selectedPaletteColors.map { it.g }.average().roundToInt().toFloat()
            val avgB = selectedPaletteColors.map { it.b }.average().roundToInt().toFloat()
            redValue = avgR
            greenValue = avgG
            blueValue = avgB
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(currentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = if ((rInt + gInt + bInt) > 400) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "ترکیب رنگ RGB و سفارشی‌سازی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "انتخاب چند رنگ و ترکیب همزمان آنها",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Live Color Display Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.5.dp, currentColor.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Color Swatch Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(currentColor)
                                .border(2.dp, Color.White, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "کد رنگی: $currentHex",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if ((rInt + gInt + bInt) > 380) Color.Black else Color.White
                            )
                        }

                        // Gradient bar if multiple colors are selected
                        if (selectedPaletteColors.size >= 2) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val gradientColors = selectedPaletteColors.map { Color(it.r, it.g, it.b) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.horizontalGradient(gradientColors))
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "طیف ترکیبی ${selectedPaletteColors.size.toPersianDigits()} رنگ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preview Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudentRankBadge(
                                rankTitle = "پیش‌نمایش رنک",
                                rankColorHex = currentHex,
                                isLarge = true
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = currentColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, currentColor)
                            ) {
                                Text(
                                    text = "RGB($rInt, $gInt, $bInt)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = currentColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 1: MULTI-COLOR MIXER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = SchoolBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "انتخاب چند رنگ برای ترکیب:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (selectedPaletteColors.isNotEmpty()) {
                        TextButton(
                            onClick = { selectedPaletteColors.clear() },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("پاک کردن", fontSize = 11.sp)
                        }
                    }
                }

                Text(
                    text = "می‌توانید ۲ یا چند رنگ مختلف را انتخاب کنید تا با هم ترکیب شوند و رنگ RGB جدیدی بسازند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { paletteColor ->
                        val isSelected = selectedPaletteColors.any { it.hex == paletteColor.hex }
                        val chipColor = Color(paletteColor.r, paletteColor.g, paletteColor.b)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) chipColor.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) chipColor else Color.LightGray.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    if (isSelected) {
                                        selectedPaletteColors.removeAll { it.hex == paletteColor.hex }
                                    } else {
                                        selectedPaletteColors.add(paletteColor)
                                    }
                                    blendSelectedColors()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(chipColor)
                                        .border(1.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if ((paletteColor.r + paletteColor.g + paletteColor.b) > 400) Color.Black else Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = paletteColor.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (selectedPaletteColors.size >= 2) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ترکیب ${selectedPaletteColors.size.toPersianDigits()} رنگ با موفقیت ایجاد شد! (میانگین RGB کانال‌ها)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 2: MANUAL RGB SLIDERS
                Text(
                    text = "تنظیم دستی مؤلفه‌های RGB:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Red Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "قرمز (Red)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            text = rInt.toPersianDigits(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                    Slider(
                        value = redValue,
                        onValueChange = {
                            redValue = it
                            selectedPaletteColors.clear()
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFDC2626),
                            activeTrackColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.testTag("slider_red")
                    )
                }

                // Green Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "سبز (Green)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = gInt.toPersianDigits(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                    Slider(
                        value = greenValue,
                        onValueChange = {
                            greenValue = it
                            selectedPaletteColors.clear()
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF16A34A),
                            activeTrackColor = Color(0xFF22C55E)
                        ),
                        modifier = Modifier.testTag("slider_green")
                    )
                }

                // Blue Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "آبی (Blue)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB)
                        )
                        Text(
                            text = bInt.toPersianDigits(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                    Slider(
                        value = blueValue,
                        onValueChange = {
                            blueValue = it
                            selectedPaletteColors.clear()
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2563EB),
                            activeTrackColor = Color(0xFF3B82F6)
                        ),
                        modifier = Modifier.testTag("slider_blue")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(currentHex) },
                colors = ButtonDefaults.buttonColors(containerColor = currentColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_rgb_color_button")
            ) {
                Text(
                    text = "تأیید و اعمال رنگ RGB",
                    fontWeight = FontWeight.Bold,
                    color = if ((rInt + gInt + bInt) > 380) Color.Black else Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
