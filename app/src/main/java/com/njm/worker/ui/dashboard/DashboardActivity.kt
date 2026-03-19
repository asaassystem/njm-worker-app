package com.njm.worker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.njm.worker.R
import com.njm.worker.data.api.AppCookieJar
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.ui.login.PinLoginActivity
import com.njm.worker.utils.LangManager
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * DashboardActivity - NJM Worker App Main Dashboard
 * Design v2: NJM logo in header via Glide
 * Fix: doLogout clears AppCookieJar + SessionManager
 * v6.4: Added refreshWashData() so NewWashFragment can notify Today/Month to reload
 * Developer: meshari.tech
 */
class DashboardActivity : AppCompatActivity() {
    val repo = WorkerRepository()
    private val fragments = mutableListOf<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isLoggedIn(this)) {
            goToLogin()
            return
        }
        // Apply saved language
        LangManager.applyLanguage(this, SessionManager.getLang(this))
        setContentView(R.layout.activity_dashboard)
        setupViews()
    }

    /** Called by NewWashFragment after a wash is successfully recorded */
    fun refreshWashData() {
        // Find TodayFragment (index 1) and MonthFragment (index 2) and refresh them
        fragments.getOrNull(1)?.let { (it as? TodayFragment)?.forceRefresh() }
        fragments.getOrNull(2)?.let { (it as? MonthFragment)?.forceRefresh() }
    }

    private fun setupViews() {
        // Load NJM logo if ImageView exists
        try {
            val ivLogo = findViewById<ImageView>(R.id.ivLogo)
            if (ivLogo != null) {
                Glide.with(this)
                    .load("https://njm.company/static/img/logo.png")
                    .into(ivLogo)
            }
        } catch (e: Exception) { /* logo view optional */ }

        // Worker name
        val tvWorkerName = findViewById<TextView>(R.id.tvWorkerName)
        tvWorkerName?.text = SessionManager.getWorkerName(this)

        // Developer credit
        val tvDev = findViewById<TextView>(R.id.tvDeveloper)
        tvDev?.text = "meshari.tech"

        // Tabs
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        fragments.clear()
        fragments.addAll(listOf(
            NewWashFragment(),
            TodayFragment(),
            MonthFragment(),
            InvoicesFragment(),
            PrintSettingsFragment()
        ))

        val tabTitles = listOf(
            getLangString("tab_new_wash"),
            getLangString("tab_today"),
            getLangString("tab_month"),
            getLangString("tab_invoices"),
            getLangString("tab_settings")
        )

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = tabTitles[pos]
        }.attach()

        viewPager.setCurrentItem(0, false)

        // Logout button - clears session AND cookies
        findViewById<Button>(R.id.btnLogout)?.setOnClickListener { doLogout() }

        setupLanguageSwitcher()
    }

    private fun getLangString(key: String): String {
        val lang = SessionManager.getLang(this)
        return when (key) {
            "tab_new_wash" -> when (lang) {
                "en" -> "New Wash"; "bn" -> "\u09a8\u09a4\u09c1\u09a8 \u0993\u09af\u09bc\u09be\u09b6"; else -> "\u062a\u0633\u062c\u064a\u0644 \u063a\u0633\u064a\u0644"
            }
            "tab_today" -> when (lang) {
                "en" -> "Today"; "bn" -> "\u0986\u099c"; else -> "\u0627\u0644\u064a\u0648\u0645"
            }
            "tab_month" -> when (lang) {
                "en" -> "Month"; "bn" -> "\u09ae\u09be\u09b8"; else -> "\u0627\u0644\u0634\u0647\u0631"
            }
            "tab_invoices" -> when (lang) {
                "en" -> "Invoices"; "bn" -> "\u0987\u09a8\u09ad\u09af\u09bc\u09c7\u09b8"; else -> "\u0627\u0644\u0641\u0648\u0627\u062a\u064a\u0631"
            }
            "tab_settings" -> when (lang) {
                "en" -> "Settings"; "bn" -> "\u09b8\u09c7\u099f\u09bf\u0982\u09b8"; else -> "\u0627\u0644\u0625\u0639\u062f\u0627\u062f\u0627\u062a"
            }
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
        recreate()
    }

    private fun doLogout() {
        lifecycleScope.launch {
            try { repo.logout() } catch (_: Exception) {}
            // Clear session cookies so next login is clean
            AppCookieJar.clear()
            SessionManager.logout(this@DashboardActivity)
            goToLogin()
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, PinLoginActivity::class.java))
        finish()
    }
}
