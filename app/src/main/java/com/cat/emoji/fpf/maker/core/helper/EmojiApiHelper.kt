package com.cat.emoji.fpf.maker.core.helper

import com.cat.emoji.fpf.maker.core.utils.key.EmojiApiConfig
import com.cat.emoji.fpf.maker.core.utils.key.EmojiCategory
import com.cat.emoji.fpf.maker.data.model.custom.CustomizeModel
import com.cat.emoji.fpf.maker.data.model.custom.LayerListModel
import com.cat.emoji.fpf.maker.data.model.custom.LayerModel
import java.net.URLEncoder

object EmojiApiHelper {

    // Thứ tự vẽ layers (từ dưới lên trên)
    private val LAYER_ORDER = listOf(
        "Backgrounds",      // 0 - Nền
        "Shape",            // 1 - Hình dạng cơ bản
        "Pattern Shape",    // 2 - Pattern trên shape
        "Eyes",             // 3 - Mắt
        "Eyes Big",         // 4 - Mắt to
        "Eyebrows",         // 5 - Lông mày
        "Nose",             // 6 - Mũi
        "Happy Mouth",      // 7 - Miệng vui
        "Sad Mouth",        // 8 - Miệng buồn
        "Beard",            // 9 - Râu
        "Stache",           // 10 - Ria mép
        "Glasses",          // 11 - Kính
        "Hair",             // 12 - Tóc
        "Hats",             // 13 - Mũ
        "Mask",             // 14 - Mặt nạ
        "Hands",            // 15 - Tay
        "Misc",             // 16 - Khác
        "More Shape"        // 17 - Shape thêm
    )

    // Items to exclude per category (e.g. contains cigarette)
    val EXCLUDED_ITEMS = mapOf(
        "Happy Mouth" to setOf(125, 126, 127, 128, 129, 130, 134, 156, 157, 158, 160),
        "Misc" to setOf(83,85,86,87),
        "Hair" to setOf(17, 130),              // ✅ thêm 130
        "Eyebrows" to setOf(134,159),
        "Nose" to setOf(134,159),
        "Hats" to setOf(17),
        "Mask" to setOf(18),


        "Eyes Big" to setOf(256, 257),   // ✅ add theo log mới

        // ✅ New 404 excludes from your log:
        "Eyes" to setOf(111),
        "Sad Mouth" to setOf(198),
        "Hands" to setOf(2, 36, 61, 64, 92, 120, 125, 163),
        "Backgrounds" to setOf(32)                // bg_32.jpg (404)
    )

    fun buildEmojiCustomizeModel(): CustomizeModel {
        val layerList = ArrayList<LayerListModel>()

        LAYER_ORDER.forEachIndexed { navIndex, categoryName ->
            val category = EmojiApiConfig.getCategoryByName(categoryName) ?: return@forEachIndexed

            val layers = buildLayersForCategory(category)
            val navigationImage = if (layers.isNotEmpty()) layers.first().image else ""

            val layerListModel = LayerListModel(
                positionCustom = navIndex,      // Thứ tự vẽ
                positionNavigation = navIndex,  // Thứ tự trong navigation
                imageNavigation = navigationImage,
                layer = layers
            )
            layerList.add(layerListModel)
        }

        // Avatar dùng Shape đầu tiên
        val avatarUrl = EmojiApiConfig.getImageUrl("Shape", 1)

        return CustomizeModel(
            dataName = "Emoji Maker",
            avatar = avatarUrl,
            layerList = layerList,
            level = 1,
            isFromAPI = true
        )
    }

    private fun buildLayersForCategory(category: EmojiCategory): ArrayList<LayerModel> {
        val layers = ArrayList<LayerModel>()
        val encodedName = URLEncoder.encode(category.name, "UTF-8").replace("+", "%20")
        val excluded = EXCLUDED_ITEMS[category.name] ?: emptySet()

        for (i in 1..category.count) {
            if (i in excluded) continue
            val imageUrl = if (category.prefix.isNotEmpty()) {
                "${EmojiApiConfig.BASE_URL}$encodedName/${category.prefix}$i.${category.extension}"
            } else {
                "${EmojiApiConfig.BASE_URL}$encodedName/$i.${category.extension}"
            }

            layers.add(
                LayerModel(
                    image = imageUrl,
                    isMoreColors = false,
                    listColor = arrayListOf()
                )
            )
        }

        return layers
    }

    fun getAllCategories(): List<EmojiCategory> {
        return EmojiApiConfig.CATEGORIES + listOf(EmojiApiConfig.BACKGROUNDS)
    }
}
