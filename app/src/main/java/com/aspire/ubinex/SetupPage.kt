package com.aspire.ubinex

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aspire.ubinex.databinding.ActivitySetupPageBinding
import com.aspire.ubinex.model.UserDataModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

class SetupPage : AppCompatActivity() {

    private lateinit var binding : ActivitySetupPageBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var database : FirebaseDatabase
    private lateinit var storage : FirebaseStorage
    private lateinit var setupImageUri : Uri
    private lateinit var gendertype : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        val gender = listOf("Gender", "Male", "Female", "transgender", "Others")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gender)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.genderSpinner.adapter = adapter
        gendertype = ""
        window.setFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.black)
        //window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding.genderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                if(selectedItem != "Gender"){
                Toast.makeText(applicationContext, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                gendertype = selectedItem
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //do nothing
            }

        }

        binding.setupImage.setOnClickListener{
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent,45)
        }

        binding.saveSetupProfile.setOnClickListener {
            val name = binding.setupUsernameEdt.text.toString().trim()
            if (gendertype == ""){
                Toast.makeText(this,"Please select a Gender",Toast.LENGTH_SHORT).show()
            }
            if(name.isEmpty()){
                binding.setupUsernameEdt.error = "Username can't be empty"
            }else {
                if (setupImageUri != null) {
                    val reference = storage.reference.child("profile")
                        .child(auth.currentUser!!.uid)
                    reference.putFile(setupImageUri).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            reference.downloadUrl.addOnCompleteListener { uri ->
                                val imageUrl = uri.toString()
                                val uid = auth.currentUser!!.uid
                                val phone = auth.currentUser!!.phoneNumber
                                val username = binding.setupUsernameEdt.text.toString()
                                val user = UserDataModel(uid, username, phone, imageUrl,gendertype)

                                database.reference
                                    .child("users")
                                    .child(uid)
                                    .setValue(user)
                                    .addOnCompleteListener{
                                        val intent  = Intent(this@SetupPage,MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Please select a Image", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data != null){
            if(data.data != null){
                val uri = data.data //file Path
                val storage = FirebaseStorage.getInstance()
                val time = Date().time
                val reference = storage.reference
                    .child("Profile")
                    .child(time.toString()+"")
                reference.putFile(uri!!).addOnCompleteListener{task ->
                    if(task.isSuccessful){
                        reference.downloadUrl.addOnCompleteListener { uri ->
                            val filePath = uri.toString()
                            val obj = HashMap<String,Any>()
                            obj["image"] = filePath
                            database.reference
                                .child("users")
                                .child(FirebaseAuth.getInstance().uid!!)
                                .updateChildren(obj).addOnSuccessListener {  }
                        }
                    }
                }
                binding.setupImage.setImageURI(data.data)
                setupImageUri = data.data!!
            }
        }
    }
}