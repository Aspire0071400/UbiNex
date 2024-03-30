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
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class OTPAuth : AppCompatActivity() {
    private lateinit var binding: ActivityOtpauthBinding
    private lateinit var phoneNumberToCheck : String
    private lateinit var resendToken : PhoneAuthProvider.ForceResendingToken
    private lateinit var auth : FirebaseAuth
    private lateinit var OTP : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpauthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        timer.start()

        phoneNumberToCheck = intent.getStringExtra("Phone")!!
        OTP = intent.getStringExtra("OTP").toString()

        binding.numberTv.text = phoneNumberToCheck


        window.setFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding.resendOtp.setOnClickListener {
            binding.resendOtp.visibility = View.GONE
            binding.timer.visibility = View.VISIBLE

            resendOTP()

        }

        binding.wrongNumber.setOnClickListener {

            val i = Intent(this@OTPAuth, Login::class.java)
            startActivity(i)
            finish()
        }

        binding.loginBtn.setOnClickListener{

            if(binding.resendOtp.visibility != View.GONE){
                binding.resendOtp.visibility =View.GONE
            }

            if(binding.otpEdt.text.isNotEmpty()){
                if(binding.otpEdt.text.length == 6){
                    val credential : PhoneAuthCredential = PhoneAuthProvider.getCredential(OTP,binding.otpEdt.text.toString())
                    signInWithPhoneAuthCredential(credential)
                }else{
                    Toast.makeText(this, "Please Enter correct OTP",Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this, "Please Enter OTP",Toast.LENGTH_SHORT).show()
            }

        }


    }

    private fun resendOTP() {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumberToCheck)
            .setTimeout(90L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        timer.start()
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(this,"Login successful",Toast.LENGTH_SHORT).show()
                    val i = Intent(this@OTPAuth, MainActivity::class.java)
                    startActivity(i)

                } else {
                    // Sign in failed, display a message and update the UI

                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    // Update UI
                }
            }
    }


    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {

            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {

            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                Toast.makeText(this@OTPAuth,"$e",Toast.LENGTH_SHORT).show()
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
                Toast.makeText(this@OTPAuth,"$e",Toast.LENGTH_SHORT).show()
            } else if (e is FirebaseAuthMissingActivityForRecaptchaException) {
                // reCAPTCHA verification attempted with null Activity
                Toast.makeText(this@OTPAuth,"$e",Toast.LENGTH_SHORT).show()
            }

        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken ) {
            OTP = verificationId
            resendToken = token
            Toast.makeText(this@OTPAuth,"OTP has been sent to your number",Toast.LENGTH_SHORT).show()
            timer.cancel()
            timer.start()
        }
    }

    private val timer = object : CountDownTimer(90000, 1000) { // 60 seconds countdown
        override fun onTick(millisUntilFinished: Long) {
            val secondsRemaining = millisUntilFinished / 1000
            // Update UI with remaining time
            binding.timer.text = getString(R.string.timer_format, secondsRemaining)
        }

        override fun onFinish() {
            // Handle timeout (if needed)
            binding.timer.visibility = View.INVISIBLE
        }
    }

}