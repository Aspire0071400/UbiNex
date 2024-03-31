package com.aspire.ubinex.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aspire.ubinex.R
import com.aspire.ubinex.SetupPage
import com.aspire.ubinex.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit


class Login : AppCompatActivity() {
    private lateinit var binding : ActivityLoginBinding
    private lateinit var auth : FirebaseAuth
    private var doubleBackPressed = false
    private lateinit var phoneNumberForOTP : String
    lateinit var tkn : PhoneAuthProvider.ForceResendingToken

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
            phoneNumberForOTP = binding.ccp.formattedFullNumber.toString()
            binding.progressBar.visibility = View.VISIBLE
            if(!binding.ccp.isValidFullNumber){
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this,"Enter a Valid Number!",Toast.LENGTH_SHORT).show()
            }else if(binding.ccp.isValidFullNumber){
                binding.progressBar.visibility = View.VISIBLE
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumberForOTP)
                    .setTimeout(90L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(callbacks)
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)

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

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {

            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {

            when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    // Invalid request
                    Toast.makeText(this@Login,"$e 1",Toast.LENGTH_SHORT).show()
                }

                is FirebaseTooManyRequestsException -> {
                    // The SMS quota for the project has been exceeded
                    Toast.makeText(this@Login,"$e 2",Toast.LENGTH_SHORT).show()
                }

                is FirebaseAuthMissingActivityForRecaptchaException -> {
                    // reCAPTCHA verification attempted with null Activity
                    Toast.makeText(this@Login,"$e 3",Toast.LENGTH_SHORT).show()
                }
            }

        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken ) {
            Toast.makeText(this@Login,"OTP has been sent to your number",Toast.LENGTH_SHORT).show()


            val i = Intent(this@Login, OTPAuth::class.java)
            i.putExtra("OTP", verificationId)
            i.putExtra("Phone", binding.ccp.formattedFullNumber)
            i.putExtra("resendToken", token)
            startActivity(i)

        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(this,"Login successful",Toast.LENGTH_SHORT).show()
                    sendToMainScreen()

                } else {
                    // Sign in failed, display a message and update the UI

                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        Toast.makeText(this, "Wrong OTP Entered",Toast.LENGTH_SHORT).show()
                    }
                    // Update UI
                }
            }
    }

    private fun sendToMainScreen(){
        startActivity(Intent(this,SetupPage::class.java))
    }

    override fun onStart() {
        super.onStart()
        if(auth.currentUser != null){
            startActivity(Intent(this,SetupPage::class.java))
        }
    }


}