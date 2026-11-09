package com.example.presentation.behavior

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import android.widget.Toast
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import com.example.core.behavior.PrayerLockManager
import com.example.domain.model.RankPermission
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.utils.DateTimeUtils
import com.example.domain.model.AcademicMonth
import com.example.domain.model.UserRole
import com.example.domain.model.toPersianDigits
import com.example.presentation.components.SearchableStudentPickerDialog
import com.example.presentation.components.VoiceDictationTextField
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.MainViewModel
import com.example.presentation.viewmodel.OperationState
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.IncidentRedContainer
import com.example.ui.theme.OnIncidentRedContainer
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenContainer
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import java.io.ByteArrayOutputStream
import kotlin.math.abs

private val PrayerTeal = Color(0xFF0D9488)
private val PrayerTealContainer = Color(0xFFCCFBF1)
private val OnPrayerTealContainer = Color(0xFF115E59)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterPrayerScreen(
    initialStudentId: String? = null,
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val allStudents by mainViewModel.allStudents.collectAsState()
    val students by mainViewModel.students.collectAsState()
    val effectiveStudents = if (allStudents.isNotEmpty()) allStudents else students
    val operationState by mainViewModel.operationState.collectAsState()

    var selectedStudentId by remember {
        mutableStateOf(initialStudentId ?: "")
    }

    // Prayer time selection
    val prayerTimes = listOf(
        "نماز ظهر و عصر",
        "نماز صبح",
        "نماز مغرب و عشاء",
        "نماز جمعه و جماعت"
    )
    var selectedPrayerTime by remember { mutableStateOf(prayerTimes[0]) }

    var behaviorText by remember { mutableStateOf("") }
    var positiveCount by remember { mutableIntStateOf(1) }
    var negativeCount by remember { mutableIntStateOf(0) }
    var notifyParent by remember { mutableStateOf(true) }
    var attachedImageBase64 by remember { mutableStateOf<String?>(null) }
    var showStudentPicker by remember { mutableStateOf(false) }

    var selectedMonth by remember { mutableStateOf(AcademicMonth.getCurrentMonth()) }
    var isMonthDropdownExpanded by remember { mutableStateOf(false) }

    val currentDate = remember { DateTimeUtils.getCurrentPersianDate() }
    val currentTime = remember { DateTimeUtils.getCurrentTime() }

    val currentStudent = effectiveStudents.firstOrNull { it.id == selectedStudentId }

    // Net points calculation
    val netPoints = positiveCount - negativeCount

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        PrayerLockManager.init(context)
    }
    val prayerLocks by PrayerLockManager.prayerLocks.collectAsState()

    val userPerms = currentUser?.permissions ?: emptyList()
    val isCreator = currentUser?.isCreator == true
    val canLockPrayer = isCreator || currentUser?.role == UserRole.MANAGER || currentUser?.role == UserRole.VICE_PRINCIPAL || userPerms.contains(RankPermission.REGISTER_PRAYER)

    val isCurrentPrayerLocked = remember(prayerLocks, currentDate, selectedPrayerTime) {
        PrayerLockManager.isLocked(currentDate, selectedPrayerTime)
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val maxDimension = 600
                    val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
                    val scaledBitmap = if (scale < 1.0f) {
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt(),
                            true
                        )
                    } else bitmap
                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val bytes = outputStream.toByteArray()
                    attachedImageBase64 = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } catch (_: Exception) {}
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            try {
                val maxDimension = 600
                val scale = maxDimension.toFloat() / maxOf(it.width, it.height).coerceAtLeast(1)
                val scaledBitmap = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(
                        it,
                        (it.width * scale).toInt(),
                        (it.height * scale).toInt(),
                        true
                    )
                } else it
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val bytes = outputStream.toByteArray()
                attachedImageBase64 = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (_: Exception) {}
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("register_prayer_screen")
    ) {
        // App Bar / Header
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("prayer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrayerTealContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = null,
                            tint = PrayerTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "ثبت نماز خواندن",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "ارزیابی حضور و ادای فریضه نماز دانش‌آموزان",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // 1. Student Selection Section
                    Text(
                        text = "۱. انتخاب دانش‌آموز *",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentStudent == null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStudentPicker = true }
                                .testTag("select_prayer_student_btn"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PrayerTeal.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = PrayerTeal
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "انتخاب دانش‌آموز از لیست...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = PrayerTeal
                                    )
                                    Text(
                                        text = "جستجو بر اساس نام، کلاس یا کد ۶ رقمی",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = PrayerTealContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, PrayerTeal.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PrayerTeal),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentStudent.fullName.take(2),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentStudent.fullName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "${currentStudent.grade} • ${currentStudent.className} | کد: ${currentStudent.studentCode.toPersianDigits()}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { showStudentPicker = true },
                                    modifier = Modifier.testTag("change_prayer_student_btn")
                                ) {
                                    Text(
                                        text = "تغییر",
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = PrayerTeal
                                    )
                                }
                            }
                        }
                    }

                    if (showStudentPicker) {
                        SearchableStudentPickerDialog(
                            students = effectiveStudents,
                            selectedStudentId = selectedStudentId,
                            title = "انتخاب دانش‌آموز جهت ثبت حضور در نماز",
                            onSelectStudent = { s ->
                                selectedStudentId = s.id
                                showStudentPicker = false
                            },
                            onDismiss = { showStudentPicker = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. Prayer Time Selection (زمان نماز)
                    Text(
                        text = "۲. زمان نماز (وعده انتخابی) *",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        prayerTimes.forEach { timeTitle ->
                            val isSelected = selectedPrayerTime == timeTitle
                            val isItemLocked = PrayerLockManager.isLocked(currentDate, timeTitle)
                            var menuExpanded by remember { mutableStateOf(false) }

                            val containerColor = when {
                                isItemLocked && isSelected -> IncidentRedContainer.copy(alpha = 0.35f)
                                isItemLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                isSelected -> PrayerTealContainer.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val borderColor = when {
                                isItemLocked && isSelected -> IncidentRed
                                isItemLocked -> IncidentRed.copy(alpha = 0.45f)
                                isSelected -> PrayerTeal
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedPrayerTime = timeTitle
                                        if (isItemLocked) {
                                            Toast.makeText(
                                                context,
                                                "وعده «$timeTitle» برای امروز قفل است و امکان ثبت ندارد.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .testTag("prayer_time_item_${timeTitle}"),
                                shape = RoundedCornerShape(12.dp),
                                color = containerColor,
                                border = BorderStroke(if (isSelected || isItemLocked) 1.5.dp else 1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Selection / Lock icon + Title + Badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isItemLocked -> IncidentRed.copy(alpha = 0.15f)
                                                        isSelected -> PrayerTeal
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isItemLocked) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "قفل شده",
                                                    tint = IncidentRed,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            } else if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Text(
                                            text = timeTitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = when {
                                                isItemLocked -> IncidentRed
                                                isSelected -> OnPrayerTealContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        if (isItemLocked) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = IncidentRedContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = IncidentRed,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "قفل شده",
                                                        color = IncidentRed,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFamily = VazirmatnFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Three Dots Menu (سه نقطه برای هر زمان نماز)
                                    Box {
                                        IconButton(
                                            onClick = { menuExpanded = true },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("prayer_menu_${timeTitle}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "تنظیمات قفل وعده",
                                                tint = if (isItemLocked) IncidentRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false }
                                        ) {
                                            if (canLockPrayer) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = if (isItemLocked) "باز کردن قفل این وعده (امروز)" else "قفل کردن این وعده (امروز)",
                                                            fontFamily = VazirmatnFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isItemLocked) PositiveGreen else IncidentRed
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = if (isItemLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                                            contentDescription = null,
                                                            tint = if (isItemLocked) PositiveGreen else IncidentRed
                                                        )
                                                    },
                                                    onClick = {
                                                        menuExpanded = false
                                                        val newLockState = !isItemLocked
                                                        PrayerLockManager.setPrayerLock(
                                                            date = currentDate,
                                                            prayerTime = timeTitle,
                                                            isLocked = newLockState,
                                                            lockedBy = currentUser?.name ?: "مدیریت"
                                                        )
                                                        val msg = if (newLockState) {
                                                            "وعده «$timeTitle» برای امروز قفل شد و در لحظه برای همه کاربران مسدود گردید 🔒"
                                                        } else {
                                                            "قفل وعده «$timeTitle» برای امروز با موفقیت باز شد 🔓"
                                                        }
                                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            } else {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = "فقط مدیریت مجاز به قفل یا باز کردن وعده است",
                                                            fontFamily = VazirmatnFontFamily,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    },
                                                    onClick = { menuExpanded = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isCurrentPrayerLocked) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = IncidentRedContainer,
                            border = BorderStroke(1.dp, IncidentRed.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prayer_locked_warning_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "قفل",
                                    tint = IncidentRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "این وعده نماز برای تاریخ امروز قفل شده است 🔒",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = OnIncidentRedContainer
                                    )
                                    Text(
                                        text = "امکان ثبت ارزیابی برای «$selectedPrayerTime» در تاریخ $currentDate وجود ندارد.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily
                                        ),
                                        color = OnIncidentRedContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3. Month Selection (ماه پرونده)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedMonth.persianName,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("ماه ثبت در پرونده انضباطی", fontFamily = VazirmatnFontFamily)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isMonthDropdownExpanded = !isMonthDropdownExpanded }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "انتخاب ماه"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isMonthDropdownExpanded = true }
                                .testTag("prayer_month_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        DropdownMenu(
                            expanded = isMonthDropdownExpanded,
                            onDismissRequest = { isMonthDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            AcademicMonth.values().forEach { month ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = month.persianName,
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (month == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                            color = if (month == selectedMonth) PrayerTeal else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedMonth = month
                                        isMonthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Dual Positive & Negative Number Inputs (امتیاز مثبت و تذکر منفی)
                    Text(
                        text = "۳. ارزیابی امتیازات (مثبت و منفی)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "می‌توانید هم‌زمان امتیاز مثبت (تشویق) و منفی (تذکر) ثبت کنید؛ موازنه آن‌ها به صورت خودکار اعمال می‌گردد.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = VazirmatnFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Positive & Negative Input Boxes Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Positive Counter Card (سبز)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("prayer_pos_card"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PositiveGreenContainer.copy(alpha = 0.4f)),
                            border = BorderStroke(1.5.dp, PositiveGreen)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "تعداد مثبت (+)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = PositiveGreen
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = { if (positiveCount > 0) positiveCount-- },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(PositiveGreen.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "کاهش مثبت",
                                            tint = PositiveGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        text = positiveCount.toPersianDigits(),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = PositiveGreen
                                        ),
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )

                                    IconButton(
                                        onClick = { positiveCount++ },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(PositiveGreen.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "افزایش مثبت",
                                            tint = PositiveGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Presets
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(0, 1, 2, 3).forEach { v ->
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { positiveCount = v },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (positiveCount == v) PositiveGreen else Color.White.copy(alpha = 0.8f)
                                        ) {
                                            Text(
                                                text = "+${v.toPersianDigits()}",
                                                textAlign = TextAlign.Center,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (positiveCount == v) Color.White else PositiveGreen,
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Negative Counter Card (قرمز)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("prayer_neg_card"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = IncidentRedContainer.copy(alpha = 0.4f)),
                            border = BorderStroke(1.5.dp, IncidentRed)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "تعداد منفی (-)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = IncidentRed
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = { if (negativeCount > 0) negativeCount-- },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(IncidentRed.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "کاهش منفی",
                                            tint = IncidentRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        text = negativeCount.toPersianDigits(),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = IncidentRed
                                        ),
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )

                                    IconButton(
                                        onClick = { negativeCount++ },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(IncidentRed.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "افزایش منفی",
                                            tint = IncidentRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Presets
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(0, 1, 2, 3).forEach { v ->
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { negativeCount = v },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (negativeCount == v) IncidentRed else Color.White.copy(alpha = 0.8f)
                                        ) {
                                            Text(
                                                text = "-${v.toPersianDigits()}",
                                                textAlign = TextAlign.Center,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (negativeCount == v) Color.White else IncidentRed,
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. Net Calculation Result Card (محاسبه موازنه امتیاز)
                    val (netBgColor, netBorderColor, netTextColor, netIcon, netTitle, netDescription) = when {
                        netPoints > 0 -> {
                            val count = netPoints
                            Tuple6(
                                PositiveGreenContainer.copy(alpha = 0.6f),
                                PositiveGreen,
                                PositiveGreen,
                                Icons.Default.CheckCircle,
                                "نتیجه نهایی: دریافت +${count.toPersianDigits()} امتیاز مثبت",
                                "به دلیل برتری موارد مثبت (${positiveCount.toPersianDigits()}) بر موارد منفی (${negativeCount.toPersianDigits()})، دانش‌آموز ${count.toPersianDigits()} امتیاز تشویقی مثبت دریافت می‌کند."
                            )
                        }
                        netPoints < 0 -> {
                            val count = abs(netPoints)
                            Tuple6(
                                IncidentRedContainer.copy(alpha = 0.6f),
                                IncidentRed,
                                IncidentRed,
                                Icons.Default.Warning,
                                "نتیجه نهایی: ثبت ${count.toPersianDigits()} تذکر / کسر امتیاز",
                                "به دلیل برتری موارد منفی (${negativeCount.toPersianDigits()}) بر تشویق‌ها (${positiveCount.toPersianDigits()})، برای دانش‌آموز ${count.toPersianDigits()} تذکر انضباطی اعمال خواهد شد."
                            )
                        }
                        else -> {
                            Tuple6(
                                Color(0xFFFEF3C7),
                                Color(0xFFD97706),
                                Color(0xFFB45309),
                                Icons.Default.Balance,
                                "نتیجه نهایی: وضعیت خنثی (۰ امتیاز)",
                                "تعداد امتیازات مثبت (${positiveCount.toPersianDigits()}) و منفی (${negativeCount.toPersianDigits()}) برابر است؛ امتیازات یکدیگر را خنثی کرده و صرفاً حضور در نماز ثبت می‌شود."
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayer_net_result_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = netBgColor,
                        border = BorderStroke(1.dp, netBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = netIcon,
                                contentDescription = null,
                                tint = netTextColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = netTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = netTextColor
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = netDescription,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 6. Behavior Description Field with Voice Dictation
                    Text(
                        text = "۴. شرح و توضیحات ارزیابی",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    VoiceDictationTextField(
                        value = behaviorText,
                        onValueChange = { behaviorText = it },
                        label = "شرح رفتار دانش‌آموز در نماز",
                        placeholder = "مثال: حضور منظم و به موقع در صف نماز جماعت، ادای با حضور قلب و رعایت سکوت...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayer_description_input"),
                        minLines = 3,
                        testTagPrefix = "prayer_voice"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 7. Photo Attachment UI (Gallery & Direct Camera)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = PrayerTeal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "بارگذاری عکس",
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = PrayerTeal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "عکاسی با دوربین",
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (attachedImageBase64 != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            AsyncImage(
                                model = attachedImageBase64,
                                contentDescription = "تصویر پیوست شده نماز",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { attachedImageBase64 = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "حذف تصویر",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 8. Dynamic Date, Time & Submitter Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تاریخ: $currentDate (${selectedMonth.persianName}) - $currentTime",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ثبت‌کننده: ${currentUser?.name ?: "مسئول اقامه نماز"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = PrayerTeal
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 9. Parent Notification Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "اطلاع‌رسانی به اولیاء",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "ارسال گزارش و ثبت در اعلان کارنامه والدین",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifyParent,
                            onCheckedChange = { notifyParent = it }
                        )
                    }

                    // Error Banner if any
                    if (operationState is OperationState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IncidentRedContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = (operationState as OperationState.Error).message,
                                color = OnIncidentRedContainer,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 10. Submit and Cancel Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isCurrentPrayerLocked) {
                                    Toast.makeText(
                                        context,
                                        "وعده «$selectedPrayerTime» برای تاریخ امروز قفل شده است و امکان ثبت آن وجود ندارد.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@Button
                                }
                                if (selectedStudentId.isBlank() || currentStudent == null) {
                                    mainViewModel.operationState.value = OperationState.Error("لطفاً ابتدا یک دانش‌آموز را جهت ثبت نماز انتخاب کنید.")
                                    return@Button
                                }
                                val text = behaviorText.trim()
                                val defaultDesc = if (text.isBlank()) "حضور و اقامه $selectedPrayerTime" else text

                                mainViewModel.registerPrayerRecord(
                                    studentId = selectedStudentId,
                                    prayerTime = selectedPrayerTime,
                                    positiveCount = positiveCount,
                                    negativeCount = negativeCount,
                                    description = defaultDesc,
                                    date = "$currentDate (${selectedMonth.persianName})",
                                    time = currentTime,
                                    createdBy = currentUser?.name ?: "مسئول اقامه نماز",
                                    createdByRole = currentUser?.role ?: UserRole.VICE_PRINCIPAL,
                                    parentNotified = notifyParent,
                                    imageUrl = attachedImageBase64
                                )
                                onBackClick()
                            },
                            enabled = !isCurrentPrayerLocked,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("submit_prayer_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrayerTeal,
                                disabledContainerColor = IncidentRed.copy(alpha = 0.15f),
                                disabledContentColor = IncidentRed
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isCurrentPrayerLocked) Icons.Default.Lock else Icons.Default.VolunteerActivism,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCurrentPrayerLocked) "این وعده امروز قفل است 🔒" else "ثبت ارزیابی نماز",
                                fontWeight = FontWeight.Bold,
                                fontFamily = VazirmatnFontFamily
                            )
                        }

                        OutlinedButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .weight(0.6f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("انصراف", fontFamily = VazirmatnFontFamily)
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

private data class Tuple6<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)
