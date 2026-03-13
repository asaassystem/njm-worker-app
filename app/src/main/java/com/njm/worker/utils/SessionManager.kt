package com.njm.worker.utils

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("njm_session", Context.MODE_PRIVATE)
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("logged_in", false)
        set(value) = prefs.edit().putBoolean("logged_in", value).apply()

    var workerName: String
        get() = prefs.getString("worker_name", "") ?: ""
        set(value) = prefs.edit().putString("worker_name", value).apply()

    var orgId: Int
        get() = prefs.getInt("org_id", 0)
        set(value) = prefs.edit().putInt("org_id", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}