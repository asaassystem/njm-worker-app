package com.njm.worker.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
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
 * PinLoginActivity v6.0 - NJM Worker PIN login
 * FIXED: Use correct XML view IDs (dot1-4, tvError, btnDelete, btnLogin)
 * Developer: meshari.tech
 */
class PinLoginActivity : AppCompatActivity() {

    private val repo = WorkerRepository()
    private val pinBuilder = StringBuilder()

    // PIN dot views - matched to activity_pin_login.xml
    private lateinit var dot1: TextView
    private lateinit var dot2: TextView
    private lateinit var dot3: TextView
    private lateinit var dot4: TextView
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager.isLoggedIn(this)) {
            startDashboard()
            return
        }
        setContentView(R.layout.activity_pin_login)

        val ivAppLogo = findViewById<ImageView>(R.id.ivAppLogo)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
        dot4 = findViewById(R.id.dot4)
        tvError = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)

        Glide.with(this)
            .load("https://njm.company/static/img/logo.png")
            .placeholder(R.drawable.njm_logo)
            .error(R.drawable.njm_logo)
            .into(ivAppLogo)

        setupNumpad()
        updatePinDots()
    }

    private fun setupNumpad() {
        val digits = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3",
            R.id.btn4 to "4", R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7",
            R.id.btn8 to "8", R.id.btn9 to "9"
        )
        digits.forEach { (id, digit) ->
            findViewById<View>(id)?.setOnClickListener {
                if (pinBuilder.length < 4) {
                    pinBuilder.append(digit)
                    updatePinDots()
                    tvError.visibility = View.GONE
                    if (pinBuilder.length == 4) performLogin()
                }
            }
        }

        // btnDelete = backspace (XML id: btnDelete)
        findViewById<View>(R.id.btnDelete)?.setOnClickListener {
            if (pinBuilder.isNotEmpty()) {
                pinBuilder.deleteCharAt(pinBuilder.length - 1)
                updatePinDots()
                tvError.visibility = View.GONE
            }
        }

        // btnLogin = manual login trigger (XML id: btnLogin)
        // Auto-login fires at 4 digits, but btnLogin allows manual confirm
        findViewById<View>(R.id.btnLogin)?.setOnClickListener {
            if (pinBuilder.length == 4) performLogin()
        }
    }

    // Update the 4 dot TextViews using filled/empty circle chars
    private fun updatePinDots() {
        val filled = "●"  // filled circle
        val empty  = "○"  // empty circle
        val dots = listOf(dot1, dot2, dot3, dot4)
        dots.forEachIndexed { index, tv ->
            tv.text = if (index < pinBuilder.length) filled else empty
        }
    }

    private fun performLogin() {
        val pinHash = sha256(pinBuilder.toString())
        tvError.text = "جاري تسجيل الدخول..."
        tvError.setTextColor(android.graphics.Color.parseColor("#c9a227"))
        tvError.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        setButtonsEnabled(false)

        lifecycleScope.launch {
            try {
                repo.login(pinHash)
                    .onSuccess { data ->
                        progressBar.visibility = View.GONE
                        if (data.success) {
                            SessionManager.saveWorker(
                                this@PinLoginActivity,
                                data.workerId ?: 0,
                                data.workerName ?: "",
                                data.orgId ?: 0,
                                pinBuilder.toString()
                            )
                            startDashboard()
                        } else {
                            showError(data.message ?: "رمز PIN غير صحيح")
                        }
                    }
                    .onFailure {
                        progressBar.visibility = View.GONE
                        showError("خطأ في الاتصال")
                    }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                showError("خطأ غير متوقع")
            }
        }
    }

    private fun showError(msg: String) {
        pinBuilder.clear()
        updatePinDots()
        tvError.text = msg
        tvError.setTextColor(android.graphics.Color.parseColor("#ff5252"))
        tvError.visibility = View.VISIBLE
        setButtonsEnabled(true)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3,
            R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7,
            R.id.btn8, R.id.btn9, R.id.btnDelete, R.id.btnLogin
        ).forEach { id -> findViewById<View>(id)?.isEnabled = enabled }
    }

    private fun startDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
