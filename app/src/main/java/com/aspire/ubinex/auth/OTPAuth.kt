package com.aspire.ubinex.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aspire.ubinex.MainActivity
import com.aspire.ubinex.R
import com.aspire.ubinex.databinding.ActivityOtpauthBinding

class OTPAuth : AppCompatActivity() {
    private lateinit var binding: ActivityOtpauthBinding
    private var doubleBackPressed = false
    private lateinit var phoneNumber:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpauthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        phoneNumber =   intent.getStringExtra("Phone").toString()
        binding.numberTv.text = phoneNumber
        Toast.makeText(this,phoneNumber,Toast.LENGTH_SHORT).show()
        timer.start()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding.resendOtp.setOnClickListener {
            binding.resendOtp.visibility = View.GONE
            binding.timer.visibility = View.VISIBLE
            timer.start()
        }

        binding.wrongNumber.setOnClickListener {
            timer.cancel()
            val i = Intent(this@OTPAuth, Login::class.java)
            startActivity(i)
            finish()
        }

        binding.loginBtn.setOnClickListener{

            timer.cancel()
            if(binding.resendOtp.visibility != View.GONE){
                binding.resendOtp.visibility =View.GONE
            }

            val i = Intent(this@OTPAuth, MainActivity::class.java)
            startActivity(i)
            finish()
        }

    }

    private val timer = object :CountDownTimer(90000, 1000){
        override fun onTick(millisUntilFinished: Long) {
            val remainingTime = millisUntilFinished / 1000
            binding.timer.text = getString(R.string.timer_format, remainingTime)
        }

        override fun onFinish() {
            binding.resendOtp.visibility = View.VISIBLE
            binding.timer.visibility = View.GONE
        }

    }

    override fun onBackPressed() {
        if (doubleBackPressed) {
            super.onBackPressed()
            return
        }

        doubleBackPressed = true
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

        window.decorView.postDelayed({
            doubleBackPressed = false
        }, 3000)
    }
}