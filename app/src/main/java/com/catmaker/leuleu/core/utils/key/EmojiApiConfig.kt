package com.catmaker.leuleu.core.utils.key

import java.net.URLEncoder

object EmojiApiConfig {
    const val BASE_URL = "https://emoji-maker.com/assets/emojis/"

    // Categories với số lượng items
    val CATEGORIES = listOf(
        EmojiCategory("Shape", 68, "png"),
        EmojiCategory("More Shape", 48, "png"),
        EmojiCategory("Eyes", 161, "png"),
        EmojiCategory("Eyes Big", 293, "png"),
        EmojiCategory("Eyebrows", 47, "png"),
        EmojiCategory("Happy Mouth", 200, "png"),
        EmojiCategory("Sad Mouth", 202, "png"),
        EmojiCategory("Nose", 24, "png"),
        EmojiCategory("Beard", 45, "png"),
        EmojiCategory("Stache", 42, "png"),
        EmojiCategory("Glasses", 188, "png"),
        EmojiCategory("Hair", 140, "png"),
        EmojiCategory("Mask", 98, "png"),
        EmojiCategory("Misc", 123, "png"),
        EmojiCategory("Hats", 291, "png"),
        EmojiCategory("Hands", 169, "png"),
        EmojiCategory("Pattern Shape", 7, "png")
    )

    // Backgrounds có format khác: bg_1.jpg
    val BACKGROUNDS = EmojiCategory("Backgrounds", 56, "jpg", "bg_")

    fun getImageUrl(category: String, index: Int): String {
        val encodedCategory = URLEncoder.encode(category, "UTF-8").replace("+", "%20")
        return if (category == "Backgrounds") {
            "${BASE_URL}Backgrounds/bg_$index.jpg"
        } else {
            "$BASE_URL$encodedCategory/$index.png"
        }
    }

    fun getCategoryByName(name: String): EmojiCategory? {
        return CATEGORIES.find { it.name == name } ?: if (name == "Backgrounds") BACKGROUNDS else null
    }
}

data class EmojiCategory(
    val name: String,
    val count: Int,
    val extension: String,
    val prefix: String = ""
)
