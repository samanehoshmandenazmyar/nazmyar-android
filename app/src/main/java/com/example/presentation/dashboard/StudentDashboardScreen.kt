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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.session.SessionManager
import com.example.domain.model.RankPermission
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.presentation.components.BehaviorRecordCard
import com.example.presentation.components.NezamYarHeader
import com.example.presentation.components.StudentRankBadge
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenContainer
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary

@Composable
fun StudentDashboardScreen(
    user: User?,
    mainViewModel: MainViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToChess: () -> Unit,
    onNavigateToPositive: () -> Unit = {},
    onNavigateToDisciplinary: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToStudents: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val notifications by mainViewModel.notifications.collectAsState()
    val allRecords by mainViewModel.records.collectAsState()
    val allStudents by mainViewModel.students.collectAsState()
    val unreadNotifs = notifications.count { !it.read }

    val currentStudent = allStudents.firstOrNull { it.id == user?.id || it.studentCode == user?.studentCode }
    val myRecords = allRecords.filter { it.studentId == currentStudent?.id || (user != null && it.studentId == user.id) }
    val currentScore = currentStudent?.score ?: 100

    var showCelebrationDialog by remember(user?.rankTitle, user?.id) {
        mutableStateOf(
            user != null &&
            user.rankTitle != "دانش‌آموز" &&
            !SessionManager.hasSeenRankCelebration(user.id, user.rankTitle)
        )
    }

    if (showCelebrationDialog && user != null) {
        AlertDialog(
            onDismissRequest = {
                SessionManager.markRankCelebrationSeen(user.id, user.rankTitle)
                showCelebrationDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🎉 تبریک صمیمانه ارتقای رنک!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "دانش‌آموز گرامی ${user.name}،",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "شما با شایستگی موفق به کسب رنک «${user.rankTitle}» در سامانه نظمیار شدید! اختیارات و دسترسی‌های زیر برای حساب کاربری شما فعال گردید:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F)
                            )
                        }
                    }

                    Text(
                        text = "اختیارات و امکانات فعال‌شده برای شما:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    if (user.permissions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PositiveGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "نشان افتخاری سامانه", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(text = "این رنک به عنوان نشان شایستگی و الگو بودن در سامانه برای شما ثبت شده است.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                }
                            }
                        }
                    } else {
                        user.permissions.forEach { permKey ->
                            val (title, desc) = when (permKey) {
                                RankPermission.REGISTER_POSITIVE -> Pair("ثبت تشویق و امتیاز مثبت", "امکان ثبت رفتار مثبت برای هم‌کلاسی‌ها (همیار معلم)")
                                RankPermission.REGISTER_DISCIPLINARY -> Pair("ثبت گزارش انضباطی", "امکان ثبت گزارش و تذکر انضباطی کلاسی")
                                RankPermission.VIEW_REPORTS -> Pair("مشاهده آمار و گزارش‌ها", "دسترسی به نمودارها و سوابق آماری سامانه")
                                RankPermission.ACCESS_GAMES -> Pair("دسترسی به بخش بازی‌ها", "ورود و رقابت در بازی شطرنج")
                                RankPermission.VIEW_STUDENT_PROFILES -> Pair("پرونده دانش‌آموزان", "مشاهده سوابق و پرونده رفتاری دانش‌آموزان")
                                RankPermission.EDIT_CLASS_INFO -> Pair("همیار کلاسی", "مدیریت مشخصات و اطلاعیه‌های کلاس")
                                RankPermission.SPECIAL_BADGE -> Pair("نشان ویژه", "نمایش کادر و نشان برجسته طلایی در حساب کاربری")
                                else -> Pair(permKey, "دسترسی ویژه رنک")
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PositiveGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text(text = desc, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        SessionManager.markRankCelebrationSeen(user.id, user.rankTitle)
                        showCelebrationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("متشکرم، شروع فعالیت ✨", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("student_dashboard")
    ) {
        item {
            NezamYarHeader(
                user = user,
                unreadNotificationCount = unreadNotifs,
                onNotificationClick = onNavigateToNotifications,
                onMenuClick = onMenuClick
            )
        }

        // Promoted Rank Celebration Banner
        if (user != null && user.rankTitle != "دانش‌آموز") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🏆", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تبریک! رنک شما «${user.rankTitle}» است",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "اختیارات و دسترسی‌های این رنک فعال شده است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309)
                            )
                        }
                        IconButton(onClick = { showCelebrationDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "مشاهده تبریک",
                                tint = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }
        }

        // Student Welcome & 6-digit Code Banner
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(SchoolBluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "خوش آمدید، ${user?.name ?: "دانش‌آموز عزیز"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = SchoolBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "کد دانش‌آموزی شما: ${user?.studentCode?.toPersianDigits() ?: "۱۲۳۴۵۶"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SchoolBluePrimary
                            )
                        }
                    }

                    StudentRankBadge(
                        rankTitle = user?.rankTitle ?: "دانش‌آموز",
                        rankColorHex = user?.rankColorHex ?: "#64748B",
                        isLarge = true
                    )
                }
            }
        }

        // Rank Permissions & Capabilities Granted to this student
        if (user?.permissions?.isNotEmpty() == true) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SchoolBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "اختیارات اعطا شده به رنک شما (${user.rankTitle}):",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (user.permissions.contains(RankPermission.REGISTER_POSITIVE)) {
                            Button(
                                onClick = onNavigateToPositive,
                                colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text("ثبت تشویق و امتیاز مثبت برای هم‌کلاسی‌ها (همیار معلم)", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (user.permissions.contains(RankPermission.REGISTER_DISCIPLINARY)) {
                            Button(
                                onClick = onNavigateToDisciplinary,
                                colors = ButtonDefaults.buttonColors(containerColor = IncidentRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text("ثبت گزارش انضباطی کلاسی", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (user.permissions.contains(RankPermission.VIEW_REPORTS)) {
                            Button(
                                onClick = onNavigateToReports,
                                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text("مشاهده آمار و گزارش‌های انضباطی", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (user.permissions.contains(RankPermission.VIEW_STUDENT_PROFILES)) {
                            Button(
                                onClick = onNavigateToStudents,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text("مشاهده پرونده و لیست دانش‌آموزان", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (user.permissions.contains(RankPermission.ACCESS_GAMES)) {
                            Button(
                                onClick = onNavigateToChess,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text("ورود به بخش بازی‌ها (شطرنج)", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (user.permissions.contains(RankPermission.EDIT_CLASS_INFO)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SchoolBluePrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("همیار کلاسی (مدیریت مشخصات و اطلاعیه‌های کلاس)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SchoolBluePrimary)
                                }
                            }
                        }

                        if (user.permissions.contains(RankPermission.SPECIAL_BADGE)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("دارای نشان افتخار ویژه سامانه", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Discipline Score & Honor Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PositiveGreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = PositiveGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "نمره و رتبه انضباطی",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "نیم‌سال اول تحصیلی",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "${currentScore.toPersianDigits()} / ۱۰۰",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (currentScore >= 90) PositiveGreen else if (currentScore >= 80) Color(0xFFD97706) else Color(0xFFDC2626)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (currentScore >= 90) Color(0xFFF0FDF4) else if (currentScore >= 80) Color(0xFFFFFBEB) else Color(0xFFFEF2F2)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (currentScore >= 90) Icons.Default.Star else Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (currentScore >= 90) PositiveGreen else if (currentScore >= 80) Color(0xFFD97706) else Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentScore >= 90) "وضعیت انضباطی شما در عالی‌ترین سطح است. برای کسب امتیازهای ویژه به رفتارهای مثبت ادامه دهید."
                                else if (currentScore >= 80) "وضعیت انضباطی شما خوب است. با فعالیت‌های مثبت امتیاز خود را ارتقا دهید."
                                else "توجه: مواردی برای شما ثبت شده است. لطفاً جهت بهبود نمره انضباط دقت بیشتری بفرمایید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentScore >= 90) Color(0xFF166534) else if (currentScore >= 80) Color(0xFF92400E) else Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }

        // Section: Real Registered Records (Positive and Disciplinary) for Student & Parents
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = SchoolBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "موارد و تشویق‌های ثبت‌شده برای شما (${myRecords.size.toPersianDigits()} مورد)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (myRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PositiveGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تاکنون هیچ موردی برای شما ثبت نشده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(myRecords, key = { it.id }) { record ->
                BehaviorRecordCard(
                    record = record,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Chess Game Shortcut for Student Entertainment
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SchoolBluePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "♞",
                                fontSize = 28.sp,
                                color = SchoolBluePrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "بازی شطرنج فکری",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "بازی دونفره و مسابقه هوش در زنگ تفریح",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onNavigateToChess,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                    ) {
                        Text(text = "شروع بازی")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Rules and Positive Behaviors Guide for Students
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SchoolBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "راهنمای رفتارهای شایسته و تشویق‌ها",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val tips = listOf(
                        "حضور به موقع در کلاس و مراسم صبحگاهی (+ امتیاز انضباطی)",
                        "مشارکت فعال در فعالیت‌های علمی، پرورشی و ورزشی",
                        "احترام متقابل به هم‌کلاسی‌ها، دبیران و کادر مدرسه",
                        "رعایت نظم، بهداشت و حفاظت از اموال مدرسه"
                    )

                    tips.forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SchoolBluePrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
