package com.example.presentation.face

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.auth.AccessControl
import com.example.core.face.FaceRecognitionManager
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.toPersianDigits
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.theme.WarningAmber

@Composable
fun FaceRegisterCard(
    student: Student,
    currentUser: User?,
    onFaceUpdated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FaceRecognitionManager.init(context)
    }

    val canManage = remember(currentUser) {
        AccessControl.canManageFacesAndBehaviors(currentUser)
    }

    // Students and parents must NEVER see or use face management
    if (!canManage) return

    var isEnrolled by remember(student.id) {
        mutableStateOf(FaceRecognitionManager.isFaceEnrolled(student.id))
    }

    var showCameraView by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var operationMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("face_management_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = SchoolBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شناسایی بیومتریک چهره",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirmatnFontFamily
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isEnrolled) PositiveGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isEnrolled) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isEnrolled) PositiveGreen else WarningAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEnrolled) "چهره فعال است" else "ثبت نشده",
                            color = if (isEnrolled) PositiveGreen else WarningAmber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isEnrolled) {
                    "الگوی چهره ${student.fullName} با موفقیت ثبت شده و در اسکن سریع قابل شناسایی است."
                } else {
                    "چهره این دانش‌آموز هنوز ثبت نشده است. با دوربین گوشی می‌توانید الگوی چهره ایشان را ثبت کنید."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = VazirmatnFontFamily,
                lineHeight = 18.sp
            )

            if (operationMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isError) IncidentRed.copy(alpha = 0.12f) else PositiveGreen.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = operationMessage!!,
                        color = if (isError) IncidentRed else PositiveGreen,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = VazirmatnFontFamily,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        operationMessage = null
                        showCameraView = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("register_face_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnrolled) "بروزرسانی چهره" else "ثبت چهره دانش‌آموز",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isEnrolled) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IncidentRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("delete_face_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف چهره",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "حذف چهره",
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Full-screen camera capture dialog
    if (showCameraView) {
        Dialog(
            onDismissRequest = { showCameraView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FaceCaptureCameraView(
                title = "ثبت چهره ${student.fullName}",
                subtitle = "چهره دانش‌آموز را مستقیم روبروی دوربین بگیرید",
                onBitmapCaptured = { bitmap ->
                    val success = FaceRecognitionManager.enrollFace(context, student.id, bitmap)
                    showCameraView = false
                    if (success) {
                        isEnrolled = true
                        isError = false
                        operationMessage = "الگوی چهره ${student.fullName} با موفقیت استخراج و ذخیره شد."
                        onFaceUpdated()
                    } else {
                        isError = true
                        operationMessage = "خطا در استخراج الگوی چهره. لطفاً در نور مناسب و بدون پوشش چهره مجدداً تلاش کنید."
                    }
                },
                onCancel = { showCameraView = false }
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text("حذف الگوی چهره", fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "آیا از حذف الگوی چهره ذخیره‌شده برای ${student.fullName} اطمینان دارید؟ پس از حذف، در اسکن سریع شناخته نخواهد شد.",
                    fontFamily = VazirmatnFontFamily
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        FaceRecognitionManager.deleteFace(context, student.id)
                        isEnrolled = false
                        showDeleteConfirm = false
                        isError = false
                        operationMessage = "الگوی چهره با موفقیت حذف شد."
                        onFaceUpdated()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncidentRed)
                ) {
                    Text("حذف شود", fontFamily = VazirmatnFontFamily)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("انصراف", fontFamily = VazirmatnFontFamily)
                }
            }
        )
    }
}
