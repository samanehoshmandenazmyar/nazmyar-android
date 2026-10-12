package com.example.presentation.ranks

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.runtime.mutableStateListOf
import com.example.domain.model.CustomRank
import com.example.domain.model.RankPermission
import com.example.domain.model.Student
import com.example.domain.model.toPersianDigits
import com.example.presentation.components.RgbColorMixerDialog
import com.example.presentation.components.RgbRankBadge
import com.example.presentation.components.StudentMultiRankBadge
import com.example.presentation.components.StudentRankBadge
import com.example.presentation.components.UserRankBadge
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.MainViewModel
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.data.demo.DemoData
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.SchoolBlueContainer
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RankManagementScreen(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val students by mainViewModel.students.collectAsState()
    val customRanks by mainViewModel.customRanks.collectAsState()

    LaunchedEffect(Unit) {
        mainViewModel.refreshData()
    }

    val allUsers by authViewModel.allUsers.collectAsState()
    val staffUsers = remember(allUsers) {
        val list = allUsers.filter { it.role != UserRole.STUDENT && it.role != UserRole.PARENT }
        if (list.isNotEmpty()) list else DemoData.demoUsers
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var staffSearchQuery by remember { mutableStateOf("") }

    // Dialog state for assigning rank to a student
    var studentForRankAssignment by remember { mutableStateOf<Student?>(null) }

    // Dialog state for assigning rank and permissions to school staff
    var userForRankAssignment by remember { mutableStateOf<User?>(null) }

    // Dialog state for creating a new reusable custom rank
    var showCreateRankDialog by remember { mutableStateOf(false) }

    // Dialog state for creator modifying own ranks directly
    var showCreatorOwnRankDialog by remember { mutableStateOf(false) }

    // Dialog state for directly assigning a saved rank template to a student
    var rankTemplateForDirectAssign by remember { mutableStateOf<CustomRank?>(null) }

    // Dialog state for displaying congratulatory confirmation on screen when rank is assigned
    var successCelebrationInfo by remember { mutableStateOf<RankCelebrationData?>(null) }

    val isAuthorized = currentUser?.isCreator == true
    if (!isAuthorized) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = IncidentRed,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "دسترسی اختصاصی سازنده سامانه",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "این بخش منحصراً برای حساب کاربری شما (مدیر سامانه: سید حسین موسوی نژاد / hosseinvexter@gmail.com) جهت مدیریت رنک‌ها و تعیین اختیارات دانش‌آموزان در نظر گرفته شده است.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("بازگشت به داشبورد")
            }
        }
        return
    }

    // Filter students
    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
                it.studentCode.contains(searchQuery) ||
                it.className.contains(searchQuery, ignoreCase = true)
        }
    }

    val presetColors = listOf(
        "#8B5CF6" to "بنفش سلطنتی",
        "#2563EB" to "آبی دیپلمات",
        "#10B981" to "سبز زمردی",
        "#F97316" to "نارنجی تند",
        "#EC4899" to "صورتی درخشان",
        "#F59E0B" to "طلایی",
        "#EF4444" to "قرمز آتشی",
        "#06B6D4" to "فیروزه‌ای",
        "#0D9488" to "یشمی",
        "#64748B" to "طوسی رسمی",
        "#1E293B" to "مشکی سربی"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("rank_management_screen")
    ) {
        // App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("rank_mgmt_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = SchoolBluePrimary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "مدیریت رنک‌ها و اختیارات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        RgbRankBadge(text = "پنل سازنده", isSmall = true)
                    }
                    Text(
                        text = "بخش اختصاصی کاربر: ${currentUser?.name ?: "سید حسین موسوی نژاد"} (hosseinvexter@gmail.com)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = SchoolBluePrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "دانش‌آموزان (${students.size.toPersianDigits()})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مدیران و کادر (${staffUsers.size.toPersianDigits()})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "رنک‌های آماده (${customRanks.size.toPersianDigits()})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        // TAB 0: STUDENTS LIST & RANK ASSIGNMENT
        if (selectedTab == 0) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SchoolBluePrimary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("rank_student_search_input")
                )

                if (filteredStudents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "دانش‌آموزی یافت نشد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredStudents, key = { it.id }) { student ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rank_student_card_${student.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(SchoolBluePrimary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = student.fullName.take(2),
                                                    fontWeight = FontWeight.Bold,
                                                    color = SchoolBluePrimary
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(
                                                    text = student.fullName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "کد: ${student.studentCode.toPersianDigits()} | ${student.grade} - ${student.className}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Current Rank Badge
                                        StudentRankBadge(
                                            rankTitle = student.rankTitle,
                                            rankColorHex = student.rankColorHex,
                                            isLarge = false
                                        )
                                    }

                                    // Display active permissions if any
                                    if (student.permissions.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            student.permissions.forEach { perm ->
                                                val label = when (perm) {
                                                    RankPermission.REGISTER_POSITIVE -> "ثبت تشویقی"
                                                    RankPermission.REGISTER_DISCIPLINARY -> "ثبت گزارش"
                                                    RankPermission.VIEW_REPORTS -> "مشاهده آمار"
                                                    RankPermission.ACCESS_GAMES -> "بازی شطرنج"
                                                    RankPermission.VIEW_STUDENT_PROFILES -> "پرونده دانش‌آموزان"
                                                    RankPermission.EDIT_CLASS_INFO -> "همیار کلاس"
                                                    RankPermission.SPECIAL_BADGE -> "نشان ویژه"
                                                    else -> perm
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFF1F5F9),
                                                    border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                                                ) {
                                                    Text(
                                                        text = "✦ $label",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF334155),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Button
                                    Button(
                                        onClick = { studentForRankAssignment = student },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .testTag("assign_rank_btn_${student.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "اعطای رنک و تعیین اختیارات",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }

        // TAB 1: SAVED RANKS LIBRARY
        if (selectedTab == 1) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header with Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "کتابخانه رنک‌های ذخیره‌شده",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "رنک‌های آماده را یک‌بار بسازید و سریع به هر دانش‌آموز اختصاص دهید.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showCreateRankDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                        modifier = Modifier.testTag("create_custom_rank_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "رنک جدید", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customRanks, key = { it.id }) { rank ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("saved_rank_card_${rank.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StudentRankBadge(
                                            rankTitle = rank.title,
                                            rankColorHex = rank.colorHex,
                                            isLarge = true
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = rank.colorHex,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Delete action (if not base student rank)
                                    if (rank.id != "rank_student") {
                                        IconButton(
                                            onClick = { mainViewModel.deleteCustomRank(rank.id) },
                                            modifier = Modifier.testTag("delete_rank_${rank.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف رنک",
                                                tint = IncidentRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                if (rank.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = rank.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (rank.permissions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rank.permissions.forEach { perm ->
                                            val label = when (perm) {
                                                RankPermission.REGISTER_POSITIVE -> "ثبت تشویقی"
                                                RankPermission.REGISTER_DISCIPLINARY -> "ثبت گزارش"
                                                RankPermission.VIEW_REPORTS -> "مشاهده آمار"
                                                RankPermission.ACCESS_GAMES -> "بازی شطرنج"
                                                RankPermission.VIEW_STUDENT_PROFILES -> "پرونده دانش‌آموزان"
                                                RankPermission.EDIT_CLASS_INFO -> "همیار کلاس"
                                                RankPermission.SPECIAL_BADGE -> "نشان ویژه"
                                                else -> perm
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFF0FDF4),
                                                border = BorderStroke(0.5.dp, Color(0xFFBBF7D0))
                                            ) {
                                                Text(
                                                    text = "✓ $label",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF166534),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Apply to Student Button
                                Button(
                                    onClick = { rankTemplateForDirectAssign = rank },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .testTag("apply_rank_${rank.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "اعطای سریع این رنک به دانش‌آموز",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(30.dp)) }
                }
            }
        }
    }

    // DIALOG: ASSIGN RANK TO STUDENT
    studentForRankAssignment?.let { targetStudent ->
        var rankNameInput by remember { mutableStateOf(targetStudent.rankTitle) }
        var rankColorInput by remember { mutableStateOf(targetStudent.rankColorHex) }
        var selectedPermissions by remember { mutableStateOf(targetStudent.permissions.toSet()) }
        var saveAsTemplate by remember { mutableStateOf(false) }

        // Multi-rank support for students
        val studentExtraRanks = remember { mutableStateListOf<String>().apply { addAll(targetStudent.extraRanks) } }
        val studentExtraColors = remember { mutableStateListOf<String>().apply { addAll(targetStudent.extraRankColors) } }
        var newExtraRankTitle by remember { mutableStateOf("") }
        var newExtraRankColor by remember { mutableStateOf("#8B5CF6") }

        // RGB Mixer Dialog State
        var showRgbMixerForPrimaryRank by remember { mutableStateOf(false) }
        var showRgbMixerForExtraRank by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { studentForRankAssignment = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SchoolBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اعطای رنک و دسترسی به ${targetStudent.fullName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Quick Pick from Saved Ranks
                    Text(
                        text = "انتخاب سریع از رنک‌های آماده:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customRanks.forEach { rank ->
                            FilterChip(
                                selected = rankNameInput == rank.title && rankColorInput == rank.colorHex,
                                onClick = {
                                    rankNameInput = rank.title
                                    rankColorInput = rank.colorHex
                                    selectedPermissions = rank.permissions.toSet()
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val c = try {
                                            Color(android.graphics.Color.parseColor(if (rank.colorHex.startsWith("#")) rank.colorHex else "#${rank.colorHex}"))
                                        } catch (_: Exception) {
                                            Color(0xFF64748B)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = rank.title, fontSize = 12.sp)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Customization fields
                    Text(
                        text = "مشخصات رنک اصلی:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rankNameInput,
                        onValueChange = { rankNameInput = it },
                        label = { Text("نام رنک اصلی") },
                        placeholder = { Text("مثال: مبصر کلاس، سرگروه، همیار انضباطی...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_rank_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Color palette chips
                    Text(
                        text = "انتخاب یا ترکیب رنگ رنک:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetColors.forEach { (hex, name) ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = rankColorInput.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { rankColorInput = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Color Hex & RGB Color Mixer Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = rankColorInput,
                            onValueChange = { rankColorInput = it },
                            label = { Text("کد رنگی (HEX)") },
                            placeholder = { Text("مثال: #8B5CF6") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                val c = try {
                                    Color(android.graphics.Color.parseColor(if (rankColorInput.startsWith("#")) rankColorInput else "#$rankColorInput"))
                                } catch (_: Exception) {
                                    Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(1.dp, Color.Gray, CircleShape)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dialog_rank_color_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showRgbMixerForPrimaryRank = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ترکیب RGB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // MULTIPLE RANKS SECTION (اعطای چند رنک همزمان)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "اعطای چند رنک همزمان به این دانش‌آموز:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (studentExtraRanks.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    studentExtraRanks.forEachIndexed { idx, extraR ->
                                        val cHex = studentExtraColors.getOrNull(idx) ?: "#8B5CF6"
                                        val cColor = try {
                                            Color(android.graphics.Color.parseColor(if (cHex.startsWith("#")) cHex else "#$cHex"))
                                        } catch (_: Exception) {
                                            Color(0xFF8B5CF6)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = cColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, cColor)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = extraR,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = cColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "حذف",
                                                    tint = cColor,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            studentExtraRanks.removeAt(idx)
                                                            if (idx < studentExtraColors.size) {
                                                                studentExtraColors.removeAt(idx)
                                                            }
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Add Extra Rank Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newExtraRankTitle,
                                    onValueChange = { newExtraRankTitle = it },
                                    placeholder = { Text("نام رنک دوم/سوم...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = { showRgbMixerForExtraRank = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(newExtraRankColor)))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ColorLens,
                                        contentDescription = "رنگ RGB",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Button(
                                    onClick = {
                                        if (newExtraRankTitle.isNotBlank()) {
                                            studentExtraRanks.add(newExtraRankTitle.trim())
                                            studentExtraColors.add(newExtraRankColor)
                                            newExtraRankTitle = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Permissions Checkboxes
                    Text(
                        text = "اختیارات و دسترسی‌های این رنک:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    RankPermission.all.forEach { (permKey, permDesc) ->
                        val isChecked = selectedPermissions.contains(permKey)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPermissions = if (isChecked) {
                                        selectedPermissions - permKey
                                    } else {
                                        selectedPermissions + permKey
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedPermissions = if (checked) {
                                        selectedPermissions + permKey
                                    } else {
                                        selectedPermissions - permKey
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = SchoolBluePrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = permDesc, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Checkbox save as template
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveAsTemplate = !saveAsTemplate }
                    ) {
                        Checkbox(
                            checked = saveAsTemplate,
                            onCheckedChange = { saveAsTemplate = it },
                            colors = CheckboxDefaults.colors(checkedColor = SchoolBluePrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ذخیره در رنک‌های آماده برای استفاده‌های بعدی",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Preview Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "پیش‌نمایش نشان رنک:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudentRankBadge(
                                    rankTitle = rankNameInput.ifBlank { "پیش‌نمایش" },
                                    rankColorHex = rankColorInput,
                                    isLarge = true
                                )
                                studentExtraRanks.forEachIndexed { idx, extraR ->
                                    val cHex = studentExtraColors.getOrNull(idx) ?: "#8B5CF6"
                                    StudentRankBadge(
                                        rankTitle = extraR,
                                        rankColorHex = cHex,
                                        isLarge = true
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = rankNameInput.trim().ifBlank { "دانش‌آموز" }
                        val finalColor = rankColorInput.trim().ifBlank { "#64748B" }
                        val finalPerms = selectedPermissions.toList()

                        mainViewModel.assignMultipleRanksToStudent(
                            studentId = targetStudent.id,
                            ranks = listOf(finalTitle) + studentExtraRanks.toList(),
                            colors = listOf(finalColor) + studentExtraColors.toList(),
                            permissions = finalPerms
                        )

                        if (saveAsTemplate && finalTitle.isNotBlank()) {
                            mainViewModel.saveCustomRank(
                                CustomRank(
                                    id = "rank_${UUID.randomUUID().toString().take(8)}",
                                    title = finalTitle,
                                    colorHex = finalColor,
                                    permissions = finalPerms,
                                    description = "رنک ایجاد شده توسط سازنده برای $finalTitle"
                                )
                            )
                        }

                        studentForRankAssignment = null
                        successCelebrationInfo = RankCelebrationData(
                            studentName = targetStudent.fullName,
                            rankTitle = finalTitle,
                            colorHex = finalColor,
                            permissions = finalPerms
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_assign_rank_button")
                ) {
                    Text(text = "اعمال و ثبت رنک‌ها", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentForRankAssignment = null }) {
                    Text("انصراف")
                }
            }
        )

        if (showRgbMixerForPrimaryRank) {
            RgbColorMixerDialog(
                initialColorHex = rankColorInput,
                onColorSelected = {
                    rankColorInput = it
                    showRgbMixerForPrimaryRank = false
                },
                onDismiss = { showRgbMixerForPrimaryRank = false }
            )
        }

        if (showRgbMixerForExtraRank) {
            RgbColorMixerDialog(
                initialColorHex = newExtraRankColor,
                onColorSelected = {
                    newExtraRankColor = it
                    showRgbMixerForExtraRank = false
                },
                onDismiss = { showRgbMixerForExtraRank = false }
            )
        }
    }

    // DIALOG: CREATE NEW REUSABLE CUSTOM RANK
    if (showCreateRankDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newColor by remember { mutableStateOf("#8B5CF6") }
        var newDesc by remember { mutableStateOf("") }
        var newPermissions by remember { mutableStateOf(setOf<String>()) }

        AlertDialog(
            onDismissRequest = { showCreateRankDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = SchoolBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ایجاد رنک آماده جدید",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("نام رنک") },
                        placeholder = { Text("مثال: همیار علمی، قهرمان ورزشی...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_rank_title_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("توضیحات کوتاه") },
                        placeholder = { Text("وظایف و توضیحات این رنک...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "انتخاب رنگ:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetColors.forEach { (hex, name) ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = newColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { newColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newColor,
                        onValueChange = { newColor = it },
                        label = { Text("کد رنگ دلخواه (HEX)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "اختیارات این رنک:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    RankPermission.all.forEach { (permKey, permDesc) ->
                        val isChecked = newPermissions.contains(permKey)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    newPermissions = if (isChecked) newPermissions - permKey else newPermissions + permKey
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    newPermissions = if (checked) newPermissions + permKey else newPermissions - permKey
                                },
                                colors = CheckboxDefaults.colors(checkedColor = SchoolBluePrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = permDesc, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preview
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "پیش‌نمایش نشان:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            StudentRankBadge(
                                rankTitle = newTitle.ifBlank { "رنک جدید" },
                                rankColorHex = newColor,
                                isLarge = true
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newTitle.trim().ifBlank { "رنک سفارشی" }
                        val color = newColor.trim().ifBlank { "#8B5CF6" }
                        mainViewModel.saveCustomRank(
                            CustomRank(
                                id = "rank_${UUID.randomUUID().toString().take(8)}",
                                title = title,
                                colorHex = color,
                                permissions = newPermissions.toList(),
                                description = newDesc.trim()
                            )
                        )
                        showCreateRankDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_create_rank_button")
                ) {
                    Text(text = "ذخیره در کتابخانه رنک‌ها", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRankDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // DIALOG: DIRECTLY ASSIGN SAVED TEMPLATE TO A STUDENT
    rankTemplateForDirectAssign?.let { template ->
        var studentSearch by remember { mutableStateOf("") }
        val selectableStudents = remember(students, studentSearch) {
            if (studentSearch.isBlank()) students
            else students.filter {
                it.fullName.contains(studentSearch, ignoreCase = true) ||
                    it.studentCode.contains(studentSearch) ||
                    it.className.contains(studentSearch, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { rankTemplateForDirectAssign = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SchoolBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اعطای رنک آماده به دانش‌آموز",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    // Selected Template Info Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "رنک انتخابی:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            StudentRankBadge(
                                rankTitle = template.title,
                                rankColorHex = template.colorHex,
                                isLarge = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "یک دانش‌آموز را برای دریافت فوری این رنک انتخاب کنید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = studentSearch,
                        onValueChange = { studentSearch = it },
                        placeholder = { Text("جستجو...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("direct_assign_student_search")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(selectableStudents, key = { it.id }) { s ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mainViewModel.assignRankToStudent(
                                            studentId = s.id,
                                            rankTitle = template.title,
                                            colorHex = template.colorHex,
                                            permissions = template.permissions
                                        )
                                        successCelebrationInfo = RankCelebrationData(
                                            studentName = s.fullName,
                                            rankTitle = template.title,
                                            colorHex = template.colorHex,
                                            permissions = template.permissions
                                        )
                                        rankTemplateForDirectAssign = null
                                    }
                                    .testTag("pick_student_${s.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = s.fullName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "کد: ${s.studentCode.toPersianDigits()} | ${s.className}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            mainViewModel.assignRankToStudent(
                                                studentId = s.id,
                                                rankTitle = template.title,
                                                colorHex = template.colorHex,
                                                permissions = template.permissions
                                            )
                                            successCelebrationInfo = RankCelebrationData(
                                                studentName = s.fullName,
                                                rankTitle = template.title,
                                                colorHex = template.colorHex,
                                                permissions = template.permissions
                                            )
                                            rankTemplateForDirectAssign = null
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                                    ) {
                                        Text(text = "انتخاب", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { rankTemplateForDirectAssign = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // DIALOG: CELEBRATION & SUCCESS FEEDBACK ON SCREEN
    if (successCelebrationInfo != null) {
        val info = successCelebrationInfo!!
        AlertDialog(
            onDismissRequest = { successCelebrationInfo = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🎉 تبریک! رنک با موفقیت اعمال شد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "رنک «${info.rankTitle}» با موفقیت برای «${info.studentName}» ثبت و در سرور ذخیره شد.",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF065F46)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "کلیه دسترسی‌های این رنک به صورت بلادرنگ برای حساب کاربری دانش‌آموز فعال گردید و پیام تبریک به همراه اختیارات اعطا شده روی صفحه وی نمایش داده خواهد شد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF047857)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "نشان ثبت‌شده:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        StudentRankBadge(rankTitle = info.rankTitle, rankColorHex = info.colorHex, isLarge = true)
                    }

                    Text(
                        text = "دسترسی‌های فعال‌شده (${info.permissions.size.toPersianDigits()} مورد):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )

                    if (info.permissions.isEmpty()) {
                        Text(
                            text = "نشان افتخاری بدون دسترسی اجرایی",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        info.permissions.forEach { permKey ->
                            val (title, _) = when (permKey) {
                                RankPermission.REGISTER_POSITIVE -> Pair("ثبت رفتار مثبت برای هم‌کلاسی‌ها (همیار معلم)", "")
                                RankPermission.REGISTER_DISCIPLINARY -> Pair("ثبت گزارش و تذکر انضباطی کلاسی", "")
                                RankPermission.VIEW_REPORTS -> Pair("مشاهده آمار و گزارش‌های انضباطی", "")
                                RankPermission.ACCESS_GAMES -> Pair("دسترسی به بازی‌ها (شطرنج)", "")
                                RankPermission.VIEW_STUDENT_PROFILES -> Pair("مشاهده پرونده و سوابق دانش‌آموزان", "")
                                RankPermission.EDIT_CLASS_INFO -> Pair("همیار کلاسی (مدیریت مشخصات کلاس)", "")
                                RankPermission.SPECIAL_BADGE -> Pair("نمایش نشان ویژه و برجسته سامانه", "")
                                else -> Pair(permKey, "")
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PositiveGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { successCelebrationInfo = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("متوجه شدم، بسیار عالی ✨", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }
}

data class RankCelebrationData(
    val studentName: String,
    val rankTitle: String,
    val colorHex: String,
    val permissions: List<String>
)
