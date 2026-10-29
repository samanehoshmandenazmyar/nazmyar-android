package com.example.presentation.students

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.mutableIntStateOf
import com.example.ui.theme.PositiveGreen
import kotlin.random.Random
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.example.data.demo.DemoData
import com.example.data.demo.AllowedStudentsRegistry
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.model.isValidPhoneNumber
import com.example.domain.model.toPersianDigits
import com.example.domain.model.toUser
import com.example.presentation.components.NezamYarHeader
import com.example.presentation.components.SpecialUserListItemCard
import com.example.presentation.components.StudentListItemCard
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsListScreen(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onStudentClick: (String) -> Unit,
    onNotificationClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val students by mainViewModel.students.collectAsState()
    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val selectedGrade by mainViewModel.selectedGrade.collectAsState()
    val selectedClass by mainViewModel.selectedClass.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()
    val unreadNotifs = notifications.count { !it.read }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: دانش‌آموزان, 1: افراد ویژه
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var selectedStaffUser by remember { mutableStateOf<User?>(null) }
    var selectedStaffMatchingStudent by remember { mutableStateOf<com.example.domain.model.Student?>(null) }

    // Initial on-mount refresh
    LaunchedEffect(Unit) {
        mainViewModel.refreshData()
    }

    val allUsers by authViewModel.allUsers.collectAsState()

    val filteredStudents = remember(students, searchQuery, selectedGrade) {
        students.filter { s ->
            val matchesQuery = searchQuery.isBlank() ||
                s.fullName.contains(searchQuery, ignoreCase = true) ||
                s.studentCode.contains(searchQuery) ||
                s.nationalId.contains(searchQuery)

            val matchesGrade = MainViewModel.matchesGradeAndClassFilter(s, selectedGrade)

            matchesQuery && matchesGrade
        }
    }

    // Special individuals list: explicitly includes Mehdi Faryabi, Yasin Darahki, Creator,
    // and ONLY users/students who have a rank OTHER THAN "دانش‌آموز", "پدر دانش‌آموز", or "مادر دانش‌آموز"
    val specialUsers = remember(currentUser, students, allUsers) {
        val list = mutableListOf<User>()

        fun isExcludedBaseRank(rank: String?): Boolean {
            if (rank.isNullOrBlank()) return true
            val normalized = rank.trim().replace("‌", " ").replace("\\s+".toRegex(), " ")
            return normalized == "دانش آموز" ||
                normalized == "پدر دانش آموز" ||
                normalized == "مادر دانش آموز"
        }

        val sourceUsers = if (allUsers.isNotEmpty()) {
            (allUsers + DemoData.demoUsers).distinctBy { it.id }
        } else {
            DemoData.demoUsers
        }

        // 1. Mandatory designated special persons (as requested: Mehdi Faryabi, Yasin Darahki, and Creator)
        val faryabi = sourceUsers.firstOrNull {
            it.id == "user_manager_mehdi_faryabi" || it.name.replace(" ", "") == "مهدیفاریابی"
        } ?: DemoData.demoUsers.firstOrNull { it.id == "user_manager_mehdi_faryabi" }

        val dorahaki = sourceUsers.firstOrNull {
            it.id == "user_counselor_yasin_dorahaki" || it.name.replace(" ", "") == "یاسیندراهکی"
        } ?: DemoData.demoUsers.firstOrNull { it.id == "user_counselor_yasin_dorahaki" }

        val creator = currentUser?.takeIf { it.isCreator }
            ?: sourceUsers.firstOrNull { it.isCreator }
            ?: DemoData.demoUsers.firstOrNull { it.isCreator }

        if (creator != null) list.add(creator)
        if (faryabi != null) list.add(faryabi)
        if (dorahaki != null) list.add(dorahaki)

        // 2. All other registered users whose role is staff or whose rank is special
        sourceUsers.forEach { u ->
            val isDesignated = (u.id == faryabi?.id || u.id == dorahaki?.id || u.id == creator?.id)
            if (!isDesignated) {
                val isStaff = u.role == UserRole.MANAGER ||
                    u.role == UserRole.VICE_PRINCIPAL ||
                    u.role == UserRole.COUNSELOR ||
                    u.role == UserRole.TEACHER ||
                    u.isCreator
                val hasSpecialRank = !isExcludedBaseRank(u.rankTitle) ||
                    u.extraRanks.any { !isExcludedBaseRank(it) }
                if (isStaff || hasSpecialRank) {
                    list.add(u)
                }
            }
        }

        // 3. Students from students list who have a rank other than "دانش‌آموز"
        students.forEach { s ->
            val hasSpecialRank = !isExcludedBaseRank(s.rankTitle) ||
                s.extraRanks.any { !isExcludedBaseRank(it) }
            if (hasSpecialRank) {
                list.add(s.toUser())
            }
        }

        list.distinctBy { it.id }
    }

    val filteredSpecialUsers = remember(specialUsers, searchQuery) {
        if (searchQuery.isBlank()) {
            specialUsers
        } else {
            specialUsers.filter { u ->
                u.name.contains(searchQuery, ignoreCase = true) ||
                u.studentCode.contains(searchQuery) ||
                u.rankTitle.contains(searchQuery, ignoreCase = true) ||
                u.extraRanks.any { it.contains(searchQuery, ignoreCase = true) } ||
                u.role.farsiTitle.contains(searchQuery, ignoreCase = true) ||
                u.email.contains(searchQuery, ignoreCase = true) ||
                u.phone.contains(searchQuery)
            }
        }
    }

    val grades = listOf("همه", "هفتم یک", "هفتم دو", "هشتم یک", "هشتم دو", "نهم یک", "نهم دو")

    Scaffold(
        floatingActionButton = {
            if (selectedTabIndex == 0 && (currentUser?.role == UserRole.MANAGER || currentUser?.role == UserRole.VICE_PRINCIPAL)) {
                FloatingActionButton(
                    onClick = { showAddStudentDialog = true },
                    containerColor = SchoolBluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_student_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "افزودن دانش‌آموز جدید")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("students_list_screen")
        ) {
            item {
                NezamYarHeader(
                    user = currentUser,
                    unreadNotificationCount = unreadNotifs,
                    onNotificationClick = onNotificationClick,
                    onMenuClick = onMenuClick
                )
            }

            // Title & Count Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "لیست مخاطب‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = if (selectedTabIndex == 0) {
                            "${students.size.toPersianDigits()} مخاطب"
                        } else {
                            "${filteredSpecialUsers.size.toPersianDigits()} فرد ویژه"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tabs Row (مخاطب‌ها / افراد ویژه)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = SchoolBluePrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Text(
                                    text = "مخاطب‌ها (${students.size.toPersianDigits()})",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Light
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "مخاطب‌ها",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Text(
                                    text = "افراد ویژه (${filteredSpecialUsers.size.toPersianDigits()})",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Light
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "افراد ویژه",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTabIndex == 1) SchoolBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            // Search Bar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { mainViewModel.searchQuery.value = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .testTag("student_search_input"),
                        placeholder = {
                            Text(
                                text = "جستجو...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "جستجو")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { mainViewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Grade & Status Filter Chips (Only relevant for Students tab)
            if (selectedTabIndex == 0) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            text = "فیلتر بر اساس پایه:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.testTag("grade_filter_row")
                        ) {
                            items(grades) { grade ->
                                FilterChip(
                                    selected = selectedGrade == grade,
                                    onClick = {
                                        mainViewModel.selectedGrade.value = grade
                                        mainViewModel.selectedClass.value = "همه"
                                    },
                                    label = { Text(grade) },
                                    modifier = Modifier.testTag("filter_chip_$grade")
                                )
                            }
                        }

                    }
                }
            }

            // Header info & count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTabIndex == 0) "فهرست مخاطب‌ها" else "فهرست افراد ویژه و مدیران",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedTabIndex == 0) {
                            "${filteredStudents.size.toPersianDigits()} مخاطب"
                        } else {
                            "${filteredSpecialUsers.size.toPersianDigits()} نفر"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // STUDENTS TAB
                if (filteredStudents.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "دانش‌آموزی با این مشخصات یافت نشد.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showAddStudentDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("افزودن اولین دانش‌آموز")
                                }
                            }
                        }
                    }
                } else {
                    // Student Cards
                    items(filteredStudents, key = { it.id }) { student ->
                        StudentListItemCard(
                            student = student,
                            allUsers = allUsers,
                            onClick = { onStudentClick(student.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                // SPECIAL INDIVIDUALS TAB (افراد ویژه)
                if (filteredSpecialUsers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "فرد ویژه‌ای با این مشخصات یافت نشد.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredSpecialUsers, key = { it.id }) { user ->
                        val matchingStudent = students.firstOrNull {
                            it.id == user.id ||
                            (user.studentCode.isNotBlank() && it.studentCode == user.studentCode) ||
                            (AllowedStudentsRegistry.normalize(it.fullName) == AllowedStudentsRegistry.normalize(user.name))
                        }
                        SpecialUserListItemCard(
                            user = user,
                            allUsers = allUsers,
                            matchingStudent = matchingStudent,
                            onClick = {
                                selectedStaffUser = user
                                selectedStaffMatchingStudent = matchingStudent
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }

    if (selectedStaffUser != null) {
        StaffProfileDialog(
            user = selectedStaffUser!!,
            matchingStudent = selectedStaffMatchingStudent,
            onDismiss = {
                selectedStaffUser = null
                selectedStaffMatchingStudent = null
            },
            onNavigateToStudentProfile = { studentId ->
                selectedStaffUser = null
                selectedStaffMatchingStudent = null
                onStudentClick(studentId)
            }
        )
    }

    if (showAddStudentDialog) {
        AddStudentDialog(
            onDismiss = { showAddStudentDialog = false },
            onAddPending = { firstName, lastName, grade, className ->
                mainViewModel.addPendingStudent(
                    firstName = firstName,
                    lastName = lastName,
                    grade = grade,
                    className = className
                ) {
                    showAddStudentDialog = false
                }
            },
            onAddFull = { firstName, lastName, grade, className, code, phone, email, password ->
                mainViewModel.addNewStudent(
                    firstName = firstName,
                    lastName = lastName,
                    grade = grade,
                    className = className,
                    studentCode = code,
                    parentPhone = phone,
                    email = email,
                    password = password,
                    gender = "پسر"
                ) {
                    showAddStudentDialog = false
                }
            }
        )
    }
}

@Composable
fun AddStudentDialog(
    onDismiss: () -> Unit,
    onAddPending: (firstName: String, lastName: String, grade: String, className: String) -> Unit,
    onAddFull: (firstName: String, lastName: String, grade: String, className: String, code: String, phone: String, email: String, password: String) -> Unit
) {
    var selectedMode by remember { mutableIntStateOf(0) } // 0: در انتظار ورود (فقط نام و نام خانوادگی), 1: ثبت‌نام کامل (دارای حساب)
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    val classOptions = listOf("کلاس 7/1", "کلاس 7/2", "کلاس 8/1", "کلاس 8/2", "کلاس 9/1", "کلاس 9/2")
    var selectedGradeClass by remember { mutableStateOf("کلاس 7/1") }
    var code by remember { mutableStateOf(Random.nextInt(100000, 999999).toString()) }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun generateCode() {
        code = Random.nextInt(100000, 999999).toString()
    }

    val (g, c) = when (selectedGradeClass) {
        "کلاس 7/1" -> Pair("پایه هفتم", "کلاس 7/1")
        "کلاس 7/2" -> Pair("پایه هفتم", "کلاس 7/2")
        "کلاس 8/1" -> Pair("پایه هشتم", "کلاس 8/1")
        "کلاس 8/2" -> Pair("پایه هشتم", "کلاس 8/2")
        "کلاس 9/1" -> Pair("پایه نهم", "کلاس 9/1")
        "کلاس 9/2" -> Pair("پایه نهم", "کلاس 9/2")
        else -> Pair("پایه هفتم", "کلاس 7/1")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "افزودن دانش‌آموز جدید",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily
                )
                Text(
                    text = "نوع افزودن دانش‌آموز به سامانه را انتخاب کنید:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = VazirmatnFontFamily
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode Selector Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mode 0: Pending / Unregistered (Name only)
                    Surface(
                        onClick = {
                            selectedMode = 0
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedMode == 0) SchoolBluePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            width = if (selectedMode == 0) 1.8.dp else 1.dp,
                            color = if (selectedMode == 0) SchoolBluePrimary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = if (selectedMode == 0) SchoolBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "در انتظار ورود",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMode == 0) SchoolBluePrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "فقط نام و نام خانوادگی",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Mode 1: Full Direct Registration
                    Surface(
                        onClick = {
                            selectedMode = 1
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedMode == 1) SchoolBluePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            width = if (selectedMode == 1) 1.8.dp else 1.dp,
                            color = if (selectedMode == 1) SchoolBluePrimary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HowToReg,
                                contentDescription = null,
                                tint = if (selectedMode == 1) SchoolBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "ورودیافته (حساب کامل)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMode == 1) SchoolBluePrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "با مشخصات و رمز ورود",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (selectedMode == 0) {
                    // MODE 0: UNREGISTERED / PENDING (NAME ONLY)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(0.5.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "در این حالت دانش‌آموز با عنوان «افزوده‌شده توسط مدرسه (در انتظار ورود)» در مخاطبان اضافه می‌شود و فقط نیاز به نام و نام خانوادگی دارد.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    color = Color(0xFF78350F),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Grade & Class
                    Text(
                        text = "پایه و کلاس دانش‌آموز:",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = VazirmatnFontFamily),
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(classOptions) { opt ->
                            FilterChip(
                                selected = selectedGradeClass == opt,
                                onClick = { selectedGradeClass = opt },
                                label = { Text(opt, fontFamily = VazirmatnFontFamily) }
                            )
                        }
                    }

                    // First Name
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = {
                            firstName = it.replace("دانش‌آموز", "").replace("دانش آموز", "")
                            errorMessage = null
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        label = { Text("نام دانش‌آموز", fontFamily = VazirmatnFontFamily) },
                        placeholder = { Text("مثال: علی", fontFamily = VazirmatnFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SchoolBluePrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_pending_student_fname")
                    )

                    // Last Name
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = {
                            lastName = it.replace("دانش‌آموز", "").replace("دانش آموز", "")
                            errorMessage = null
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        label = { Text("نام خانوادگی", fontFamily = VazirmatnFontFamily) },
                        placeholder = { Text("مثال: رضایی", fontFamily = VazirmatnFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SchoolBluePrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_pending_student_lname")
                    )
                } else {
                    // MODE 1: FULL DIRECT REGISTRATION
                    Text(
                        text = "پایه و کلاس دانش‌آموز:",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = VazirmatnFontFamily),
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(classOptions) { opt ->
                            FilterChip(
                                selected = selectedGradeClass == opt,
                                onClick = { selectedGradeClass = opt },
                                label = { Text(opt, fontFamily = VazirmatnFontFamily) }
                            )
                        }
                    }

                    // First Name
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = {
                            firstName = it.replace("دانش‌آموز", "").replace("دانش آموز", "")
                            errorMessage = null
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        label = { Text("نام دانش‌آموز", fontFamily = VazirmatnFontFamily) },
                        placeholder = { Text("مثال: علی", fontFamily = VazirmatnFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SchoolBluePrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_fname")
                    )

                    // Last Name
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = {
                            lastName = it.replace("دانش‌آموز", "").replace("دانش آموز", "")
                            errorMessage = null
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        label = { Text("نام خانوادگی (اختیاری)", fontFamily = VazirmatnFontFamily) },
                        placeholder = { Text("مثال: رضایی", fontFamily = VazirmatnFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SchoolBluePrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_lname")
                    )

                    // Student Code with 🔄 button
                    OutlinedTextField(
                        value = if (code.isNotEmpty()) code.toPersianDigits() else "",
                        onValueChange = {},
                        readOnly = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        label = { Text("کد دانش‌آموزی ۶ رقمی (سیستمی)", fontFamily = VazirmatnFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = SchoolBluePrimary) },
                        trailingIcon = {
                            IconButton(onClick = { generateCode() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "تولید مجدد کد", tint = SchoolBluePrimary)
                            }
                        },
                        supportingText = {
                            Text(
                                text = "کد ورود: $code (برای تغییر، دکمه بازتولید را لمس کنید)",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_code")
                    )

                    // Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            errorMessage = null
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        label = { Text("شماره تماس اولیاء", fontFamily = VazirmatnFontFamily) },
                        placeholder = { Text("مثال: 09123456789", fontFamily = VazirmatnFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SchoolBluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_phone")
                    )

                    // Email (Optional)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("ایمیل (اختیاری)", fontFamily = VazirmatnFontFamily) },
                        placeholder = { Text("جهت بازیابی رمز عبور", fontFamily = VazirmatnFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SchoolBluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_email")
                    )

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("رمز عبور ورود دانش‌آموز", fontFamily = VazirmatnFontFamily) },
                        placeholder = { Text("حداقل ۴ کاراکتر", fontFamily = VazirmatnFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SchoolBluePrimary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_password")
                    )

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text("تکرار رمز عبور", fontFamily = VazirmatnFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SchoolBluePrimary) },
                        trailingIcon = {
                            if (confirmPassword.isNotEmpty() && confirmPassword == password) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "مطابقت دارد",
                                    tint = PositiveGreen
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_confirm_password")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (firstName.trim().isBlank()) {
                        errorMessage = "لطفاً نام دانش‌آموز را وارد کنید."
                        return@Button
                    }

                    if (selectedMode == 0) {
                        // Pending / Unregistered mode: Requires only first name and last name
                        onAddPending(firstName, lastName, g, c)
                    } else {
                        // Full registration mode
                        if (code.trim().length != 6 || !code.trim().all { it.isDigit() }) {
                            errorMessage = "کد دانش‌آموزی باید یک عدد ۶ رقمی معتبر باشد."
                            return@Button
                        }
                        if (phone.trim().isBlank()) {
                            errorMessage = "لطفاً شماره تماس را وارد کنید."
                            return@Button
                        }
                        if (password.trim().length < 4) {
                            errorMessage = "رمز عبور باید حداقل ۴ رقم یا کاراکتر باشد."
                            return@Button
                        }
                        if (password.trim() != confirmPassword.trim()) {
                            errorMessage = "رمز عبور با تکرار آن مطابقت ندارد."
                            return@Button
                        }
                        onAddFull(firstName, lastName, g, c, code, phone, email, password)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                modifier = Modifier.testTag("confirm_add_student")
            ) {
                Text(
                    text = if (selectedMode == 0) "افزودن به مخاطبان" else "ثبت و ایجاد حساب",
                    fontFamily = VazirmatnFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("انصراف", fontFamily = VazirmatnFontFamily)
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffProfileDialog(
    user: User,
    matchingStudent: com.example.domain.model.Student?,
    onDismiss: () -> Unit,
    onNavigateToStudentProfile: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اطلاعات و پنل فرد ویژه",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "بستن")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(com.example.ui.theme.SchoolBlueContainer)
                        .border(1.5.dp, SchoolBluePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(2).ifBlank { "فرد" },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = SchoolBluePrimary
                        )
                    )
                }

                // Name
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Ranks / Role Badges
                com.example.presentation.components.UserRankBadge(user = user, isLarge = true)

                // Phone Number (ONLY if registered and non-blank)
                if (user.phone.isValidPhoneNumber()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = SchoolBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "شماره تماس همراه:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    text = user.phone.toPersianDigits(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = SchoolBluePrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Teaching Subjects (ONLY if non-empty, otherwise DO NOT SHOW)
                if (user.teachingSubjects.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "دروس تدریسی:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                user.teachingSubjects.forEach { subject ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SchoolBluePrimary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = subject,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = SchoolBluePrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Assigned Classes / Class Branches (ONLY if non-empty, otherwise DO NOT SHOW)
                if (user.assignedClasses.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شعبه‌های کلاس‌های تحت تدریس:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                user.assignedClasses.forEach { cls ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PositiveGreen.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = cls,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = PositiveGreen
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bio / Description
                if (user.bio.isNotBlank()) {
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Student linkage button (if user is also a student)
                if (matchingStudent != null && user.role == UserRole.STUDENT) {
                    Button(
                        onClick = { onNavigateToStudentProfile(matchingStudent.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "مشاهده پرونده و کارنامه دانش‌آموزی",
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
            ) {
                Text("متوجه شدم", fontFamily = VazirmatnFontFamily)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
