package com.example.presentation.reports

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.domain.model.RecordType
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.model.toPersianDigits
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

@Composable
fun ReportsScreen(
    user: User?,
    mainViewModel: MainViewModel,
    onNotificationClick: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ParentAccessManager.init(context)
    }

    val stats by mainViewModel.stats.collectAsState()
    val records by mainViewModel.records.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()
    val allUsers by mainViewModel.allUsers.collectAsState()
    val allStudents by mainViewModel.students.collectAsState()
    val unreadNotifs = notifications.count { !it.read }

    val isParent = user?.role == UserRole.PARENT
    val parentChild = remember(allStudents, user) {
        allStudents.firstOrNull { s ->
            user?.childStudentIds?.contains(s.id) == true || s.studentCode == user?.studentCode
        }
    }
    val studentCode = parentChild?.studentCode ?: user?.studentCode.orEmpty()
    val studentName = parentChild?.fullName ?: user?.name.orEmpty()

    var refreshTrigger by remember { mutableIntStateOf(0) }
    val isFatherIn = remember(studentCode, allUsers, refreshTrigger) {
        ParentAccessManager.isFatherLoggedIn(context, studentCode, allUsers)
    }
    val isMotherIn = remember(studentCode, allUsers, refreshTrigger) {
        ParentAccessManager.isMotherLoggedIn(context, studentCode, allUsers)
    }
    val bothParentsIn = !isParent || (isFatherIn && isMotherIn)

    var selectedGrade by remember { mutableStateOf("همه پایه‌ها") }
    val grades = listOf("همه پایه‌ها", "پایه هفتم", "پایه هشتم", "پایه نهم")

    val relevantRecords = if (isParent && parentChild != null) {
        records.filter { it.studentId == parentChild.id }
    } else {
        records
    }

    val filteredRecords = if (selectedGrade == "همه پایه‌ها") relevantRecords else relevantRecords.filter { it.studentGrade == selectedGrade }

    val positiveRecords = filteredRecords.filter { it.type.isPositive }
    val disciplinaryRecords = filteredRecords.filter { !it.type.isPositive }

    // Dynamic positive categories grouping
    val positiveCategoryCounts = positiveRecords.groupingBy { it.category }.eachCount()
    val totalPos = positiveRecords.size.coerceAtLeast(1)

    // Dynamic disciplinary categories grouping
    val disciplinaryCategoryCounts = disciplinaryRecords.groupingBy { it.category }.eachCount()
    val totalDisc = disciplinaryRecords.size.coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reports_screen")
    ) {
        item {
            NezamYarHeader(
                user = user,
                unreadNotificationCount = unreadNotifs,
                onNotificationClick = onNotificationClick,
                onMenuClick = onMenuClick
            )
        }

        // Title and Share Report
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "گزارش‌ها و تحلیل آماری",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isParent) "کارنامه و پرونده تحلیلی فرزند" else "تحلیل آماری رفتارهای ثبت‌شده",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (bothParentsIn) {
                    Button(
                        onClick = {
                            val reportText = buildString {
                                appendLine("گزارش سامانه هوشمند نظمیار")
                                if (isParent && parentChild != null) {
                                    appendLine("دانش‌آموز: ${parentChild.fullName} (کد: ${parentChild.studentCode})")
                                }
                                appendLine("پایه: $selectedGrade")
                                appendLine("تعداد کل رخدادها: ${filteredRecords.size}")
                                appendLine("رفتارهای مثبت: ${positiveRecords.size}")
                                appendLine("موارد انضباطی: ${disciplinaryRecords.size}")
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, reportText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "ارسال گزارش نظمیار")
                            context.startActivity(shareIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ارسال گزارش", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Dual-parent check for parent role: if not both logged in, show security lock card
        if (isParent && !bothParentsIn) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("reports_locked_dual_parent_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, WarningAmber.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(WarningAmberContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "قفل گزارش‌ها",
                                tint = WarningAmber,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "گزارش‌ها و نمودارها موقتاً قفل است",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "الزام حضور هم‌زمان هر دو والد (پدر و مادر)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WarningAmber
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "جهت حفظ امنیت و محرمانگی پرونده تحصیلی و تربیتی، دسترسی به گزارش‌های تحلیلی تنها پس از ورود هردو والد (پدر و مادر) به سامانه هوشمند نظمیار فعال خواهد شد.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status Checklist
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                        Text("ورود پدر به سامانه:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                    Text(
                                        text = if (isFatherIn) "تأیید شد ✓" else "در انتظار ورود پدر ⏳",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFatherIn) PositiveGreen else WarningAmber
                                    )
                                }

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
                                        Text("ورود مادر به سامانه:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                    Text(
                                        text = if (isMotherIn) "تأیید شد ✓" else "در انتظار ورود مادر ⏳",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMotherIn) PositiveGreen else WarningAmber
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val shareText = buildString {
                                    appendLine("سلام،")
                                    appendLine("برای بازگشایی گزارش‌ها و کارنامه عملکرد فرزندمان $studentName در برنامه نظمیار، لازم است هردو وارد برنامه شویم.")
                                    appendLine("لطفاً با انتخاب نقش «ولی دانش‌آموز» و ثبت کد دانش‌آموزی زیر وارد برنامه شوید:")
                                    appendLine("کد دانش‌آموزی: $studentCode")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "ارسال راهنمای ورود به همسر")
                                context.startActivity(shareIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_reports_invite_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ارسال پیام ورود و کد به همسر", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                refreshTrigger++
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("refresh_reports_access_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("بررسی مجدد وضعیت ورود همسر")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SchoolBlueContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SchoolBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "توجه: حساب کاربری خود دانش‌آموز به تمامی گزارش‌ها، امتیازات و رخدادها دسترسی مستقیم دارد. این الزام امنیتی صرفاً برای اطمینان از اطلاع هم‌زمان پدر و مادر در پنل والدین اعمال گردیده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSchoolBlueContainer,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            // When unlocked for parents, show celebratory confirmation banner
            if (isParent) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = PositiveGreenContainer,
                        border = BorderStroke(1.dp, PositiveGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PositiveGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "دسترسی کامل اولیا فعال گردید: هر دو والد (پدر و مادر) وارد برنامه شده‌اند و گزارش‌های تفصیلی بازگشایی شد ✓",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = OnPositiveGreenContainer
                            )
                        }
                    }
                }
            }

            // Grade Filters
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "تفکیک بر اساس پایه تحصیلی:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(grades) { g ->
                            FilterChip(
                                selected = selectedGrade == g,
                                onClick = { selectedGrade = g },
                                label = { Text(g) }
                            )
                        }
                    }
                }
            }

            // Real Summary Metrics
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "رفتارهای مثبت",
                            value = filteredRecords.count { it.type == RecordType.POSITIVE }.toString(),
                            icon = Icons.Default.CheckCircle,
                            containerColor = PositiveGreenContainer,
                            contentColor = OnPositiveGreenContainer,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "افتخارات و تشویق‌ها",
                            value = filteredRecords.count { it.type == RecordType.ACHIEVEMENT || it.type == RecordType.REWARD }.toString(),
                            icon = Icons.Default.EmojiEvents,
                            containerColor = WarningAmberContainer,
                            contentColor = OnWarningAmberContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "تذکرات انضباطی",
                            value = filteredRecords.count { it.type == RecordType.WARNING }.toString(),
                            icon = Icons.Default.Warning,
                            containerColor = WarningAmberContainer.copy(alpha = 0.5f),
                            contentColor = OnWarningAmberContainer,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "موارد و تخلفات",
                            value = filteredRecords.count { it.type == RecordType.DISCIPLINARY }.toString(),
                            icon = Icons.Default.Report,
                            containerColor = IncidentRedContainer,
                            contentColor = OnIncidentRedContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Dynamic Positive Categories Breakdown
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "فراوانی دسته‌بندی رفتارهای مثبت",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PositiveGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (positiveCategoryCounts.isEmpty()) {
                            Text(
                                text = "رکوردی ثبت نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            positiveCategoryCounts.entries.forEach { (cat, count) ->
                                val pct = count.toFloat() / totalPos
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, style = MaterialTheme.typography.bodySmall)
                                        Text("${count.toPersianDigits()} مورد (${(pct * 100).toInt()}%".toPersianDigits() + ")", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = PositiveGreen,
                                        trackColor = PositiveGreenContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dynamic Disciplinary Categories Breakdown
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "فراوانی دسته‌بندی موارد انضباطی و تذکرات",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = IncidentRed
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (disciplinaryCategoryCounts.isEmpty()) {
                            Text(
                                text = "مورد انضباطی ثبت نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            disciplinaryCategoryCounts.entries.forEach { (cat, count) ->
                                val pct = count.toFloat() / totalDisc
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, style = MaterialTheme.typography.bodySmall)
                                        Text("${count.toPersianDigits()} مورد (${(pct * 100).toInt()}%".toPersianDigits() + ")", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = IncidentRed,
                                        trackColor = IncidentRedContainer
                                    )
                                }
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

