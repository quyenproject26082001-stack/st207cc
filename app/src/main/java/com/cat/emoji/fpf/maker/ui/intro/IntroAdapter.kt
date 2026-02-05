package com.cat.emoji.fpf.maker.ui.intro

import android.content.Context
import com.cat.emoji.fpf.maker.core.base.BaseAdapter
import com.cat.emoji.fpf.maker.core.extensions.loadImage
import com.cat.emoji.fpf.maker.core.extensions.select
import com.cat.emoji.fpf.maker.core.extensions.strings
import com.cat.emoji.fpf.maker.data.model.IntroModel
import com.cat.emoji.fpf.maker.databinding.ItemIntroBinding

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