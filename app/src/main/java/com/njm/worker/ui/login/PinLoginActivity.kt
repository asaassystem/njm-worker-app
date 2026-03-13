package com.njm.worker.ui.login

import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.databinding.ActivityPinLoginBinding
import com.njm.worker.ui.dashboard.DashboardActivity
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

class PinLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinLoginBinding
    private val repo = WorkerRepository()
    private val pinBuilder = StringBuilder()
    private val MAX_PIN = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already logged in, go to dashboard
        if (SessionManager.isLoggedIn) {
            startDashboard()
            return
        }

        binding = ActivityPinLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNumpad()
    }

    private fun setupNumpad() {
        val numButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )

        numButtons.forEachIndexed { index, button ->
            button.setOnClickListener { appendDigit(index.toString()) }
        }

        binding.btnDelete.setOnClickListener { deleteDigit() }
        binding.btnConfirm.setOnClickListener { submitPin() }
    }

    private fun appendDigit(digit: String) {
        if (pinBuilder.length >= MAX_PIN) return
        pinBuilder.append(digit)
        updatePinDisplay()
        if (pinBuilder.length == 4) submitPin() // Auto-submit on 4 digits
    }

    private fun deleteDigit() {
        if (pinBuilder.isNotEmpty()) {
            pinBuilder.deleteCharAt(pinBuilder.length - 1)
            updatePinDisplay()
        }
    }

    private fun updatePinDisplay() {
        val dots = buildString {
            repeat(pinBuilder.length) { append("● ") }
            repeat(MAX_PIN - pinBuilder.length) { append("○ ") }
        }
        binding.tvPinDots.text = dots.trim()
    }

    private fun submitPin() {
        val pin = pinBuilder.toString()
        if (pin.length < 4) {
            showError("أدخل 4 أرقام على الأقل")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            repo.login(pin)
                .onSuccess {
                    startDashboard()
                }
                .onFailure { error ->
                    setLoading(false)
                    showError(error.message ?: "PIN غير صحيح")
                    vibrate()
                    clearPin()
                }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.numpadLayout.isEnabled = !loading
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun clearPin() {
        pinBuilder.clear()
        updatePinDisplay()
        binding.tvError.visibility = View.GONE
    }

    private fun startDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}