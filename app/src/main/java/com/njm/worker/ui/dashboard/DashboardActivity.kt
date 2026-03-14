package com.njm.worker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.njm.worker.R
import com.njm.worker.ui.login.PinLoginActivity
import com.njm.worker.utils.SessionManager

/**
 * DashboardActivity - NJM Worker App Main Dashboard
 * Navy/Gold design with NJM logo in header
 * Developer: meshari.tech
 */
class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var navigationView: NavigationView
    private lateinit var headerLogo: ImageView
    private lateinit var headerWorkerName: TextView
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        session = SessionManager(this)
        if (!session.isLoggedIn()) {
            logout()
            return
        }

        initViews()
        setupNavigation()
        loadWorkerInfo()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(NewWashFragment())
            bottomNav.selectedItemId = R.id.nav_new_wash
        }
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        bottomNav = findViewById(R.id.bottom_navigation)
        navigationView = findViewById(R.id.nav_view)

        // Header views in navigation drawer
        val headerView = navigationView.getHeaderView(0)
        headerLogo = headerView.findViewById(R.id.iv_header_logo)
        headerWorkerName = headerView.findViewById(R.id.tv_worker_name)

        // Toolbar logo
        val toolbarLogo = findViewById<ImageView>(R.id.iv_toolbar_logo)
        loadLogoInto(toolbarLogo)
    }

    private fun loadLogoInto(imageView: ImageView?) {
        imageView ?: return
        Glide.with(this)
            .load("https://njm.company/static/img/logo.png")
            .placeholder(R.drawable.ic_logo_placeholder)
            .error(R.drawable.ic_logo_placeholder)
            .into(imageView)
    }

    private fun setupNavigation() {
        // Bottom navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_new_wash -> { loadFragment(NewWashFragment()); true }
                R.id.nav_today -> { loadFragment(TodayFragment()); true }
                R.id.nav_month -> { loadFragment(MonthFragment()); true }
                R.id.nav_print_settings -> { loadFragment(PrintSettingsFragment()); true }
                else -> false
            }
        }

        // Navigation drawer
        navigationView.setNavigationItemSelectedListener(this)

        // Hamburger menu
        findViewById<View>(R.id.btn_menu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun loadWorkerInfo() {
        val workerName = session.getWorkerName()
        headerWorkerName.text = workerName.ifEmpty { getString(R.string.worker) }
        loadLogoInto(headerLogo)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_new_wash -> {
                loadFragment(NewWashFragment())
                bottomNav.selectedItemId = R.id.nav_new_wash
            }
            R.id.menu_today -> {
                loadFragment(TodayFragment())
                bottomNav.selectedItemId = R.id.nav_today
            }
            R.id.menu_month -> {
                loadFragment(MonthFragment())
                bottomNav.selectedItemId = R.id.nav_month
            }
            R.id.menu_print_settings -> {
                loadFragment(PrintSettingsFragment())
                bottomNav.selectedItemId = R.id.nav_print_settings
            }
            R.id.menu_logout -> logout()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logout() {
        session.logout()
        startActivity(Intent(this, PinLoginActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
