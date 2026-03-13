package com.njm.worker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    val repo = WorkerRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isLoggedIn(this)) { goToLogin(); return }
        setContentView(R.layout.activity_dashboard)
        setupViews()
    }

    private fun setupViews() {
        // Set worker name
        val tvWorkerName = findViewById<TextView>(R.id.tvWorkerName)
        tvWorkerName?.text = SessionManager.getWorkerName(this)

        // Setup ViewPager with 4 tabs
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        val fragments = listOf(
            TodayFragment(),
            MonthFragment(),
            PrintReportFragment(),
            PrintSettingsFragment()
        )
        val titles = listOf(
            "اليوم",
            "الشهر",
            "طباعة التقرير",
            "الإعدادات"
        )

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = titles[pos]
        }.attach()

        // Logout button
        findViewById<Button>(R.id.btnLogout)?.setOnClickListener { doLogout() }
    }

    fun loadStats() {
        // Stats are now shown within each fragment, nothing to do at Activity level
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
