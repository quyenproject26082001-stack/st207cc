package com.cat.emoji.fpf.maker.ui.my_creation.view_model

import android.content.Context
import androidx.lifecycle.ViewModel
import com.cat.emoji.fpf.maker.core.helper.MediaHelper
import com.cat.emoji.fpf.maker.core.utils.key.ValueKey
import com.cat.emoji.fpf.maker.data.model.MyAlbumModel
import com.cat.emoji.fpf.maker.data.model.custom.EmojiEditModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MyEmojiViewModel : ViewModel() {
    private val _myEmojiList = MutableStateFlow<ArrayList<MyAlbumModel>>(arrayListOf())
    val myEmojiList = _myEmojiList.asStateFlow()

    fun loadMyEmoji(context: Context) {
        val albumList = ArrayList<MyAlbumModel>()
        try {
            val emojiEditList = MediaHelper.readListFromFile<EmojiEditModel>(context, ValueKey.EMOJI_EDIT_FILE_INTERNAL)
            emojiEditList.forEach { emojiModel ->
                val file = java.io.File(emojiModel.pathInternalEdit)
                if (file.exists()) {
                    albumList.add(MyAlbumModel(emojiModel.pathInternalEdit, isEmoji = true))
                }
            }
            _myEmojiList.value = albumList
        } catch (e: Exception) {
            _myEmojiList.value = arrayListOf()
        }
    }

    suspend fun deleteItem(context: Context, pathList: ArrayList<String>) {
        val emojiOriginList = MediaHelper
            .readListFromFile<EmojiEditModel>(context, ValueKey.EMOJI_EDIT_FILE_INTERNAL)
            .toCollection(ArrayList())

        val newList = ArrayList(emojiOriginList).apply {
            removeAll { it.pathInternalEdit in pathList }
        }
        MediaHelper.writeListToFile(context, ValueKey.EMOJI_EDIT_FILE_INTERNAL, newList)

        val newEmojiList = ArrayList(_myEmojiList.value).apply {
            removeAll { it.path in pathList }
        }
        _myEmojiList.value = newEmojiList
    }

    suspend fun prepareEmojiEdit(context: Context, pathInternal: String): Boolean {
        return try {
            val emojiEditList = MediaHelper.readListFromFile<EmojiEditModel>(
                context,
                ValueKey.EMOJI_EDIT_FILE_INTERNAL
            )
            val editModel = emojiEditList.firstOrNull { it.pathInternalEdit == pathInternal }
            if (editModel != null) {
                MediaHelper.writeModelToFile(
                    context,
                    ValueKey.EMOJI_SUGGESTION_FILE_INTERNAL,
                    editModel
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun showLongClick(positionSelect: Int) {
        _myEmojiList.value = _myEmojiList.value.mapIndexed { position, item ->
            item.copy(isSelected = position == positionSelect, isShowSelection = true)
        }.toCollection(ArrayList())
    }

    fun toggleSelect(position: Int) {
        val list = _myEmojiList.value.toMutableList()
        list[position] = list[position].copy(isSelected = !list[position].isSelected, isShowSelection = true)
        _myEmojiList.value = list.toCollection(ArrayList())
    }

    fun selectAll(shouldSelect: Boolean) {
        _myEmojiList.value = _myEmojiList.value.map {
            it.copy(isSelected = shouldSelect, isShowSelection = true)
        }.toCollection(ArrayList())
    }

    fun getPathSelected(): ArrayList<String> {
        return _myEmojiList.value
            .filter { it.isSelected }
            .map { it.path }
            .toCollection(ArrayList())
    }

    fun clearSelection() {
        _myEmojiList.value = _myEmojiList.value.map {
            it.copy(isSelected = false, isShowSelection = false)
        }.toCollection(ArrayList())
    }
}
