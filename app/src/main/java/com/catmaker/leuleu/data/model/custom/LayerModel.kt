package com.catmaker.leuleu.data.model.custom

import com.catmaker.leuleu.data.model.custom.ColorModel

data class LayerModel(
    val image: String,
    val isMoreColors: Boolean = false,
    var listColor: ArrayList<ColorModel> = arrayListOf()
)