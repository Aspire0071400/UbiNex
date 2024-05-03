package com.aspire.ubinex

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aspire.ubinex.adapter.ChatAdapter
import com.aspire.ubinex.databinding.ActivitySoloChatBinding
import com.aspire.ubinex.model.ChatModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SoloChatActivity : AppCompatActivity() {

    private lateinit var senderUid: String
    private lateinit var binding : ActivitySoloChatBinding
    private lateinit var chatAdapter : ChatAdapter
    private lateinit var messageList : ArrayList<ChatModel>
    private lateinit var auth : FirebaseAuth
    private lateinit var fireStoreDb : FirebaseFirestore
    private lateinit var dbRef : DatabaseReference
    private var isKeyboardVisible = false
    private lateinit var userStatusRef: DatabaseReference
    private lateinit var storage : FirebaseStorage
    private lateinit var receiverRoom : String
    private lateinit var senderRoom : String
    private val REQUEST_IMAGE_PICK = 25
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    //private val STORAGE_PERMISSION_REQUEST_CODE = 200


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySoloChatBinding.inflate(layoutInflater)
        val context : Context = this
        messageList = ArrayList()
        auth = FirebaseAuth.getInstance()
        fireStoreDb = FirebaseFirestore.getInstance()
        dbRef = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        val name = intent.getStringExtra("name")
        val imageUrl = intent.getStringExtra("image")
        val receiverUid = intent.getStringExtra("uid").toString()
        senderUid = auth.currentUser!!.uid

        binding.soloChatReceiverUserName.text = name
        Glide.with(this).load(imageUrl).into(binding.soloChatReceiverImage)

        senderRoom = "$receiverUid$senderUid"
        receiverRoom = "$senderUid$receiverUid"

        chatAdapter = ChatAdapter(context,binding.soloChatRecycler, messageList,senderRoom,receiverRoom)

        setContentView(binding.root)

        userStatusRef = FirebaseDatabase.getInstance().reference.child("user_status")

        binding.soloChatBackBtn.setOnClickListener { finish() }

        binding.soloChatKeyboardHandler.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            if (isKeyboardVisible) {
                imm.hideSoftInputFromWindow(binding.soloChatMessageField.windowToken, 0)
                binding.soloChatMessageField.clearFocus()
                isKeyboardVisible = false
            } else {
                binding.soloChatMessageField.requestFocus()
                imm.showSoftInput(binding.soloChatMessageField, InputMethodManager.SHOW_IMPLICIT)
                binding.soloChatMessageField.requestFocus()
                isKeyboardVisible = true
            }
        }

        binding.soloChatRecycler.layoutManager = LinearLayoutManager(this@SoloChatActivity)
        binding.soloChatRecycler.adapter = chatAdapter

        dbRef.child("chats").child(senderRoom).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = ArrayList<ChatModel>()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(ChatModel::class.java)
                        message?.let { list.add(it) }
                    }
                    messageList.clear()
                    messageList.addAll(list)
                    chatAdapter.notifyDataSetChanged()
                    chatAdapter.scrollToEnd()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SoloChatActivity, "Network Error, check your network!", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        binding.soloChatSend.setOnClickListener {
            val messageText = binding.soloChatMessageField.text.toString()
            if (messageText.isNotBlank()) {
                val timestamp = System.currentTimeMillis()
                val messageObject = ChatModel(
                    message = messageText,
                    senderId = senderUid,
                    timeStamp = timestamp
                )

                dbRef.child("chats").child(senderRoom).child("messages")
                    .push().setValue(messageObject).addOnSuccessListener {
                        dbRef.child("chats").child(receiverRoom).child("messages")
                            .push().setValue(messageObject)
                            .addOnSuccessListener {
                                binding.soloChatMessageField.text = null
                                binding.soloChatMessageField.clearFocus()
                            }
                    }
            }
        }


        binding.soloChatShareAttachment.setOnClickListener {
           openGallery()
        }

        binding.soloChatCamera.setOnClickListener {
            requestPermissions(arrayOf("android.permission.CAMERA"),CAMERA_PERMISSION_REQUEST_CODE)
        }

        val handler = Handler()
        binding.soloChatMessageField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val currentUserID = auth.currentUser?.uid ?: return
                val userStatusMap = hashMapOf<String, Any>("status" to "typing...")
                userStatusRef.child(currentUserID).updateChildren(userStatusMap)

                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping, 2000)
            }

            private val userStoppedTyping = Runnable {
                val currentUserID = auth.currentUser?.uid ?: return@Runnable
                val userStatusMap = hashMapOf<String, Any>("status" to "active")
                userStatusRef.child(currentUserID).updateChildren(userStatusMap)
            }
        })

        setupUserStatusListener(receiverUid)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK) {
            if (resultCode == Activity.RESULT_OK) {
                handleImagePickResult(data)
            }
        }
    }

    private fun uploadImageToStorage(imageUri: Uri) {
        val time = System.currentTimeMillis()
        val storageRef = FirebaseStorage.getInstance().reference.child("chats").child(senderRoom).child("$time")

        storageRef.putFile(imageUri).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                storageRef.downloadUrl.addOnCompleteListener { uriTask ->
                    val imageUrl = uriTask.result.toString()
                    val messageTxt = "-/*photo*/-"
                    val timestamp = System.currentTimeMillis()
                    val messageWithImageObject = ChatModel(
                        message = messageTxt,
                        senderId = senderUid,
                        timeStamp = timestamp,
                        imageUrl = imageUrl
                    )
                    sendMessageToFirebase(messageWithImageObject)
                }
            } else {
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessageToFirebase(message: ChatModel) {
        dbRef.child("chats").child(senderRoom).child("messages")
            .push().setValue(message).addOnSuccessListener {
                dbRef.child("chats").child(receiverRoom).child("messages")
                    .push().setValue(message)
                    .addOnSuccessListener {
                        binding.soloChatMessageField.text = null
                        Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun handleImagePickResult(data: Intent?) {
        val selectedImageUri = data?.data
        selectedImageUri?.let { uri ->
            uploadImageToStorage(uri)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 100){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "CAMERA Permission granted", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "CAMERA Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent,REQUEST_IMAGE_PICK)
    }

    private fun setupUserStatusListener(receiverUid: String) {
        userStatusRef.child(receiverUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.child("status").value?.toString()
                    binding.soloChatCurrentStatus.text = when (status) {
                        "online" -> "Online"
                        "offline" -> "Offline"
                        "active" -> "Active"
                        "typing..." -> "Typing..."
                        else -> "Unknown"
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateUserStatus(status: String) {
        val currentUserID = auth.currentUser?.uid ?: return
        val userStatusMap = hashMapOf<String, Any>("status" to status)
        userStatusRef.child(currentUserID).updateChildren(userStatusMap)
    }

    override fun onPause() {
        updateUserStatus("online")
        super.onPause()
    }

    override fun onResume() {
        updateUserStatus("active")
        super.onResume()
    }

    override fun onBackPressed() {
        updateUserStatus("online")
        super.onBackPressed()
        finish()
    }

    override fun finish() {
        updateUserStatus("online")
        super.finish()
    }

}
