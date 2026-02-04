package com.catmaker.leuleu.ui.choose_character

import com.catmaker.leuleu.core.base.BaseAdapter
import com.catmaker.leuleu.core.extensions.gone
import com.catmaker.leuleu.core.extensions.loadImage
import com.catmaker.leuleu.core.extensions.tap
import com.catmaker.leuleu.data.model.custom.CustomizeModel
import com.catmaker.leuleu.databinding.ItemChooseAvatarBinding

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