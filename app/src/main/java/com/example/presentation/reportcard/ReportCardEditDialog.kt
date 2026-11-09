package com.example.presentation.reportcard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.reportcard.ReportCardManager
import com.example.domain.model.AppNotification
import com.example.domain.model.RecordType
import com.example.domain.model.ReportCard
import com.example.domain.model.Student
import com.example.domain.model.SubjectGrade
import com.example.domain.model.toPersianDigits
import com.example.presentation.viewmodel.MainViewModel
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SchoolBluePrimary
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportCardEditDialog(
    initialCard: ReportCard?,
    students: List<Student>,
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSaved: (ReportCard) -> Unit
) {
    val context = LocalContext.current

    var reportCardImageUrl by remember { mutableStateOf(initialCard?.reportCardImageUrl) }

    val dialogImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
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
                    reportCardImageUrl = base64
                    Toast.makeText(context, "تصویر کارنامه با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در بارگذاری تصویر: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Card Type State: "TERM" or "MONTHLY"
    var cardType by remember { mutableStateOf(initialCard?.cardType ?: "MONTHLY") }
    var selectedMonth by remember { mutableStateOf(initialCard?.selectedMonth ?: "مهر") }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    val monthOptions = listOf("مهر", "آبان", "آذر", "دی", "بهمن", "اسفند", "فروردین", "اردیبهشت", "خرداد")

    // Active selected student
    val selectedStudent = remember(initialCard, students) {
        students.firstOrNull { it.studentCode == initialCard?.studentCode || it.id == initialCard?.studentId }
            ?: students.firstOrNull()
    }

    var studentFirstName by remember(selectedStudent) {
        mutableStateOf(initialCard?.studentFirstName ?: selectedStudent?.firstName.orEmpty())
    }
    var studentLastName by remember(selectedStudent) {
        mutableStateOf(initialCard?.studentLastName ?: selectedStudent?.lastName.orEmpty())
    }
    var fatherName by remember(selectedStudent) {
        mutableStateOf(initialCard?.fatherName ?: selectedStudent?.parentName.orEmpty().ifBlank { "سیدمحمد" })
    }
    var nationalId by remember(selectedStudent) {
        mutableStateOf(initialCard?.nationalId ?: selectedStudent?.nationalId.orEmpty().ifBlank { selectedStudent?.studentCode.orEmpty() })
    }

    // Student Code Autofill Logic:
    // If logged in, use actual student code; if not logged in, use default student code.
    var studentCode by remember(selectedStudent) {
        val code = if (selectedStudent?.hasLoggedIn == true || selectedStudent?.isRegistered == true || selectedStudent?.studentCode.orEmpty().isNotBlank()) {
            selectedStudent?.studentCode.orEmpty()
        } else {
            selectedStudent?.studentCode.orEmpty().ifBlank { "3570243427" }
        }
        mutableStateOf(initialCard?.studentCode ?: code)
    }

    // Grade & Class Pickers
    var grade by remember(selectedStudent) {
        mutableStateOf(initialCard?.grade ?: selectedStudent?.grade.orEmpty().ifBlank { "هفتم" })
    }
    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    val gradeOptions = listOf("هفتم", "هشتم", "نهم")

    val initialClassRaw = initialCard?.className ?: selectedStudent?.className.orEmpty()
    val formattedClassInitial = when {
        initialClassRaw.contains("7/1") || initialClassRaw.contains("هفتم ۱") || initialClassRaw.contains("هفتم 1") -> "کلاس 7/1"
        initialClassRaw.contains("7/2") || initialClassRaw.contains("هفتم ۲") || initialClassRaw.contains("هفتم 2") -> "کلاس 7/2"
        initialClassRaw.contains("8/1") || initialClassRaw.contains("هشتم ۱") || initialClassRaw.contains("هشتم 1") -> "کلاس 8/1"
        initialClassRaw.contains("8/2") || initialClassRaw.contains("هشتم ۲") || initialClassRaw.contains("هشتم 2") -> "کلاس 8/2"
        initialClassRaw.contains("9/1") || initialClassRaw.contains("نهم ۱") || initialClassRaw.contains("نهم 1") -> "کلاس 9/1"
        initialClassRaw.contains("9/2") || initialClassRaw.contains("نهم ۲") || initialClassRaw.contains("نهم 2") -> "کلاس 9/2"
        initialClassRaw.isNotBlank() -> if (initialClassRaw.startsWith("کلاس")) initialClassRaw else "کلاس $initialClassRaw"
        else -> "کلاس 7/1"
    }

    var className by remember(selectedStudent) {
        mutableStateOf(formattedClassInitial)
    }
    var classDropdownExpanded by remember { mutableStateOf(false) }
    val classOptions = listOf("کلاس 7/1", "کلاس 7/2", "کلاس 8/1", "کلاس 8/2", "کلاس 9/1", "کلاس 9/2")

    var schoolName by remember { mutableStateOf(initialCard?.schoolName ?: "دبیرستان شهید رجایی") }
    var schoolCode by remember { mutableStateOf(initialCard?.schoolCode ?: "۹۵۰۹۶۸۷۳") }
    var province by remember { mutableStateOf(initialCard?.province ?: "بوشهر") }
    var region by remember { mutableStateOf(initialCard?.region ?: "دیر") }
    var academicYear by remember { mutableStateOf(initialCard?.academicYear ?: "۱۴۰۵-۱۴۰۶") } // Default 1405-1406
    var issueDate by remember { mutableStateOf(initialCard?.issueDate ?: "۱۴۰۵/۰۷/۳۰") }
    var birthDate by remember { mutableStateOf(initialCard?.birthDate ?: "۱۳۹۱/۱۲/۱۶") }
    var birthPlace by remember { mutableStateOf(initialCard?.birthPlace ?: "کنگان") }

    var principalName by remember { mutableStateOf(initialCard?.principalName ?: "مهدی فاریابی") }
    var executiveDeputyName by remember { mutableStateOf(initialCard?.executiveDeputyName ?: "یاسین دراهکی") }
    var conductTerm1 by remember { mutableStateOf(initialCard?.conductTerm1 ?: "عالی") }
    var conductTerm2 by remember { mutableStateOf(initialCard?.conductTerm2 ?: "عالی") }
    var finalStatus by remember { mutableStateOf(initialCard?.finalStatus ?: "قبول خرداد") }

    // Monthly Metrics
    var highestClassGpa by remember { mutableStateOf(initialCard?.highestClassGpa ?: "۱۹.۲۵") }
    var monthlyStudentGpa by remember { mutableStateOf(initialCard?.monthlyStudentGpa ?: "۱۹.۰۰") }
    var lowestClassGpa by remember { mutableStateOf(initialCard?.lowestClassGpa ?: "۱۳.۰۸") }
    var overallClassRank by remember { mutableStateOf(initialCard?.overallClassRank ?: "۲") }

    // Subjects
    var subjectsList by remember(cardType) {
        val defaultList = if (cardType == "MONTHLY") ReportCard.defaultMonthlySubjectsList() else ReportCard.defaultSubjectsList()
        mutableStateOf(initialCard?.subjects?.ifEmpty { defaultList } ?: defaultList)
    }

    var totalTerm1 by remember { mutableStateOf(initialCard?.totalTerm1 ?: "۲۸۰") }
    var totalAnnual by remember { mutableStateOf(initialCard?.totalAnnual ?: "۵۶۰") }
    var gpaTerm1 by remember { mutableStateOf(initialCard?.gpaTerm1 ?: "۲۰") }
    var gpaAnnual by remember { mutableStateOf(initialCard?.gpaAnnual ?: "۲۰") }
    var gpaInWords by remember { mutableStateOf(initialCard?.gpaInWords ?: "بیست") }

    fun recalculateAverages(currentSubjects: List<SubjectGrade>) {
        if (cardType == "MONTHLY") {
            var sum = 0f
            var count = 0
            currentSubjects.forEach { s ->
                val sc = s.monthlyMyScore.toFloatOrNull() ?: 0f
                if (sc > 0f) {
                    sum += sc
                    count++
                }
            }
            if (count > 0) {
                val avg = sum / count
                monthlyStudentGpa = String.format("%.2f", avg).toPersianDigits()
            }
        } else {
            var sumT1 = 0f
            var countT1 = 0
            var sumAnnual = 0f
            var countAnnual = 0

            currentSubjects.forEach { s ->
                val t1 = s.term1Score.toFloatOrNull() ?: s.term1Final.toFloatOrNull() ?: 0f
                if (t1 > 0f) {
                    sumT1 += t1
                    countT1++
                }
                val ann = s.annualScore.toFloatOrNull() ?: 0f
                if (ann > 0f) {
                    sumAnnual += ann
                    countAnnual++
                }
            }

            if (countT1 > 0) {
                val avgT1 = sumT1 / countT1
                gpaTerm1 = String.format("%.2f", avgT1).toPersianDigits()
                totalTerm1 = sumT1.toInt().toString().toPersianDigits()
            }
            if (countAnnual > 0) {
                val avgAnn = sumAnnual / countAnnual
                gpaAnnual = String.format("%.2f", avgAnn).toPersianDigits()
                totalAnnual = sumAnnual.toInt().toString().toPersianDigits()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("report_card_edit_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SchoolBluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (initialCard != null) "ویرایش کارنامه دانش‌آموز" else "صدور کارنامه جدید",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تنظیم و ثبت رسمی نمرات در سامانه",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Image Upload Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
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
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = SchoolBluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "تصویر و عکس کارنامه (آپلود مستقیم):",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (!reportCardImageUrl.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Black.copy(alpha = 0.05f))
                                    ) {
                                        coil.compose.AsyncImage(
                                            model = reportCardImageUrl,
                                            contentDescription = "پیش‌نمایش عکس کارنامه",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                        IconButton(
                                            onClick = { reportCardImageUrl = null },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .background(Color.Red.copy(alpha = 0.85f), CircleShape)
                                                .size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "حذف عکس", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Button(
                                    onClick = { dialogImagePicker.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (reportCardImageUrl.isNullOrBlank()) "انتخاب و آپلود تصویر کارنامه از دستگاه" else "تغییر و آپلود تصویر جدید",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 1. Report Card Type Selector (ماهانه vs ترم/سالیانه)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "نوع کارنامه و دوره امتحانی:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = cardType == "MONTHLY",
                                        onClick = { cardType = "MONTHLY" },
                                        label = { Text("گزارش عملکرد تحصیلی ماهانه", fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = cardType == "TERM",
                                        onClick = { cardType = "TERM" },
                                        label = { Text("کارنامه ترم / سالیانه", fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (cardType == "MONTHLY") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ExposedDropdownMenuBox(
                                        expanded = monthDropdownExpanded,
                                        onExpandedChange = { monthDropdownExpanded = !monthDropdownExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = "انتخاب ماه: $selectedMonth",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("ماه عملکرد تحصیلی") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = monthDropdownExpanded,
                                            onDismissRequest = { monthDropdownExpanded = false }
                                        ) {
                                            monthOptions.forEach { m ->
                                                DropdownMenuItem(
                                                    text = { Text("ماه $m") },
                                                    onClick = {
                                                        selectedMonth = m
                                                        monthDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Student Search & Selector
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "مشخصات دانش‌آموز: $studentFirstName $studentLastName",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolBluePrimary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = studentFirstName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("نام (ثابت)") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = studentLastName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("نام خانوادگی (ثابت)") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = studentCode,
                                        onValueChange = { studentCode = it },
                                        label = { Text("کد دانش‌آموزی") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = fatherName,
                                        onValueChange = { fatherName = it },
                                        label = { Text("نام پدر") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Selectable Grade Dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = gradeDropdownExpanded,
                                        onExpandedChange = { gradeDropdownExpanded = !gradeDropdownExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = grade,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("پایه تحصیلی") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeDropdownExpanded) },
                                            modifier = Modifier.menuAnchor(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = gradeDropdownExpanded,
                                            onDismissRequest = { gradeDropdownExpanded = false }
                                        ) {
                                            gradeOptions.forEach { g ->
                                                DropdownMenuItem(
                                                    text = { Text(g) },
                                                    onClick = {
                                                        grade = g
                                                        gradeDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Selectable Class Dropdown (6 choices)
                                    ExposedDropdownMenuBox(
                                        expanded = classDropdownExpanded,
                                        onExpandedChange = { classDropdownExpanded = !classDropdownExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = className,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("کلاس") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                                            modifier = Modifier.menuAnchor(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = classDropdownExpanded,
                                            onDismissRequest = { classDropdownExpanded = false }
                                        ) {
                                            classOptions.forEach { c ->
                                                DropdownMenuItem(
                                                    text = { Text(c) },
                                                    onClick = {
                                                        className = c
                                                        classDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = academicYear,
                                        onValueChange = { academicYear = it },
                                        label = { Text("سال تحصیلی") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = issueDate,
                                        onValueChange = { issueDate = it },
                                        label = { Text("تاریخ صدور") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Quick Actions Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ورود نمرات دروس:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        subjectsList = subjectsList.map { s ->
                                            if (cardType == "MONTHLY") {
                                                s.copy(monthlyMyScore = "۲۰", monthlyClassAvg = "۱۸.۵۰", monthlyClassRank = "۱")
                                            } else {
                                                s.copy(
                                                    term1Continuous = if (s.isContinuousTerm1Hatched) "" else "۲۰",
                                                    term1Final = "۲۰",
                                                    term1Score = "۲۰",
                                                    term2Continuous = if (s.isContinuousTerm2Hatched) "" else "۲۰",
                                                    term2Final = "۲۰",
                                                    term2ScoreWithCoefficient = "۴۰",
                                                    annualScore = "۲۰"
                                                )
                                            }
                                        }
                                        recalculateAverages(subjectsList)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تکمیل با ۲۰", style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = { recalculateAverages(subjectsList) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("محاسبه معدل", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // 4. Subject Grades List
                    itemsIndexed(subjectsList) { index, subject ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${subject.rowNumber}. ${subject.subjectName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolBluePrimary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                if (cardType == "MONTHLY") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = subject.monthlyMyScore,
                                            onValueChange = { newVal ->
                                                val updated = subjectsList.toMutableList()
                                                updated[index] = subject.copy(monthlyMyScore = newVal)
                                                subjectsList = updated
                                                recalculateAverages(updated)
                                            },
                                            label = { Text("نمره شما") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        OutlinedTextField(
                                            value = subject.monthlyClassAvg,
                                            onValueChange = { newVal ->
                                                val updated = subjectsList.toMutableList()
                                                updated[index] = subject.copy(monthlyClassAvg = newVal)
                                                subjectsList = updated
                                            },
                                            label = { Text("میانگین کلاس") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        OutlinedTextField(
                                            value = subject.monthlyClassRank,
                                            onValueChange = { newVal ->
                                                val updated = subjectsList.toMutableList()
                                                updated[index] = subject.copy(monthlyClassRank = newVal)
                                                subjectsList = updated
                                            },
                                            label = { Text("رتبه در کلاس") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                } else {
                                    // Term mode fields
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (!subject.isContinuousTerm1Hatched) {
                                            OutlinedTextField(
                                                value = subject.term1Continuous,
                                                onValueChange = { newVal ->
                                                    val updated = subjectsList.toMutableList()
                                                    updated[index] = subject.copy(term1Continuous = newVal)
                                                    subjectsList = updated
                                                    recalculateAverages(updated)
                                                },
                                                label = { Text("تکوینی نوبت۱") },
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        OutlinedTextField(
                                            value = subject.term1Final,
                                            onValueChange = { newVal ->
                                                val updated = subjectsList.toMutableList()
                                                updated[index] = subject.copy(term1Final = newVal)
                                                subjectsList = updated
                                                recalculateAverages(updated)
                                            },
                                            label = { Text("پایانی نوبت۱") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        if (!subject.isContinuousTerm2Hatched) {
                                            OutlinedTextField(
                                                value = subject.term2Continuous,
                                                onValueChange = { newVal ->
                                                    val updated = subjectsList.toMutableList()
                                                    updated[index] = subject.copy(term2Continuous = newVal)
                                                    subjectsList = updated
                                                    recalculateAverages(updated)
                                                },
                                                label = { Text("تکوینی نوبت۲") },
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        OutlinedTextField(
                                            value = subject.term2Final,
                                            onValueChange = { newVal ->
                                                val updated = subjectsList.toMutableList()
                                                updated[index] = subject.copy(term2Final = newVal)
                                                subjectsList = updated
                                                recalculateAverages(updated)
                                            },
                                            label = { Text("پایانی نوبت۲") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        OutlinedTextField(
                                            value = subject.annualScore,
                                            onValueChange = { newVal ->
                                                val updated = subjectsList.toMutableList()
                                                updated[index] = subject.copy(annualScore = newVal)
                                                subjectsList = updated
                                                recalculateAverages(updated)
                                            },
                                            label = { Text("سالیانه") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Summary & Signatures Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "خلاصه وضعیت و مسئولین مدرسه:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                if (cardType == "MONTHLY") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = monthlyStudentGpa,
                                            onValueChange = { monthlyStudentGpa = it },
                                            label = { Text("معدل شما") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        OutlinedTextField(
                                            value = highestClassGpa,
                                            onValueChange = { highestClassGpa = it },
                                            label = { Text("بالاترین معدل") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = lowestClassGpa,
                                            onValueChange = { lowestClassGpa = it },
                                            label = { Text("پایین‌ترین معدل") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        OutlinedTextField(
                                            value = overallClassRank,
                                            onValueChange = { overallClassRank = it },
                                            label = { Text("رتبه در کلاس") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = gpaTerm1,
                                            onValueChange = { gpaTerm1 = it },
                                            label = { Text("معدل نوبت اول") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        OutlinedTextField(
                                            value = gpaAnnual,
                                            onValueChange = { gpaAnnual = it },
                                            label = { Text("معدل سالیانه") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = gpaInWords,
                                            onValueChange = { gpaInWords = it },
                                            label = { Text("معدل به حروف") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        OutlinedTextField(
                                            value = conductTerm2,
                                            onValueChange = { conductTerm2 = it },
                                            label = { Text("انضباط") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = principalName,
                                        onValueChange = { principalName = it },
                                        label = { Text("مدیر آموزشگاه") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = executiveDeputyName,
                                        onValueChange = { executiveDeputyName = it },
                                        label = { Text("معاون اجرایی") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Dialog Save Action Button
                Button(
                    onClick = {
                        val cardToSave = ReportCard(
                            id = initialCard?.id ?: "report_card_${studentCode}_${System.currentTimeMillis()}",
                            studentId = selectedStudent?.id ?: initialCard?.studentId ?: "",
                            studentCode = studentCode,
                            nationalId = nationalId,
                            studentFirstName = studentFirstName,
                            studentLastName = studentLastName,
                            fatherName = fatherName,
                            grade = grade,
                            className = className,
                            cardType = cardType,
                            selectedMonth = selectedMonth,
                            schoolName = schoolName,
                            schoolCode = schoolCode,
                            province = province,
                            region = region,
                            academicYear = academicYear,
                            issueDate = issueDate,
                            principalName = principalName,
                            executiveDeputyName = executiveDeputyName,
                            conductTerm1 = conductTerm1,
                            conductTerm2 = conductTerm2,
                            finalStatus = finalStatus,
                            subjects = subjectsList,
                            totalTerm1 = totalTerm1,
                            totalAnnual = totalAnnual,
                            gpaTerm1 = gpaTerm1,
                            gpaAnnual = gpaAnnual,
                            gpaInWords = gpaInWords,
                            highestClassGpa = highestClassGpa,
                            monthlyStudentGpa = monthlyStudentGpa,
                            lowestClassGpa = lowestClassGpa,
                            overallClassRank = overallClassRank,
                            reportCardImageUrl = reportCardImageUrl
                        )

                        val success = ReportCardManager.saveReportCard(cardToSave)
                        if (success) {
                            Toast.makeText(context, "کارنامه با موفقیت در سامانه ثبت شد", Toast.LENGTH_SHORT).show()

                            // Send instant notification to student & parents
                            val title = if (cardType == "MONTHLY") "صدور گزارش ماهانه $selectedMonth" else "صدور کارنامه تحصیلی"
                            mainViewModel.addNotification(
                                AppNotification(
                                    id = UUID.randomUUID().toString(),
                                    schoolId = "school_1",
                                    recipientUserId = selectedStudent?.id ?: "",
                                    title = title,
                                    body = "کارنامه رسمی $studentFirstName $studentLastName برای سال تحصیلی $academicYear صادر شد.",
                                    type = RecordType.ACHIEVEMENT,
                                    studentId = selectedStudent?.id ?: "",
                                    studentName = "$studentFirstName $studentLastName",
                                    recordId = cardToSave.id,
                                    date = issueDate,
                                    time = "12:00"
                                )
                            )

                            onSaved(cardToSave)
                        } else {
                            Toast.makeText(context, "خطا در ذخیره‌سازی کارنامه", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_report_card_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBluePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ثبت و صدور نهایی کارنامه", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
