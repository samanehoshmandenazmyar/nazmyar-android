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
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("special_user_card_${user.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (user.isCreator) {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
                            )
                        } else {
                            androidx.compose.ui.graphics.SolidColor(SchoolBlueContainer)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(2).ifBlank { "ویژه" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        UserRankBadge(
                            user = user,
                            isSmall = true
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (user.isCreator) Color(0xFFEFF6FF) else if (user.role == UserRole.MANAGER) Color(0xFFFEF3C7) else if (user.role == UserRole.COUNSELOR) Color(0xFFF3E8FF) else Color(0xFFEDE9FE)
                    ) {
                        Text(
                            text = if (user.isCreator) "سازنده سامانه 👑" else if (user.role == UserRole.MANAGER) "مدیر مدرسه 🏫" else if (user.role == UserRole.COUNSELOR) "معاون پرورشی 🌟" else "فرد ویژه ⭐",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.isCreator) Color(0xFF1E40AF) else if (user.role == UserRole.MANAGER) Color(0xFFB45309) else if (user.role == UserRole.COUNSELOR) Color(0xFF7C3AED) else Color(0xFF6D28D9),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "نقش: ${user.role.farsiTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (user.studentCode.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "کد: ${user.studentCode.toPersianDigits()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (user.phone.isNotBlank() || user.email.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (user.phone.isNotBlank()) {
                            Text(
                                text = "تماس: ${user.phone.toPersianDigits()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (user.email.isNotBlank()) {
                            if (user.phone.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentListItemCard(
    student: Student,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("student_card_${student.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    text = (student.firstName.take(1) + student.lastName.take(1)).ifBlank { student.fullName.take(1) },
                    style = MaterialTheme.typography.titleMedium,
                    color = SchoolBluePrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        StudentMultiRankBadge(
                            student = student,
                            isSmall = true
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (student.score >= 90) PositiveGreenContainer else if (student.score >= 80) WarningAmberContainer else IncidentRedContainer
                    ) {
                        Text(
                            text = "نمره: ${student.score.toPersianDigits()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (student.score >= 90) OnPositiveGreenContainer else if (student.score >= 80) OnWarningAmberContainer else OnIncidentRedContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "کد: ${student.studentCode.toPersianDigits()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${student.grade} - ${student.className}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PositiveGreenContainer
                    ) {
                        Text(
                            text = "⭐ ${student.positiveCount.toPersianDigits()} مثبت",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnPositiveGreenContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (student.warningCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WarningAmberContainer
                        ) {
                            Text(
                                text = "⚠️ ${student.warningCount.toPersianDigits()} تذکر",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnWarningAmberContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (student.disciplinaryCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IncidentRedContainer
                        ) {
                            Text(
                                text = "🚨 ${student.disciplinaryCount.toPersianDigits()} تخلف",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnIncidentRedContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
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
}
