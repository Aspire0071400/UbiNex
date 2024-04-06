package com.aspire.ubinex

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.navigation.NavController
import androidx.viewpager.widget.ViewPager
import com.aspire.ubinex.databinding.ActivityMainBinding
import com.aspire.ubinex.fragments.AccountFragment
import com.aspire.ubinex.fragments.MessageFragment
import com.aspire.ubinex.fragments.MusicFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

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

}