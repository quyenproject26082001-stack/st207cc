package com.cat.emoji.fpf.maker.ui.emoji_sticker.adapter

import android.util.Log
import com.cat.emoji.fpf.maker.core.base.BaseAdapter
import com.cat.emoji.fpf.maker.core.extensions.loadImage
import com.cat.emoji.fpf.maker.core.extensions.loadImageStickerList
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.core.utils.key.DomainKey
import com.cat.emoji.fpf.maker.data.model.StickerCategoryModel
import com.cat.emoji.fpf.maker.databinding.ItemCatEmojiStickerBinding
import java.net.URLEncoder

class CatEmojiStickerAdapter : BaseAdapter<StickerCategoryModel, ItemCatEmojiStickerBinding>(ItemCatEmojiStickerBinding::inflate) {
    var onItemClick: ((StickerCategoryModel) -> Unit) = {}

    override fun onBind(binding: ItemCatEmojiStickerBinding, item: StickerCategoryModel, position: Int) {
        binding.apply {
            tvSticker.text = item.category
            val encodedCategory = URLEncoder.encode(item.category, "UTF-8").replace("+", "%20")
            val thumbUrl = "${DomainKey.BASE_URL}${DomainKey.SUB_DOMAIN_CAT_STICKER}/Sticker/$encodedCategory/1${DomainKey.LAYER_EXTENSION}"
            Log.d("StickerAdapter", "Loading thumb URL: $thumbUrl")
            loadImageStickerList(thumbUrl, imvImage, onShowLoading = {
                sflShimmer.visibility = android.view.View.VISIBLE
                sflShimmer.showShimmer(true)
            }, onDismissLoading = {
                sflShimmer.hideShimmer()
                sflShimmer.visibility = android.view.View.GONE
            })
            root.tap { onItemClick.invoke(item) }
        }
    }
}
