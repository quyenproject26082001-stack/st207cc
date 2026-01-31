package com.pony.avatar.ocmaker.ui.add_character

import android.R.attr.bitmap
import android.R.attr.type
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModel
import com.pony.avatar.ocmaker.core.helper.AssetHelper
import com.pony.avatar.ocmaker.core.helper.BitmapHelper
import com.pony.avatar.ocmaker.core.helper.MediaHelper
import com.pony.avatar.ocmaker.core.utils.DataLocal
import com.pony.avatar.ocmaker.core.utils.key.AssetsKey
import com.pony.avatar.ocmaker.core.utils.key.ValueKey
import com.pony.avatar.ocmaker.core.utils.state.SaveState
import com.pony.avatar.ocmaker.data.model.SelectedModel
import com.pony.avatar.ocmaker.data.model.draw.Draw
import com.pony.avatar.ocmaker.data.model.draw.DrawableDraw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Date

class AddCharacterViewModel : ViewModel() {
    // Data class để lưu state snapshot cho undo/redo
    data class AddCharacterState(
        val backgroundImageList: ArrayList<SelectedModel>,
        val backgroundColorList: ArrayList<SelectedModel>,
        val stickerList: ArrayList<SelectedModel>,
        val speechList: ArrayList<SelectedModel>,
        val textFontList: ArrayList<SelectedModel>,
        val textColorList: ArrayList<SelectedModel>,
        val drawViewList: ArrayList<DrawableDraw>,
        val typeNavigation: Int,
        val typeBackground: Int,
        val backgroundImagePath: String,  // Path của background image hiện tại
        val backgroundColor: Int,          // Màu background hiện tại
        val currentText: String,          // Text trong EditText
        val currentTextFont: Int,         // Font đang dùng
        val currentTextColor: Int         // Màu text đang dùng
    )

    // Undo/Redo stacks
    private val undoStack = ArrayDeque<AddCharacterState>()
    private val redoStack = ArrayDeque<AddCharacterState>()
    private val maxHistorySize = 50

    // StateFlows để track trạng thái undo/redo
    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    var backgroundImageList: ArrayList<SelectedModel> = arrayListOf()
    var backgroundColorList: ArrayList<SelectedModel> = arrayListOf()
    var stickerList: ArrayList<SelectedModel> = arrayListOf()
    var speechList: ArrayList<SelectedModel> = arrayListOf()
    var textFontList: ArrayList<SelectedModel> = arrayListOf()
    var textColorList: ArrayList<SelectedModel> = arrayListOf()

    private val _typeNavigation = MutableStateFlow<Int>(-1)
    val typeNavigation = _typeNavigation.asStateFlow()

    private val _typeBackground = MutableStateFlow<Int>(-1)
    val typeBackground = _typeBackground.asStateFlow()

    private val _isFocusEditText = MutableStateFlow<Boolean>(false)
    val isFocusEditText = _isFocusEditText.asStateFlow()

    var currentDraw: Draw? = null

    var drawViewList: ArrayList<DrawableDraw> = arrayListOf()

    lateinit var layoutParams: ViewGroup.MarginLayoutParams

    var originalMarginBottom: Int = 0

    var pathDefault = ""

    // Cache cho restore UI
    var cachedBackgroundImagePath: String = ""
    var cachedBackgroundColor: Int = android.graphics.Color.TRANSPARENT
    var cachedCurrentText: String = ""
    var cachedCurrentTextFont: Int = 0
    var cachedCurrentTextColor: Int = android.graphics.Color.BLACK

    fun setTypeNavigation(type: Int) {
        _typeNavigation.value = type
    }

    fun setTypeBackground(type: Int) {
        _typeBackground.value = type
    }

    fun setIsFocusEditText(status: Boolean) {
        _isFocusEditText.value = status
    }

    suspend fun loadDataDefault(context: Context) {
        backgroundImageList.clear()
        backgroundImageList.addAll(
            AssetHelper.getSubfoldersAsset(context, AssetsKey.BACKGROUND_ASSET).map { SelectedModel(path = it) })

        backgroundColorList.clear()
        backgroundColorList.addAll(DataLocal.getBackgroundColorDefault(context))


        stickerList.clear()
        stickerList.addAll(
            AssetHelper.getSubfoldersAsset(context, AssetsKey.STICKER_ASSET).map { SelectedModel(path = it) })


        speechList.clear()
        speechList.addAll(
            AssetHelper.getSubfoldersAsset(context, AssetsKey.SPEECH_ASSET).map { SelectedModel(path = it) })

        textFontList.clear()
        textFontList.addAll(DataLocal.getTextFontDefault())
        textFontList.first().isSelected = true

        textColorList.clear()
        textColorList.addAll(DataLocal.getTextColorDefault(context))
        textColorList[1].isSelected = true
    }

    suspend fun updateBackgroundImageSelected(position: Int) {
        backgroundColorList = backgroundColorList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        backgroundImageList.forEachIndexed { index, model ->
            model.isSelected = index == position
        }
    }

    suspend fun updateBackgroundColorSelected(position: Int) {
        Log.d("AddCharacterViewModel", "updateBackgroundColorSelected called with position=$position")
        Log.d("AddCharacterViewModel", "Before update: backgroundColorList[0].color=${String.format("#%06X", 0xFFFFFF and backgroundColorList[0].color)}, isSelected=${backgroundColorList[0].isSelected}")

        backgroundImageList = backgroundImageList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        backgroundColorList.forEachIndexed { index, model ->
            Log.d("AddCharacterViewModel", "Setting position $index isSelected = ${index == position}")
            model.isSelected = index == position
        }

        Log.d("AddCharacterViewModel", "After update: backgroundColorList[0].color=${String.format("#%06X", 0xFFFFFF and backgroundColorList[0].color)}, isSelected=${backgroundColorList[0].isSelected}")
    }

    fun updateTextFontSelected(position: Int) {
        textFontList = textFontList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        textFontList.forEachIndexed { index, model ->
            model.isSelected = index == position
        }
    }

    fun updateTextColorSelected(position: Int) {
        Log.d("AddCharacterViewModel", "updateTextColorSelected called with position=$position")
        Log.d("AddCharacterViewModel", "Before update: textColorList[0].color=${String.format("#%06X", 0xFFFFFF and textColorList[0].color)}, isSelected=${textColorList[0].isSelected}")

        textColorList = textColorList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        textColorList.forEachIndexed { index, model ->
            Log.d("AddCharacterViewModel", "Setting position $index isSelected = ${index == position}")
            model.isSelected = index == position
        }

        Log.d("AddCharacterViewModel", "After update: textColorList[0].color=${String.format("#%06X", 0xFFFFFF and textColorList[0].color)}, isSelected=${textColorList[0].isSelected}")
    }

    fun updateCurrentCurrentDraw(draw: Draw) {
        currentDraw = draw
    }

    fun addDrawView(draw: Draw) {
        if (draw is DrawableDraw) {
            drawViewList.add(draw)
        }
    }

    fun deleteDrawView(draw: Draw) {
        drawViewList.removeIf { it == draw }
    }

    fun updatePathDefault(path: String){
        pathDefault = path
    }
    fun loadDrawableEmoji(context: Context, bitmap: Bitmap, isCharacter: Boolean = false, isText: Boolean = false): DrawableDraw {
        val drawable = bitmap.toDrawable(context.resources)
        val drawableEmoji = DrawableDraw(drawable, "${SimpleDateFormat("dd_MM_yyyy_hh_mm_ss").format(Date())}.png")
        drawableEmoji.isCharacter = isCharacter
        drawableEmoji.isText = isText
        return drawableEmoji
    }

    fun resetDraw() {
        drawViewList.clear()

    }

    fun saveImageFromView(context: Context, view: View): Flow<SaveState> = flow {
        emit(SaveState.Loading)
        val bitmap = BitmapHelper.createBimapFromView(view)
        MediaHelper.saveBitmapToInternalStorage(context, ValueKey.DOWNLOAD_ALBUM, bitmap).collect { state ->
            emit(state)
        }
    }.flowOn(Dispatchers.IO)

    //----------------------------------------------------------------------------------------------------------------------
    // Undo/Redo Functions

    /**
     * Lưu state hiện tại vào undo stack trước khi thực hiện thay đổi
     */
    fun saveStateForUndo() {
        val currentState = AddCharacterState(
            backgroundImageList = ArrayList(backgroundImageList.map { it.copy() }),
            backgroundColorList = ArrayList(backgroundColorList.map { it.copy() }),
            stickerList = ArrayList(stickerList.map { it.copy() }),
            speechList = ArrayList(speechList.map { it.copy() }),
            textFontList = ArrayList(textFontList.map { it.copy() }),
            textColorList = ArrayList(textColorList.map { it.copy() }),
            drawViewList = ArrayList(drawViewList),  // Draw objects are immutable
            typeNavigation = _typeNavigation.value,
            typeBackground = _typeBackground.value,
            backgroundImagePath = cachedBackgroundImagePath,
            backgroundColor = cachedBackgroundColor,
            currentText = cachedCurrentText,
            currentTextFont = cachedCurrentTextFont,
            currentTextColor = cachedCurrentTextColor
        )

        undoStack.addLast(currentState)

        // Giới hạn kích thước stack
        if (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }

        // Clear redo stack khi có action mới
        redoStack.clear()

        updateUndoRedoState()
    }

    /**
     * Undo - khôi phục state trước đó
     */
    suspend fun performUndo(): Boolean {
        if (undoStack.isEmpty()) return false

        // Lưu state hiện tại vào redo stack
        val currentState = AddCharacterState(
            backgroundImageList = ArrayList(backgroundImageList.map { it.copy() }),
            backgroundColorList = ArrayList(backgroundColorList.map { it.copy() }),
            stickerList = ArrayList(stickerList.map { it.copy() }),
            speechList = ArrayList(speechList.map { it.copy() }),
            textFontList = ArrayList(textFontList.map { it.copy() }),
            textColorList = ArrayList(textColorList.map { it.copy() }),
            drawViewList = ArrayList(drawViewList),
            typeNavigation = _typeNavigation.value,
            typeBackground = _typeBackground.value,
            backgroundImagePath = cachedBackgroundImagePath,
            backgroundColor = cachedBackgroundColor,
            currentText = cachedCurrentText,
            currentTextFont = cachedCurrentTextFont,
            currentTextColor = cachedCurrentTextColor
        )
        redoStack.addLast(currentState)

        // Restore state từ undo stack
        val previousState = undoStack.removeLast()
        restoreState(previousState)

        updateUndoRedoState()
        return true
    }

    /**
     * Redo - áp dụng lại state đã undo
     */
    suspend fun performRedo(): Boolean {
        if (redoStack.isEmpty()) return false

        // Lưu state hiện tại vào undo stack
        val currentState = AddCharacterState(
            backgroundImageList = ArrayList(backgroundImageList.map { it.copy() }),
            backgroundColorList = ArrayList(backgroundColorList.map { it.copy() }),
            stickerList = ArrayList(stickerList.map { it.copy() }),
            speechList = ArrayList(speechList.map { it.copy() }),
            textFontList = ArrayList(textFontList.map { it.copy() }),
            textColorList = ArrayList(textColorList.map { it.copy() }),
            drawViewList = ArrayList(drawViewList),
            typeNavigation = _typeNavigation.value,
            typeBackground = _typeBackground.value,
            backgroundImagePath = cachedBackgroundImagePath,
            backgroundColor = cachedBackgroundColor,
            currentText = cachedCurrentText,
            currentTextFont = cachedCurrentTextFont,
            currentTextColor = cachedCurrentTextColor
        )
        undoStack.addLast(currentState)

        // Restore state từ redo stack
        val nextState = redoStack.removeLast()
        restoreState(nextState)

        updateUndoRedoState()
        return true
    }

    /**
     * Khôi phục state vào ViewModel
     */
    private suspend fun restoreState(state: AddCharacterState) {
        backgroundImageList = ArrayList(state.backgroundImageList.map { it.copy() })
        backgroundColorList = ArrayList(state.backgroundColorList.map { it.copy() })
        stickerList = ArrayList(state.stickerList.map { it.copy() })
        speechList = ArrayList(state.speechList.map { it.copy() })
        textFontList = ArrayList(state.textFontList.map { it.copy() })
        textColorList = ArrayList(state.textColorList.map { it.copy() })
        drawViewList = ArrayList(state.drawViewList)

        _typeNavigation.value = state.typeNavigation
        _typeBackground.value = state.typeBackground

        // Restore cache
        cachedBackgroundImagePath = state.backgroundImagePath
        cachedBackgroundColor = state.backgroundColor
        cachedCurrentText = state.currentText
        cachedCurrentTextFont = state.currentTextFont
        cachedCurrentTextColor = state.currentTextColor
    }

    /**
     * Cập nhật trạng thái có thể undo/redo
     */
    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    /**
     * Clear toàn bộ undo/redo history
     */
    fun clearUndoRedoHistory() {
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoState()
    }

    //----------------------------------------------------------------------------------------------------------------------
}