package com.cat.emoji.fpf.maker.ui.customize

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cat.emoji.fpf.maker.R
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.facebook.shimmer.ShimmerDrawable
import com.cat.emoji.fpf.maker.core.extensions.dp
import com.cat.emoji.fpf.maker.core.extensions.setMargins
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.core.utils.DataLocal
import com.cat.emoji.fpf.maker.data.model.custom.NavigationModel
import com.cat.emoji.fpf.maker.databinding.ItemBottomNavigationBinding

class BottomNavigationCustomizeAdapter(private val context: Context) :
    ListAdapter<NavigationModel, BottomNavigationCustomizeAdapter.BottomNavViewHolder>(DiffCallback) {
    var onItemClick: (Int) -> Unit = {}




    inner class BottomNavViewHolder(
        private val binding: ItemBottomNavigationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NavigationModel, position: Int) = with(binding) {

            cvContent.clipToOutline = true

            val cornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                6.3f,
                cvContent.resources.displayMetrics
            )

            imvImage.clipToOutline = true
            imvImage.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }

            imvImageBG.clipToOutline = true
            imvImageBG.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }

            // Cancel any running animations to prevent jumps when recycling views
            cvContent.animate().cancel()

            if (item.isSelected) {
                imvImage.setBackgroundColor(Color.TRANSPARENT)
                imvImageBG.background = null // hoặc Color.TRANSPARENT nếu bạn thích

                cvContent.setBackgroundResource(R.drawable.bg_select_navi_shape)
            } else {
                imvImage.setBackgroundColor(Color.TRANSPARENT)
                imvImageBG.background = null

                cvContent.setBackgroundResource(R.drawable.bg_unselect_navi_shape)
            }


            // Layer 1: imvImage - shimmer fills full circle (0dp margin)
            val shimmerDrawable = ShimmerDrawable().apply {
                setShimmer(DataLocal.shimmer)
            }
            imvImage.setImageDrawable(shimmerDrawable)
            imvImage.visibility = View.VISIBLE

            // Layer 2: imvImageBG - actual image with margin (2dp margin)
            Glide.with(root)
                .load(item.imageNavigation)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hide shimmer when image loads successfully
                        imvImage.visibility = View.GONE
                        return false
                    }
                })
                .into(imvImageBG)

            root.tap { onItemClick.invoke(position) }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomNavViewHolder {
        val binding = ItemBottomNavigationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BottomNavViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BottomNavViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<NavigationModel>() {
            override fun areItemsTheSame(oldItem: NavigationModel, newItem: NavigationModel): Boolean {
                // Nếu NavigationModel có id riêng thì nên so sánh id, ở đây tạm so sánh hình
                return oldItem.imageNavigation == newItem.imageNavigation
            }

            override fun areContentsTheSame(oldItem: NavigationModel, newItem: NavigationModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
