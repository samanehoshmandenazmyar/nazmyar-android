package com.example.presentation.face

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun FaceCaptureCameraView(
    title: String = "اسکن و تشخیص چهره",
    subtitle: String = "صورت دانش‌آموز را داخل کادر دایره‌ای قرار دهید",
    onBitmapCaptured: (Bitmap) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission check state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Default lens selection: REAR / BACK CAMERA as requested
    var cameraFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraErrorState by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var isCapturing by remember { mutableStateOf(false) }

    // Dynamic rotation tracking
    var displayRotation by remember { mutableIntStateOf(Surface.ROTATION_0) }

    // Thread executor for image capture operations
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // ImageCapture use case instance with dynamic target rotation
    val imageCapture = remember(displayRotation) {
        ImageCapture.Builder()
            .setTargetRotation(displayRotation)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // Register OrientationEventListener for continuous real-time orientation tracking
    DisposableEffect(context) {
        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val newRotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                if (displayRotation != newRotation) {
                    displayRotation = newRotation
                    try {
                        imageCapture.targetRotation = newRotation
                    } catch (e: Exception) {
                        Log.w("CameraRotation", "Failed to update target rotation: ${e.message}")
                    }
                }
            }
        }
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }

        onDispose {
            orientationEventListener.disable()
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("face_camera_view")
    ) {
        if (!hasCameraPermission) {
            // Permission Request Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "دسترسی به دوربین مورد نیاز است",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = VazirmatnFontFamily
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "جهت اسکن و ثبت چهره دانش‌آموزان، دسترسی دوربین را تأیید نمایید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = VazirmatnFontFamily,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                    modifier = Modifier.testTag("grant_camera_permission_btn")
                ) {
                    Text("درخواست دسترسی دوربین", fontFamily = VazirmatnFontFamily)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("بازگشت", fontFamily = VazirmatnFontFamily)
                }
            }
        } else if (cameraErrorState != null) {
            // Camera Error & Hardware Fallback Failure State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "خطا در راه‌اندازی دوربین",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = VazirmatnFontFamily
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = cameraErrorState ?: "دوربین دستگاه در دسترس نیست یا توسط برنامه دیگری اشغال شده است.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = VazirmatnFontFamily,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            cameraErrorState = null
                            retryTrigger += 1
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("تلاش مجدد", fontFamily = VazirmatnFontFamily)
                        }
                    }

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("انصراف", fontFamily = VazirmatnFontFamily)
                    }
                }
            }
        } else {
            // Camera Preview View
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { previewView ->
                    // Touch retryTrigger to force update re-evaluation if user clicked Retry
                    @Suppress("UNUSED_VARIABLE")
                    val retryVal = retryTrigger

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            // Get current display rotation from PreviewView context or display
                            val currentRot = previewView.display?.rotation ?: displayRotation

                            val preview = Preview.Builder()
                                .setTargetRotation(currentRot)
                                .build().also { p ->
                                    p.surfaceProvider = previewView.surfaceProvider
                                }

                            // Determine target selector with hardware fallback logic
                            var targetLens = cameraFacing
                            var cameraSelector = CameraSelector.Builder().requireLensFacing(targetLens).build()

                            // Check availability of requested lens
                            if (!cameraProvider.hasCamera(cameraSelector)) {
                                // Fallback to opposite lens (Front if Back failed, or Back if Front failed)
                                targetLens = if (targetLens == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                                cameraSelector = CameraSelector.Builder().requireLensFacing(targetLens).build()

                                // If opposite lens also fails, attempt first available camera info
                                if (!cameraProvider.hasCamera(cameraSelector)) {
                                    val availableInfo = cameraProvider.availableCameraInfos.firstOrNull()
                                    if (availableInfo != null) {
                                        cameraSelector = availableInfo.cameraSelector
                                    } else {
                                        cameraErrorState = "هیچ دوربینی روی دستگاه یافت نشد یا دوربین غیرفعال است."
                                        return@addListener
                                    }
                                }
                            }

                            // Keep current working camera facing state synced
                            cameraFacing = targetLens

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                            cameraErrorState = null
                        } catch (e: Exception) {
                            Log.e("CameraSetup", "Failed to bind camera use cases", e)
                            cameraErrorState = "بروز خطا هنگام اتصال به دوربین: ${e.localizedMessage ?: "دوربین ناموفق بود"}"
                        }
                    }, ContextCompat.getMainExecutor(previewView.context))
                },
                modifier = Modifier.fillMaxSize()
            )

            // UI Reticle & Control Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("close_camera_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "انصراف",
                            tint = Color.White
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("switch_camera_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "چرخش دوربین",
                            tint = Color.White
                        )
                    }
                }

                // Center Circle Reticle & Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .border(3.dp, SchoolBluePrimary, CircleShape)
                            .padding(6.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = subtitle,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = VazirmatnFontFamily,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // Shutter Capture Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = SchoolBluePrimary,
                            modifier = Modifier.size(56.dp)
                        )
                    } else {
                        FilledIconButton(
                            onClick = {
                                if (isCapturing) return@FilledIconButton
                                isCapturing = true

                                imageCapture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val rotation = image.imageInfo.rotationDegrees
                                            val rawBitmap = image.toBitmap()
                                            image.close()

                                            // Apply rotation matrix
                                            val matrix = Matrix()
                                            if (rotation != 0) {
                                                matrix.postRotate(rotation.toFloat())
                                            }
                                            // Mirror front camera captures so they appear naturally
                                            if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                                                matrix.postScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f)
                                            }

                                            val finalBitmap = if (!matrix.isIdentity) {
                                                Bitmap.createBitmap(
                                                    rawBitmap,
                                                    0,
                                                    0,
                                                    rawBitmap.width,
                                                    rawBitmap.height,
                                                    matrix,
                                                    true
                                                )
                                            } else {
                                                rawBitmap
                                            }

                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                                onBitmapCaptured(finalBitmap)
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("CameraCapture", "Error capturing image", exception)
                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                            }
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .border(4.dp, Color.White, CircleShape)
                                .testTag("shutter_capture_btn"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = SchoolBluePrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "عکس‌برداری و اسکن",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
