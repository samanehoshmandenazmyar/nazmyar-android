package com.example.presentation.behavior

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.DateTimeUtils
import com.example.domain.model.AcademicMonth
import com.example.domain.model.BehaviorRecord
import com.example.domain.model.RankPermission
import com.example.domain.model.RecordType
import com.example.domain.model.SeverityLevel
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.model.toPersianDigits
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.theme.WarningAmber
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentMonthlyDossierScreen(
    initialStudentId: String? = null,
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val allStudents by mainViewModel.students.collectAsState()
    val allRecords by mainViewModel.records.collectAsState()

    // 1. Strict Privacy & Authorization Check
    // "خودشم نبینه فقط کسانی که دسترسی دارند و بهشون دادم بتونند ببینند"
    val isAuthorized = remember(currentUser) {
        val u = currentUser ?: return@remember false
        if (u.role == UserRole.STUDENT || u.role == UserRole.PARENT) {
            false
        } else {
            u.isCreator ||
                u.role == UserRole.MANAGER ||
                u.role == UserRole.VICE_PRINCIPAL ||
                u.permissions.contains(RankPermission.VIEW_STUDENT_PROFILES) ||
                u.permissions.contains(RankPermission.REGISTER_DISCIPLINARY) ||
                u.permissions.contains(RankPermission.VIEW_REPORTS) ||
                u.role == UserRole.TEACHER ||
                u.role == UserRole.COUNSELOR
        }
    }

    if (!isAuthorized) {
        // Access Denied Screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("پرونده انضباطی دانش‌آموز", fontFamily = VazirmatnFontFamily) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, IncidentRed.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(IncidentRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = IncidentRed,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "دسترسی غیرمجاز (محرمانه)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = IncidentRed
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "این پرونده حاوی گزارش‌ها و تخلفات انضباطی محرمانه است. دانش‌آموزان و اولیا به این فرم دسترسی ندارند و تنها کادر مدیریت و معاونین مدرسه می‌توانند آن را مشاهده نمایند.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("بازگشت به صفحه قبل", fontFamily = VazirmatnFontFamily)
                        }
                    }
                }
            }
        }
        return
    }

    // Selected Student State
    var selectedStudentId by remember {
        mutableStateOf(
            initialStudentId ?: allStudents.firstOrNull()?.id ?: ""
        )
    }

    val student = allStudents.firstOrNull { it.id == selectedStudentId } ?: allStudents.firstOrNull()
    val studentRecords = remember(student, allRecords) {
        if (student == null) emptyList()
        else {
            val sId = student.id
            val sCode = student.studentCode.trim()
            val sNormName = com.example.domain.model.normalizePersianText(student.fullName)
            allRecords.filter { r ->
                r.studentId == sId ||
                (sCode.isNotBlank() && r.studentId == sCode) ||
                (r.studentName.isNotBlank() && com.example.domain.model.normalizePersianText(r.studentName) == sNormName)
            }
        }
    }

    // Iranian Academic Month Selection (Default to current Iranian month, e.g. بهمن)
    val currentAcademicMonth = remember { AcademicMonth.getCurrentMonth() }
    var selectedMonth by remember { mutableStateOf<AcademicMonth?>(currentAcademicMonth) }
    var isFullYearSelected by remember { mutableStateOf(false) }

    // Dialog state for quick addition in this month
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var quickAddIsPositive by remember { mutableStateOf(true) }

    // Filtering records by selected month or full year
    val filteredRecords = remember(studentRecords, selectedMonth, isFullYearSelected) {
        if (isFullYearSelected || selectedMonth == null) {
            studentRecords
        } else {
            studentRecords.filter { record ->
                AcademicMonth.fromDate(record.date, record.createdAt) == selectedMonth
            }
        }
    }

    val positiveRecords = filteredRecords.filter { it.type.isPositive }
    val negativeRecords = filteredRecords.filter { !it.type.isPositive }

    // Scores calculation
    val baseScore = 100
    val totalDeductions = negativeRecords.sumOf { (it.points ?: it.severity?.weight ?: 1) * 2 }
    val totalBonuses = positiveRecords.sumOf { it.points ?: 2 }
    val monthlyScore = (baseScore - totalDeductions + (totalBonuses / 2)).coerceIn(0, 100)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "فرم پرونده تربیتی و انضباطی",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "سال تحصیلی ۱۴۰۳-۱۴۰۴ (تفکیک ماهانه)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (student != null) {
                        IconButton(
                            onClick = {
                                shareOfficialDossier(context, student, selectedMonth, isFullYearSelected, filteredRecords, monthlyScore)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "اشتراک‌گذاری گزارش رسمی",
                                tint = SchoolBluePrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("student_monthly_dossier_screen")
    ) { innerPadding ->
        if (student == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("هیچ دانش‌آموزی در سامانه تعریف نشده است.", fontFamily = VazirmatnFontFamily)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Student Dropdown Selector (if viewing all or switching)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                var showStudentPicker by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "انتخاب پرونده دانش‌آموز:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, SchoolBluePrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable { showStudentPicker = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = SchoolBluePrimary.copy(alpha = 0.08f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = SchoolBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${student.fullName} (${student.className})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolBluePrimary
                                )
                            )
                        }
                    }

                    if (showStudentPicker) {
                        com.example.presentation.components.SearchableStudentPickerDialog(
                            students = allStudents,
                            selectedStudentId = selectedStudentId,
                            title = "انتخاب و جستجوی پرونده دانش‌آموز",
                            onSelectStudent = { s ->
                                selectedStudentId = s.id
                                showStudentPicker = false
                            },
                            onDismiss = { showStudentPicker = false }
                        )
                    }
                }
            }
            }

            // Student Official Dossier Profile Card (Auto-filled data)
            item {
                StudentOfficialDossierHeader(student = student, totalRecords = studentRecords.size)
            }

            // Iranian Academic Months Selector (مهر تا خرداد + تابستان)
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ماه‌های سال تحصیلی ایران:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (isFullYearSelected) "نمایش کل سال تحصیلی" else "ماه انتخابی: ${selectedMonth?.persianName ?: ""}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = SchoolBluePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Full Year Tab
                        item {
                            FilterChip(
                                selected = isFullYearSelected,
                                onClick = {
                                    isFullYearSelected = true
                                    selectedMonth = null
                                },
                                label = { Text("کل سال تحصیلی", fontFamily = VazirmatnFontFamily, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SchoolBluePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        // 12 Months of the Academic Year
                        items(AcademicMonth.ACADEMIC_YEAR_ORDER) { month ->
                            val isSelected = !isFullYearSelected && selectedMonth == month
                            val isCurrent = currentAcademicMonth == month

                            // Count records for this month
                            val monthCount = studentRecords.count {
                                AcademicMonth.fromDate(it.date, it.createdAt) == month
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    isFullYearSelected = false
                                    selectedMonth = month
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(month.persianName, fontFamily = VazirmatnFontFamily, fontSize = 12.sp)
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) Color.White else SchoolBluePrimary,
                                                modifier = Modifier.size(6.dp)
                                            ) {}
                                        }
                                        if (monthCount > 0) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) Color.White.copy(alpha = 0.25f) else SchoolBluePrimary.copy(alpha = 0.15f),
                                                modifier = Modifier.padding(1.dp)
                                            ) {
                                                Text(
                                                    text = monthCount.toPersianDigits(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp),
                                                    color = if (isSelected) Color.White else SchoolBluePrimary
                                                )
                                            }
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SchoolBluePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Monthly Summary Card & Conduct Score
            item {
                MonthlyStatusCard(
                    monthTitle = if (isFullYearSelected) "کارنامه کل سال تحصیلی" else "کارنامه ماه ${selectedMonth?.persianName ?: ""}",
                    monthlyScore = monthlyScore,
                    positiveCount = positiveRecords.size,
                    negativeCount = negativeRecords.size,
                    onAddPositiveClick = {
                        quickAddIsPositive = true
                        showQuickAddDialog = true
                    },
                    onAddNegativeClick = {
                        quickAddIsPositive = false
                        showQuickAddDialog = true
                    }
                )
            }

            // Section 1: Positive Behaviors in this month
            item {
                SectionHeader(
                    title = "رفتارهای مثبت و تشویقی (${positiveRecords.size.toPersianDigits()} مورد)",
                    icon = Icons.Default.CheckCircle,
                    tint = PositiveGreen
                )
            }

            if (positiveRecords.isEmpty()) {
                item {
                    EmptyRecordCard(
                        text = "در این ماه هیچ رفتار مثبت یا تشویقی برای دانش‌آموز ثبت نشده است.",
                        actionText = "+ ثبت تشویق برای این ماه",
                        onClick = {
                            quickAddIsPositive = true
                            showQuickAddDialog = true
                        }
                    )
                }
            } else {
                items(positiveRecords, key = { it.id }) { record ->
                    BehaviorItemCard(record = record, isPositive = true)
                }
            }

            // Section 2: Disciplinary Violations in this month
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    title = "موارد و تخلفات انضباطی (${negativeRecords.size.toPersianDigits()} مورد)",
                    icon = Icons.Default.Report,
                    tint = IncidentRed
                )
            }

            if (negativeRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PositiveGreen.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PositiveGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "عالی! هیچ مورد یا تخلف انضباطی در این ماه برای دانش‌آموز ثبت نشده است.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = PositiveGreen
                                )
                            )
                        }
                    }
                }
            } else {
                items(negativeRecords, key = { it.id }) { record ->
                    BehaviorItemCard(record = record, isPositive = false)
                }
            }
        }
    }

    // Quick Registration Dialog for this Student & Month
    if (showQuickAddDialog && student != null) {
        QuickAddBehaviorDialog(
            student = student,
            isPositive = quickAddIsPositive,
            selectedMonth = selectedMonth ?: currentAcademicMonth,
            currentUser = currentUser,
            onDismiss = { showQuickAddDialog = false },
            onSave = { title, category, description, points, severity ->
                val monthName = selectedMonth?.persianName ?: currentAcademicMonth.persianName
                val date = "۱۴۰۳/${selectedMonth?.monthNumber ?: currentAcademicMonth.monthNumber}/۱۵".toPersianDigits()
                val time = DateTimeUtils.getCurrentTime()

                mainViewModel.registerBehaviorRecord(
                    studentId = student.id,
                    type = if (quickAddIsPositive) RecordType.POSITIVE else RecordType.DISCIPLINARY,
                    category = category,
                    title = title,
                    description = description,
                    date = date,
                    time = time,
                    location = if (quickAddIsPositive) "کلاس درس" else "آموزشگاه",
                    severity = severity,
                    points = points,
                    createdBy = currentUser?.name ?: "مدیریت مدرسه",
                    createdByRole = currentUser?.role ?: UserRole.VICE_PRINCIPAL
                )
                showQuickAddDialog = false
                Toast.makeText(context, "مورد جدید با موفقیت در پرونده ماه $monthName ثبت شد.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun StudentOfficialDossierHeader(student: Student, totalRecords: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ministry / School Official Subheader
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وزارت آموزش و پرورش • سامانه نظام‌یار",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = VazirmatnFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = IncidentRed.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "🔒 پرونده محرمانه انضباطی",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            color = IncidentRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Auto-populated student particulars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SchoolBluePrimary, SchoolBluePrimary.copy(alpha = 0.75f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.fullName.take(2),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.fullName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${student.grade} - ${student.className}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            color = SchoolBluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "کد دانش‌آموزی: ${student.studentCode.toPersianDigits()}  |  کد ملی: ${(student.nationalId.ifBlank { "ثبت در سامانه" }).toPersianDigits()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Student Ranks & Badges if assigned
            if (student.allRanks.isNotEmpty() && student.allRanks != listOf("دانش‌آموز")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    student.allRanks.forEach { rank ->
                        val badgeColor = try {
                            Color(android.graphics.Color.parseColor(if (student.rankColorHex.startsWith("#")) student.rankColorHex else "#${student.rankColorHex}"))
                        } catch (_: Exception) {
                            SchoolBluePrimary
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = badgeColor, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = rank,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Family / Guardian and Birth details grid
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نام ولی: ${student.parentName.ifBlank { "ثبت در پرونده" }}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = (student.parentPhone.ifBlank { "---" }).toPersianDigits(),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تاریخ تولد: ${student.birthDate.toPersianDigits()} (${student.gender})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "کل سوابق ثبت‌شده: ${totalRecords.toPersianDigits()} مورد",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = SchoolBluePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyStatusCard(
    monthTitle: String,
    monthlyScore: Int,
    positiveCount: Int,
    negativeCount: Int,
    onAddPositiveClick: () -> Unit,
    onAddNegativeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Monthly Conduct Score Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (monthlyScore >= 90) PositiveGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نمره انضباط: ",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily)
                        )
                        Text(
                            text = "${monthlyScore.toPersianDigits()} / ۱۰۰",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (monthlyScore >= 90) PositiveGreen else WarningAmber
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two stat badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Positive stats box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PositiveGreen.copy(alpha = 0.09f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تشویق‌ها و موارد مثبت",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = PositiveGreen,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "${positiveCount.toPersianDigits()} مورد",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = PositiveGreen
                            )
                        )
                    }
                }

                // Negative stats box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = IncidentRed.copy(alpha = 0.09f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تخلفات و موارد انضباطی",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = IncidentRed,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "${negativeCount.toPersianDigits()} مورد",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = IncidentRed
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick add buttons for this month
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddPositiveClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ثبت تشویق این ماه", style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily))
                }

                Button(
                    onClick = onAddNegativeClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncidentRed)
                ) {
                    Icon(Icons.Default.RemoveCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ثبت تخلف این ماه", style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily))
                }
            }
        }
    }
}

@Composable
fun BehaviorItemCard(record: BehaviorRecord, isPositive: Boolean) {
    val accentColor = if (isPositive) PositiveGreen else IncidentRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.CheckCircle else Icons.Default.Report,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (isPositive) "+${(record.points ?: 2).toPersianDigits()} امتیاز" else "-${(record.points ?: 1).toPersianDigits()} نمره",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (record.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = VazirmatnFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دسته: ${record.category}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = VazirmatnFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = "ثبت‌کننده: ${record.createdBy}  |  ${record.date} (${record.time})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = VazirmatnFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = VazirmatnFontFamily,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun EmptyRecordCard(text: String, actionText: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = VazirmatnFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onClick) {
                Text(actionText, fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickAddBehaviorDialog(
    student: Student,
    isPositive: Boolean,
    selectedMonth: AcademicMonth,
    currentUser: User?,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, description: String, points: Int, severity: SeverityLevel) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (isPositive) "اخلاقی و انضباطی" else "انضباطی") }
    var description by remember { mutableStateOf("") }
    var points by remember { mutableIntStateOf(if (isPositive) 2 else 1) }

    val positivePresets = listOf("رعایت اخلاق حسنه", "همکاری با اولیای مدرسه", "کوشش در نظافت کلاس", "پیشرفت چشمگیر در درس", "نماز جماعت و فعالیت پرورشی")
    val negativePresets = listOf("تأخیر در ورود به مدرسه", "بی‌نظمی در کلاس درس", "عدم رعایت پوشش و آراستگی", "عدم انجام تکالیف محوله", "غیبت غیرموجه")

    val presets = if (isPositive) positivePresets else negativePresets

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPositive) "ثبت رفتار مثبت برای ماه ${selectedMonth.persianName}" else "ثبت تخلف انضباطی برای ماه ${selectedMonth.persianName}",
                fontFamily = VazirmatnFontFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "دانش‌آموز: ${student.fullName} (${student.className})",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily, color = SchoolBluePrimary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("عناوین پرتکرار و پیشنهادی:", style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily))
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets) { p ->
                        FilterChip(
                            selected = title == p,
                            onClick = { title = p },
                            label = { Text(p, fontSize = 11.sp, fontFamily = VazirmatnFontFamily) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isPositive) "عنوان رفتار مثبت *" else "عنوان تخلف انضباطی *", fontFamily = VazirmatnFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات و جزئیات (اختیاری)", fontFamily = VazirmatnFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, category, description, points, SeverityLevel.MEDIUM)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isPositive) PositiveGreen else IncidentRed)
            ) {
                Text("ثبت در پرونده ماه ${selectedMonth.persianName}", fontFamily = VazirmatnFontFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", fontFamily = VazirmatnFontFamily)
            }
        }
    )
}

private fun shareOfficialDossier(
    context: Context,
    student: Student,
    month: AcademicMonth?,
    isFullYear: Boolean,
    records: List<BehaviorRecord>,
    monthlyScore: Int
) {
    val monthTitle = if (isFullYear || month == null) "کل سال تحصیلی ۱۴۰۳-۱۴۰۴" else "ماه ${month.persianName}"
    val sb = StringBuilder()
    sb.appendLine("📋 «گزارش رسمی پرونده انضباطی و تربیتی دانش‌آموز»")
    sb.appendLine("وزارت آموزش و پرورش - سامانه مدرسه")
    sb.appendLine("----------------------------------------")
    sb.appendLine("نام و نام خانوادگی: ${student.fullName}")
    sb.appendLine("کد دانش‌آموزی: ${student.studentCode}")
    sb.appendLine("پایه و کلاس: ${student.grade} - ${student.className}")
    sb.appendLine("نام ولی: ${student.parentName} (${student.parentPhone})")
    sb.appendLine("بازه پرونده: $monthTitle")
    sb.appendLine("نمره انضباط این دوره: $monthlyScore از ۱۰۰")
    sb.appendLine("----------------------------------------")

    val positives = records.filter { it.type.isPositive }
    val negatives = records.filter { !it.type.isPositive }

    sb.appendLine("🟢 تشویق‌ها و رفتارهای مثبت (${positives.size} مورد):")
    if (positives.isEmpty()) {
        sb.appendLine(" - موردی ثبت نشده است.")
    } else {
        positives.forEachIndexed { index, r ->
            sb.appendLine(" ${index + 1}. ${r.title} | ${r.date} (+${r.points ?: 2} امتیاز)")
            if (r.description.isNotBlank()) sb.appendLine("    شرح: ${r.description}")
        }
    }

    sb.appendLine("\n🔴 تخلفات و تذکرات انضباطی (${negatives.size} مورد):")
    if (negatives.isEmpty()) {
        sb.appendLine(" - هیچ تخلفی ثبت نشده است.")
    } else {
        negatives.forEachIndexed { index, r ->
            sb.appendLine(" ${index + 1}. ${r.title} | ${r.date} (-${r.points ?: 1} نمره)")
            if (r.description.isNotBlank()) sb.appendLine("    شرح: ${r.description}")
        }
    }

    sb.appendLine("----------------------------------------")
    sb.appendLine("تاریخ صدور گزارش: ${DateTimeUtils.getCurrentPersianDate()} - ساعت ${DateTimeUtils.getCurrentTime()}")
    sb.appendLine("تأییدیه: شورای انضباطی و مدیریت آموزشگاه")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "پرونده انضباطی ${student.fullName}")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری گزارش رسمی پرونده"))
}
