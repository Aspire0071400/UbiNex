package com.aspire.ubinex.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aspire.ubinex.R
import com.aspire.ubinex.model.BotModel
import com.aspire.ubinex.model.UserDataModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BotAdapter (var context : Context, var inputMsgList : List<BotModel>, senderId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    companion object {
        private const val ITEM_RECEIVE = 1
        private const val ITEM_SENT = 2
        val userModel : UserDataModel
            get() {
                return UserDataModel()
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.send_to_bot_layout, parent, false)
            SendViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.receive_from_bot_layout, parent, false)
            ReceiveViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val currentMsg = inputMsgList[position]
        when (holder) {

            is SendViewHolder -> {

                holder.senderInput.text = currentMsg.inputMessage
                Glide.with(context).load(userModel.profileImage).into(holder.senderProfilePic)
                holder.senderInputTime.text = formatDate(currentMsg.timeStamp)

            }
            is ReceiveViewHolder -> {

                holder.receiverInput.text = currentMsg.inputMessage
                holder.receiverInputTime.text = formatDate(currentMsg.timeStamp)
                Glide.with(context).load(R.drawable.android).into(holder.receiverProfilePic)

            }

        }
    }

    override fun getItemViewType(position: Int): Int {
        val currentMsg = inputMsgList[position]
        return if (FirebaseAuth.getInstance().currentUser!!.uid == currentMsg.currentUid) ITEM_SENT else ITEM_RECEIVE
    }
    override fun getItemCount(): Int = inputMsgList.size



    inner class SendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderProfilePic : ImageView = itemView.findViewById(R.id.sender_profile_image)
        val senderInput : TextView = itemView.findViewById(R.id.sender_input)
        val senderInputTime : TextView = itemView.findViewById(R.id.sender_input_time)
    }

    inner class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiverProfilePic : ImageView = itemView.findViewById(R.id.receiver_profile_image)
        val receiverInput : TextView = itemView.findViewById(R.id.receiver_input)
        val receiverInputTime : TextView = itemView.findViewById(R.id.receiver_input_time)

    }

    private fun formatDate(timestamp: Long?): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val date = timestamp?.let { Date(it) } ?: return ""
        val currentDate = Date()
        val diff = (currentDate.time - date.time) / (1000 * 60 * 60 * 24)

        return when {
            diff >= 2 -> SimpleDateFormat("dd MMMM", Locale.getDefault()).format(date)
            diff == 1L -> "Yesterday"
            else -> sdf.format(date)
        }
    }

}