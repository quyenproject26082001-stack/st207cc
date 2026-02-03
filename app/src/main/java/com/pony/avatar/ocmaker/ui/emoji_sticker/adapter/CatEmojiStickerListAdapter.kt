package com.pony.avatar.ocmaker.ui.emoji_sticker.adapter

import com.pony.avatar.ocmaker.core.base.BaseAdapter
import com.pony.avatar.ocmaker.core.extensions.loadImage
import com.pony.avatar.ocmaker.core.extensions.tap
import com.pony.avatar.ocmaker.databinding.ItemCatEmojiStickerListBinding

class CatEmojiStickerListAdapter : BaseAdapter<String, ItemCatEmojiStickerListBinding>(ItemCatEmojiStickerListBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}

    override fun onBind(binding: ItemCatEmojiStickerListBinding, item: String, position: Int) {
        binding.apply {
            loadImage(item, imvImage, onShowLoading = { sflShimmer.showShimmer(true) }, onDismissLoading = { sflShimmer.hideShimmer() })
            root.tap { onItemClick.invoke(item) }
        }
    }
}
