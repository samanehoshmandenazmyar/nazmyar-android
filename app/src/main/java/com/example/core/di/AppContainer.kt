package com.example.core.di

import android.content.Context
import com.example.core.session.SessionManager
import com.example.data.demo.DemoRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BehaviorRepository
import com.example.domain.repository.ClassRepository
import com.example.domain.repository.NotificationRepository
import com.example.domain.repository.StudentRepository

/**
 * Service Locator / Dependency Container.
 */
object AppContainer {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        SessionManager.init(context)
        com.example.core.auth.ParentAccessManager.init(context)
        com.example.core.notification.NotificationHelper.createNotificationChannel(appContext)
        demoRepo.restoreSavedSession()
    }

    private val demoRepo = DemoRepositoryImpl()

    val authRepository: AuthRepository = demoRepo
    val studentRepository: StudentRepository = demoRepo
    val behaviorRepository: BehaviorRepository = demoRepo
    val classRepository: ClassRepository = demoRepo
    val notificationRepository: NotificationRepository = demoRepo
}
