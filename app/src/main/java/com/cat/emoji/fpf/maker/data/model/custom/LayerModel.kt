package com.cat.emoji.fpf.maker.data.model.custom

import com.cat.emoji.fpf.maker.data.model.custom.ColorModel

data class LayerModel(
    val image: String,
    val isMoreColors: Boolean = false,
    var listColor: ArrayList<ColorModel> = arrayListOf()
)