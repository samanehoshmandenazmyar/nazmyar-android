package com.example.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.isAppInDarkTheme
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
    onLoginSuccess: () -> Unit,
    onNavigateToSupport: () -> Unit = {}
) {
    val uiState by authViewModel.uiState.collectAsState()
    val isDark = isAppInDarkTheme()

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

    fun generateRandomStudentCode(): String {
        val code = Random.nextInt(100000, 999999).toString()
        regStudentCode = code
        return code
    }

    // Dynamic color tokens for perfect light/dark adaptation
    val cardBackground = if (isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val inputBackground = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val inputBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val titleTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subtitleTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val labelTextColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF0F172A),
                            Color(0xFF0B1120),
                            Color(0xFF020617)
                        )
                    } else {
                        listOf(
                            Color(0xFFE0E7FF).copy(alpha = 0.45f),
                            Color(0xFFF8FAFC),
                            Color(0xFFF1F5F9)
                        )
                    }
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Decorative ambient glow at top
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(y = (-110).dp)
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SchoolBluePrimary.copy(alpha = if (isDark) 0.22f else 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Support Access Button for Registration and Login
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SchoolBluePrimary.copy(alpha = if (isDark) 0.22f else 0.1f),
                    border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToSupport() }
                        .testTag("login_top_support_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HeadsetMic,
                            contentDescription = "پشتیبانی",
                            tint = SchoolBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "پشتیبانی و راهنما",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = SchoolBluePrimary
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)).copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "دبیرستان شهید رجایی",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            color = subtitleTextColor
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // School Logo in Glassmorphic Card Container
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = cardBackground,
                shadowElevation = if (isDark) 4.dp else 8.dp,
                border = BorderStroke(1.5.dp, cardBorder),
                modifier = Modifier.size(92.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.app_icon),
                        contentDescription = "لوگوی سامانه نظمیار",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) SchoolBluePrimary.copy(alpha = 0.2f) else SchoolBluePrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = if (isDark) 0.4f else 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SchoolBluePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "سامانه جامع آموزشی و انضباطی",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = SchoolBluePrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "سامانه هوشمند نظمیار",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = VazirmatnFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleTextColor,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "پرتال جامع احراز هویت، ثبت‌نام و مدیریت انضباطی",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = VazirmatnFontFamily,
                    color = subtitleTextColor
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 2.dp else 4.dp),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Modern Segmented Pill Switcher: ورود / ثبت‌نام
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Login Tab Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selectedTab == 0) {
                                            if (isDark) Color(0xFF334155) else Color.White
                                        } else Color.Transparent
                                    )
                                    .then(
                                        if (selectedTab == 0) {
                                            Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                                        } else Modifier
                                    )
                                    .clickable {
                                        selectedTab = 0
                                        authViewModel.clearState()
                                    }
                                    .testTag("tab_login"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        tint = if (selectedTab == 0) {
                                            if (isDark) Color.White else SchoolBluePrimary
                                        } else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "ورود به سامانه",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedTab == 0) {
                                                if (isDark) Color.White else SchoolBluePrimary
                                            } else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    )
                                }
                            }

                            // Register Tab Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selectedTab == 1) {
                                            if (isDark) Color(0xFF334155) else Color.White
                                        } else Color.Transparent
                                    )
                                    .then(
                                        if (selectedTab == 1) {
                                            Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                                        } else Modifier
                                    )
                                    .clickable {
                                        selectedTab = 1
                                        authViewModel.clearState()
                                        if (regStudentCode.isEmpty()) {
                                            generateRandomStudentCode()
                                        }
                                    }
                                    .testTag("tab_register"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = if (selectedTab == 1) {
                                            if (isDark) Color.White else SchoolBluePrimary
                                        } else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "ثبت‌نام جدید",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedTab == 1) {
                                                if (isDark) Color.White else SchoolBluePrimary
                                            } else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Banner
                    AnimatedVisibility(visible = uiState is AuthUiState.Error) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = IncidentRedContainer,
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = OnIncidentRedContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = (uiState as? AuthUiState.Error)?.message.orEmpty(),
                                    color = OnIncidentRedContainer,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // ==========================================
                    // TAB 0: LOGIN FORM
                    // ==========================================
                    if (selectedTab == 0) {
                        // Section Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp, 16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(SchoolBluePrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مشخصات ورود به حساب کاربری",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = titleTextColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Role Selection: Card Pickers
                        Text(
                            text = "ورود به عنوان:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = labelTextColor
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Student Role Card
                            val isStudent = loginRole == UserRole.STUDENT
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isStudent) SchoolBluePrimary.copy(alpha = if (isDark) 0.25f else 0.08f) else inputBackground,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isStudent) SchoolBluePrimary else cardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { loginRole = UserRole.STUDENT }
                                    .testTag("login_role_student")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = if (isStudent) SchoolBluePrimary else subtitleTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "دانش‌آموز",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (isStudent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isStudent) (if (isDark) Color.White else SchoolBluePrimary) else labelTextColor,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            // Parent Role Card
                            val isParent = loginRole == UserRole.PARENT
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isParent) SchoolBluePrimary.copy(alpha = if (isDark) 0.25f else 0.08f) else inputBackground,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isParent) SchoolBluePrimary else cardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { loginRole = UserRole.PARENT }
                                    .testTag("login_role_parent")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FamilyRestroom,
                                        contentDescription = null,
                                        tint = if (isParent) SchoolBluePrimary else subtitleTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "اولیاء",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (isParent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isParent) (if (isDark) Color.White else SchoolBluePrimary) else labelTextColor,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            // Staff Role Card (ورود به عنوان کادر آموزشی)
                            val isStaff = (loginRole == UserRole.MANAGER || loginRole == UserRole.VICE_PRINCIPAL || loginRole == UserRole.TEACHER || loginRole == UserRole.COUNSELOR)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isStaff) SchoolBluePrimary.copy(alpha = if (isDark) 0.25f else 0.08f) else inputBackground,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isStaff) SchoolBluePrimary else cardBorder
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clickable { loginRole = UserRole.MANAGER }
                                    .testTag("login_role_staff")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (isStaff) SchoolBluePrimary else subtitleTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "کادر و دبیران",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (isStaff) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isStaff) (if (isDark) Color.White else SchoolBluePrimary) else labelTextColor,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Parent Relation: پدر vs مادر
                        if (loginRole == UserRole.PARENT) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "نسبت ولی گرامی:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = labelTextColor
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = loginParentRelation == "پدر دانش‌آموز",
                                    onClick = { loginParentRelation = "پدر دانش‌آموز" },
                                    label = {
                                        Text(
                                            text = "پدر دانش‌آموز",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = if (loginParentRelation == "پدر دانش‌آموز") FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SchoolBluePrimary.copy(alpha = if (isDark) 0.3f else 0.12f),
                                        selectedLabelColor = if (isDark) Color.White else SchoolBluePrimary,
                                        containerColor = inputBackground,
                                        labelColor = labelTextColor
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (loginParentRelation == "پدر دانش‌آموز") SchoolBluePrimary else cardBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("login_parent_dad")
                                )
                                FilterChip(
                                    selected = loginParentRelation == "مادر دانش‌آموز",
                                    onClick = { loginParentRelation = "مادر دانش‌آموز" },
                                    label = {
                                        Text(
                                            text = "مادر دانش‌آموز",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = if (loginParentRelation == "مادر دانش‌آموز") FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SchoolBluePrimary.copy(alpha = if (isDark) 0.3f else 0.12f),
                                        selectedLabelColor = if (isDark) Color.White else SchoolBluePrimary,
                                        containerColor = inputBackground,
                                        labelColor = labelTextColor
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (loginParentRelation == "مادر دانش‌آموز") SchoolBluePrimary else cardBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("login_parent_mom")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.35f) else Color(0xFFF0F9FF),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF0284C7).copy(alpha = 0.4f) else Color(0xFFBAE6FD))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "اولیای گرامی: جهت ورود، نیازی به ثبت‌نام جداگانه ندارید. کافیست نام و کد دانش‌آموزی فرزند خود و رمز عبور او را وارد فرمایید.",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            color = if (isDark) Color(0xFFBAE6FD) else Color(0xFF0369A1),
                                            lineHeight = 18.sp
                                        )
                                    )
                                }
                            }
                        }

                        val isStaff = (loginRole == UserRole.MANAGER || loginRole == UserRole.VICE_PRINCIPAL || loginRole == UserRole.TEACHER || loginRole == UserRole.COUNSELOR)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Full Name Input
                        OutlinedTextField(
                            value = loginName,
                            onValueChange = { loginName = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = if (loginRole == UserRole.PARENT) "نام و نام خانوادگی فرزند دانش‌آموز"
                                           else "نام و نام خانوادگی",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                                )
                            },
                            placeholder = {
                                Text(
                                    text = if (loginRole == UserRole.PARENT) "مثال: علی رضایی"
                                           else if (isStaff) "مثال: مهدی فاریابی یا حسین راسخیان"
                                           else "نام کامل خود را وارد کنید",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = subtitleTextColor
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
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBackground,
                                unfocusedContainerColor = inputBackground,
                                focusedBorderColor = SchoolBluePrimary,
                                unfocusedBorderColor = inputBorder,
                                focusedTextColor = titleTextColor,
                                unfocusedTextColor = titleTextColor,
                                focusedLabelColor = SchoolBluePrimary,
                                unfocusedLabelColor = subtitleTextColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_name_input")
                        )

                        // Student Code Input (ONLY for Student and Parent)
                        if (!isStaff) {
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = loginStudentCode,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                        loginStudentCode = it
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                                label = {
                                    Text(
                                        text = if (loginRole == UserRole.PARENT) "کد ۶ رقمی فرزند دانش‌آموز" else "کد دانش‌آموزی (۶ رقمی)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = "کد ۶ رقمی را وارد کنید",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            color = subtitleTextColor
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
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            color = subtitleTextColor
                                        )
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBackground,
                                    unfocusedContainerColor = inputBackground,
                                    focusedBorderColor = SchoolBluePrimary,
                                    unfocusedBorderColor = inputBorder,
                                    focusedTextColor = titleTextColor,
                                    unfocusedTextColor = titleTextColor,
                                    focusedLabelColor = SchoolBluePrimary,
                                    unfocusedLabelColor = subtitleTextColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_student_code_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Input
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = if (loginRole == UserRole.PARENT) "رمز عبور حساب فرزند" else "رمز عبور",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "رمز عبور خود را وارد کنید",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = subtitleTextColor
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
                                        contentDescription = if (loginPasswordVisible) "مخفی کردن" else "نمایش",
                                        tint = subtitleTextColor
                                    )
                                }
                            },
                            visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBackground,
                                unfocusedContainerColor = inputBackground,
                                focusedBorderColor = SchoolBluePrimary,
                                unfocusedBorderColor = inputBorder,
                                focusedTextColor = titleTextColor,
                                unfocusedTextColor = titleTextColor,
                                focusedLabelColor = SchoolBluePrimary,
                                unfocusedLabelColor = subtitleTextColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

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
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("login_submit_button")
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isStaff) "ورود به عنوان کادر آموزشی"
                                               else if (loginRole == UserRole.PARENT) "ورود به عنوان $loginParentRelation"
                                               else "ورود به سامانه نظمیار",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // TAB 1: REGISTRATION FORM (STUDENTS)
                    // ==========================================
                    else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp, 16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(SchoolBluePrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "فرم ثبت‌نام دانش‌آموز جدید",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = titleTextColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "ثبت‌نام مستقیم دانش‌آموزان (اولیا گرامی جهت ورود از تب «ورود به سامانه» اقدام فرمایند)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = subtitleTextColor
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Grade & Class Selection
                        Text(
                            text = "انتخاب پایه و کلاس دانش‌آموز:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = labelTextColor
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("هفتم یک", "هفتم دو", "هشتم یک").forEach { opt ->
                                    val isSelected = regGradeClass == opt
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { regGradeClass = opt },
                                        label = {
                                            Text(
                                                text = opt,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = VazirmatnFontFamily,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SchoolBluePrimary.copy(alpha = if (isDark) 0.3f else 0.12f),
                                            selectedLabelColor = if (isDark) Color.White else SchoolBluePrimary,
                                            containerColor = inputBackground,
                                            labelColor = labelTextColor
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) SchoolBluePrimary else cardBorder
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
                                    val isSelected = regGradeClass == opt
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { regGradeClass = opt },
                                        label = {
                                            Text(
                                                text = opt,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = VazirmatnFontFamily,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SchoolBluePrimary.copy(alpha = if (isDark) 0.3f else 0.12f),
                                            selectedLabelColor = if (isDark) Color.White else SchoolBluePrimary,
                                            containerColor = inputBackground,
                                            labelColor = labelTextColor
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) SchoolBluePrimary else cardBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("reg_grade_chip_$opt")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Full Name with Registry Matching Indicator
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
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = "نام و نام خانوادگی دانش‌آموز (طبق لیست مدرسه)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "مثال: علی عالیپور یا محمد طاها ارشادی",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = subtitleTextColor
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
                                        tint = if (isDark) Color(0xFF34D399) else PositiveGreen
                                    )
                                }
                            },
                            supportingText = {
                                if (matchedStudent != null) {
                                    Text(
                                        text = "✓ دانش‌آموز مجاز: ${matchedStudent.fullName} (${matchedStudent.grade} - ${matchedStudent.className})",
                                        color = if (isDark) Color(0xFF34D399) else PositiveGreen,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                } else if (regName.trim().length >= 3) {
                                    Text(
                                        text = "فقط دانش‌آموزان موجود در لیست رسمی مدرسه مجاز به ثبت‌نام هستند.",
                                        color = if (isDark) Color(0xFFF87171) else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily)
                                    )
                                } else {
                                    Text(
                                        text = "نام و نام خانوادگی را دقیق وارد کنید (فاصله‌ها خودکار تطبیق داده می‌شوند)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily, color = subtitleTextColor)
                                    )
                                }
                            },
                            isError = regName.trim().length >= 3 && matchedStudent == null,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBackground,
                                unfocusedContainerColor = inputBackground,
                                focusedBorderColor = SchoolBluePrimary,
                                unfocusedBorderColor = inputBorder,
                                focusedTextColor = titleTextColor,
                                unfocusedTextColor = titleTextColor,
                                focusedLabelColor = SchoolBluePrimary,
                                unfocusedLabelColor = subtitleTextColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_name_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Student Code (Auto-generated 6-digit number with sleek token styling)
                        OutlinedTextField(
                            value = if (regStudentCode.isNotEmpty()) regStudentCode.toPersianDigits() else "",
                            onValueChange = {},
                            readOnly = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF34D399) else PositiveGreen
                            ),
                            label = {
                                Text(
                                    text = "کد دانش‌آموزی ۶ رقمی (سیستمی و اختصاصی)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "برای تولید کد دکمه بازتولید را لمس کنید",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
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
                                        "کد اختصاصی شما: ${regStudentCode.toPersianDigits()} (برای کد جدید، آیکون رفرش را بزنید)"
                                    else
                                        "برای تولید کد ۶ رقمی، دکمه بازتولید را لمس کنید",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = subtitleTextColor
                                    )
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFFF0FDF4),
                                unfocusedContainerColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.2f) else Color(0xFFF0FDF4),
                                focusedBorderColor = PositiveGreen,
                                unfocusedBorderColor = if (isDark) PositiveGreen.copy(alpha = 0.5f) else Color(0xFFBBF7D0),
                                focusedTextColor = if (isDark) Color(0xFF34D399) else PositiveGreen,
                                unfocusedTextColor = if (isDark) Color(0xFF34D399) else PositiveGreen
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_student_code_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Phone Number
                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = "شماره تماس همراه",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "مثال: 09123456789",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = subtitleTextColor
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = SchoolBluePrimary)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBackground,
                                unfocusedContainerColor = inputBackground,
                                focusedBorderColor = SchoolBluePrimary,
                                unfocusedBorderColor = inputBorder,
                                focusedTextColor = titleTextColor,
                                unfocusedTextColor = titleTextColor,
                                focusedLabelColor = SchoolBluePrimary,
                                unfocusedLabelColor = subtitleTextColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_phone_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Email (Optional, for recovery)
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = "ایمیل (اختیاری جهت بازیابی)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "name@example.com",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = subtitleTextColor
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = SchoolBluePrimary)
                            },
                            supportingText = {
                                Text(
                                    text = "اختیاری - فقط جهت بازیابی آسان‌تر رمز عبور و کد دانش‌آموزی",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = subtitleTextColor
                                    )
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBackground,
                                unfocusedContainerColor = inputBackground,
                                focusedBorderColor = SchoolBluePrimary,
                                unfocusedBorderColor = inputBorder,
                                focusedTextColor = titleTextColor,
                                unfocusedTextColor = titleTextColor,
                                focusedLabelColor = SchoolBluePrimary,
                                unfocusedLabelColor = subtitleTextColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_email_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = "رمز عبور جدید",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "رمز عبور دلخواه خود را وارد کنید",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = subtitleTextColor
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
                                        contentDescription = null,
                                        tint = subtitleTextColor
                                    )
                                }
                            },
                            visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBackground,
                                unfocusedContainerColor = inputBackground,
                                focusedBorderColor = SchoolBluePrimary,
                                unfocusedBorderColor = inputBorder,
                                focusedTextColor = titleTextColor,
                                unfocusedTextColor = titleTextColor,
                                focusedLabelColor = SchoolBluePrimary,
                                unfocusedLabelColor = subtitleTextColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_password_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Confirm Password
                        OutlinedTextField(
                            value = regConfirmPassword,
                            onValueChange = { regConfirmPassword = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = "تکرار رمز عبور",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "مجدداً رمز عبور را وارد کنید",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = subtitleTextColor
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
                                        tint = if (isDark) Color(0xFF34D399) else PositiveGreen
                                    )
                                }
                            },
                            visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBackground,
                                unfocusedContainerColor = inputBackground,
                                focusedBorderColor = SchoolBluePrimary,
                                unfocusedBorderColor = inputBorder,
                                focusedTextColor = titleTextColor,
                                unfocusedTextColor = titleTextColor,
                                focusedLabelColor = SchoolBluePrimary,
                                unfocusedLabelColor = subtitleTextColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_confirm_password_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

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
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("register_submit_button")
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "ثبت‌نام و ورود دانش‌آموز به سامانه",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
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
                shape = RoundedCornerShape(24.dp),
                containerColor = cardBackground,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SchoolBluePrimary.copy(alpha = if (isDark) 0.25f else 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "بازیابی رمز عبور و کد دانش‌آموزی",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = titleTextColor
                            )
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "دریافت کد دانش‌آموزی و رمز عبور از طریق شماره تماس (پیامک) یا صندوق ایمیل ثبت‌شده امکان‌پذیر است:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = labelTextColor
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = recoveryQuery,
                            onValueChange = {
                                recoveryQuery = it
                                recoveryError = null
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                            label = { Text("شماره همراه یا ایمیل", style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily)) },
                            placeholder = { Text("مثال: 09123456789 یا email@domain.com", style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily, color = subtitleTextColor)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBackground,
                                unfocusedContainerColor = inputBackground,
                                focusedBorderColor = SchoolBluePrimary,
                                unfocusedBorderColor = inputBorder,
                                focusedTextColor = titleTextColor,
                                unfocusedTextColor = titleTextColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recovery_query_input")
                        )

                        // Error or Result Display
                        recoveryError?.let { err ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = IncidentRedContainer,
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = err,
                                    color = OnIncidentRedContainer,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        recoveryResultUser?.let { user ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF059669).copy(alpha = 0.4f) else Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF34D399) else PositiveGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "اطلاعات با موفقیت بازیابی شد:",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF34D399) else Color(0xFF166534)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "📱 شماره پیامک: ${user.phone.toPersianDigits()}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily, color = titleTextColor)
                                    )
                                    Text(
                                        text = "✉️ آدرس ایمیل: ${if (user.email.isNotBlank()) user.email else "ایمیل ثبت نشده است"}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily, color = titleTextColor)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SchoolBluePrimary.copy(alpha = if (isDark) 0.2f else 0.08f),
                                        border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = if (isDark) 0.4f else 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "کد: ${user.studentCode.toPersianDigits()}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = VazirmatnFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SchoolBluePrimary
                                                )
                                            )
                                            Text(
                                                text = "رمز عبور: ${user.password}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = VazirmatnFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SchoolBluePrimary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (recoveryResultUser != null) {
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("recovery_submit_button")
                    ) {
                        Text(
                            text = if (recoveryResultUser != null) "انتقال به فرم ورود" else "ارسال به پیامک / ایمیل",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecoveryDialog = false }) {
                        Text("بستن", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily, color = subtitleTextColor))
                    }
                }
            )
        }
    }
}
