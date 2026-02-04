package com.catmaker.leuleu.ui.intro

import android.content.Context
import com.catmaker.leuleu.core.base.BaseAdapter
import com.catmaker.leuleu.core.extensions.loadImage
import com.catmaker.leuleu.core.extensions.select
import com.catmaker.leuleu.core.extensions.strings
import com.catmaker.leuleu.data.model.IntroModel
import com.catmaker.leuleu.databinding.ItemIntroBinding

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