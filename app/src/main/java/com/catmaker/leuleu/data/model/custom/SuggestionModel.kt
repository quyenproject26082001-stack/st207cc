package com.catmaker.leuleu.data.model.custom

data class SuggestionModel (
    var avatarPath: String = "",
    var positionColorItemList : ArrayList<Int> = arrayListOf(),
    var itemNavList : ArrayList<ArrayList<ItemNavCustomModel>> = arrayListOf(),
    var colorItemNavList : ArrayList<ArrayList<ItemColorModel>> = arrayListOf(),
    var isSelectedItemList : ArrayList<Boolean> = arrayListOf(),
    var keySelectedItemList : ArrayList<String> = arrayListOf(),
    var isShowColorList : ArrayList<Boolean> = arrayListOf(),
    var pathSelectedList : ArrayList<String> = arrayListOf(),
    var pathInternalRandom: String = "",
    var pathInternalEdit: String = "",
    var dataType: String = "",
)