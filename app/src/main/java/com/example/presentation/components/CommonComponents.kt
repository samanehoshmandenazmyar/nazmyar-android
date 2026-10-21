package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import com.example.domain.model.RankPermission
import com.example.data.demo.DemoData
import com.example.domain.model.UserRole
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.heightIn
import com.example.domain.model.matchesQuery
import com.example.ui.theme.VazirmatnFontFamily
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.demo.AllowedStudentsRegistry
import com.example.domain.model.BehaviorRecord
import com.example.domain.model.RecordType
import com.example.domain.model.SeverityLevel
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.IncidentRedContainer
import com.example.ui.theme.OnIncidentRedContainer
import com.example.ui.theme.OnPositiveGreenContainer
import com.example.ui.theme.OnWarningAmberContainer
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenContainer
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

private val RgbColors = listOf(
    Color(0xFFFF0055), // Red-Pink
    Color(0xFFFF7700), // Vibrant Orange
    Color(0xFFFFDD00), // Vibrant Gold
    Color(0xFF00FF66), // Neon Green
    Color(0xFF00F0FF), // Electric Cyan
    Color(0xFF8B5CF6), // Royal Violet
    Color(0xFFFF00AA), // Neon Magenta
    Color(0xFFFF0055)  // Repeat
)

private val RainbowBrush = Brush.linearGradient(
    colors = RgbColors
)

@Composable
fun RgbRankBadge(
    modifier: Modifier = Modifier,
    text: String = "سازنده",
    isSmall: Boolean = false,
    isLarge: Boolean = false
) {
    Surface(
        modifier = modifier.testTag("rgb_rank_badge"),
        shape = RoundedCornerShape(if (isLarge) 10.dp else 8.dp),
        color = Color(0xFF0F172A), // Dark slate / obsidian for maximum RGB contrast
        border = BorderStroke(if (isLarge) 2.dp else 1.5.dp, RainbowBrush),
        shadowElevation = if (isLarge) 4.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isLarge) 12.dp else if (isSmall) 8.dp else 10.dp,
                vertical = if (isLarge) 5.dp else if (isSmall) 2.dp else 3.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = if (isLarge) {
                    MaterialTheme.typography.labelMedium.copy(
                        brush = RainbowBrush,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                } else if (isSmall) {
                    MaterialTheme.typography.labelSmall.copy(
                        brush = RainbowBrush,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                } else {
                    MaterialTheme.typography.labelSmall.copy(
                        brush = RainbowBrush,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

@Composable
fun UserRankBadge(
    user: User,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false,
    isLarge: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (user.isCreator) {
            RgbRankBadge(
                text = "سازنده",
                isSmall = isSmall,
                isLarge = isLarge
            )
        } else if (user.rankTitle.isBlank() && user.extraRanks.isEmpty()) {
            Surface(
                modifier = Modifier.testTag("user_rank_badge_empty"),
                shape = RoundedCornerShape(if (isLarge) 10.dp else 6.dp),
                color = Color(0xFF64748B).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFF64748B).copy(alpha = 0.25f))
            ) {
                Text(
                    text = "بدون رنک",
                    style = if (isLarge) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(
                        horizontal = if (isLarge) 12.dp else if (isSmall) 6.dp else 8.dp,
                        vertical = if (isLarge) 5.dp else if (isSmall) 2.dp else 3.dp
                    ),
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            val badgeColor = try {
                val hex = if (user.rankColorHex.startsWith("#")) user.rankColorHex else "#${user.rankColorHex}"
                Color(android.graphics.Color.parseColor(hex))
            } catch (_: Exception) {
                Color(user.rankBadgeColorHex)
            }
            Surface(
                modifier = Modifier.testTag("user_rank_badge"),
                shape = RoundedCornerShape(if (isLarge) 10.dp else 6.dp),
                color = badgeColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
            ) {
                Text(
                    text = user.rankTitle,
                    style = if (isLarge) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    modifier = Modifier.padding(
                        horizontal = if (isLarge) 12.dp else if (isSmall) 6.dp else 8.dp,
                        vertical = if (isLarge) 5.dp else if (isSmall) 2.dp else 3.dp
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Render extra ranks if present
        user.extraRanks.forEachIndexed { index, extraRank ->
            val extraColorHex = user.extraRankColors.getOrNull(index) ?: "#8B5CF6"
            val extraBadgeColor = try {
                val hex = if (extraColorHex.startsWith("#")) extraColorHex else "#$extraColorHex"
                Color(android.graphics.Color.parseColor(hex))
            } catch (_: Exception) {
                Color(0xFF8B5CF6)
            }
            Surface(
                shape = RoundedCornerShape(if (isLarge) 10.dp else 6.dp),
                color = extraBadgeColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, extraBadgeColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = extraRank,
                    style = if (isLarge) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    color = extraBadgeColor,
                    modifier = Modifier.padding(
                        horizontal = if (isLarge) 10.dp else if (isSmall) 5.dp else 6.dp,
                        vertical = if (isLarge) 4.dp else if (isSmall) 2.dp else 2.5.dp
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StudentRankBadge(
    rankTitle: String,
    rankColorHex: String,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false,
    isLarge: Boolean = false
) {
    if (rankTitle == "سازنده") {
        RgbRankBadge(
            modifier = modifier,
            text = "سازنده",
            isSmall = isSmall,
            isLarge = isLarge
        )
        } else if (rankTitle.isBlank()) {
            Surface(
                modifier = modifier.testTag("student_rank_badge_empty"),
                shape = RoundedCornerShape(if (isLarge) 10.dp else 6.dp),
                color = Color(0xFF64748B).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFF64748B).copy(alpha = 0.25f))
            ) {
                Text(
                    text = "بدون رنک",
                    style = if (isLarge) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(
                        horizontal = if (isLarge) 12.dp else if (isSmall) 6.dp else 8.dp,
                        vertical = if (isLarge) 5.dp else if (isSmall) 2.dp else 3.dp
                    ),
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            val badgeColor = try {
                val hex = if (rankColorHex.startsWith("#")) rankColorHex else "#$rankColorHex"
                Color(android.graphics.Color.parseColor(hex))
            } catch (_: Exception) {
                Color(0xFF64748B)
            }
            Surface(
                modifier = modifier.testTag("student_rank_badge"),
                shape = RoundedCornerShape(if (isLarge) 10.dp else 6.dp),
                color = badgeColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = rankTitle,
                    style = if (isLarge) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    modifier = Modifier.padding(
                        horizontal = if (isLarge) 12.dp else if (isSmall) 6.dp else 8.dp,
                        vertical = if (isLarge) 5.dp else if (isSmall) 2.dp else 3.dp
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }
}

@Composable
fun StudentMultiRankBadge(
    student: Student,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false,
    isLarge: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudentRankBadge(
            rankTitle = student.rankTitle,
            rankColorHex = student.rankColorHex,
            isSmall = isSmall,
            isLarge = isLarge
        )
        student.extraRanks.forEachIndexed { index, extraRank ->
            val extraColorHex = student.extraRankColors.getOrNull(index) ?: "#8B5CF6"
            val extraBadgeColor = try {
                val hex = if (extraColorHex.startsWith("#")) extraColorHex else "#$extraColorHex"
                Color(android.graphics.Color.parseColor(hex))
            } catch (_: Exception) {
                Color(0xFF8B5CF6)
            }
            Surface(
                shape = RoundedCornerShape(if (isLarge) 10.dp else 6.dp),
                color = extraBadgeColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, extraBadgeColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = extraRank,
                    style = if (isLarge) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    color = extraBadgeColor,
                    modifier = Modifier.padding(
                        horizontal = if (isLarge) 10.dp else if (isSmall) 5.dp else 6.dp,
                        vertical = if (isLarge) 4.dp else if (isSmall) 2.dp else 2.5.dp
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NezamYarHeader(
    user: User?,
    unreadNotificationCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("nezamyar_header"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E3A8A),
                            Color(0xFF1E40AF)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (onMenuClick != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            IconButton(
                                onClick = onMenuClick,
                                modifier = Modifier.testTag("menu_hamburger_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "منوی سه خط (همبرگری)",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.app_icon),
                            contentDescription = "سامانه نظمیar",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "سامانه هوشمند نظمیار",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        user?.let { u ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = u.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                UserRankBadge(user = u)
                            }
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.testTag("notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge(
                                        containerColor = IncidentRed,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Text(
                                            text = unreadNotificationCount.toPersianDigits(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (unreadNotificationCount > 0) Icons.Default.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "اعلان‌ها",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("stat_card_$title"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value.toPersianDigits(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun BehaviorRecordCard(
    record: BehaviorRecord,
    modifier: Modifier = Modifier,
    showStudentName: Boolean = true
) {
    val (typeColor, containerColor, icon) = when (record.type) {
        RecordType.POSITIVE -> Triple(PositiveGreen, PositiveGreenContainer, Icons.Default.CheckCircle)
        RecordType.ACHIEVEMENT -> Triple(Color(0xFFD97706), WarningAmberContainer, Icons.Default.EmojiEvents)
        RecordType.REWARD -> Triple(PositiveGreen, PositiveGreenContainer, Icons.Default.Star)
        RecordType.WARNING -> Triple(WarningAmber, WarningAmberContainer, Icons.Default.Warning)
        RecordType.DISCIPLINARY -> Triple(IncidentRed, IncidentRedContainer, Icons.Default.Warning)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("record_card_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = containerColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = record.type.farsiTitle,
                            tint = typeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = record.type.farsiTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "${record.date.toPersianDigits()} - ${record.time.toPersianDigits()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (showStudentName) {
                Text(
                    text = "${record.studentName} (${record.studentClass})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (record.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = record.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    record.severity?.let {
                        SeverityBadge(severity = it)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    record.points?.let { pts ->
                        val sign = if (pts > 0) "+$pts" else "$pts"
                        val pColor = if (pts > 0) PositiveGreen else IncidentRed
                        Text(
                            text = "${sign.toPersianDigits()} امتیاز",
                            style = MaterialTheme.typography.labelSmall,
                            color = pColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = "ثبت: ${record.createdBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SeverityBadge(severity: SeverityLevel) {
    val (color, container) = when (severity) {
        SeverityLevel.LOW -> Pair(PositiveGreen, PositiveGreenContainer)
        SeverityLevel.MEDIUM -> Pair(WarningAmber, WarningAmberContainer)
        SeverityLevel.HIGH -> Pair(Color(0xFFEA580C), Color(0xFFFFEDD5))
        SeverityLevel.CRITICAL -> Pair(IncidentRed, IncidentRedContainer)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = container
    ) {
        Text(
            text = "شدت: ${severity.farsiTitle}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SpecialUserListItemCard(
    user: User,
    allUsers: List<User> = emptyList(),
    matchingStudent: Student? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCreator = user.isCreator ||
        user.id == "user_creator_hossein" ||
        user.studentCode == "454545" ||
        user.name.trim() == "سید حسین موسوی نژاد" ||
        user.phone == "09923072572"

    if (isCreator) {
        val creatorStudent = matchingStudent ?: Student(
            id = user.id.ifBlank { "user_creator_hossein" },
            firstName = "سید حسین",
            lastName = "موسوی نژاد",
            studentCode = "454545",
            nationalId = "454545",
            grade = "پایه هشتم",
            className = "کلاس 8/2",
            parentPhone = user.phone.ifBlank { "09923072572" },
            email = "hosseinvexter@gmail.com",
            rankTitle = "سازنده",
            rankColorHex = "#1E40AF",
            permissions = listOf(
                RankPermission.REGISTER_POSITIVE,
                RankPermission.REGISTER_DISCIPLINARY,
                RankPermission.VIEW_REPORTS,
                RankPermission.ACCESS_GAMES,
                RankPermission.VIEW_STUDENT_PROFILES,
                RankPermission.EDIT_CLASS_INFO,
                RankPermission.SPECIAL_BADGE
            ),
            isRegistered = true,
            schoolId = user.schoolId.ifBlank { DemoData.DEFAULT_SCHOOL_ID }
        )
        StudentListItemCard(
            student = creatorStudent,
            allUsers = allUsers,
            onClick = onClick,
            modifier = modifier
        )
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("special_user_card_${user.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(SchoolBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(2).ifBlank { "کاربر" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = SchoolBluePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Name only (no role tag like "مدیر مدرسه" or "مسئول پرورشی")
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // ONLY CONTACT PHONE NUMBER (no role text, no "نقش: ...")
                if (user.phone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = SchoolBluePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "تماس: ${user.phone.toPersianDigits()}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentListItemCard(
    student: Student,
    allUsers: List<User> = emptyList(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val normStudentName = AllowedStudentsRegistry.normalize(student.fullName)
    val sCode = student.studentCode.trim()
    val sId = student.id
    val isCreatorCard = (sId == "user_creator_hossein" || sCode == "454545" || normStudentName == AllowedStudentsRegistry.normalize("سید حسین موسوی نژاد"))

    val fatherUser = remember(allUsers, student) {
        allUsers.firstOrNull { u ->
            u.role == UserRole.PARENT &&
            (u.gender.contains("پدر") || u.gender.contains("آقا") || u.rankTitle.contains("پدر") || (!u.gender.contains("مادر") && !u.rankTitle.contains("مادر"))) &&
            (u.studentCode == sCode ||
             (isCreatorCard && (u.studentCode == "454545" || u.childStudentIds.contains("454545") || u.childStudentIds.contains("user_creator_hossein"))) ||
             u.childStudentIds.contains(sId) ||
             u.childStudentIds.contains(sCode) ||
             (normStudentName.isNotBlank() && AllowedStudentsRegistry.normalize(u.name) == normStudentName) ||
             (normStudentName.isNotBlank() && u.childStudentIds.any { AllowedStudentsRegistry.normalize(it) == normStudentName }))
        }
    }

    val motherUser = remember(allUsers, student) {
        allUsers.firstOrNull { u ->
            u.role == UserRole.PARENT &&
            (u.gender.contains("مادر") || u.gender.contains("خانم") || u.rankTitle.contains("مادر")) &&
            (u.studentCode == sCode ||
             (isCreatorCard && (u.studentCode == "454545" || u.childStudentIds.contains("454545") || u.childStudentIds.contains("user_creator_hossein"))) ||
             u.childStudentIds.contains(sId) ||
             u.childStudentIds.contains(sCode) ||
             (normStudentName.isNotBlank() && AllowedStudentsRegistry.normalize(u.name) == normStudentName) ||
             (normStudentName.isNotBlank() && u.childStudentIds.any { AllowedStudentsRegistry.normalize(it) == normStudentName }))
        }
    }

    // Main Outer Container Card
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("student_card_${student.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ================= 1. TOP SHAPE: FATHER =================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (fatherUser != null) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(
                    width = 0.8.dp,
                    color = if (fatherUser != null) Color(0xFF93C5FD) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (fatherUser != null) Color(0xFFBFDBFE) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (fatherUser != null) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = if (fatherUser != null) "پدر دانش‌آموز" else "پدر به برنامه ورود نکرده است",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = if (fatherUser != null) FontWeight.Bold else FontWeight.Normal,
                                color = if (fatherUser != null) Color(0xFF1E3A8A) else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (fatherUser != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(0.5.dp, Color(0xFF86EFAC))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "وارد شده",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "عدم ورود",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // ================= 2. MIDDLE SHAPE: STUDENT (LARGER & PROMINENT) =================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.2.dp, SchoolBluePrimary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Larger prominent avatar
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(SchoolBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (student.firstName.take(1) + student.lastName.take(1)).ifBlank { student.fullName.take(1) },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = SchoolBluePrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Student Full Name (Wraps cleanly without clipping even if very long)
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            lineHeight = 22.sp
                        )

                        // Multi Rank Badges (placed cleanly under the name)
                        StudentMultiRankBadge(
                            student = student,
                            isSmall = true
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Details row: Student Code & Grade / Class
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "کد: ${student.studentCode.toPersianDigits()}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${student.grade} - ${student.className}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        if (student.parentPhone.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "تماس: ${student.parentPhone.toPersianDigits()}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        // Registration status tag if not registered in the app
                        if (!student.isRegistered) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "در برنامه ثبت‌نام نکرده است",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "مشاهده پرونده",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ================= 3. BOTTOM SHAPE: MOTHER =================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (motherUser != null) Color(0xFFFDF2F8) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(
                    width = 0.8.dp,
                    color = if (motherUser != null) Color(0xFFF472B6) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (motherUser != null) Color(0xFFFCE7F3) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (motherUser != null) Color(0xFFBE185D) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = if (motherUser != null) "مادر دانش‌آموز" else "مادر به برنامه ورود نکرده است",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = if (motherUser != null) FontWeight.Bold else FontWeight.Normal,
                                color = if (motherUser != null) Color(0xFF831843) else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (motherUser != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(0.5.dp, Color(0xFF86EFAC))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "وارد شده",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "عدم ورود",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PtaElectionHomeBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasVoted: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pta_election_home_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = SchoolBluePrimary.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(SchoolBluePrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HowToVote,
                    contentDescription = null,
                    tint = SchoolBluePrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "انتخاب انجمن و اولیا و مربیان",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = "موقت",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (hasVoted) "رأی شما ثبت شده است • برای مشاهده درصدها و نتایج کلیک کنید"
                           else "رأی‌گیری آنلاین و مشاهده ۱۰ نامزد محترم (ثبت فوری رأی)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "ورود به بخش رأی‌گیری",
                tint = SchoolBluePrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Dialog for selecting a student with real-time search (by first name, last name, full name, or code)
 * and grade/class filtering.
 */
@Composable
fun SearchableStudentPickerDialog(
    students: List<Student>,
    selectedStudentId: String? = null,
    title: String = "انتخاب و جستجوی دانش‌آموز",
    onSelectStudent: (Student) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val grades = listOf("همه", "هفتم یک", "هفتم دو", "هشتم یک", "هشتم دو", "نهم یک", "نهم دو")
    var selectedGrade by remember { mutableStateOf("همه") }

    val filteredStudents = remember(students, searchQuery, selectedGrade) {
        students.filter { s ->
            val matchGrade = if (searchQuery.isNotBlank()) true else when (selectedGrade) {
                "همه" -> true
                "هفتم یک" -> s.className.contains("7/1") || s.className.contains("یک")
                "هفتم دو" -> s.className.contains("7/2") || s.className.contains("دو")
                "هشتم یک" -> s.className.contains("8/1") || s.className.contains("یک")
                "هشتم دو" -> s.className.contains("8/2") || s.className.contains("دو")
                "نهم یک" -> s.className.contains("9/1") || s.className.contains("یک")
                "نهم دو" -> s.className.contains("9/2") || s.className.contains("دو")
                else -> true
            }
            matchGrade && s.matchesQuery(searchQuery)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "جستجو بر اساس نام، نام خانوادگی، نام کامل یا کد دانش‌آموزی",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = {
                        Text(
                            text = "مثال: فاریابی یا ابوالفضل...",
                            fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = com.example.ui.theme.SchoolBluePrimary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(grades) { g ->
                        FilterChip(
                            selected = selectedGrade == g,
                            onClick = { selectedGrade = g },
                            label = { Text(g, fontSize = 11.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${filteredStudents.size.toPersianDigits()} دانش‌آموز یافت شد",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (filteredStudents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "دانش‌آموزی با این مشخصات یافت نشد.",
                            fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredStudents, key = { it.id }) { s ->
                            val isSelected = s.id == selectedStudentId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectStudent(s) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) com.example.ui.theme.SchoolBluePrimary.copy(alpha = 0.12f)
                                                     else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isSelected) BorderStroke(1.5.dp, com.example.ui.theme.SchoolBluePrimary)
                                         else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(com.example.ui.theme.SchoolBlueContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = s.fullName.take(1),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = com.example.ui.theme.SchoolBluePrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.fullName,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${s.grade} - ${s.className}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                                                    color = com.example.ui.theme.SchoolBluePrimary
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "کد: ${s.studentCode.toPersianDigits()}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }

                                    if (!s.isRegistered) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Text(
                                                text = "ثبت‌نام نکرده",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", fontFamily = com.example.ui.theme.VazirmatnFontFamily)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}


