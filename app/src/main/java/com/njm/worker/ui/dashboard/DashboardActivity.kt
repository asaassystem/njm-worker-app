package com.njm.worker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.njm.worker.R
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.ui.login.PinLoginActivity
import com.njm.worker.utils.LangManager
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    val repo = WorkerRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isLoggedIn(this)) { goToLogin(); return }
        // Apply saved language
        LangManager.applyLanguage(this, SessionManager.getLang(this))
        setContentView(R.layout.activity_dashboard)
        setupViews()
    }

    private fun setupViews() {
        // Worker name
        val tvWorkerName = findViewById<TextView>(R.id.tvWorkerName)
        tvWorkerName?.text = SessionManager.getWorkerName(this)

        // Logo
        val ivLogo = findViewById<ImageView>(R.id.ivLogo)
        ivLogo?.setImageResource(R.drawable.njm_logo)

        // Developer credit
        val tvDev = findViewById<TextView>(R.id.tvDeveloper)
        tvDev?.text = "meshari.tech"

        // Tabs: تسجيل غسيل | اليوم | الشهر | الفواتير | الإعدادات
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        val fragments = listOf(
            NewWashFragment(),
            TodayFragment(),
            MonthFragment(),
            InvoicesFragment(),
            PrintSettingsFragment()
        )

        val tabData = listOf(
            Pair(getLangString("tab_new_wash"), R.drawable.ic_wash),
            Pair(getLangString("tab_today"), R.drawable.ic_today),
            Pair(getLangString("tab_month"), R.drawable.ic_month),
            Pair(getLangString("tab_invoices"), R.drawable.ic_invoice),
            Pair(getLangString("tab_settings"), R.drawable.ic_settings)
        )

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = tabData[pos].first
        }.attach()

        // Default to first tab (new wash)
        viewPager.setCurrentItem(0, false)

        // Logout
        findViewById<Button>(R.id.btnLogout)?.setOnClickListener { doLogout() }

        // Language switcher
        setupLanguageSwitcher()
    }

    private fun getLangString(key: String): String {
        val lang = SessionManager.getLang(this)
        return when (key) {
            "tab_new_wash" -> when (lang) { "en" -> "New Wash"; "bn" -> "নতুন ওয়াশ"; else -> "تسجيل غسيل" }
            "tab_today" -> when (lang) { "en" -> "Today"; "bn" -> "আজ"; else -> "اليوم" }
            "tab_month" -> when (lang) { "en" -> "Month"; "bn" -> "মাস"; else -> "الشهر" }
            "tab_invoices" -> when (lang) { "en" -> "Invoices"; "bn" -> "ইনভয়েস"; else -> "الفواتير" }
            "tab_settings" -> when (lang) { "en" -> "Settings"; "bn" -> "সেটিংস"; else -> "الإعدادات" }
            else -> key
        }
    }

    private fun setupLanguageSwitcher() {
        val btnAr = findViewById<Button>(R.id.btnLangAr)
        val btnEn = findViewById<Button>(R.id.btnLangEn)
        val btnBn = findViewById<Button>(R.id.btnLangBn)

        btnAr?.setOnClickListener { switchLanguage("ar") }
        btnEn?.setOnClickListener { switchLanguage("en") }
        btnBn?.setOnClickListener { switchLanguage("bn") }
    }

    private fun switchLanguage(lang: String) {
        SessionManager.setLang(this, lang)
        lifecycleScope.launch {
            try { repo.updateLanguage(lang) } catch (_: Exception) {}
        }
        // Restart activity to apply language
        recreate()
    }

    fun loadStats() {
        // Stats loaded within each fragment
    }

    override fun onResume() {
        super.onResume()
    }

    private fun doLogout() {
        lifecycleScope.launch {
            repo.logout()
            SessionManager.logout(this@DashboardActivity)
            goToLogin()
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, PinLoginActivity::class.java))
        finish()
    }
}
