package com.cat.emoji.fpf.maker.ui.my_creation.view_model

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseActivity
import com.cat.emoji.fpf.maker.core.helper.InternetHelper
import com.cat.emoji.fpf.maker.core.helper.MediaHelper
import com.cat.emoji.fpf.maker.core.utils.key.ValueKey
import com.cat.emoji.fpf.maker.core.utils.state.HandleState
import com.cat.emoji.fpf.maker.data.model.MyAlbumModel
import com.cat.emoji.fpf.maker.data.model.custom.CustomizeModel
import com.cat.emoji.fpf.maker.data.model.custom.EmojiEditModel
import com.cat.emoji.fpf.maker.data.model.custom.SuggestionModel
import com.cat.emoji.fpf.maker.ui.my_creation.MyCreationActivity
import com.cat.emoji.fpf.maker.ui.random_character.RandomCharacterActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class MyAvatarViewModel : ViewModel() {
    private val _myAvatarList = MutableStateFlow<ArrayList<MyAlbumModel>>(arrayListOf())
    val myAvatarList = _myAvatarList.asStateFlow()
    private val _isLastItem = MutableStateFlow<Boolean>(false)
    val isLastItem: StateFlow<Boolean> = _isLastItem


    var isApi: Boolean = false
    var positionCharacter = -1
    var editModel = SuggestionModel()

    fun loadMyAvatar(context: Context) {
        android.util.Log.d("MyAvatarViewModel", "📂 loadMyAvatar() START")
        android.util.Log.d("MyAvatarViewModel", "Thread: ${Thread.currentThread().name}")
        android.util.Log.d("MyAvatarViewModel", "Context: ${context.javaClass.simpleName}")

        val albumList = ArrayList<MyAlbumModel>()

        try {
            // Load avatar edit list
            val avatarEditList = MediaHelper.readListFromFile<SuggestionModel>(context, ValueKey.EDIT_FILE_INTERNAL)
            android.util.Log.d("MyAvatarViewModel", "✅ Loaded ${avatarEditList.size} avatars from EDIT_FILE_INTERNAL")

            avatarEditList.forEach { suggestion ->
                val file = java.io.File(suggestion.pathInternalEdit)
                if (file.exists()) {
                    albumList.add(MyAlbumModel(suggestion.pathInternalEdit, isEmoji = false))
                }
            }

            _myAvatarList.value = albumList

            android.util.Log.d("MyAvatarViewModel", "✅ Updated myAvatarList with ${albumList.size} avatars")
        } catch (e: Exception) {
            android.util.Log.e("MyAvatarViewModel", "❌ ERROR loading avatars: ${e.message}", e)
            _myAvatarList.value = arrayListOf()
        }

        checkLastItem()
        android.util.Log.d("MyAvatarViewModel", "📂 loadMyAvatar() END")
    }

    private fun checkLastItem() {
        _isLastItem.value = _myAvatarList.value.any { !it.isSelected }
    }

    suspend fun deleteItem(context: Context, pathList: ArrayList<String>) {
        // Delete from avatar edit list
        val avatarOriginList = MediaHelper
            .readListFromFile<SuggestionModel>(context, ValueKey.EDIT_FILE_INTERNAL)
            .toCollection(ArrayList())

        val avatarDeleteList = avatarOriginList.filter { it.pathInternalEdit in pathList }
        val newAvatarOriginList = ArrayList(avatarOriginList).apply {
            removeAll(avatarDeleteList)
        }
        MediaHelper.writeListToFile(context, ValueKey.EDIT_FILE_INTERNAL, newAvatarOriginList)

        // Update StateFlow
        val myAvatarDeleteList = _myAvatarList.value.filter { it.path in pathList }
        val newAvatarList = ArrayList(_myAvatarList.value).apply {
            removeAll(myAvatarDeleteList)
        }
        _myAvatarList.value = newAvatarList
    }

    suspend fun editItem(context: Context, pathInternal: String, allData: ArrayList<CustomizeModel>){
        val originList = MediaHelper
            .readListFromFile<SuggestionModel>(context, ValueKey.EDIT_FILE_INTERNAL)
            .toCollection(ArrayList())

        editModel = originList.first { it.pathInternalEdit == pathInternal }
        positionCharacter = allData.indexOfFirst { it.avatar == editModel.avatarPath }
        // ✅ FIX: Use isFromAPI flag from character data instead of position
        isApi = if (positionCharacter >= 0) allData[positionCharacter].isFromAPI else false
        MediaHelper.writeModelToFile(context, ValueKey.SUGGESTION_FILE_INTERNAL, editModel)
    }

    fun checkDataInternet(context: BaseActivity<*>, action: (() -> Unit)) {
        if (!isApi) {
            action.invoke()
            return
        }
        InternetHelper.checkInternet(context) { result ->
            if (result == HandleState.SUCCESS) {
                action.invoke()
            } else {
                // Show No Internet dialog
                val dialog = com.cat.emoji.fpf.maker.dialog.YesNoDialog(
                    context,
                    com.cat.emoji.fpf.maker.R.string.no_internet,
                    com.cat.emoji.fpf.maker.R.string.please_check_your_internet,
                    isError = true,
                    dialogType = com.cat.emoji.fpf.maker.dialog.DialogType.INTERNET
                )
                dialog.show()
                dialog.onYesClick = {
                    dialog.dismiss()
                }
            }
        }
    }

    fun showLongClick(positionSelect: Int) {
        _myAvatarList.value = _myAvatarList.value.mapIndexed { position, item ->
            item.copy(isSelected = position == positionSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    fun selectAll(shouldSelect: Boolean) {
        _myAvatarList.value = _myAvatarList.value.map {
            it.copy(isSelected = shouldSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    fun toggleSelect(position: Int) {
        val list = _myAvatarList.value.toMutableList()
        list[position] = list[position].copy(isSelected = !list[position].isSelected, isShowSelection = true)
        _myAvatarList.value = list.toCollection(ArrayList())
        checkLastItem()
    }

    fun getPathSelected() : ArrayList<String>{
        return _myAvatarList.value
            .filter { it.isSelected }
            .map { it.path }
            .toCollection(ArrayList())
    }

    fun clearSelection() {
        _myAvatarList.value = _myAvatarList.value.map {
            it.copy(isSelected = false, isShowSelection = false)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    // ========== EMOJI EDIT SUPPORT ==========

    /**
     * Check if the given path is an emoji (from myAvatarList)
     */
    fun isEmoji(pathInternal: String): Boolean {
        return _myAvatarList.value.find { it.path == pathInternal }?.isEmoji ?: false
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
            android.util.Log.e("MyAvatarViewModel", "Error preparing emoji edit: ${e.message}")
            false
        }
    }
}