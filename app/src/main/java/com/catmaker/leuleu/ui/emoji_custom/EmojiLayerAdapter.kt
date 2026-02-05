package com.catmaker.leuleu.ui.emoji_custom

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
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
import com.catmaker.leuleu.databinding.ItemEmojiCusBinding

class EmojiLayerAdapter : ListAdapter<EmojiLayerItem, EmojiLayerAdapter.ViewHolder>(DiffCallback()) {

    var onItemClick: ((EmojiLayerItem) -> Unit) = {}
    var onItemLoadError: ((EmojiLayerItem) -> Unit) = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEmojiCusBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val binding: ItemEmojiCusBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EmojiLayerItem, position: Int) {
            // Show shimmer, hide image khi bắt đầu load
            binding.sflShimmer.visibility = View.VISIBLE
            binding.sflShimmer.startShimmer()
            binding.imvImage.visibility = View.INVISIBLE

            Glide.with(binding.root.context)
                .load(item.imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        binding.sflShimmer.stopShimmer()
                        binding.sflShimmer.visibility = View.GONE
                        onItemLoadError(item)
                        return false
                    }
                    override fun onResourceReady(resource: Drawable, model: Any?, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        // Hide shimmer, show image khi load xong
                        binding.sflShimmer.stopShimmer()
                        binding.sflShimmer.visibility = View.GONE
                        binding.imvImage.visibility = View.VISIBLE
                        return false
                    }
                })
                .into(binding.imvImage)

            // Background theo trạng thái selected
            if (item.isSelected) {
                binding.cardLayerItem.setBackgroundResource(R.drawable.bg_item_shadow_slt)
            } else {
                binding.cardLayerItem.setBackgroundResource(R.drawable.bg_item_shadow_uslt)
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
