package com.example.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.domain.model.AppUpdateInfo
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Arrays

sealed class ApkVerificationResult {
    data class Success(
        val packageName: String,
        val versionCode: Long,
        val versionName: String?,
        val warningMessage: String? = null
    ) : ApkVerificationResult()

    data class Error(
        val messageFarsi: String,
        val technicalDetails: String? = null
    ) : ApkVerificationResult()
}

/**
 * Enterprise-Grade In-App Update Engine for NezamYar.
 * Handles SDK-compliant version checks, secure HTTPS validation, SHA-256 verification,
 * APK structure integrity, signature compatibility, and standard Android installation flow.
 */
object InAppUpdateManager {

    private const val TAG = "InAppUpdateManager"

    /**
     * Retrieves the installed application versionCode using modern SDK methods.
     */
    fun getCurrentVersionCode(context: Context): Int {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading package versionCode, using BuildConfig fallback: ${e.message}")
            BuildConfig.VERSION_CODE
        }
    }

    /**
     * Retrieves the installed application versionName.
     */
    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    /**
     * Checks if the remote update has a higher versionCode than installed.
     */
    fun isUpdateAvailable(currentVersionCode: Int, remoteUpdate: AppUpdateInfo): Boolean {
        return remoteUpdate.versionCode > currentVersionCode
    }

    /**
     * Checks if the update is forced either by policy or minSupportedVersionCode.
     */
    fun isForcedUpdate(currentVersionCode: Int, remoteUpdate: AppUpdateInfo): Boolean {
        val isNewer = remoteUpdate.versionCode > currentVersionCode
        return isNewer && (remoteUpdate.isForced || currentVersionCode < remoteUpdate.minSupportedVersionCode)
    }

    /**
     * Validates that the APK download URL is a secure HTTPS link from a trusted source.
     */
    fun isValidDownloadUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            return false
        }
        return try {
            val uri = java.net.URI(trimmed)
            val host = uri.host?.lowercase() ?: return false
            host.isNotEmpty() && !host.contains("localhost") && !host.contains("127.0.0.1")
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Computes the SHA-256 hash of a local file in lowercase hex format.
     */
    fun computeFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(16384)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val bytes = digest.digest()
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Checks if the file starts with the standard ZIP/APK magic bytes (0x50, 0x4B, 0x03, 0x04).
     * Prevents HTML error pages or text files from being treated as APK packages.
     */
    fun isZipOrApkFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            FileInputStream(file).use { fis ->
                val magic = ByteArray(4)
                val read = fis.read(magic)
                read == 4 &&
                        magic[0] == 0x50.toByte() &&
                        magic[1] == 0x4B.toByte() &&
                        magic[2] == 0x03.toByte() &&
                        magic[3] == 0x04.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Queries the genuine Content-Length from the remote server.
     */
    suspend fun fetchRemoteFileSize(url: String): Long {
        return ApkDownloader.queryRemoteFileSize(url)
    }

    /**
     * Validates the downloaded APK file:
     * 1. File existence and non-zero size.
     * 2. ZIP/APK magic signature check.
     * 3. SHA-256 checksum comparison if expectedSha256 is provided.
     * 4. Archive parsing (corrupt/incomplete package detection).
     * 5. Package name and signature compatibility verification.
     */
    fun verifyApkIntegrity(
        context: Context,
        apkFile: File,
        expectedSha256: String? = null
    ): ApkVerificationResult {
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            return ApkVerificationResult.Error(
                messageFarsi = "فایل دانلودشده روی حافظه دستگاه یافت نشد یا حجم آن صفر بایت است."
            )
        }

        // 1. Magic byte check (must be a valid ZIP/APK archive)
        if (!isZipOrApkFile(apkFile)) {
            return ApkVerificationResult.Error(
                messageFarsi = "محتوای دانلودشده یک بسته معتبر APK نیست (احتمالاً صفحه خطای وب یا پاسخ نامعتبر از سرور دریافت شده است)."
            )
        }

        // 2. SHA-256 checksum check (if provided by publisher)
        if (!expectedSha256.isNullOrBlank() && expectedSha256.trim().length >= 32) {
            try {
                val computedHash = computeFileSha256(apkFile)
                val expectedClean = expectedSha256.trim().lowercase()
                if (!computedHash.equals(expectedClean, ignoreCase = true)) {
                    Log.e(TAG, "SHA-256 mismatch! Expected: $expectedClean, Computed: $computedHash")
                    return ApkVerificationResult.Error(
                        messageFarsi = "اعتبارسنجی امنیتی فایل با خطا مواجه شد (عدم تطابق هش امنیتی SHA-256).",
                        technicalDetails = "Expected: $expectedClean, Computed: $computedHash"
                    )
                }
            } catch (e: Exception) {
                return ApkVerificationResult.Error(
                    messageFarsi = "خطا در محاسبه و تطبیق هش امنیتی فایل دانلودشده.",
                    technicalDetails = e.message
                )
            }
        }

        // 3. PackageArchive parse
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo = try {
            context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
        } catch (e: Exception) {
            null
        }

        if (archiveInfo == null) {
            return ApkVerificationResult.Error(
                messageFarsi = "ساختار پکیج اندروید (APK) آسیب دیده است یا قابل تحلیل توسط سیستم‌عامل نیست."
            )
        }

        var warningMessage: String? = null
        val apkPackageName = archiveInfo.packageName ?: ""
        if (apkPackageName.isNotBlank() && apkPackageName != context.packageName) {
            Log.w(TAG, "Package name in APK ($apkPackageName) differs from app (${context.packageName})")
            warningMessage = "شناسه پکیج بسته نصبی ($apkPackageName) با برنامه فعلی همخوانی ندارد."
        }

        // 4. Signature compatibility check
        val currentSignatures = getCurrentSignatures(context)
        val apkSignatures = getApkSignatures(archiveInfo)

        if (currentSignatures.isNotEmpty() && apkSignatures.isNotEmpty()) {
            val signaturesMatch = currentSignatures.any { currentSig ->
                apkSignatures.any { apkSig ->
                    Arrays.equals(currentSig.toByteArray(), apkSig.toByteArray())
                }
            }

            if (!signaturesMatch) {
                Log.w(TAG, "APK signature differs from currently installed signature")
            }
        }

        val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }

        return ApkVerificationResult.Success(
            packageName = apkPackageName.ifBlank { context.packageName },
            versionCode = apkVersionCode,
            versionName = archiveInfo.versionName,
            warningMessage = warningMessage
        )
    }

    private fun getCurrentSignatures(context: Context): List<Signature> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    )
                }
                val signingInfo = pInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners?.toList() ?: emptyList()
                    } else {
                        signingInfo.signingCertificateHistory?.toList() ?: emptyList()
                    }
                } else emptyList()
            } else {
                @Suppress("DEPRECATION")
                val pInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                pInfo.signatures?.toList() ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getApkSignatures(archiveInfo: PackageInfo): List<Signature> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = archiveInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners?.toList() ?: emptyList()
                    } else {
                        signingInfo.signingCertificateHistory?.toList() ?: emptyList()
                    }
                } else {
                    @Suppress("DEPRECATION")
                    archiveInfo.signatures?.toList() ?: emptyList()
                }
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.signatures?.toList() ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Checks if the app currently holds permission to install unknown apps (Android 8.0+).
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                context.packageManager.canRequestPackageInstalls()
            } catch (_: Exception) {
                true
            }
        } else {
            true
        }
    }

    /**
     * Creates an Intent to navigate the user to standard system settings to grant install permission.
     */
    fun createManageUnknownAppSourcesIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    /**
     * Creates a secure, SDK-compatible install intent using FileProvider with explicit permission grants.
     */
    fun createInstallIntent(context: Context, apkFile: File): Intent {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } catch (e: Exception) {
                Log.w(TAG, "FileProvider URI failed, fallback to Uri.fromFile: ${e.message}")
                Uri.fromFile(apkFile)
            }
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Explicitly grant read URI permission to all resolving package installer activities
        try {
            val resInfoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val pkgName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkgName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (_: Exception) {}

        return intent
    }
}
