package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.session.SessionManager
import com.example.core.update.ApkDownloader
import com.example.core.update.ApkVerificationResult
import com.example.core.update.InAppUpdateManager
import com.example.domain.model.AppUpdateInfo
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InAppUpdateTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        SessionManager.init(context)
    }

    @Test
    fun testUpdateAvailable_whenRemoteVersionIsHigher() {
        val currentVersionCode = 8
        val remoteUpdate = AppUpdateInfo(
            versionCode = 9,
            versionName = "1.1.0",
            isUpdateAvailable = true
        )
        val needed = InAppUpdateManager.isUpdateAvailable(currentVersionCode, remoteUpdate)
        assertTrue("Update should be available when remote versionCode > currentVersionCode", needed)
    }

    @Test
    fun testUpdateNotAvailable_whenVersionsAreEqual() {
        val currentVersionCode = 8
        val remoteUpdate = AppUpdateInfo(
            versionCode = 8,
            versionName = "1.0.7",
            isUpdateAvailable = false
        )
        val needed = InAppUpdateManager.isUpdateAvailable(currentVersionCode, remoteUpdate)
        assertFalse("Update should not be available when remote versionCode == currentVersionCode", needed)
    }

    @Test
    fun testUpdateNotAvailable_whenRemoteVersionIsLower() {
        val currentVersionCode = 10
        val remoteUpdate = AppUpdateInfo(
            versionCode = 8,
            versionName = "1.0.7"
        )
        val needed = InAppUpdateManager.isUpdateAvailable(currentVersionCode, remoteUpdate)
        assertFalse("Update should not be available when remote versionCode < currentVersionCode", needed)
    }

    @Test
    fun testForcedUpdate_whenExplicitlyForced() {
        val currentVersionCode = 7
        val remoteUpdate = AppUpdateInfo(
            versionCode = 8,
            isForced = true,
            minSupportedVersionCode = 1
        )
        val forced = InAppUpdateManager.isForcedUpdate(currentVersionCode, remoteUpdate)
        assertTrue("Update should be forced when isForced is true", forced)
    }

    @Test
    fun testForcedUpdate_whenCurrentBelowMinSupportedVersion() {
        val currentVersionCode = 3
        val remoteUpdate = AppUpdateInfo(
            versionCode = 8,
            isForced = false,
            minSupportedVersionCode = 5
        )
        val forced = InAppUpdateManager.isForcedUpdate(currentVersionCode, remoteUpdate)
        assertTrue("Update should be forced when currentVersionCode < minSupportedVersionCode", forced)
    }

    @Test
    fun testOptionalUpdate_whenSupportedAndNotForced() {
        val currentVersionCode = 6
        val remoteUpdate = AppUpdateInfo(
            versionCode = 8,
            isForced = false,
            minSupportedVersionCode = 5
        )
        val forced = InAppUpdateManager.isForcedUpdate(currentVersionCode, remoteUpdate)
        assertFalse("Update should be optional when isForced=false and current >= minSupported", forced)
    }

    @Test
    fun testUrlValidation_rejectsHttpAndAcceptsHttps() {
        val httpsGithubUrl = "https://github.com/HosseinVexter/Vexter-IP-Scanner/releases/download/v1.1.0/app-release.apk"
        val httpUrl = "http://github.com/HosseinVexter/Vexter-IP-Scanner/releases/download/v1.1.0/app-release.apk"
        val emptyUrl = ""
        val malformedUrl = "not-a-url"

        assertTrue("HTTPS URL must be valid", InAppUpdateManager.isValidDownloadUrl(httpsGithubUrl))
        assertFalse("HTTP URL must be rejected for security", InAppUpdateManager.isValidDownloadUrl(httpUrl))
        assertFalse("Empty URL must be rejected", InAppUpdateManager.isValidDownloadUrl(emptyUrl))
        assertFalse("Malformed URL must be rejected", InAppUpdateManager.isValidDownloadUrl(malformedUrl))
    }

    @Test
    fun testSha256ChecksumCalculation() {
        val testFile = File.createTempFile("test_sha", ".bin")
        try {
            val content = "NezamYar Enterprise Security Update Verification Payload"
            testFile.writeText(content)

            val computedHash = InAppUpdateManager.computeFileSha256(testFile)

            val md = MessageDigest.getInstance("SHA-256")
            val expectedHash = md.digest(content.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

            assertEquals("Computed SHA-256 must match standard calculation", expectedHash, computedHash)
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testZipMagicByteValidation_distinguishesApkFromHtml() {
        val validZipFile = File.createTempFile("valid_apk", ".apk")
        val fakeHtmlFile = File.createTempFile("fake_html", ".apk")

        try {
            // Write standard ZIP/APK header 'PK\x03\x04'
            FileOutputStream(validZipFile).use { fos ->
                fos.write(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00))
            }

            // Write HTML text that GitHub returns on 404 or errors
            fakeHtmlFile.writeText("<!DOCTYPE html><html><head><title>404 Not Found</title></head></html>")

            assertTrue("Valid ZIP/APK file must pass magic byte check", InAppUpdateManager.isZipOrApkFile(validZipFile))
            assertFalse("HTML response disguised as APK must fail magic byte check", InAppUpdateManager.isZipOrApkFile(fakeHtmlFile))
        } finally {
            validZipFile.delete()
            fakeHtmlFile.delete()
        }
    }

    @Test
    fun testRealFileSizeQuery_fromGitHubReleases() = runBlocking {
        val realGithubUrl = "https://github.com/HosseinVexter/Vexter-IP-Scanner/releases/download/v1.1.0/app-release.apk"
        val realLength = ApkDownloader.queryRemoteFileSize(realGithubUrl)

        // Real size of HosseinVexter release asset is 19420705 bytes (~19.4 MB)
        assertTrue("Real remote file size must be greater than 15 MB", realLength > 15_000_000L)
        assertEquals(19420705L, realLength)
    }

    @Test
    fun testJsonPayloadParsing_supportsBothNestedAndFlatSchemas() {
        val nestedJsonStr = """
            {
              "app_update": {
                "version_code": 12,
                "version_name": "1.2.0",
                "apk_url": "https://github.com/HosseinVexter/Vexter-IP-Scanner/releases/download/v1.1.0/app-release.apk",
                "update_message": "رفع باگ‌ها، بهبود عملکرد و افزودن امکانات جدید",
                "is_forced": false,
                "min_supported_version_code": 5,
                "sha256": "abcdef1234567890"
              }
            }
        """.trimIndent()

        val rawObj = JSONObject(nestedJsonStr)
        val obj = if (rawObj.has("app_update")) rawObj.getJSONObject("app_update") else rawObj

        assertEquals(12, obj.getInt("version_code"))
        assertEquals("1.2.0", obj.getString("version_name"))
        assertEquals("https://github.com/HosseinVexter/Vexter-IP-Scanner/releases/download/v1.1.0/app-release.apk", obj.getString("apk_url"))
        assertEquals("رفع باگ‌ها، بهبود عملکرد و افزودن امکانات جدید", obj.getString("update_message"))
        assertFalse(obj.getBoolean("is_forced"))
        assertEquals(5, obj.getInt("min_supported_version_code"))
        assertEquals("abcdef1234567890", obj.getString("sha256"))
    }

    @Test
    fun testSessionManager_cachingUpdateInfo() {
        val sampleInfo = AppUpdateInfo(
            versionCode = 15,
            versionName = "2.0.0",
            apkUrl = "https://github.com/HosseinVexter/Vexter-IP-Scanner/releases/download/v1.1.0/app-release.apk",
            updateMessage = "تغییرات بزرگ نسخه جدید",
            isForced = true,
            minSupportedVersionCode = 10,
            sha256 = "08e180f7ee8475f0007c8c697d4ac926aeced49e1a12589e2662789bd98464b2",
            isUpdateAvailable = true
        )

        SessionManager.saveLastKnownUpdate(sampleInfo)
        val cached = SessionManager.getLastKnownUpdate()

        assertNotNull("Cached update should not be null", cached)
        assertEquals(15, cached?.versionCode)
        assertEquals("2.0.0", cached?.versionName)
        assertEquals(true, cached?.isForced)
        assertEquals(10, cached?.minSupportedVersionCode)
        assertEquals("08e180f7ee8475f0007c8c697d4ac926aeced49e1a12589e2662789bd98464b2", cached?.sha256)
        assertEquals("تغییرات بزرگ نسخه جدید", cached?.updateMessage)
    }

    @Test
    fun testApkVerification_rejectsNonExistentFile() {
        val missingFile = File(context.cacheDir, "non_existent_package.apk")
        if (missingFile.exists()) missingFile.delete()

        val result = InAppUpdateManager.verifyApkIntegrity(context, missingFile, "anyhash")
        assertTrue("Verification must fail on missing file", result is ApkVerificationResult.Error)
    }
}
