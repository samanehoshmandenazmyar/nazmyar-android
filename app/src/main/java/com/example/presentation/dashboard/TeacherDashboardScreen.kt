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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.domain.model.User
import com.example.presentation.components.BehaviorRecordCard
import com.example.presentation.components.NezamYarHeader
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
    onMenuClick: () -> Unit = {}
) {
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
                                        text = "مشاهده دانش‌آموزان",
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
