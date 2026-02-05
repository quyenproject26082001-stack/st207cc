package com.cat.emoji.fpf.maker.core.custom.layout

import android.widget.ImageView
import com.cat.emoji.fpf.maker.core.custom.imageview.StrokeImageView

interface EventRatioFrame {
    fun onImageClick(image: StrokeImageView, btnEdit: ImageView)
}