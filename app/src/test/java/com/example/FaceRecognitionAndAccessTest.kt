package com.example

import com.example.core.auth.AccessControl
import com.example.core.auth.ParentAccessManager
import com.example.core.face.FaceRecognitionManager
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceRecognitionAndAccessTest {

    @Test
    fun testAccessControlStaffAndCreatorAllowed() {
        val creatorUser = User(
            id = "user_creator_hossein",
            name = "سید حسین موسوی نژاد",
            phone = "09923072572",
            email = "hosseinvexter@gmail.com",
            role = UserRole.MANAGER,
            schoolId = "school_1",
            studentCode = "454545"
        )
        assertTrue(AccessControl.canManageFacesAndBehaviors(creatorUser))

        val managerUser = User(
            id = "user_mgr",
            name = "مدیر مدرسه",
            phone = "09121234567",
            email = "manager@school.ir",
            role = UserRole.MANAGER,
            schoolId = "school_1"
        )
        assertTrue(AccessControl.canManageFacesAndBehaviors(managerUser))

        val teacherUser = User(
            id = "user_tch",
            name = "دبیر محترم",
            phone = "09121234568",
            email = "teacher@school.ir",
            role = UserRole.TEACHER,
            schoolId = "school_1"
        )
        assertTrue(AccessControl.canManageFacesAndBehaviors(teacherUser))

        val vicePrincipal = User(
            id = "user_vp",
            name = "معاون انضباطی",
            phone = "09121234569",
            email = "vp@school.ir",
            role = UserRole.VICE_PRINCIPAL,
            schoolId = "school_1"
        )
        assertTrue(AccessControl.canManageFacesAndBehaviors(vicePrincipal))
    }

    @Test
    fun testAccessControlStudentsAndParentsStrictlyBlocked() {
        val studentUser = User(
            id = "user_student_1",
            name = "علی کاظمی",
            phone = "09120000001",
            email = "student@school.ir",
            role = UserRole.STUDENT,
            schoolId = "school_1",
            studentCode = "101010"
        )
        assertFalse(AccessControl.canManageFacesAndBehaviors(studentUser))
        assertTrue(AccessControl.getAccessDeniedMessage(studentUser).contains("دانش‌آموزان"))

        val parentUser = User(
            id = "user_parent_1",
            name = "پدر دانش‌آموز",
            phone = "09120000002",
            email = "parent@school.ir",
            role = UserRole.PARENT,
            schoolId = "school_1"
        )
        assertFalse(AccessControl.canManageFacesAndBehaviors(parentUser))
        assertTrue(AccessControl.getAccessDeniedMessage(parentUser).contains("اولیاء"))

        assertFalse(AccessControl.canManageFacesAndBehaviors(null))
    }

    @Test
    fun testFaceEmbeddingCosineSimilarity() {
        val v1 = floatArrayOf(0.6f, 0.8f, 0f, 0f)
        val v2 = floatArrayOf(0.6f, 0.8f, 0f, 0f) // Identical
        val similarityIdentical = FaceRecognitionManager.computeCosineSimilarity(v1, v2)
        assertEquals(1.0f, similarityIdentical, 0.001f)

        val vOrthogonal = floatArrayOf(0f, 0f, 0.6f, 0.8f) // Orthogonal
        val similarityOrthogonal = FaceRecognitionManager.computeCosineSimilarity(v1, vOrthogonal)
        assertEquals(0.0f, similarityOrthogonal, 0.001f)

        // Threshold checks
        assertTrue(FaceRecognitionManager.MATCH_THRESHOLD == 0.72f)
        assertTrue(FaceRecognitionManager.UNCERTAIN_THRESHOLD == 0.58f)
    }

    @Test
    fun testCandidateMatchRanking() {
        val student1 = Student(
            id = "s1",
            firstName = "محمد",
            lastName = "رضایی",
            studentCode = "101",
            grade = "دهم تجربی",
            className = "101",
            schoolId = "school_1"
        )
        val student2 = Student(
            id = "s2",
            firstName = "علی",
            lastName = "حسینی",
            studentCode = "102",
            grade = "دهم تجربی",
            className = "102",
            schoolId = "school_1"
        )

        val match1 = com.example.core.face.CandidateMatch(student1, similarity = 0.88f, confidencePct = 85, isTopMatch = true)
        val match2 = com.example.core.face.CandidateMatch(student2, similarity = 0.52f, confidencePct = 30, isTopMatch = false)

        val list = listOf(match2, match1).sortedByDescending { it.similarity }
        assertEquals("s1", list.first().student.id)
        assertTrue(list.first().isTopMatch)
        assertFalse(list.last().isTopMatch)
    }

    @Test
    fun testDualParentRequirementAndNotification() {
        val studentCode = "202020"
        val fatherUser = User(
            id = "parent_${studentCode}_dad",
            name = "پدر دانش‌آموز",
            phone = "09121111111",
            email = "dad@school.ir",
            role = UserRole.PARENT,
            schoolId = "school_1",
            studentCode = studentCode,
            gender = "پدر",
            rankTitle = "پدر دانش‌آموز"
        )
        val motherUser = User(
            id = "parent_${studentCode}_mom",
            name = "مادر دانش‌آموز",
            phone = "09122222222",
            email = "mom@school.ir",
            role = UserRole.PARENT,
            schoolId = "school_1",
            studentCode = studentCode,
            gender = "مادر",
            rankTitle = "مادر دانش‌آموز"
        )

        // Neither parent logged in
        assertFalse(ParentAccessManager.areBothParentsLoggedIn(null, studentCode, emptyList()))

        // Only father logged in -> still false
        assertFalse(ParentAccessManager.areBothParentsLoggedIn(null, studentCode, listOf(fatherUser)))
        assertTrue(ParentAccessManager.isFatherLoggedIn(null, studentCode, listOf(fatherUser)))
        assertFalse(ParentAccessManager.isMotherLoggedIn(null, studentCode, listOf(fatherUser)))

        // Only mother logged in -> still false
        assertFalse(ParentAccessManager.areBothParentsLoggedIn(null, studentCode, listOf(motherUser)))
        assertFalse(ParentAccessManager.isFatherLoggedIn(null, studentCode, listOf(motherUser)))
        assertTrue(ParentAccessManager.isMotherLoggedIn(null, studentCode, listOf(motherUser)))

        // Both parents logged in -> true!
        val bothList = listOf(fatherUser, motherUser)
        assertTrue(ParentAccessManager.areBothParentsLoggedIn(null, studentCode, bothList))

        // Celebratory notifications generation
        val notifs = ParentAccessManager.createBothParentsUnlockedNotifications(
            studentCode = studentCode,
            studentName = "کیان مرادی",
            studentId = "s_kian",
            schoolId = "school_1"
        )
        assertEquals(3, notifs.size)
        val dadNotif = notifs.find { it.recipientUserId == "parent_${studentCode}_dad" }
        val momNotif = notifs.find { it.recipientUserId == "parent_${studentCode}_mom" }
        val studentNotif = notifs.find { it.recipientUserId == "s_kian" }

        assertTrue(dadNotif != null)
        assertTrue(momNotif != null)
        assertTrue(studentNotif != null)
        assertTrue(dadNotif!!.title.contains("تبریک"))
        assertTrue(dadNotif.body.contains("پدر و مادر"))
    }
}
