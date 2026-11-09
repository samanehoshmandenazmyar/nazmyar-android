package com.example.core.reportcard

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.domain.model.ReportCard
import com.example.domain.model.toPersianDigits
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ReportCardExportHelper {

    private const val CANVAS_WIDTH = 1400
    private const val CANVAS_HEIGHT = 1950

    /**
     * Draws the complete report card (Term or Monthly) on a Canvas.
     */
    fun drawReportCard(canvas: Canvas, card: ReportCard) {
        if (card.cardType == "MONTHLY") {
            drawMonthlyReportCard(canvas, card)
        } else {
            drawTermReportCard(canvas, card)
        }
    }

    /**
     * Draws the Monthly Report Card (گزارش عملکرد تحصیلی ماهانه) matching image_2d0a5380.png.
     */
    private fun drawMonthlyReportCard(canvas: Canvas, card: ReportCard) {
        val width = CANVAS_WIDTH
        val height = CANVAS_HEIGHT

        // Background
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Line paints
        val borderThick = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        val borderThin = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val centerTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 19f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val centerBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val smallCenterPaint = Paint().apply {
            color = Color.BLACK
            textSize = 15f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val margin = 35f
        canvas.drawRect(margin, margin, width - margin, height - margin, borderThick)
        canvas.drawRect(margin + 4f, margin + 4f, width - margin - 4f, height - margin - 4f, borderThin)

        // 1. Header Box (y: 45 to 210)
        val headerTop = 45f
        val headerBottom = 210f
        canvas.drawLine(margin, headerBottom, width - margin, headerBottom, borderThick)

        // Top Left Photo Box
        val photoWidth = 180f
        canvas.drawRect(margin + 5f, headerTop + 5f, margin + photoWidth, headerBottom - 5f, borderThin)
        smallCenterPaint.textSize = 17f
        canvas.drawText("محل الصاق عکس", margin + (photoWidth / 2f), headerTop + 90f, smallCenterPaint)

        // Center Title Header
        val centerCenterX = margin + photoWidth + ((width - 2 * margin - photoWidth) / 2f)
        centerBoldPaint.textSize = 22f
        canvas.drawText("بسمه تعالی", centerCenterX, headerTop + 35f, centerBoldPaint)
        centerBoldPaint.textSize = 34f
        canvas.drawText("گزارش عملکرد تحصيلي ماهانه", centerCenterX, headerTop + 90f, centerBoldPaint)

        centerTextPaint.textSize = 21f
        val schoolLine = "دبیرستان: ${card.schoolName}    منطقه/شهرستان: ${card.region}    سالتحصیلی: ${card.academicYear.toPersianDigits()}"
        canvas.drawText(schoolLine, centerCenterX, headerTop + 138f, centerTextPaint)

        // 2. Student & Greeting Box (y: 210 to 350)
        val infoBottom = 350f
        canvas.drawLine(margin, infoBottom, width - margin, infoBottom, borderThick)

        textPaint.textSize = 20f
        val studentInfoLine = "ولی محترم دانش آموز: ${card.studentFullName}    دانش آموز پایه: ${card.grade} ${card.className}    رشته: عمومی    تاریخ: ${card.issueDate.toPersianDigits()}"
        canvas.drawText(studentInfoLine, width - margin - 20f, 245f, textPaint)

        val greetingText = "احتراما بدینوسیله عملکرد تحصیلی ماهانه فرزند شما به شرح ذیل اعلام میگردد. لطفا پس از رویت و درج نظرات ارزشمندتان، آن را امضا فرمائید."
        canvas.drawText(greetingText, width - margin - 20f, 285f, textPaint)

        val managerLine = "مدیر آموزشگاه : ${card.principalName}"
        canvas.drawText(managerLine, width - margin - 20f, 325f, textPaint)

        // 3. Main Monthly Table Layout (y: 360 to 1700)
        val tableTop = 360f
        val tableBottom = 1700f
        val tableLeft = 50f
        val tableRight = width - 50f
        val tableWidth = tableRight - tableLeft // 1300f

        val colRadifWidth = 50f
        val colNameWidth = 220f
        val monthsAreaWidth = tableWidth - colRadifWidth - colNameWidth // 1030f

        val months = listOf("مهر", "آبان", "آذر", "بهمن", "اسفند", "فروردین", "اردیبهشت")
        val monthWidth = monthsAreaWidth / months.size // ~147.14f
        val subColWidth = monthWidth / 3f // ~49f

        val headerRow1Height = 55f
        val headerRow2Height = 90f
        val headerTotalHeight = headerRow1Height + headerRow2Height // 145f

        val tableHeaderBottom = tableTop + headerTotalHeight // 505f

        // Draw Table Outer Border
        canvas.drawRect(tableLeft, tableTop, tableRight, tableBottom, borderThick)
        // Horizontal Header line 1 (Separates Months headers from Sub-headers)
        canvas.drawLine(tableLeft, tableTop + headerRow1Height, tableRight - colRadifWidth - colNameWidth, tableTop + headerRow1Height, borderThin)
        // Horizontal Header line 2 (Separates Table Header from Rows)
        canvas.drawLine(tableLeft, tableHeaderBottom, tableRight, tableHeaderBottom, borderThick)

        // Draw Column Vertical Lines & Titles
        // Radif Column (Rightmost)
        val xRadifLine = tableRight - colRadifWidth
        canvas.drawLine(xRadifLine, tableTop, xRadifLine, tableBottom, borderThin)
        smallCenterPaint.textSize = 17f
        smallCenterPaint.isFakeBoldText = true
        canvas.drawText("ردیف", tableRight - (colRadifWidth / 2f), tableTop + 85f, smallCenterPaint)

        // Subject Name Column
        val xNameLine = xRadifLine - colNameWidth
        canvas.drawLine(xNameLine, tableTop, xNameLine, tableBottom, borderThin)
        centerBoldPaint.textSize = 21f
        canvas.drawText("نام درس", xRadifLine - (colNameWidth / 2f), tableTop + 85f, centerBoldPaint)

        // Months Columns
        for (mIndex in months.indices) {
            val monthName = months[mIndex]
            val xMonthRight = xNameLine - (mIndex * monthWidth)
            val xMonthLeft = xMonthRight - monthWidth

            // Vertical Month Divider
            if (mIndex > 0) {
                canvas.drawLine(xMonthRight, tableTop, xMonthRight, tableBottom, borderThick)
            }

            // Month Title (Header Row 1)
            centerBoldPaint.textSize = 21f
            canvas.drawText(monthName, xMonthRight - (monthWidth / 2f), tableTop + 36f, centerBoldPaint)

            // Sub-columns Vertical Dividers & Titles (Header Row 2)
            // Sub-col 1: میانگین نمره کلاس (Right sub-col in month)
            // Sub-col 2: نمره شما (Middle sub-col)
            // Sub-col 3: رتبه درسی شما در کلاس (Left sub-col)
            val xSub1Left = xMonthRight - subColWidth
            val xSub2Left = xSub1Left - subColWidth

            canvas.drawLine(xSub1Left, tableTop + headerRow1Height, xSub1Left, tableBottom, borderThin)
            canvas.drawLine(xSub2Left, tableTop + headerRow1Height, xSub2Left, tableBottom, borderThin)

            // Draw vertical/multiline column titles
            smallCenterPaint.textSize = 13f
            smallCenterPaint.isFakeBoldText = false

            // Sub 1 Title: میانگین نمره کلاس
            val c1X = xMonthRight - (subColWidth / 2f)
            canvas.drawText("میانگین", c1X, tableTop + headerRow1Height + 25f, smallCenterPaint)
            canvas.drawText("نمره", c1X, tableTop + headerRow1Height + 45f, smallCenterPaint)
            canvas.drawText("کلاس", c1X, tableTop + headerRow1Height + 65f, smallCenterPaint)

            // Sub 2 Title: نمره شما
            val c2X = xSub1Left - (subColWidth / 2f)
            smallCenterPaint.isFakeBoldText = true
            canvas.drawText("نمره", c2X, tableTop + headerRow1Height + 35f, smallCenterPaint)
            canvas.drawText("شما", c2X, tableTop + headerRow1Height + 58f, smallCenterPaint)

            // Sub 3 Title: رتبه درسی شما در کلاس
            val c3X = xSub2Left - (subColWidth / 2f)
            smallCenterPaint.isFakeBoldText = false
            canvas.drawText("رتبه", c3X, tableTop + headerRow1Height + 22f, smallCenterPaint)
            canvas.drawText("درسی", c3X, tableTop + headerRow1Height + 42f, smallCenterPaint)
            canvas.drawText("کلاس", c3X, tableTop + headerRow1Height + 62f, smallCenterPaint)
        }

        // Draw Subject Rows
        val subjectsList = if (card.subjects.isNotEmpty()) card.subjects else ReportCard.defaultMonthlySubjectsList()
        val numSubjects = subjectsList.size // 13
        val subjectRowHeight = 62f
        val subjectsTotalHeight = numSubjects * subjectRowHeight // ~806f

        val activeMonth = card.selectedMonth.trim()

        for (i in 0 until numSubjects) {
            val s = subjectsList[i]
            val yRowTop = tableHeaderBottom + (i * subjectRowHeight)
            val yRowBottom = yRowTop + subjectRowHeight
            val yText = yRowTop + 40f

            // Horizontal Row Line
            canvas.drawLine(tableLeft, yRowBottom, tableRight, yRowBottom, borderThin)

            // Row Number
            smallCenterPaint.textSize = 17f
            smallCenterPaint.isFakeBoldText = false
            canvas.drawText(s.rowNumber.toString().toPersianDigits(), tableRight - (colRadifWidth / 2f), yText, smallCenterPaint)

            // Subject Name
            textPaint.textSize = 19f
            textPaint.isFakeBoldText = true
            canvas.drawText(s.subjectName, xRadifLine - 15f, yText, textPaint)

            // Grades across months
            for (mIndex in months.indices) {
                val mName = months[mIndex]
                val xMonthRight = xNameLine - (mIndex * monthWidth)
                val c1X = xMonthRight - (subColWidth / 2f)
                val c2X = xMonthRight - subColWidth - (subColWidth / 2f)
                val c3X = xMonthRight - (2 * subColWidth) - (subColWidth / 2f)

                if (mName == activeMonth || (activeMonth.isBlank() && mIndex == 0)) {
                    // Active Month data!
                    centerTextPaint.textSize = 18f
                    centerTextPaint.isFakeBoldText = false
                    canvas.drawText(s.monthlyClassAvg.toPersianDigits(), c1X, yText, centerTextPaint)

                    centerTextPaint.textSize = 19f
                    centerTextPaint.isFakeBoldText = true
                    canvas.drawText(s.monthlyMyScore.toPersianDigits(), c2X, yText, centerTextPaint)

                    centerTextPaint.textSize = 18f
                    centerTextPaint.isFakeBoldText = false
                    canvas.drawText(s.monthlyClassRank.toPersianDigits(), c3X, yText, centerTextPaint)
                } else {
                    // Inactive months: draw dot or empty
                    smallCenterPaint.textSize = 18f
                    canvas.drawText(".", c1X, yText, smallCenterPaint)
                    canvas.drawText(".", c2X, yText, smallCenterPaint)
                    canvas.drawText(".", c3X, yText, smallCenterPaint)
                }
            }
        }

        // Summary Rows at Bottom of Table
        val ySummaryStart = tableHeaderBottom + subjectsTotalHeight
        val summaryRowHeight = 55f

        val summaryLabels = listOf(
            "بالاترین معدل کلاس",
            "معدل شما",
            "پایین ترین معدل کلاس",
            "رتبه کلی شما در کلاس"
        )

        for (sIdx in summaryLabels.indices) {
            val label = summaryLabels[sIdx]
            val ySumTop = ySummaryStart + (sIdx * summaryRowHeight)
            val ySumBottom = ySumTop + summaryRowHeight
            val ySumText = ySumTop + 36f

            canvas.drawLine(tableLeft, ySumBottom, tableRight, ySumBottom, borderThin)

            // Label Span (Right to Name Column)
            centerBoldPaint.textSize = 19f
            canvas.drawText(label, xRadifLine - (colNameWidth / 2f), ySumText, centerBoldPaint)

            for (mIndex in months.indices) {
                val mName = months[mIndex]
                val xMonthRight = xNameLine - (mIndex * monthWidth)
                val c2X = xMonthRight - subColWidth - (subColWidth / 2f)

                if (mName == activeMonth || (activeMonth.isBlank() && mIndex == 0)) {
                    val valStr = when (sIdx) {
                        0 -> card.highestClassGpa
                        1 -> card.monthlyStudentGpa.ifBlank { card.gpaTerm1 }
                        2 -> card.lowestClassGpa
                        3 -> card.overallClassRank
                        else -> ""
                    }
                    centerBoldPaint.textSize = 19f
                    canvas.drawText(valStr.toPersianDigits(), c2X, ySumText, centerBoldPaint)
                } else {
                    smallCenterPaint.textSize = 18f
                    canvas.drawText(".,..", c2X, ySumText, smallCenterPaint)
                }
            }
        }

        // 4. Footer Box
        val footerTop = tableBottom + 20f
        textPaint.textSize = 21f
        textPaint.isFakeBoldText = true
        canvas.drawText("نظرات ولی دانش آموز :", width - margin - 20f, footerTop + 45f, textPaint)

        canvas.drawText("امضاء", margin + 120f, footerTop + 140f, textPaint)
    }

    /**
     * Draws the Term Report Card (کارنامه رسمی ارزشیابی تحصیلی تربیتی) matching image_0387e2d8.png.
     */
    private fun drawTermReportCard(canvas: Canvas, card: ReportCard) {
        val width = CANVAS_WIDTH
        val height = CANVAS_HEIGHT

        // Background
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Line paints
        val borderThick = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        val borderThin = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 21f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val centerTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val centerBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 21f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val smallCenterPaint = Paint().apply {
            color = Color.BLACK
            textSize = 17f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val hatchPaint = Paint().apply {
            color = Color.rgb(235, 237, 240)
            style = Paint.Style.FILL
        }

        val hatchLinePaint = Paint().apply {
            color = Color.rgb(205, 210, 215)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        // 1. Outer Borders
        val margin = 30f
        canvas.drawRect(margin, margin, width - margin, height - margin, borderThick)
        canvas.drawRect(margin + 5f, margin + 5f, width - margin - 5f, height - margin - 5f, borderThin)

        // 2. Header Box (y: 40 to 265)
        val headerTop = 42f
        val headerBottom = 265f
        canvas.drawLine(margin, headerBottom, width - margin, headerBottom, borderThick)

        // Header Title Right
        centerBoldPaint.textSize = 22f
        canvas.drawText("جمهوری اسلامی ایران", 1215f, headerTop + 30f, centerBoldPaint)
        centerBoldPaint.textSize = 20f
        canvas.drawText("وزارت آموزش و پرورش", 1215f, headerTop + 60f, centerBoldPaint)

        // Header Middle Text
        centerBoldPaint.textSize = 23f
        canvas.drawText("اداره کل آموزش و پرورش استان ${card.province}", 700f, headerTop + 30f, centerBoldPaint)
        canvas.drawText("اداره آموزش و پرورش شهرستان/منطقه/ناحیه ${card.region}", 700f, headerTop + 65f, centerBoldPaint)
        centerBoldPaint.textSize = 25f
        canvas.drawText("کارنامه ارزشیابی تحصیلی تربیتی ${card.educationalPeriod}", 700f, headerTop + 105f, centerBoldPaint)
        centerTextPaint.textSize = 21f
        canvas.drawText("سال تحصیلی: ${card.academicYear.toPersianDigits()}    دوره امتحانی: ${card.semesterTitle}", 700f, headerTop + 145f, centerTextPaint)

        // Header Left Box (School Metadata)
        val leftHeaderX = 280f
        canvas.drawLine(leftHeaderX, headerTop, leftHeaderX, headerBottom, borderThin)
        textPaint.textSize = 18f
        canvas.drawText("آموزشگاه: ${card.schoolName}", leftHeaderX - 10f, headerTop + 25f, textPaint)
        canvas.drawText("کد آموزشگاه: ${card.schoolCode.toPersianDigits()}", leftHeaderX - 10f, headerTop + 55f, textPaint)
        canvas.drawText("نوع آموزشگاه: ${card.schoolType}", leftHeaderX - 10f, headerTop + 85f, textPaint)
        canvas.drawText("موقعیت: ${card.schoolLocation}  خصوصیت: ${card.schoolFeature}", leftHeaderX - 10f, headerTop + 115f, textPaint)

        // 3. Student Profile Header Row (y: 190 to 265)
        canvas.drawLine(margin, 190f, width - margin, 190f, borderThin)
        textPaint.textSize = 19f
        val studentLine1 = "نام و نام خانوادگی: ${card.studentFullName}   کد دانش‌آموزی: ${card.studentCode.toPersianDigits()}   کد ملی: ${card.nationalId.toPersianDigits()}   نام پدر: ${card.fatherName}"
        canvas.drawText(studentLine1, width - margin - 15f, 222f, textPaint)
        val studentLine2 = "پایه: ${card.grade}   کلاس: ${card.className}   جنسیت: ${card.gender}   تاریخ تولد: ${card.birthDate.toPersianDigits()}   محل تولد: ${card.birthPlace}"
        canvas.drawText(studentLine2, width - margin - 15f, 252f, textPaint)

        // 4. Main Grades Table Layout (y: 280 to 1480)
        val tableTop = 280f
        val tableBottom = 1530f
        val tableLeft = 45f
        val tableRight = width - 45f

        val colWidths = floatArrayOf(
            50f,  // 0: ردیف
            210f, // 1: نام درس
            65f,  // 2: نوبت اول - تکوینی
            65f,  // 3: نوبت اول - پایانی
            75f,  // 4: نوبت اول - نمره درس
            65f,  // 5: نوبت دوم - تکوینی
            65f,  // 6: نوبت دوم - پایانی
            80f,  // 7: نوبت دوم - با ضریب
            90f,  // 8: نمره سالیانه
            65f,  // 9: تابستان - پایانی
            65f,  // 10: تابستان - سالیانه
            110f, // 11: تعیین رشته
            100f, // 12: نتیجه
            105f  // 13: ملاحظات
        )

        // Calculate X coordinates
        val colX = FloatArray(colWidths.size + 1)
        var currentX = tableRight
        colX[0] = currentX
        for (i in colWidths.indices) {
            currentX -= colWidths[i]
            colX[i + 1] = currentX
        }

        val headerRow1Height = 45f
        val headerRow2Height = 65f
        val tableHeaderBottom = tableTop + headerRow1Height + headerRow2Height // 390f

        // Draw Table Outer Box
        canvas.drawRect(tableLeft, tableTop, tableRight, tableBottom, borderThick)
        canvas.drawLine(tableLeft, tableTop + headerRow1Height, tableRight, tableTop + headerRow1Height, borderThin)
        canvas.drawLine(tableLeft, tableHeaderBottom, tableRight, tableHeaderBottom, borderThick)

        // Vertical Column Dividers
        for (i in 1 until colX.size - 1) {
            canvas.drawLine(colX[i], tableTop, colX[i], tableBottom, borderThin)
        }

        // Table Column Titles
        smallCenterPaint.textSize = 16f
        smallCenterPaint.isFakeBoldText = true

        canvas.drawText("ردیف", (colX[0] + colX[1]) / 2f, tableTop + 65f, smallCenterPaint)
        canvas.drawText("نام درس", (colX[1] + colX[2]) / 2f, tableTop + 65f, smallCenterPaint)

        // Nobat 1
        canvas.drawText("نوبت اول", (colX[2] + colX[5]) / 2f, tableTop + 30f, smallCenterPaint)
        canvas.drawText("تکوینی", (colX[2] + colX[3]) / 2f, tableTop + 80f, smallCenterPaint)
        canvas.drawText("پایانی", (colX[3] + colX[4]) / 2f, tableTop + 80f, smallCenterPaint)
        canvas.drawText("نمره درس", (colX[4] + colX[5]) / 2f, tableTop + 80f, smallCenterPaint)

        // Nobat 2
        canvas.drawText("نوبت دوم (خرداد)", (colX[5] + colX[8]) / 2f, tableTop + 30f, smallCenterPaint)
        canvas.drawText("تکوینی", (colX[5] + colX[6]) / 2f, tableTop + 80f, smallCenterPaint)
        canvas.drawText("پایانی", (colX[6] + colX[7]) / 2f, tableTop + 80f, smallCenterPaint)
        canvas.drawText("ضریب", (colX[7] + colX[8]) / 2f, tableTop + 80f, smallCenterPaint)

        canvas.drawText("نمره سالیانه", (colX[8] + colX[9]) / 2f, tableTop + 65f, smallCenterPaint)

        // Summer
        canvas.drawText("تابستان", (colX[9] + colX[11]) / 2f, tableTop + 30f, smallCenterPaint)
        canvas.drawText("پایانی", (colX[9] + colX[10]) / 2f, tableTop + 80f, smallCenterPaint)
        canvas.drawText("سالیانه", (colX[10] + colX[11]) / 2f, tableTop + 80f, smallCenterPaint)

        canvas.drawText("تعیین رشته", (colX[11] + colX[12]) / 2f, tableTop + 65f, smallCenterPaint)
        canvas.drawText("نتیجه", (colX[12] + colX[13]) / 2f, tableTop + 65f, smallCenterPaint)
        canvas.drawText("ملاحظات", (colX[13] + colX[14]) / 2f, tableTop + 65f, smallCenterPaint)

        // Draw Subject Rows (14 Subjects)
        val subjectsList = if (card.subjects.isNotEmpty()) card.subjects else ReportCard.defaultSubjectsList()
        val numSubjects = 14
        val subjectRowHeight = 58f

        for (i in 0 until numSubjects) {
            val s = if (i < subjectsList.size) subjectsList[i] else com.example.domain.model.SubjectGrade(i + 1, "درس ${i + 1}")
            val yRowTop = tableHeaderBottom + (i * subjectRowHeight)
            val yRowBottom = yRowTop + subjectRowHeight
            val yText = yRowTop + 38f

            canvas.drawLine(tableLeft, yRowBottom, tableRight, yRowBottom, borderThin)

            // Row Number
            smallCenterPaint.textSize = 17f
            smallCenterPaint.isFakeBoldText = false
            canvas.drawText(s.rowNumber.toString().toPersianDigits(), (colX[0] + colX[1]) / 2f, yText, smallCenterPaint)

            // Subject Name
            textPaint.textSize = 18f
            textPaint.isFakeBoldText = true
            canvas.drawText(s.subjectName, colX[1] - 10f, yText, textPaint)

            // Term 1 Continuous (Hatched for non-continuous subjects e.g. 8, 10, 11)
            if (s.isContinuousTerm1Hatched) {
                drawHatchedBox(canvas, colX[3], yRowTop, colX[2], yRowBottom, hatchPaint, hatchLinePaint, borderThin)
            } else {
                canvas.drawText(s.term1Continuous.toPersianDigits(), (colX[2] + colX[3]) / 2f, yText, smallCenterPaint)
            }

            canvas.drawText(s.term1Final.toPersianDigits(), (colX[3] + colX[4]) / 2f, yText, smallCenterPaint)
            canvas.drawText(s.term1Score.toPersianDigits(), (colX[4] + colX[5]) / 2f, yText, smallCenterPaint)

            // Term 2 Continuous
            if (s.isContinuousTerm2Hatched) {
                drawHatchedBox(canvas, colX[6], yRowTop, colX[5], yRowBottom, hatchPaint, hatchLinePaint, borderThin)
            } else {
                canvas.drawText(s.term2Continuous.toPersianDigits(), (colX[5] + colX[6]) / 2f, yText, smallCenterPaint)
            }

            canvas.drawText(s.term2Final.toPersianDigits(), (colX[6] + colX[7]) / 2f, yText, smallCenterPaint)
            canvas.drawText(s.term2ScoreWithCoefficient.toPersianDigits(), (colX[7] + colX[8]) / 2f, yText, smallCenterPaint)
            canvas.drawText(s.annualScore.toPersianDigits(), (colX[8] + colX[9]) / 2f, yText, smallCenterPaint)

            canvas.drawText(s.summerFinal.toPersianDigits(), (colX[9] + colX[10]) / 2f, yText, smallCenterPaint)
            canvas.drawText(s.summerAnnual.toPersianDigits(), (colX[10] + colX[11]) / 2f, yText, smallCenterPaint)

            canvas.drawText(s.majorFieldSelection, (colX[11] + colX[12]) / 2f, yText, smallCenterPaint)
            canvas.drawText(s.result, (colX[12] + colX[13]) / 2f, yText, smallCenterPaint)
            canvas.drawText(s.notes, (colX[13] + colX[14]) / 2f, yText, smallCenterPaint)
        }

        // Summary Rows (Totals, GPA, Conduct, Final Status)
        val yTotals = tableHeaderBottom + (numSubjects * subjectRowHeight)
        val rowHeightSummary = 48f

        // Row 1: Total Scores
        canvas.drawLine(tableLeft, yTotals + rowHeightSummary, tableRight, yTotals + rowHeightSummary, borderThin)
        centerBoldPaint.textSize = 18f
        canvas.drawText("جمع نمرات", (colX[1] + colX[2]) / 2f, yTotals + 32f, centerBoldPaint)
        canvas.drawText(card.totalTerm1.toPersianDigits(), (colX[4] + colX[5]) / 2f, yTotals + 32f, centerBoldPaint)
        canvas.drawText(card.totalAnnual.toPersianDigits(), (colX[8] + colX[9]) / 2f, yTotals + 32f, centerBoldPaint)

        // Row 2: GPA
        val yGpa = yTotals + rowHeightSummary
        canvas.drawLine(tableLeft, yGpa + rowHeightSummary, tableRight, yGpa + rowHeightSummary, borderThin)
        canvas.drawText("معدل", (colX[1] + colX[2]) / 2f, yGpa + 32f, centerBoldPaint)
        canvas.drawText(card.gpaTerm1.toPersianDigits(), (colX[4] + colX[5]) / 2f, yGpa + 32f, centerBoldPaint)
        canvas.drawText(card.gpaAnnual.toPersianDigits(), (colX[8] + colX[9]) / 2f, yGpa + 32f, centerBoldPaint)

        // Row 3: GPA in Words
        val yWords = yGpa + rowHeightSummary
        canvas.drawLine(tableLeft, yWords + rowHeightSummary, tableRight, yWords + rowHeightSummary, borderThin)
        canvas.drawText("معدل به حروف", (colX[1] + colX[2]) / 2f, yWords + 32f, centerBoldPaint)
        textPaint.textSize = 18f
        canvas.drawText(card.gpaInWords, colX[2] - 15f, yWords + 32f, textPaint)

        // Row 4: Conduct
        val yConduct = yWords + rowHeightSummary
        canvas.drawLine(tableLeft, yConduct + rowHeightSummary, tableRight, yConduct + rowHeightSummary, borderThin)
        canvas.drawText("انضباط", (colX[1] + colX[2]) / 2f, yConduct + 32f, centerBoldPaint)
        canvas.drawText(card.conductTerm1, (colX[4] + colX[5]) / 2f, yConduct + 32f, centerBoldPaint)
        canvas.drawText(card.conductTerm2, (colX[8] + colX[9]) / 2f, yConduct + 32f, centerBoldPaint)

        // Row 5: Final Status
        val yFinal = yConduct + rowHeightSummary
        canvas.drawText("وضعیت نهایی سال تحصیلی", (colX[1] + colX[2]) / 2f, yFinal + 32f, centerBoldPaint)
        textPaint.textSize = 19f
        textPaint.isFakeBoldText = true
        canvas.drawText(card.finalStatus, colX[2] - 15f, yFinal + 32f, textPaint)

        // 5. Certification Paragraph & Signatures Box (y: 1545 to 1900)
        val certTop = 1545f
        textPaint.textSize = 18f
        textPaint.isFakeBoldText = false

        val certText = card.certificationText.ifBlank {
            "گواهی می‌شود دانش‌آموز با مشخصات فوق در ارزشیابی خرداد ماه سال ${card.academicYear.substringAfter("-")} در پایه ${card.grade} شرکت و ${card.finalStatus} شده است مندرجات این کارنامه با مندرجات دفتر نتایج ارزشیابی مطابقت دارد."
        }
        canvas.drawText(certText, width - margin - 20f, certTop + 30f, textPaint)

        // Signatures Boxes
        val sigTop = certTop + 75f
        centerBoldPaint.textSize = 19f

        // Left Signature: Executive Deputy
        canvas.drawText("نام و نام خانوادگی معاون اجرایی:", 320f, sigTop, centerBoldPaint)
        centerTextPaint.textSize = 18f
        canvas.drawText(card.executiveDeputyName, 320f, sigTop + 35f, centerTextPaint)
        smallCenterPaint.textSize = 16f
        canvas.drawText("امضاء", 320f, sigTop + 95f, smallCenterPaint)

        // Center Box: Stamp Placeholder
        val stampRect = RectF(560f, sigTop, 760f, sigTop + 140f)
        canvas.drawRoundRect(stampRect, 10f, 10f, borderThin)
        smallCenterPaint.textSize = 16f
        canvas.drawText("محل مهر مدرسه", 660f, sigTop + 75f, smallCenterPaint)

        // Right Signature: Principal
        canvas.drawText("نام و نام خانوادگی مدیر آموزشگاه:", 1060f, sigTop, centerBoldPaint)
        centerTextPaint.textSize = 18f
        canvas.drawText(card.principalName, 1060f, sigTop + 35f, centerTextPaint)
        canvas.drawText("امضاء", 1060f, sigTop + 95f, smallCenterPaint)

        // Vertical Side Indicator Bar on Right Edge
        val indicatorStr = "${card.issueDate.toPersianDigits()} - ${card.schoolCode.toPersianDigits()} - ${card.studentFullName} - ${card.birthDate.toPersianDigits()} - ${card.semesterTitle} ${card.academicYear.toPersianDigits()}"
        canvas.save()
        canvas.rotate(-90f, width - 15f, 1000f)
        smallCenterPaint.textSize = 14f
        canvas.drawText(indicatorStr, width - 15f, 1000f, smallCenterPaint)
        canvas.restore()
    }

    private fun drawHatchedBox(
        canvas: Canvas,
        left: Float, top: Float, right: Float, bottom: Float,
        fillPaint: Paint, linePaint: Paint, borderPaint: Paint
    ) {
        val rectLeft = Math.min(left, right)
        val rectRight = Math.max(left, right)
        canvas.drawRect(rectLeft, top, rectRight, bottom, fillPaint)
        canvas.drawRect(rectLeft, top, rectRight, bottom, borderPaint)

        var step = rectLeft - (bottom - top)
        while (step < rectRight) {
            canvas.drawLine(step, bottom, step + (bottom - top), top, linePaint)
            step += 12f
        }
    }

    fun getReportCardImageBitmap(card: ReportCard): Bitmap? {
        val imgUrl = card.reportCardImageUrl
        if (!imgUrl.isNullOrBlank()) {
            try {
                return if (imgUrl.startsWith("data:image")) {
                    val cleanBase64 = imgUrl.substringAfter(",")
                    val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else {
                    val bytes = android.util.Base64.decode(imgUrl, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun generateReportCardBitmap(card: ReportCard): Bitmap {
        val uploadedBitmap = getReportCardImageBitmap(card)
        if (uploadedBitmap != null) {
            return uploadedBitmap
        }
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawReportCard(canvas, card)
        return bitmap
    }

    fun exportReportCardAsJpg(context: Context, card: ReportCard): File? {
        return try {
            val bitmap = generateReportCardBitmap(card)
            val fileName = "ReportCard_${card.studentCode}_${card.academicYear.replace("-", "_")}.jpg"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            saveBitmapToMediaStore(context, bitmap, fileName)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportReportCardAsPdf(context: Context, card: ReportCard): File? {
        return try {
            val pdfDocument = PdfDocument()
            val uploadedBitmap = getReportCardImageBitmap(card)
            val width = uploadedBitmap?.width ?: CANVAS_WIDTH
            val height = uploadedBitmap?.height ?: CANVAS_HEIGHT

            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            if (uploadedBitmap != null) {
                page.canvas.drawBitmap(uploadedBitmap, 0f, 0f, null)
            } else {
                drawReportCard(page.canvas, card)
            }
            pdfDocument.finishPage(page)

            val fileName = "ReportCard_${card.studentCode}_${card.academicYear.replace("-", "_")}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            savePdfToMediaStore(context, file, fileName)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NezamYar")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun savePdfToMediaStore(context: Context, pdfFile: File, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NezamYar")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        pdfFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun openFile(context: Context, file: File, mimeType: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "مشاهده کارنامه"))
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در باز کردن فایل: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, studentName: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "کارنامه رسمی تحصیلی - $studentName")
                putExtra(Intent.EXTRA_TEXT, "کارنامه تحصیلی تربیتی $studentName صادرشده از سامانه هوشمند نظمیار")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری کارنامه"))
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در اشتراک‌گذاری: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
