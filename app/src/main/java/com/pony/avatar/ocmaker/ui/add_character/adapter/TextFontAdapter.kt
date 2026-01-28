package com.pony.avatar.ocmaker.ui.add_character.adapter

import android.annotation.SuppressLint
import android.content.Context
import com.pony.avatar.ocmaker.R
import com.pony.avatar.ocmaker.core.base.BaseAdapter
import com.pony.avatar.ocmaker.core.extensions.setFont
import com.pony.avatar.ocmaker.core.extensions.tap
import com.pony.avatar.ocmaker.data.model.SelectedModel
import com.pony.avatar.ocmaker.databinding.ItemFontBinding

class TextFontAdapter(val context: Context) : BaseAdapter<SelectedModel, ItemFontBinding>(ItemFontBinding::inflate) {
    var onTextFontClick: ((Int, Int) -> Unit) = { _, _ -> }
    private var currentSelected = 0

    override fun onBind(binding: ItemFontBinding, item: SelectedModel, position: Int) {
        binding.apply {
            tvFont.setFont(item.color)

            if (item.isSelected) {
                // Selected state - set selected background and change text color
                cvMain.setBackgroundResource(R.drawable.bg_item_font_selected)
                tvFont.setTextColor(android.graphics.Color.parseColor("#AB5BFF")) // White text
            } else {
                // Not selected state - white circle background
                cvMain.setBackgroundResource(R.drawable.bg_item_font_not_selected)
                tvFont.setTextColor(android.graphics.Color.parseColor("#FFFFFF")) // Black text
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