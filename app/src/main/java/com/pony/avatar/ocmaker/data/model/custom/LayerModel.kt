package com.pony.avatar.ocmaker.data.model.custom

import com.pony.avatar.ocmaker.data.model.custom.ColorModel

data class LayerModel(
    val image: String,
    val isMoreColors: Boolean = false,
    var listColor: ArrayList<ColorModel> = arrayListOf()
)