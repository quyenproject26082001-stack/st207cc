package com.pony.avatar.ocmaker.ui.emoji_sticker.adapter

import com.pony.avatar.ocmaker.core.base.BaseAdapter
import com.pony.avatar.ocmaker.core.extensions.loadImage
import com.pony.avatar.ocmaker.core.extensions.tap
import com.pony.avatar.ocmaker.core.utils.key.DomainKey
import com.pony.avatar.ocmaker.data.model.StickerCategoryModel
import com.pony.avatar.ocmaker.databinding.ItemCatEmojiStickerBinding
import java.net.URLEncoder

class CatEmojiStickerAdapter : BaseAdapter<StickerCategoryModel, ItemCatEmojiStickerBinding>(ItemCatEmojiStickerBinding::inflate) {
    var onItemClick: ((StickerCategoryModel) -> Unit) = {}

    override fun onBind(binding: ItemCatEmojiStickerBinding, item: StickerCategoryModel, position: Int) {
        binding.apply {
            tvSticker.text = item.category
            val encodedCategory = URLEncoder.encode(item.category, "UTF-8").replace("+", "%20")
            val thumbUrl = "${DomainKey.BASE_URL}${DomainKey.SUB_DOMAIN_CAT_STICKER}/$encodedCategory/1${DomainKey.LAYER_EXTENSION}"
            loadImage(thumbUrl, imvImage, onShowLoading = { sflShimmer.showShimmer(true) }, onDismissLoading = { sflShimmer.hideShimmer() })
            root.tap { onItemClick.invoke(item) }
        }
    }
}
