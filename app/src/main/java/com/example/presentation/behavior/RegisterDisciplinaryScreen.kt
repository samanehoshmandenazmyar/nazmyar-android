package com.example.presentation.behavior

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.DateTimeUtils
import com.example.domain.model.SeverityLevel
import com.example.domain.model.UserRole
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.MainViewModel
import com.example.presentation.viewmodel.OperationState
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.IncidentRedContainer
import com.example.ui.theme.OnIncidentRedContainer
import com.example.ui.theme.VazirmatnFontFamily

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Person
import com.example.presentation.components.VoiceDictationTextField

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.domain.model.AcademicMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDisciplinaryScreen(
    initialStudentId: String? = null,
    isLockedFromScan: Boolean = false,
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
        mutableStateOf(initialStudentId ?: mainViewModel.discStudentId.value)
    }

    var discText by remember { mutableStateOf("") }
    var negativeCountText by remember { mutableStateOf("1") }
    var notifyParent by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var attachedImageBase64 by remember { mutableStateOf<String?>(null) }
    var showStudentPicker by remember { mutableStateOf(false) }

    var selectedMonth by remember { mutableStateOf(AcademicMonth.getCurrentMonth()) }
    var showMonthMenu by remember { mutableStateOf(false) }

    val currentStudent = effectiveStudents.firstOrNull { it.id == selectedStudentId }

    val currentDate = remember { DateTimeUtils.getCurrentPersianDate() }
    val currentTime = remember { DateTimeUtils.getCurrentTime() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("register_disciplinary_screen")
    ) {
        // App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت مورد انضباطی دانش‌آموز",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Form Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {

                    // Student selection field without any lock icon
                    OutlinedTextField(
                        value = currentStudent?.let { "${it.fullName} (${it.className})" } ?: "برای جستجو و انتخاب دانش‌آموز کلیک کنید...",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "دانش‌آموز",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showStudentPicker = true }) {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "تغییر دانش‌آموز"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStudentPicker = true }
                            .testTag("select_disc_student_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (showStudentPicker) {
                        com.example.presentation.components.SearchableStudentPickerDialog(
                            students = effectiveStudents,
                            selectedStudentId = selectedStudentId,
                            title = "انتخاب و جستجوی دانش‌آموز جهت ثبت تخلف",
                            onSelectStudent = { s ->
                                selectedStudentId = s.id
                                mainViewModel.discStudentId.value = s.id
                                showStudentPicker = false
                            },
                            onDismiss = { showStudentPicker = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Disciplinary Description Field with Voice Dictation
                    VoiceDictationTextField(
                        value = discText,
                        onValueChange = {
                            discText = it
                            mainViewModel.discTitle.value = it
                            mainViewModel.discDescription.value = it
                        },
                        label = if (attachedImageBase64 != null) "شرح مورد انضباطی (اختیاری در صورت داشتن تصویر)" else "شرح مورد انضباطی",
                        placeholder = "مثال: تأخیر در ورود به کلاس، عدم انجام تکالیف...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("disc_title_input"),
                        minLines = 3,
                        testTagPrefix = "disc_voice"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Negative Count Field (Numeric Only, Label: تعداد منفی *)
                    OutlinedTextField(
                        value = negativeCountText,
                        onValueChange = { input ->
                            val filtered = input.filter { c -> c.isDigit() }
                            negativeCountText = filtered
                            val count = filtered.toIntOrNull() ?: 1
                            mainViewModel.discPoints.value = count
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = {
                            Text(
                                text = "تعداد منفی *",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        },
                        placeholder = {
                            Text("1", fontFamily = VazirmatnFontFamily)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("disc_count_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Photo Attachment UI
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri: android.net.Uri? ->
                        uri?.let {
                            try {
                                val inputStream = context.contentResolver.openInputStream(it)
                                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                if (bitmap != null) {
                                    val stream = java.io.ByteArrayOutputStream()
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                                    val bytes = stream.toByteArray()
                                    attachedImageBase64 = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
                    ) { bitmap: android.graphics.Bitmap? ->
                        bitmap?.let {
                            val stream = java.io.ByteArrayOutputStream()
                            it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                            val bytes = stream.toByteArray()
                            attachedImageBase64 = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        }
                    }

                    if (attachedImageBase64 != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, IncidentRed)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Report, contentDescription = null, tint = IncidentRed)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تصویر پیوست شد ✓ (شرح متنی اختیاری است)", fontFamily = VazirmatnFontFamily, fontSize = 12.sp, color = IncidentRed, fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { attachedImageBase64 = null }) {
                                    Icon(Icons.Default.Report, contentDescription = "حذف عکس", tint = IncidentRedContainer)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = IncidentRed),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("عکس با دوربین", fontFamily = VazirmatnFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("انتخاب از گالری", fontFamily = VazirmatnFontFamily, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Month Selector Field
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "ماه ${selectedMonth.persianName} (${selectedMonth.season})",
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    text = "ماه ثبت در پرونده انضباطی",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showMonthMenu = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "انتخاب ماه")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMonthMenu = true }
                                .testTag("select_month_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        DropdownMenu(
                            expanded = showMonthMenu,
                            onDismissRequest = { showMonthMenu = false }
                        ) {
                            AcademicMonth.ACADEMIC_YEAR_ORDER.forEach { m ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "ماه ${m.persianName} (${m.season})",
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = if (m == selectedMonth) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedMonth = m
                                        showMonthMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Date & User
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
                            text = "ثبت‌کننده: ${currentUser?.name ?: "کاربر سیستم"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Parent Notification Switch
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
                                text = "ارسال پیام به اولیا (در صورت ورود)",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifyParent,
                            onCheckedChange = {
                                notifyParent = it
                                mainViewModel.discNotifyParent.value = it
                            }
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

                    // Action Buttons (ثبت / لغو)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedStudentId.isBlank() || currentStudent == null) {
                                    mainViewModel.operationState.value = OperationState.Error("لطفاً ابتدا یک دانش‌آموز را جهت ثبت مورد انضباطی انتخاب کنید.")
                                    return@Button
                                }
                                val text = discText.trim()
                                if (text.isBlank() && attachedImageBase64 == null) {
                                    mainViewModel.operationState.value = OperationState.Error("لطفاً شرح مورد انضباطی را وارد کرده یا یک تصویر پیوست نمایید.")
                                } else {
                                    showConfirmDialog = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("submit_disc_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = IncidentRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Report, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ثبت مورد انضباطی", fontWeight = FontWeight.Bold, fontFamily = VazirmatnFontFamily)
                        }

                        OutlinedButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("لغو", fontFamily = VazirmatnFontFamily)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showConfirmDialog) {
        val text = discText.trim()
        val finalText = if (text.isBlank()) "تصویر پیوست شده مورد انضباطی" else text

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "تأیید ثبت مورد انضباطی",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = VazirmatnFontFamily),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "آیا از ثبت این مورد انضباطی برای دانش‌آموز «${currentStudent?.fullName}» اطمینان دارید؟",
                    fontFamily = VazirmatnFontFamily
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = negativeCountText.toIntOrNull() ?: 1
                        showConfirmDialog = false
                        mainViewModel.registerBehaviorRecord(
                            studentId = selectedStudentId,
                            type = com.example.domain.model.RecordType.DISCIPLINARY,
                            category = "مورد انضباطی",
                            title = "مورد انضباطی",
                            description = finalText,
                            date = "$currentDate (${selectedMonth.persianName})",
                            points = count,
                            createdBy = currentUser?.name ?: "معاون",
                            createdByRole = currentUser?.role ?: UserRole.VICE_PRINCIPAL,
                            parentNotified = notifyParent,
                            imageUrl = attachedImageBase64
                        )
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncidentRed)
                ) {
                    Text("بله، ثبت نهایی", fontFamily = VazirmatnFontFamily)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("بازبینی", fontFamily = VazirmatnFontFamily)
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }
}
