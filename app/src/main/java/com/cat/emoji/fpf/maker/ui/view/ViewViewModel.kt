package com.cat.emoji.fpf.maker.ui.view

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cat.emoji.fpf.maker.core.extensions.shareImagesPaths
import com.cat.emoji.fpf.maker.core.helper.MediaHelper
import com.cat.emoji.fpf.maker.core.utils.key.ValueKey
import com.cat.emoji.fpf.maker.core.utils.state.HandleState
import com.cat.emoji.fpf.maker.data.model.custom.SuggestionModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class ViewViewModel : ViewModel() {
    private val _pathInternal = MutableStateFlow<String>("")
    val pathInternal: StateFlow<String> = _pathInternal.asStateFlow()

    var statusFrom = ValueKey.AVATAR_TYPE

    fun setPath(path: String) {
        _pathInternal.value = path
    }

    fun deleteFile(context: Context, path: String): Flow<HandleState> = flow {
        if (statusFrom == ValueKey.MY_DESIGN_TYPE) {
            emitAll(MediaHelper.deleteFileByPath(arrayListOf(path)))
        } else {
            emit(HandleState.LOADING)
            try {
                // First try to remove from the generic EDIT_FILE_INTERNAL (SuggestionModel)
                val originList = MediaHelper
                    .readListFromFile<SuggestionModel>(context, ValueKey.EDIT_FILE_INTERNAL)
                    .toMutableList()

                val editDelete = originList.firstOrNull { it.pathInternalEdit == path }

                if (editDelete != null) {
                    originList.remove(editDelete)
                    MediaHelper.writeListToFile(context, ValueKey.EDIT_FILE_INTERNAL, originList)
                    emit(HandleState.SUCCESS)
                    return@flow
                }

                // If not found there, try the emoji edit file (emoji edits are stored separately)
                val emojiList = MediaHelper
                    .readListFromFile<com.cat.emoji.fpf.maker.data.model.custom.EmojiEditModel>(
                        context, ValueKey.EMOJI_EDIT_FILE_INTERNAL
                    )
                    .toMutableList()

                val emojiDelete = emojiList.firstOrNull { it.pathInternalEdit == path }

                if (emojiDelete != null) {
                    emojiList.remove(emojiDelete)
                    MediaHelper.writeListToFile(context, ValueKey.EMOJI_EDIT_FILE_INTERNAL, emojiList)
                    emit(HandleState.SUCCESS)
                    return@flow
                }

                // Item not found in either list -> fail
                Log.e("ViewViewModel", "deleteFile: item not found in edit lists for path=$path")
                emit(HandleState.FAIL)
            } catch (e: Exception) {
                Log.e("nbhieu", "deleteFile: $e")
                emit(HandleState.FAIL)
            }
        }
    }

    fun shareFiles(context: Activity) {
        viewModelScope.launch {
            context.shareImagesPaths(arrayListOf(_pathInternal.value))
        }
    }

    fun downloadFiles(context: Activity): Flow<HandleState> = flow {
        emitAll(
            MediaHelper.downloadPartsToExternal(
                context, arrayListOf(_pathInternal.value)
            )
        )
    }

    fun updateStatusFrom(status: Int) {
        statusFrom = status
    }
}
