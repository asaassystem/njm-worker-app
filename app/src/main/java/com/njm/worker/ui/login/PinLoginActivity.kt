package com.njm.worker.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.njm.worker.R
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.ui.dashboard.DashboardActivity
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * PinLoginActivity v5.0 - NJM Worker PIN login
 * FIXED: ivAppLogo ID reference (was ivLogo - caused crash)
 * Developer: meshari.tech
 */
class PinLoginActivity : AppCompatActivity() {
    private val repo = WorkerRepository()
    private val pinBuilder = StringBuilder()
    private lateinit var tvPinDisplay: TextView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager.isLoggedIn(this)) { startDashboard(); return }
        setContentView(R.layout.activity_pin_login)
        // FIXED: correct ID is ivAppLogo (not ivLogo)
        val ivAppLogo = findViewById<ImageView>(R.id.ivAppLogo)
        tvPinDisplay = findViewById(R.id.tvPinDisplay)
        tvStatus = findViewById(R.id.tvStatus)
        Glide.with(this).load("https://njm.company/static/img/logo.png")
            .placeholder(R.drawable.njm_logo).error(R.drawable.njm_logo).into(ivAppLogo)
        setupNumpad()
    }

    private fun setupNumpad() {
        val digits = mapOf(R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8", R.id.btn9 to "9")
        digits.forEach { (id, digit) ->
            findViewById<View>(id)?.setOnClickListener {
                if (pinBuilder.length < 4) {
                    pinBuilder.append(digit); updatePinDisplay()
                    if (pinBuilder.length == 4) performLogin()
                }
            }
        }
        findViewById<View>(R.id.btnBackspace)?.setOnClickListener {
            if (pinBuilder.isNotEmpty()) { pinBuilder.deleteCharAt(pinBuilder.length - 1); updatePinDisplay() }
        }
        findViewById<View>(R.id.btnClear)?.setOnClickListener {
            pinBuilder.clear(); updatePinDisplay(); tvStatus.text = ""
        }
    }

    private fun updatePinDisplay() {
        tvPinDisplay.text = "●".repeat(pinBuilder.length) + "○".repeat(4 - pinBuilder.length)
    }

    private fun performLogin() {
        val pinHash = sha256(pinBuilder.toString())
        tvStatus.text = "جاري تسجيل الدخول..."; tvStatus.visibility = View.VISIBLE
        setButtonsEnabled(false)
        lifecycleScope.launch {
            try {
                repo.loginWithPin(pinHash)
                    .onSuccess { data ->
                        if (data.success) {
                            SessionManager.saveSession(this@PinLoginActivity,
                                data.workerId ?: 0, data.workerName ?: "",
                                data.orgName ?: "", data.orgId ?: 0)
                            startDashboard()
                        } else showError(data.message ?: "رمز PIN غير صحيح")
                    }
                    .onFailure { showError("خطأ في الاتصال") }
            } catch (e: Exception) { showError("خطأ غير متوقع") }
        }
    }

    private fun showError(msg: String) {
        pinBuilder.clear(); updatePinDisplay()
        tvStatus.text = msg; tvStatus.visibility = View.VISIBLE; setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        listOf(R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
               R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnBackspace, R.id.btnClear)
            .forEach { id -> findViewById<View>(id)?.isEnabled = enabled }
    }

    private fun startDashboard() { startActivity(Intent(this, DashboardActivity::class.java)); finish() }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
