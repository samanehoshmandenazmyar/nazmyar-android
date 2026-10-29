package com.example.presentation.dashboard

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.auth.ParentAccessManager
import com.example.core.banner.BannerManager
import com.example.domain.model.RecordType
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.presentation.components.BehaviorRecordCard
import com.example.presentation.components.HomeBannerCarousel
import com.example.presentation.components.NezamYarHeader
import com.example.presentation.components.PtaElectionHomeBanner
import com.example.presentation.components.StatCard
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.IncidentRedContainer
import com.example.ui.theme.OnIncidentRedContainer
import com.example.ui.theme.OnPositiveGreenContainer
import com.example.ui.theme.OnSchoolBlueContainer
import com.example.ui.theme.OnWarningAmberContainer
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenContainer
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@Composable
fun ParentDashboardScreen(
    user: User?,
    mainViewModel: MainViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPtaVoting: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        BannerManager.init(context)
        ParentAccessManager.init(context)
    }

    val allBanners by BannerManager.banners.collectAsState()
    val allStudents by mainViewModel.students.collectAsState()
    val allRecords by mainViewModel.records.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()
    val allUsers by mainViewModel.allUsers.collectAsState()

    val parentChildIds = user?.childStudentIds ?: emptyList()
    val children = allStudents.filter { parentChildIds.contains(it.id) || it.studentCode == user?.studentCode }.ifEmpty {
        allStudents.take(1)
    }

    val parentBanners = remember(allBanners, children, user) {
        BannerManager.getBannersForUser(
            user = user,
            parentChildren = children.map { Pair(it.grade, it.className) }
        )
    }

    var selectedChildId by remember(children) {
        mutableStateOf(children.firstOrNull()?.id ?: "")
    }

    val currentChild = children.firstOrNull { it.id == selectedChildId } ?: children.firstOrNull()
    val studentCode = currentChild?.studentCode.orEmpty()

    // Dual-parent verification check: both father and mother must have logged in
    val isFatherIn = remember(studentCode, allUsers) {
        ParentAccessManager.isFatherLoggedIn(context, studentCode, allUsers)
    }
    val isMotherIn = remember(studentCode, allUsers) {
        ParentAccessManager.isMotherLoggedIn(context, studentCode, allUsers)
    }
    val bothParentsIn = isFatherIn && isMotherIn

    val isCurrentParentMother = user?.gender?.contains("مادر") == true ||
        user?.rankTitle?.contains("مادر") == true ||
        user?.id?.endsWith("_mom") == true

    // When both parents are detected as logged in, send celebratory unlock notification once
    LaunchedEffect(bothParentsIn, studentCode) {
        if (bothParentsIn && studentCode.isNotBlank() && currentChild != null) {
            val shouldNotify = ParentAccessManager.checkAndMarkNotificationSent(context, studentCode)
            if (shouldNotify) {
                mainViewModel.notifyBothParentsUnlocked(
                    studentCode = studentCode,
                    studentName = currentChild.fullName,
                    studentId = currentChild.id,
                    schoolId = currentChild.schoolId
                )
            }
        }
    }

    val childRecords = allRecords.filter { it.studentId == currentChild?.id }
    val unreadNotifs = notifications.count { !it.read && it.recipientUserId == user?.id }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("موفقیت‌ها", "تشویق‌ها", "تذکرها", "سوابق کامل")

    val displayedRecords = when (selectedTab) {
        0 -> childRecords.filter { it.type == RecordType.ACHIEVEMENT }
        1 -> childRecords.filter { it.type == RecordType.POSITIVE || it.type == RecordType.REWARD }
        2 -> childRecords.filter { it.type == RecordType.WARNING || it.type == RecordType.DISCIPLINARY }
        else -> childRecords
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("parent_dashboard")
    ) {
        // App Header
        item {
            NezamYarHeader(
                user = user,
                unreadNotificationCount = unreadNotifs,
                onNotificationClick = onNavigateToNotifications,
                onMenuClick = onMenuClick
            )
        }

        // Welcome / Dual-Parent Status Card
        item {
            if (bothParentsIn) {
                // Both father and mother logged in -> Full Access Active
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("parent_access_unlocked_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PositiveGreen.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PositiveGreen,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "دسترسی کامل اولیا فعال گردید ✓",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PositiveGreen,
                                fontFamily = VazirmatnFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ورود هر دو ولی (پدر و مادر) در سامانه ثبت شده است و کلیه گزارش‌ها و اطلاعات پرونده برای شما باز است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }
                }
            } else {
                // One parent logged in, waiting for other parent
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("parent_access_pending_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFB45309),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "مشاهده اطلاعات مشروط به ورود هر دو ولی است",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                fontFamily = VazirmatnFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "جهت بازگشایی پرونده و ارسال گزارش‌ها، ورود هر دو ولی (پدر و مادر) به برنامه الزامی است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F),
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }
                }
            }
        }

        // 16:9 Auto-sliding Home Banners Carousel
        if (parentBanners.isNotEmpty()) {
            item {
                HomeBannerCarousel(
                    banners = parentBanners,
                    canManage = false,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // PTA Election Banner
        item {
            PtaElectionHomeBanner(
                onClick = onNavigateToPtaVoting,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Child Switcher (if multiple children)
        if (children.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    children.forEach { child ->
                        FilterChip(
                            selected = child.id == selectedChildId,
                            onClick = { selectedChildId = child.id },
                            label = { Text(child.fullName, fontFamily = VazirmatnFontFamily) }
                        )
                    }
                }
            }
        }

        // Child Profile Card
        currentChild?.let { child ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("parent_child_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(SchoolBlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${child.firstName.take(1)}${child.lastName.take(1)}",
                                    color = SchoolBluePrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "فرزند: ${child.fullName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "پایه: ${child.grade}  |  کلاس: ${child.className}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Text(
                                    text = "کد دانش‌آموزی: ${child.studentCode.toPersianDigits()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = VazirmatnFontFamily
                                )
                            }

                            if (bothParentsIn) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = PositiveGreenContainer
                                    ) {
                                        Text(
                                            text = "${child.positiveCount.toPersianDigits()}+",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = OnPositiveGreenContainer,
                                            fontFamily = VazirmatnFontFamily,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = IncidentRedContainer
                                    ) {
                                        Text(
                                            text = "${child.disciplinaryCount.toPersianDigits()}-",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = OnIncidentRedContainer,
                                            fontFamily = VazirmatnFontFamily,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "قفل نمره",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF92400E),
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = VazirmatnFontFamily
                                        )
                                    }
                                }
                            }
                        }

                        // If both parents are in, show the metric stats
                        if (bothParentsIn) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatCard(
                                    title = "موفقیت‌ها",
                                    value = child.achievementCount.toString(),
                                    icon = Icons.Default.EmojiEvents,
                                    containerColor = WarningAmberContainer,
                                    contentColor = OnWarningAmberContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "رفتار مثبت",
                                    value = child.positiveCount.toString(),
                                    icon = Icons.Default.CheckCircle,
                                    containerColor = PositiveGreenContainer,
                                    contentColor = OnPositiveGreenContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "تذکرات",
                                    value = (child.warningCount + child.disciplinaryCount).toString(),
                                    icon = Icons.Default.Warning,
                                    containerColor = if (child.warningCount > 0) WarningAmberContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (child.warningCount > 0) OnWarningAmberContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gated Content: If both parents logged in -> Show Tabs and Behavior Records
        if (bothParentsIn) {
            // Filter Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = VazirmatnFontFamily
                                )
                            }
                        )
                    }
                }
            }

            // Section Title
            item {
                Text(
                    text = "${tabTitles[selectedTab]} (${displayedRecords.size.toPersianDigits()} مورد)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (displayedRecords.isEmpty()) {
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "بدون مورد",
                                tint = PositiveGreen,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "موردی در این دسته ثبت نشده است.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }
                }
            } else {
                items(displayedRecords, key = { it.id }) { record ->
                    BehaviorRecordCard(
                        record = record,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        showStudentName = false
                    )
                }
            }
        } else {
            // When only ONE parent is logged in: Show the Locked Dual-Parent Verification Card
            currentChild?.let { child ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("dual_parent_lock_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "قفل مشاهده اطلاعات و گزارش‌های فرزند",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                textAlign = TextAlign.Center,
                                fontFamily = VazirmatnFontFamily
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "طبق قوانین نظارت و هماهنگی تربیتی مدرسه، مشاهده سوابق انضباطی، کارنامه رفتاری و گزارش‌های «${child.fullName}» منوط به حضور همزمان هر دو ولی گرامی (پدر و مادر) در برنامه است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                fontFamily = VazirmatnFontFamily
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Verification status checklist
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Father Status Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isFatherIn) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                                contentDescription = null,
                                                tint = if (isFatherIn) PositiveGreen else WarningAmber,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "ورود پدر دانش‌آموز",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isFatherIn) PositiveGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = if (isFatherIn) "انجام شد ✓" else "در انتظار ورود...",
                                                color = if (isFatherIn) PositiveGreen else Color(0xFFB45309),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        }
                                    }

                                    // Mother Status Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isMotherIn) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                                contentDescription = null,
                                                tint = if (isMotherIn) PositiveGreen else WarningAmber,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "ورود مادر دانش‌آموز",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isMotherIn) PositiveGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = if (isMotherIn) "انجام شد ✓" else "در انتظار ورود...",
                                                color = if (isMotherIn) PositiveGreen else Color(0xFFB45309),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Guidance for the current logged-in parent
                            val waitingRole = if (isCurrentParentMother) "پدر دانش‌آموز" else "مادر دانش‌آموز"
                            Text(
                                text = if (isCurrentParentMother) {
                                    "مادر گرامی، حساب شما فعال است. لطفاً از پدر دانش‌آموز بخواهید با نصب برنامه و انتخاب گزینه «ولی دانش‌آموز (پدر)» و وارد کردن کد ${child.studentCode.toPersianDigits()} وارد برنامه شوند تا تمام گزارش‌ها برای هر دوی شما ارسال و فعال گردد."
                                } else {
                                    "پدر گرامی، حساب شما فعال است. لطفاً از مادر دانش‌آموز بخواهید با نصب برنامه و انتخاب گزینه «ولی دانش‌آموز (مادر)» و وارد کردن کد ${child.studentCode.toPersianDigits()} وارد برنامه شوند تا تمام گزارش‌ها برای هر دوی شما ارسال و فعال گردد."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F),
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp,
                                fontFamily = VazirmatnFontFamily
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Share invite button
                            Button(
                                onClick = {
                                    val shareText = buildString {
                                        appendLine("سامانه هوشمند نظم‌یار مدرسه")
                                        appendLine("سلام، لطفاً جهت فعال‌سازی و مشاهده گزارش‌ها و پرونده تحصیلی و انضباطی فرزندمان «${child.fullName}»، برنامه نظم‌یار را نصب کرده و با مشخصات زیر وارد شوید:")
                                        appendLine("نقش: ولی دانش‌آموز ($waitingRole)")
                                        appendLine("نام فرزند: ${child.fullName}")
                                        appendLine("کد ۶ رقمی دانش‌آموزی: ${child.studentCode}")
                                        appendLine("پس از ورود شما، دسترسی به کلیه اطلاعات فرزندمان برای هر دوی ما باز خواهد شد.")
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "ارسال راهنمای ورود به همسر")
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("share_parent_invite_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ارسال راهنمای ورود و کد به همسر",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Refresh button
                            OutlinedButton(
                                onClick = {
                                    mainViewModel.refreshData()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("refresh_parent_status_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "بررسی مجدد وضعیت ورود همسر",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
