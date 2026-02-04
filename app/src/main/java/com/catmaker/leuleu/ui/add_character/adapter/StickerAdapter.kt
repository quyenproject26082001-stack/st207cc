package com.catmaker.leuleu.ui.add_character.adapter

import com.catmaker.leuleu.core.base.BaseAdapter
import com.catmaker.leuleu.core.extensions.loadImage
import com.catmaker.leuleu.core.extensions.loadImageSticker
import com.catmaker.leuleu.core.extensions.tap
import com.catmaker.leuleu.data.model.SelectedModel
import com.catmaker.leuleu.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick : ((String) -> Unit) = {}
    override fun onBind(binding: ItemStickerBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSticker)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}