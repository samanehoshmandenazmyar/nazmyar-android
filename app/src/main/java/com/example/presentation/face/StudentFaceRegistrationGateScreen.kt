package com.example.presentation.face

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.face.FaceRecognitionManager
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Composable
fun StudentFaceRegistrationGateScreen(
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel,
    onRegistrationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.currentUser.collectAsState()

    var frontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var leftBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rightBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var frontBase64 by remember { mutableStateOf<String?>(null) }
    var leftBase64 by remember { mutableStateOf<String?>(null) }
    var rightBase64 by remember { mutableStateOf<String?>(null) }

    var activeAngleIndex by remember { mutableStateOf<Int?>(null) } // 0: Front, 1: Left, 2: Right
    var showCameraView by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Lock back press to prevent student from bypassing face registration gate
    BackHandler {
        Toast.makeText(context, "ثبت چهره از ۳ زاویه برای ورود به سامانه الزامی است", Toast.LENGTH_SHORT).show()
    }

    // Gallery launcher for selecting photo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val base64 = bitmapToBase64(bitmap)
                    when (activeAngleIndex) {
                        0 -> {
                            frontBitmap = bitmap
                            frontBase64 = base64
                        }
                        1 -> {
                            leftBitmap = bitmap
                            leftBase64 = base64
                        }
                        2 -> {
                            rightBitmap = bitmap
                            rightBase64 = base64
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "خطا در بارگذاری تصویر: ${e.message}"
            }
        }
    }

    if (showCameraView && activeAngleIndex != null) {
        val angleTitles = listOf("زاویه ۱: مستقیم (روبرو)", "زاویه ۲: نیم‌رخ چپ", "زاویه ۳: نیم‌رخ راست")
        Dialog(
            onDismissRequest = { showCameraView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FaceCaptureCameraView(
                title = "ثبت چهره دانش‌آموز",
                subtitle = angleTitles.getOrElse(activeAngleIndex!!) { "چهره را در دایره نگه دارید" },
                onBitmapCaptured = { capturedBmp ->
                    showCameraView = false
                    val base64 = bitmapToBase64(capturedBmp)
                    when (activeAngleIndex) {
                        0 -> {
                            frontBitmap = capturedBmp
                            frontBase64 = base64
                        }
                        1 -> {
                            leftBitmap = capturedBmp
                            leftBase64 = base64
                        }
                        2 -> {
                            rightBitmap = capturedBmp
                            rightBase64 = base64
                        }
                    }
                },
                onCancel = { showCameraView = false }
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("student_face_registration_gate_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SchoolBluePrimary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SchoolBluePrimary,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "احراز هویت بیومتریک و ثبت چهره دانش‌آموز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SchoolBluePrimary,
                            fontFamily = VazirmatnFontFamily,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "خوش آمدید ${currentUser?.name ?: ""}! طبق قوانین مدرسه، ثبت چهره از سه زاویه (روبرو، نیم‌رخ چپ و نیم‌رخ راست) جهت ورود به سامانه نظمیار الزامی است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = VazirmatnFontFamily,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Instructions Box
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarningAmber.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "لطفاً در محیطی با نور کافی قرار بگیرید. هر سه زاویه زیر را به ترتیب عکاسی یا بارگذاری نمایید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF78350F),
                        fontFamily = VazirmatnFontFamily
                    )
                }
            }

            // 3 Angles Capture Slots
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Angle 1: Front
                    AngleCaptureCard(
                        angleTitle = "زاویه ۱: روبرو (مستقیم)",
                        angleDescription = "مستقیم به دوربین نگاه کنید",
                        bitmap = frontBitmap,
                        testTag = "angle_front_card",
                        onCameraClick = {
                            activeAngleIndex = 0
                            showCameraView = true
                        },
                        onGalleryClick = {
                            activeAngleIndex = 0
                            galleryLauncher.launch("image/*")
                        }
                    )

                    // Angle 2: Left Profile
                    AngleCaptureCard(
                        angleTitle = "زاویه ۲: نیم‌رخ چپ",
                        angleDescription = "سر را کمی به سمت چپ بچرخانید",
                        bitmap = leftBitmap,
                        testTag = "angle_left_card",
                        onCameraClick = {
                            activeAngleIndex = 1
                            showCameraView = true
                        },
                        onGalleryClick = {
                            activeAngleIndex = 1
                            galleryLauncher.launch("image/*")
                        }
                    )

                    // Angle 3: Right Profile
                    AngleCaptureCard(
                        angleTitle = "زاویه ۳: نیم‌رخ راست",
                        angleDescription = "سر را کمی به سمت راست بچرخانید",
                        bitmap = rightBitmap,
                        testTag = "angle_right_card",
                        onCameraClick = {
                            activeAngleIndex = 2
                            showCameraView = true
                        },
                        onGalleryClick = {
                            activeAngleIndex = 2
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            }

            // Error display if any
            errorMessage?.let { msg ->
                item {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = VazirmatnFontFamily
                    )
                }
            }

            // Submit Button
            item {
                val allAnglesCaptured = frontBitmap != null && leftBitmap != null && rightBitmap != null
                Button(
                    onClick = {
                        if (!allAnglesCaptured) {
                            errorMessage = "لطفاً هر سه زاویه را تکمیل نمایید."
                            return@Button
                        }
                        val user = currentUser ?: return@Button
                        isSubmitting = true
                        errorMessage = null

                        scope.launch {
                            try {
                                val bitmaps = listOfNotNull(frontBitmap, leftBitmap, rightBitmap)
                                val success = FaceRecognitionManager.enrollMultiShotFace(
                                    context = context,
                                    studentId = user.id,
                                    bitmaps = bitmaps
                                )

                                val updatedUser = user.copy(
                                    faceRegistered = true,
                                    faceFrontUrl = frontBase64,
                                    faceLeftUrl = leftBase64,
                                    faceRightUrl = rightBase64,
                                    profileImage = frontBase64 ?: user.profileImage
                                )

                                authViewModel.updateUserProfile(updatedUser)
                                mainViewModel.updateStudentFaceRegistration(
                                    studentId = user.id,
                                    frontUrl = frontBase64,
                                    leftUrl = leftBase64,
                                    rightUrl = rightBase64
                                )

                                isSubmitting = false
                                Toast.makeText(context, "ثبت چهره با موفقیت انجام شد. خوش آمدید!", Toast.LENGTH_LONG).show()
                                onRegistrationSuccess()
                            } catch (e: Exception) {
                                isSubmitting = false
                                errorMessage = "خطا در ثبت چهره: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_student_face_registration_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allAnglesCaptured) PositiveGreen else SchoolBluePrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (allAnglesCaptured) "تکمیل و ورود به برنامه" else "ثبت ۳ زاویه چهره الزامی است",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AngleCaptureCard(
    angleTitle: String,
    angleDescription: String,
    bitmap: Bitmap?,
    testTag: String,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (bitmap != null) PositiveGreen else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Photo Preview or Icon
                Box(modifier = Modifier.size(56.dp)) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = angleTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            shape = CircleShape,
                            color = PositiveGreen,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = angleTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirmatnFontFamily
                    )
                    Text(
                        text = if (bitmap != null) "تصویر اخذ گردید ✓" else angleDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (bitmap != null) PositiveGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = VazirmatnFontFamily
                    )
                }
            }

            // Action Buttons (Camera / Gallery)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    onClick = onCameraClick,
                    shape = RoundedCornerShape(10.dp),
                    color = SchoolBluePrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "عکاسی با دوربین",
                            tint = SchoolBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = onGalleryClick,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "انتخاب از گالری",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    // Compress bitmap to JPEG with quality 80
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    val bytes = outputStream.toByteArray()
    return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}
