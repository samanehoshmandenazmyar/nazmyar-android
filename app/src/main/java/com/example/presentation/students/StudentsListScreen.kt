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
import com.example.data.demo.DemoData
import com.example.domain.model.UserRole
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

    // Initial on-mount refresh
    LaunchedEffect(Unit) {
        mainViewModel.refreshData()
    }

    val allUsers by authViewModel.allUsers.collectAsState()

    // Special individuals list (includes creator, school staff e.g. Mehdi Faryabi and Yasin Dorahaki, and students with good rank)
    val specialUsers = remember(currentUser, students, allUsers) {
        val list = mutableListOf<User>()
        // 1. Staff users (Creator, Manager Mehdi Faryabi, Counselor Yasin Dorahaki, etc.)
        val staffList = allUsers.filter { it.role != UserRole.STUDENT && it.role != UserRole.PARENT }
        if (staffList.isNotEmpty()) {
            list.addAll(staffList)
        } else {
            list.addAll(DemoData.demoUsers)
        }
        if (currentUser != null && currentUser?.isCreator == true && list.none { it.id == currentUser?.id }) {
            list.add(currentUser!!)
        }
        // 2. Students with good ranks appear in the Special Individuals tab as requested by the user:
        val goodRankStudents = students.filter { it.hasGoodRank() }.map { it.toUser() }
        list.addAll(goodRankStudents)

        list.distinctBy { it.id }
    }

    val filteredSpecialUsers = remember(specialUsers, searchQuery) {
        if (searchQuery.isBlank()) {
            specialUsers
        } else {
            specialUsers.filter { u ->
                u.name.contains(searchQuery, ignoreCase = true) ||
                u.studentCode.contains(searchQuery) ||
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
                            "${students.size.toPersianDigits()} دانش‌آموز"
                        } else {
                            "${filteredSpecialUsers.size.toPersianDigits()} فرد ویژه"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tabs Row (دانش‌آموزان / افراد ویژه)
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
                                    text = "دانش‌آموزان (${students.size.toPersianDigits()})",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Light
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "دانش‌آموزان",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Text(
                                    text = "افراد ویژه ⭐ (${filteredSpecialUsers.size.toPersianDigits()})",
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
                                    tint = if (selectedTabIndex == 1) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant
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

            // Grade Filter Chips (Only relevant for Students tab)
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
                        text = if (selectedTabIndex == 0) "فهرست دانش‌آموزان" else "فهرست افراد ویژه و مدیران",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedTabIndex == 0) {
                            "${students.size.toPersianDigits()} دانش‌آموز"
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
                if (students.isEmpty()) {
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
                                    text = "هنوز هیچ دانش‌آموزی در سامانه ثبت نشده است.",
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
                    items(students, key = { it.id }) { student ->
                        StudentListItemCard(
                            student = student,
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
                        SpecialUserListItemCard(
                            user = user,
                            onClick = {
                                if (students.any { it.id == user.id }) {
                                    onStudentClick(user.id)
                                }
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

    if (showAddStudentDialog) {
        AddStudentDialog(
            onDismiss = { showAddStudentDialog = false },
            onAdd = { firstName, lastName, grade, className, code, phone, email, password ->
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
    onAdd: (firstName: String, lastName: String, grade: String, className: String, code: String, phone: String, email: String, password: String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    val classOptions = listOf("هفتم یک", "هفتم دو", "هشتم یک", "هشتم دو", "نهم یک", "نهم دو")
    var selectedGradeClass by remember { mutableStateOf("هفتم یک") }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "افزودن و ثبت‌نام دانش‌آموز جدید",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Text(
                    text = "اطلاعات زیر دقیقاً مشابه فرم ثبت‌نام ثبت خواهد شد و دانش‌آموز می‌تواند بلافاصله با همین مشخصات وارد برنامه شود:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Grade & Class
                Text(
                    text = "پایه و کلاس دانش‌آموز:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(classOptions) { opt ->
                        FilterChip(
                            selected = selectedGradeClass == opt,
                            onClick = { selectedGradeClass = opt },
                            label = { Text(opt) }
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
                    label = {
                        Text(
                            text = "نام دانش‌آموز",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Light
                            )
                        )
                    },
                    placeholder = {
                        Text(
                            text = "مثال: علی",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Light
                            )
                        )
                    },
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
                    label = {
                        Text(
                            text = "نام خانوادگی (اختیاری)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Light
                            )
                        )
                    },
                    placeholder = {
                        Text(
                            text = "مثال: رضایی",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Light
                            )
                        )
                    },
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
                    label = {
                        Text(
                            text = "کد دانش‌آموزی ۶ رقمی (سیستمی)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Light
                            )
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = SchoolBluePrimary) },
                    trailingIcon = {
                        IconButton(onClick = { generateCode() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تولید مجدد کد", tint = SchoolBluePrimary)
                        }
                    },
                    supportingText = {
                        Text(
                            text = "کد ورود: $code (برای تغییر روی فلش دایره‌ای 🔄 بزنید)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Light
                            )
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
                    label = {
                        Text(
                            text = "شماره تماس",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Light
                            )
                        )
                    },
                    placeholder = {
                        Text(
                            text = "مثال: 09123456789",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Light
                            )
                        )
                    },
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
                    label = { Text("ایمیل (اختیاری)") },
                    placeholder = { Text("جهت بازیابی رمز عبور") },
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
                    label = { Text("رمز عبور ورود دانش‌آموز") },
                    placeholder = { Text("حداقل ۴ کاراکتر") },
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
                    label = { Text("تکرار رمز عبور") },
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (firstName.trim().isBlank()) {
                        errorMessage = "لطفاً نام دانش‌آموز را وارد کنید."
                        return@Button
                    }
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

                    val (g, c) = when (selectedGradeClass) {
                        "هفتم یک" -> Pair("پایه هفتم", "کلاس 7/1")
                        "هفتم دو" -> Pair("پایه هفتم", "کلاس 7/2")
                        "هشتم یک" -> Pair("پایه هشتم", "کلاس 8/1")
                        "هشتم دو" -> Pair("پایه هشتم", "کلاس 8/2")
                        "نهم یک" -> Pair("پایه نهم", "کلاس 9/1")
                        "نهم دو" -> Pair("پایه نهم", "کلاس 9/2")
                        else -> Pair("پایه هفتم", "کلاس 7/1")
                    }
                    onAdd(firstName, lastName, g, c, code, phone, email, password)
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                modifier = Modifier.testTag("confirm_add_student")
            ) {
                Text("ثبت و ایجاد حساب")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("انصراف")
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}
