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
import com.aspire.ubinex.SetupPage
import com.aspire.ubinex.databinding.ActivityOtpauthBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit


class OTPAuth : AppCompatActivity() {
    private lateinit var binding: ActivityOtpauthBinding
    private lateinit var phoneNumberToCheck : String
    private lateinit var auth : FirebaseAuth
    private lateinit var otp : String
    private lateinit var fireStoreDB : FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpauthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        timer.start()

        phoneNumberToCheck = intent.getStringExtra("phoneNumber")!!
        otp = intent.getStringExtra("otp").toString()
        //reToken = intent.getStringExtra("resendToken")


        binding.numberTv.text = phoneNumberToCheck


        window.setFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding.resendOtp.setOnClickListener {
            binding.resendOtp.visibility = View.GONE
            binding.timer.visibility = View.VISIBLE

            resendCode()

        }

        binding.wrongNumber.setOnClickListener {
            val i = Intent(this@OTPAuth, Login::class.java)
            startActivity(i)
            finish()
        }

        binding.loginBtn.setOnClickListener{
            val enteredOtp = binding.otpEdt.text.toString()
            if(enteredOtp.isEmpty() and (enteredOtp.length != 6)){
                Toast.makeText(this, "Please enter correct OTP",Toast.LENGTH_SHORT).show()
            }else {
                binding.progressBar2.visibility = View.VISIBLE
                val credential = PhoneAuthProvider.getCredential(otp, enteredOtp)
                signInWithPhoneAuthCredential(credential)
            }
        }


        if (auth.currentUser != null) {
            // User is signed in, check if the user exists in Firestore
            val uid = auth.currentUser!!.uid
            val userRef = fireStoreDB.collection("users").document(uid)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Existing user found, jump to MainActivity
                        val intent = Intent(this@OTPAuth, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // New user, continue with setup
                        Toast.makeText(this,"Welcome New User",Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error checking user existence: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

    }

    private fun resendCode() {
        timer.start()
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumberToCheck)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(p0: PhoneAuthCredential) {
            TODO("Not yet implemented")
        }

        override fun onVerificationFailed(p0: FirebaseException) {
            TODO("Not yet implemented")
        }

        override fun onCodeSent(code: String, resendToken: PhoneAuthProvider.ForceResendingToken) {

            Toast.makeText(applicationContext, "sms has been sent to $phoneNumberToCheck" ,Toast.LENGTH_SHORT).show()
            otp = code
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {

        auth.signInWithCredential(credential).addOnCompleteListener{task ->
            if(task.isSuccessful){
                binding.progressBar2.visibility = View.GONE
                Toast.makeText(this, "Login Success",Toast.LENGTH_SHORT).show()
                val i = Intent(this@OTPAuth, SetupPage::class.java)
                startActivity(i)
                finish()
            }else{
                if(task.exception is FirebaseAuthInvalidCredentialsException){
                    binding.progressBar2.visibility = View.GONE
                    Toast.makeText(this, "LogIn Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }



    private val timer = object : CountDownTimer(60000, 1000) { // 60 seconds countdown
        override fun onTick(millisUntilFinished: Long) {
            val secondsRemaining = millisUntilFinished / 1000
            // Update UI with remaining time
            binding.timer.text = getString(R.string.timer_format, secondsRemaining)
        }

        override fun onFinish() {
            // Handle timeout (if needed)
            binding.timer.visibility = View.INVISIBLE
            binding.resendOtp.visibility = View.VISIBLE
        }
    }

}