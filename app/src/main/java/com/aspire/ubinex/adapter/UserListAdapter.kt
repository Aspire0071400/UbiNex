package com.aspire.ubinex.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aspire.ubinex.R
import com.aspire.ubinex.SoloChatActivity
import com.aspire.ubinex.databinding.UserListItemLayoutBinding
import com.aspire.ubinex.model.UserDataModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserListAdapter (var context : Context, var userList : ArrayList<UserDataModel>) :
    RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {
    lateinit var currentReceiverUid : String
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

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
        currentReceiverUid = user.uid.toString()
        holder.binding.itemUserName.text = user.name
        Glide.with(context).load(user.profileImage).placeholder(R.drawable.place_holder).into(holder.binding.itemUserImage)
        holder.itemView.setOnClickListener {
            val i = Intent(context, SoloChatActivity::class.java)
            i.putExtra("name",user.name)
            i.putExtra("image",user.profileImage)
            i.putExtra("uid",user.uid)
            context.startActivity(i)
        }
    }

}