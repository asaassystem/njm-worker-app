package com.njm.worker.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LangManager {

    fun applyLanguage(context: Context, lang: String): Context {
        val locale = when (lang) {
            "en" -> Locale.ENGLISH
            "bn" -> Locale("bn")
            else -> Locale("ar")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getSupportedLanguages(): List<Pair<String, String>> = listOf(
        Pair("ar", "العربية"),
        Pair("en", "English"),
        Pair("bn", "বাংলা")
    )
}
