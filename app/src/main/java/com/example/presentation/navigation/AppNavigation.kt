package com.example.presentation.navigation

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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Group
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
    data object Students : Screen("students", "لیست مخاطب‌ها", Icons.Default.Group, Icons.Outlined.Group)
    data object Games : Screen("games", "بازی‌ها و شطرنج", Icons.Default.SportsEsports, Icons.Outlined.SportsEsports)
    data object ChessGame : Screen("chess_game", "بازی شطرنج", Icons.Default.SportsEsports, Icons.Outlined.SportsEsports)
    data object RegisterPositive : Screen("register_positive", "ثبت رفتار مثبت", Icons.Default.AddCircle, Icons.Default.AddCircle)
    data object RegisterDisciplinary : Screen("register_disciplinary", "ثبت گزارش انضباطی", Icons.Default.AddCircle, Icons.Default.AddCircle)
    data object Reports : Screen("reports", "گزارش‌ها و آمار", Icons.Default.Assessment, Icons.Outlined.Assessment)
    data object Notifications : Screen("notifications", "اعلان‌ها و رویدادها", Icons.Default.Notifications, Icons.Outlined.Notifications)
    data object Profile : Screen("profile", "پروفایل و تنظیمات", Icons.Default.Person, Icons.Outlined.Person)
    data object RankManagement : Screen("rank_management", "مدیریت رنک‌ها و دسترسی‌ها", Icons.Default.Security, Icons.Outlined.Security)
    data object PublishAppUpdate : Screen("publish_app_update", "آپلود و انتشار آپدیت برنامه", Icons.Default.CloudUpload, Icons.Default.CloudUpload)
    data object StudentProfile : Screen("student_profile/{studentId}", "پرونده دانش‌آموز", Icons.Default.Face, Icons.Outlined.Face) {
        fun createRoute(studentId: String) = "student_profile/$studentId"
    }
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()
    val unreadNotifs = notifications.count {
        !it.read && (currentUser?.role != UserRole.PARENT || it.recipientUserId == currentUser?.id)
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

    // Handle Back press to close drawer if open
    BackHandler(enabled = drawerState.isOpen) {
        closeDrawer()
    }

    LaunchedEffect(currentUser) {
        if (currentUser == null && currentRoute != null && currentRoute != Screen.Login.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val effectiveRole = if (currentUser?.isCreator == true) UserRole.MANAGER else currentUser?.role

    val userPerms = currentUser?.permissions ?: emptyList()

    val drawerItems = when (effectiveRole) {
        UserRole.STUDENT -> {
            val items = mutableListOf<Screen>(Screen.Home)
            if (userPerms.contains(RankPermission.VIEW_STUDENT_PROFILES) || userPerms.contains(RankPermission.EDIT_CLASS_INFO)) {
                items.add(Screen.Students)
            }
            items.add(Screen.Games)
            if (userPerms.contains(RankPermission.VIEW_REPORTS)) {
                items.add(Screen.Reports)
            }
            items.add(Screen.Notifications)
            items.add(Screen.Profile)
            items
        }
        UserRole.PARENT -> listOf(
            Screen.Home,
            Screen.Games,
            Screen.Reports,
            Screen.Notifications,
            Screen.Profile
        )
        else -> {
            if (currentUser?.isCreator == true) {
                listOf(
                    Screen.Home,
                    Screen.Students,
                    Screen.Games,
                    Screen.Reports,
                    Screen.Notifications,
                    Screen.Profile
                )
            } else {
                val items = mutableListOf<Screen>(Screen.Home)
                if (userPerms.contains(RankPermission.VIEW_STUDENT_PROFILES) || userPerms.contains(RankPermission.EDIT_CLASS_INFO)) {
                    items.add(Screen.Students)
                }
                if (userPerms.contains(RankPermission.ACCESS_GAMES)) {
                    items.add(Screen.Games)
                }
                if (userPerms.contains(RankPermission.VIEW_REPORTS)) {
                    items.add(Screen.Reports)
                }
                items.add(Screen.Notifications)
                items.add(Screen.Profile)
                items
            }
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

                    // Main Drawer Navigation Items (transferred from bottom bar)
                    drawerItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationDrawerItem(
                            selected = isSelected,
                            onClick = {
                                closeDrawer()
                                if (currentRoute != screen.route) {
                                    if (screen.route == Screen.Home.route) {
                                        val popped = navController.popBackStack(Screen.Home.route, false)
                                        if (!popped) {
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Home.route) {
                                                saveState = false
                                                inclusive = false
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Light
                                    )
                                )
                            },
                            badge = {
                                if (screen == Screen.Notifications && unreadNotifs > 0) {
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
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .testTag("drawer_item_${screen.route}")
                        )
                    }

                    // Direct Quick Action Shortcuts
                    val canRegisterPos = currentUser?.isCreator == true ||
                            userPerms.contains(RankPermission.REGISTER_POSITIVE)

                    val canRegisterDisc = currentUser?.isCreator == true ||
                            userPerms.contains(RankPermission.REGISTER_DISCIPLINARY)

                    if (canRegisterPos || canRegisterDisc) {
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "مدیریت رنک‌ها و دسترسی‌ها",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (isRankSelected) FontWeight.Bold else FontWeight.Light
                                        ),
                                        color = if (isRankSelected) Color(0xFF6D28D9) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    RgbRankBadge(text = "ویژه", isSmall = true)
                                }
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
                                    contentDescription = "آپلود و انتشار آپدیت برنامه",
                                    tint = SchoolBluePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "آپلود و انتشار نسخه جدید",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (isUpdateSelected) FontWeight.Bold else FontWeight.Light
                                        ),
                                        color = if (isUpdateSelected) SchoolBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    RgbRankBadge(text = "سازنده", isSmall = true)
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
                startDestination = if (currentUser != null) Screen.Home.route else Screen.Login.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
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
                            onMenuClick = openDrawer
                        )
                        UserRole.PARENT -> ParentDashboardScreen(
                            user = currentUser,
                            mainViewModel = mainViewModel,
                            onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                            onMenuClick = openDrawer
                        )
                        UserRole.TEACHER -> TeacherDashboardScreen(
                            user = currentUser,
                            mainViewModel = mainViewModel,
                            onNavigateToPositive = { navController.navigate(Screen.RegisterPositive.route) },
                            onNavigateToDisciplinary = { navController.navigate(Screen.RegisterDisciplinary.route) },
                            onNavigateToStudents = { navController.navigate(Screen.Students.route) },
                            onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                            onMenuClick = openDrawer
                        )
                        else -> ManagerDashboardScreen(
                            user = currentUser,
                            mainViewModel = mainViewModel,
                            onNavigateToPositive = { navController.navigate(Screen.RegisterPositive.route) },
                            onNavigateToDisciplinary = { navController.navigate(Screen.RegisterDisciplinary.route) },
                            onNavigateToStudents = { navController.navigate(Screen.Students.route) },
                            onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                            onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                            onNavigateToRanks = { navController.navigate(Screen.RankManagement.route) },
                            onNavigateToPublishUpdate = { navController.navigate(Screen.PublishAppUpdate.route) },
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
                        }
                    )
                }

                composable(Screen.RegisterPositive.route) {
                    BackHandler { navController.popBackStack() }
                    RegisterPositiveScreen(
                        mainViewModel = mainViewModel,
                        authViewModel = authViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.RegisterDisciplinary.route) {
                    BackHandler { navController.popBackStack() }
                    RegisterDisciplinaryScreen(
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
