package com.classroomassessment.scanner

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.classroomassessment.scanner.core.AppContainer

class ScannerApplication : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.INFO else Log.WARN)
            .build()
}
