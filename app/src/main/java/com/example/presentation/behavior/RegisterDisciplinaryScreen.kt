package com.example.presentation.behavior

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.example.core.utils.DateTimeUtils
import com.example.domain.model.SeverityLevel
import com.example.domain.model.UserRole
import com.example.domain.model.toPersianDigits
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.MainViewModel
import com.example.presentation.viewmodel.OperationState
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.IncidentRedContainer
import com.example.ui.theme.OnIncidentRedContainer
import com.example.ui.theme.VazirmatnFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDisciplinaryScreen(
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
        mutableStateOf(initialStudentId ?: mainViewModel.discStudentId.value.ifBlank { effectiveStudents.firstOrNull()?.id ?: "" })
    }
    var isStudentDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "تأخیر",
        "غیبت",
        "بی‌نظمی",
        "عدم رعایت قوانین مدرسه",
        "رفتار نامناسب",
        "درگیری",
        "آسیب به اموال",
        "استفاده غیرمجاز از تلفن همراه",
        "سایر"
    )

    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var selectedSeverity by remember { mutableStateOf(SeverityLevel.MEDIUM) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("کلاس درس") }
    var notifyParent by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }

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
                    text = "ثبت مورد انضباطی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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

                    // Student Selector
                    Text(
                        text = "انتخاب دانش‌آموز:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var showStudentPicker by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = currentStudent?.let { "${it.fullName} (${it.grade} - ${it.className})" } ?: "برای جستجو و انتخاب دانش‌آموز کلیک کنید...",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "جستجو",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showStudentPicker = true }) {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "انتخاب دانش‌آموز"
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

                    // Severity Selector
                    Text(
                        text = "سطح شدت مورد انضباطی:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeverityLevel.entries.forEach { sev ->
                            FilterChip(
                                selected = selectedSeverity == sev,
                                onClick = {
                                    selectedSeverity = sev
                                    mainViewModel.discSeverity.value = sev
                                },
                                label = { Text(sev.farsiTitle) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Selector
                    Text(
                        text = "عنوان تخلف / دسته‌بندی:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = cat
                                    mainViewModel.discCategory.value = cat
                                },
                                label = { Text(cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            mainViewModel.discTitle.value = it
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        label = {
                            Text(
                                text = "شرح مختصر / عنوان مورد انضباطی *",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        placeholder = {
                            Text(
                                text = "عنوان مورد انضباطی",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("disc_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Location
                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            mainViewModel.discLocation.value = it
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        label = {
                            Text(
                                text = "محل وقوع",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        placeholder = {
                            Text(
                                text = "کلاس، راهرو، حیاط و ...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("disc_location_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            mainViewModel.discDescription.value = it
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
                        label = {
                            Text(
                                text = "توضیحات تکمیلی",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        placeholder = {
                            Text(
                                text = "جزئیات ماجرا و تذکرات داده شده...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("disc_desc_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Date & User
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تاریخ: $currentDate - $currentTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ثبت‌کننده: ${currentUser?.name ?: "کاربر سیستم"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "ثبت در سامانه اولیاء دانش‌آموز",
                                style = MaterialTheme.typography.bodySmall,
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
                                style = MaterialTheme.typography.bodySmall,
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
                                if (title.trim().length < 3) {
                                    mainViewModel.operationState.value = OperationState.Error("عنوان مورد انضباطی باید حداقل ۳ حرف باشد.")
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
                            Text("ثبت", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("لغو")
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
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "تأیید ثبت مورد انضباطی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "آیا از ثبت این مورد انضباطی برای دانش‌آموز «${currentStudent?.fullName}» اطمینان دارید؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        mainViewModel.discStudentId.value = selectedStudentId
                        mainViewModel.discCategory.value = selectedCategory
                        mainViewModel.discTitle.value = title
                        mainViewModel.discDescription.value = description
                        mainViewModel.discLocation.value = location
                        mainViewModel.discSeverity.value = selectedSeverity
                        mainViewModel.discNotifyParent.value = notifyParent

                        mainViewModel.submitDisciplinaryRecord(
                            creatorName = currentUser?.name ?: "معاون",
                            creatorRole = currentUser?.role ?: UserRole.VICE_PRINCIPAL,
                            onSuccess = { onBackClick() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncidentRed)
                ) {
                    Text("بله، ثبت نهایی")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("بازبینی")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }
}
