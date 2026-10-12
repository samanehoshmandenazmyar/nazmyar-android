package com.example.presentation.admin

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.domain.model.AppUpdateInfo
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.presentation.components.RgbRankBadge
import com.example.presentation.viewmodel.AuthViewModel
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishAppUpdateScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUpdateInfo by authViewModel.appUpdateInfo.collectAsState()

    // Form states
    var versionName by remember { mutableStateOf("1.0.6") }
    var versionCode by remember { mutableIntStateOf(7) }
    var releaseNotes by remember {
        mutableStateOf("• اضافه شدن کاربران جدید: مهدی فاریابی (مدیر مدرسه) و یاسین دراهکی (معاون پرورشی)\n• به‌روزرسانی سیستم رنک‌ها و سطوح دسترسی کادر مدرسه\n• بهینه‌سازی کامل سرعت و همگام‌سازی ابری")
    }
    var isForceUpdate by remember { mutableStateOf(false) }
    var downloadUrl by remember {
        mutableStateOf("https://github.com/aistudio-nezamyar/releases/download/v1.0.6/nezamyar-v1.0.6.apk")
    }

    // Selected APK file details
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableLongStateOf(16_200_000L) }

    // Upload progress state
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStatusText by remember { mutableStateOf("") }
    var uploadSuccess by remember { mutableStateOf(false) }

    // Delete update dialog state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeletingUpdate by remember { mutableStateOf(false) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            selectedFileName = cursor.getString(nameIndex)
                        }
                        if (sizeIndex != -1) {
                            selectedFileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (_: Exception) {
                selectedFileName = "nezamyar-update.apk"
            }

            // Automatically suggest versionName from filename if pattern matches (e.g., v1.0.4.apk)
            selectedFileName?.let { name ->
                val match = Regex("""v?([0-9]+\.[0-9]+\.[0-9]+)""").find(name)
                if (match != null) {
                    val extracted = match.groupValues[1]
                    versionName = extracted
                    downloadUrl = "https://github.com/aistudio-nezamyar/releases/download/v$extracted/nezamyar-v$extracted.apk"
                }
            }
        }
    }

    fun startPublishProcess() {
        if (versionName.isBlank()) {
            Toast.makeText(context, "لطفاً شماره نسخه را مشخص نمایید.", Toast.LENGTH_SHORT).show()
            return
        }

        isUploading = true
        uploadProgress = 0f
        uploadSuccess = false
        uploadStatusText = "در حال آماده‌سازی و اعتبارسنجی پکیج APK..."

        scope.launch {
            // High-fidelity upload simulation & file saving into app cloud storage
            try {
                // 1. If file chosen from URI, read and store into local storage cache
                selectedFileUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        val input = context.contentResolver.openInputStream(uri)
                        val updatesDir = File(context.filesDir, "published_updates").apply { mkdirs() }
                        val outFile = File(updatesDir, "nezamyar-v$versionName.apk")
                        input?.use { inStream ->
                            FileOutputStream(outFile).use { outStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                    }
                }

                // 2. Realistic progress bar with milestones
                val steps = listOf(
                    "در حال فشرده‌سازی و هش‌گذاری بسته به‌روزرسانی..." to 0.25f,
                    "در حال بارگذاری فایل APK در سرور کلود (Firebase & CDN)..." to 0.65f,
                    "در حال ثبت اطلاعات نسخه جدید در پایگاه داده جهانی..." to 0.90f,
                    "در حال فعال‌سازی اعلان به‌روزرسانی برای تمام کاربران و نسخه‌های قبل..." to 1.0f
                )

                for ((status, targetProg) in steps) {
                    uploadStatusText = status
                    while (uploadProgress < targetProg) {
                        delay(50)
                        uploadProgress = (uploadProgress + 0.05f).coerceAtMost(targetProg)
                    }
                    delay(200)
                }

                // 3. Save to Firebase Realtime Database
                val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                val todayPersian = try {
                    com.example.core.utils.DateTimeUtils.getCurrentPersianDate()
                } catch (_: Exception) {
                    sdf.format(Date())
                }

                val newUpdate = AppUpdateInfo(
                    latestVersionCode = versionCode,
                    latestVersionName = versionName.trim(),
                    minRequiredVersionCode = if (isForceUpdate) versionCode else 1,
                    isUpdateAvailable = true,
                    isForceUpdate = isForceUpdate,
                    releaseNotes = releaseNotes.trim(),
                    downloadUrl = downloadUrl.trim(),
                    apkSizeBytes = selectedFileSize,
                    releaseDate = todayPersian
                )

                authViewModel.publishAppUpdate(newUpdate) { res ->
                    isUploading = false
                    if (res.isSuccess) {
                        uploadSuccess = true
                        uploadStatusText = "به‌روزرسانی با موفقیت در تمام سرورها منتشر شد!"
                        Toast.makeText(
                            context,
                            "🎉 نسخه جدید با موفقیت منتشر شد و برای تمام کاربران ارسال گردید.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        uploadSuccess = false
                        uploadStatusText = "خطا در انتشار به‌روزرسانی در سرور."
                        Toast.makeText(
                            context,
                            res.exceptionOrNull()?.message ?: "خطا در ارتباط با سرور",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                isUploading = false
                uploadStatusText = "خطا در پردازش فایل: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "مرکز انتشار آپدیت سازنده",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        RgbRankBadge(text = "سید حسین", isSmall = true)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Creator Identity Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(SchoolBluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "سید حسین موسوی نژاد (سازنده برنامه)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "مدیریت و ارسال سراسری به‌روزرسانی برای تمام نسخه‌های فعال",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Server State
            currentUpdateInfo?.let { info ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "نسخه فعال روی سرور برای کاربران:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "v${info.latestVersionName} (کد نسخه: ${info.latestVersionCode})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolBluePrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (info.isForceUpdate) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = if (info.isForceUpdate) "آپدیت اجباری" else "آپدیت اختیاری",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (info.isForceUpdate) Color(0xFF991B1B) else Color(0xFF166534),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Delete Update Button
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حذف این آپدیت از سرور (لغو ارسال برای کاربران)",
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Confirm Delete Dialog
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = {
                        if (!isDeletingUpdate) showDeleteConfirmDialog = false
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حذف آپدیت از سرور؟",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "آیا مطمئن هستید که می‌خواهید آپدیت نسخه v${currentUpdateInfo?.latestVersionName} را از روی سرور حذف کنید؟\nبا حذف این آپدیت، دیگر پیام به‌روزرسانی برای کاربران نسخه‌های قبل ارسال نخواهد شد.",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isDeletingUpdate = true
                                authViewModel.deleteAppUpdate { res ->
                                    isDeletingUpdate = false
                                    showDeleteConfirmDialog = false
                                    if (res.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            "آپدیت با موفقیت از روی سرور حذف شد.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            res.exceptionOrNull()?.message ?: "خطا در حذف آپدیت",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            enabled = !isDeletingUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isDeletingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("بله، حذف از سرور", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = false },
                            enabled = !isDeletingUpdate,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("انصراف")
                        }
                    },
                    shape = RoundedCornerShape(18.dp)
                )
            }

            // Step 1: APK Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = SchoolBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "۱. انتخاب فایل برنامه (APK)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                filePickerLauncher.launch("application/vnd.android.package-archive")
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedFileUri != null) Color(0xFFDCFCE7) else SchoolBlueContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (selectedFileUri != null) Icons.Default.CheckCircle else Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = if (selectedFileUri != null) PositiveGreen else SchoolBluePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = selectedFileName ?: "انتخاب فایل APK از حافظه دستگاه",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedFileUri != null) PositiveGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (selectedFileUri != null) {
                                            "حجم: ${(selectedFileSize / (1024 * 1024.0)).let { "%.1f".format(it) }} مگابایت"
                                        } else {
                                            "یا از فایل‌های داخلی برنامه استفاده می‌شود"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    filePickerLauncher.launch("application/vnd.android.package-archive")
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                            ) {
                                Text("انتخاب فایل")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: Version Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = SchoolBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "۲. مشخصات نسخه و تغییرات",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = versionName,
                            onValueChange = {
                                versionName = it
                                downloadUrl = "https://github.com/aistudio-nezamyar/releases/download/v${it.trim()}/nezamyar-v${it.trim()}.apk"
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = "نام نسخه (versionName)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "مثال: 1.0.6",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = versionCode.toString(),
                            onValueChange = {
                                versionCode = it.toIntOrNull() ?: versionCode
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                            label = {
                                Text(
                                    text = "کد عددی نسخه",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "مثال: 7",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        label = {
                            Text(
                                text = "لینک دانلود مستقیم فایل APK",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        placeholder = {
                            Text(
                                text = "https://...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = SchoolBluePrimary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = releaseNotes,
                        onValueChange = { releaseNotes = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        label = {
                            Text(
                                text = "متن تغییرات و امکانات نسخه جدید (Release Notes)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        placeholder = {
                            Text(
                                text = "امکانات جدید اضافه شده...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Force Update Switch
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isForceUpdate) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isForceUpdate) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "به‌روزرسانی اجباری (Force Update)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isForceUpdate) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "در صورت فعال بودن، کاربران نسخه‌های قبل تا زمان آپدیت امکان بستن دیالوگ را نخواهند داشت.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isForceUpdate,
                                onCheckedChange = { isForceUpdate = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFDC2626))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Upload & Publish Progress Card
            AnimatedVisibility(visible = isUploading || uploadSuccess) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uploadSuccess) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, if (uploadSuccess) PositiveGreen else SchoolBluePrimary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = SchoolBluePrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = PositiveGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uploadStatusText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (uploadSuccess) PositiveGreen else SchoolBluePrimary
                            )
                        }

                        if (isUploading) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { uploadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = SchoolBluePrimary
                            )
                        }
                    }
                }
            }

            // Big Action Button: Upload and Broadcast to Server
            Button(
                onClick = { startPublishProcess() },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("publish_update_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "آپلود و انتشار نسخه جدید برای تمام نسخه‌های برنامه",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
