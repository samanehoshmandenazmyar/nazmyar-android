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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.demo.DemoData
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.presentation.components.BehaviorRecordCard
import com.example.presentation.components.NezamYarHeader
import com.example.presentation.components.StatCard
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.IncidentRedContainer
import com.example.ui.theme.OnIncidentRedContainer
import com.example.ui.theme.OnPositiveGreenContainer
import com.example.ui.theme.OnSchoolBlueContainer
import com.example.ui.theme.OnWarningAmberContainer
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenContainer
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import com.example.domain.model.RankPermission

@Composable
fun ManagerDashboardScreen(
    user: User?,
    mainViewModel: MainViewModel,
    onNavigateToPositive: () -> Unit,
    onNavigateToDisciplinary: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToRanks: () -> Unit = {},
    onNavigateToPublishUpdate: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val stats by mainViewModel.stats.collectAsState()
    val records by mainViewModel.records.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()
    val unreadNotifs = notifications.count { !it.read }

    val positivePct = (stats.positiveRatio * 100).toInt()
    val disciplinaryPct = 100 - positivePct

    val userPerms = user?.permissions ?: emptyList()
    val isCreator = user?.isCreator == true
    val canRegisterPos = isCreator || userPerms.contains(RankPermission.REGISTER_POSITIVE)
    val canRegisterDisc = isCreator || userPerms.contains(RankPermission.REGISTER_DISCIPLINARY)
    val canViewStudents = isCreator || userPerms.contains(RankPermission.VIEW_STUDENT_PROFILES) || userPerms.contains(RankPermission.EDIT_CLASS_INFO)
    val canViewReports = isCreator || userPerms.contains(RankPermission.VIEW_REPORTS)

    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("manager_dashboard")
    ) {
        item {
            NezamYarHeader(
                user = user,
                unreadNotificationCount = unreadNotifs,
                onNotificationClick = onNavigateToNotifications,
                onMenuClick = onMenuClick
            )
        }

        // Welcome banner & School indicator
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SchoolBlueContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "سلام، ${user?.name ?: "مدیر مدرسه"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSchoolBlueContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DemoData.SCHOOL_NAME,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSchoolBlueContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Alert banner if account has no active permissions yet
        if (!isCreator && userPerms.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEF3C7)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "حساب کاربری بدون دسترسی و رنک فعال",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "در حال حاضر هیچ دسترسی یا رنکی به این حساب داده نشده است. سازنده و مالک سامانه (سید حسین موسوی نژاد) دسترسی‌ها و رنک مورد نظر را برای شما فعال خواهد کرد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "دسترسی سریع",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "ثبت رفتار مثبت",
                        icon = Icons.Default.CheckCircle,
                        containerColor = PositiveGreen,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (canRegisterPos) onNavigateToPositive()
                            else permissionDeniedMessage = "شما مجوز «ثبت رفتار مثبت» را ندارید. لطفاً از سازنده سامانه درخواست فعال‌سازی دسترسی نمایید."
                        },
                        tag = "quick_positive_btn"
                    )

                    QuickActionButton(
                        title = "ثبت مورد انضباطی",
                        icon = Icons.Default.Report,
                        containerColor = IncidentRed,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (canRegisterDisc) onNavigateToDisciplinary()
                            else permissionDeniedMessage = "شما مجوز «ثبت مورد انضباطی» را ندارید. لطفاً از سازنده سامانه درخواست فعال‌سازی دسترسی نمایید."
                        },
                        tag = "quick_disciplinary_btn"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "دانش‌آموزان",
                        icon = Icons.Default.Group,
                        containerColor = SchoolBluePrimary,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (canViewStudents) onNavigateToStudents()
                            else permissionDeniedMessage = "شما مجوز «مشاهده لیست دانش‌آموزان» را ندارید. لطفاً از سازنده سامانه درخواست فعال‌سازی دسترسی نمایید."
                        },
                        tag = "quick_students_btn"
                    )

                    QuickActionButton(
                        title = "گزارش‌ها",
                        icon = Icons.Default.Assessment,
                        containerColor = Color(0xFF475569),
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (canViewReports) onNavigateToReports()
                            else permissionDeniedMessage = "شما مجوز «مشاهده گزارش‌ها و آمار» را ندارید. لطفاً از سازنده سامانه درخواست فعال‌سازی دسترسی نمایید."
                        },
                        tag = "quick_reports_btn"
                    )
                }

                if (user?.isCreator == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    QuickActionButton(
                        title = "مدیریت رنک‌ها و سطوح دسترسی (بخش اختصاصی شما)",
                        icon = Icons.Default.Security,
                        containerColor = Color(0xFF7C3AED),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToRanks,
                        tag = "creator_ranks_quick_btn"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    QuickActionButton(
                        title = "آپلود و انتشار نسخه جدید برنامه (ارسال به تمام کلاینت‌ها)",
                        icon = Icons.Default.CloudUpload,
                        containerColor = SchoolBluePrimary,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToPublishUpdate,
                        tag = "creator_publish_update_btn"
                    )
                }
            }
        }

        // Stats Section (Completely Dynamic)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "آمار و وضعیت رفتاری مدرسه",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "تعداد کل دانش‌آموزان",
                        value = stats.totalStudents.toString(),
                        icon = Icons.Default.Group,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = SchoolBluePrimary,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "رفتارهای مثبت",
                        value = stats.totalPositive.toString(),
                        icon = Icons.Default.CheckCircle,
                        containerColor = PositiveGreenContainer,
                        contentColor = OnPositiveGreenContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "تشویق‌ها و افتخارات",
                        value = stats.totalAchievements.toString(),
                        icon = Icons.Default.EmojiEvents,
                        containerColor = WarningAmberContainer,
                        contentColor = OnWarningAmberContainer,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "تذکرهای انضباطی",
                        value = stats.totalWarnings.toString(),
                        icon = Icons.Default.Warning,
                        containerColor = WarningAmberContainer.copy(alpha = 0.5f),
                        contentColor = OnWarningAmberContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "تخلفات انضباطی",
                        value = stats.totalDisciplinary.toString(),
                        icon = Icons.Default.Report,
                        containerColor = IncidentRedContainer,
                        contentColor = OnIncidentRedContainer,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "نیازمند توجه ویژه",
                        value = stats.studentsNeedingAttention.toString(),
                        icon = Icons.Default.PriorityHigh,
                        containerColor = IncidentRedContainer.copy(alpha = 0.6f),
                        contentColor = OnIncidentRedContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Behavioral Climate Progress Card (Dynamic Calculation)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "شاخص رفتارهای مثبت مدرسه",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$positivePct٪".toPersianDigits(),
                            style = MaterialTheme.typography.titleSmall,
                            color = PositiveGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { stats.positiveRatio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PositiveGreen,
                        trackColor = IncidentRedContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$positivePct٪ موارد مثبت و تشویقی".toPersianDigits(),
                            style = MaterialTheme.typography.bodySmall,
                            color = PositiveGreen
                        )
                        Text(
                            text = "$disciplinaryPct٪ تذکر و انضباطی".toPersianDigits(),
                            style = MaterialTheme.typography.bodySmall,
                            color = IncidentRed
                        )
                    }
                }
            }
        }

        // Recent Activities Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخرین فعالیت‌ها",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(onClick = onNavigateToReports) {
                    Text("مشاهده همه", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Recent Activities List
        if (records.isEmpty()) {
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
                            text = "هنوز رخدادی ثبت نشده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(records.take(5), key = { it.id }) { record ->
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

    permissionDeniedMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { permissionDeniedMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = IncidentRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "عدم دسترسی به این بخش",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { permissionDeniedMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                ) {
                    Text("متوجه شدم")
                }
            }
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = modifier
            .height(54.dp)
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
