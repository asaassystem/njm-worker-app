package com.njm.worker.utils

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

object SessionManager {
    private const val PREF_NAME = "njm_worker_session"
    private const val KEY_WORKER_ID = "worker_id"
    private const val KEY_WORKER_NAME = "worker_name"
    private const val KEY_ORG_ID = "org_id"
    private const val KEY_ORG_NAME = "org_name"
    private const val KEY_LOGGED_IN = "is_logged_in"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(workerId: Int, workerName: String, orgId: Int, orgName: String? = null) {
        prefs.edit().apply {
            putInt(KEY_WORKER_ID, workerId)
            putString(KEY_WORKER_NAME, workerName)
            putInt(KEY_ORG_ID, orgId)
            putString(KEY_ORG_NAME, orgName ?: "")
            putBoolean(KEY_LOGGED_IN, true)
            apply()
        }
    }

    fun isLoggedIn() = prefs.getBoolean(KEY_LOGGED_IN, false)
    fun getWorkerId() = prefs.getInt(KEY_WORKER_ID, -1)
    fun getWorkerName() = prefs.getString(KEY_WORKER_NAME, "") ?: ""
    fun getOrgId() = prefs.getInt(KEY_ORG_ID, -1)
    fun getOrgName() = prefs.getString(KEY_ORG_NAME, "") ?: ""

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}