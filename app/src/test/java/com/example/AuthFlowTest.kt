package com.example

import com.example.data.demo.DemoRepositoryImpl
import com.example.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthFlowTest {

    private lateinit var repo: DemoRepositoryImpl

    @Before
    fun setup() {
        repo = DemoRepositoryImpl()
    }

    @Test
    fun testCreatorLoginWithStudentCode() = runBlocking {
        val result = repo.login(
            name = "سید حسین موسوی نژاد",
            studentCode = "454545",
            password = "hossein4545"
        )
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("454545", user?.studentCode)
        assertEquals("سازنده", user?.tag)
        assertEquals("سازنده", user?.rankTitle)
        assertTrue(user?.isCreator == true)
        assertFalse(user?.isReadOnly ?: true)
    }

    @Test
    fun testStudentRegistrationWithRandomCodeAndGrayRank() = runBlocking {
        val studentCode = "987654"
        val regResult = repo.register(
            name = "علی محمدی",
            studentCode = studentCode,
            phone = "09121112233",
            email = "ali@example.com",
            password = "pass1234",
            role = UserRole.STUDENT,
            gender = "پسر"
        )

        assertTrue(regResult.isSuccess)
        val student = regResult.getOrNull()
        assertNotNull(student)
        assertEquals(studentCode, student?.studentCode)
        assertEquals(0xFF64748B, student?.rankBadgeColorHex) // Grey rank
        assertEquals("دانش‌آموز", student?.rankTitle)
        assertFalse(student?.isReadOnly ?: true)

        // Login with registered student
        val loginResult = repo.login("علی محمدی", studentCode, "pass1234")
        assertTrue(loginResult.isSuccess)
    }

    @Test
    fun testParentRegistrationReadOnlyAccess() = runBlocking {
        val parentCode = "654321"
        val regResult = repo.register(
            name = "رضا احمدی",
            studentCode = parentCode,
            phone = "09351234567",
            email = "reza@example.com",
            password = "parentPass",
            role = UserRole.PARENT,
            gender = "پدر (آقا)"
        )

        assertTrue(regResult.isSuccess)
        val parent = regResult.getOrNull()
        assertNotNull(parent)
        assertEquals(0xFF64748B, parent?.rankBadgeColorHex) // Grey rank
        assertTrue(parent?.isReadOnly == true) // Parents are read-only
    }

    @Test
    fun testAccountRecoveryByEmailOrPhone() = runBlocking {
        val code = "555666"
        repo.register(
            name = "مریم حسینی",
            studentCode = code,
            phone = "09129998877",
            email = "maryam@gmail.com",
            password = "secretPassword",
            role = UserRole.STUDENT,
            gender = "دختر"
        )

        // Recover by email
        val recoverResult1 = repo.recoverAccount("maryam@gmail.com")
        assertTrue(recoverResult1.isSuccess)
        assertEquals(code, recoverResult1.getOrNull()?.studentCode)
        assertEquals("secretPassword", recoverResult1.getOrNull()?.password)

        // Recover by phone
        val recoverResult2 = repo.recoverAccount("09129998877")
        assertTrue(recoverResult2.isSuccess)
        assertEquals(code, recoverResult2.getOrNull()?.studentCode)
    }

    @Test
    fun testLoginFailsWithInvalidCodeLength() = runBlocking {
        val result = repo.login("تست", "123", "pass")
        assertTrue(result.isFailure)
    }

    @Test
    fun testParentLoginUsingChildCredentialsForFatherAndMother() = runBlocking {
        val studentCode = "778899"
        val regResult = repo.register(
            name = "امیرحسین رضایی",
            studentCode = studentCode,
            phone = "09123334455",
            email = "amir@example.com",
            password = "childPassword123",
            role = UserRole.STUDENT,
            gender = "پسر"
        )
        assertTrue(regResult.isSuccess)

        // Login as Father using child's name, code and password
        val fatherLogin = repo.login(
            name = "امیرحسین رضایی",
            studentCode = studentCode,
            password = "childPassword123",
            role = UserRole.PARENT,
            parentRelation = "پدر دانش‌آموز"
        )
        assertTrue(fatherLogin.isSuccess)
        val fatherUser = fatherLogin.getOrNull()
        assertNotNull(fatherUser)
        assertEquals("امیرحسین رضایی", fatherUser?.name)
        assertEquals(UserRole.PARENT, fatherUser?.role)
        assertEquals("پدر دانش‌آموز", fatherUser?.rankTitle)
        assertTrue(fatherUser?.isReadOnly == true)

        // Login as Mother using child's credentials
        val motherLogin = repo.login(
            name = "امیرحسین رضایی",
            studentCode = studentCode,
            password = "childPassword123",
            role = UserRole.PARENT,
            parentRelation = "مادر دانش‌آموز"
        )
        assertTrue(motherLogin.isSuccess)
        val motherUser = motherLogin.getOrNull()
        assertNotNull(motherUser)
        assertEquals("امیرحسین رضایی", motherUser?.name)
        assertEquals(UserRole.PARENT, motherUser?.role)
        assertEquals("مادر دانش‌آموز", motherUser?.rankTitle)
        assertTrue(motherUser?.isReadOnly == true)
    }

    @Test
    fun testAddStudentFromListAllowsImmediateLoginForStudentAndParent() = runBlocking {
        val code = "112233"
        val student = com.example.domain.model.Student(
            id = "std_test_112233",
            firstName = "محمد",
            lastName = "صادقی",
            studentCode = code,
            grade = "پایه هشتم",
            className = "کلاس 8/1",
            gender = "پسر",
            schoolId = com.example.data.demo.DemoData.DEFAULT_SCHOOL_ID,
            score = 100,
            parentPhone = "09127778899",
            email = "mohammad@example.com",
            password = "mySecretPassword99"
        )
        val addResult = repo.addStudent(student)
        assertTrue(addResult.isSuccess)

        // Student logs in with their credentials
        val studentLogin = repo.login("محمد صادقی", code, "mySecretPassword99", UserRole.STUDENT)
        assertTrue(studentLogin.isSuccess)
        assertEquals(UserRole.STUDENT, studentLogin.getOrNull()?.role)

        // Parent (Father) logs in with child's credentials
        val parentLogin = repo.login("محمد صادقی", code, "mySecretPassword99", UserRole.PARENT, "پدر دانش‌آموز")
        assertTrue(parentLogin.isSuccess)
        val parentUser = parentLogin.getOrNull()
        assertEquals("محمد صادقی", parentUser?.name)
        assertEquals(UserRole.PARENT, parentUser?.role)
        assertEquals("پدر دانش‌آموز", parentUser?.rankTitle)
    }
}
