package com.njm.worker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.njm.worker.R
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.ui.login.PinLoginActivity
import com.njm.worker.ui.search.SearchActivity
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private val repo = WorkerRepository()
    private lateinit var tvWorkerName: TextView
    private lateinit var tvTodayCount: TextView
    private lateinit var tvTodayRevenue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isLoggedIn(this)) {
            goToLogin(); return
        }
        setContentView(R.layout.activity_dashboard)
        setupViews()
        loadStats()
    }

    private fun setupViews() {
        tvWorkerName = findViewById(R.id.tvWorkerName)
        tvTodayCount = findViewById(R.id.tvTodayCount)
        tvTodayRevenue = findViewById(R.id.tvTodayRevenue)

        tvWorkerName.text = SessionManager.getWorkerName(this)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        val fragments = listOf(
            TodayFragment(),
            PrintSettingsFragment()
        )
        val titles = listOf("Today", "Print Settings")

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = titles[pos]
        }.attach()

        findViewById<Button>(R.id.btnSearchCar).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<Button>(R.id.btnLogout).setOnClickListener { doLogout() }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    fun loadStats() {
        lifecycleScope.launch {
            val result = repo.getTodayWashes()
            result.onSuccess { data ->
                val washes = data.washes ?: emptyList()
                tvTodayCount.text = washes.size.toString()
                val revenue = washes.sumOf { it.cost ?: 0.0 }
                tvTodayRevenue.text = String.format("%.0f", revenue)
            }
        }
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