package com.cat.emoji.fpf.maker.ui.emoji_custom.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.custom.drawview.DrawView
import com.cat.emoji.fpf.maker.data.model.draw.DrawableDraw
import com.cat.emoji.fpf.maker.databinding.ItemLayerBinding
import java.util.concurrent.Executor

class LayerAdapter(
    private val drawView: DrawView,
    private val onClick: (DrawableDraw, Int) -> Unit
) : ListAdapter<DrawableDraw, LayerAdapter.LayerVH>(
    AsyncDifferConfig.Builder(object : DiffUtil.ItemCallback<DrawableDraw>() {
        override fun areItemsTheSame(oldItem: DrawableDraw, newItem: DrawableDraw): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DrawableDraw, newItem: DrawableDraw): Boolean {
            return oldItem.id == newItem.id && oldItem.isHide == newItem.isHide
        }
    })
        // Make diff synchronous to keep drag reorder and canvas order in sync
        .setBackgroundThreadExecutor(DIRECT_EXECUTOR)
        .setMainThreadExecutor(DIRECT_EXECUTOR)
        .build()
) {

    companion object {
        private val DIRECT_EXECUTOR = Executor { it.run() }
        private const val TAG = "LayerDrag"
        private const val LOG_ENABLED = true
    }

    private var selectItemPosition = RecyclerView.NO_POSITION

    override fun submitList(list: List<DrawableDraw>?) {
//        android.util.Log.d("LayerAdapter", "📋📋📋 SUBMIT LIST 📋📋📋")
//        android.util.Log.d("LayerAdapter", "📋 New list size: ${list?.size ?: 0}")
        list?.forEachIndexed { index, item ->
      //      android.util.Log.d("LayerAdapter", "  [$index] ${item.drawablePath}")
        }
        super.submitList(list)
   //     android.util.Log.d("LayerAdapter", "📋📋📋 END SUBMIT 📋📋📋")
    }

    inner class LayerVH(private val binding: ItemLayerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("CheckResult", "SetTextI18n")
        fun bindData(draw: DrawableDraw, position: Int) {
//            android.util.Log.d("LayerAdapter", "════════════════════════════════════════")
//            android.util.Log.d("LayerAdapter", "📌 BIND - position=$position")
//            android.util.Log.d("LayerAdapter", "📌 BIND - draw.hashCode=${draw.hashCode()}")
//            android.util.Log.d("LayerAdapter", "📌 BIND - draw.drawablePath=${draw.drawablePath}")
//            android.util.Log.d("LayerAdapter", "📌 BIND - draw.drawable.hashCode=${draw.drawable.hashCode()}")

            binding.apply {
                tvLayer.text = "${itemView.context.getString(R.string.layer)} ${position + 1}"

 //               android.util.Log.d("LayerAdapter", "📌 BIND - tvLayer.text=${tvLayer.text}")

                // Tag the ImageView with the drawablePath for debugging
                img.tag = draw.drawablePath

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

                // Load drawable preview - clear previous image to avoid showing stale data
                // Reset ImageView state to avoid ghosting from reused holders
                img.setImageDrawable(null)
                img.alpha = 1f
                img.clearColorFilter()
                Glide.with(itemView.context)
                    .clear(img)
                // Use a cloned drawable to avoid sharing mutable state with canvas rendering
                val previewDrawable = try {
                    draw.getOriginalDrawable()
                } catch (e: Exception) {
                    draw.drawable.constantState?.newDrawable()?.mutate() ?: draw.drawable
                }
                Glide.with(itemView.context)
                    .load(previewDrawable)
                    .into(img)

 //               android.util.Log.d("LayerAdapter", "📌 BIND - img.tag=${img.tag}")

                // Eye button click - toggle show/hide
                btnEye.setOnClickListener {
//                    android.util.Log.e("LayerAdapter", "🔴🔴🔴 CLICK BTNEYE 🔴🔴🔴")
//                    android.util.Log.e("LayerAdapter", "🔴 BEFORE - parameter draw.hashCode=${draw.hashCode()}")
//                    android.util.Log.e("LayerAdapter", "🔴 BEFORE - parameter draw.drawablePath=${draw.drawablePath}")
//                    android.util.Log.e("LayerAdapter", "🔴 BEFORE - parameter draw.drawable.hashCode=${draw.drawable.hashCode()}")
//                    android.util.Log.e("LayerAdapter", "🔴 BEFORE - tvLayer.text=${tvLayer.text}")
//                    android.util.Log.e("LayerAdapter", "🔴 BEFORE - img.tag=${img.tag}")

                    val clickPosition = bindingAdapterPosition
//                    android.util.Log.e("LayerAdapter", "🔴 CLICK - bindingAdapterPosition=$clickPosition")
//                    android.util.Log.e("LayerAdapter", "🔴 CLICK - selectItemPosition=$selectItemPosition")

                    if (clickPosition != RecyclerView.NO_POSITION) {
                        // Get the correct draw object from current list position
                        val currentDraw = getItem(clickPosition)

//                        android.util.Log.e("LayerAdapter", "🔴 CLICK - currentDraw.hashCode=${currentDraw.hashCode()}")
//                        android.util.Log.e("LayerAdapter", "🔴 CLICK - currentDraw.drawablePath=${currentDraw.drawablePath}")
//                        android.util.Log.e("LayerAdapter", "🔴 CLICK - currentDraw.drawable.hashCode=${currentDraw.drawable.hashCode()}")
//                        android.util.Log.e("LayerAdapter", "🔴 CLICK - currentDraw.isHide=${currentDraw.isHide}")
//                        android.util.Log.e("LayerAdapter", "🔴 CLICK - drawView.getCurrentDraw()=${drawView.getCurrentDraw()?.drawablePath}")

                        // Check if this is the selected item
                        val isSelectedPosition = clickPosition == selectItemPosition
                        val isCurrentlySelected = drawView.getCurrentDraw() == currentDraw
//                        android.util.Log.e("LayerAdapter", "🔴 CLICK - isSelectedPosition=$isSelectedPosition")
//                        android.util.Log.e("LayerAdapter", "🔴 CLICK - isCurrentlySelected=$isCurrentlySelected")

                        // Toggle hide/show
                        drawView.showOrHideDraw(currentDraw, clickPosition)
    //                    android.util.Log.e("LayerAdapter", "🔴 ACTION - After toggle, currentDraw.isHide=${currentDraw.isHide}")

                        // Sync selection state:
                        // - If this is the selected position AND draw is now visible, ensure it's selected in DrawView
                        // - If this is the selected position AND draw is now hidden, deselect in DrawView
                        if (isSelectedPosition) {
                            if (!currentDraw.isHide) {
    //                            android.util.Log.e("LayerAdapter", "🔴 ACTION - Selected item is now visible, ensuring DrawView selection")
                                drawView.selectCurrentDraw(currentDraw)
                            } else {
   //                             android.util.Log.e("LayerAdapter", "🔴 ACTION - Selected item is now hidden, hiding DrawView selection")
                                drawView.hideSelect()
                            }
                        }

                        notifyItemChanged(clickPosition)

   //                     android.util.Log.e("LayerAdapter", "🔴 AFTER CLICK - notifyItemChanged($clickPosition) called")
                    }
    //                android.util.Log.e("LayerAdapter", "🔴🔴🔴 END CLICK 🔴🔴🔴")
                }

                // Item background - highlight selected
                val isSelected = position == selectItemPosition
                itemView.setBackgroundColor(
                    if (isSelected) {
                        ContextCompat.getColor(itemView.context, R.color.colorPrimary1)

                    }else {
                        ContextCompat.getColor(itemView.context, android.R.color.white)
                    }
                )

                // Tint btnEye / btnMove white khi selected
                if (isSelected) {
                    val whiteColor = ContextCompat.getColor(itemView.context, android.R.color.white)
                    btnEye.setColorFilter(whiteColor)
                    btnMove.setColorFilter(whiteColor)
                    tvLayer.setTextColor(whiteColor)
                } else {
                    btnEye.clearColorFilter()
                    btnMove.clearColorFilter()
                    tvLayer.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                }

                // Item click - select layer
                itemView.setOnClickListener {
    //                android.util.Log.i("LayerAdapter", "💡 CLICK ITEM")
                    val clickPosition = bindingAdapterPosition
    //                android.util.Log.i("LayerAdapter", "💡 CLICK - bindingAdapterPosition=$clickPosition")
                    if (clickPosition != RecyclerView.NO_POSITION) {
                        selectItemPosition = clickPosition
                        notifyDataSetChanged()
                        // Get the correct draw object from current list position
                        val currentDraw = getItem(clickPosition)
   //                     android.util.Log.i("LayerAdapter", "💡 CLICK - currentDraw.drawablePath=${currentDraw.drawablePath}")
                        onClick.invoke(currentDraw, clickPosition)
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
        val item = getItem(position)
    //    android.util.Log.d("LayerAdapter", "⚙️ onBindViewHolder - position=$position, item.drawablePath=${item.drawablePath}")
        holder.bindData(item, position)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        logOrder("before move", fromPosition, toPosition)
//        android.util.Log.w("LayerAdapter", "🔄🔄🔄 MOVE ITEM 🔄🔄🔄")
//        android.util.Log.w("LayerAdapter", "🔄 MOVE - fromPosition=$fromPosition → toPosition=$toPosition")
//        android.util.Log.w("LayerAdapter", "🔄 MOVE - selectItemPosition BEFORE=$selectItemPosition")

        // Update adapter's list to match the new order (preview only)
        val newList = currentList.toMutableList()
        val item = newList.removeAt(fromPosition)
   //     android.util.Log.w("LayerAdapter", "🔄 MOVE - moving item.drawablePath=${item.drawablePath}")
        newList.add(toPosition, item)

//        android.util.Log.w("LayerAdapter", "🔄 MOVE - New list order:")
//        newList.forEachIndexed { index, drawableDraw ->
//    //        android.util.Log.w("LayerAdapter", "  [$index] ${drawableDraw.drawablePath}")
//        }

        // Update selected position if affected by the move
        if (selectItemPosition != RecyclerView.NO_POSITION) {
            val oldSelectPos = selectItemPosition
            selectItemPosition = when {
                selectItemPosition == fromPosition -> toPosition
                fromPosition < toPosition && selectItemPosition in (fromPosition + 1)..toPosition -> selectItemPosition - 1
                fromPosition > toPosition && selectItemPosition in toPosition until fromPosition -> selectItemPosition + 1
                else -> selectItemPosition
            }
    //        android.util.Log.w("LayerAdapter", "🔄 MOVE - selectItemPosition: $oldSelectPos → $selectItemPosition")

            // Sync DrawView selection with new position
            if (selectItemPosition != RecyclerView.NO_POSITION && selectItemPosition < newList.size) {
                val selectedDraw = newList[selectItemPosition]
    //            android.util.Log.w("LayerAdapter", "🔄 MOVE - Syncing DrawView to select: ${selectedDraw.drawablePath} at position $selectItemPosition")
                drawView.selectCurrentDraw(selectedDraw)
            }
        }

        // Submit list and force rebind affected items
        submitList(newList) {
            // Force rebind the range of affected positions to update ViewHolders
            val minPos = kotlin.math.min(fromPosition, toPosition)
            val maxPos = kotlin.math.max(fromPosition, toPosition)
   //         android.util.Log.w("LayerAdapter", "🔄 MOVE - Force rebind positions $minPos to $maxPos")
            notifyItemRangeChanged(minPos, maxPos - minPos + 1)
            logOrder("after move", fromPosition, toPosition)
        }

    //    android.util.Log.w("LayerAdapter", "🔄🔄🔄 END MOVE 🔄🔄🔄")
    }

    fun resetItemSelected() {
        selectItemPosition = RecyclerView.NO_POSITION
    }

    fun setSelectedPosition(position: Int) {
        selectItemPosition = position
        notifyDataSetChanged()
    }

    private fun logOrder(label: String, fromPosition: Int, toPosition: Int) {
        if (!LOG_ENABLED) return
        val draws = drawView.getDraws()
        Log.d(TAG, "$label from=$fromPosition to=$toPosition rcvSize=${currentList.size} canvasSize=${draws.size}")

        currentList.forEachIndexed { index, item ->
            val canvasIndex = draws.indexOfFirst { it.id == item.id }
            Log.d(
                TAG,
                "RCV[$index] id=${item.id} path=${shortPath(item.drawablePath)} canvasIndex=$canvasIndex"
            )
        }

        draws.forEachIndexed { index, item ->
            val rcvIndex = currentList.indexOfFirst { it.id == item.id }
            Log.d(
                TAG,
                "CANVAS[$index] id=${item.id} path=${shortPath(item.drawablePath)} rcvIndex=$rcvIndex"
            )
        }
    }

    private fun shortPath(path: String): String {
        val max = 32
        return if (path.length <= max) path else path.takeLast(max)
    }
}
