package com.catmaker.leuleu.data.model

data class StickerCategoryModel(
    val id: Int,
    val category: String,
    val level: Int,
    val quantity: Int = 0
)
