package com.catmaker.leuleu.data.model

data class MyAlbumModel(
    val path: String,
    var isShowSelection: Boolean = false,
    var isSelected: Boolean = false,
    var isEmoji: Boolean = false  // true = emoji edit, false = avatar edit
)
