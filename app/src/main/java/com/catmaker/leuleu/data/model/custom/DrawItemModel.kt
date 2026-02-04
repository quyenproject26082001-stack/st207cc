package com.catmaker.leuleu.data.model.custom

/**
 * Data class for serializing individual draw items in EmojiCustomActivity.
 * Used for persisting DrawableDraw and TextDraw state for edit functionality.
 */
data class DrawItemModel(
    // === IDENTITY ===
    var id: String = "",                          // UUID, generated at save time
    var type: String = "drawable",                // "drawable" | "text"

    // === COMMON Draw FIELDS ===
    var drawablePath: String = "",                // URL for stickers, file path for freehand
    var matrixValues: ArrayList<Float> = arrayListOf(),  // 9 floats from Matrix
    var isFlippedH: Boolean = false,
    var isFlippedV: Boolean = false,
    var isLock: Boolean = false,
    var isHide: Boolean = false,
    var pagerSelected: Int = 0,
    var positionSelected: Int = 0,
    var isCharacter: Boolean = false,
    var alpha: Int = 255,

    // === TEXT-SPECIFIC FIELDS ===
    var isText: Boolean = false,
    var text: String? = null,
    var textColor: Int? = null,
    var textAlignString: String = "ALIGN_CENTER",  // Exact enum name
    var idTypeFace: Int = 0,
    var textCheckAlign: String? = null,

    // === TEXT GRADIENT FIELDS ===
    var isTextGradient: Boolean = false,
    var textGradient: ArrayList<String> = arrayListOf(),
    var positionGradient: Int? = null
)
