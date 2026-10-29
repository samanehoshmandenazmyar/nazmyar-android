package com.example.domain.model

data class SubjectGrade(
    val rowNumber: Int,
    val subjectName: String,
    // نوبت اول / دوم
    val term1Continuous: String = "۲۰",      // تکوینی
    val term1Final: String = "۲۰",           // پایانی
    val term1Score: String = "۲۰",           // نمره درس
    val term2Continuous: String = "۲۰",      // تکوینی
    val term2Final: String = "۲۰",           // پایانی
    val term2ScoreWithCoefficient: String = "۴۰", // نمره درس (با ضریب)
    val annualScore: String = "۲۰",          // نمره سالیانه درس
    val summerFinal: String = "",            // پایانی
    val summerAverage: String = "",          // میانگین
    val summerAnnual: String = "",           // سالیانه
    val majorFieldSelection: String = "",
    val result: String = "قبول",
    val notes: String = "",
    val isContinuousTerm1Hatched: Boolean = false,
    val isContinuousTerm2Hatched: Boolean = false,

    // گزارش عملکرد تحصیلی ماهانه
    val monthlyMyScore: String = "۲۰",       // نمره شما
    val monthlyClassAvg: String = "۱۸.۵۰",   // میانگین نمره کلاس
    val monthlyClassRank: String = "۱"       // رتبه درسی شما در کلاس
)

data class ReportCard(
    val id: String,
    val studentId: String,
    val studentCode: String,
    val nationalId: String = "",
    val studentFirstName: String,
    val studentLastName: String,
    val fatherName: String = "",
    val gender: String = "پسر",
    val birthDate: String = "۱۳۹۱/۱۲/۱۶",
    val birthPlace: String = "کنگان",
    val studentPhoto: String? = null,

    // نوع کارنامه: "TERM" (ترم / سالیانه) یا "MONTHLY" (ماهانه)
    val cardType: String = "TERM",
    val selectedMonth: String = "مهر", // مهر، آبان، آذر، دی، بهمن، اسفند، فروردین، اردیبهشت، خرداد

    // مشخصات مدرسه و تحصیلی
    val province: String = "بوشهر",
    val region: String = "دیر",
    val schoolName: String = "دبیرستان شهید رجایی",
    val schoolCode: String = "۹۵۰۹۶۸۷۳",
    val grade: String = "هفتم",
    val className: String = "هفتم ۱",
    val schoolType: String = "دولتی",
    val schoolLocation: String = "شهری",
    val schoolFeature: String = "عادی",
    val educationalPeriod: String = "دوره اول متوسطه",
    val academicYear: String = "۱۴۰۵-۱۴۰۶",
    val semesterTitle: String = "ضمن سال", // ضمن سال، نوبت اول، نوبت دوم خرداد
    val issueDate: String = "۱۴۰۵/۰۷/۳۰",

    // نمرات دروس
    val subjects: List<SubjectGrade> = emptyList(),

    // جمع نمرات و معدل ترم / سالیانه
    val totalTerm1: String = "۲۸۰",
    val totalAnnual: String = "۵۶۰",
    val gpaTerm1: String = "۲۰",
    val gpaAnnual: String = "۲۰",
    val gpaInWords: String = "بیست",
    val conductTerm1: String = "عالی",
    val conductTerm2: String = "عالی",
    val finalStatus: String = "قبول خرداد",

    // خلاصه‌های عملکرد ماهانه
    val highestClassGpa: String = "۱۹.۲۵",   // بالاترین معدل کلاس
    val monthlyStudentGpa: String = "۱۹.۰۰",  // معدل شما
    val lowestClassGpa: String = "۱۳.۰۸",    // پایین ترین معدل کلاس
    val overallClassRank: String = "۲",      // رتبه کلی شما در کلاس

    // امضاها و گواهی
    val principalName: String = "مهدی فاریابی",
    val executiveDeputyName: String = "یاسین دراهکی",
    val certificationText: String = "گواهی می‌شود دانش‌آموز با مشخصات فوق در ارزشیابی خرداد ماه سال ۱۴۰۵ در پایه هفتم شرکت و قبولی خرداد شده است مندرجات این کارنامه با مندرجات دفتر نتایج ارزشیابی مطابقت دارد.",
    val indicatorNumber: String = "۱۴۰۵-۵۰۸-۲",
    val reportCardImageUrl: String? = null,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val studentFullName: String
        get() = "$studentFirstName $studentLastName".trim().ifBlank { "دانش‌آموز" }

    companion object {
        fun defaultSubjectsList(): List<SubjectGrade> = listOf(
            SubjectGrade(1, "قرآن", "۲۰", "۲۰", "۲۰", "۲۰", "۲۰", "۴۰", "۲۰"),
            SubjectGrade(2, "معارف اسلامی (پیام‌های آسمان)", "۲۰", "۲۰", "۲۰", "۲۰", "۲۰", "۴۰", "۲۰"),
            SubjectGrade(3, "املای فارسی", "۲۰", "۲۰", "۲۰", "۲۰", "۲۰", "۴۰", "۲۰"),
            SubjectGrade(4, "زبان عربی", "۲۰", "۲۰", "۲۰", "۲۰", "۲۰", "۴۰", "۲۰"),
            SubjectGrade(5, "زبان خارجی", "۲۰", "۲۰", "۲۰", "۲۰", "۲۰", "۴۰", "۲۰"),
            SubjectGrade(6, "علوم تجربی", "۲۰", "۲۰", "۲۰", "۲۰", "۲۰", "۴۰", "۲۰"),
            SubjectGrade(7, "ریاضیات", "۲۰", "۲۰", "۲۰", "۲۰", "۲۰", "۴۰", "۲۰"),
            SubjectGrade(8, "تربیت بدنی و سلامت", "", "۲۰", "۲۰", "", "۲۰", "۴۰", "۲۰", isContinuousTerm1Hatched = true, isContinuousTerm2Hatched = true),
            SubjectGrade(9, "مطالعات اجتماعی", "۲۰", "۲۰", "۲۰", "۲۰", "۲۰", "۴۰", "۲۰"),
            SubjectGrade(10, "فرهنگ و هنر", "", "۲۰", "۲۰", "", "۲۰", "۴۰", "۲۰", isContinuousTerm1Hatched = true, isContinuousTerm2Hatched = true),
            SubjectGrade(11, "کار و فناوری", "", "۲۰", "۲۰", "", "۲۰", "۴۰", "۲۰", isContinuousTerm1Hatched = true, isContinuousTerm2Hatched = true),
            SubjectGrade(12, "تفکر و سبک زندگی", "۲۰", "۲۰", "۲۰", "۲۰", "۲۰", "۴۰", "۲۰"),
            SubjectGrade(13, "فارسی", "۲۰", "۲۰", "۲۰", "۲۰", "۲۰", "۴۰", "۲۰"),
            SubjectGrade(14, "نگارش", "۲۰", "۱۹", "۲۰", "۲۰", "۲۰", "۴۰", "۲۰")
        )

        fun defaultMonthlySubjectsList(): List<SubjectGrade> = listOf(
            SubjectGrade(1, "فارسی", monthlyMyScore = "۲۰", monthlyClassAvg = "۱۷.۵۸", monthlyClassRank = "۱"),
            SubjectGrade(2, "ریاضی", monthlyMyScore = "۱۹", monthlyClassAvg = "۱۰.۳۳", monthlyClassRank = "۲"),
            SubjectGrade(3, "علوم", monthlyMyScore = "۲۰", monthlyClassAvg = "۸.۲۳", monthlyClassRank = "۱"),
            SubjectGrade(4, "مطالعات", monthlyMyScore = "۱۷", monthlyClassAvg = "۱۵.۲۲", monthlyClassRank = "۳"),
            SubjectGrade(5, "پیام آسمانی", monthlyMyScore = "۱۶", monthlyClassAvg = "۱۵.۶۶", monthlyClassRank = "۸"),
            SubjectGrade(6, "عربی", monthlyMyScore = "۲۰", monthlyClassAvg = "۱۲.۳۲", monthlyClassRank = "۱"),
            SubjectGrade(7, "زبان", monthlyMyScore = "۲۰", monthlyClassAvg = "۱۰.۶۶", monthlyClassRank = "۱"),
            SubjectGrade(8, "تفکر و سبک زندگی", monthlyMyScore = "۲۰", monthlyClassAvg = "۱۸.۳۷", monthlyClassRank = "۱"),
            SubjectGrade(9, "کار و فناوری", monthlyMyScore = "۱۶", monthlyClassAvg = "۱۶.۲۵", monthlyClassRank = "۱۰"),
            SubjectGrade(10, "فرهنگ و هنر", monthlyMyScore = "۲۰", monthlyClassAvg = "۲۰.۰۰", monthlyClassRank = "۱"),
            SubjectGrade(11, "تربیت بدنی و سلامت", monthlyMyScore = "۲۰", monthlyClassAvg = "۲۰.۰۰", monthlyClassRank = "۱"),
            SubjectGrade(12, "قرآن", monthlyMyScore = "۲۰", monthlyClassAvg = "۱۹.۷۶", monthlyClassRank = "۱"),
            SubjectGrade(13, "انضباط", monthlyMyScore = "۲۰", monthlyClassAvg = "۲۰.۰۰", monthlyClassRank = "۱")
        )
    }
}
