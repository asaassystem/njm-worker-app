package com.njm.worker.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.njm.worker.R
import com.njm.worker.data.api.ApiClient
import com.njm.worker.data.model.LoginRequest
import com.njm.worker.ui.dashboard.DashboardActivity
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * PinLoginActivity - NJM Worker App Login Screen
 * Design v2: NJM logo loaded via Glide
 * Developer: meshari.tech
 */
class PinLoginActivity : AppCompatActivity() {

    private var pin = StringBuilder()
    private lateinit var dots: Array<TextView>
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

                    val hasPinHash = SessionManager.getStoredPinHash(this).isNotEmpty()
                    // Auto-login: if session is marked logged in and we have a PIN hash, go to dashboard
                                // The API session cookie handles auth; we don't need to re-send PIN
                                            if (SessionManager.isLoggedIn(this) && hasPinHash) {
            setContentView(R.layout.activity_pin_login)
            initViews()
            loadLogo()
            progressBar.visibility = View.VISIBLE
            tvError.visibility = View.GONE
            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.loginWithPin(LoginRequest(pin = savedPin))
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()!!
                        SessionManager.saveWorker(
                            this@PinLoginActivity,
                            body.workerId ?: 0,
                            body.workerName ?: "",
                            body.orgId ?: 0,
                            savedPin
                        )
                        startDashboard()
                    } else {
                        SessionManager.logout(this@PinLoginActivity)
                        setupButtons()
                        tvError.text = "انتهت الجلسة. أدخل PIN مرة أخرى"
                        tvError.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    progressBar.visibility = View.GONE
                    startDashboard()
                }
            }            // Session cookie valid - go directly to dashboard
                            // Worker info will be refreshed from server on dashboard load
                                            progressBar.visibility = View.VISIBLE
                                                                lifecycleScope.launch {
                                                                                        try {
                                                                                                                    val infoResp = ApiClient.apiService.getWorkerInfo()
                                                                                                                                            progressBar.visibility = View.GONE
                                                                                                                    if (infoResp.isSuccessful && infoResp.body()?.success == true) {
                                                                                                                                                    startDashboard()
                                                                                                                    } else {
                                                                                                                                                    // Session expired - force re-login
                                                                                                                                                    AppCookieJar.clear()
                                                                                                                                                                                SessionManager.logout(this@PinLoginActivity)
                                                                                                                                                                                                            setupButtons()
                                                                                                                                                                                                                                        tvError.text = "انتهت الجلسة. أدخل PIN مرة أخرى"
                                                                                                                                                    tvError.visibility = View.VISIBLE
                                                                                                                    }
                                                                                        } catch (e: Exception) {
                                                                                                                    progressBar.visibility = View.GONE
                                                                                                                    // Network error - still attempt dashboard (offline resilience)
                                                                                                                    startDashboard()
                                                                                        }
                                                                }
                                                                                return
            return
        }

        setContentView(R.layout.activity_pin_login)
        initViews()
        loadLogo()
        setupButtons()
    }

    private fun initViews() {
        dots = arrayOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        )
        tvError = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun loadLogo() {
        try {
            val logoView = findViewById<ImageView>(R.id.ivLogo) ?: return
            com.bumptech.glide.Glide.with(this)
                .load("https://njm.company/static/img/logo.png")
                .into(logoView)
        } catch (e: Exception) {
            // Logo view may not exist in old layout - ignore
        }
    }

    private fun setupButtons() {
        val btnIds = listOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9"
        )
        btnIds.forEach { (id, digit) ->
            findViewById<Button>(id)?.setOnClickListener { addDigit(digit) }
        }
        findViewById<Button>(R.id.btnDelete)?.setOnClickListener { deleteDigit() }
        findViewById<Button>(R.id.btnLogin)?.setOnClickListener { doLogin() }
    }

    private fun addDigit(d: String) {
        if (pin.length >= 4) return
        pin.append(d)
        updateDots()
        if (pin.length == 4) doLogin()
    }

    private fun deleteDigit() {
        if (pin.isNotEmpty()) {
            pin.deleteCharAt(pin.length - 1)
            updateDots()
        }
    }

    private fun updateDots() {
        dots.forEachIndexed { i, dot ->
            dot.text = if (i < pin.length) "●" else "○"
        }
    }

    private fun doLogin() {
        val pinStr = pin.toString()
        if (pinStr.length != 4) {
            tvError.text = "PIN يجب أن يكون 4 أرقام"
            tvError.visibility = View.VISIBLE
            return
        }
        tvError.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.loginWithPin(LoginRequest(pin = pinStr))
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    SessionManager.saveWorker(
                        this@PinLoginActivity,
                        body.workerId ?: 0,
                        body.workerName ?: "",
                        body.orgId ?: 0,
                        pinStr
                    )
                    startDashboard()
                } else {
                    val msg = response.body()?.message ?: "PIN غير صحيح"
                    tvError.text = msg
                    tvError.visibility = View.VISIBLE
                    pin.clear()
                    updateDots()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvError.text = "خطأ في الاتصال: " + (e.message ?: "")
                tvError.visibility = View.VISIBLE
                pin.clear()
                updateDots()
            }
        }
    }

    private fun startDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
