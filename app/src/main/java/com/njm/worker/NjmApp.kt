package com.njm.worker

import android.app.Application
import com.njm.worker.utils.SessionManager

class NjmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}