package com.example.presentation.navigation

import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.model.RankPermission
import com.example.domain.model.UserRole
import com.example.domain.model.toPersianDigits
import com.example.presentation.components.InAppUpdateDialog
import com.example.presentation.components.RgbRankBadge
import com.example.presentation.components.UserRankBadge
import com.example.presentation.admin.PublishAppUpdateScreen
import com.example.presentation.auth.LoginScreen
import com.example.presentation.behavior.RegisterDisciplinaryScreen
import com.example.presentation.behavior.RegisterPositiveScreen
import com.example.presentation.behavior.RegisterPrayerScreen
import androidx.compose.material.icons.filled.VolunteerActivism
import com.example.presentation.dashboard.ManagerDashboardScreen
import com.example.presentation.dashboard.ParentDashboardScreen
import com.example.presentation.dashboard.StudentDashboardScreen
import com.example.presentation.dashboard.TeacherDashboardScreen
import com.example.presentation.games.ChessScreen
import com.example.presentation.games.GamesScreen
import com.example.presentation.notifications.NotificationsScreen
import com.example.presentation.profile.ProfileScreen
import com.example.presentation.ranks.RankManagementScreen
import com.example.presentation.reports.ReportsScreen
import com.example.presentation.students.StudentProfileScreen
import com.example.presentation.students.StudentsListScreen
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.MainViewModel
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.Description
import com.example.presentation.voting.PollsScreen
import com.example.presentation.banner.BannerManagementScreen
import com.example.presentation.messenger.ClassMessengerListScreen
import com.example.presentation.messenger.ClassChatScreen
import com.example.presentation.behavior.StudentMonthlyDossierScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Login : Screen("login", "ورود", Icons.Default.Person, Icons.Outlined.Person)
    data object Home : Screen("home", "خانه و داشبورد", Icons.Default.Home, Icons.Outlined.Home)
    data object ClassMessenger : Screen("class_messenger", "پیام‌رسان کلاس‌ها", Icons.Default.Forum, Icons.Default.Forum)
    data object ClassChat : Screen("class_chat/{groupId}", "گفتگوی کلاس", Icons.Default.Forum, Icons.Default.Forum) {
        fun createRoute(groupId: String) = "class_chat/$groupId"
    }
    data object Students : Screen("students", "مخاطب‌ها", Icons.Default.Group, Icons.Outlined.Group)
    data object PtaVoting : Screen("pta_voting", "نظرسنجی‌ها", Icons.Default.HowToVote, Icons.Default.HowToVote)
    data object Polls : Screen("polls", "نظرسنجی‌ها", Icons.Default.HowToVote, Icons.Default.HowToVote)
    data object BannerManagement : Screen("banner_management", "مدیریت بنرهای صفحه اول", Icons.Default.Campaign, Icons.Outlined.Campaign)
    data object Games : Screen("games", "بازی‌ها", Icons.Default.SportsEsports, Icons.Outlined.SportsEsports)
    data object ChessGame : Screen("chess_game", "بازی شطرنج", Icons.Default.SportsEsports, Icons.Outlined.SportsEsports)
    data object RegisterPositive : Screen(
        "register_positive?studentId={studentId}&isLocked={isLocked}",
        "ثبت رفتار مثبت",
        Icons.Default.AddCircle,
        Icons.Default.AddCircle
    ) {
        fun createRoute(studentId: String = "", isLocked: Boolean = false) =
            "register_positive?studentId=$studentId&isLocked=$isLocked"
    }
    data object RegisterDisciplinary : Screen(
        "register_disciplinary?studentId={studentId}&isLocked={isLocked}",
        "ثبت گزارش انضباطی",
        Icons.Default.AddCircle,
        Icons.Default.AddCircle
    ) {
        fun createRoute(studentId: String = "", isLocked: Boolean = false) =
            "register_disciplinary?studentId=$studentId&isLocked=$isLocked"
    }
    data object RegisterPrayer : Screen(
        "register_prayer?studentId={studentId}",
        "ثبت نماز خواندن",
        Icons.Default.VolunteerActivism,
        Icons.Default.VolunteerActivism
    ) {
        fun createRoute(studentId: String = "") =
            if (studentId.isBlank()) "register_prayer" else "register_prayer?studentId=$studentId"
    }
    data object Reports : Screen("reports", "گزارش‌ها", Icons.Default.Assessment, Icons.Outlined.Assessment)
    data object Notifications : Screen("notifications", "اعلان‌ها", Icons.Default.Notifications, Icons.Outlined.Notifications)
    data object Profile : Screen("profile", "پروفایل", Icons.Default.Person, Icons.Outlined.Person)
    data object RankManagement : Screen("rank_management", "مدیریت رنک‌ها", Icons.Default.Security, Icons.Outlined.Security)
    data object PublishAppUpdate : Screen("publish_app_update", "انتشار نسخه جدید", Icons.Default.CloudUpload, Icons.Default.CloudUpload)
    data object StudentProfile : Screen("student_profile/{studentId}", "پرونده دانش‌آموز", Icons.Default.Face, Icons.Outlined.Face) {
        fun createRoute(studentId: String) = "student_profile/$studentId"
    }
    data object StudentMonthlyDossier : Screen(
        "student_monthly_dossier?studentId={studentId}",
        "پرونده انضباطی ماهانه",
        Icons.Default.Assignment,
        Icons.Outlined.Assignment
    ) {
        fun createRoute(studentId: String = "") = if (studentId.isBlank()) "student_monthly_dossier" else "student_monthly_dossier?studentId=$studentId"
    }
    data object FaceScan : Screen("face_scan", "اسکن سریع چهره", Icons.Default.Face, Icons.Outlined.Face)
    data object FaceEnrollment : Screen("face_enrollment", "مرکز ثبت چهره دانش‌آموزان", Icons.Default.Face, Icons.Outlined.Face)
    data object FaceRegistrationGate : Screen("face_registration_gate", "ثبت چهره دانش‌آموز", Icons.Default.Face, Icons.Outlined.Face)
    data object ReportCard : Screen("report_card", "دریافت کارنامه", Icons.Default.Description, Icons.Outlined.Description)
    data object Support : Screen("support", "پشتیبانی", Icons.Default.HeadsetMic, Icons.Outlined.HeadsetMic)
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val allStudents by mainViewModel.students.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()
    val unreadNotifs = notifications.count {
        !it.read && (currentUser?.role != UserRole.PARENT || it.recipientUserId == currentUser?.id)
    }

    val allRecords by mainViewModel.records.collectAsState()
    val hiddenReportIds by mainViewModel.hiddenFromReportsRecordIds.collectAsState()
    val reportsCount = remember(allRecords, hiddenReportIds, currentUser, allStudents) {
        val visible = allRecords.filter { it.id !in hiddenReportIds }
        if (currentUser?.role == UserRole.PARENT) {
            val parentChild = allStudents.firstOrNull { s ->
                currentUser?.childStudentIds?.contains(s.id) == true || s.studentCode == currentUser?.studentCode
            }
            if (parentChild != null) {
                visible.count { it.studentId == parentChild.id }
            } else {
                visible.size
            }
        } else {
            visible.size
        }
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        com.example.core.support.SupportManager.init(context)
    }
    val supportTickets by com.example.core.support.SupportManager.tickets.collectAsState()
    val unreadSupportCount = if (currentUser?.isSupportStaff == true) {
        supportTickets.count { it.unreadBySupport }
    } else {
        supportTickets.filter { it.userId == currentUser?.id || (currentUser?.phone?.isNotBlank() == true && it.userPhoneOrCode == currentUser?.phone) }.count { it.unreadByUser }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val appUpdateInfo by authViewModel.appUpdateInfo.collectAsState()
    var dismissedUpdateVersion by remember { mutableStateOf<Int?>(null) }
    val showUpdateDialog = appUpdateInfo != null &&
            appUpdateInfo!!.isUpdateAvailable &&
            (appUpdateInfo!!.isForceUpdate || dismissedUpdateVersion != appUpdateInfo!!.latestVersionCode)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val openDrawer: () -> Unit = {
        coroutineScope.launch { drawerState.open() }
    }
    val closeDrawer: () -> Unit = {
        coroutineScope.launch { drawerState.close() }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Handle Back press to close drawer if open
    BackHandler(enabled = drawerState.isOpen) {
        closeDrawer()
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (currentUser == null && currentRoute != null && currentRoute != Screen.Login.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        } else if (currentUser != null && currentUser?.role == UserRole.STUDENT) {
            val u = currentUser!!
            val isEnrolled = com.example.core.face.FaceRecognitionManager.isFaceEnrolled(u.id) || u.faceRegistered
            if (!isEnrolled && currentRoute != Screen.FaceRegistrationGate.route) {
                navController.navigate(Screen.FaceRegistrationGate.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val effectiveRole = if (currentUser?.isCreator == true) UserRole.MANAGER else currentUser?.role

    val userPerms = currentUser?.permissions ?: emptyList()

    val drawerItems = when (effectiveRole) {
        UserRole.STUDENT -> {
            val items = mutableListOf<Screen>(Screen.Home, Screen.PtaVoting, Screen.ReportCard)
            if (userPerms.contains(RankPermission.VIEW_STUDENT_PROFILES) || userPerms.contains(RankPermission.EDIT_CLASS_INFO)) {
                items.add(Screen.Students)
            }
            items.add(Screen.Games)
            if (userPerms.contains(RankPermission.VIEW_REPORTS)) {
                items.add(Screen.Reports)
            }
            items.add(Screen.Notifications)
            items.add(Screen.Support)
            items.add(Screen.Profile)
            items
        }
        UserRole.PARENT -> listOf(
            Screen.Home,
            Screen.PtaVoting,
            Screen.ReportCard,
            Screen.Games,
            Screen.Reports,
            Screen.Notifications,
            Screen.Support,
            Screen.Profile
        )
        else -> {
            val items = mutableListOf<Screen>(Screen.Home)
            if (com.example.core.auth.AccessControl.canManageFacesAndBehaviors(currentUser)) {
                items.add(Screen.FaceEnrollment)
                items.add(Screen.FaceScan)
            }
            items.add(Screen.PtaVoting)
            items.add(Screen.ReportCard)
            if (currentUser?.isCreator == true || currentUser?.role == UserRole.MANAGER || currentUser?.role == UserRole.VICE_PRINCIPAL) {
                items.add(Screen.BannerManagement)
                items.add(Screen.Students)
                items.add(Screen.StudentMonthlyDossier)
                items.add(Screen.Games)
                items.add(Screen.Reports)
            } else {
                if (userPerms.contains(RankPermission.VIEW_STUDENT_PROFILES) || userPerms.contains(RankPermission.EDIT_CLASS_INFO)) {
                    items.add(Screen.Students)
                    items.add(Screen.StudentMonthlyDossier)
                }
                if (userPerms.contains(RankPermission.ACCESS_GAMES)) {
                    items.add(Screen.Games)
                }
                if (userPerms.contains(RankPermission.VIEW_REPORTS)) {
                    items.add(Screen.Reports)
                }
            }
            items.add(Screen.Notifications)
            items.add(Screen.Support)
            items.add(Screen.Profile)
            items
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentUser != null,
        modifier = Modifier.testTag("app_navigation_drawer"),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .widthIn(max = 330.dp)
                    .testTag("modal_drawer_sheet"),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                var isDashboardExpanded by remember { mutableStateOf(false) }
                var isFaceExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header with School Branding & User Info
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        SchoolBluePrimary,
                                        Color(0xFF1E3A8A)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "سامانه هوشمند نظمیار",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "دبیرستان هوشمند نمونه",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            currentUser?.let { u ->
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = u.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    UserRankBadge(user = u)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "بخش‌های برنامه",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    // 1. خانه (HOME)
                    val isHomeSelected = currentRoute == Screen.Home.route
                    NavigationDrawerItem(
                        selected = isHomeSelected,
                        onClick = {
                            closeDrawer()
                            if (currentRoute != Screen.Home.route) {
                                val popped = navController.popBackStack(Screen.Home.route, false)
                                if (!popped) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isHomeSelected) Icons.Default.Home else Icons.Outlined.Home,
                                contentDescription = "خانه",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "خانه",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = if (isHomeSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                            selectedIconColor = SchoolBluePrimary,
                            selectedTextColor = SchoolBluePrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_item_home")
                    )

                    // 2. مخاطب‌ها (CONTACTS / STUDENTS)
                    val canViewStudents = currentUser?.isCreator == true ||
                        currentUser?.role == UserRole.MANAGER ||
                        currentUser?.role == UserRole.VICE_PRINCIPAL ||
                        userPerms.contains(RankPermission.VIEW_STUDENT_PROFILES) ||
                        userPerms.contains(RankPermission.EDIT_CLASS_INFO)

                    if (canViewStudents) {
                        val isStudentsSelected = currentRoute == Screen.Students.route
                        NavigationDrawerItem(
                            selected = isStudentsSelected,
                            onClick = {
                                closeDrawer()
                                if (currentRoute != Screen.Students.route) {
                                    navController.navigate(Screen.Students.route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isStudentsSelected) Icons.Default.Group else Icons.Outlined.Group,
                                    contentDescription = "مخاطب‌ها",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "مخاطب‌ها",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = if (isStudentsSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                                selectedIconColor = SchoolBluePrimary,
                                selectedTextColor = SchoolBluePrimary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_item_students")
                        )
                    }

                    // 3. داشبورد (نوار باز شو شامل: پیام‌رسان و دریافت کارنامه)
                    val isDashboardActive = currentRoute == Screen.ClassMessenger.route ||
                        currentRoute?.startsWith("class_chat") == true ||
                        currentRoute == Screen.ReportCard.route

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDashboardActive) SchoolBluePrimary.copy(alpha = 0.08f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { isDashboardExpanded = !isDashboardExpanded }
                            .testTag("drawer_dashboard_accordion_parent")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = null,
                                    tint = if (isDashboardActive) SchoolBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "داشبورد",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDashboardActive) SchoolBluePrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            Icon(
                                imageVector = if (isDashboardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isDashboardExpanded) "بستن زیرمنو" else "باز کردن زیرمنو",
                                tint = if (isDashboardActive) SchoolBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isDashboardExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp)
                        ) {
                            // زیرمنوی ۱: پیام‌رسان
                            val isMessengerSelected = currentRoute == Screen.ClassMessenger.route || currentRoute?.startsWith("class_chat") == true
                            NavigationDrawerItem(
                                selected = isMessengerSelected,
                                onClick = {
                                    closeDrawer()
                                    if (currentRoute != Screen.ClassMessenger.route) {
                                        navController.navigate(Screen.ClassMessenger.route) {
                                            popUpTo(Screen.Home.route) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Forum,
                                        contentDescription = "پیام‌رسان",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "پیام‌رسان",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (isMessengerSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                badge = {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2AABEE).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "۶ گروه",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                color = Color(0xFF0288D1),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                                    selectedIconColor = SchoolBluePrimary,
                                    selectedTextColor = SchoolBluePrimary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .testTag("drawer_subitem_class_messenger")
                            )

                            // زیرمنوی ۲: دریافت کارنامه
                            val isReportCardSelected = currentRoute == Screen.ReportCard.route
                            NavigationDrawerItem(
                                selected = isReportCardSelected,
                                onClick = {
                                    closeDrawer()
                                    if (currentRoute != Screen.ReportCard.route) {
                                        navController.navigate(Screen.ReportCard.route) {
                                            popUpTo(Screen.Home.route) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isReportCardSelected) Icons.Default.Description else Icons.Outlined.Description,
                                        contentDescription = "دریافت کارنامه",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "دریافت کارنامه",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (isReportCardSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                                    selectedIconColor = SchoolBluePrimary,
                                    selectedTextColor = SchoolBluePrimary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .testTag("drawer_subitem_report_card")
                            )
                        }
                    }

                    // 4. تشخیص و اسکن چهره (نوار باز شو شامل: مرکز ثبت چهره و اسکن سریع)
                    val canAccessFace = com.example.core.auth.AccessControl.canManageFacesAndBehaviors(currentUser)
                    if (canAccessFace) {
                        val isFaceActive = currentRoute == Screen.FaceEnrollment.route || currentRoute == Screen.FaceScan.route
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isFaceActive) SchoolBluePrimary.copy(alpha = 0.08f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { isFaceExpanded = !isFaceExpanded }
                                .testTag("drawer_face_accordion_parent")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = null,
                                        tint = if (isFaceActive) SchoolBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "تشخیص و اسکن چهره",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFaceActive) SchoolBluePrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }

                                Icon(
                                    imageVector = if (isFaceExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isFaceExpanded) "بستن زیرمنو" else "باز کردن زیرمنو",
                                    tint = if (isFaceActive) SchoolBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isFaceExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 8.dp)
                            ) {
                                // زیرمنوی ۱: مرکز ثبت چهره دانش‌آموز
                                val isEnrollmentSelected = currentRoute == Screen.FaceEnrollment.route
                                NavigationDrawerItem(
                                    selected = isEnrollmentSelected,
                                    onClick = {
                                        closeDrawer()
                                        if (currentRoute != Screen.FaceEnrollment.route) {
                                            navController.navigate(Screen.FaceEnrollment.route) {
                                                popUpTo(Screen.Home.route) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isEnrollmentSelected) Icons.Default.Face else Icons.Outlined.Face,
                                            contentDescription = "مرکز ثبت چهره دانش‌آموز",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "مرکز ثبت چهره دانش‌آموز",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = if (isEnrollmentSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                                        selectedIconColor = SchoolBluePrimary,
                                        selectedTextColor = SchoolBluePrimary,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)
                                        .testTag("drawer_subitem_face_enrollment")
                                )

                                // زیرمنوی ۲: اسکن سریع چهره
                                val isScanSelected = currentRoute == Screen.FaceScan.route
                                NavigationDrawerItem(
                                    selected = isScanSelected,
                                    onClick = {
                                        closeDrawer()
                                        if (currentRoute != Screen.FaceScan.route) {
                                            navController.navigate(Screen.FaceScan.route) {
                                                popUpTo(Screen.Home.route) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isScanSelected) Icons.Default.Face else Icons.Outlined.Face,
                                            contentDescription = "اسکن سریع چهره",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "اسکن سریع چهره",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = if (isScanSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                                        selectedIconColor = SchoolBluePrimary,
                                        selectedTextColor = SchoolBluePrimary,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)
                                        .testTag("drawer_subitem_face_scan")
                                )
                            }
                        }
                    }

                    // 5. مدیریت بنرهای صفحه اول
                    val canManageBanners = currentUser?.isCreator == true ||
                        currentUser?.role == UserRole.MANAGER ||
                        currentUser?.role == UserRole.VICE_PRINCIPAL

                    if (canManageBanners) {
                        val isBannerSelected = currentRoute == Screen.BannerManagement.route
                        NavigationDrawerItem(
                            selected = isBannerSelected,
                            onClick = {
                                closeDrawer()
                                if (currentRoute != Screen.BannerManagement.route) {
                                    navController.navigate(Screen.BannerManagement.route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isBannerSelected) Icons.Default.Campaign else Icons.Outlined.Campaign,
                                    contentDescription = "مدیریت بنرهای صفحه اول",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "مدیریت بنرهای صفحه اول",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = if (isBannerSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                                selectedIconColor = SchoolBluePrimary,
                                selectedTextColor = SchoolBluePrimary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_item_banner_management")
                        )
                    }

                    // 6. پرونده انضباطی ماهانه
                    val canAccessDossier = currentUser?.isCreator == true ||
                        currentUser?.role == UserRole.MANAGER ||
                        currentUser?.role == UserRole.VICE_PRINCIPAL ||
                        currentUser?.role == UserRole.TEACHER ||
                        userPerms.contains(RankPermission.VIEW_STUDENT_PROFILES)

                    if (canAccessDossier) {
                        val isDossierSelected = currentRoute?.startsWith("student_monthly_dossier") == true
                        NavigationDrawerItem(
                            selected = isDossierSelected,
                            onClick = {
                                closeDrawer()
                                if (currentRoute?.startsWith("student_monthly_dossier") != true) {
                                    navController.navigate("student_monthly_dossier") {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isDossierSelected) Icons.Default.Assignment else Icons.Outlined.Assignment,
                                    contentDescription = "پرونده انضباطی ماهانه",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "پرونده انضباطی ماهانه",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = if (isDossierSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            badge = {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = IncidentRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "محرمانه",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            color = IncidentRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                                selectedIconColor = SchoolBluePrimary,
                                selectedTextColor = SchoolBluePrimary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_item_monthly_dossier")
                        )
                    }

                    // 7. بازی‌ها
                    val canAccessGames = currentUser?.isCreator == true ||
                        currentUser?.role == UserRole.MANAGER ||
                        currentUser?.role == UserRole.VICE_PRINCIPAL ||
                        currentUser?.role == UserRole.STUDENT ||
                        currentUser?.role == UserRole.PARENT ||
                        userPerms.contains(RankPermission.ACCESS_GAMES)

                    if (canAccessGames) {
                        val isGamesSelected = currentRoute == Screen.Games.route
                        NavigationDrawerItem(
                            selected = isGamesSelected,
                            onClick = {
                                closeDrawer()
                                if (currentRoute != Screen.Games.route) {
                                    navController.navigate(Screen.Games.route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isGamesSelected) Icons.Default.SportsEsports else Icons.Outlined.SportsEsports,
                                    contentDescription = "بازی‌ها",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "بازی‌ها",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = if (isGamesSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                                selectedIconColor = SchoolBluePrimary,
                                selectedTextColor = SchoolBluePrimary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_item_games")
                        )
                    }

                    // 8. گزارش‌ها
                    val canViewReports = currentUser?.isCreator == true ||
                        currentUser?.role == UserRole.MANAGER ||
                        currentUser?.role == UserRole.VICE_PRINCIPAL ||
                        currentUser?.role == UserRole.PARENT ||
                        userPerms.contains(RankPermission.VIEW_REPORTS)

                    if (canViewReports) {
                        val isReportsSelected = currentRoute == Screen.Reports.route
                        NavigationDrawerItem(
                            selected = isReportsSelected,
                            onClick = {
                                closeDrawer()
                                if (currentRoute != Screen.Reports.route) {
                                    navController.navigate(Screen.Reports.route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isReportsSelected) Icons.Default.Assessment else Icons.Outlined.Assessment,
                                    contentDescription = "گزارش‌ها",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "گزارش‌ها",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = if (isReportsSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            badge = {
                                if (reportsCount > 0) {
                                    Badge(containerColor = SchoolBluePrimary) {
                                        Text(
                                            text = reportsCount.toPersianDigits(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                                selectedIconColor = SchoolBluePrimary,
                                selectedTextColor = SchoolBluePrimary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_item_reports")
                        )
                    }

                    // انتخاب انجمن اولیا و مربیان
                    val isPtaSelected = currentRoute == Screen.PtaVoting.route
                    NavigationDrawerItem(
                        selected = isPtaSelected,
                        onClick = {
                            closeDrawer()
                            if (currentRoute != Screen.PtaVoting.route) {
                                navController.navigate(Screen.PtaVoting.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.HowToVote,
                                contentDescription = "نظرسنجی‌ها",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "نظرسنجی‌ها",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = if (isPtaSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                            selectedIconColor = SchoolBluePrimary,
                            selectedTextColor = SchoolBluePrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_item_pta_voting")
                    )

                    // 9. اعلان‌ها
                    val isNotificationsSelected = currentRoute == Screen.Notifications.route
                    NavigationDrawerItem(
                        selected = isNotificationsSelected,
                        onClick = {
                            closeDrawer()
                            if (currentRoute != Screen.Notifications.route) {
                                navController.navigate(Screen.Notifications.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isNotificationsSelected) Icons.Default.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "اعلان‌ها",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "اعلان‌ها",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = if (isNotificationsSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        badge = {
                            if (unreadNotifs > 0) {
                                Badge(containerColor = IncidentRed) {
                                    Text(
                                        text = unreadNotifs.toPersianDigits(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                            selectedIconColor = SchoolBluePrimary,
                            selectedTextColor = SchoolBluePrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_item_notifications")
                    )

                    // 10. پشتیبانی
                    val isSupportSelected = currentRoute == Screen.Support.route
                    NavigationDrawerItem(
                        selected = isSupportSelected,
                        onClick = {
                            closeDrawer()
                            if (currentRoute != Screen.Support.route) {
                                navController.navigate(Screen.Support.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSupportSelected) Icons.Default.HeadsetMic else Icons.Outlined.HeadsetMic,
                                contentDescription = "پشتیبانی",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "پشتیبانی",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = if (isSupportSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        badge = {
                            if (unreadSupportCount > 0) {
                                Badge(containerColor = IncidentRed) {
                                    Text(
                                        text = unreadSupportCount.toPersianDigits(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                            selectedIconColor = SchoolBluePrimary,
                            selectedTextColor = SchoolBluePrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_item_support")
                    )

                    // 11. پروفایل
                    val isProfileSelected = currentRoute == Screen.Profile.route
                    NavigationDrawerItem(
                        selected = isProfileSelected,
                        onClick = {
                            closeDrawer()
                            if (currentRoute != Screen.Profile.route) {
                                navController.navigate(Screen.Profile.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isProfileSelected) Icons.Default.Person else Icons.Outlined.Person,
                                contentDescription = "پروفایل",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "پروفایل",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = if (isProfileSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                            selectedIconColor = SchoolBluePrimary,
                            selectedTextColor = SchoolBluePrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_item_profile")
                    )

                    // Direct Quick Action Shortcuts
                    val canRegisterPos = currentUser?.isCreator == true ||
                            userPerms.contains(RankPermission.REGISTER_POSITIVE)

                    val canRegisterDisc = currentUser?.isCreator == true ||
                            userPerms.contains(RankPermission.REGISTER_DISCIPLINARY)

                    val canRegisterPrayer = currentUser?.isCreator == true ||
                            userPerms.contains(RankPermission.REGISTER_PRAYER) ||
                            canRegisterPos ||
                            canRegisterDisc

                    if (canRegisterPos || canRegisterDisc || canRegisterPrayer) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Text(
                            text = "عملیات سریع",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )

                        if (canRegisterPos) {
                            val isPosSelected = currentRoute == Screen.RegisterPositive.route
                            NavigationDrawerItem(
                                selected = isPosSelected,
                                onClick = {
                                    closeDrawer()
                                    navController.navigate(Screen.RegisterPositive.route)
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = "ثبت رفتار مثبت",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "ثبت رفتار مثبت",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (isPosSelected) FontWeight.Bold else FontWeight.Light
                                        )
                                    )
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color(0xFF16A34A).copy(alpha = 0.12f),
                                    selectedIconColor = Color(0xFF16A34A),
                                    selectedTextColor = Color(0xFF16A34A),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 3.dp)
                                    .testTag("drawer_quick_positive")
                            )
                        }

                        if (canRegisterDisc) {
                            val isDiscSelected = currentRoute == Screen.RegisterDisciplinary.route
                            NavigationDrawerItem(
                                selected = isDiscSelected,
                                onClick = {
                                    closeDrawer()
                                    navController.navigate(Screen.RegisterDisciplinary.route)
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = "ثبت گزارش انضباطی",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "ثبت گزارش انضباطی",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (isDiscSelected) FontWeight.Bold else FontWeight.Light
                                        )
                                    )
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color(0xFFDC2626).copy(alpha = 0.12f),
                                    selectedIconColor = Color(0xFFDC2626),
                                    selectedTextColor = Color(0xFFDC2626),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 3.dp)
                                    .testTag("drawer_quick_disciplinary")
                            )
                        }

                        if (canRegisterPrayer) {
                            val isPrayerSelected = currentRoute == Screen.RegisterPrayer.route
                            NavigationDrawerItem(
                                selected = isPrayerSelected,
                                onClick = {
                                    closeDrawer()
                                    navController.navigate(Screen.RegisterPrayer.route)
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.VolunteerActivism,
                                        contentDescription = "ثبت نماز خواندن",
                                        tint = Color(0xFF0D9488),
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "ثبت نماز خواندن",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (isPrayerSelected) FontWeight.Bold else FontWeight.Light
                                        )
                                    )
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color(0xFF0D9488).copy(alpha = 0.12f),
                                    selectedIconColor = Color(0xFF0D9488),
                                    selectedTextColor = Color(0xFF0D9488),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 3.dp)
                                    .testTag("drawer_quick_prayer")
                            )
                        }
                    }

                    // Creator-only Rank Management & App Update Menu Items
                    if (currentUser?.isCreator == true) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val isRankSelected = currentRoute == Screen.RankManagement.route
                        NavigationDrawerItem(
                            selected = isRankSelected,
                            onClick = {
                                closeDrawer()
                                navController.navigate(Screen.RankManagement.route) {
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "مدیریت رنک‌ها",
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "مدیریت رنک‌ها",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = if (isRankSelected) FontWeight.Bold else FontWeight.Light
                                    ),
                                    color = if (isRankSelected) Color(0xFF6D28D9) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFF7C3AED).copy(alpha = 0.12f),
                                selectedIconColor = Color(0xFF7C3AED),
                                selectedTextColor = Color(0xFF7C3AED),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .testTag("drawer_rank_management_button")
                        )

                        val isUpdateSelected = currentRoute == Screen.PublishAppUpdate.route
                        NavigationDrawerItem(
                            selected = isUpdateSelected,
                            onClick = {
                                closeDrawer()
                                navController.navigate(Screen.PublishAppUpdate.route) {
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "انتشار نسخه جدید",
                                    tint = SchoolBluePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "انتشار نسخه جدید",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = if (isUpdateSelected) FontWeight.Bold else FontWeight.Light
                                    ),
                                    color = if (isUpdateSelected) SchoolBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = SchoolBluePrimary.copy(alpha = 0.12f),
                                selectedIconColor = SchoolBluePrimary,
                                selectedTextColor = SchoolBluePrimary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .testTag("drawer_publish_update_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Logout Option in Drawer
                    NavigationDrawerItem(
                        selected = false,
                        onClick = {
                            closeDrawer()
                            authViewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "خروج از حساب",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "خروج از حساب کاربری",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .testTag("drawer_logout_button")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "سامانه هوشمند نظمیار - نسخه ${com.example.BuildConfig.VERSION_NAME}".toPersianDigits(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        // Scaffold without bottom bar (all sections moved to hamburger menu)
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (currentUser != null) {
                    if (currentUser?.role == UserRole.STUDENT) {
                        val u = currentUser!!
                        val isEnrolled = com.example.core.face.FaceRecognitionManager.isFaceEnrolled(u.id) || u.faceRegistered
                        if (!isEnrolled) Screen.FaceRegistrationGate.route else Screen.Home.route
                    } else {
                        Screen.Home.route
                    }
                } else Screen.Login.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = {
                            val u = authViewModel.currentUser.value
                            if (u != null && u.role == UserRole.STUDENT) {
                                val isEnrolled = com.example.core.face.FaceRecognitionManager.isFaceEnrolled(u.id) || u.faceRegistered
                                if (!isEnrolled) {
                                    navController.navigate(Screen.FaceRegistrationGate.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                    return@LoginScreen
                                }
                            }
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToSupport = {
                            navController.navigate(Screen.Support.route)
                        }
                    )
                }

                composable(Screen.Home.route) {
                    val role = if (currentUser?.isCreator == true) UserRole.MANAGER else currentUser?.role
                    when (role) {
                        UserRole.STUDENT -> StudentDashboardScreen(
                            user = currentUser,
                            mainViewModel = mainViewModel,
                            onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                            onNavigateToChess = { navController.navigate(Screen.ChessGame.route) },
                            onNavigateToPositive = { navController.navigate(Screen.RegisterPositive.route) },
                            onNavigateToDisciplinary = { navController.navigate(Screen.RegisterDisciplinary.route) },
                            onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                            onNavigateToStudents = { navController.navigate(Screen.Students.route) },
                            onNavigateToPtaVoting = { navController.navigate(Screen.PtaVoting.route) },
                            onNavigateToReportCard = { navController.navigate(Screen.ReportCard.route) },
                            onMenuClick = openDrawer
                        )
                        UserRole.PARENT -> ParentDashboardScreen(
                            user = currentUser,
                            mainViewModel = mainViewModel,
                            onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                            onNavigateToPtaVoting = { navController.navigate(Screen.PtaVoting.route) },
                            onMenuClick = openDrawer
                        )
                        UserRole.TEACHER -> TeacherDashboardScreen(
                            user = currentUser,
                            mainViewModel = mainViewModel,
                            onNavigateToPositive = { navController.navigate(Screen.RegisterPositive.route) },
                            onNavigateToDisciplinary = { navController.navigate(Screen.RegisterDisciplinary.route) },
                            onNavigateToPrayer = { navController.navigate(Screen.RegisterPrayer.route) },
                            onNavigateToStudents = { navController.navigate(Screen.Students.route) },
                            onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                            onNavigateToPtaVoting = { navController.navigate(Screen.PtaVoting.route) },
                            onNavigateToFaceScan = { navController.navigate(Screen.FaceScan.route) },
                            onNavigateToFaceEnrollment = { navController.navigate(Screen.FaceEnrollment.route) },
                            onMenuClick = openDrawer
                        )
                        else -> ManagerDashboardScreen(
                            user = currentUser,
                            mainViewModel = mainViewModel,
                            onNavigateToPositive = { navController.navigate(Screen.RegisterPositive.route) },
                            onNavigateToDisciplinary = { navController.navigate(Screen.RegisterDisciplinary.route) },
                            onNavigateToPrayer = { navController.navigate(Screen.RegisterPrayer.route) },
                            onNavigateToStudents = { navController.navigate(Screen.Students.route) },
                            onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                            onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                            onNavigateToRanks = { navController.navigate(Screen.RankManagement.route) },
                            onNavigateToPublishUpdate = { navController.navigate(Screen.PublishAppUpdate.route) },
                            onNavigateToPtaVoting = { navController.navigate(Screen.PtaVoting.route) },
                            onNavigateToBannerManagement = { navController.navigate(Screen.BannerManagement.route) },
                            onNavigateToFaceScan = { navController.navigate(Screen.FaceScan.route) },
                            onNavigateToFaceEnrollment = { navController.navigate(Screen.FaceEnrollment.route) },
                            onMenuClick = openDrawer
                        )
                    }
                }

                composable(Screen.Students.route) {
                    BackHandler {
                        val popped = navController.popBackStack(Screen.Home.route, false)
                        if (!popped) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                    StudentsListScreen(
                        mainViewModel = mainViewModel,
                        authViewModel = authViewModel,
                        onStudentClick = { studentId ->
                            navController.navigate(Screen.StudentProfile.createRoute(studentId))
                        },
                        onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                        onMenuClick = openDrawer,
                        onBackClick = {
                            val popped = navController.popBackStack(Screen.Home.route, false)
                            if (!popped) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                composable(
                    route = Screen.StudentProfile.route,
                    arguments = listOf(navArgument("studentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val studentId = backStackEntry.arguments?.getString("studentId").orEmpty()
                    BackHandler { navController.popBackStack() }
                    StudentProfileScreen(
                        studentId = studentId,
                        mainViewModel = mainViewModel,
                        authViewModel = authViewModel,
                        onBackClick = { navController.popBackStack() },
                        onRegisterPositive = { sId ->
                            mainViewModel.posStudentId.value = sId
                            navController.navigate(Screen.RegisterPositive.route)
                        },
                        onRegisterDisciplinary = { sId ->
                            mainViewModel.discStudentId.value = sId
                            navController.navigate(Screen.RegisterDisciplinary.route)
                        },
                        onNavigateToDossier = { sId ->
                            navController.navigate(Screen.StudentMonthlyDossier.createRoute(sId))
                        }
                    )
                }

                composable(
                    route = Screen.StudentMonthlyDossier.route,
                    arguments = listOf(
                        navArgument("studentId") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        }
                    )
                ) { backStackEntry ->
                    val studentId = backStackEntry.arguments?.getString("studentId")?.ifBlank { null }
                    BackHandler {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                    StudentMonthlyDossierScreen(
                        initialStudentId = studentId,
                        mainViewModel = mainViewModel,
                        authViewModel = authViewModel,
                        onBackClick = {
                            val popped = navController.popBackStack()
                            if (!popped) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                composable(
                    route = Screen.RegisterPositive.route,
                    arguments = listOf(
                        navArgument("studentId") { type = NavType.StringType; defaultValue = "" },
                        navArgument("isLocked") { type = NavType.BoolType; defaultValue = false }
                    )
                ) { backStackEntry ->
                    val studentId = backStackEntry.arguments?.getString("studentId").orEmpty()
                    val isLocked = backStackEntry.arguments?.getBoolean("isLocked") ?: false
                    BackHandler { navController.popBackStack() }
                    RegisterPositiveScreen(
                        initialStudentId = studentId.ifBlank { null },
                        isLockedFromScan = isLocked,
                        mainViewModel = mainViewModel,
                        authViewModel = authViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.RegisterDisciplinary.route,
                    arguments = listOf(
                        navArgument("studentId") { type = NavType.StringType; defaultValue = "" },
                        navArgument("isLocked") { type = NavType.BoolType; defaultValue = false }
                    )
                ) { backStackEntry ->
                    val studentId = backStackEntry.arguments?.getString("studentId").orEmpty()
                    val isLocked = backStackEntry.arguments?.getBoolean("isLocked") ?: false
                    BackHandler { navController.popBackStack() }
                    RegisterDisciplinaryScreen(
                        initialStudentId = studentId.ifBlank { null },
                        isLockedFromScan = isLocked,
                        mainViewModel = mainViewModel,
                        authViewModel = authViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.RegisterPrayer.route,
                    arguments = listOf(
                        navArgument("studentId") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val studentId = backStackEntry.arguments?.getString("studentId").orEmpty()
                    BackHandler { navController.popBackStack() }
                    RegisterPrayerScreen(
                        initialStudentId = studentId.ifBlank { null },
                        mainViewModel = mainViewModel,
                        authViewModel = authViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Reports.route) {
                    ReportsScreen(
                        user = currentUser,
                        mainViewModel = mainViewModel,
                        onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                        onMenuClick = openDrawer
                    )
                }

                composable(Screen.ReportCard.route) {
                    BackHandler {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                    com.example.presentation.reportcard.ReportCardViewScreen(
                        user = currentUser,
                        mainViewModel = mainViewModel,
                        onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                        onMenuClick = openDrawer
                    )
                }

                composable(Screen.Notifications.route) {
                    BackHandler { navController.popBackStack() }
                    NotificationsScreen(
                        mainViewModel = mainViewModel,
                        onBackClick = { navController.popBackStack() },
                        onMenuClick = openDrawer
                    )
                }

                composable(Screen.Games.route) {
                    GamesScreen(
                        authViewModel = authViewModel,
                        onNavigateToChess = { navController.navigate(Screen.ChessGame.route) },
                        onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                        onMenuClick = openDrawer
                    )
                }

                composable(Screen.ChessGame.route) {
                    BackHandler { navController.popBackStack() }
                    ChessScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.RankManagement.route) {
                    BackHandler { navController.popBackStack() }
                    RankManagementScreen(
                        mainViewModel = mainViewModel,
                        authViewModel = authViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.PublishAppUpdate.route) {
                    BackHandler { navController.popBackStack() }
                    PublishAppUpdateScreen(
                        authViewModel = authViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.PtaVoting.route) {
                    BackHandler { navController.popBackStack() }
                    PollsScreen(
                        currentUserId = currentUser?.id ?: "",
                        currentUserRole = currentUser?.role ?: UserRole.STUDENT,
                        currentUser = currentUser,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Polls.route) {
                    BackHandler { navController.popBackStack() }
                    PollsScreen(
                        currentUserId = currentUser?.id ?: "",
                        currentUserRole = currentUser?.role ?: UserRole.STUDENT,
                        currentUser = currentUser,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.BannerManagement.route) {
                    BackHandler { navController.popBackStack() }
                    BannerManagementScreen(
                        user = currentUser,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.ClassMessenger.route) {
                    BackHandler {
                        val popped = navController.popBackStack(Screen.Home.route, false)
                        if (!popped) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                    ClassMessengerListScreen(
                        currentUser = currentUser,
                        onNavigateToChat = { groupId ->
                            navController.navigate(Screen.ClassChat.createRoute(groupId))
                        },
                        onMenuClick = openDrawer
                    )
                }

                composable(
                    route = Screen.ClassChat.route,
                    arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString("groupId").orEmpty()
                    BackHandler { navController.popBackStack() }
                    ClassChatScreen(
                        classGroupId = groupId,
                        currentUser = currentUser,
                        mainViewModel = mainViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        authViewModel = authViewModel,
                        onLogoutClick = {
                            authViewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onMenuClick = openDrawer
                    )
                }

                composable(Screen.FaceScan.route) {
                    BackHandler { navController.popBackStack() }
                    com.example.presentation.face.FaceScanScreen(
                        currentUser = currentUser,
                        mainViewModel = mainViewModel,
                        onNavigateToPositive = { studentId, isLocked ->
                            navController.navigate(Screen.RegisterPositive.createRoute(studentId, isLocked))
                        },
                        onNavigateToDisciplinary = { studentId, isLocked ->
                            navController.navigate(Screen.RegisterDisciplinary.createRoute(studentId, isLocked))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.FaceEnrollment.route) {
                    BackHandler { navController.popBackStack() }
                    com.example.presentation.face.FaceEnrollmentScreen(
                        currentUser = currentUser,
                        mainViewModel = mainViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.FaceRegistrationGate.route) {
                    com.example.presentation.face.StudentFaceRegistrationGateScreen(
                        authViewModel = authViewModel,
                        mainViewModel = mainViewModel,
                        onRegistrationSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.FaceRegistrationGate.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Support.route) {
                    BackHandler { navController.popBackStack() }
                    com.example.presentation.support.SupportScreen(
                        user = currentUser,
                        allStudents = allStudents,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }

        // In-app Update Dialog (Appears automatically when an update is available on the server)
        if (showUpdateDialog && appUpdateInfo != null) {
            InAppUpdateDialog(
                updateInfo = appUpdateInfo!!,
                onDismiss = {
                    dismissedUpdateVersion = appUpdateInfo!!.latestVersionCode
                }
            )
        }
    }
}
