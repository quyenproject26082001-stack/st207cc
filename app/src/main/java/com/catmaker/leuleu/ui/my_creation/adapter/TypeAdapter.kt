package com.catmaker.leuleu.ui.my_creation.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.catmaker.leuleu.ui.my_creation.fragment.MyAvatarFragment
import com.catmaker.leuleu.ui.my_creation.fragment.MyDesignFragment

class TypeAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> MyAvatarFragment()
            else -> MyDesignFragment()
        }
    }

    override fun getItemCount(): Int = 2
}