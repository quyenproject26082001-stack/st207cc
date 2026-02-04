package com.catmaker.leuleu.ui.customize

import android.content.Context
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.catmaker.leuleu.core.base.BaseAdapter
import com.catmaker.leuleu.core.extensions.tap
import com.catmaker.leuleu.data.model.custom.ItemColorModel
import com.catmaker.leuleu.databinding.ItemColorBinding

class ColorLayerCustomizeAdapter(val context: Context) :
    BaseAdapter<ItemColorModel, ItemColorBinding>(ItemColorBinding::inflate) {
    var onItemClick: ((Int) -> Unit) = {}
    override fun onBindHolder(holder: BaseViewHolder, binding: ItemColorBinding, item: ItemColorModel, position: Int) {
        binding.apply {
            imvImage.setBackgroundColor(item.color.toColorInt())
            imvFocus.isVisible = item.isSelected
            root.tap {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick.invoke(pos)
                }
            }
        }
    }

    override fun onBind(binding: ItemColorBinding, item: ItemColorModel, position: Int) {}
}