package com.catmaker.leuleu.ui.emoji_custom

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.catmaker.leuleu.R
import com.catmaker.leuleu.databinding.ItemCustomizeBinding

class EmojiLayerAdapter : ListAdapter<EmojiLayerItem, EmojiLayerAdapter.ViewHolder>(DiffCallback()) {

    var onItemClick: ((EmojiLayerItem) -> Unit) = {}
    var onItemLoadError: ((EmojiLayerItem) -> Unit) = {}

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
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        onItemLoadError(item)
                        return false
                    }
                    override fun onResourceReady(resource: Drawable, model: Any?, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        return false
                    }
                })
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
                onItemClick(item)
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
