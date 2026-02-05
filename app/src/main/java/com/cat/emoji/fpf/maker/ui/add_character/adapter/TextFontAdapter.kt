package com.cat.emoji.fpf.maker.ui.add_character.adapter

import android.annotation.SuppressLint
import android.content.Context
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseAdapter
import com.cat.emoji.fpf.maker.core.extensions.setFont
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.data.model.SelectedModel
import com.cat.emoji.fpf.maker.databinding.ItemFontBinding

class TextFontAdapter(val context: Context, private val isEmojiCustom: Boolean = false) : BaseAdapter<SelectedModel, ItemFontBinding>(ItemFontBinding::inflate) {
    var onTextFontClick: ((Int, Int) -> Unit) = { _, _ -> }
    private var currentSelected = 0

    override fun onBind(binding: ItemFontBinding, item: SelectedModel, position: Int) {
        binding.apply {
            tvFont.setFont(item.color)

            if (item.isSelected) {
                // Selected state - set selected background and change text color
                cvMain.setBackgroundResource(R.drawable.bg_item_font_selected)
                tvFont.setTextColor(android.graphics.Color.parseColor("#497E00")) // White text
            } else {
                if (isEmojiCustom) {
                    cvMain.setBackgroundResource(R.drawable.bg_item_font_not_selected_emojicus)
                    tvFont.setTextColor(android.graphics.Color.WHITE)
                } else {
                    cvMain.setBackgroundResource(R.drawable.bg_item_font_not_selected)
                    tvFont.setTextColor(android.graphics.Color.parseColor("#7AAB36"))
                }
            }

            root.tap { onTextFontClick.invoke(item.color, position) }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>) {
        if (position != currentSelected) {
            items.clear()
            items.addAll(list)

            notifyItemChanged(currentSelected)
            notifyItemChanged(position)

            currentSelected = position
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitListReset(list: ArrayList<SelectedModel>){
        items.clear()
        items.addAll(list)
        currentSelected = 0
        notifyDataSetChanged()
    }
}