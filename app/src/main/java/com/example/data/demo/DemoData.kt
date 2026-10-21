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
            phone = "09923072572",
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
            phone = "09164237120",
            email = "mehdi.faryabi@school.ir",
            role = UserRole.MANAGER,
            schoolId = DEFAULT_SCHOOL_ID,
            tag = "مدیر مدرسه",
            studentCode = "100001",
            password = "faryabi123",
            rankTitle = "",
            rankBadgeColorHex = 0xFF64748B,
            rankColorHex = "#64748B",
            permissions = emptyList(),
            extraRanks = emptyList(),
            extraRankColors = emptyList()
        ),
        User(
            id = "user_counselor_yasin_dorahaki",
            name = "یاسین دراهکی",
            phone = "09172738837",
            email = "yasin.dorahaki@school.ir",
            role = UserRole.COUNSELOR,
            schoolId = DEFAULT_SCHOOL_ID,
            tag = "معاون پرورشی",
            studentCode = "100002",
            password = "dorahaki123",
            rankTitle = "",
            rankBadgeColorHex = 0xFF64748B,
            rankColorHex = "#64748B",
            permissions = emptyList(),
            extraRanks = emptyList(),
            extraRankColors = emptyList()
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
            parentPhone = if (isCreator) "09923072572" else "",
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
            teacherId = "teacher_01",
            teacherName = "دبیر پایه هفتم",
            studentCount = AllowedStudentsRegistry.grade7Class1.size
        ),
        SchoolClass(
            id = "class_7_2",
            schoolId = DEFAULT_SCHOOL_ID,
            grade = "پایه هفتم",
            name = "کلاس 7/2",
            teacherId = "teacher_02",
            teacherName = "دبیر پایه هفتم",
            studentCount = AllowedStudentsRegistry.grade7Class2.size
        ),
        SchoolClass(
            id = "class_8_1",
            schoolId = DEFAULT_SCHOOL_ID,
            grade = "پایه هشتم",
            name = "کلاس 8/1",
            teacherId = "teacher_03",
            teacherName = "دبیر پایه هشتم",
            studentCount = AllowedStudentsRegistry.grade8Class1.size
        ),
        SchoolClass(
            id = "class_8_2",
            schoolId = DEFAULT_SCHOOL_ID,
            grade = "پایه هشتم",
            name = "کلاس 8/2",
            teacherId = "teacher_04",
            teacherName = "دبیر پایه هشتم",
            studentCount = AllowedStudentsRegistry.grade8Class2.size
        ),
        SchoolClass(
            id = "class_9_1",
            schoolId = DEFAULT_SCHOOL_ID,
            grade = "پایه نهم",
            name = "کلاس 9/1",
            teacherId = "teacher_05",
            teacherName = "دبیر پایه نهم",
            studentCount = AllowedStudentsRegistry.grade9Class1.size
        ),
        SchoolClass(
            id = "class_9_2",
            schoolId = DEFAULT_SCHOOL_ID,
            grade = "پایه نهم",
            name = "کلاس 9/2",
            teacherId = "teacher_06",
            teacherName = "دبیر پایه نهم",
            studentCount = AllowedStudentsRegistry.grade9Class2.size
        )
    )
    val demoRecords: List<BehaviorRecord> = emptyList()
    val demoNotifications: List<AppNotification> = emptyList()
}
