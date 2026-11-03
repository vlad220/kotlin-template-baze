package com.template

import android.app.Application
import com.template.util.Notifier

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
    }
}