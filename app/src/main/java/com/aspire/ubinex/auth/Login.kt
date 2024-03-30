package com.aspire.ubinex.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aspire.ubinex.R
import com.aspire.ubinex.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth


class Login : AppCompatActivity() {
    private lateinit var binding : ActivityLoginBinding
    private lateinit var auth : FirebaseAuth
    private var doubleBackPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR


        binding.ccp.registerCarrierNumberEditText(binding.phoneNumberEdt)

        binding.sendOtpBtn.setOnClickListener{
            binding.progressBar.visibility = View.VISIBLE
            if(!binding.ccp.isValidFullNumber){
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this,"Enter a Valid Number!",Toast.LENGTH_SHORT).show()
            }else if(binding.ccp.isValidFullNumber){
                binding.progressBar.visibility = View.VISIBLE
                val i = Intent(this@Login, OTPAuth::class.java)
                i.putExtra("Phone",binding.ccp.formattedFullNumber)
                startActivity(i)
                finish()
            }
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