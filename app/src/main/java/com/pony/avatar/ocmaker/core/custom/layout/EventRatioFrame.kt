package com.pony.avatar.ocmaker.core.custom.layout

import android.widget.ImageView
import com.pony.avatar.ocmaker.core.custom.imageview.StrokeImageView

interface EventRatioFrame {
    fun onImageClick(image: StrokeImageView, btnEdit: ImageView)
}