package com.aspire.ubinex

import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aspire.ubinex.databinding.ActivitySetupPageBinding
import com.aspire.ubinex.model.UserDataModel
import com.aspire.ubinex.utils.PermissionHandler
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

class SetupPage : AppCompatActivity() {

    private lateinit var genders: List<String>
    private lateinit var email: String
    private lateinit var username: String
    private lateinit var binding: ActivitySetupPageBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fireStoreDB: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var setupImageUri: Uri
    private lateinit var genderType: String
    private var userData: UserDataModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        fireStoreDB = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        genders = listOf("Gender", "Male", "Female", "transgender", "Others")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.genderSpinner.adapter = adapter
        genderType = ""

        window.setFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.black)

        binding.genderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                if (selectedItem != "Gender") {
                    genderType = selectedItem
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        getExistingUserData()

        binding.setupImage.setOnClickListener {
            if(PermissionHandler.checkStoragePermissions(this)){
                openGallery()
            }else{
                PermissionHandler.requestStoragePermissions(this)
            }
        }

        binding.saveSetupProfile.setOnClickListener {
            val name = binding.setupUsernameEdt.text.toString().trim()
            if (name.isBlank() && binding.setupEmailEdt.text.toString().isBlank() && genderType.isBlank()) {
                Toast.makeText(this, "Fields can't be empty", Toast.LENGTH_SHORT).show()
            } else {
                if (::setupImageUri.isInitialized) {
                    binding.loadingBg.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.VISIBLE
                    val storageReference = storage.reference.child("ProfilePics").child(auth.currentUser!!.uid)

                    // Check if the selected image URI is different from the existing profile image URI
                    if (setupImageUri.toString() != (userData?.profileImage ?: "")) {
                        storageReference.putFile(setupImageUri).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                storageReference.downloadUrl.addOnCompleteListener { uriTask ->
                                    if (uriTask.isSuccessful) {
                                        val imageUrl = uriTask.result.toString()
                                        val uid = auth.currentUser!!.uid
                                        val phone = auth.currentUser!!.phoneNumber
                                        username = binding.setupUsernameEdt.text.toString()
                                        email = binding.setupEmailEdt.text.toString()
                                        val user = UserDataModel(uid, username, phone, imageUrl, genderType, email)

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
                        // If the image URI is the same, proceed without re-uploading the image
                        val uid = auth.currentUser!!.uid
                        val phone = auth.currentUser!!.phoneNumber
                        username = binding.setupUsernameEdt.text.toString()
                        email = binding.setupEmailEdt.text.toString()
                        val user = UserDataModel(uid, username, phone, setupImageUri.toString(), genderType, email)

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
                    }
                } else {
                    Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                    binding.loadingBg.visibility = View.GONE
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun getExistingUserData() {
        val currentUser = auth.currentUser?.uid
        if (currentUser != null) {
            fireStoreDB.collection("users").document(currentUser).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userData = task.result.toObject(UserDataModel::class.java)
                    if (userData != null) {
                        binding.setupUsernameEdt.setText(userData!!.name)
                        binding.setupEmailEdt.setText(userData!!.email)
                        userData?.gender?.let { gender ->
                            val genderIndex = genders.indexOf(gender)
                            if (genderIndex != 0) {
                                binding.genderSpinner.setSelection(genderIndex)
                            }
                        }
                        Glide.with(this).load(userData!!.profileImage).into(binding.setupImage)
                        setupImageUri = Uri.parse(userData!!.profileImage)
                    }
                } else {
                    Log.e(TAG, "Error fetching user data: ${task.exception?.message}")
                    // Handle the error (e.g., display an error message to the user)
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
                .child(auth.uid.toString())
                .child(time.toString() + "")

            storageReference.putFile(uri!!).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    storageReference.downloadUrl.addOnCompleteListener { uriTask ->
                        if (uriTask.isSuccessful) {
                            binding.setupImage.setImageURI(data.data)
                            setupImageUri = data.data!!
                        } else {
                            Toast.makeText(this@SetupPage, "Error getting download URL: ${uriTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@SetupPage, "Error uploading image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent, 45)
    }
}
