package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.core.auth.AccessControl
import com.example.core.banner.BannerManager
import com.example.domain.model.User
import com.example.presentation.components.BehaviorRecordCard
import com.example.presentation.components.HomeBannerCarousel
import com.example.presentation.components.NezamYarHeader
import com.example.presentation.components.PtaElectionHomeBanner
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.OnSchoolBlueContainer
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary

@Composable
fun TeacherDashboardScreen(
    user: User?,
    mainViewModel: MainViewModel,
    onNavigateToPositive: () -> Unit,
    onNavigateToDisciplinary: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPrayer: () -> Unit = {},
    onNavigateToPtaVoting: () -> Unit = {},
    onNavigateToFaceScan: () -> Unit = {},
    onNavigateToFaceEnrollment: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        BannerManager.init(context)
    }
    val allBanners by BannerManager.banners.collectAsState()
    val teacherBanners = remember(allBanners, user) {
        BannerManager.getBannersForUser(user)
    }
    val classes by mainViewModel.classes.collectAsState()
    val records by mainViewModel.records.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()
    val unreadNotifs = notifications.count { !it.read }

    val assignedClassNames = user?.assignedClasses ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("teacher_dashboard")
    ) {
        item {
            NezamYarHeader(
                user = user,
                unreadNotificationCount = unreadNotifs,
                onNotificationClick = onNavigateToNotifications,
                onMenuClick = onMenuClick
            )
        }

        // Welcome
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
                        text = "سلام، ${user?.name ?: "دبیر محترم"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSchoolBlueContainer
                    )
                    if (assignedClassNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "کلاس‌های تخصیص‌یافته: ${assignedClassNames.joinToString("، ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSchoolBlueContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 16:9 Auto-sliding Home Banners Carousel
        if (teacherBanners.isNotEmpty()) {
            item {
                HomeBannerCarousel(
                    banners = teacherBanners,
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

        // Quick Register Actions
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "ثبت سریع رفتار کلاسی",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (AccessControl.canManageFacesAndBehaviors(user)) {
                    Surface(
                        onClick = onNavigateToFaceScan,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("teacher_face_scan_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = SchoolBluePrimary,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "اسکن سریع چهره دانش‌آموز",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "شناسایی هوشمند چهره با دوربین و ثبت فوری رفتار",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        onClick = onNavigateToFaceEnrollment,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("teacher_face_enrollment_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = PositiveGreen,
                                    modifier = Modifier.size(44.dp)
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
                                        text = "مرکز ثبت چهره دانش‌آموزان",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "ثبت چهره و تست تفکیک چهره اصلی میان دانش‌آموزان",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "ثبت رفتار مثبت / فعالیت",
                        icon = Icons.Default.CheckCircle,
                        containerColor = PositiveGreen,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPositive,
                        tag = "teacher_quick_positive"
                    )

                    QuickActionButton(
                        title = "ثبت تذکر کلاسی",
                        icon = Icons.Default.Report,
                        containerColor = IncidentRed,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDisciplinary,
                        tag = "teacher_quick_disciplinary"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                QuickActionButton(
                    title = "ثبت نماز خواندن",
                    icon = Icons.Default.VolunteerActivism,
                    containerColor = Color(0xFF0D9488),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToPrayer,
                    tag = "teacher_quick_prayer"
                )
            }
        }

        // Assigned Classes Cards
        if (assignedClassNames.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "کلاس‌های من",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        assignedClassNames.forEach { className ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        mainViewModel.selectedClass.value = className
                                        onNavigateToStudents()
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = className,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SchoolBluePrimary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Class,
                                            contentDescription = className,
                                            tint = SchoolBluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "مشاهده مخاطب‌ها",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Class Activities
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "آخرین وقایع کلاسی",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        val filteredRecords = if (assignedClassNames.isEmpty()) records else records.filter { assignedClassNames.contains(it.studentClass) }
        if (filteredRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "موردی برای نمایش وجود ندارد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                BehaviorRecordCard(
                    record = record,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    showStudentName = true
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
