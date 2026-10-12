package com.example

import android.app.Application
import com.example.core.di.AppContainer
import com.example.core.session.SessionManager

class NezamYarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        AppContainer.init(this)
    }
}
