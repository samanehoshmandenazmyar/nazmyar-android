package com.example.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.model.toPersianDigits
import com.example.presentation.viewmodel.AuthUiState
import com.example.presentation.viewmodel.AuthViewModel
import com.example.ui.theme.IncidentRedContainer
import com.example.ui.theme.OnIncidentRedContainer
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import kotlin.random.Random

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val savedUsers = authViewModel.savedUsers

    // Tabs: 0 -> ورود (Login), 1 -> ثبت‌نام (Sign Up)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Login Form State
    var loginRole by remember { mutableStateOf(UserRole.STUDENT) }
    var loginParentRelation by remember { mutableStateOf("پدر دانش‌آموز") }
    var loginName by remember { mutableStateOf("") }
    var loginStudentCode by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Register Form State
    var regName by remember { mutableStateOf("") }
    var regStudentCode by remember { mutableStateOf(Random.nextInt(100000, 999999).toString()) }
    var regPhone by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf(UserRole.STUDENT) }
    var regGender by remember { mutableStateOf("پسر") }
    var regGradeClass by remember { mutableStateOf("هفتم یک") }
    var regPasswordVisible by remember { mutableStateOf(false) }

    // Forgot Password / Recovery Dialog State
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryQuery by remember { mutableStateOf("") }
    var recoveryResultUser by remember { mutableStateOf<User?>(null) }
    var recoveryError by remember { mutableStateOf<String?>(null) }
    var isRecovering by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        }
    }

    // Function to generate random 6-digit student code
    fun generateRandomStudentCode(): String {
        val code = Random.nextInt(100000, 999999).toString()
        regStudentCode = code
        return code
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // School Logo & App Branding
            Image(
                painter = painterResource(id = com.example.R.drawable.app_icon),
                contentDescription = "لوگوی سامانه نظمیار",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "سامانه هوشمند نظمیار",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "پرتال جامع احراز هویت، ثبت‌نام و مدیریت انضباطی",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Navigation Tabs: ورود / ثبت‌نام
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = SchoolBluePrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                authViewModel.clearState()
                            },
                            text = {
                                Text(
                                    text = "ورود به سامانه",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_login")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                authViewModel.clearState()
                                if (regStudentCode.isEmpty()) {
                                    generateRandomStudentCode()
                                }
                            },
                            text = {
                                Text(
                                    text = "ثبت‌نام جدید",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_register")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Banner
                    AnimatedVisibility(visible = uiState is AuthUiState.Error) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = IncidentRedContainer
                        ) {
                            Text(
                                text = (uiState as? AuthUiState.Error)?.message.orEmpty(),
                                color = OnIncidentRedContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // TAB 0: LOGIN FORM
                    if (selectedTab == 0) {
                        Text(
                            text = "مشخصات ورود به حساب کاربری",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Role Selection: دانش‌آموز vs ولی دانش‌آموز
                        Text(
                            text = "ورود به عنوان:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = loginRole == UserRole.STUDENT,
                                onClick = { loginRole = UserRole.STUDENT },
                                label = { Text("دانش‌آموز") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = SchoolBluePrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("login_role_student")
                            )

                            FilterChip(
                                selected = loginRole == UserRole.PARENT,
                                onClick = { loginRole = UserRole.PARENT },
                                label = { Text("ولی دانش‌آموز") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = SchoolBluePrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("login_role_parent")
                            )
                        }

                        // Parent Relation: پدر vs مادر
                        if (loginRole == UserRole.PARENT) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "نسبت ولی:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = loginParentRelation == "پدر دانش‌آموز",
                                    onClick = { loginParentRelation = "پدر دانش‌آموز" },
                                    label = { Text("پدر دانش‌آموز") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = SchoolBluePrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("login_parent_dad")
                                )
                                FilterChip(
                                    selected = loginParentRelation == "مادر دانش‌آموز",
                                    onClick = { loginParentRelation = "مادر دانش‌آموز" },
                                    label = { Text("مادر دانش‌آموز") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = SchoolBluePrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("login_parent_mom")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "اولیای گرامی: جهت ورود، نیازی به ثبت‌نام جداگانه ندارید. کافیست نام و کد دانش‌آموزی فرزند خود و رمز عبور او را وارد فرمایید.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full Name
                        OutlinedTextField(
                            value = loginName,
                            onValueChange = { loginName = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = if (loginRole == UserRole.PARENT) "نام و نام خانوادگی فرزند دانش‌آموز" else "نام و نام خانوادگی",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    text = if (loginRole == UserRole.PARENT) "مثال: علی رضایی (نام فرزند)" else "نام و نام خانوادگی خود را وارد کنید",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Student Code (6 digits)
                        OutlinedTextField(
                            value = loginStudentCode,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    loginStudentCode = it
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = if (loginRole == UserRole.PARENT) "کد ۶ رقمی فرزند دانش‌آموز" else "کد دانش‌آموزی (۶ رقمی)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "کد ۶ رقمی را وارد کنید",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary
                                )
                            },
                            supportingText = {
                                Text(
                                    text = if (loginRole == UserRole.PARENT) "کد ۶ رقمی ثبت‌شده برای فرزند" else "کد ۶ رقمی اختصاصی دریافت شده هنگام ثبت‌نام",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_student_code_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = if (loginRole == UserRole.PARENT) "رمز عبور حساب فرزند" else "رمز عبور",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "رمز عبور خود را وارد کنید",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                    Icon(
                                        imageVector = if (loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (loginPasswordVisible) "مخفی کردن" else "نمایش"
                                    )
                                }
                            },
                            visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        // Forgot Password / Recovery button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    recoveryQuery = ""
                                    recoveryResultUser = null
                                    recoveryError = null
                                    showRecoveryDialog = true
                                },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = SchoolBluePrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "رمز عبور یا کد دانش‌آموزی را فراموش کرده‌اید؟",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SchoolBluePrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Submit Login Button
                        Button(
                            onClick = {
                                authViewModel.login(
                                    name = loginName,
                                    studentCode = loginStudentCode,
                                    password = loginPassword,
                                    role = loginRole,
                                    parentRelation = loginParentRelation
                                )
                            },
                            enabled = uiState !is AuthUiState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button")
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (loginRole == UserRole.PARENT) "ورود به عنوان $loginParentRelation" else "ورود دانش‌آموز به سامانه",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // TAB 1: REGISTRATION FORM (ONLY STUDENTS)
                    else {
                        Text(
                            text = "فرم ثبت‌نام دانش‌آموز جدید",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "ثبت‌نام مستقیم دانش‌آموزان (اولیا محترم جهت ورود، از بخش «ورود به سامانه» اقدام نمایند)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Grade & Class Selection
                        Text(
                            text = "پایه و کلاس دانش‌آموز:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("هفتم یک", "هفتم دو", "هشتم یک").forEach { opt ->
                                    FilterChip(
                                        selected = regGradeClass == opt,
                                        onClick = { regGradeClass = opt },
                                        label = {
                                            Text(
                                                text = opt,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.15f),
                                            selectedLabelColor = SchoolBluePrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("reg_grade_chip_$opt")
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("هشتم دو", "نهم یک", "نهم دو").forEach { opt ->
                                    FilterChip(
                                        selected = regGradeClass == opt,
                                        onClick = { regGradeClass = opt },
                                        label = {
                                            Text(
                                                text = opt,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.15f),
                                            selectedLabelColor = SchoolBluePrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("reg_grade_chip_$opt")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Full Name
                        val matchedStudent = remember(regName) {
                            if (regName.trim().length >= 2) {
                                com.example.data.demo.AllowedStudentsRegistry.findMatchingAllowedStudent(regName)
                            } else null
                        }

                        OutlinedTextField(
                            value = regName,
                            onValueChange = { input ->
                                regName = input
                                val found = com.example.data.demo.AllowedStudentsRegistry.findMatchingAllowedStudent(input)
                                if (found != null) {
                                    regGradeClass = when ("${found.grade} - ${found.className}") {
                                        "پایه هفتم - کلاس 7/1" -> "هفتم یک"
                                        "پایه هفتم - کلاس 7/2" -> "هفتم دو"
                                        "پایه هشتم - کلاس 8/1" -> "هشتم یک"
                                        "پایه هشتم - کلاس 8/2" -> "هشتم دو"
                                        "پایه نهم - کلاس 9/1" -> "نهم یک"
                                        "پایه نهم - کلاس 9/2" -> "نهم دو"
                                        else -> regGradeClass
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = "نام و نام خانوادگی دانش‌آموز (طبق لیست مدرسه)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "مثال: علی عالیپور یا محمد طاها ارشادی",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = SchoolBluePrimary)
                            },
                            trailingIcon = {
                                if (matchedStudent != null) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "تأیید در لیست مدرسه",
                                        tint = PositiveGreen
                                    )
                                }
                            },
                            supportingText = {
                                if (matchedStudent != null) {
                                    Text(
                                        text = "✓ دانش‌آموز مجاز: ${matchedStudent.fullName} (${matchedStudent.grade} - ${matchedStudent.className})",
                                        color = PositiveGreen,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                } else if (regName.trim().length >= 3) {
                                    Text(
                                        text = "فقط دانش‌آموزان موجود در لیست رسمی مدرسه مجاز به ثبت‌نام هستند.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = "نام و نام خانوادگی را دقیق وارد کنید (فاصله‌ها به صورت خودکار تصحیح می‌شوند)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            isError = regName.trim().length >= 3 && matchedStudent == null,
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Student Code (Auto-generated 6-digit number, generated/changed only via circular arrow)
                        OutlinedTextField(
                            value = if (regStudentCode.isNotEmpty()) regStudentCode.toPersianDigits() else "",
                            onValueChange = {}, // Read-only: cannot be typed manually
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
                            placeholder = {
                                Text(
                                    text = "برای تولید کد دکمه بازتولید را لمس کنید",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = SchoolBluePrimary)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { generateRandomStudentCode() },
                                    modifier = Modifier.testTag("generate_student_code_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تولید مجدد کد تصادفی",
                                        tint = SchoolBluePrimary
                                    )
                                }
                            },
                            supportingText = {
                                Text(
                                    text = if (regStudentCode.isNotEmpty())
                                        "کد اختصاصی: $regStudentCode (برای تغییر، دکمه بازتولید را لمس کنید)"
                                    else
                                        "برای تولید کد ۶ رقمی، دکمه بازتولید را لمس کنید",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_student_code_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Phone Number
                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it },
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
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = SchoolBluePrimary)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_phone_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Email (Optional, for recovery)
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = "ایمیل (اختیاری)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "جهت بازیابی رمز عبور و کد دانش‌آموزی",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = SchoolBluePrimary)
                            },
                            supportingText = {
                                Text(
                                    text = "اختیاری - فقط جهت بازیابی رمز عبور و کد دانش‌آموزی",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_email_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = "رمز عبور",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "رمز عبور را وارد کنید",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SchoolBluePrimary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                    Icon(
                                        imageVector = if (regPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_password_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Confirm Password
                        OutlinedTextField(
                            value = regConfirmPassword,
                            onValueChange = { regConfirmPassword = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = "تکرار رمز عبور",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "مجدداً رمز عبور را وارد کنید",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SchoolBluePrimary)
                            },
                            trailingIcon = {
                                if (regConfirmPassword.isNotEmpty() && regConfirmPassword == regPassword) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "مطابقت دارد",
                                        tint = PositiveGreen
                                    )
                                }
                            },
                            visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_confirm_password_input")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Submit Registration Button
                        Button(
                            onClick = {
                                if (regStudentCode.isEmpty()) {
                                    generateRandomStudentCode()
                                }
                                val found = com.example.data.demo.AllowedStudentsRegistry.findMatchingAllowedStudent(regName)
                                val (selectedGrade, selectedClass) = if (found != null) {
                                    Pair(found.grade, found.className)
                                } else when (regGradeClass) {
                                    "هفتم یک" -> Pair("پایه هفتم", "کلاس 7/1")
                                    "هفتم دو" -> Pair("پایه هفتم", "کلاس 7/2")
                                    "هشتم یک" -> Pair("پایه هشتم", "کلاس 8/1")
                                    "هشتم دو" -> Pair("پایه هشتم", "کلاس 8/2")
                                    "نهم یک" -> Pair("پایه نهم", "کلاس 9/1")
                                    "نهم دو" -> Pair("پایه نهم", "کلاس 9/2")
                                    else -> Pair("پایه هفتم", "کلاس 7/1")
                                }
                                val finalName = found?.fullName ?: regName
                                authViewModel.register(
                                    name = finalName,
                                    studentCode = regStudentCode,
                                    phone = regPhone,
                                    email = regEmail,
                                    password = regPassword,
                                    confirmPassword = regConfirmPassword,
                                    role = UserRole.STUDENT,
                                    gender = "پسر",
                                    grade = selectedGrade,
                                    className = selectedClass
                                )
                            },
                            enabled = uiState !is AuthUiState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("register_submit_button")
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "ثبت‌نام و ورود دانش‌آموز به سامانه",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Account Recovery Dialog
        if (showRecoveryDialog) {
            AlertDialog(
                onDismissRequest = { showRecoveryDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = SchoolBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بازیابی از طریق پیامک یا ایمیل",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "دریافت کد دانش‌آموزی و رمز عبور فقط از طریق شماره تماس (ارسال پیامک) یا ارسال به صندوق ایمیل ثبت‌شده امکان‌پذیر است:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = recoveryQuery,
                            onValueChange = {
                                recoveryQuery = it
                                recoveryError = null
                            },
                            label = { Text("شماره تماس (پیامک) یا ایمیل") },
                            placeholder = { Text("مثال: 09123456789 یا email@domain.com") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recovery_query_input")
                        )

                        // Error or Result Display
                        recoveryError?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        recoveryResultUser?.let { user ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF0FDF4), // Success green light
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = PositiveGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "اطلاعات با موفقیت آماده ارسال شد:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "📱 دریافت از طریق پیامک: شماره ${user.phone.toPersianDigits()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "✉️ دریافت از طریق ایمیل: ${if (user.email.isNotBlank()) user.email else "ایمیل ثبت نشده است"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "کد دانش‌آموزی: ${user.studentCode.toPersianDigits()} | رمز عبور: ${user.password}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SchoolBluePrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "✅ مشخصات کاربری با موفقیت بررسی شد و به سرشماره پیامک و صندوق ایمیل ارسال گردید.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (recoveryResultUser != null) {
                                // Auto-fill into login form
                                loginName = recoveryResultUser?.name.orEmpty()
                                loginStudentCode = recoveryResultUser?.studentCode.orEmpty()
                                loginPassword = recoveryResultUser?.password.orEmpty()
                                showRecoveryDialog = false
                            } else {
                                isRecovering = true
                                authViewModel.recoverAccount(recoveryQuery) { result ->
                                    isRecovering = false
                                    result.onSuccess { u ->
                                        recoveryResultUser = u
                                        recoveryError = null
                                    }.onFailure { err ->
                                        recoveryResultUser = null
                                        recoveryError = err.message ?: "اطلاعاتی یافت نشد."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("recovery_submit_button")
                    ) {
                        Text(if (recoveryResultUser != null) "انتقال به فرم ورود" else "ارسال به پیامک / ایمیل")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecoveryDialog = false }) {
                        Text("بستن")
                    }
                }
            )
        }
    }
}
