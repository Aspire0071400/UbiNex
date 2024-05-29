package com.aspire.ubinex.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aspire.ubinex.Splash
import com.aspire.ubinex.databinding.FragmentAccountBinding
import com.aspire.ubinex.model.UserDataModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Date


class AccountFragment : Fragment() {

    private lateinit var accountImageUri: Uri
    private lateinit var binding: FragmentAccountBinding
    private lateinit var email: String
    private lateinit var username: String
    private lateinit var auth: FirebaseAuth
    private lateinit var fireStoreDB: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var genderType: String
    private lateinit var genders: List<String>
    private var userData: UserDataModel? = null
    private lateinit var context :Context
        override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
            binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        fireStoreDB = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        context = requireContext()

        genders = listOf("Gender", "Male", "Female", "transgender", "Others")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, genders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.genderSpinnerAccount.adapter = adapter
        genderType = ""

        binding.genderSpinnerAccount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                if (selectedItem != "Gender") {
                    genderType = selectedItem
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.editAccount.visibility = View.VISIBLE
        binding.editAccount.setOnClickListener {
            binding.editAccount.visibility = View.GONE
            binding.updateAccount.visibility = View.VISIBLE
        }
        binding.updateAccount.setOnClickListener {
            updateAccount()
            binding.editAccount.visibility = View.VISIBLE
            binding.updateAccount.visibility = View.GONE
        }
        binding.logout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
        binding.accountImage.setOnClickListener {
            openGallery()
        }

        getExistingUserData()

    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Logout") { dialogInterface: DialogInterface, i: Int ->
            auth.signOut()
            val intent = Intent(context, Splash::class.java)
            startActivity(intent)
            activity?.finish()
        }
        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun updateAccount() {

        binding.accountUsernameEdt.clearFocus()
        binding.accountEmailEdt.clearFocus()

        val name = binding.accountUsernameEdt.text.toString().trim()
        if (name.isBlank() && binding.accountEmailEdt.text.toString().isBlank() && genderType.isBlank()) {
            Toast.makeText(context, "Fields can't be empty", Toast.LENGTH_SHORT).show()
        } else {
            if (::accountImageUri.isInitialized) {
                binding.loadingBgAccount.visibility = View.VISIBLE
                binding.progressBarAccount.visibility = View.VISIBLE
                val storageReference = storage.reference.child("ProfilePics").child(auth.currentUser!!.uid)

                // Check if the selected image URI is different from the existing profile image URI
                if (accountImageUri.toString() != (userData?.profileImage ?: "")) {
                    storageReference.putFile(accountImageUri).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            storageReference.downloadUrl.addOnCompleteListener { uriTask ->
                                if (uriTask.isSuccessful) {
                                    val imageUrl = uriTask.result.toString()
                                    val uid = auth.currentUser!!.uid
                                    val phone = auth.currentUser!!.phoneNumber
                                    username = binding.accountUsernameEdt.text.toString()
                                    email = binding.accountEmailEdt.text.toString()
                                    val user = UserDataModel(uid, username, phone, imageUrl, genderType, email)

                                    fireStoreDB.collection("users").document(uid).set(user)
                                        .addOnCompleteListener { firestoreTask ->
                                            if (firestoreTask.isSuccessful) {
                                                Toast.makeText(context, "Account updated successfully", Toast.LENGTH_SHORT).show()
                                                binding.loadingBgAccount.visibility = View.GONE
                                                binding.progressBarAccount.visibility = View.GONE
                                            } else {
                                                Toast.makeText(context, "Error adding document: ${firestoreTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                                binding.loadingBgAccount.visibility = View.GONE
                                                binding.progressBarAccount.visibility = View.GONE
                                            }
                                        }
                                } else {
                                    Toast.makeText(context, "Error getting download URL: ${uriTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    binding.loadingBgAccount.visibility = View.GONE
                                    binding.progressBarAccount.visibility = View.GONE
                                }
                            }
                        } else {
                            Toast.makeText(context, "Error uploading image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            binding.loadingBgAccount.visibility = View.GONE
                            binding.progressBarAccount.visibility = View.GONE
                        }
                    }
                } else {
                    // If the image URI is the same, proceed without re-uploading the image
                    val uid = auth.currentUser!!.uid
                    val phone = auth.currentUser!!.phoneNumber
                    username = binding.accountUsernameEdt.text.toString()
                    email = binding.accountEmailEdt.text.toString()
                    val user = UserDataModel(uid, username, phone, accountImageUri.toString(), genderType, email)

                    fireStoreDB.collection("users").document(uid).set(user)
                        .addOnCompleteListener { firestoreTask ->
                            if (firestoreTask.isSuccessful) {
                                Toast.makeText(context, "Account updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error adding document: ${firestoreTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    binding.loadingBgAccount.visibility = View.GONE
                    binding.progressBarAccount.visibility = View.GONE
                }
            } else {
                Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
                binding.loadingBgAccount.visibility = View.GONE
                binding.progressBarAccount.visibility = View.GONE
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
                        binding.accountUsernameEdt.setText(userData!!.name)
                        binding.accountEmailEdt.setText(userData!!.email)
                        binding.phoneNumberAccount.text = userData!!.phoneNumber
                        userData?.gender?.let { gender ->
                            val genderIndex = genders.indexOf(gender)
                            if (genderIndex != 0) {
                                binding.genderSpinnerAccount.setSelection(genderIndex)
                            }
                        }
                        Glide.with(this).load(userData!!.profileImage).into(binding.accountImage)
                        accountImageUri = Uri.parse(userData!!.profileImage)
                    }
                } else {
                    Toast.makeText(context, "Error getting document: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
                            binding.accountImage.setImageURI(data.data)
                            accountImageUri = data.data!!
                        } else {
                            Toast.makeText(context, "Error getting download URL: ${uriTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Error uploading image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun openGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent, 90)
    }


}