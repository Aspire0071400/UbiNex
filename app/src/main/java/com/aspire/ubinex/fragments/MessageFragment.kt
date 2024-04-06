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
import com.google.firebase.firestore.FirebaseFirestore

class MessageFragment : Fragment() {

    private lateinit var binding: FragmentMessageBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var users: ArrayList<UserDataModel>
    private var userAdapter: UserListAdapter? = null
    private var currentUser: UserDataModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMessageBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        users = ArrayList()
        userAdapter = UserListAdapter(requireContext(), users)

        val layoutManager = LinearLayoutManager(context)
        binding.userListRecycler.layoutManager = layoutManager
        binding.userListRecycler.adapter = userAdapter

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

    override fun onResume() {
        super.onResume()
        val currentId = auth.uid ?: ""
        firestore.collection("presence").document(currentId).set(mapOf("status" to "Online"))
    }

    override fun onPause() {
        super.onPause()
        val currentId = auth.uid ?: ""
        firestore.collection("presence").document(currentId).set(mapOf("status" to "Offline"))
    }
}
