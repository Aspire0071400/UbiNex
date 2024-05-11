package com.aspire.ubinex.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aspire.ubinex.adapter.BotAdapter
import com.aspire.ubinex.databinding.FragmentChatbotBinding
import com.aspire.ubinex.model.BotModel

class ChatBotFragment : Fragment() {

    private lateinit var binding: FragmentChatbotBinding
    private lateinit var context: Context
    private lateinit var botAdapter: BotAdapter
    private lateinit var botList: ArrayList<BotModel>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChatbotBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context = requireContext()
        botList = ArrayList()


        binding.chatBotSend.setOnClickListener {
        }


    }


}