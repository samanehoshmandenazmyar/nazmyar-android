package com.example.presentation.behavior

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.domain.model.AcademicMonth
import com.example.domain.model.BehaviorRecord
import com.example.domain.model.RankPermission
import com.example.domain.model.RecordType
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

// Class filter matching helper function
fun matchesClassFilter(st: Student, filter: String): Boolean {
    if (filter == "همه" || filter.isBlank()) return true
    val g = st.grade
    val c = st.className
    return when (filter) {
        "هفتم ۱" -> (g.contains("هفتم") || g.contains("7") || g.contains("۷")) && (c.contains("1") || c.contains("۱") || c.contains("7/1") || c.contains("۷/۱"))
        "هفتم ۲" -> (g.contains("هفتم") || g.contains("7") || g.contains("۷")) && (c.contains("2") || c.contains("۲") || c.contains("7/2") || c.contains("۷/۲"))
        "هشتم ۱" -> (g.contains("هشتم") || g.contains("8") || g.contains("۸")) && (c.contains("1") || c.contains("۱") || c.contains("8/1") || c.contains("۸/۱"))
        "هشتم ۲" -> (g.contains("هشتم") || g.contains("8") || g.contains("۸")) && (c.contains("2") || c.contains("۲") || c.contains("8/2") || c.contains("۸/۲"))
        "نهم ۱" -> (g.contains("نهم") || g.contains("9") || g.contains("۹")) && (c.contains("1") || c.contains("۱") || c.contains("9/1") || c.contains("۹/۱"))
        "نهم ۲" -> (g.contains("نهم") || g.contains("9") || g.contains("۹")) && (c.contains("2") || c.contains("۲") || c.contains("9/2") || c.contains("۹/۲"))
        else -> g.contains(filter, ignoreCase = true) || c.contains(filter, ignoreCase = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    // 1. Authorization & Privacy Check (Confidential)
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
                            text = "این پرونده حاوی گزارش‌ها و سوابق انضباطی محرمانه است. دانش‌آموزان و اولیا به این فرم دسترسی ندارند.",
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
    var selectedStudent by remember(initialStudentId, allStudents) {
        val initial = if (!initialStudentId.isNullOrBlank()) {
            allStudents.firstOrNull { it.id == initialStudentId }
        } else null
        mutableStateOf<Student?>(initial)
    }

    // List view states
    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf("همه") }

    val classFilterChips = listOf("همه", "هفتم ۱", "هفتم ۲", "هشتم ۱", "هشتم ۲", "نهم ۱", "نهم ۲")

    // Filtered Students for the list view
    val filteredStudents = remember(allStudents, searchQuery, selectedClassFilter) {
        allStudents.filter { st ->
            val matchesSearch = searchQuery.isBlank() ||
                st.fullName.contains(searchQuery, ignoreCase = true) ||
                st.studentCode.contains(searchQuery) ||
                st.nationalId.contains(searchQuery)

            val matchesClass = matchesClassFilter(st, selectedClassFilter)

            matchesSearch && matchesClass
        }
    }

    // Student detail states (when student is selected)
    val currentAcademicMonth = remember { AcademicMonth.getCurrentMonth() }
    var selectedMonth by remember { mutableStateOf<AcademicMonth?>(currentAcademicMonth) }
    var isFullYearSelected by remember { mutableStateOf(false) }

    // Delete confirmation dialog state
    var recordToDelete by remember { mutableStateOf<BehaviorRecord?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedStudent == null) "پرونده انضباطی ماهانه" else "پرونده ${selectedStudent!!.fullName}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (selectedStudent == null) "انتخاب دانش‌آموز جهت مشاهده و مدیریت سوابق" else "سال تحصیلی ۱۴۰۵-۱۴۰۶ (${selectedStudent!!.className})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedStudent != null) {
                                selectedStudent = null
                            } else {
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
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

        // MODE 1: STUDENTS LIST VIEW (Fahrest)
        if (selectedStudent == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search & Class Filters Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "جستجوی نام یا کد دانش‌آموز...",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily)
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = null)
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Class Filter Chips
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(classFilterChips) { filter ->
                                    FilterChip(
                                        selected = selectedClassFilter == filter,
                                        onClick = { selectedClassFilter = filter },
                                        label = { Text(filter, fontFamily = VazirmatnFontFamily, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SchoolBluePrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "فهرست دانش‌آموزان (${filteredStudents.size.toPersianDigits()} نفر):",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (filteredStudents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "هیچ دانش‌آموزی با این مشخصات یافت نشد.",
                                fontFamily = VazirmatnFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredStudents) { st ->
                        val stRecords = allRecords.filter { r ->
                            r.studentId == st.id ||
                            (st.studentCode.isNotBlank() && r.studentId == st.studentCode)
                        }
                        val posCount = stRecords.count { it.type.isPositive }
                        val negCount = stRecords.count { !it.type.isPositive }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStudent = st }
                                .testTag("dossier_student_item_${st.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Student Avatar / Initials
                                    Surface(
                                        shape = CircleShape,
                                        color = SchoolBluePrimary.copy(alpha = 0.12f),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = st.fullName.take(2),
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontFamily = VazirmatnFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SchoolBluePrimary
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = st.fullName,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = st.className,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = VazirmatnFontFamily,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "کد: ${st.studentCode.toPersianDigits()}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = VazirmatnFontFamily,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                            val stHasLoggedIn = (st.id == "user_creator_hossein" || st.studentCode == "454545") || (st.hasLoggedIn && st.isRegistered)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (stHasLoggedIn) PositiveGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                                border = BorderStroke(0.5.dp, if (stHasLoggedIn) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Text(
                                                    text = if (stHasLoggedIn) "ورود یافته به سامانه ✓" else "افزوده‌شده توسط مدرسه (در انتظار ورود)",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = VazirmatnFontFamily,
                                                        color = if (stHasLoggedIn) PositiveGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = if (stHasLoggedIn) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 9.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Badges summary count
                                Column(horizontalAlignment = Alignment.End) {
                                    if (posCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PositiveGreen.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "${posCount.toPersianDigits()} رفتار مثبت",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = VazirmatnFontFamily,
                                                    color = PositiveGreen,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (negCount > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = IncidentRed.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "${negCount.toPersianDigits()} مورد انضباطی",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = VazirmatnFontFamily,
                                                    color = IncidentRed,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (posCount == 0 && negCount == 0) {
                                        Text(
                                            text = "بدون سابقه",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // MODE 2: DETAILED STUDENT DOSSIER VIEW
            val st = selectedStudent!!

            val studentRecords = remember(st, allRecords) {
                val sId = st.id
                val sCode = st.studentCode.trim()
                allRecords.filter { r ->
                    r.studentId == sId || (sCode.isNotBlank() && r.studentId == sCode)
                }
            }

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
            val disciplinaryRecords = filteredRecords.filter { !it.type.isPositive }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Card for Student Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = st.fullName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "پایه و کلاس: ${st.grade} - ${st.className} | کد دانش‌آموزی: ${st.studentCode.toPersianDigits()}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            OutlinedButton(
                                onClick = { selectedStudent = null },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("تغییر دانش‌آموز", fontFamily = VazirmatnFontFamily, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Month Filter Selector Chips
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "انتخاب بازه زمانی:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                            items(AcademicMonth.ACADEMIC_YEAR_ORDER) { month ->
                                val isSelected = !isFullYearSelected && selectedMonth == month
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
                                            if (monthCount > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else SchoolBluePrimary.copy(alpha = 0.15f)
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

                // SECTION 1: POSITIVE ITEMS (موارد مثبت)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PositiveGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "موارد مثبت (${positiveRecords.size.toPersianDigits()})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = PositiveGreen
                                )
                            )
                        }
                    }
                }

                if (positiveRecords.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "هیچ رفتار مثبتی برای این بازه ثبت نشده است.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(positiveRecords) { rec ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PositiveGreen.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "تعداد مثبت: ${(rec.points ?: 1).toPersianDigits()}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = PositiveGreen
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "زمان انتشار و ثبت گزارش: ${rec.date} ساعت ${rec.time}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { recordToDelete = rec },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = IncidentRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = rec.title.ifBlank { rec.description },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                if (rec.description.isNotBlank() && rec.description != rec.title) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rec.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                if (!rec.imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    RecordImageThumbnail(
                                        imageUrlOrBase64 = rec.imageUrl,
                                        onClick = { previewImageUrl = rec.imageUrl }
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ثبت توسط: ${rec.createdBy}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                // SECTION 2: DISCIPLINARY ITEMS (موارد انضباطی)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Report,
                                contentDescription = null,
                                tint = IncidentRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "موارد انضباطی (${disciplinaryRecords.size.toPersianDigits()})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = IncidentRed
                                )
                            )
                        }
                    }
                }

                if (disciplinaryRecords.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "هیچ مورد انضباطی برای این بازه ثبت نشده است.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(disciplinaryRecords) { rec ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, IncidentRed.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = IncidentRed.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = rec.category.ifBlank { "مورد انضباطی" },
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = IncidentRed
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "زمان انتشار و ثبت گزارش: ${rec.date} ساعت ${rec.time}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { recordToDelete = rec },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = IncidentRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = rec.title.ifBlank { rec.description },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                if (rec.description.isNotBlank() && rec.description != rec.title) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rec.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                if (!rec.imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    RecordImageThumbnail(
                                        imageUrlOrBase64 = rec.imageUrl,
                                        onClick = { previewImageUrl = rec.imageUrl }
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ثبت توسط: ${rec.createdBy}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (recordToDelete != null) {
        val targetRec = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = {
                Text(
                    text = "تأیید حذف از پرونده انضباطی",
                    fontFamily = VazirmatnFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "آیا از حذف «${targetRec.title.ifBlank { targetRec.category }}» از پرونده این دانش‌آموز اطمینان دارید؟ این سابقه به طور کامل پاک خواهد شد.",
                    fontFamily = VazirmatnFontFamily
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mainViewModel.deleteBehaviorRecord(targetRec.id)
                        recordToDelete = null
                        Toast.makeText(context, "مورد موردنظر با موفقیت از پرونده حذف شد.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncidentRed)
                ) {
                    Text("حذف شود", fontFamily = VazirmatnFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("انصراف", fontFamily = VazirmatnFontFamily)
                }
            }
        )
    }

    // Zoomable Image Viewer Dialog
    if (!previewImageUrl.isNullOrBlank()) {
        com.example.presentation.components.ZoomableImageViewerDialog(
            imageUrlOrBase64 = previewImageUrl,
            onDismiss = { previewImageUrl = null }
        )
    }
}

@Composable
fun RecordImageThumbnail(
    imageUrlOrBase64: String,
    onClick: () -> Unit
) {
    val bitmap: ImageBitmap? = androidx.compose.runtime.remember(imageUrlOrBase64) {
        if (imageUrlOrBase64.startsWith("data:image") || imageUrlOrBase64.length > 500) {
            try {
                val cleanBase64 = if (imageUrlOrBase64.contains(",")) {
                    imageUrlOrBase64.substringAfter(",")
                } else {
                    imageUrlOrBase64
                }
                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bmp?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "تصویر پیوست رفتار",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = imageUrlOrBase64,
                    contentDescription = "تصویر پیوست رفتار",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "بزرگ‌نمایی و زوم",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = VazirmatnFontFamily
                    )
                }
            }
        }
    }
}

