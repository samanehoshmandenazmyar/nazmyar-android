package com.example.presentation.components

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.update.ApkDownloader
import com.example.core.update.ApkVerificationResult
import com.example.core.update.InAppUpdateManager
import com.example.domain.model.AppUpdateInfo
import com.example.domain.model.toPersianDigits
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class UpdateState {
    READY_TO_DOWNLOAD,
    DOWNLOADING,
    VERIFYING,
    PERMISSION_REQUEST,
    READY_TO_INSTALL,
    INSTALLING,
    ERROR
}

/**
 * Enterprise In-App Update Dialog for NezamYar.
 * Performs 100% genuine byte-level HTTP streaming downloads directly from GitHub Releases / HTTPS servers.
 * Calculates real file size from Content-Length or byte-streaming, with zero simulated or fake progress.
 */
@Composable
fun InAppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var updateState by remember { mutableStateOf(UpdateState.READY_TO_DOWNLOAD) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    // Start with -1L (indeterminate) unless positive server size is already verified
    var totalBytes by remember {
        mutableLongStateOf(if (updateInfo.apkSizeBytes > 0L) updateInfo.apkSizeBytes else -1L)
    }
    var isQueryingServerSize by remember { mutableStateOf(totalBytes <= 0L) }
    var downloadSpeedKbps by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorDetails by remember { mutableStateOf<String?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    val currentVersionCode = remember { InAppUpdateManager.getCurrentVersionCode(context) }
    val currentVersionName = remember { InAppUpdateManager.getCurrentVersionName(context) }
    val isForced = remember { InAppUpdateManager.isForcedUpdate(currentVersionCode, updateInfo) }
    val targetUrl = remember(updateInfo) { updateInfo.apkUrl.ifBlank { updateInfo.downloadUrl }.trim() }

    // Query genuine remote file size via HTTP HEAD / Range request if not known
    LaunchedEffect(targetUrl) {
        if (totalBytes <= 0L && InAppUpdateManager.isValidDownloadUrl(targetUrl)) {
            isQueryingServerSize = true
            val realLength = ApkDownloader.queryRemoteFileSize(targetUrl)
            if (realLength > 0L) {
                totalBytes = realLength
            }
            isQueryingServerSize = false
        }
    }

    // Lifecycle observer: check if installed version updated when user returns from package installer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val installedCode = InAppUpdateManager.getCurrentVersionCode(context)
                if (installedCode >= updateInfo.versionCode) {
                    downloadedFile?.delete()
                    onDismiss()
                } else if (updateState == UpdateState.PERMISSION_REQUEST) {
                    if (InAppUpdateManager.canRequestPackageInstalls(context)) {
                        updateState = UpdateState.READY_TO_INSTALL
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            downloadJob?.cancel()
        }
    }

    // Permission launcher for unknown app sources (Android 8.0+)
    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (InAppUpdateManager.canRequestPackageInstalls(context)) {
            updateState = UpdateState.READY_TO_INSTALL
            downloadedFile?.let { apk ->
                try {
                    val intent = InAppUpdateManager.createInstallIntent(context, apk)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    errorMessage = "خطا در اجرای برنامه نصب اندروید: ${e.message}"
                    updateState = UpdateState.ERROR
                }
            }
        } else {
            updateState = UpdateState.PERMISSION_REQUEST
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = downloadProgress,
        label = "update_download_progress"
    )

    fun startRealDownload() {
        if (!InAppUpdateManager.isValidDownloadUrl(targetUrl)) {
            errorMessage = "آدرس دانلود نسخه جدید نامعتبر است (باید با https:// شروع شود)."
            updateState = UpdateState.ERROR
            return
        }

        updateState = UpdateState.DOWNLOADING
        downloadProgress = 0f
        downloadedBytes = 0L
        downloadSpeedKbps = 0L
        errorMessage = null
        errorDetails = null

        val apkDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(apkDir, "nezamyar_v${updateInfo.versionName}.apk")
        downloadedFile = targetFile

        // Fast path: if genuine valid APK is already completely downloaded, verify and skip redownload
        if (targetFile.exists() && targetFile.length() > 0L) {
            val cachedResult = InAppUpdateManager.verifyApkIntegrity(context, targetFile, updateInfo.sha256)
            if (cachedResult is ApkVerificationResult.Success) {
                totalBytes = targetFile.length()
                downloadedBytes = targetFile.length()
                downloadProgress = 1f
                updateState = UpdateState.READY_TO_INSTALL
                return
            } else {
                targetFile.delete()
            }
        }

        downloadJob?.cancel()
        downloadJob = scope.launch(Dispatchers.IO) {
            try {
                // Execute real byte-level streaming HTTP download
                val completedFile = ApkDownloader.downloadApk(
                    url = targetUrl,
                    destinationFile = targetFile,
                    onProgress = { currentRead, serverTotal, speed ->
                        // Safely dispatch UI state updates to the Main thread
                        scope.launch(Dispatchers.Main) {
                            downloadedBytes = currentRead
                            if (serverTotal > 0L) {
                                totalBytes = serverTotal
                                downloadProgress = (currentRead.toFloat() / serverTotal).coerceIn(0f, 1f)
                            } else {
                                downloadProgress = 0f
                            }
                            downloadSpeedKbps = speed
                        }
                    },
                    isCancelled = { !isActive }
                )

                withContext(Dispatchers.Main) {
                    downloadProgress = 1f
                    downloadedBytes = completedFile.length()
                    totalBytes = completedFile.length()
                    updateState = UpdateState.VERIFYING
                }

                // Verify genuine APK package structure and SHA-256
                val verifyResult = InAppUpdateManager.verifyApkIntegrity(
                    context = context,
                    apkFile = completedFile,
                    expectedSha256 = updateInfo.sha256
                )

                withContext(Dispatchers.Main) {
                    when (verifyResult) {
                        is ApkVerificationResult.Success -> {
                            updateState = UpdateState.READY_TO_INSTALL
                        }
                        is ApkVerificationResult.Error -> {
                            completedFile.delete()
                            errorMessage = verifyResult.messageFarsi
                            errorDetails = verifyResult.technicalDetails
                            updateState = UpdateState.ERROR
                        }
                    }
                }
            } catch (e: InterruptedException) {
                withContext(Dispatchers.Main) {
                    updateState = UpdateState.READY_TO_DOWNLOAD
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = e.message ?: "خطا در دریافت فایل بسته نصبی از سرور."
                    updateState = UpdateState.ERROR
                }
            }
        }
    }

    fun initiateInstall() {
        val file = downloadedFile ?: return
        if (!file.exists() || file.length() <= 0L) {
            errorMessage = "فایل بسته نصبی روی حافظه دستگاه یافت نشد. لطفاً مجدداً دانلود نمایید."
            updateState = UpdateState.ERROR
            return
        }

        if (!InAppUpdateManager.canRequestPackageInstalls(context)) {
            updateState = UpdateState.PERMISSION_REQUEST
            val intent = InAppUpdateManager.createManageUnknownAppSourcesIntent(context)
            installPermissionLauncher.launch(intent)
            return
        }

        try {
            updateState = UpdateState.INSTALLING
            val installIntent = InAppUpdateManager.createInstallIntent(context, file)
            context.startActivity(installIntent)
        } catch (e: Exception) {
            errorMessage = "خطا در فراخوانی بخش نصب اندروید: ${e.message}"
            updateState = UpdateState.ERROR
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isForced && updateState != UpdateState.DOWNLOADING && updateState != UpdateState.VERIFYING) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForced && updateState != UpdateState.DOWNLOADING && updateState != UpdateState.VERIFYING,
            dismissOnClickOutside = !isForced && updateState != UpdateState.DOWNLOADING && updateState != UpdateState.VERIFYING
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isForced) Color(0xFFFEE2E2) else SchoolBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isForced) Icons.Default.Warning else Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = if (isForced) Color(0xFFDC2626) else SchoolBluePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isForced) "به‌روزرسانی ضروری سامانه" else "نسخه جدید منتشر شد",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = VazirmatnFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "نسخه ${updateInfo.versionName} (کد نسخه: ${updateInfo.versionCode.toPersianDigits()})",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isForced && updateState != UpdateState.DOWNLOADING && updateState != UpdateState.VERIFYING) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("update_dialog_close_icon")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Info Card: Current Version vs Server Version
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "نسخه فعلی شما:",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "v$currentVersionName (کد: ${currentVersionCode.toPersianDigits()})",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = VazirmatnFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = SchoolBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "نسخه جدید در دسترس:",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "v${updateInfo.versionName}",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = VazirmatnFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = PositiveGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Release Notes / Update Message
                val notes = updateInfo.updateMessage.ifBlank { updateInfo.releaseNotes }
                if (notes.isNotBlank()) {
                    Text(
                        text = "تغییرات و امکانات نسخه جدید:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                            color = Color(0xFF334155),
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Real File Size Display (Calculated from actual server response or local disk)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "حجم بسته نصبی:",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val sizeText = when {
                        downloadedFile != null && downloadedFile!!.exists() && downloadedFile!!.length() > 0L -> {
                            val mb = String.format(Locale.US, "%.1f", downloadedFile!!.length() / (1024.0 * 1024.0))
                            "$mb مگابایت (دانلودشده)".toPersianDigits()
                        }
                        totalBytes > 0L -> {
                            val mb = String.format(Locale.US, "%.1f", totalBytes / (1024.0 * 1024.0))
                            "$mb مگابایت".toPersianDigits()
                        }
                        isQueryingServerSize -> "در حال استعلام از سرور..."
                        else -> "نامشخص (هنگام دانلود محاسبه می‌شود)"
                    }
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // SHA-256 Checksum badge if available
                if (updateInfo.sha256.isNotBlank() && updateInfo.sha256.length >= 32) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "اعتبارسنجی هش امنیتی SHA-256:",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                color = Color(0xFF0369A1)
                            )
                        }
                        Text(
                            text = "فعال و ایمن",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = PositiveGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive States
                when (updateState) {
                    UpdateState.READY_TO_DOWNLOAD -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isForced) {
                                        "برای ادامه استفاده از سامانه نظمیار، دریافت این به‌روزرسانی الزامی است."
                                    } else {
                                        "برای ارتقا به آخرین امکانات، بر روی دکمه «به‌روزرسانی» کلیک نمایید."
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                    color = Color(0xFF1E40AF)
                                )
                            }
                        }
                    }

                    UpdateState.DOWNLOADING -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "در حال بارگیری مستقیم فایل از سرور...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolBluePrimary
                                )
                                val percentText = if (totalBytes > 0L) {
                                    "${(animatedProgress * 100).toInt()}%".toPersianDigits()
                                } else {
                                    "در حال دانلود..."
                                }
                                Text(
                                    text = percentText,
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = VazirmatnFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolBluePrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (totalBytes > 0L) {
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .testTag("download_progress_bar"),
                                    color = SchoolBluePrimary,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .testTag("download_progress_bar"),
                                    color = SchoolBluePrimary,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val downloadedMb = String.format(Locale.US, "%.1f", downloadedBytes / (1024.0 * 1024.0))
                                val progressLabel = if (totalBytes > 0L) {
                                    val totalMb = String.format(Locale.US, "%.1f", totalBytes / (1024.0 * 1024.0))
                                    "$downloadedMb / $totalMb مگابایت".toPersianDigits()
                                } else {
                                    "$downloadedMb مگابایت دریافت‌شده".toPersianDigits()
                                }
                                Text(
                                    text = progressLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                    color = Color(0xFF64748B)
                                )
                                if (downloadSpeedKbps > 0) {
                                    Text(
                                        text = "سرعت: ${downloadSpeedKbps.toPersianDigits()} KB/s",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    UpdateState.VERIFYING -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PositiveGreen,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "در حال اعتبارسنجی بایت‌های دریافتی و ساختار پکیج APK...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }

                    UpdateState.PERMISSION_REQUEST -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.InstallMobile,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "مجوز نصب برنامه لازم است",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                                        color = Color(0xFF92400E)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "جهت تکمیل فرآیند نصب نسخه جدید، لطفاً در صفحه تنظیمات گزینه «نصب برنامه‌های ناشناس» را برای سامانه فعال فرمایید.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    }

                    UpdateState.READY_TO_INSTALL -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PositiveGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "بسته نصبی با موفقیت و صحت کامل دریافت شد!",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                                        color = Color(0xFF166534)
                                    )
                                    val diskBytes = downloadedFile?.length() ?: 0L
                                    val diskMb = String.format(Locale.US, "%.1f", diskBytes / (1024.0 * 1024.0))
                                    Text(
                                        text = "حجم واقعی ذخیره‌شده روی حافظه: $diskMb مگابایت".toPersianDigits(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }

                    UpdateState.INSTALLING -> {
                        Text(
                            text = "در حال انتقال بسته نصبی به برنامه مدیریت نصب اندروید...",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                            color = SchoolBluePrimary
                        )
                    }

                    UpdateState.ERROR -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = errorMessage ?: "خطایی در فرآیند دریافت به‌روزرسانی رخ داد.",
                                    color = Color(0xFF991B1B),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                                    fontWeight = FontWeight.Bold
                                )
                                errorDetails?.let { details ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = details,
                                        color = Color(0xFFB91C1C),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (updateState) {
                UpdateState.READY_TO_DOWNLOAD -> {
                    Button(
                        onClick = { startRealDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("start_update_download_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "به‌روزرسانی",
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily
                        )
                    }
                }

                UpdateState.DOWNLOADING -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            val text = if (totalBytes > 0L) {
                                "در حال دانلود (${(animatedProgress * 100).toInt()}%)".toPersianDigits()
                            } else {
                                "در حال دریافت..."
                            }
                            Text(text = text, fontFamily = VazirmatnFontFamily)
                        }
                        if (!isForced) {
                            OutlinedButton(
                                onClick = {
                                    downloadJob?.cancel()
                                    updateState = UpdateState.READY_TO_DOWNLOAD
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("لغو", fontFamily = VazirmatnFontFamily)
                            }
                        }
                    }
                }

                UpdateState.VERIFYING -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "در حال بررسی بسته...", fontFamily = VazirmatnFontFamily)
                    }
                }

                UpdateState.PERMISSION_REQUEST -> {
                    Button(
                        onClick = {
                            val intent = InAppUpdateManager.createManageUnknownAppSourcesIntent(context)
                            installPermissionLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اعطای مجوز و ادامه", fontWeight = FontWeight.Bold, fontFamily = VazirmatnFontFamily)
                    }
                }

                UpdateState.READY_TO_INSTALL -> {
                    Button(
                        onClick = { initiateInstall() },
                        colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("install_update_button")
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "نصب نسخه جدید", fontWeight = FontWeight.Bold, fontFamily = VazirmatnFontFamily)
                    }
                }

                UpdateState.INSTALLING -> {
                    Button(
                        onClick = { initiateInstall() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تلاش مجدد نصب", fontFamily = VazirmatnFontFamily)
                    }
                }

                UpdateState.ERROR -> {
                    Button(
                        onClick = { startRealDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("retry_update_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تلاش مجدد دانلود", fontFamily = VazirmatnFontFamily)
                    }
                }
            }
        },
        dismissButton = {
            if (!isForced && updateState != UpdateState.DOWNLOADING && updateState != UpdateState.VERIFYING) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("update_dismiss_button")
                ) {
                    Text("بعداً", fontFamily = VazirmatnFontFamily)
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("update_dialog")
    )
}
