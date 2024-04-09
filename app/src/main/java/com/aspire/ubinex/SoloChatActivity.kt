package com.aspire.ubinex

import ChatAdapter
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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

class SoloChatActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySoloChatBinding
    private lateinit var chatAdapter : ChatAdapter
    private lateinit var messageList : ArrayList<ChatModel>
    private lateinit var auth : FirebaseAuth
    private lateinit var firestoreDb : FirebaseFirestore
    private lateinit var dbRef : DatabaseReference
    private var isKeyboardVisible = false
    private lateinit var userStatusRef: DatabaseReference
    private lateinit var userStatusListener: ValueEventListener

    var receiverRoom : String? = null
    var senderRoom : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySoloChatBinding.inflate(layoutInflater)
        messageList = ArrayList()
        chatAdapter = ChatAdapter(binding.soloChatRecycler, messageList)

        auth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()
        dbRef = FirebaseDatabase.getInstance().reference

        setContentView(binding.root)

        userStatusRef = FirebaseDatabase.getInstance().reference.child("user_status")

        binding.soloChatBackBtn.setOnClickListener { finish() }
        binding.soloChatEmoji.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            if (isKeyboardVisible) {
                imm.hideSoftInputFromWindow(binding.soloChatMessageField.windowToken, 0)
                isKeyboardVisible = false
            } else {
                binding.soloChatMessageField.requestFocus()
                imm.showSoftInput(binding.soloChatMessageField, InputMethodManager.SHOW_IMPLICIT)
                isKeyboardVisible = true
            }
        }

        val name = intent.getStringExtra("name")
        val imageUrl = intent.getStringExtra("image")
        val receiverUid = intent.getStringExtra("uid")
        val senderUid = auth.currentUser!!.uid

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        binding.soloChatReceiverUserName.text = name
        Glide.with(this).load(imageUrl).into(binding.soloChatReceiverImage)

        binding.soloChatRecycler.layoutManager = LinearLayoutManager(this)
        binding.soloChatRecycler.adapter = chatAdapter

        dbRef.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(ChatModel::class.java)
                        messageList.add(message!!)
                    }
                    chatAdapter.notifyDataSetChanged()
                    chatAdapter.scrollToEnd()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled
                }
            })

        binding.soloChatSend.setOnClickListener {
            if (binding.soloChatMessageField.text.isNotBlank() || binding.soloChatMessageField.text.isNotEmpty()) {
                val messageText = binding.soloChatMessageField.text.toString()
                val timestamp = System.currentTimeMillis()
                val messageObject = ChatModel(message = messageText, senderId = senderUid, timeStamp = timestamp)

                dbRef.child("chats").child(senderRoom!!).child("messages")
                    .push().setValue(messageObject).addOnSuccessListener {
                        dbRef.child("chats").child(receiverRoom!!).child("messages")
                            .push().setValue(messageObject)
                            .addOnSuccessListener {
                                binding.soloChatMessageField.text = null
                            }
                    }
            }
        }

        setupUserStatusListener(receiverUid.toString())

    }

    private fun setupUserStatusListener(receiverUid: String) {
        userStatusListener = userStatusRef.child(receiverUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.child("status").value?.toString()
                    // Update status TextView in the toolbar
                    binding.soloChatCurrentStatus.text = when (status) {
                        "online" -> "Online"
                        "offline" -> "Offline"
                        "active" -> "active"
                        else -> "Unknown"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled
                }
            })
    }


    override fun onPause() {
        super.onPause()

        val currentUserID = auth.currentUser?.uid ?: return
        val userStatusMap = HashMap<String, Any>()
        userStatusMap["status"] = "offline"
        userStatusRef.child(currentUserID).updateChildren(userStatusMap)

    }

    override fun onResume() {
        super.onResume()

        val currentUserID = auth.currentUser?.uid ?: return
        val userStatusMap = HashMap<String, Any>()
        userStatusMap["status"] = "online"
        userStatusRef.child(currentUserID).updateChildren(userStatusMap)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val currentUserID = auth.currentUser?.uid ?: return
        val userStatusMap = HashMap<String, Any>()
        userStatusMap["status"] = "active"
        userStatusRef.child(currentUserID).updateChildren(userStatusMap)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        val currentUserID = auth.currentUser?.uid ?: return
        val userStatusMap = HashMap<String, Any>()
        userStatusMap["status"] = "active"
        userStatusRef.child(currentUserID).updateChildren(userStatusMap)
        userStatusRef.removeEventListener(userStatusListener)
    }

//    override fun onStop() {
//        super.onStop()
//        // Set user status to "offline" when the activity is stopped
//        val currentUserID = auth.currentUser?.uid ?: return
//        val userStatusMap = HashMap<String, Any>()
//        userStatusMap["status"] = "offline"
//        userStatusRef.child(currentUserID).updateChildren(userStatusMap)
//    }

}
