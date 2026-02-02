package com.pony.avatar.ocmaker.data.model.custom

/**
 * Data class for persisting emoji edit state.
 * Similar to SuggestionModel but for EmojiCustomActivity.
 */
data class EmojiEditModel(
    var pathInternalEdit: String = "",                              // Saved emoji image path
    var drawItems: ArrayList<DrawItemModel> = arrayListOf(),        // All draw items in order
    var selectedByCategory: HashMap<String, String?> = hashMapOf() // categoryName → DrawItemModel.id
)
