package com.aspire.ubinex.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.aspire.ubinex.MainActivity
import com.aspire.ubinex.R
import com.aspire.ubinex.databinding.ActivityOtpauthBinding

class OTPAuth : AppCompatActivity() {
    private lateinit var binding: ActivityOtpauthBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpauthBinding.inflate(layoutInflater)
        setContentView(binding.root)


//        enableEdgeToEdge()
//        setContentView(R.layout.activity_otpauth)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding.loginBtn.setOnClickListener{
            val i = Intent(this@OTPAuth, MainActivity::class.java)
            startActivity(i)
            finish()
        }

    }
}