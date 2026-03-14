package com.njm.worker.ui.login

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.njm.worker.R
import com.njm.worker.ui.dashboard.DashboardActivity
import com.njm.worker.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.njm.worker.data.ApiClient
import com.njm.worker.data.model.LoginRequest
import com.njm.worker.data.model.LoginResponse

/**
 * PinLoginActivity - NJM Worker App Login Screen
 * Navy/Gold design with NJM logo
 * Developer: meshari.tech
 */
class PinLoginActivity : AppCompatActivity() {

    private val enteredPin = StringBuilder()
    private val pinDots = mutableListOf<View>()
    private lateinit var statusText: TextView
    private lateinit var logoImage: ImageView
    private lateinit var pinContainer: View
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_login)

        // Check if already logged in
        val session = SessionManager(this)
        if (session.isLoggedIn()) {
            startDashboard()
            return
        }

        initViews()
        loadLogo()
        animateEntrance()
    }

    private fun initViews() {
        logoImage = findViewById(R.id.iv_logo)
        statusText = findViewById(R.id.tv_status)
        pinContainer = findViewById(R.id.pin_dots_container)

        // Collect pin dot views
        pinDots.clear()
        pinDots.add(findViewById(R.id.dot_1))
        pinDots.add(findViewById(R.id.dot_2))
        pinDots.add(findViewById(R.id.dot_3))
        pinDots.add(findViewById(R.id.dot_4))

        // Setup numpad buttons
        val numIds = mapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2",
            R.id.btn_3 to "3", R.id.btn_4 to "4", R.id.btn_5 to "5",
            R.id.btn_6 to "6", R.id.btn_7 to "7", R.id.btn_8 to "8",
            R.id.btn_9 to "9"
        )
        numIds.forEach { (id, digit) ->
            findViewById<View>(id)?.setOnClickListener { onDigitPressed(digit) }
        }

        // Backspace button
        findViewById<View>(R.id.btn_backspace)?.setOnClickListener { onBackspace() }
    }

    private fun loadLogo() {
        Glide.with(this)
            .load("https://njm.company/static/img/logo.png")
            .placeholder(R.drawable.ic_logo_placeholder)
            .error(R.drawable.ic_logo_placeholder)
            .into(logoImage)
    }

    private fun animateEntrance() {
        logoImage.alpha = 0f
        logoImage.scaleX = 0.7f
        logoImage.scaleY = 0.7f

        val fadeIn = ObjectAnimator.ofFloat(logoImage, "alpha", 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0.7f, 1f)
        val scaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0.7f, 1f)

        val set = AnimatorSet()
        set.playTogether(fadeIn, scaleX, scaleY)
        set.duration = 800
        set.interpolator = AccelerateDecelerateInterpolator()
        set.start()

        // Slide up pin container
        pinContainer.translationY = 200f
        pinContainer.alpha = 0f
        pinContainer.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun onDigitPressed(digit: String) {
        if (enteredPin.length >= 4) return

        enteredPin.append(digit)
        updatePinDots()

        // Animate the dot
        val dot = pinDots[enteredPin.length - 1]
        dot.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction {
            dot.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()

        if (enteredPin.length == 4) {
            handler.postDelayed({ verifyPin() }, 200)
        }
    }

    private fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.length - 1)
            updatePinDots()
            statusText.text = ""
        }
    }

    private fun updatePinDots() {
        pinDots.forEachIndexed { index, dot ->
            if (index < enteredPin.length) {
                dot.setBackgroundResource(R.drawable.pin_dot_filled)
            } else {
                dot.setBackgroundResource(R.drawable.pin_dot_empty)
            }
        }
    }

    private fun verifyPin() {
        val pin = enteredPin.toString()
        statusText.text = getString(R.string.verifying)
        statusText.setTextColor(ContextCompat.getColor(this, R.color.gold_light))

        ApiClient.apiService.login(LoginRequest(pin))
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val session = SessionManager(this@PinLoginActivity)
                        response.body()?.let {
                            session.saveSession(it.worker_id ?: 0, it.name ?: "", pin)
                        }
                        onLoginSuccess()
                    } else {
                        onLoginFailed()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    onLoginFailed()
                }
            })
    }

    private fun onLoginSuccess() {
        statusText.text = getString(R.string.login_success)
        statusText.setTextColor(ContextCompat.getColor(this, R.color.success_green))

        // Success animation
        pinDots.forEach { dot ->
            dot.setBackgroundResource(R.drawable.pin_dot_success)
        }

        handler.postDelayed({ startDashboard() }, 600)
    }

    private fun onLoginFailed() {
        statusText.text = getString(R.string.invalid_pin)
        statusText.setTextColor(ContextCompat.getColor(this, R.color.error_red))

        // Shake animation
        val shake = ObjectAnimator.ofFloat(pinContainer, "translationX",
            0f, -20f, 20f, -20f, 20f, -10f, 10f, 0f)
        shake.duration = 500
        shake.start()

        // Reset dots to error state
        pinDots.forEach { dot ->
            dot.setBackgroundResource(R.drawable.pin_dot_error)
        }

        handler.postDelayed({
            enteredPin.clear()
            updatePinDots()
            statusText.text = ""
        }, 1000)
    }

    private fun startDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
