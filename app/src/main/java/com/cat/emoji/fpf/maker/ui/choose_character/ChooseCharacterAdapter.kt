package com.cat.emoji.fpf.maker.ui.choose_character

import com.cat.emoji.fpf.maker.core.base.BaseAdapter
import com.cat.emoji.fpf.maker.core.extensions.gone
import com.cat.emoji.fpf.maker.core.extensions.loadImage
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.data.model.custom.CustomizeModel
import com.cat.emoji.fpf.maker.databinding.ItemChooseAvatarBinding

class ChooseCharacterAdapter : BaseAdapter<CustomizeModel, ItemChooseAvatarBinding>(ItemChooseAvatarBinding::inflate) {
    var onItemClick: ((position: Int) -> Unit) = {}
    override fun onBind(binding: ItemChooseAvatarBinding, item: CustomizeModel, position: Int) {
        binding.apply {
            loadImage(item.avatar, imvImage, onDismissLoading = {
                sflShimmer.stopShimmer()
                sflShimmer.gone()
            })
            root.tap { onItemClick.invoke(position) }
        }
    }
}