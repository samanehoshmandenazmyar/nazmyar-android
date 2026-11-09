package com.example.presentation.reportcard

import android.graphics.Bitmap
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.auth.ParentAccessManager
import com.example.core.reportcard.ReportCardExportHelper
import com.example.core.reportcard.ReportCardManager
import com.example.domain.model.ReportCard
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.model.toPersianDigits
import com.example.presentation.components.NezamYarHeader
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.OnPositiveGreenContainer
import com.example.ui.theme.OnSchoolBlueContainer
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.PositiveGreenContainer
import com.example.ui.theme.SchoolBlueContainer
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReportCardViewScreen(
    user: User?,
    mainViewModel: MainViewModel,
    onNotificationClick: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ReportCardManager.init(context)
        ParentAccessManager.init(context)
    }

    val allCards by ReportCardManager.reportCards.collectAsState()
    val allStudents by mainViewModel.students.collectAsState()
    val allUsers by mainViewModel.allUsers.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()
    val unreadNotifs = notifications.count { !it.read }

    val isStaff = user?.isCreator == true ||
            user?.role == UserRole.MANAGER ||
            user?.role == UserRole.VICE_PRINCIPAL ||
            user?.role == UserRole.COUNSELOR ||
            user?.role == UserRole.TEACHER

    val isParent = user?.role == UserRole.PARENT
    val isStudent = user?.role == UserRole.STUDENT

    // Active selected student for viewing/creating report card
    var activeStudent by remember(user, allStudents) {
        val initial = if (isStudent) {
            allStudents.firstOrNull { it.id == user?.id || it.studentCode == user?.studentCode }
        } else if (isParent) {
            allStudents.firstOrNull { s -> user?.childStudentIds?.contains(s.id) == true || s.studentCode == user?.studentCode }
        } else {
            null // Staff starts in list view!
        }
        mutableStateOf<Student?>(initial)
    }

    // Search query & Grade filter for staff student list
    var studentSearchQuery by remember { mutableStateOf("") }
    var selectedGradeFilter by remember { mutableStateOf("همه") }

    // Dual parent check for parent role
    val parentStudentCode = if (isParent && activeStudent != null) activeStudent!!.studentCode else ""
    var refreshKey by remember { mutableIntStateOf(0) }
    val isFatherIn = remember(parentStudentCode, allUsers, refreshKey) {
        ParentAccessManager.isFatherLoggedIn(context, parentStudentCode, allUsers)
    }
    val isMotherIn = remember(parentStudentCode, allUsers, refreshKey) {
        ParentAccessManager.isMotherLoggedIn(context, parentStudentCode, allUsers)
    }
    val bothParentsIn = !isParent || (isFatherIn && isMotherIn)

    // Current report card for active student
    val currentCard: ReportCard? = remember(allCards, activeStudent) {
        if (activeStudent == null) null
        else allCards.firstOrNull { it.studentCode == activeStudent!!.studentCode || it.nationalId == activeStudent!!.studentCode || it.studentId == activeStudent!!.id }
    }

    // Dialog & Alert States
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var zoomReportCardImage by remember { mutableStateOf<String?>(null) }

    // Report card image picker launcher for staff
    val reportCardImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
                    val base64 = "data:image/jpeg;base64," + android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
                    if (activeStudent != null) {
                        val updatedCard = (currentCard ?: ReportCard(
                            id = "rc_${System.currentTimeMillis()}",
                            studentId = activeStudent!!.id,
                            studentCode = activeStudent!!.studentCode,
                            studentFirstName = activeStudent!!.firstName,
                            studentLastName = activeStudent!!.lastName,
                            grade = activeStudent!!.grade,
                            className = activeStudent!!.className
                        )).copy(reportCardImageUrl = base64)
                        ReportCardManager.saveReportCard(updatedCard)
                        Toast.makeText(context, "تصویر کارنامه با موفقیت ثبت شد.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در بارگذاری تصویر: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // High performance asynchronous bitmap generation
    var cardBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingBitmap by remember { mutableStateOf(false) }

    LaunchedEffect(currentCard) {
        if (currentCard != null) {
            isGeneratingBitmap = true
            cardBitmap = withContext(Dispatchers.Default) {
                ReportCardExportHelper.generateReportCardBitmap(currentCard)
            }
            isGeneratingBitmap = false
        } else {
            cardBitmap = null
        }
    }

    // Filtered Students List for Staff View
    val filteredStudents = remember(allStudents, studentSearchQuery, selectedGradeFilter) {
        allStudents.filter { st ->
            val matchesQuery = studentSearchQuery.isBlank() ||
                    st.fullName.contains(studentSearchQuery, ignoreCase = true) ||
                    st.studentCode.contains(studentSearchQuery) ||
                    st.nationalId.contains(studentSearchQuery)

            val matchesGrade = com.example.presentation.behavior.matchesClassFilter(st, selectedGradeFilter)

            matchesQuery && matchesGrade
        }
    }

    // Main Layout Container
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("report_card_view_screen")
    ) {
        item {
            NezamYarHeader(
                user = user,
                unreadNotificationCount = unreadNotifs,
                onNotificationClick = onNotificationClick,
                onMenuClick = onMenuClick
            )
        }

        // Mode 1: Active Student Selected -> Show Dedicated Report Card View
        if (activeStudent != null) {
            val selectedSt = activeStudent!!

            // Top bar: Back to student list
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (isStaff) {
                                    activeStudent = null
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        if (isStaff) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت به فهرست",
                                tint = SchoolBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "بازگشت به فهرست دانش‌آموزان",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolBluePrimary
                                )
                            )
                        } else {
                            Text(
                                text = "کارنامه تحصیلی ${selectedSt.fullName}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = VazirmatnFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SchoolBlueContainer,
                        border = BorderStroke(0.5.dp, SchoolBluePrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${selectedSt.grade} - ${selectedSt.className}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = VazirmatnFontFamily,
                                color = OnSchoolBlueContainer,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Dual Parent Lock Check for Parent Role
            if (isParent && !bothParentsIn) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("report_card_parent_locked_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, WarningAmber.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(WarningAmberContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "قفل کارنامه",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "کارنامه موقتاً قفل است",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "الزام حضور هم‌زمان هر دو والد (پدر و مادر)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = WarningAmber
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "طبق مقررات سامانه، مشاهده کارنامه تنها زمانی برای والدین بازگشایی می‌شود که هم پدر و هم مادر وارد سامانه شده باشند.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isFatherIn) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                                contentDescription = null,
                                                tint = if (isFatherIn) PositiveGreen else WarningAmber,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("ورود پدر به سامانه:", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text(
                                            text = if (isFatherIn) "تأیید شد ✓" else "در انتظار ورود پدر ⏳",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFatherIn) PositiveGreen else WarningAmber
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isMotherIn) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                                contentDescription = null,
                                                tint = if (isMotherIn) PositiveGreen else WarningAmber,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("ورود مادر به سامانه:", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text(
                                            text = if (isMotherIn) "تأیید شد ✓" else "در انتظار ورود مادر ⏳",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMotherIn) PositiveGreen else WarningAmber
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedButton(
                                onClick = { refreshKey++ },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("بررسی مجدد وضعیت ورود همسر")
                            }
                        }
                    }
                }
            } else if (currentCard == null) {
                // Empty State: No report card registered for this student
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("report_card_empty_state_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(SchoolBlueContainer.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentLate,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "کارنامه‌ای برای ${selectedSt.fullName} ثبت نشده است",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "هنوز کارنامه‌ای برای این دانش‌آموز ثبت نشده است. می‌توانید تصویر کارنامه را آپلود کنید یا نمرات را ثبت نمایید.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { reportCardImagePicker.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("empty_state_upload_image_button")
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ثبت و بارگذاری تصویر/عکس کارنامه", fontWeight = FontWeight.Bold)
                                }

                                if (isStaff) {
                                    OutlinedButton(
                                        onClick = { showEditDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("empty_state_issue_card_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تنظیم نمرات دستی و ساخت کارنامه", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Report Card Exists!
                // Requirement: Image FIRST, then Download Options right below it!

                // 1. REPORT CARD IMAGE PREVIEW FIRST!
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("official_report_card_canvas_preview"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            if (!currentCard.reportCardImageUrl.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { zoomReportCardImage = currentCard.reportCardImageUrl }
                                ) {
                                    coil.compose.AsyncImage(
                                        model = currentCard.reportCardImageUrl,
                                        contentDescription = "تصویر کارنامه تحصیلی آپلود شده",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("لمس جهت بزرگنمایی و زوم", fontSize = 10.sp, color = Color.White, fontFamily = VazirmatnFontFamily)
                                        }
                                    }
                                }
                            } else if (cardBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    Image(
                                        bitmap = cardBitmap!!.asImageBitmap(),
                                        contentDescription = "تصویر کارنامه تحصیلی",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = SchoolBluePrimary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "در حال ایجاد خروجی باکیفیت کارنامه...",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = VazirmatnFontFamily,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. DOWNLOAD & EXPORT OPTIONS RIGHT BELOW IMAGE!
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = SchoolBlueContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, SchoolBluePrimary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    val cardTitle = if (currentCard.cardType == "MONTHLY") "گزارش عملکرد ماهانه ${currentCard.selectedMonth}" else "کارنامه ترم/سالیانه"
                                    Text(
                                        text = "$cardTitle - ${currentCard.studentFullName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSchoolBlueContainer
                                    )
                                    val gpaStr = if (currentCard.cardType == "MONTHLY") currentCard.monthlyStudentGpa else currentCard.gpaAnnual
                                    Text(
                                        text = "معدل کل: ${gpaStr.toPersianDigits()} | پایه ${currentCard.grade} ${currentCard.className}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSchoolBlueContainer.copy(alpha = 0.85f)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val file = ReportCardExportHelper.exportReportCardAsPdf(context, currentCard)
                                            ?: ReportCardExportHelper.exportReportCardAsJpg(context, currentCard)
                                        if (file != null) {
                                            val mime = if (file.extension == "pdf") "application/pdf" else "image/jpeg"
                                            ReportCardExportHelper.shareFile(context, file, mime, currentCard.studentFullName)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری", tint = SchoolBluePrimary)
                                }
                            }

                            // Action Download Buttons (JPG & PDF)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val file = ReportCardExportHelper.exportReportCardAsJpg(context, currentCard)
                                        if (file != null) {
                                            Toast.makeText(context, "تصویر کارنامه (JPG) با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                                            ReportCardExportHelper.openFile(context, file, "image/jpeg")
                                        } else {
                                            Toast.makeText(context, "خطا در دریافت JPG", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("download_report_card_jpg_button")
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("دانلود به صورت تصویر (JPG)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val file = ReportCardExportHelper.exportReportCardAsPdf(context, currentCard)
                                        if (file != null) {
                                            Toast.makeText(context, "فایل چاپی کارنامه (PDF) ذخیره شد", Toast.LENGTH_SHORT).show()
                                            ReportCardExportHelper.openFile(context, file, "application/pdf")
                                        } else {
                                            Toast.makeText(context, "خطا در دریافت PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("download_report_card_pdf_button")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("دانلود به صورت پی دی اف (PDF)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 3. IMAGE UPLOAD & MANAGEMENT ACTIONS
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { reportCardImagePicker.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_report_card_image_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentCard.reportCardImageUrl.isNullOrBlank()) "ثبت و بارگذاری تصویر/عکس کارنامه" else "تغییر / ثبت مجدد تصویر کارنامه",
                                fontWeight = FontWeight.Bold,
                                fontFamily = VazirmatnFontFamily
                            )
                        }

                        if (isStaff) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showEditDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("edit_report_card_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ویرایش کارنامه")
                                }

                                Button(
                                    onClick = { showDeleteConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("delete_report_card_button")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حذف به کل کارنامه", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Official Notice
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = PositiveGreenContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PositiveGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "این کارنامه طبق استاندارد رسمی صادر گردیده و نسخه خروجی تصویر (JPG) و پی‌دی‌اف (PDF) کاملاً معتبر و قابل چاپ می‌باشد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnPositiveGreenContainer,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Mode 2: Staff Student List View (Clean Contacts-Style Layout!)


            // Search Bar & Grade Filters
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = studentSearchQuery,
                            onValueChange = { studentSearchQuery = it },
                            placeholder = {
                                Text(
                                    text = "جستجوی نام یا کد دانش‌آموز...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily)
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (studentSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { studentSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Grade & Class Chips
                        val gradeChips = listOf("همه", "هفتم ۱", "هفتم ۲", "هشتم ۱", "هشتم ۲", "نهم ۱", "نهم ۲")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(gradeChips) { grade ->
                                FilterChip(
                                    selected = selectedGradeFilter == grade,
                                    onClick = { selectedGradeFilter = grade },
                                    label = { Text(grade) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "فهرست دانش‌آموزان",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${filteredStudents.size.toPersianDigits()} نفر",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredStudents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "دانش‌آموزی با این مشخصات یافت نشد.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Concise Student Card List (Contacts-like style with Grade & Class attached together)
                items(filteredStudents, key = { it.id }) { st ->
                    val hasCard = ReportCardManager.hasReportCard(st.studentCode, st.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { activeStudent = st }
                            .testTag("report_card_student_item_${st.studentCode}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SchoolBlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SchoolBluePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = st.fullName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "کد: ${st.studentCode.toPersianDigits()}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Text("•", color = MaterialTheme.colorScheme.outline)
                                    // Grade and Class Attached Together
                                    Text(
                                        text = "${st.grade} - ${st.className}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = VazirmatnFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            color = SchoolBluePrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Has Report Card Status Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (hasCard) PositiveGreenContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(0.5.dp, if (hasCard) PositiveGreen else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = if (hasCard) "دارای کارنامه ✓" else "بدون کارنامه",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VazirmatnFontFamily,
                                        color = if (hasCard) OnPositiveGreenContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (hasCard) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "مشاهده کارنامه",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Edit / Issue Dialog
    if (showEditDialog) {
        val defaultPreFillCard = activeStudent?.let { st ->
            currentCard ?: ReportCard(
                id = "rc_${st.studentCode}_${System.currentTimeMillis()}",
                studentId = st.id,
                studentCode = st.studentCode,
                nationalId = st.nationalId.ifBlank { st.studentCode },
                studentFirstName = st.firstName,
                studentLastName = st.lastName,
                grade = st.grade,
                className = st.className,
                academicYear = "۱۴۰۵-۱۴۰۶"
            )
        } ?: currentCard

        ReportCardEditDialog(
            initialCard = defaultPreFillCard,
            students = allStudents,
            mainViewModel = mainViewModel,
            onDismiss = { showEditDialog = false },
            onSaved = { savedCard ->
                showEditDialog = false
                val matchingSt = allStudents.firstOrNull { it.studentCode == savedCard.studentCode || it.id == savedCard.studentId }
                if (matchingSt != null) {
                    activeStudent = matchingSt
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && currentCard != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("حذف کامل کارنامه", fontWeight = FontWeight.Bold) },
            text = {
                Text("آیا از حذف کامل کارنامه ${currentCard.studentFullName} اطمینان دارید؟ این عمل غیرقابل بازگشت خواهد بود.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cardId = currentCard.id
                        val success = ReportCardManager.deleteReportCard(cardId)
                        if (success) {
                            Toast.makeText(context, "کارنامه با موفقیت به کل حذف شد", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "خطا در حذف کارنامه", Toast.LENGTH_SHORT).show()
                        }
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("بله، حذف شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Zoomable Image Viewer Dialog
    if (!zoomReportCardImage.isNullOrBlank()) {
        com.example.presentation.components.ZoomableImageViewerDialog(
            imageUrlOrBase64 = zoomReportCardImage,
            onDismiss = { zoomReportCardImage = null }
        )
    }
}
