package com.aspire.ubinex.auth

import com.aspire.ubinex.MainActivity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aspire.ubinex.R
import com.aspire.ubinex.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.hbb20.CountryCodePicker.OnCountryChangeListener
import java.util.concurrent.TimeUnit


class Login : AppCompatActivity() {
    private lateinit var binding : ActivityLoginBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var countryCode : String
    private var doubleBackPressed = false
    private lateinit var phoneNumberForOTP : String
    lateinit var codeSent : String

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

        countryCode = binding.ccp.selectedCountryCodeWithPlus
        binding.ccp.setOnCountryChangeListener(OnCountryChangeListener {
            countryCode = binding.ccp.selectedCountryCodeWithPlus
        })

        binding.sendOtpBtn.setOnClickListener{
            phoneNumberForOTP = binding.phoneNumberEdt.text.toString()
            if(phoneNumberForOTP.isEmpty()){
                Toast.makeText(applicationContext, "Phone number can not be empty" ,Toast.LENGTH_SHORT).show()
            }else{
                binding.progressBar.visibility = View.VISIBLE
                phoneNumberForOTP = countryCode + phoneNumberForOTP

                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumberForOTP)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(callbacks)
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)
            }
        }
    }

    private val callbacks = object : OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(p0: PhoneAuthCredential) {
            TODO("Not yet implemented")
        }

        override fun onVerificationFailed(p0: FirebaseException) {
            TODO("Not yet implemented")
        }

        override fun onCodeSent(code: String, resendToken: PhoneAuthProvider.ForceResendingToken) {

            Toast.makeText(applicationContext, "sms has been sent to $phoneNumberForOTP" ,Toast.LENGTH_SHORT).show()
            codeSent = code
            val i = Intent(this@Login, OTPAuth::class.java)
            i.putExtra("phoneNumber",phoneNumberForOTP)
            i.putExtra("otp", codeSent)
            i.putExtra("resendToken", resendToken.toString())
            startActivity(i)
            finish()

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

    override fun onStart() {
        super.onStart()
        if(auth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}