package com.example.data.demo

import com.example.domain.model.AppNotification
import com.example.domain.model.BehaviorRecord
import com.example.domain.model.SchoolClass
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.UserRole

object DemoData {

    const val DEFAULT_SCHOOL_ID = "school_nezamyar_01"
    const val SCHOOL_NAME = "سامانه هوشمند نظم‌یار"
    const val CREATOR_PASSWORD = "hossein4545"

    // Pre-saved authorized staff and creator in the system
    val demoUsers = listOf(
        User(
            id = "user_creator_hossein",
            name = "سید حسین موسوی نژاد",
            phone = "09120000000",
            email = "hosseinvexter@gmail.com",
            role = UserRole.MANAGER,
            schoolId = DEFAULT_SCHOOL_ID,
            tag = "سازنده",
            studentCode = "454545",
            password = CREATOR_PASSWORD,
            rankTitle = "سازنده",
            rankBadgeColorHex = 0xFF1E40AF,
            rankColorHex = "#1E40AF"
        ),
        User(
            id = "user_manager_mehdi_faryabi",
            name = "مهدی فاریابی",
            phone = "09170000001",
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
            phone = "09170000002",
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

    // Completely empty student and record data sets until registered
    val demoStudents: List<Student> = emptyList()
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
