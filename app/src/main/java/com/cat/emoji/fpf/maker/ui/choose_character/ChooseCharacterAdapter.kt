package com.cat.emoji.fpf.maker.ui.choose_character

import com.cat.emoji.fpf.maker.core.base.BaseAdapter
import com.cat.emoji.fpf.maker.core.extensions.gone
import com.cat.emoji.fpf.maker.core.extensions.loadImage
import com.cat.emoji.fpf.maker.core.extensions.loadImageChooseCharacter
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.core.extensions.visible
import com.cat.emoji.fpf.maker.data.model.custom.CustomizeModel
import com.cat.emoji.fpf.maker.databinding.ItemChooseAvatarBinding

class ChooseCharacterAdapter : BaseAdapter<CustomizeModel, ItemChooseAvatarBinding>(ItemChooseAvatarBinding::inflate) {
    var onItemClick: ((position: Int) -> Unit) = {}
    override fun onBind(binding: ItemChooseAvatarBinding, item: CustomizeModel, position: Int) {
        binding.apply {

            // 1) Reset UI trạng thái loading mỗi lần bind
            sflShimmer.visible()
            sflShimmer.startShimmer()

            // 2) Tránh flash ảnh cũ
            imvImage.setImageDrawable(null)


            loadImageChooseCharacter(
                path = item.avatar,
                imageView = imvImage,
                onDismissLoading = {
                    sflShimmer.stopShimmer()
                    sflShimmer.gone()
                }
            )
            root.tap { onItemClick.invoke(position) }
        }
    }
}