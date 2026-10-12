package com.example.presentation.reports

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.RecordType
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.presentation.components.NezamYarHeader
import com.example.presentation.components.StatCard
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.IncidentRedContainer
import com.example.ui.theme.OnIncidentRedContainer
import com.example.ui.theme.OnPositiveGreenContainer
import com.example.ui.theme.OnWarningAmberContainer
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenContainer
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
    val stats by mainViewModel.stats.collectAsState()
    val records by mainViewModel.records.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()
    val unreadNotifs = notifications.count { !it.read }

    var selectedGrade by remember { mutableStateOf("همه پایه‌ها") }
    val grades = listOf("همه پایه‌ها", "پایه هفتم", "پایه هشتم", "پایه نهم")

    val filteredRecords = if (selectedGrade == "همه پایه‌ها") records else records.filter { it.studentGrade == selectedGrade }

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
                        text = "تحلیل آماری رفتارهای ثبت‌شده",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        val reportText = buildString {
                            appendLine("گزارش سامانه هوشمند نظمیار")
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

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
