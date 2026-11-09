package com.example.data.demo

import com.example.domain.model.AppNotification
import com.example.domain.model.BehaviorRecord
import com.example.domain.model.SchoolClass
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.model.RankPermission

object DemoData {

    const val DEFAULT_SCHOOL_ID = "school_nezamyar_01"
    const val SCHOOL_NAME = "سامانه هوشمند نظم‌یار"
    const val CREATOR_PASSWORD = "hossein4545"

    // Pre-saved authorized staff and creator in the system
    val demoUsers = listOf(
        User(
            id = "user_creator_hossein",
            name = "سید حسین موسوی نژاد",
            phone = "+989923072572",
            email = "hosseinvexter@gmail.com",
            role = UserRole.MANAGER,
            schoolId = DEFAULT_SCHOOL_ID,
            tag = "سازنده",
            studentCode = "454545",
            password = CREATOR_PASSWORD,
            rankTitle = "سازنده",
            rankBadgeColorHex = 0xFF1E40AF,
            rankColorHex = "#1E40AF",
            permissions = listOf(
                RankPermission.REGISTER_POSITIVE,
                RankPermission.REGISTER_DISCIPLINARY,
                RankPermission.VIEW_REPORTS,
                RankPermission.ACCESS_GAMES,
                RankPermission.VIEW_STUDENT_PROFILES,
                RankPermission.EDIT_CLASS_INFO,
                RankPermission.SPECIAL_BADGE
            )
        ),
        User(
            id = "user_manager_mehdi_faryabi",
            name = "مهدی فاریابی",
            phone = "+989164237120",
            email = "mehdi.faryabi@school.ir",
            role = UserRole.MANAGER,
            schoolId = DEFAULT_SCHOOL_ID,
            tag = "مدیر مدرسه",
            studentCode = "100001",
            password = "faryabi123",
            rankTitle = "مدیر مدرسه",
            rankBadgeColorHex = 0xFFD97706,
            rankColorHex = "#D97706",
            permissions = emptyList(),
            extraRanks = emptyList(),
            extraRankColors = emptyList()
        ),
        User(
            id = "user_counselor_yasin_dorahaki",
            name = "یاسین دراهکی",
            phone = "+989172738837",
            email = "yasin.dorahaki@school.ir",
            role = UserRole.COUNSELOR,
            schoolId = DEFAULT_SCHOOL_ID,
            tag = "معاون پرورشی",
            studentCode = "100002",
            password = "dorahaki123",
            rankTitle = "معاون پرورشی",
            rankBadgeColorHex = 0xFF059669,
            rankColorHex = "#059669",
            permissions = emptyList(),
            extraRanks = emptyList(),
            extraRankColors = emptyList()
        ),
        User(
            id = "user_teacher_hosein_rasekhiyan",
            name = "حسین راسخیان",
            phone = "+989906573982",
            email = "rasekhiyan@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100003",
            password = "Nezam@3982",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        ),
        User(
            id = "user_teacher_peyman_tobak",
            name = "پیمان توبک",
            phone = "+989164973001",
            email = "tobak@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100004",
            password = "Nezam@3001",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        ),
        User(
            id = "user_teacher_abdollah_akbarifard",
            name = "عبدالله اکبری فرد",
            phone = "+989173828459",
            email = "akbarifard@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100005",
            password = "Nezam@8459",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        ),
        User(
            id = "user_teacher_hosein_jatoot",
            name = "حسین جاتوت",
            phone = "+989176670372",
            email = "jatoot@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100006",
            password = "Nezam@0372",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        ),
        User(
            id = "user_teacher_gholamreza_khayat",
            name = "غلامرضا خیاط",
            phone = "+989177758126",
            email = "khayat@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100007",
            password = "Nezam@8126",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        ),
        User(
            id = "user_teacher_mokhtar_alipoor",
            name = "مختار عالیپور",
            phone = "+989177753618",
            email = "alipoor@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100008",
            password = "Nezam@3618",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        ),
        User(
            id = "user_teacher_hosein_vahdat",
            name = "حسین وحدت",
            phone = "+989175293998",
            email = "vahdat@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100009",
            password = "Nezam@3998",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        ),
        User(
            id = "user_teacher_aghil_miri",
            name = "عقیل میری",
            phone = "+989179738096",
            email = "miri@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100010",
            password = "Nezam@8096",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        ),
        User(
            id = "user_teacher_noroozian",
            name = "مصطفی نوروزیان",
            phone = "+989171738917",
            email = "noroozian@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100011",
            password = "Nezam@8917",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        ),
        User(
            id = "user_teacher_pakseresht",
            name = "پاک سرشت",
            phone = "+989171753163",
            email = "pakseresht@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100012",
            password = "Nezam@3163",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        ),
        User(
            id = "user_teacher_mohammadmehdi_bahreini",
            name = "محمد مهدی بحرینی",
            phone = "+989179750058",
            email = "bahreini@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100013",
            password = "Nezam@0058",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        ),
        User(
            id = "user_teacher_khodanazar_bahadori",
            name = "خدانظر بهادری",
            phone = "+989177753381",
            email = "bahadori@school.ir",
            role = UserRole.TEACHER,
            schoolId = DEFAULT_SCHOOL_ID,
            studentCode = "100014",
            password = "Nezam@3381",
            rankTitle = "دبیر",
            rankBadgeColorHex = 0xFF0D9488,
            rankColorHex = "#0D9488"
        )
    )

    // All official students from school roster added to contacts list
    val demoStudents: List<Student> = AllowedStudentsRegistry.allAllowedStudents.mapIndexed { index, entry ->
        val isCreator = (entry.fullName == "سید حسین موسوی نژاد") ||
            (entry.firstName == "سید حسین" && entry.lastName == "موسوی نژاد") ||
            AllowedStudentsRegistry.normalize(entry.fullName) == AllowedStudentsRegistry.normalize("سید حسین موسوی نژاد")
        val codeNum = if (isCreator) "454545" else (100001 + index).toString()
        Student(
            id = if (isCreator) "user_creator_hossein" else "std_roster_${index + 1}",
            firstName = entry.firstName,
            lastName = entry.lastName,
            studentCode = codeNum,
            nationalId = if (isCreator) "454545" else "",
            grade = entry.grade,
            className = entry.className,
            gender = "پسر",
            birthDate = "۱۳۸۹/۰۴/۱۵",
            parentName = "ولی ${entry.lastName}",
            parentPhone = if (isCreator) "+989923072572" else "",
            email = if (isCreator) "hosseinvexter@gmail.com" else "",
            schoolId = DEFAULT_SCHOOL_ID,
            score = 100,
            rankTitle = if (isCreator) "سازنده" else "دانش‌آموز",
            rankColorHex = if (isCreator) "#1E40AF" else "#64748B",
            permissions = if (isCreator) listOf(
                RankPermission.REGISTER_POSITIVE,
                RankPermission.REGISTER_DISCIPLINARY,
                RankPermission.VIEW_REPORTS,
                RankPermission.ACCESS_GAMES,
                RankPermission.VIEW_STUDENT_PROFILES,
                RankPermission.EDIT_CLASS_INFO,
                RankPermission.SPECIAL_BADGE
            ) else emptyList(),
            isRegistered = isCreator
        )
    }
    val demoClasses: List<SchoolClass> = listOf(
        SchoolClass(
            id = "class_7_1",
            schoolId = DEFAULT_SCHOOL_ID,
            grade = "پایه هفتم",
            name = "کلاس 7/1",
            teacherId = "user_teacher_hosein_rasekhiyan",
            teacherName = "حسین راسخیان",
            studentCount = AllowedStudentsRegistry.grade7Class1.size
        ),
        SchoolClass(
            id = "class_7_2",
            schoolId = DEFAULT_SCHOOL_ID,
            grade = "پایه هفتم",
            name = "کلاس 7/2",
            teacherId = "user_teacher_peyman_tobak",
            teacherName = "پیمان توبک",
            studentCount = AllowedStudentsRegistry.grade7Class2.size
        ),
        SchoolClass(
            id = "class_8_1",
            schoolId = DEFAULT_SCHOOL_ID,
            grade = "پایه هشتم",
            name = "کلاس 8/1",
            teacherId = "user_teacher_abdollah_akbarifard",
            teacherName = "عبدالله اکبری فرد",
            studentCount = AllowedStudentsRegistry.grade8Class1.size
        ),
        SchoolClass(
            id = "class_8_2",
            schoolId = DEFAULT_SCHOOL_ID,
            grade = "پایه هشتم",
            name = "کلاس 8/2",
            teacherId = "user_teacher_hosein_jatoot",
            teacherName = "حسین جاتوت",
            studentCount = AllowedStudentsRegistry.grade8Class2.size
        ),
        SchoolClass(
            id = "class_9_1",
            schoolId = DEFAULT_SCHOOL_ID,
            grade = "پایه نهم",
            name = "کلاس 9/1",
            teacherId = "user_teacher_gholamreza_khayat",
            teacherName = "غلامرضا خیاط",
            studentCount = AllowedStudentsRegistry.grade9Class1.size
        ),
        SchoolClass(
            id = "class_9_2",
            schoolId = DEFAULT_SCHOOL_ID,
            grade = "پایه نهم",
            name = "کلاس 9/2",
            teacherId = "user_teacher_mokhtar_alipoor",
            teacherName = "مختار عالیپور",
            studentCount = AllowedStudentsRegistry.grade9Class2.size
        )
    )
    val demoRecords: List<BehaviorRecord> = emptyList()
    val demoNotifications: List<AppNotification> = emptyList()
}
