package com.pony.avatar.ocmaker.ui.emoji_custom.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pony.avatar.ocmaker.R
import com.pony.avatar.ocmaker.core.custom.drawview.DrawView
import com.pony.avatar.ocmaker.data.model.draw.DrawableDraw
import com.pony.avatar.ocmaker.databinding.ItemLayerBinding

class LayerAdapter(
    private val drawView: DrawView,
    private val onClick: (DrawableDraw, Int) -> Unit
) : ListAdapter<DrawableDraw, LayerAdapter.LayerVH>(
    AsyncDifferConfig.Builder(object : DiffUtil.ItemCallback<DrawableDraw>() {
        override fun areItemsTheSame(oldItem: DrawableDraw, newItem: DrawableDraw): Boolean {
            return oldItem.drawablePath == newItem.drawablePath
        }

        override fun areContentsTheSame(oldItem: DrawableDraw, newItem: DrawableDraw): Boolean {
            return oldItem == newItem
        }
    }).build()
) {

    private var selectItemPosition = RecyclerView.NO_POSITION

    inner class LayerVH(private val binding: ItemLayerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("CheckResult", "SetTextI18n")
        fun bindData(draw: DrawableDraw, position: Int) {
            binding.apply {
                tvLayer.text = "${itemView.context.getString(R.string.layer)} ${position + 1}"

                // Show/Hide button
                if (draw.isHide) {
                    btnEye.setImageDrawable(
                        ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.ic_hide
                        )
                    )
                } else {
                    btnEye.setImageDrawable(
                        ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.ic_show
                        )
                    )
                }

                // Load drawable preview
                Glide.with(itemView.context)
                    .load(draw.drawable)
                    .into(img)

                // Eye button click - toggle show/hide
                btnEye.setOnClickListener {
                    val clickPosition = adapterPosition
                    if (clickPosition != RecyclerView.NO_POSITION) {
                        drawView.showOrHideDraw(draw, position)
                        notifyItemChanged(clickPosition)
                    }
                }

                // Item background - highlight selected
                itemView.setBackgroundColor(
                    if (position == selectItemPosition)
                        ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                    else
                        ContextCompat.getColor(itemView.context, android.R.color.white)
                )

                // Item click - select layer
                itemView.setOnClickListener {
                    val clickPosition = adapterPosition
                    if (clickPosition != RecyclerView.NO_POSITION) {
                        selectItemPosition = clickPosition
                        notifyDataSetChanged()
                        onClick.invoke(draw, position)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerVH {
        return LayerVH(
            ItemLayerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LayerVH, position: Int) {
        holder.bindData(getItem(position), position)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        drawView.exchangeLayers(fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun resetItemSelected() {
        selectItemPosition = RecyclerView.NO_POSITION
    }
}
