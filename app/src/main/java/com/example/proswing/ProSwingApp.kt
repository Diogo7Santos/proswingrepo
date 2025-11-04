package com.example.proswing

import android.app.Application
import android.content.Context

class ProSwingApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        // global context for repositories like SettingsRepository
        lateinit var appContext: Context
            private set
    }
}
