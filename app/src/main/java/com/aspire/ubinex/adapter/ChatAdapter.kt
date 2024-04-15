
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aspire.ubinex.R
import com.aspire.ubinex.model.ChatModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jsibbold.zoomage.ZoomageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    var context : Context,
    private val recyclerView: RecyclerView,
    private val msgList: ArrayList<ChatModel>,
    senderRoom: String,
    receiverRoom: String
)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_RECEIVE = 1
    private val ITEM_SENT = 2

    var senderRoomId = senderRoom
    var receiverRoomId = receiverRoom

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutResId = if (viewType == ITEM_RECEIVE) R.layout.receive_text_layout else R.layout.send_text_layout
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return if (viewType == ITEM_RECEIVE) ReceiveViewHolder(view) else SendViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        val currentMsg = msgList[position]
        return if (FirebaseAuth.getInstance().currentUser!!.uid == currentMsg.senderId) ITEM_SENT else ITEM_RECEIVE
    }

    override fun getItemCount(): Int = msgList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMsg = msgList[position]
        if (holder is SendViewHolder) {
            if(!(currentMsg.message != "-/*photo*/-" && currentMsg.imageUrl == null))
            {
                    holder.sendDoc.visibility = View.VISIBLE
                    holder.sendMsg.visibility = View.GONE
                    Glide.with(context).load(currentMsg.imageUrl.toString())
                        .placeholder(R.drawable.doc_place_holder).into(holder.sendDoc)
                    holder.timestamp.text = formatDate(currentMsg.timeStamp)

                holder.itemView.setOnClickListener {
                    showImageDialog(currentMsg.imageUrl.toString())
                }


            }else{
                holder.sendDoc.visibility = View.GONE
                holder.sendMsg.visibility = View.VISIBLE
                holder.sendMsg.text = currentMsg.message
                holder.timestamp.text = formatDate(currentMsg.timeStamp)

            }

            holder.itemView.setOnLongClickListener {

                showDeleteOptionsDialog(msgList[position],senderRoomId,receiverRoomId)

                true // Consume the long press event
            }


        } else if (holder is ReceiveViewHolder) {
            if(!(currentMsg.message != "-/*photo*/-" && currentMsg.imageUrl == null))
            {
                    holder.receiveDoc.visibility = View.VISIBLE
                    holder.receiveMsg.visibility = View.GONE
                    Glide.with(context).load(currentMsg.imageUrl.toString())
                        .placeholder(R.drawable.doc_place_holder).into(holder.receiveDoc)
                    holder.timestamp.text = formatDate(currentMsg.timeStamp)

                holder.itemView.setOnClickListener {
                    showImageDialog(currentMsg.imageUrl.toString())
                }

            }else {
                holder.receiveDoc.visibility = View.GONE
                holder.receiveMsg.visibility = View.VISIBLE
                holder.receiveMsg.text = currentMsg.message
                holder.timestamp.text = formatDate(currentMsg.timeStamp)
            }

            holder.itemView.setOnLongClickListener {

                showDeleteOptionsDialog(msgList[position],senderRoomId,receiverRoomId)

                true // Consume the long press event
            }

        }
    }

    private fun showDeleteOptionsDialog(message: ChatModel, senderRoom: String, receiverRoom: String) {
        val dialog = Dialog(recyclerView.context)
        dialog.setContentView(R.layout.delete_options_layout)

        val delForMeOption = dialog.findViewById<TextView>(R.id.del_for_me)
        val delForEveryoneOption = dialog.findViewById<TextView>(R.id.del_every)

        // Check if the current user is the sender of the message
        if (FirebaseAuth.getInstance().currentUser!!.uid == message.senderId) {
            // If yes, show both options
            delForMeOption.visibility = View.VISIBLE
            delForEveryoneOption.visibility = View.VISIBLE
            delForMeOption.setOnClickListener {
                deleteMessageForMe(message, senderRoom)
                dialog.dismiss()
            }
            delForEveryoneOption.setOnClickListener {
                deleteMessageForEveryone(message, senderRoom, receiverRoom)
                dialog.dismiss()
            }
        } else if (FirebaseAuth.getInstance().currentUser!!.uid != message.senderId) {
            // If the current user is the receiver, show only the "Delete for Me" option
            delForMeOption.visibility = View.VISIBLE
            delForEveryoneOption.visibility = View.GONE
            delForMeOption.setOnClickListener {
                deleteMessageForMe(message, senderRoom)
                dialog.dismiss()
            }
        }

        dialog.findViewById<TextView>(R.id.cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun deleteMessageForMe(message: ChatModel, roomId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid

        // Check if the current user is the sender of the message
        val isSender = message.senderId == currentUserId

        val delObject = ChatModel("This message is deleted", message.senderId, message.timeStamp)

        val roomRef = FirebaseDatabase.getInstance().reference.child("chats").child(roomId).child("messages")

        roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val messageSenderId = snapshot.child("senderId").getValue(String::class.java)

                    // Check if the current user is the sender and the message is theirs, or
                    // if the current user is the receiver and the message is received
                    if ((isSender && messageSenderId == currentUserId) || (!isSender && messageSenderId != currentUserId)) {
                        val messageContent = snapshot.child("message").getValue(String::class.java)
                        val messageTimeStamp = snapshot.child("timeStamp").getValue(Long::class.java)

                        if (messageContent == message.message && messageTimeStamp == message.timeStamp) {
                            val messageKey = snapshot.key
                            if (messageKey != null) {
                                // Update the message content in the room
                                roomRef.child(messageKey).setValue(delObject)
                                    .addOnSuccessListener {
                                        // Remove the message from the local list
                                        // msgList.remove(message)
                                        notifyDataSetChanged()
                                    }
                                    .addOnFailureListener { exception ->
                                        // Handle failure to update the room
                                    }
                                break // Stop looping after finding the message
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle onCancelled
            }
        })
    }


    private fun deleteMessageForEveryone(message: ChatModel, senderRoom: String, receiverRoom: String) {
        val delObject = ChatModel("This message is deleted", message.senderId, message.timeStamp)

        val senderRef = FirebaseDatabase.getInstance().reference.child("chats")
            .child(senderRoom).child("messages")

        val receiverRef = FirebaseDatabase.getInstance().reference.child("chats")
            .child(receiverRoom).child("messages")

        senderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val messageContent = snapshot.child("message").getValue(String::class.java)
                    val messageTimeStamp = snapshot.child("timeStamp").getValue(Long::class.java)

                    if (messageContent == message.message && messageTimeStamp == message.timeStamp) {
                        val messageKey = snapshot.key
                        if (messageKey != null) {
                            // Update the message content in sender's room
                            senderRef.child(messageKey).setValue(delObject)
                                .addOnSuccessListener {
                                    // Remove the message from the local list
                                    //msgList.remove(message)

                                    notifyDataSetChanged()
                                }
                            break // Stop looping after finding the message
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        receiverRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val messageContent = snapshot.child("message").getValue(String::class.java)
                    val messageTimeStamp = snapshot.child("timeStamp").getValue(Long::class.java)

                    if (messageContent == message.message && messageTimeStamp == message.timeStamp) {
                        val messageKey = snapshot.key
                        if (messageKey != null) {
                            // Update the message content in sender's room
                            receiverRef.child(messageKey).setValue(delObject)
                                .addOnSuccessListener {
                                    // Remove the message from the local list
                                    //msgList.remove(message)
                                    notifyDataSetChanged()
                                }
                            break // Stop looping after finding the message
                        }
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    inner class SendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sendMsg: TextView = itemView.findViewById(R.id.sender_text)
        val timestamp: TextView = itemView.findViewById(R.id.sender_text_time)
        val sendDoc : ImageView = itemView.findViewById(R.id.sender_doc)

    }

    inner class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMsg: TextView = itemView.findViewById(R.id.receiver_text)
        val timestamp: TextView = itemView.findViewById(R.id.receiver_text_time)
        val receiveDoc : ImageView = itemView.findViewById(R.id.receiver_doc)

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

    fun scrollToEnd() {
        val lastItemPosition = msgList.size - 1
        if (lastItemPosition >= 0) {
            recyclerView.scrollToPosition(lastItemPosition)
        }
    }

    private fun showImageDialog(imageUrl: String) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.image_dialog_layout)
        val imageView: ZoomageView = dialog.findViewById(R.id.image_preview)
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.doc_place_holder)
            .into(imageView)
        dialog.show()

    }
}
