package com.njm.worker.utils

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "njm_worker_prefs"
    private const val KEY_WORKER_ID = "worker_id"
    private const val KEY_WORKER_NAME = "worker_name"
    private const val KEY_ORG_ID = "org_id"
    private const val KEY_PIN = "worker_pin"
    private const val KEY_LOGGED_IN = "is_logged_in"
    private const val KEY_LANG = "app_lang"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveWorker(context: Context, workerId: Int, workerName: String, orgId: Int, pin: String) {
        prefs(context).edit().apply {
            putInt(KEY_WORKER_ID, workerId)
            putString(KEY_WORKER_NAME, workerName)
            putInt(KEY_ORG_ID, orgId)
            putString(KEY_PIN, pin)
            putBoolean(KEY_LOGGED_IN, true)
            apply()
        }
    }

    fun isLoggedIn(context: Context): Boolean = prefs(context).getBoolean(KEY_LOGGED_IN, false)
    fun getWorkerId(context: Context): Int = prefs(context).getInt(KEY_WORKER_ID, 0)
    fun getWorkerName(context: Context): String = prefs(context).getString(KEY_WORKER_NAME, "") ?: ""
    fun getOrgId(context: Context): Int = prefs(context).getInt(KEY_ORG_ID, 0)
    fun getPin(context: Context): String = prefs(context).getString(KEY_PIN, "") ?: ""

    fun getLang(context: Context): String = prefs(context).getString(KEY_LANG, "ar") ?: "ar"
    fun setLang(context: Context, lang: String) {
        prefs(context).edit().putString(KEY_LANG, lang).apply()
    }

    fun logout(context: Context) {
        val lang = getLang(context) // preserve lang on logout
        prefs(context).edit().clear().putString(KEY_LANG, lang).apply()
    }
}
