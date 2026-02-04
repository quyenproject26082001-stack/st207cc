package com.catmaker.leuleu.ui.emoji_custom

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.catmaker.leuleu.R
import com.catmaker.leuleu.databinding.ItemBottomNavigationBinding

class EmojiNavigationAdapter : ListAdapter<EmojiNavItem, EmojiNavigationAdapter.ViewHolder>(DiffCallback()) {

    var onItemClick: ((Int) -> Unit) = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBottomNavigationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val binding: ItemBottomNavigationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EmojiNavItem, position: Int) {
            Glide.with(binding.root.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.bg_item_layer)
                .into(binding.imvImage)

            // Highlight nếu được chọn
            binding.root.alpha = if (item.isSelected) 1f else 0.5f

            binding.root.setOnClickListener {
                onItemClick(position)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<EmojiNavItem>() {
        override fun areItemsTheSame(oldItem: EmojiNavItem, newItem: EmojiNavItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: EmojiNavItem, newItem: EmojiNavItem): Boolean {
            return oldItem == newItem
        }
    }
}
