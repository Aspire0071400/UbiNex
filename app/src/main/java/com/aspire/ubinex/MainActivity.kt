package com.aspire.ubinex

import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.aspire.ubinex.databinding.ActivityMainBinding
import com.aspire.ubinex.fragments.AccountFragment
import com.aspire.ubinex.fragments.MessageFragment
import com.aspire.ubinex.fragments.MusicFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var userStatusRef: DatabaseReference
    private lateinit var auth : FirebaseAuth
    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        userStatusRef = FirebaseDatabase.getInstance().reference.child("user_status")

        val pagerAdapter = MyPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = pagerAdapter

        //replaceFragment(com.aspire.ubinex.fragments.MessageFragment())
        binding.bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId){
                R.id.message -> binding.viewPager.currentItem = 0
                R.id.music -> binding.viewPager.currentItem = 1
                R.id.account -> binding.viewPager.currentItem = 2
                else ->{}
            }
            true
        }

        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                binding.bottomNavigationView.selectedItemId = when (position) {
                    0 -> R.id.message
                    1 -> R.id.music
                    2 -> R.id.account
                    else -> return
                }
            }
        })



    }

//    private fun replaceFragment(fragment : Fragment){
//        val fragmentManager = supportFragmentManager
//        val fragmentTransaction = fragmentManager.beginTransaction()
//        fragmentTransaction.replace(R.id.frame,fragment)
//        fragmentTransaction.commit()
//    }

    private inner class MyPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getCount(): Int = 3

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> MessageFragment()
                1 -> MusicFragment()
                2 -> AccountFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
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
        userStatusMap["status"] = "active"
        userStatusRef.child(currentUserID).updateChildren(userStatusMap)
    }

    override fun onDestroy() {
        super.onDestroy()
        val currentUserID = auth.currentUser?.uid ?: return
        val userStatusMap = HashMap<String, Any>()
        userStatusMap["status"] = "offline"
        userStatusRef.child(currentUserID).updateChildren(userStatusMap)
    }

    override fun onBackPressed() {
        // Get the current fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.view_pager)


        if (currentFragment is MessageFragment || currentFragment is MusicFragment || currentFragment is AccountFragment) {

            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
                onDestroy()
                finish()
                return
            }

            this.doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()


            Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 4000)
        } else {
            // Call super to handle the back press normally for other fragments
            super.onBackPressed()
        }
    }



}