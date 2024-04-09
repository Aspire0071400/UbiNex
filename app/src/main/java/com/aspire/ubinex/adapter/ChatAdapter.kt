import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aspire.ubinex.R
import com.aspire.ubinex.model.ChatModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val recyclerView: RecyclerView, private val msgList: ArrayList<ChatModel>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_RECEIVE = 1
    private val ITEM_SENT = 2

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
            holder.sendMsg.text = currentMsg.message
            holder.timestamp.text = formatDate(currentMsg.timeStamp)
        } else if (holder is ReceiveViewHolder) {
            holder.receiveMsg.text = currentMsg.message
            holder.timestamp.text = formatDate(currentMsg.timeStamp)
        }
    }

    inner class SendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sendMsg: TextView = itemView.findViewById(R.id.sender_text)
        val timestamp: TextView = itemView.findViewById(R.id.sender_text_time)
    }

    inner class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMsg: TextView = itemView.findViewById(R.id.receiver_text)
        val timestamp: TextView = itemView.findViewById(R.id.receiver_text_time)
    }

    private fun formatDate(timestamp: Long?): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val date = timestamp?.let { Date(it) } ?: return ""
        val currentDate = Date()
        val diff = (currentDate.time - date.time) / (1000 * 60 * 60 * 24)

        if (diff >= 2) {
            val dayFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())
            return dayFormat.format(date)
        } else if (diff == 1L) {
            return "Yesterday"
        } else {
            return sdf.format(date)
        }
    }

    fun scrollToEnd() {
        val lastItemPosition = msgList.size - 1
        if (lastItemPosition >= 0) {
            recyclerView.scrollToPosition(lastItemPosition)
        }
    }
}
