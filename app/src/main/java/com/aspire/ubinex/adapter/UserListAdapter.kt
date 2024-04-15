package com.aspire.ubinex.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.aspire.ubinex.R
import com.aspire.ubinex.SoloChatActivity
import com.aspire.ubinex.databinding.UserListItemLayoutBinding
import com.aspire.ubinex.model.ChatModel
import com.aspire.ubinex.model.UserDataModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserListAdapter(
    private val context: Context,
    private val userList: ArrayList<UserDataModel>,
) : RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {

    //lateinit var currentReceiverUid : String
    private lateinit var dbRef: DatabaseReference

    inner class UserViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val binding : UserListItemLayoutBinding = UserListItemLayoutBinding.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.user_list_item_layout,parent,false)
        return UserViewHolder(v)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        val uid = user.uid ?: return

        holder.binding.itemUserName.text = user.name
        Glide.with(context).load(user.profileImage).placeholder(R.drawable.place_holder).into(holder.binding.itemUserImage)
        holder.itemView.setOnClickListener {
            val i = Intent(context, SoloChatActivity::class.java)
            i.putExtra("name", user.name)
            i.putExtra("image", user.profileImage)
            i.putExtra("uid", user.uid)
            context.startActivity(i)
        }

        val name = holder.binding.itemUserName.text
        holder.itemView.setOnLongClickListener {
            Toast.makeText(context,"${name}",Toast.LENGTH_SHORT).show()
            true
        }

        // Fetch last message and user status in real-time
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val receiverUid = user.uid ?: ""
        if (currentUserUid.isNotEmpty() && receiverUid.isNotEmpty()) {
            dbRef = FirebaseDatabase.getInstance().reference
            val userStatusRef = FirebaseDatabase.getInstance().reference.child("user_status")
            val senderRoom = "$currentUserUid$receiverUid"

            // Fetch last message
            dbRef.child("chats").child(senderRoom).child("messages").limitToLast(1)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (messageSnapshot in snapshot.children) {
                                val lastMessage = messageSnapshot.getValue(ChatModel::class.java)
                                if (lastMessage != null) {
                                    val lastMessageText = lastMessage.message
                                    val lastMessageTime = lastMessage.timeStamp // Assuming timeStamp is the field for message time
                                    val formattedTime = formatDate(lastMessageTime)
                                    holder.binding.itemUserLastText.text = lastMessageText
                                    holder.binding.itemUserLastTextTime.text = formattedTime
                                }
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        // Handle onCancelled
                    }
                })

            // Fetch user status
            userStatusRef.child(receiverUid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(statusSnapshot: DataSnapshot) {
                    val userStatus = statusSnapshot.child("status").value?.toString()
                    if (userStatus == "active" || userStatus == "online") {
                        // If user is active or online, set visibility of presence indicator to gone
                        holder.binding.itemUserPresenceIndicator.visibility = View.VISIBLE
                    } else {
                        // If user status is not active or online, set visibility to visible
                        holder.binding.itemUserPresenceIndicator.visibility = View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled
                }
            })
        }
    }


    private fun formatDate(timestamp: Long?): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val date = timestamp?.let { Date(it) } ?: return ""
        val currentDate = Date()
        val diff = (currentDate.time - date.time) / (1000 * 60 * 60 * 24)

        return if (diff >= 2) {
            val dayFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())
            dayFormat.format(date)
        } else if (diff == 1L) {
            "Yesterday"
        } else {
            sdf.format(date)
        }
    }

}