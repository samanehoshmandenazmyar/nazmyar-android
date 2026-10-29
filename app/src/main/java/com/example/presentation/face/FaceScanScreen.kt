package com.example.presentation.face

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.auth.AccessControl
import com.example.core.face.FaceMatchResult
import com.example.core.face.FaceRecognitionManager
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.matchesQuery
import com.example.domain.model.toPersianDigits
import com.example.presentation.behavior.QuickBehaviorRecordSheet
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.theme.WarningAmber

import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Report

@Composable
fun FaceScanScreen(
    currentUser: User?,
    mainViewModel: MainViewModel,
    onNavigateToPositive: (studentId: String, isScanLocked: Boolean) -> Unit,
    onNavigateToDisciplinary: (studentId: String, isScanLocked: Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FaceRecognitionManager.init(context)
    }

    val students by mainViewModel.allStudents.collectAsState()

    // Access check
    val canAccess = remember(currentUser) {
        AccessControl.canManageFacesAndBehaviors(currentUser)
    }

    if (!canAccess) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = IncidentRed,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "عدم دسترسی به اسکن چهره",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AccessControl.getAccessDeniedMessage(currentUser),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontFamily = VazirmatnFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                ) {
                    Text("بازگشت به صفحه اصلی", fontFamily = VazirmatnFontFamily)
                }
            }
        }
        return
    }

    // Scanning states
    var scanResult by remember { mutableStateOf<FaceMatchResult?>(null) }
    var confirmedStudent by remember { mutableStateOf<Student?>(null) }
    var showManualSearchDialog by remember { mutableStateOf(false) }
    var isProcessingFace by remember { mutableStateOf(false) }

    // If student identity is confirmed, show the Action Selection Screen (Positive vs Disciplinary)
    if (confirmedStudent != null) {
        val student = confirmedStudent!!
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.testTag("scan_action_selection_screen")
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PositiveGreen,
                    modifier = Modifier
                        .size(64.dp)
                        .background(PositiveGreen.copy(alpha = 0.12f), CircleShape)
                        .padding(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "هویت دانش‌آموز تأیید شد",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PositiveGreen,
                    fontFamily = VazirmatnFontFamily
                )

                Spacer(modifier = Modifier.height(16.dp))

                StudentInfoCard(student = student)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "لطفاً نوع اقدام را انتخاب کنید:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Positive Behavior
                Card(
                    onClick = {
                        onNavigateToPositive(student.id, true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_positive_from_scan"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PositiveGreen.copy(alpha = 0.1f)),
                    border = BorderStroke(1.5.dp, PositiveGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "ثبت رفتار مثبت",
                            tint = PositiveGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "ثبت رفتار مثبت",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PositiveGreen,
                                fontFamily = VazirmatnFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "اعطای نمره، آفرین و ثبت تشویقی",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = VazirmatnFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Disciplinary Incident
                Card(
                    onClick = {
                        onNavigateToDisciplinary(student.id, true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_disciplinary_from_scan"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = IncidentRed.copy(alpha = 0.1f)),
                    border = BorderStroke(1.5.dp, IncidentRed)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Report,
                            contentDescription = "ثبت مورد انضباطی",
                            tint = IncidentRed,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "ثبت مورد انضباطی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncidentRed,
                                fontFamily = VazirmatnFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ثبت تخلف، کسر نمره و اطلاع‌رسانی اولیاء",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = VazirmatnFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        confirmedStudent = null
                        scanResult = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اسکن یا جستجوی دانش‌آموز دیگر", fontFamily = VazirmatnFontFamily)
                }
            }
        }
    } else if (scanResult != null) {
        // If we have a scan result to review
        FaceResultReviewScreen(
            result = scanResult!!,
            onConfirm = { student ->
                confirmedStudent = student
            },
            onRescan = {
                scanResult = null
            },
            onManualSearch = {
                showManualSearchDialog = true
            },
            onBack = onBackClick
        )
    } else {
        // Camera Scanning Viewfinder
        Box(modifier = Modifier.fillMaxSize()) {
            FaceCaptureCameraView(
                title = "اسکن سریع چهره دانش‌آموز",
                subtitle = "چهره دانش‌آموز را مقابل دوربین قرار داده و شاتر را لمس کنید",
                onBitmapCaptured = { bitmap ->
                    isProcessingFace = true
                    val result = FaceRecognitionManager.identifyFace(bitmap, students)
                    isProcessingFace = false
                    scanResult = result
                },
                onCancel = onBackClick
            )

            // Manual Search Button at Bottom Left
            Surface(
                onClick = { showManualSearchDialog = true },
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("manual_search_trigger_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "جستجوی دستی",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "جستجوی دستی دانش‌آموز",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = VazirmatnFontFamily
                    )
                }
            }

            // Enrolled count badge at Top Center
            val count = FaceRecognitionManager.getEnrolledCount()
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                Text(
                    text = "تعداد چهره‌های ثبت‌شده: ${count.toPersianDigits()}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = VazirmatnFontFamily,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }

    // Manual Search Dialog
    if (showManualSearchDialog) {
        ManualStudentSearchDialog(
            students = students,
            onStudentSelected = { selected ->
                showManualSearchDialog = false
                confirmedStudent = selected
            },
            onDismiss = { showManualSearchDialog = false }
        )
    }
}

@Composable
fun FaceResultReviewScreen(
    result: FaceMatchResult,
    onConfirm: (Student) -> Unit,
    onRescan: () -> Unit,
    onManualSearch: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("face_result_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (result) {
                is FaceMatchResult.Match -> {
                    // Confirmed Match with High Confidence
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PositiveGreen,
                        modifier = Modifier
                            .size(72.dp)
                            .background(PositiveGreen.copy(alpha = 0.12f), CircleShape)
                            .padding(16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "دانش‌آموز شناسایی شد",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PositiveGreen,
                        fontFamily = VazirmatnFontFamily
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PositiveGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "میزان تطابق چهره: ${result.confidencePercentage.toPersianDigits()}٪",
                            color = PositiveGreen,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    StudentInfoCard(student = result.student)

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRescan,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("رد و اسکن مجدد", fontFamily = VazirmatnFontFamily)
                        }

                        Button(
                            onClick = { onConfirm(result.student) },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("confirm_student_identity_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تأیید هویت و ثبت رفتار", fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is FaceMatchResult.Uncertain -> {
                    // Uncertain match
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier
                            .size(72.dp)
                            .background(WarningAmber.copy(alpha = 0.12f), CircleShape)
                            .padding(16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "شناسایی نامطمئن (نیاز به بررسی)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber,
                        fontFamily = VazirmatnFontFamily
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "میزان شباهت چهره قطعی نیست (${result.confidencePercentage.toPersianDigits()}٪). لطفاً هویت دانش‌آموز را بررسی و در صورت صحت تأیید نمایید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontFamily = VazirmatnFontFamily,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    StudentInfoCard(student = result.student)

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRescan,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("رد و اسکن مجدد", fontFamily = VazirmatnFontFamily)
                        }

                        Button(
                            onClick = { onConfirm(result.student) },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تأیید هویت و ثبت رفتار", fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is FaceMatchResult.Unknown -> {
                    // Unknown or unregistered face
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = IncidentRed,
                        modifier = Modifier
                            .size(72.dp)
                            .background(IncidentRed.copy(alpha = 0.12f), CircleShape)
                            .padding(16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "چهره شناسایی نشد",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IncidentRed,
                        fontFamily = VazirmatnFontFamily
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "هیچ دانش‌آموزی با این چهره در سامانه یافت نشد یا چهره دانش‌آموز هنوز ثبت نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontFamily = VazirmatnFontFamily,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = onRescan,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اسکن مجدد", fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onManualSearch,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جستجوی دستی دانش‌آموز", fontFamily = VazirmatnFontFamily)
                    }
                }

                is FaceMatchResult.NoFaceDetected -> {
                    // No face found in reticle
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier
                            .size(72.dp)
                            .background(WarningAmber.copy(alpha = 0.12f), CircleShape)
                            .padding(16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "چهره‌ای تشخیص داده نشد",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber,
                        fontFamily = VazirmatnFontFamily
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = result.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontFamily = VazirmatnFontFamily,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onRescan,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تلاش مجدد", fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("بازگشت به داشبورد", fontFamily = VazirmatnFontFamily)
            }
        }
    }
}

@Composable
fun StudentInfoCard(student: Student) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = SchoolBluePrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = student.fullName.take(2),
                            fontWeight = FontWeight.Bold,
                            color = SchoolBluePrimary,
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = student.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirmatnFontFamily
                    )
                    Text(
                        text = "${student.grade} - ${student.className}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = VazirmatnFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "کد دانش‌آموزی: ${student.studentCode.toPersianDigits()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = VazirmatnFontFamily
                )
                Text(
                    text = "${student.positiveCount.toPersianDigits()}+  |  ${student.disciplinaryCount.toPersianDigits()}-",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = SchoolBluePrimary,
                    fontFamily = VazirmatnFontFamily
                )
            }
        }
    }
}

@Composable
fun ManualStudentSearchDialog(
    students: List<Student>,
    onStudentSelected: (Student) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students.take(15)
        else students.filter { it.matchesQuery(searchQuery) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("جستجوی دستی دانش‌آموز", fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("نام یا کد دانش‌آموزی...", fontFamily = VazirmatnFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(filteredStudents) { st ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onStudentSelected(st) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = st.fullName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                    Text(
                                        text = "${st.grade} - ${st.className}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                }
                                Text(
                                    text = st.studentCode.toPersianDigits(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SchoolBluePrimary,
                                    fontFamily = VazirmatnFontFamily
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("بستن", fontFamily = VazirmatnFontFamily)
            }
        }
    )
}
