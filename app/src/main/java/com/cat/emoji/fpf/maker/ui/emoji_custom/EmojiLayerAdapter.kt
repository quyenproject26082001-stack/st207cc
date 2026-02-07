package com.cat.emoji.fpf.maker.ui.emoji_custom

import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.databinding.ItemCustomizeBinding
import com.cat.emoji.fpf.maker.databinding.ItemEmojiCusBinding

class EmojiLayerAdapter : ListAdapter<EmojiLayerItem, EmojiLayerAdapter.ViewHolder>(DiffCallback()) {

    var onItemClick: ((EmojiLayerItem) -> Unit) = {}
    var onItemLoadError: ((EmojiLayerItem) -> Unit) = {}

    var onItemSlow: ((EmojiLayerItem, Long) -> Unit) = { _, _ -> }


    private val slowUrls = mutableSetOf<String>()
    private val badUrls = mutableSetOf<String>()


    val startMs = SystemClock.elapsedRealtime()


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
                .override(160, 160)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .thumbnail(0.25f)
                .dontAnimate()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        binding.sflShimmer.stopShimmer()
                        binding.sflShimmer.visibility = View.GONE

                        val url = item.imageUrl


                        // ✅ Lấy category từ URL (ví dụ: Eyes%20Big -> Eyes Big)
                        val categoryFromUrl = try {
                            val encoded = java.net.URL(url).path
                                .substringAfter("/emojis/")
                                .substringBeforeLast("/") // Eyes%20Big
                            java.net.URLDecoder.decode(encoded, "UTF-8") // Eyes Big
                        } catch (ex: Exception) {
                            "unknown"
                        }
                        // Nếu muốn thấy nguyên nhân chi tiết theo từng source:
                        e?.rootCauses?.forEachIndexed { i, cause ->
                            android.util.Log.e("EmojiGlide", "  rootCause[$i]=${cause.javaClass.simpleName}: ${cause.message}")
                        }

                        onItemLoadError(item)
                        return false
                    }
                    override fun onResourceReady(resource: Drawable, model: Any?, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        val took = android.os.SystemClock.elapsedRealtime() - startMs

                        // ✅ GỌI Ở ĐÂY
                        if (took >= 1200) {
                            onItemSlow(item, took)
                        }
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

    private fun dumpBadSlowUrls() {
        Log.e("EmojiGlide", "===== BAD URLS (${badUrls.size}) =====")
        badUrls.forEach { Log.e("EmojiGlide", it) }

        Log.w("EmojiGlide", "===== SLOW URLS (${slowUrls.size}) =====")
        slowUrls.forEach { Log.w("EmojiGlide", it) }
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
