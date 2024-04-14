package com.aspire.ubinex.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.aspire.ubinex.adapter.UserListAdapter
import com.aspire.ubinex.databinding.FragmentMessageBinding
import com.aspire.ubinex.model.UserDataModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class MessageFragment : Fragment() {

    private lateinit var binding: FragmentMessageBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userStatusRef: DatabaseReference
    private lateinit var users: ArrayList<UserDataModel>
    private var userAdapter: UserListAdapter? = null
    private var currentUser: UserDataModel? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        users = ArrayList()
        userStatusRef = FirebaseDatabase.getInstance().reference.child("user_status")
        userAdapter = UserListAdapter(requireContext(), users)

        val layoutManager = LinearLayoutManager(context)
        binding.userListRecycler.layoutManager = layoutManager
        binding.userListRecycler.adapter = userAdapter


        loadUserData()
        addSnapshotListener()
        updateUserStatus("online")

    }


    override fun onDestroy() {
        updateUserStatus("offline")
        super.onDestroy()
    }

    override fun onResume() {
        updateUserStatus("online")
        super.onResume()
    }

    private fun addSnapshotListener() {
        val currentUserUid = auth.currentUser?.uid ?: ""
        firestore.collection("users").document(currentUserUid)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    // Handle Firestore exception
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    currentUser = snapshot.toObject(UserDataModel::class.java)
                }
            }
    }

    private fun updateUserStatus(status: String) {
        val currentUserID = auth.currentUser?.uid ?: return
        val userStatusMap = hashMapOf<String, Any>("status" to status)
        userStatusRef.child(currentUserID).updateChildren(userStatusMap)
    }

    private fun loadUserData() {
        val currentUserUid = auth.currentUser?.uid ?: ""
        firestore.collection("users").document(currentUserUid)
            .get()
            .addOnSuccessListener { snapshot ->
                currentUser = snapshot.toObject(UserDataModel::class.java)
            }
            .addOnFailureListener {
                // Handle Firestore failure
            }

        firestore.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                users.clear()
                for (document in querySnapshot.documents) {
                    val user = document.toObject(UserDataModel::class.java)
                    if (user != null && user.uid != currentUserUid) {
                        users.add(user)
                    }
                }
                userAdapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Handle Firestore failure
            }
    }


//    override fun onDestroyView() {
//
//        val currentUserID = auth.currentUser?.uid ?: return
//        val userStatusMap = HashMap<String, Any>()
//        userStatusMap["status"] = "offline"
//        userStatusRef.child(currentUserID).updateChildren(userStatusMap)
//
//        super.onDestroyView()
//    }

}