package com.catmaker.leuleu.ui.emoji_custom

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.catmaker.leuleu.R
import com.catmaker.leuleu.databinding.ItemCustomizeBinding

class EmojiLayerAdapter : ListAdapter<EmojiLayerItem, EmojiLayerAdapter.ViewHolder>(DiffCallback()) {

    var onItemClick: ((Int) -> Unit) = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomizeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val binding: ItemCustomizeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EmojiLayerItem, position: Int) {
            Glide.with(binding.root.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.bg_item_layer)
                .into(binding.imvImage)

            // Highlight nếu được chọn
            binding.root.alpha = if (item.isSelected) 1f else 0.7f

            // Border nếu selected
            if (item.isSelected) {
                binding.cardLayerItem.setBackgroundResource(R.drawable.bg_item_layer_selected)
            } else {
                binding.cardLayerItem.setBackgroundResource(R.drawable.bg_item_layer)
            }

            binding.root.setOnClickListener {
                onItemClick(position)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<EmojiLayerItem>() {
        override fun areItemsTheSame(oldItem: EmojiLayerItem, newItem: EmojiLayerItem): Boolean {
            return oldItem.imageUrl == newItem.imageUrl
        }

        override fun areContentsTheSame(oldItem: EmojiLayerItem, newItem: EmojiLayerItem): Boolean {
            return oldItem == newItem
        }
    }
}
