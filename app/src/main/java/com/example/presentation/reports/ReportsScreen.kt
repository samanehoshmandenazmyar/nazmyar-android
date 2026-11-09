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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.example.core.utils.DateTimeUtils
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.domain.model.BehaviorRecord
import com.example.ui.theme.VazirmatnFontFamily
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

    val hiddenReportIds by mainViewModel.hiddenFromReportsRecordIds.collectAsState()
    var recordToHideInReports by remember { mutableStateOf<BehaviorRecord?>(null) }

    val visibleRecords = remember(records, hiddenReportIds) {
        records.filter { it.id !in hiddenReportIds }
    }

    var selectedClassFilter by remember { mutableStateOf("همه") }
    val classFilters = listOf("همه", "هفتم یک", "هفتم دو", "هشتم یک", "هشتم دو", "نهم یک", "نهم دو")

    val relevantRecords = if (isParent && parentChild != null) {
        visibleRecords.filter { it.studentId == parentChild.id }
    } else {
        visibleRecords
    }

    val filteredRecords = if (selectedClassFilter == "همه" || isParent) {
        relevantRecords
    } else {
        relevantRecords.filter { matchesClassFilter(it, selectedClassFilter) }
    }

    var selectedTypeFilter by remember { mutableStateOf("همه موارد") }
    val typeFilters = listOf("همه موارد", "رفتارهای مثبت (+)", "موارد انضباطی (-)")
    var viewingPhotoUrl by remember { mutableStateOf<String?>(null) }

    val displayRecords = remember(filteredRecords, selectedTypeFilter) {
        when (selectedTypeFilter) {
            "رفتارهای مثبت (+)" -> filteredRecords.filter { it.type.isPositive }
            "موارد انضباطی (-)" -> filteredRecords.filter { !it.type.isPositive }
            else -> filteredRecords
        }
    }

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
                        text = "گزارش‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isParent) "کارنامه و پرونده تحلیلی فرزند" else "تحلیل و بررسی رفتارهای ثبت‌شده دانش‌آموزان",
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
                                appendLine("کلاس/پایه: $selectedClassFilter")
                                appendLine("تعداد کل رخدادها: ${filteredRecords.size}")
                                appendLine("رفتارهای مثبت: ${filteredRecords.count { it.type.isPositive }}")
                                appendLine("موارد انضباطی: ${filteredRecords.count { !it.type.isPositive }}")
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

            // Filter Chips: Type Filter
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "نوع رفتار:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = VazirmatnFontFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(typeFilters) { tf ->
                            FilterChip(
                                selected = selectedTypeFilter == tf,
                                onClick = { selectedTypeFilter = tf },
                                label = { Text(tf, fontFamily = VazirmatnFontFamily) }
                            )
                        }
                    }
                }
            }

            // Grade & Class Filters (For staff)
            if (!isParent) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            text = "تفکیک بر اساس کلاس و پایه:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = VazirmatnFontFamily
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(classFilters) { cf ->
                                FilterChip(
                                    selected = selectedClassFilter == cf,
                                    onClick = { selectedClassFilter = cf },
                                    label = { Text(cf, fontFamily = VazirmatnFontFamily) }
                                )
                            }
                        }
                    }
                }
            }

            // Counter Header Strip
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سوابق ثبت‌شده (${displayRecords.size.toPersianDigits()} مورد)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = PositiveGreenContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "+${displayRecords.count { it.type.isPositive }.toPersianDigits()} مثبت",
                                    color = PositiveGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                color = IncidentRedContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "-${displayRecords.count { !it.type.isPositive }.toPersianDigits()} انضباطی",
                                    color = IncidentRed,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Items List
            if (displayRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "هیچ سابقه رفتاری یا انضباطی در این بخش ثبت نشده است.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }
                }
            } else {
                items(displayRecords, key = { it.id }) { record ->
                    BehaviorRecordItemCard(
                        record = record,
                        onImageClick = { viewingPhotoUrl = it },
                        onDeleteClick = { recordToHideInReports = record }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (recordToHideInReports != null) {
        AlertDialog(
            onDismissRequest = { recordToHideInReports = null },
            title = {
                Text(
                    text = "حذف گزارش",
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily
                )
            },
            text = {
                Text(
                    text = "آیا از حذف این گزارش از این بخش اطمینان دارید؟\nاین مورد صرفاً از فهرست گزارش‌ها حذف می‌شود و در پرونده انضباطی دانش‌آموز باقی خواهد ماند.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = VazirmatnFontFamily,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        recordToHideInReports?.let { rec ->
                            mainViewModel.hideRecordFromReports(rec.id)
                        }
                        recordToHideInReports = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncidentRed)
                ) {
                    Text("حذف از گزارش‌ها", fontFamily = VazirmatnFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { recordToHideInReports = null }
                ) {
                    Text("انصراف", fontFamily = VazirmatnFontFamily)
                }
            }
        )
    }

    if (viewingPhotoUrl != null) {
        AlertDialog(
            onDismissRequest = { viewingPhotoUrl = null },
            confirmButton = {
                Button(
                    onClick = { viewingPhotoUrl = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                ) {
                    Text("بستن تصویر", fontFamily = VazirmatnFontFamily)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = viewingPhotoUrl,
                        contentDescription = "تصویر پیوست گزارش",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        )
    }
}

@Composable
private fun BehaviorRecordItemCard(
    record: BehaviorRecord,
    onImageClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val isPos = record.type.isPositive
    val cardBorderColor = if (isPos) PositiveGreen.copy(alpha = 0.4f) else IncidentRed.copy(alpha = 0.4f)
    val accentColor = if (isPos) PositiveGreen else IncidentRed
    val containerBg = if (isPos) PositiveGreenContainer.copy(alpha = 0.2f) else IncidentRedContainer.copy(alpha = 0.2f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("record_card_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Student Name + Class & Badge Points + Delete from reports
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(containerBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPos) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = record.studentName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily
                        )
                        Text(
                            text = "${record.studentGrade} • ${record.studentClass}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = VazirmatnFontFamily
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Points Badge
                    record.points?.let { pts ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPos) PositiveGreenContainer else IncidentRedContainer
                        ) {
                            Text(
                                text = if (isPos) "+${pts.toPersianDigits()} امتیاز" else "-${kotlin.math.abs(pts).toPersianDigits()} امتیاز",
                                color = accentColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = VazirmatnFontFamily,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Delete from reports button (hides from reports, keeps in student dossier)
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_report_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف از گزارش‌ها",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.title.ifBlank { record.category },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = VazirmatnFontFamily
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = record.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = VazirmatnFontFamily,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (record.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                    fontFamily = VazirmatnFontFamily
                )
            }

            // Attached Photo if exists
            val photoUrl = record.imageUrl ?: record.attachments.firstOrNull()
            if (!photoUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { onImageClick(photoUrl) }
                ) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "تصویر رفتار ثبت شده",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "مشاهده بزرگ‌تر 🔍",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = VazirmatnFontFamily,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Date & Time (without submitter / createdBy, normalized month based on solar date)
            val normalizedDate = DateTimeUtils.normalizeReportDate(record.date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "زمان انتشار و ثبت گزارش: $normalizedDate ساعت ${record.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = VazirmatnFontFamily
                )
            }
        }
    }
}

private fun matchesClassFilter(record: BehaviorRecord, filter: String): Boolean {
    if (filter == "همه") return true

    val sGrade = record.studentGrade.lowercase()
    val sClass = record.studentClass.lowercase()
    val combined = "$sGrade $sClass"

    return when (filter) {
        "هفتم یک" -> {
            combined.contains("هفتم یک") || combined.contains("هفتم ۱") || combined.contains("هفتم 1") ||
            ((sGrade.contains("هفتم") || sGrade.contains("7") || sGrade.contains("۷")) &&
             (sClass.contains("7/1") || sClass.contains("۷/۱") || sClass.contains("یک") || sClass.contains("۱") || sClass.contains("/1") || (sClass.contains("1") && !sClass.contains("10") && !sClass.contains("11") && !sClass.contains("12"))))
        }
        "هفتم دو" -> {
            combined.contains("هفتم دو") || combined.contains("هفتم ۲") || combined.contains("هفتم 2") ||
            ((sGrade.contains("هفتم") || sGrade.contains("7") || sGrade.contains("۷")) &&
             (sClass.contains("7/2") || sClass.contains("۷/۲") || sClass.contains("دو") || sClass.contains("۲") || sClass.contains("/2") || sClass.contains("2")))
        }
        "هشتم یک" -> {
            combined.contains("هشتم یک") || combined.contains("هشتم ۱") || combined.contains("هشتم 1") ||
            ((sGrade.contains("هشتم") || sGrade.contains("8") || sGrade.contains("۸")) &&
             (sClass.contains("8/1") || sClass.contains("۸/۱") || sClass.contains("یک") || sClass.contains("۱") || sClass.contains("/1") || sClass.contains("1")))
        }
        "هشتم دو" -> {
            combined.contains("هشتم دو") || combined.contains("هشتم ۲") || combined.contains("هشتم 2") ||
            ((sGrade.contains("هشتم") || sGrade.contains("8") || sGrade.contains("۸")) &&
             (sClass.contains("8/2") || sClass.contains("۸/۲") || sClass.contains("دو") || sClass.contains("۲") || sClass.contains("/2") || sClass.contains("2")))
        }
        "نهم یک" -> {
            combined.contains("نهم یک") || combined.contains("نهم ۱") || combined.contains("نهم 1") ||
            ((sGrade.contains("نهم") || sGrade.contains("9") || sGrade.contains("۹")) &&
             (sClass.contains("9/1") || sClass.contains("۹/۱") || sClass.contains("یک") || sClass.contains("۱") || sClass.contains("/1") || sClass.contains("1")))
        }
        "نهم دو" -> {
            combined.contains("نهم دو") || combined.contains("نهم ۲") || combined.contains("نهم 2") ||
            ((sGrade.contains("نهم") || sGrade.contains("9") || sGrade.contains("۹")) &&
             (sClass.contains("9/2") || sClass.contains("۹/۲") || sClass.contains("دو") || sClass.contains("۲") || sClass.contains("/2") || sClass.contains("2")))
        }
        else -> combined.contains(filter.lowercase())
    }
}

