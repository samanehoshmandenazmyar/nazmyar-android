package com.example.data.demo

data class AllowedStudentEntry(
    val fullName: String,
    val firstName: String,
    val lastName: String,
    val grade: String,
    val className: String
)

object AllowedStudentsRegistry {

    fun normalize(text: String): String {
        return text
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('آ', 'ا')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace("‌", "") // remove zero-width non-joiner
            .replace(" ", "") // remove spaces
            .replace("(", "")
            .replace(")", "")
            .replace("-", "")
            .trim()
            .lowercase()
    }

    private fun parseRaw(fullName: String, grade: String, className: String): AllowedStudentEntry {
        val clean = fullName.trim()
        val parts = clean.split(" ").filter { it.isNotBlank() }
        val fName = parts.firstOrNull().orEmpty()
        val lName = parts.drop(1).joinToString(" ").trim()
        return AllowedStudentEntry(
            fullName = clean,
            firstName = fName,
            lastName = lName,
            grade = grade,
            className = className
        )
    }

    // List of Grade 7 Class 1 (پایه هفتم، کلاس 7/1)
    val grade7Class1 = listOf(
        "محمد طاها ارشادی",
        "رضا ایمانخواه",
        "آریا دوراهکی",
        "امیر محمد دوراهکی",
        "حسن دوراهکی",
        "حمید رضا دراهکی",
        "محمد دوراهکی",
        "محمد رضا دوراهکی",
        "حسین دشتی",
        "علیرضا دوراهکی",
        "آرش صابری",
        "حمید رضا عالیپور",
        "علی عالیپور",
        "محمد صدرا عبدالهی",
        "امیر علی عسکری",
        "سید احمد موسوی",
        "امیر حسین میرزایی",
        "حسین میرزایی",
        "محمد مهدی میرزایی خو",
        "امیر مهدی وفاپور",
        "محمد صادق وفاپور",
        "امین انیژپور",
        "امیر محمد بردستانی",
        "علی اصغر خوش چشم",
        "محمد حسین خیاط آزاد",
        "امیر محمد دوراهکی",
        "محمد طاها دوراهکی",
        "بنیامین سماچی",
        "متین شهنیایی",
        "حسین عالی",
        "امیر علی عالی پور",
        "امیر محمد عالیپور",
        "علی عبدالهی(مرتضی)",
        "علی عبدالهی(مصطفی)",
        "امیر علی مدرسی",
        "هانی محمدی"
    ).map { parseRaw(it, "پایه هفتم", "کلاس 7/1") }

    // List of Grade 7 Class 2 (پایه هفتم، کلاس 7/2)
    val grade7Class2 = listOf(
        "علیرضا احمدی",
        "اکبر میرزایی",
        "امیر عباس بردستانی",
        "محمد طاها بوشته",
        "علیرضا پارسامهر",
        "علی اصغر جمالی حقیقی",
        "حامد موذنی",
        "محمد رضا دوراهکی",
        "امیر حسین دوراهکی",
        "امیر محمد دوراهکی",
        "امیر علی شمشیری",
        "مهدی شمشیری",
        "علی عارفی نژاد",
        "حسین عالی پور",
        "مهرداد محمدی زاده",
        "محمد حسین قاسمی",
        "فرهاد قاسمی",
        "محمد طاها کنگانی",
        "علیرضا کنگانی",
        "امیر طاها آسیون",
        "علیرضا کریم پور",
        "محمد طاها ده بوده",
        "علی احسان دوراهکی",
        "حسین عمرانی",
        "یاسین پور نیازی",
        "امیر محمد جمال اوف",
        "امیررضا خدابخشی",
        "عباس عارفی نژاد",
        "یوسف عارفی نژاد",
        "محمد اسماعیل کبیری",
        "محمد جواد مجیرنیا",
        "احمد رضا عالی پور",
        "حسام کرامتی",
        "یاسان شیخ محمودی",
        "امیر حسین عالی پور",
        "رهام کامجو"
    ).map { parseRaw(it, "پایه هفتم", "کلاس 7/2") }

    // List of Grade 8 Class 1 (پایه هشتم، کلاس 8/1)
    val grade8Class1 = listOf(
        "امیر محمد آهوخوش",
        "امیر محمد احمدی",
        "حسین احمدی",
        "امیر عباس ایشم",
        "ابوالفضل بردستانی",
        "حسین بوشته",
        "محمد رضا بوشته",
        "بنیامین بیات",
        "عرفان پاریابی",
        "محمد جواد پاریابی",
        "مهدی پورانات",
        "هادی جعفری",
        "محمد حسین حمیدی پور",
        "امیر علی خیاط",
        "طاها خیاط",
        "محمد جواد خیاط",
        "ایمان دراهکی",
        "علی دراهکی",
        "محمد رضا دراهکی",
        "مهدی دراهکی",
        "عباس دراهکی",
        "محمدرضا دوراهکی",
        "مهدی دوراهکی",
        "علیرضا رضایی",
        "ابوالفضل عالیپور",
        "حمیدرضا عالیپور",
        "محمدعلی عالیپور",
        "محمدحسین گنخکی نژاد",
        "امیررضا محبیفرد",
        "امیرعلی محبیفرد",
        "محمدجواد محمدی",
        "مهدی محمدی",
        "محمدحسین میرزایی",
        "رضا نوکنیخو",
        "طاها نوکنیمطلق",
        "ایمان نیکنژاد",
        "امیرعلی وفاپور",
        "امیرمحمد وفاخواه",
        "ابوالفضل هاروت"
    ).map { parseRaw(it, "پایه هشتم", "کلاس 8/1") }

    // List of Grade 8 Class 2 (پایه هشتم، کلاس 8/2)
    val grade8Class2 = listOf(
        "علی اصغر ابراهیمی",
        "محمد رسول ابراهیمی",
        "محمد حسین امیدی نژاد",
        "عباس اولیایی",
        "محسن آهوخوش",
        "محمد تاجیک",
        "محمد حسن جمالی خواه",
        "امیر علی خوش چشم",
        "محمد جواد خوش چشم",
        "علی اکبر دارستانی",
        "قاسم دراهکی",
        "محمد مهدی دوهاروت",
        "امیر علی دهاروت",
        "محمد رستگار",
        "قاسم زارعی",
        "سبحان شهنیایی",
        "محمد طاها شیخی",
        "محمد صادق منش",
        "عارف عارفی نژاد",
        "امیر علی عالیپور(احمد)",
        "امیر علی عالیپور(اکبر)",
        "حمید رضا عمرانی اصل",
        "ابوالفضل فاریابی",
        "حسین فخرالدینی",
        "محسن فخرالدینی",
        "امیر حسین قاسمی",
        "محمد علی قاسمی",
        "حسین قیم",
        "حسام کامجو",
        "علیرضا گرامی مقدم",
        "امیر علی گرگین زاده",
        "عباس مالکی",
        "علیرضا مالکی",
        "احسان محبی",
        "حسین محبی",
        "سید حسین موسوی نژاد",
        "محمد مهدی موذنی",
        "امیر حسام موذنی زاده",
        "سعید میرزایی زاده",
        "محمد مهدی نسرین پور"
    ).map { parseRaw(it, "پایه هشتم", "کلاس 8/2") }

    // List of Grade 9 Class 1 (پایه نهم، کلاس 9/1)
    val grade9Class1 = listOf(
        "رضا احمدی",
        "محمد مهدی اسمعیلی",
        "مهدی اکبری",
        "امیر علی آهوخوش",
        "علی باقری",
        "حسام بوشته",
        "محمد جواد بیات",
        "پوریا پاریابی",
        "رضا پورانات",
        "امیر محمد خوش خرام",
        "علیرضا خیاط",
        "محمد مهدی دارستانی",
        "امیر علی دراهکی",
        "ایلیا دراهکی",
        "طاها دراهکی",
        "عباس دراهکی",
        "محمد حسن دراهکی",
        "امیر حسین دوراهکی",
        "علی دوراهکی",
        "بنیامین دوهاروت",
        "حسین زارعی",
        "میکائیل زارعی",
        "امیر حسین عبدالهی",
        "محمد صادق عمرانی",
        "حسن فاریابی",
        "علی اصغر فاریابی",
        "محمد حسن فاریابی",
        "محمد حسین قنواتی",
        "سید علی اصغر محمودی",
        "ابوالحسن کاظم معرفاوی",
        "طاها موذنی",
        "ابوالفضل میرزایی",
        "محمد جواد میرزایی",
        "محمد رضا میرزایی"
    ).map { parseRaw(it, "پایه نهم", "کلاس 9/1") }

    // List of Grade 9 Class 2 (پایه نهم، کلاس 9/2)
    val grade9Class2 = listOf(
        "امیر محمد آبدونی",
        "محمد امین آهوخوش",
        "محمد حسین ابراهیمی",
        "سینا احمدی",
        "علی اصغر انگار",
        "مهدی بحرانی",
        "مهدی بردستانی",
        "علیرضا بهشتی مطلق",
        "عرفان پرده خرم",
        "محمد رضا دانایی",
        "امیر علی دراهکی",
        "عباس دراهکی",
        "علیرضا دراهکی",
        "محمد دراهکی",
        "مهدی دراهکی",
        "امیر علی راسخ خود",
        "ابوالفضل رشیدی منفرد",
        "رضا روام بد",
        "ابوالفضل سپاهی",
        "امیر پارسا شجاعی",
        "امیر علی صفوی",
        "حسین عالیپور",
        "حمیدرضا عالیپور",
        "علی اکبر عالیپور",
        "امیر علی عباس پور بحرانی",
        "محمد امین گنخکی نیا",
        "حسن محمدی",
        "امیر علی مطلق",
        "مرتضی معرفی",
        "سید محمد جواد موسوی نسب",
        "امیر محمد میرزایی",
        "حسین میرزایی",
        "حسین نصاری",
        "امیر حسین نوشاد"
    ).map { parseRaw(it, "پایه نهم", "کلاس 9/2") }

    // Combined all allowed students
    val allAllowedStudents: List<AllowedStudentEntry> =
        grade7Class1 + grade7Class2 + grade8Class1 + grade8Class2 + grade9Class1 + grade9Class2

    /**
     * Checks if the entered name matches one of the official allowed students.
     * Tolerant to spaces, arabic/persian yeh/kaf, parentheses, and prefixes.
     */
    fun findMatchingAllowedStudent(inputName: String): AllowedStudentEntry? {
        val cleanInput = inputName.trim()
            .replace("دانش‌آموز", "")
            .replace("دانش آموز", "")
            .trim()
        val normInput = normalize(cleanInput)
        if (normInput.isBlank()) return null

        // 1. Exact normalized match with full name
        val exactMatch = allAllowedStudents.firstOrNull { normalize(it.fullName) == normInput }
        if (exactMatch != null) return exactMatch

        // 2. Exact match after stripping parentheses from registered name e.g. "علی عبدالهی"
        val noParenMatch = allAllowedStudents.firstOrNull {
            val baseWithoutParentheses = it.fullName.replace(Regex("\\(.*\\)"), "").trim()
            normalize(baseWithoutParentheses) == normInput
        }
        if (noParenMatch != null) return noParenMatch

        // 3. Last name exact match + first name start/match
        val parts = cleanInput.split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            val inputFirst = normalize(parts.first())
            val inputLast = normalize(parts.drop(1).joinToString(""))
            val partialMatch = allAllowedStudents.firstOrNull {
                val candidateFirst = normalize(it.firstName)
                val candidateLast = normalize(it.lastName)
                candidateLast == inputLast && (candidateFirst == inputFirst || candidateFirst.contains(inputFirst) || inputFirst.contains(candidateFirst))
            }
            if (partialMatch != null) return partialMatch
        }

        return null
    }

    /**
     * Returns true if the student name is in the allowed list or is the creator.
     */
    fun isStudentNameAllowed(inputName: String): Boolean {
        val cleanInput = inputName.trim()
        if (cleanInput == "سید حسین موسوی نژاد") return true
        return findMatchingAllowedStudent(cleanInput) != null
    }
}
