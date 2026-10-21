package com.example.presentation.dashboard

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.RecordType
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import com.example.core.banner.BannerManager
import com.example.presentation.components.BehaviorRecordCard
import com.example.presentation.components.HomeBannerCarousel
import com.example.presentation.components.NezamYarHeader
import com.example.presentation.components.PtaElectionHomeBanner
import com.example.presentation.components.StatCard
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.OnPositiveGreenContainer
import com.example.ui.theme.OnSchoolBlueContainer
import com.example.ui.theme.OnWarningAmberContainer
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenContainer
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary
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
    }
    val allBanners by BannerManager.banners.collectAsState()
    val allStudents by mainViewModel.students.collectAsState()
    val allRecords by mainViewModel.records.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()

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
        item {
            NezamYarHeader(
                user = user,
                unreadNotificationCount = unreadNotifs,
                onNotificationClick = onNavigateToNotifications,
                onMenuClick = onMenuClick
            )
        }

        // Welcome Parent Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SchoolBlueContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "سلام، ولی گرامی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSchoolBlueContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "پرونده تربیتی و انضباطی فرزند شما در سامانه نظمیار",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSchoolBlueContainer.copy(alpha = 0.85f)
                    )
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

        // گزینه موقت: انتخاب انجمن و اولیا و مربیان
        item {
            PtaElectionHomeBanner(
                onClick = onNavigateToPtaVoting,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Child Switcher (if more than one child)
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
                            label = { Text(child.fullName) }
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
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "فرزند: ${child.fullName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "پایه: ${child.grade}  |  کلاس: ${child.className}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "کد دانش‌آموزی: ${child.studentCode.toPersianDigits()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (child.score >= 90) PositiveGreenContainer else WarningAmberContainer
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "نمره انضباط",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (child.score >= 90) OnPositiveGreenContainer else OnWarningAmberContainer
                                    )
                                    Text(
                                        text = child.score.toPersianDigits(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (child.score >= 90) OnPositiveGreenContainer else OnWarningAmberContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Real Student Metrics
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
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
