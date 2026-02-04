package com.catmaker.leuleu.core.custom.layout

import android.widget.ImageView
import com.catmaker.leuleu.core.custom.imageview.StrokeImageView

interface EventRatioFrame {
    fun onImageClick(image: StrokeImageView, btnEdit: ImageView)
}