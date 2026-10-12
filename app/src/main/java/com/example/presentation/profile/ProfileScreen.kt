package com.example.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.demo.DemoData
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.presentation.components.NezamYarHeader
import com.example.presentation.components.RgbColorMixerDialog
import com.example.presentation.components.UserRankBadge
import com.example.presentation.viewmodel.AuthViewModel
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogoutClick: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("profile_screen")
    ) {
        item {
            NezamYarHeader(
                user = currentUser,
                onMenuClick = onMenuClick
            )
        }

        // User Profile Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val avatarStr = currentUser?.profileImage?.ifBlank { null }
                    val isEmojiAvatar = avatarStr != null && avatarStr.length <= 4

                    val borderBrush = if (currentUser?.isCreator == true) {
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFFF0055),
                                Color(0xFFFF7700),
                                Color(0xFFFFDD00),
                                Color(0xFF00FF66),
                                Color(0xFF00F0FF),
                                Color(0xFF8B5CF6),
                                Color(0xFFFF0055)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                SchoolBluePrimary,
                                Color(0xFF3B82F6)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(borderBrush)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isEmojiAvatar) {
                                Text(
                                    text = avatarStr!!,
                                    fontSize = 32.sp
                                )
                            } else {
                                Text(
                                    text = currentUser?.name?.take(2) ?: "ک",
                                    color = SchoolBluePrimary,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentUser?.name ?: "کاربر سامانه",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (!currentUser?.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentUser?.bio.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    currentUser?.let { user ->
                        UserRankBadge(user = user, isLarge = true)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Edit Profile Button
                    Button(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ویرایش کامل مشخصات، رنک و رنگ RGB",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Details list
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
                        text = "مشخصات حساب کاربری (همگام با سرور)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileInfoRow(
                        icon = Icons.Default.School,
                        label = "مدرسه",
                        value = DemoData.SCHOOL_NAME
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoRow(
                        icon = Icons.Default.Badge,
                        label = "کد دانش‌آموزی / پرسنلی",
                        value = currentUser?.studentCode?.toPersianDigits() ?: "-"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoRow(
                        icon = Icons.Default.Fingerprint,
                        label = "کد ملی",
                        value = currentUser?.nationalId?.ifBlank { "-" }?.toPersianDigits() ?: "-"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoRow(
                        icon = Icons.Default.Phone,
                        label = "شماره همراه",
                        value = currentUser?.phone?.toPersianDigits() ?: "-"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoRow(
                        icon = Icons.Default.Email,
                        label = "پست الکترونیکی",
                        value = currentUser?.email ?: "-"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoRow(
                        icon = Icons.Default.Person,
                        label = "جنسیت",
                        value = currentUser?.gender ?: "نامشخص"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileInfoRow(
                        icon = Icons.Default.Info,
                        label = "نسخه برنامه",
                        value = com.example.BuildConfig.VERSION_NAME.toPersianDigits()
                    )
                }
            }
        }

        // Logout Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("logout_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "خروج",
                        tint = IncidentRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "خروج از حساب کاربری",
                        color = IncidentRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // EDIT PROFILE DIALOG
    if (showEditProfileDialog && currentUser != null) {
        val user = currentUser!!
        EditProfileDialog(
            user = user,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedUser ->
                authViewModel.updateUserProfile(updatedUser) {
                    showEditProfileDialog = false
                    showSuccessToast = true
                }
            }
        )
    }

    if (showSuccessToast) {
        AlertDialog(
            onDismissRequest = { showSuccessToast = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PositiveGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ویرایش موفقیت‌آمیز", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("مشخصات، رنک و رنگ RGB حساب شما با موفقیت به‌روزرسانی شد و در سرور سامانه ثبت گردید.")
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessToast = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                ) {
                    Text("متوجه شدم")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var nameInput by remember { mutableStateOf(user.name) }
    var phoneInput by remember { mutableStateOf(user.phone) }
    var emailInput by remember { mutableStateOf(user.email) }
    var nationalIdInput by remember { mutableStateOf(user.nationalId) }
    var codeInput by remember { mutableStateOf(user.studentCode) }
    var passwordInput by remember { mutableStateOf(user.password) }
    var genderInput by remember { mutableStateOf(user.gender) }
    var bioInput by remember { mutableStateOf(user.bio) }
    var selectedAvatar by remember { mutableStateOf(user.profileImage ?: "👑") }

    // Ranks and RGB
    var primaryRankInput by remember { mutableStateOf(user.rankTitle) }
    var primaryColorHex by remember { mutableStateOf(user.rankColorHex) }

    // Extra ranks and colors
    val extraRanks = remember { mutableStateListOf<String>().apply { addAll(user.extraRanks) } }
    val extraColors = remember { mutableStateListOf<String>().apply { addAll(user.extraRankColors) } }

    var newExtraRankInput by remember { mutableStateOf("") }

    // RGB Mixer Dialog State
    var showRgbMixerForPrimary by remember { mutableStateOf(false) }
    var showRgbMixerForNewExtra by remember { mutableStateOf(false) }
    var extraRankColorForNew by remember { mutableStateOf("#8B5CF6") }

    val presetAvatars = listOf(
        "👑" to "تاج سازنده",
        "⭐" to "ستاره درخشان",
        "🚀" to "موشک پیشگام",
        "🎓" to "دانش‌آموخته برتر",
        "🏆" to "جام قهرمان",
        "💎" to "الماس ویژه",
        "🦁" to "شیر پرقدرت",
        "⚡" to "صاعقه پرسرعت",
        "🎨" to "طراح خلاق",
        "💻" to "کدنویس و فناور",
        "🔬" to "پژوهشگر علمی",
        "🌟" to "نخبه طلایی"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = SchoolBluePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ویرایش مشخصات و رنک من",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // AVATAR CHOOSER
                Text(
                    text = "انتخاب آواتار / نماد پروفایل:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetAvatars.forEach { (avatar, desc) ->
                        val isSelected = selectedAvatar == avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) SchoolBluePrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) SchoolBluePrimary else Color.LightGray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatar = avatar },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = avatar, fontSize = 22.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // BASIC SPECIFICATIONS
                Text(
                    text = "مشخصات هویتی و کاربری:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("نام و نام خانوادگی") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("شماره تماس همراه") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_phone_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("پست الکترونیکی (ایمیل)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_email_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nationalIdInput,
                    onValueChange = { nationalIdInput = it },
                    label = { Text("کد ملی") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_national_id_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("کد پرسنلی / دانش‌آموزی") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_code_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("رمز عبور جدید") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_password_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bioInput,
                    onValueChange = { bioInput = it },
                    label = { Text("بیوگرافی / توضیحات") },
                    placeholder = { Text("توضیحی کوتاه درباره خود بنویسید...") },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // RANK AND RGB CUSTOMIZATION
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = SchoolBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مدیریت رنک و رنگ‌های RGB من",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Primary Rank Title
                        OutlinedTextField(
                            value = primaryRankInput,
                            onValueChange = { primaryRankInput = it },
                            label = { Text("عنوان رنک اصلی من") },
                            placeholder = { Text("مثال: سازنده، مدیر مدرسه، نخبه...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Primary RGB Color & Mixer Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val c = try {
                                    Color(android.graphics.Color.parseColor(if (primaryColorHex.startsWith("#")) primaryColorHex else "#$primaryColorHex"))
                                } catch (_: Exception) {
                                    SchoolBluePrimary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(1.5.dp, Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "رنگ رنک: $primaryColorHex",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { showRgbMixerForPrimary = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ColorLens,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ترکیب رنگ RGB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // MULTIPLE RANKS (چند رنک همزمان)
                        Text(
                            text = "رنک‌های تکمیلی و همزمان (${extraRanks.size.toPersianDigits()} رنک):",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (extraRanks.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                extraRanks.forEachIndexed { idx, extraRank ->
                                    val chipColorHex = extraColors.getOrNull(idx) ?: "#8B5CF6"
                                    val chipColor = try {
                                        Color(android.graphics.Color.parseColor(if (chipColorHex.startsWith("#")) chipColorHex else "#$chipColorHex"))
                                    } catch (_: Exception) {
                                        Color(0xFF8B5CF6)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = chipColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, chipColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = extraRank,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = chipColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "حذف این رنک",
                                                tint = chipColor,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        extraRanks.removeAt(idx)
                                                        if (idx < extraColors.size) {
                                                            extraColors.removeAt(idx)
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Add Extra Rank Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newExtraRankInput,
                                onValueChange = { newExtraRankInput = it },
                                placeholder = { Text("افزودن رنک دوم/سوم...") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { showRgbMixerForNewExtra = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(extraRankColorForNew)))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "انتخاب رنگ",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    if (newExtraRankInput.isNotBlank()) {
                                        extraRanks.add(newExtraRankInput.trim())
                                        extraColors.add(extraRankColorForNew)
                                        newExtraRankInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = nameInput.trim().ifBlank { user.name }
                    val finalPhone = phoneInput.trim()
                    val finalEmail = emailInput.trim()
                    val finalNatId = nationalIdInput.trim()
                    val finalCode = codeInput.trim().ifBlank { user.studentCode }
                    val finalPass = passwordInput.trim().ifBlank { user.password }
                    val finalBio = bioInput.trim()

                    val finalPrimaryRank = primaryRankInput.trim().ifBlank { user.rankTitle }
                    val finalColor = primaryColorHex.trim().ifBlank { user.rankColorHex }

                    val colorIntLong = try {
                        val hex = if (finalColor.startsWith("#")) finalColor else "#$finalColor"
                        android.graphics.Color.parseColor(hex).toLong() and 0xFFFFFFFFL
                    } catch (_: Exception) {
                        user.rankBadgeColorHex
                    }

                    val updatedUser = user.copy(
                        name = finalName,
                        phone = finalPhone,
                        email = finalEmail,
                        nationalId = finalNatId,
                        studentCode = finalCode,
                        password = finalPass,
                        bio = finalBio,
                        profileImage = selectedAvatar,
                        rankTitle = finalPrimaryRank,
                        rankColorHex = finalColor,
                        rankBadgeColorHex = colorIntLong,
                        extraRanks = extraRanks.toList(),
                        extraRankColors = extraColors.toList()
                    )

                    onSave(updatedUser)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("ذخیره و همگام‌سازی با سرور", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )

    // RGB Color Mixer Dialogs
    if (showRgbMixerForPrimary) {
        RgbColorMixerDialog(
            initialColorHex = primaryColorHex,
            onColorSelected = { newHex ->
                primaryColorHex = newHex
                showRgbMixerForPrimary = false
            },
            onDismiss = { showRgbMixerForPrimary = false }
        )
    }

    if (showRgbMixerForNewExtra) {
        RgbColorMixerDialog(
            initialColorHex = extraRankColorForNew,
            onColorSelected = { newHex ->
                extraRankColorForNew = newHex
                showRgbMixerForNewExtra = false
            },
            onDismiss = { showRgbMixerForNewExtra = false }
        )
    }
}

@Composable
fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
