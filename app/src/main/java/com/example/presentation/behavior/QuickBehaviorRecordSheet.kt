package com.example.presentation.behavior

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.auth.AccessControl
import com.example.core.speech.PersianSpeechToTextManager
import com.example.core.speech.SpeechState
import com.example.domain.model.RecordType
import com.example.domain.model.SeverityLevel
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.model.toPersianDigits
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.IncidentRedContainer
import com.example.ui.theme.OnIncidentRedContainer
import com.example.ui.theme.OnPositiveGreenContainer
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenContainer
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickBehaviorRecordSheet(
    student: Student,
    currentUser: User?,
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit,
    onRecordSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Check Access Permission
    val hasPermission = remember(currentUser) {
        AccessControl.canManageFacesAndBehaviors(currentUser)
    }

    if (!hasPermission) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("عدم دسترسی", fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = AccessControl.getAccessDeniedMessage(currentUser),
                    fontFamily = VazirmatnFontFamily
                )
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                ) {
                    Text("متوجه شدم", fontFamily = VazirmatnFontFamily)
                }
            }
        )
        return
    }

    // Form states
    var isPositive by remember { mutableStateOf(true) }

    val positiveCategories = listOf("مشارکت و تلاش کلاسی", "انضباط و اخلاق نیکو", "فعالیت پرورشی و فرهنگی", "موفقیت در آزمون", "همیاری هم‌کلاسی‌ها")
    val negativeCategories = listOf("بی‌نظمی در کلاس", "تأخیر در ورود", "غیبت غیرموجه", "عدم انجام تکالیف", "رفتار نامناسب با هم‌کلاسی")

    var selectedCategory by remember(isPositive) {
        mutableStateOf(if (isPositive) positiveCategories.first() else negativeCategories.first())
    }

    var selectedPoints by remember(isPositive) {
        mutableIntStateOf(if (isPositive) 2 else -2)
    }

    var title by remember(isPositive, selectedCategory) {
        mutableStateOf(selectedCategory)
    }

    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("کلاس درس") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Speech To Text Manager
    val speechManager = remember { PersianSpeechToTextManager(context) }
    val speechState by speechManager.speechState.collectAsState()
    val partialText by speechManager.partialText.collectAsState()
    val soundLevel by speechManager.soundLevel.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            speechManager.stopListening()
        }
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            var baseDesc = description
            speechManager.startListening(
                onPartial = { partial ->
                    description = if (baseDesc.isBlank()) partial else "$baseDesc $partial"
                },
                onResult = { result ->
                    description = if (baseDesc.isBlank()) result else "$baseDesc $result"
                    baseDesc = description
                }
            )
        } else {
            errorMessage = "مجوز استفاده از میکروفون برای تبدیل گفتار تأیید نشد. می‌توانید متن را تایپ نمایید."
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            speechManager.stopListening()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("quick_behavior_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Student Info Badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ثبت رفتار برای:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = VazirmatnFontFamily
                        )
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = VazirmatnFontFamily
                        )
                        Text(
                            text = "${student.grade} - ${student.className} (کد: ${student.studentCode.toPersianDigits()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = VazirmatnFontFamily
                        )
                    }

                    Surface(
                        color = SchoolBluePrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "کارکرد: ${student.positiveCount.toPersianDigits()}+ | ${student.disciplinaryCount.toPersianDigits()}-",
                            color = SchoolBluePrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Positive / Negative Toggle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        isPositive = true
                        selectedCategory = positiveCategories.first()
                        selectedPoints = 2
                        title = positiveCategories.first()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("toggle_positive_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPositive) PositiveGreen else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isPositive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "رفتار مثبت / تشویق",
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = if (isPositive) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Button(
                    onClick = {
                        isPositive = false
                        selectedCategory = negativeCategories.first()
                        selectedPoints = -2
                        title = negativeCategories.first()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("toggle_negative_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isPositive) IncidentRed else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!isPositive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Report,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مورد / تذکر انضباطی",
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = if (!isPositive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection Chips
            Text(
                text = "دسته‌بندی و نوع رفتار:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = VazirmatnFontFamily
            )
            Spacer(modifier = Modifier.height(8.dp))

            val currentCategories = if (isPositive) positiveCategories else negativeCategories
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentCategories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = cat
                            title = cat
                        },
                        label = {
                            Text(
                                text = cat,
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isPositive) PositiveGreenContainer else IncidentRedContainer,
                            selectedLabelColor = if (isPositive) OnPositiveGreenContainer else OnIncidentRedContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Points selection
            Text(
                text = if (isPositive) "تعداد موارد مثبت:" else "تعداد موارد منفی:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = VazirmatnFontFamily
            )
            Spacer(modifier = Modifier.height(8.dp))

            val pointOptions = if (isPositive) listOf(1, 2, 5, 10) else listOf(-1, -2, -5, -10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pointOptions.forEach { pts ->
                    val isSelected = selectedPoints == pts
                    val label = if (pts > 0) "+${pts.toPersianDigits()}" else pts.toPersianDigits()
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clickable { selectedPoints = pts },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            if (isPositive) PositiveGreen else IncidentRed
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$label نمره",
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description with Persian Voice Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "توضیحات و شرح واقعه:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily
                )

                // Speech To Text Button
                val isListening = speechState is SpeechState.Listening
                val isProcessing = speechState is SpeechState.Processing

                Surface(
                    onClick = {
                        if (isListening || isProcessing) {
                            speechManager.stopListening()
                        } else {
                            if (!hasAudioPermission) {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                var baseDesc = description
                                speechManager.startListening(
                                    onPartial = { partial ->
                                        description = if (baseDesc.isBlank()) partial else "$baseDesc $partial"
                                    },
                                    onResult = { recognized ->
                                        description = if (baseDesc.isBlank()) recognized else "$baseDesc $recognized"
                                        baseDesc = description
                                    }
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isListening) IncidentRed else SchoolBluePrimary,
                    modifier = Modifier.testTag("speech_to_text_mic_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "تبدیل گفتار فارسی به متن",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isListening -> "در حال ضبط... (توقف)"
                                isProcessing -> "در حال پردازش..."
                                else -> "تبدیل گفتار به متن"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Live voice feedback indicator if active
            AnimatedVisibility(visible = speechState is SpeechState.Listening || speechState is SpeechState.Processing) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = SchoolBluePrimary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = SchoolBluePrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "لطفاً توضیحات را به زبان فارسی بیان کنید...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SchoolBluePrimary,
                                fontFamily = VazirmatnFontFamily
                            )
                            if (partialText.isNotBlank()) {
                                Text(
                                    text = "« $partialText »",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = VazirmatnFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // Error message from speech if any
            if (speechState is SpeechState.Error) {
                val err = (speechState as SpeechState.Error).message
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    color = IncidentRedContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = err,
                        color = OnIncidentRedContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = VazirmatnFontFamily,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("behavior_description_input"),
                placeholder = {
                    Text(
                        "توضیحات تکمیلی رفتار دانش‌آموز را بنویسید یا با دکمه بالا صحبت کنید...",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = VazirmatnFontFamily
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Final Confirm and Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        speechManager.stopListening()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("انصراف", fontFamily = VazirmatnFontFamily)
                }

                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        val trimmedTitle = title.trim().ifBlank { selectedCategory }
                        val trimmedDesc = description.trim().ifBlank { "ثبت شده از طریق اسکن سریع چهره" }

                        isSubmitting = true
                        speechManager.stopListening()

                        val recordType = if (isPositive) {
                            if (selectedCategory.contains("موفقیت")) RecordType.ACHIEVEMENT else RecordType.POSITIVE
                        } else {
                            if (selectedPoints <= -5) RecordType.DISCIPLINARY else RecordType.WARNING
                        }

                        val severity = when {
                            selectedPoints <= -10 -> SeverityLevel.CRITICAL
                            selectedPoints <= -5 -> SeverityLevel.HIGH
                            selectedPoints < 0 -> SeverityLevel.MEDIUM
                            else -> SeverityLevel.LOW
                        }

                        val creatorName = currentUser?.name ?: "مسئول سامانه"
                        val creatorRole = currentUser?.role ?: UserRole.VICE_PRINCIPAL

                        mainViewModel.registerBehaviorRecord(
                            studentId = student.id,
                            type = recordType,
                            category = selectedCategory,
                            title = trimmedTitle,
                            description = trimmedDesc,
                            location = location,
                            severity = severity,
                            points = selectedPoints,
                            createdBy = creatorName,
                            createdByRole = creatorRole,
                            parentNotified = true
                        )

                        isSubmitting = false
                        onRecordSubmitted()
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("confirm_submit_behavior_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPositive) PositiveGreen else IncidentRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تأیید نهایی و ثبت گزارش",
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
