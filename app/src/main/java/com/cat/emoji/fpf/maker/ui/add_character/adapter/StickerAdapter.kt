package com.cat.emoji.fpf.maker.ui.add_character.adapter

import com.cat.emoji.fpf.maker.core.base.BaseAdapter
import com.cat.emoji.fpf.maker.core.extensions.loadImage
import com.cat.emoji.fpf.maker.core.extensions.loadImageSticker
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.data.model.SelectedModel
import com.cat.emoji.fpf.maker.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick : ((String) -> Unit) = {}
    override fun onBind(binding: ItemStickerBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSticker)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}