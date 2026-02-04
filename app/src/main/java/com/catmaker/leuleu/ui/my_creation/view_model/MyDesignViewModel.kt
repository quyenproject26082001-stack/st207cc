package com.catmaker.leuleu.ui.my_creation.view_model

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catmaker.leuleu.core.helper.MediaHelper
import com.catmaker.leuleu.core.utils.key.ValueKey
import com.catmaker.leuleu.core.utils.state.HandleState
import com.catmaker.leuleu.data.model.MyAlbumModel
import com.catmaker.leuleu.data.model.custom.EmojiEditModel
import com.catmaker.leuleu.data.model.custom.SuggestionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class MyDesignViewModel : ViewModel() {
    private val _myDesignList = MutableStateFlow<ArrayList<MyAlbumModel>>(arrayListOf())
    val myDesignList = _myDesignList.asStateFlow()
    private val _isLastItem = MutableStateFlow<Boolean>(false)
    val isLastItem: StateFlow<Boolean> = _isLastItem

    fun loadMyDesign(context: Context) {
        android.util.Log.d("MyDesignViewModel", "📂 loadMyDesign() START")
        android.util.Log.d("MyDesignViewModel", "Thread: ${Thread.currentThread().name}")
        android.util.Log.d("MyDesignViewModel", "Context: ${context.javaClass.simpleName}")
        android.util.Log.d("MyDesignViewModel", "Loading from: ValueKey.DOWNLOAD_ALBUM")

        try {
            val imageList = MediaHelper.getImageInternal(context, ValueKey.DOWNLOAD_ALBUM)
            android.util.Log.d("MyDesignViewModel", "✅ Loaded ${imageList.size} items from DOWNLOAD_ALBUM")

            imageList.forEachIndexed { index, path ->
                android.util.Log.d("MyDesignViewModel", "  [$index] path: $path")
                // Check if file exists
                val file = java.io.File(path)
                val exists = file.exists()
                val size = if (exists) file.length() else 0
                android.util.Log.d("MyDesignViewModel", "  [$index] File exists: $exists, Size: $size bytes")
            }

            val albumList = imageList.map { MyAlbumModel(it) }.toCollection(ArrayList())
            _myDesignList.value = albumList

            android.util.Log.d("MyDesignViewModel", "✅ Updated myDesignList with ${albumList.size} items")
            android.util.Log.d("MyDesignViewModel", "Current myDesignList size: ${_myDesignList.value.size}")
        } catch (e: Exception) {
            android.util.Log.e("MyDesignViewModel", "❌ ERROR loading designs: ${e.message}", e)
            _myDesignList.value = arrayListOf()
        }

        checkLastItem()
        android.util.Log.d("MyDesignViewModel", "📂 loadMyDesign() END")
    }

    fun showLongClick(positionSelect: Int) {
        _myDesignList.value = _myDesignList.value.mapIndexed { position, item ->
            item.copy(isSelected = position == positionSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    private fun checkLastItem() {
        _isLastItem.value = _myDesignList.value.any { !it.isSelected }
    }

    suspend fun deleteItem(context: Context, pathList: ArrayList<String>){
        MediaHelper.deleteFileByPathNotFlow(pathList)
        // Also delete from emoji edit list if applicable
        deleteEmojiFromEditList(context, pathList)
    }

    fun toggleSelect(position: Int) {
        val list = _myDesignList.value.toMutableList()
        list[position] = list[position].copy(isSelected = !list[position].isSelected, isShowSelection = true)
        _myDesignList.value = list.toCollection(ArrayList())
        checkLastItem()
    }

    fun selectAll(shouldSelect: Boolean) {
        _myDesignList.value = _myDesignList.value.map {
            it.copy(isSelected = shouldSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    fun getPathSelected() : ArrayList<String>{
        return _myDesignList.value.filter { it.isSelected }.map { it.path }.toCollection(ArrayList())
    }

    fun clearSelection() {
        _myDesignList.value = _myDesignList.value.map {
            it.copy(isSelected = false, isShowSelection = false)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    // ========== EMOJI EDIT SUPPORT ==========

    /**
     * Check if the given path is an editable emoji (exists in emoji edit list)
     */
    fun isEmojiEdit(context: Context, pathInternal: String): Boolean {
        return try {
            val emojiEditList = MediaHelper.readListFromFile<EmojiEditModel>(
                context,
                ValueKey.EMOJI_EDIT_FILE_INTERNAL
            )
            emojiEditList.any { it.pathInternalEdit == pathInternal }
        } catch (e: Exception) {
            android.util.Log.e("MyDesignViewModel", "Error checking emoji edit: ${e.message}")
            false
        }
    }

    /**
     * Prepare emoji for editing - writes to suggestion file for EmojiCustomActivity to read
     */
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
            android.util.Log.e("MyDesignViewModel", "Error preparing emoji edit: ${e.message}")
            false
        }
    }

    /**
     * Delete emoji from edit list when deleting the image
     */
    suspend fun deleteEmojiFromEditList(context: Context, pathList: ArrayList<String>) {
        try {
            val emojiEditList = MediaHelper.readListFromFile<EmojiEditModel>(
                context,
                ValueKey.EMOJI_EDIT_FILE_INTERNAL
            ).toCollection(ArrayList())

            val updatedList = emojiEditList.filter { it.pathInternalEdit !in pathList }
            MediaHelper.writeListToFile(
                context,
                ValueKey.EMOJI_EDIT_FILE_INTERNAL,
                updatedList
            )
        } catch (e: Exception) {
            android.util.Log.e("MyDesignViewModel", "Error deleting emoji from edit list: ${e.message}")
        }
    }
}