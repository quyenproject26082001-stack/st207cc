package com.cat.emoji.fpf.maker.ui.emoji_custom.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import androidx.core.view.isVisible
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseAdapter
import com.cat.emoji.fpf.maker.core.extensions.gone
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.core.extensions.visible
import com.cat.emoji.fpf.maker.data.model.SelectedModel
import com.cat.emoji.fpf.maker.databinding.ItemTextColorEmojiBinding

class TextColorEmojiAdapter : BaseAdapter<SelectedModel, ItemTextColorEmojiBinding>(ItemTextColorEmojiBinding::inflate) {
    var onChooseColorClick: (() -> Unit) = {}
    var onTextColorClick: ((Int, Int) -> Unit) = { _, _ -> }

    private var currentSelected = 1

    override fun onBind(binding: ItemTextColorEmojiBinding, item: SelectedModel, position: Int) {
        binding.apply {
            vFocus.isVisible = item.isSelected
            vFocus.setBackgroundResource(R.drawable.bg_stroke_gradient_circle_color_text)


                imvColor.visible()

                val layoutParams = imvColor.layoutParams as android.widget.FrameLayout.LayoutParams
                val margin = imvColor.context.resources.displayMetrics.density * 1
                layoutParams.setMargins(margin.toInt(), margin.toInt(), margin.toInt(), margin.toInt())
                imvColor.layoutParams = layoutParams

                imvColor.setImageResource(0)
               // btnAddColor.gone()

                val parentFrame = imvColor.parent as? android.widget.FrameLayout
                parentFrame?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(item.color)
                }
                imvColor.background = drawable

                root.tap { onTextColorClick.invoke(item.color, position) }

        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>) {
        items.clear()
        items.addAll(list)

        if (position != currentSelected) {
            notifyItemChanged(currentSelected)
            notifyItemChanged(position)
            currentSelected = position
        } else {
            notifyItemChanged(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitListReset(list: ArrayList<SelectedModel>) {
        items.clear()
        items.addAll(list)
        currentSelected = 1
        notifyDataSetChanged()
    }
}
