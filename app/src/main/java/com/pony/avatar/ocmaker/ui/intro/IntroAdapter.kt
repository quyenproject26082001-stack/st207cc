package com.pony.avatar.ocmaker.ui.intro

import android.content.Context
import com.pony.avatar.ocmaker.core.base.BaseAdapter
import com.pony.avatar.ocmaker.core.extensions.loadImage
import com.pony.avatar.ocmaker.core.extensions.select
import com.pony.avatar.ocmaker.core.extensions.strings
import com.pony.avatar.ocmaker.data.model.IntroModel
import com.pony.avatar.ocmaker.databinding.ItemIntroBinding

class IntroAdapter(val context: Context) : BaseAdapter<IntroModel, ItemIntroBinding>(
    ItemIntroBinding::inflate
) {
    override fun onBind(binding: ItemIntroBinding, item: IntroModel, position: Int) {
        binding.apply {
            loadImage(root, item.image, imvImage, false)
            tvContent.text = context.strings(item.content)
            tvContent.select()
        }
    }
}