package com.example.presentation.face

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.auth.AccessControl
import com.example.core.face.CandidateMatch
import com.example.core.face.FaceRecognitionManager
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.presentation.behavior.QuickBehaviorRecordSheet
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.theme.WarningAmber

private enum class EnrollmentFilter {
    ALL,
    NOT_ENROLLED,
    ENROLLED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceEnrollmentScreen(
    currentUser: User?,
    mainViewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FaceRecognitionManager.init(context)
    }

    // Strictly enforce Access Control: Students and parents cannot access
    val canAccess = remember(currentUser) {
        AccessControl.canManageFacesAndBehaviors(currentUser)
    }

    if (!canAccess) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "ثبت چهره دانش‌آموزان",
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = AccessControl.getAccessDeniedMessage(currentUser),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            fontFamily = VazirmatnFontFamily
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("بازگشت", fontFamily = VazirmatnFontFamily)
                        }
                    }
                }
            }
        }
        return
    }

    val students by mainViewModel.students.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(EnrollmentFilter.ALL) }
    var selectedClass by remember { mutableStateOf<String?>(null) }

    // State trigger to re-read enrolled cache
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Modals & Sheets
    var studentForEnrollment by remember { mutableStateOf<Student?>(null) }
    var studentForDeleteConfirm by remember { mutableStateOf<Student?>(null) }
    var studentForBehaviorSheet by remember { mutableStateOf<Student?>(null) }
    var showLiveMatchTestDialog by remember { mutableStateOf(false) }

    val enrolledCount = remember(students, refreshTrigger) {
        students.count { FaceRecognitionManager.isFaceEnrolled(it.id) }
    }
    val notEnrolledCount = (students.size - enrolledCount).coerceAtLeast(0)
    val enrollmentRatio = if (students.isNotEmpty()) enrolledCount.toFloat() / students.size.toFloat() else 0f

    val classes = remember(students) {
        students.map { it.className }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredStudents = remember(students, searchQuery, selectedFilter, selectedClass, refreshTrigger) {
        students.filter { student ->
            val matchesSearch = searchQuery.isBlank() ||
                student.fullName.contains(searchQuery, ignoreCase = true) ||
                student.studentCode.contains(searchQuery)

            val isEnrolled = FaceRecognitionManager.isFaceEnrolled(student.id)
            val matchesFilter = when (selectedFilter) {
                EnrollmentFilter.ALL -> true
                EnrollmentFilter.NOT_ENROLLED -> !isEnrolled
                EnrollmentFilter.ENROLLED -> isEnrolled
            }

            val matchesClass = selectedClass == null || student.className == selectedClass

            matchesSearch && matchesFilter && matchesClass
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "مرکز ثبت چهره دانش‌آموزان",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily
                        )
                        Text(
                            text = "تشخیص دقیق چهره اصلی و اتصال به پرونده",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = VazirmatnFontFamily
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("face_enrollment_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showLiveMatchTestDialog = true },
                        modifier = Modifier.testTag("face_enrollment_top_match_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "تست تطبیق زنده چهره",
                            tint = SchoolBluePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("face_enrollment_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats & Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("face_enrollment_stats_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SchoolBluePrimary.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "وضعیت ثبت چهره‌های مدرسه",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SchoolBluePrimary,
                                fontFamily = VazirmatnFontFamily
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SchoolBluePrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${((enrollmentRatio * 100).toInt()).toPersianDigits()}% تکمیل",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolBluePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontFamily = VazirmatnFontFamily
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { enrollmentRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = SchoolBluePrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatMiniItem(
                                label = "کل دانش‌آموزان",
                                value = students.size.toPersianDigits(),
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatMiniItem(
                                label = "چهره ثبت‌شده",
                                value = enrolledCount.toPersianDigits(),
                                iconColor = PositiveGreen
                            )
                            StatMiniItem(
                                label = "نیاز به ثبت",
                                value = notEnrolledCount.toPersianDigits(),
                                iconColor = if (notEnrolledCount > 0) WarningAmber else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Quick Live Test Button Card
            item {
                Surface(
                    onClick = { showLiveMatchTestDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_face_match_test_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SchoolBluePrimary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "تست تطبیق و یافتن چهره اصلی",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Text(
                                    text = "دوربین را روبروی دانش‌آموز بگیرید تا چهره اصلی بین تمام افراد پیدا شود",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = VazirmatnFontFamily
                                )
                            }
                        }
                        Button(
                            onClick = { showLiveMatchTestDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تست", fontFamily = VazirmatnFontFamily, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Explanatory guidance tip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "نکته: ثبت ۲ الی ۳ زاویه مختلف (مستقیم، کمی مایل و لبخند) به برنامه کمک می‌کند حتی در شرایط نوری متغیر، چهره اصلی دانش‌آموز را به درستی تشخیص دهد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF78350F),
                        fontFamily = VazirmatnFontFamily,
                        lineHeight = 18.sp
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("face_enrollment_search_field"),
                    placeholder = {
                        Text(
                            "جستجوی نام دانش‌آموز یا کد ملی...",
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "جستجو")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Filter Chips (All, Not Enrolled, Enrolled)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == EnrollmentFilter.ALL,
                        onClick = { selectedFilter = EnrollmentFilter.ALL },
                        label = {
                            Text("همه (${students.size.toPersianDigits()})", fontFamily = VazirmatnFontFamily)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.15f),
                            selectedLabelColor = SchoolBluePrimary
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == EnrollmentFilter.NOT_ENROLLED,
                        onClick = { selectedFilter = EnrollmentFilter.NOT_ENROLLED },
                        label = {
                            Text("ثبت‌نشده (${notEnrolledCount.toPersianDigits()})", fontFamily = VazirmatnFontFamily)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarningAmber.copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFB45309)
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == EnrollmentFilter.ENROLLED,
                        onClick = { selectedFilter = EnrollmentFilter.ENROLLED },
                        label = {
                            Text("ثبت‌شده (${enrolledCount.toPersianDigits()})", fontFamily = VazirmatnFontFamily)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PositiveGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PositiveGreen
                        )
                    )
                }
            }

            // Class Filter Chips if available
            if (classes.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedClass == null,
                                onClick = { selectedClass = null },
                                label = { Text("همه کلاس‌ها", fontFamily = VazirmatnFontFamily, fontSize = 12.sp) }
                            )
                        }
                        items(classes) { className ->
                            FilterChip(
                                selected = selectedClass == className,
                                onClick = {
                                    selectedClass = if (selectedClass == className) null else className
                                },
                                label = { Text(className, fontFamily = VazirmatnFontFamily, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Empty State
            if (filteredStudents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "هیچ دانش‌آموزی با این مشخصات یافت نشد.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }
                }
            } else {
                // Students List
                items(filteredStudents, key = { it.id }) { student ->
                    StudentFaceEnrollmentRow(
                        student = student,
                        refreshKey = refreshTrigger,
                        onEnrollClick = { studentForEnrollment = student },
                        onDeleteClick = { studentForDeleteConfirm = student },
                        onQuickBehaviorClick = { studentForBehaviorSheet = student }
                    )
                }
            }
        }
    }

    // Multi-Angle Enrollment Dialog
    studentForEnrollment?.let { student ->
        MultiShotEnrollmentDialog(
            student = student,
            onDismiss = { studentForEnrollment = null },
            onEnrollSuccess = {
                refreshTrigger++
                studentForEnrollment = null
            }
        )
    }

    // Delete Confirmation Dialog
    studentForDeleteConfirm?.let { student ->
        AlertDialog(
            onDismissRequest = { studentForDeleteConfirm = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = IncidentRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "حذف الگوی چهره",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily
                )
            },
            text = {
                Text(
                    text = "آیا از حذف داده‌های بیومتریک و الگوی چهره «${student.fullName}» اطمینان دارید؟ در صورت نیاز می‌توانید چهره را مجدداً ثبت نمایید.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = VazirmatnFontFamily
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        FaceRecognitionManager.deleteFace(context, student.id)
                        refreshTrigger++
                        studentForDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncidentRed)
                ) {
                    Text("حذف چهره", fontFamily = VazirmatnFontFamily)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { studentForDeleteConfirm = null }) {
                    Text("انصراف", fontFamily = VazirmatnFontFamily)
                }
            }
        )
    }

    // Live Face Match Test Dialog
    if (showLiveMatchTestDialog) {
        LiveFaceMatchTestDialog(
            allStudents = students,
            onDismiss = { showLiveMatchTestDialog = false },
            onStudentSelectedForBehavior = { matchedStudent ->
                showLiveMatchTestDialog = false
                studentForBehaviorSheet = matchedStudent
            }
        )
    }

    // Quick Behavior Record Bottom Sheet
    studentForBehaviorSheet?.let { student ->
        QuickBehaviorRecordSheet(
            student = student,
            currentUser = currentUser,
            mainViewModel = mainViewModel,
            onDismiss = { studentForBehaviorSheet = null },
            onRecordSubmitted = {
                studentForBehaviorSheet = null
                refreshTrigger++
            }
        )
    }
}

@Composable
private fun StatMiniItem(
    label: String,
    value: String,
    iconColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = iconColor,
            fontFamily = VazirmatnFontFamily
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = VazirmatnFontFamily,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StudentFaceEnrollmentRow(
    student: Student,
    refreshKey: Int,
    onEnrollClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onQuickBehaviorClick: () -> Unit
) {
    val isEnrolled = remember(student.id, refreshKey) {
        FaceRecognitionManager.isFaceEnrolled(student.id)
    }
    val embedding = remember(student.id, refreshKey) {
        FaceRecognitionManager.getEmbedding(student.id)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_enrollment_card_${student.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isEnrolled) PositiveGreen.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with face indicator badge
                Box(modifier = Modifier.size(48.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (isEnrolled) PositiveGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isEnrolled) Icons.Default.Face else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isEnrolled) PositiveGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    if (isEnrolled) {
                        Surface(
                            shape = CircleShape,
                            color = PositiveGreen,
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isEnrolled) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PositiveGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "ثبت‌شده (${(embedding?.sampleCount ?: 1).toPersianDigits()} زاویه)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PositiveGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontFamily = VazirmatnFontFamily
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = WarningAmber.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "فاقد چهره",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontFamily = VazirmatnFontFamily
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "کلاس: ${student.className} • کد: ${student.studentCode.toPersianDigits()} • کارکرد: ${student.positiveCount.toPersianDigits()}+ | ${student.disciplinaryCount.toPersianDigits()}-",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onEnrollClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnrolled) MaterialTheme.colorScheme.secondary else SchoolBluePrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isEnrolled) Icons.Default.Refresh else Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnrolled) "بروزرسانی چهره" else "ثبت چهره",
                        fontSize = 12.sp,
                        fontFamily = VazirmatnFontFamily
                    )
                }

                if (isEnrolled) {
                    OutlinedButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IncidentRed),
                        border = BorderStroke(1.dp, IncidentRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف چهره",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onQuickBehaviorClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ثبت رفتار",
                        fontSize = 12.sp,
                        fontFamily = VazirmatnFontFamily
                    )
                }
            }
        }
    }
}

/**
 * Multi-Shot Face Enrollment Dialog
 * Enables capturing 1 to 3 distinct angles (Frontal, slight angle, smile/neutral)
 * to construct a high-precision, noise-resistant face embedding.
 */
@Composable
private fun MultiShotEnrollmentDialog(
    student: Student,
    onDismiss: () -> Unit,
    onEnrollSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isCameraActive by remember { mutableStateOf(false) }
    val capturedBitmaps = remember { mutableStateListOf<Bitmap>() }
    var currentStep by remember { mutableIntStateOf(1) } // 1, 2, or 3
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val stepPrompts = listOf(
        "زاویه ۱: روبرو و مستقیم به دوربین نگاه کنید",
        "زاویه ۲: سر را کمی به پهلو متمایل کنید (زاویه ۲۰ الی ۳۰ درجه)",
        "زاویه ۳: حالت چهره طبیعی با لبخند ملایم"
    )

    if (isCameraActive) {
        Dialog(
            onDismissRequest = { isCameraActive = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FaceCaptureCameraView(
                title = "ثبت چهره: ${student.fullName}",
                subtitle = stepPrompts.getOrElse(capturedBitmaps.size) { "چهره را در دایره نگه دارید" },
                onBitmapCaptured = { bitmap ->
                    isCameraActive = false
                    capturedBitmaps.add(bitmap)
                    if (capturedBitmaps.size < 3) {
                        currentStep = capturedBitmaps.size + 1
                    }
                },
                onCancel = { isCameraActive = false }
            )
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "ثبت چهره: ${student.fullName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily
                )
                Text(
                    text = "کلاس ${student.className} • کد: ${student.studentCode.toPersianDigits()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = VazirmatnFontFamily
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ثبت چند زاویه باعث می‌شود برنامه حتی در زوایای مختلف بتواند چهره اصلی این دانش‌آموز را میان بقیه همکلاسی‌ها دقیق تشخیص دهد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = VazirmatnFontFamily,
                    lineHeight = 18.sp
                )

                // Preview slots for the 3 samples
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0 until 3) {
                        val bitmap = capturedBitmaps.getOrNull(i)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (bitmap != null) PositiveGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (bitmap != null) PositiveGreen else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "نمونه ${(i + 1).toPersianDigits()}",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "زاویه ${(i + 1).toPersianDigits()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (bitmap != null) PositiveGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (bitmap != null) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }
                }

                errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IncidentRed.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = IncidentRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            fontFamily = VazirmatnFontFamily
                        )
                    }
                }

                // Camera capture button
                Button(
                    onClick = { isCameraActive = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (capturedBitmaps.isEmpty()) "عکس‌برداری زاویه اول" else "عکس‌برداری زاویه بعدی (${capturedBitmaps.size.toPersianDigits()} از ۳)",
                        fontFamily = VazirmatnFontFamily
                    )
                }

                if (capturedBitmaps.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { capturedBitmaps.clear() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("پاک‌کردن و شروع مجدد عکس‌ها", fontFamily = VazirmatnFontFamily, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (capturedBitmaps.isEmpty()) {
                        errorMessage = "حداقل باید یک تصویر از چهره ثبت شود."
                        return@Button
                    }
                    isSaving = true
                    val success = FaceRecognitionManager.enrollMultiShotFace(
                        context = context,
                        studentId = student.id,
                        bitmaps = capturedBitmaps
                    )
                    isSaving = false
                    if (success) {
                        onEnrollSuccess()
                    } else {
                        errorMessage = "تشخیص چهره در تصاویر با خطا مواجه شد. لطفاً در نور کافی و روبرو مجدد تلاش نمایید."
                    }
                },
                enabled = capturedBitmaps.isNotEmpty() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("ذخیره نهایی چهره اصلی", fontFamily = VazirmatnFontFamily)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("انصراف", fontFamily = VazirmatnFontFamily)
            }
        }
    )
}

/**
 * Live Face Match Testing Dialog
 * Lets staff verify how the system detects and ranks the student's true face among all enrolled faces.
 */
@Composable
private fun LiveFaceMatchTestDialog(
    allStudents: List<Student>,
    onDismiss: () -> Unit,
    onStudentSelectedForBehavior: (Student) -> Unit
) {
    var isCameraActive by remember { mutableStateOf(true) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var matches by remember { mutableStateOf<List<CandidateMatch>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }

    if (isCameraActive) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FaceCaptureCameraView(
                title = "تست تشخیص چهره اصلی",
                subtitle = "دوربین را روبروی دانش‌آموز قرار دهید تا هویت او بررسی شود",
                onBitmapCaptured = { bitmap ->
                    isCameraActive = false
                    capturedBitmap = bitmap
                    isAnalyzing = true
                    matches = FaceRecognitionManager.findTopMatches(bitmap, allStudents, topK = 3)
                    isAnalyzing = false
                },
                onCancel = onDismiss
            )
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "نتیجه تست تشخیص چهره",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = VazirmatnFontFamily
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isAnalyzing) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SchoolBluePrimary)
                    }
                } else if (matches.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "هیچ چهره‌ای با تطابق کافی میان دانش‌آموزان ثبت‌شده یافت نشد یا چهره واضح نیست.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }
                } else {
                    val topMatch = matches.first()
                    if (topMatch.isTopMatch) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PositiveGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.5.dp, PositiveGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PositiveGreen, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "چهره اصلی شناسایی شد:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PositiveGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Surface(shape = RoundedCornerShape(6.dp), color = PositiveGreen) {
                                        Text(
                                            text = "${topMatch.confidencePct.toPersianDigits()}% اطمینان",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontFamily = VazirmatnFontFamily
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = topMatch.student.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Text(
                                    text = "کلاس: ${topMatch.student.className} • کد ملی: ${topMatch.student.studentCode.toPersianDigits()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onStudentSelectedForBehavior(topMatch.student) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تأیید و ثبت رفتار برای این دانش‌آموز", fontFamily = VazirmatnFontFamily, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WarningAmber.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, WarningAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "شناسایی نامطمئن (تطابق کمتر از حد قطعیت)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "نزدیک‌ترین احتمال: ${topMatch.student.fullName} با ضریب ${topMatch.confidencePct.toPersianDigits()}٪",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F),
                                    fontFamily = VazirmatnFontFamily
                                )
                            }
                        }
                    }

                    // Secondary Candidate Matches
                    if (matches.size > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "سایر احتمالات مقایسه‌شده:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = VazirmatnFontFamily
                        )
                        matches.drop(1).forEach { cand ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${cand.student.fullName} (${cand.student.className})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Text(
                                    text = "${cand.confidencePct.toPersianDigits()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontFamily = VazirmatnFontFamily
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { isCameraActive = true },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تست مجدد دوربین", fontFamily = VazirmatnFontFamily)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("بستن", fontFamily = VazirmatnFontFamily)
            }
        }
    )
}
