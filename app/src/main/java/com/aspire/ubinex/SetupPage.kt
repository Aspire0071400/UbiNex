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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

class SetupPage : AppCompatActivity() {

    private lateinit var binding : ActivitySetupPageBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var realTimeDB : FirebaseDatabase
    private lateinit var fireStoreDB : FirebaseFirestore
    private lateinit var storage : FirebaseStorage
    private lateinit var setupImageUri : Uri
    private lateinit var gendertype : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        realTimeDB = FirebaseDatabase.getInstance()
        fireStoreDB = FirebaseFirestore.getInstance()
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
//            if (gendertype == ""){
//                Toast.makeText(this,"Please select a Gender",Toast.LENGTH_SHORT).show()
//            }
            if(name.isBlank() and binding.setupEmailEdt.text.toString().isBlank() and gendertype.isBlank()){
                Toast.makeText(this,"Fields can't be empty", Toast.LENGTH_SHORT).show()
            }else {
                if (setupImageUri != null) {
                    binding.loadingBg.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.VISIBLE
                    val storageReference = storage.reference.child("ProfilePics").child(auth.currentUser!!.uid)
                    storageReference.putFile(setupImageUri).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            storageReference.downloadUrl.addOnCompleteListener { uriTask ->
                                if (uriTask.isSuccessful) {
                                    val imageUrl = uriTask.result.toString()
                                    val uid = auth.currentUser!!.uid
                                    val phone = auth.currentUser!!.phoneNumber
                                    val username = binding.setupUsernameEdt.text.toString()
                                    val email = binding.setupEmailEdt.text.toString()
                                    val user = UserDataModel(uid, username, phone, imageUrl, gendertype, email)

                                    fireStoreDB.collection("users").document(uid).set(user)
                                        .addOnCompleteListener { firestoreTask ->
                                            if (firestoreTask.isSuccessful) {
                                                val intent = Intent(this@SetupPage, MainActivity::class.java)
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                Toast.makeText(this@SetupPage, "Error adding document: ${firestoreTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(this@SetupPage, "Error getting download URL: ${uriTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this@SetupPage, "Error uploading image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    Toast.makeText(this, "Please select a Image", Toast.LENGTH_SHORT).show()
                    binding.loadingBg.visibility = View.GONE
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && data.data != null) {
            val uri = data.data // File URI
            val time = Date().time
            val storageReference = storage.reference
                .child("ProfilePics")
                .child(time.toString() + "")

            storageReference.putFile(uri!!).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    storageReference.downloadUrl.addOnCompleteListener { uriTask ->
                        if (uriTask.isSuccessful) {
                            //val imageUrl = uriTask.result.toString()
                            // Set selected image in ImageView
                            binding.setupImage.setImageURI(data.data)
                            setupImageUri = data.data!!
                        } else {
                            // Error getting download URL
                            Toast.makeText(this@SetupPage, "Error getting download URL: ${uriTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Error uploading image
                    Toast.makeText(this@SetupPage, "Error uploading image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}