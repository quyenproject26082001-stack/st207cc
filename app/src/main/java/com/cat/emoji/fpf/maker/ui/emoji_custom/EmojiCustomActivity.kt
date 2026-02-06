package com.cat.emoji.fpf.maker.ui.emoji_custom

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.Layout
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import com.bumptech.glide.load.engine.DiskCacheStrategy

import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseActivity
import com.cat.emoji.fpf.maker.core.extensions.gone
import com.cat.emoji.fpf.maker.core.extensions.handleBackLeftToRight
import com.cat.emoji.fpf.maker.core.extensions.hideNavigation
import com.cat.emoji.fpf.maker.core.extensions.setFont
import com.cat.emoji.fpf.maker.core.extensions.setImageActionBar
import com.cat.emoji.fpf.maker.core.extensions.showInterAll
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.core.extensions.visible
import com.cat.emoji.fpf.maker.core.extensions.invisible
import com.cat.emoji.fpf.maker.core.helper.BitmapHelper
import com.cat.emoji.fpf.maker.core.helper.EmojiApiHelper
import com.cat.emoji.fpf.maker.core.helper.InternetHelper
import com.cat.emoji.fpf.maker.core.helper.LanguageHelper
import com.cat.emoji.fpf.maker.core.helper.MediaHelper
import com.cat.emoji.fpf.maker.core.utils.DataLocal
import com.cat.emoji.fpf.maker.core.utils.key.DrawKey
import com.cat.emoji.fpf.maker.core.utils.key.EmojiApiConfig
import com.cat.emoji.fpf.maker.core.utils.key.EmojiCategory
import com.cat.emoji.fpf.maker.core.utils.key.IntentKey
import com.cat.emoji.fpf.maker.core.utils.key.ValueKey
import com.cat.emoji.fpf.maker.core.utils.state.SaveState
import com.cat.emoji.fpf.maker.databinding.ActivityEmojiCustomBinding
import com.cat.emoji.fpf.maker.databinding.DialogLayerBinding
import com.cat.emoji.fpf.maker.databinding.EmDialogTextStickerBinding
import com.cat.emoji.fpf.maker.databinding.LayoutDrawBinding
import com.cat.emoji.fpf.maker.data.model.custom.DrawItemModel
import com.cat.emoji.fpf.maker.data.model.custom.EmojiEditModel
import com.cat.emoji.fpf.maker.data.model.draw.Draw
import com.cat.emoji.fpf.maker.data.model.draw.DrawableDraw
import com.cat.emoji.fpf.maker.data.model.draw.TextDraw
import com.cat.emoji.fpf.maker.dialog.DialogType
import com.cat.emoji.fpf.maker.dialog.YesNoDialog
import com.cat.emoji.fpf.maker.listener.listenerdraw.OnDrawListener
import com.cat.emoji.fpf.maker.ui.add_character.adapter.TextFontAdapter
import com.cat.emoji.fpf.maker.ui.emoji_custom.adapter.TextColorEmojiAdapter
import com.cat.emoji.fpf.maker.ui.emoji_custom.adapter.LayerAdapter
import com.cat.emoji.fpf.maker.ui.success.SuccessActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.cat.emoji.fpf.maker.core.extensions.select
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EmojiCustomActivity : BaseActivity<ActivityEmojiCustomBinding>() {

    private val navigationAdapter by lazy { EmojiNavigationAdapter() }
    private val layerAdapter by lazy { EmojiLayerAdapter() }

    private var currentCategoryIndex = 0
    private val preloadTargets = mutableListOf<com.bumptech.glide.request.target.Target<Drawable>>()

    private val categories = EmojiApiHelper.getAllCategories()

    // Lưu trữ selected DrawableDraw cho mỗi category (cho phép nhiều item)
    private val selectedDraws = mutableMapOf<String, MutableList<DrawableDraw>>()

    // Edit mode support
    private var statusFrom = ValueKey.CREATE
    private var currentEditModel: EmojiEditModel? = null

    // Map DrawableDraw to its UUID for restore
    private val drawIdMap = mutableMapOf<DrawableDraw, String>()

    // Draw overlay binding (inflated programmatically)
    private lateinit var drawBinding: LayoutDrawBinding

    // URLs that failed to load — filtered out from all categories
    private val failedUrls = mutableSetOf<String>()

    // Track if color picker has been initialized
    private var isColorPickerInitialized = false

    override fun setViewBinding(): ActivityEmojiCustomBinding {
        return ActivityEmojiCustomBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Check internet before entering screen
        if (!InternetHelper.isInternetAvailable(this)) {
            showNoInternetDialog {
                finish()
            }
            return
        }

        // Get statusFrom from intent
        statusFrom = intent.getIntExtra(IntentKey.STATUS_FROM_KEY, ValueKey.CREATE)

        initDrawView()
        initRcv()
        loadNavigationData()
        loadLayerData(0)

        if (statusFrom == ValueKey.EDIT) {
            restoreEditData()
        } else {
            addDefaultShape()
        }
    }

    private fun showNoInternetDialog(onDismiss: (() -> Unit)? = null) {
        val dialog = YesNoDialog(
            this,
            R.string.no_internet,
            R.string.please_check_your_internet,
            isError = true,
            dialogType = DialogType.INTERNET
        )
        dialog.show()
        dialog.onYesClick = {
            dialog.dismiss()
            onDismiss?.invoke()
        }
    }

    private fun checkInternetAndExecute(action: () -> Unit) {
        if (InternetHelper.isInternetAvailable(this)) {
            action.invoke()
        } else {
            showNoInternetDialog()
        }
    }

    private fun addDefaultShape() {
        val imageUrl = EmojiApiConfig.getImageUrl("Shape", 1)
        Glide.with(this)
            .asDrawable()
            .load(imageUrl)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    val drawableDraw = DrawableDraw(resource, imageUrl)
                    binding.layoutCustomLayer.addDraw(drawableDraw)

                    val id = UUID.randomUUID().toString()
                    drawIdMap[drawableDraw] = id

                    if (selectedDraws["Shape"] == null) {
                        selectedDraws["Shape"] = mutableListOf()
                    }
                    selectedDraws["Shape"]?.add(drawableDraw)

                    loadLayerData(currentCategoryIndex)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun initDrawView() {
        binding.layoutCustomLayer.apply {
            setConstrained(true)
            setLocked(false)
            setOnDrawListener(object : OnDrawListener {
                override fun onAddedDraw(draw: Draw) {}
                override fun onClickedDraw(draw: Draw) {}
                override fun onDeletedDraw(draw: Draw) {
                    // Update selectedDraws when user deletes via icon
                    if (draw is DrawableDraw) {
                        categories.forEach { category ->
                            selectedDraws[category.name]?.remove(draw)
                        }
                        drawIdMap.remove(draw)
                        loadLayerData(currentCategoryIndex)
                    }
                }
                override fun onDragFinishedDraw(draw: Draw) {}
                override fun onTouchedDownDraw(draw: Draw) {}
                override fun onZoomFinishedDraw(draw: Draw) {}
                override fun onFlippedDraw(draw: Draw) {}
                override fun onDoubleTappedDraw(draw: Draw) {}
                override fun onHideOptionIconDraw() {}
                override fun onUndoDeleteDraw(draw: List<Draw?>) {}
                override fun onUndoUpdateDraw(draw: List<Draw?>) {}
                override fun onUndoDeleteAll() {}
                override fun onRedoAll() {}
                override fun onReplaceDraw(draw: Draw) {}
                override fun onEditText(draw: DrawableDraw) {}
                override fun onReplace(draw: Draw) {}
            })

            // Setup emoji-specific icons: only delete (top right) and zoom (bottom left)
            setupEmojiIcons(45f)
        }
    }

    private fun initRcv() {
        binding.rcvNavigation.apply {
            adapter = navigationAdapter
            itemAnimator = null
        }

        binding.rcvLayer.apply {

            adapter = layerAdapter
            itemAnimator = null
        }
    }

    /**
     * Restore edit data from EMOJI_SUGGESTION_FILE_INTERNAL
     */
    private fun restoreEditData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val editModel = MediaHelper.readModelFromFile<EmojiEditModel>(
                    this@EmojiCustomActivity,
                    ValueKey.EMOJI_SUGGESTION_FILE_INTERNAL
                )
                if (editModel != null) {
                    currentEditModel = editModel
                    withContext(Dispatchers.Main) {
                        restoreDrawItems(editModel)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EmojiCustomActivity", "Error restoring edit data: ${e.message}")
            }
        }
    }

    /**
     * Restore DrawableDraw and TextDraw items from EmojiEditModel
     */
    private fun restoreDrawItems(editModel: EmojiEditModel) {
        var pendingLoads = editModel.drawItems.size
        if (pendingLoads == 0) return

        editModel.drawItems.forEach { itemModel ->
            when (itemModel.type) {
                "text" -> {
                    restoreTextDraw(itemModel, editModel)
                    pendingLoads--
                    if (pendingLoads == 0) {
                        // All items loaded, refresh UI
                        loadLayerData(currentCategoryIndex)
                    }
                }
                else -> {
                    // drawable type
                    restoreDrawableDraw(itemModel, editModel) {
                        pendingLoads--
                        if (pendingLoads == 0) {
                            // All items loaded, refresh UI
                            loadLayerData(currentCategoryIndex)
                        }
                    }
                }
            }
        }
    }

    /**
     * Restore a TextDraw from DrawItemModel
     */
    private fun restoreTextDraw(itemModel: DrawItemModel, editModel: EmojiEditModel) {
        // Create a transparent bitmap drawable with proper dimensions
        // ColorDrawable has intrinsicWidth/Height = -1 which causes resizeText() to fail
        val size = 512 // Standard size
        val transparentBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val transparentDrawable = BitmapDrawable(resources, transparentBitmap)
        val textDraw = TextDraw(this, transparentDrawable, itemModel.drawablePath)

        // Restore text properties
        itemModel.text?.let { textDraw.setText(it) }
        itemModel.textColor?.let { textDraw.setTextColor(it) }

        // Restore typeface
        val typeface = when (itemModel.idTypeFace) {
            1 -> Typeface.DEFAULT_BOLD
            2 -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT
        }
        textDraw.setTypeface(typeface)
        textDraw.idTypeFace = itemModel.idTypeFace

        // Restore text alignment
        val alignment = try {
            Layout.Alignment.valueOf(itemModel.textAlignString)
        } catch (e: Exception) {
            Layout.Alignment.ALIGN_CENTER
        }
        textDraw.setTextAlign(alignment)
        textDraw.textCheckAlign = itemModel.textCheckAlign
        textDraw.resizeText()

        // Restore matrix
        if (itemModel.matrixValues.size == 9) {
            val matrix = Matrix()
            matrix.setValues(itemModel.matrixValues.toFloatArray())
            textDraw.setMatrix(matrix)
        }

        // Restore other properties
        textDraw.setFlippedH(itemModel.isFlippedH)
        textDraw.setFlippedV(itemModel.isFlippedV)
        textDraw.setLock(itemModel.isLock)
        textDraw.setHide(itemModel.isHide)
        textDraw.setAlpha(itemModel.alpha)

        // Add to DrawView using addDrawRestored() to preserve matrix
        binding.layoutCustomLayer.addDrawRestored(textDraw)
        drawIdMap[textDraw] = itemModel.id
    }

    /**
     * Restore a DrawableDraw from DrawItemModel
     */
    private fun restoreDrawableDraw(itemModel: DrawItemModel, editModel: EmojiEditModel, onComplete: () -> Unit) {
        val drawablePath = itemModel.drawablePath

        // Check if it's a local file (freehand drawing) or URL (sticker)
        val isLocalFile = !drawablePath.startsWith("http")

        if (isLocalFile) {
            // Load from local file
            val file = File(drawablePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(drawablePath)
                if (bitmap != null) {
                    val drawable = BitmapDrawable(resources, bitmap)
                    addRestoredDrawableDraw(drawable, itemModel, editModel)
                }
            }
            onComplete()
        } else {
            // Load from URL using Glide
            Glide.with(this)
                .asDrawable()
                .load(drawablePath)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        addRestoredDrawableDraw(resource, itemModel, editModel)
                        onComplete()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        onComplete()
                    }
                })
        }
    }

    /**
     * Add a restored DrawableDraw to the canvas
     */
    private fun addRestoredDrawableDraw(drawable: Drawable, itemModel: DrawItemModel, editModel: EmojiEditModel) {
        val drawableDraw = DrawableDraw(drawable, itemModel.drawablePath)

        // Restore matrix
        if (itemModel.matrixValues.size == 9) {
            val matrix = Matrix()
            matrix.setValues(itemModel.matrixValues.toFloatArray())
            drawableDraw.setMatrix(matrix)
        }

        // Restore other properties
        drawableDraw.setFlippedH(itemModel.isFlippedH)
        drawableDraw.setFlippedV(itemModel.isFlippedV)
        drawableDraw.setLock(itemModel.isLock)
        drawableDraw.setHide(itemModel.isHide)
        drawableDraw.setPagerSelected(itemModel.pagerSelected)
        drawableDraw.setPositionSelected(itemModel.positionSelected)
        drawableDraw.isCharacter = itemModel.isCharacter
        drawableDraw.setAlpha(itemModel.alpha)

        // Add to DrawView using addDrawRestored() to preserve matrix
        binding.layoutCustomLayer.addDrawRestored(drawableDraw)
        drawIdMap[drawableDraw] = itemModel.id

        // Restore selectedDraws mapping (savedIds là string chứa nhiều ID phân cách bởi dấu phẩy)
        editModel.selectedByCategory.forEach { (categoryName, savedIds) ->
            val idList = savedIds?.split(",") ?: emptyList()
            if (idList.contains(itemModel.id)) {
                if (selectedDraws[categoryName] == null) {
                    selectedDraws[categoryName] = mutableListOf()
                }
                selectedDraws[categoryName]?.add(drawableDraw)
            }
        }
    }

    /**
     * Create EmojiEditModel from current state
     */
    private fun createEditModel(pathInternal: String): EmojiEditModel {
        val drawItems = ArrayList<DrawItemModel>()
        val selectedByCategory = HashMap<String, String?>()

        // Process all draws
        binding.layoutCustomLayer.getDraws().forEach { draw ->
            val id = drawIdMap[draw] ?: UUID.randomUUID().toString()
            drawIdMap[draw] = id

            val matrixValues = FloatArray(9)
            draw.getMatrix().getValues(matrixValues)

            val itemModel = DrawItemModel(
                id = id,
                type = if (draw is TextDraw) "text" else "drawable",
                drawablePath = draw.drawablePath,
                matrixValues = matrixValues.toCollection(ArrayList()),
                isFlippedH = draw.isFlippedH,
                isFlippedV = draw.isFlippedV,
                isLock = draw.isLock,
                isHide = draw.isHide,
                pagerSelected = draw.pagerSelected,
                positionSelected = draw.positionSelected,
                isCharacter = draw.isCharacter,
                alpha = draw.drawable.alpha
            )

            // If it's a TextDraw, add text-specific fields
            if (draw is TextDraw) {
                itemModel.isText = true
                itemModel.text = draw.text
                itemModel.textColor = draw.textColor
                itemModel.textAlignString = draw.textAlign.name
                itemModel.idTypeFace = draw.idTypeFace
                itemModel.textCheckAlign = draw.textCheckAlign
            }

            drawItems.add(itemModel)
        }

        // Build selectedByCategory mapping (lưu list IDs)
        selectedDraws.forEach { (categoryName, drawList) ->
            val ids = drawList.mapNotNull { drawIdMap[it] }.joinToString(",")
            selectedByCategory[categoryName] = ids.ifEmpty { null }
        }

        return EmojiEditModel(
            pathInternalEdit = pathInternal,
            drawItems = drawItems,
            selectedByCategory = selectedByCategory
        )
    }

    /**
     * Save freehand bitmap to file and return the file path
     */
    private fun saveFreehandBitmapToFile(bitmap: Bitmap): String {
        val drawsFolder = File(filesDir, ValueKey.EMOJI_DRAWS_FOLDER)
        if (!drawsFolder.exists()) {
            drawsFolder.mkdirs()
        }

        val fileName = "freehand_${System.currentTimeMillis()}.png"
        val file = File(drawsFolder, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return file.absolutePath
    }

    /**
     * Add emoji to edit list (for CREATE mode)
     */
    private fun addEmojiToEditList(pathInternal: String) {
        val editList = loadEmojiEditList()
        val newEditModel = createEditModel(pathInternal)
        editList.add(0, newEditModel)
        MediaHelper.writeListToFile(this, ValueKey.EMOJI_EDIT_FILE_INTERNAL, editList)
    }

    /**
     * Update existing emoji in edit list (for EDIT mode)
     */
    private fun updateEmojiEditList(pathInternal: String) {
        val editList = loadEmojiEditList()
        val oldPath = currentEditModel?.pathInternalEdit ?: return

        val indexEdit = editList.indexOfFirst { it.pathInternalEdit == oldPath }
        if (indexEdit != -1) {
            editList[indexEdit] = createEditModel(pathInternal)
            MediaHelper.writeListToFile(this, ValueKey.EMOJI_EDIT_FILE_INTERNAL, editList)
        }
    }

    /**
     * Load emoji edit list from file
     */
    private fun loadEmojiEditList(): ArrayList<EmojiEditModel> {
        return try {
            MediaHelper.readListFromFile<EmojiEditModel>(this, ValueKey.EMOJI_EDIT_FILE_INTERNAL)
                .toCollection(ArrayList())
        } catch (e: Exception) {
            android.util.Log.e("EmojiCustomActivity", "Error loading emoji edit list: ${e.message}")
            arrayListOf()
        }
    }

    private fun loadNavigationData() {
        val navItems = categories.mapIndexed { index, category ->
            EmojiNavItem(
                name = category.name,
                imageUrl = EmojiApiConfig.getImageUrl(category.name, 1),
                isSelected = index == 0
            )
        }
        navigationAdapter.submitList(navItems)
    }

    private fun loadLayerData(categoryIndex: Int) {
        preloadTargets.forEach { Glide.with(this).clear(it) }
        preloadTargets.clear()


        val startTime = System.currentTimeMillis()
        currentCategoryIndex = categoryIndex
        val category = categories[categoryIndex]
        Log.d("EmojiLayerLoad", "--- loadLayerData START category=${category.name} index=$categoryIndex ---")

        // Update navigation selection
        val navItems = categories.mapIndexed { index, cat ->
            EmojiNavItem(
                name = cat.name,
                imageUrl = EmojiApiConfig.getImageUrl(cat.name, 1),
                isSelected = index == categoryIndex
            )
        }
        navigationAdapter.submitList(navItems)
        Log.d("EmojiLayerLoad", "navSubmit done time=${System.currentTimeMillis() - startTime}ms")

        // Load items cho category (filter out excluded items)
        val buildStart = System.currentTimeMillis()
        val currentDrawPaths = binding.layoutCustomLayer.getDraws().mapNotNull { it.drawablePath }.toSet()
        val excluded = EmojiApiHelper.EXCLUDED_ITEMS[category.name] ?: emptySet()
        val items = (1..category.count)
            .filter { it !in excluded }
            .map { index ->
                val imageUrl = EmojiApiConfig.getImageUrl(category.name, index)
                EmojiLayerItem(
                    imageUrl = imageUrl,
                    isSelected = imageUrl in currentDrawPaths
                )
            }
            .filter { it.imageUrl !in failedUrls }
        Log.d("EmojiLayerLoad", "items built: count=${items.size} excluded=${excluded.size} failed=${failedUrls.size} buildTime=${System.currentTimeMillis() - buildStart}ms")

        layerAdapter.submitList(items)
        Log.d("EmojiLayerLoad", "--- submitList done total=${System.currentTimeMillis() - startTime}ms ---")

        val preloadList = items.take(20)
        preloadList.forEach { item ->
            val t = Glide.with(this)
                .load(item.imageUrl)
                .override(160, 160)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .preload()
            preloadTargets.add(t)
        }


    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Undo state from DrawView
                launch {
                    binding.layoutCustomLayer.canUndo.collect { canUndo ->
                        binding.actionBar.btnActionBarCenterLeft.apply {
                            isEnabled = canUndo
                            alpha = if (canUndo) 1.0f else 0.3f
                        }
                    }
                }

                // Observe Redo state from DrawView
                launch {
                    binding.layoutCustomLayer.canRedo.collect { canRedo ->
                        binding.actionBar.btnActionBarCenterRight.apply {
                            isEnabled = canRedo
                            alpha = if (canRedo) 1.0f else 0.3f
                        }
                    }
                }
            }
        }
    }

    override fun viewListener() {
        drawBinding = LayoutDrawBinding.inflate(LayoutInflater.from(this), binding.root, true)

        binding.apply {
            actionBar.btnActionBarLeft.tap { confirmExit() }
            actionBar.btnActionBarRightText.tap { handleSave() }
            actionBar.btnActionBarCenter.tap { confirmReset() }

            // Undo/Redo button listeners
            actionBar.btnActionBarCenterLeft.tap { handleUndo() }
            actionBar.btnActionBarCenterRight.tap { handleRedo() }

            // Flip buttons
            btnFlipH.tap {
                if (layoutCustomLayer.getCurrentDraw() != null) {
                    layoutCustomLayer.flipCurrentDraw(DrawKey.FLIP_HORIZONTALLY)
                } else {
                    showToast(R.string.please_select_item)
                }
            }

            btnFlipV.tap {
                if (layoutCustomLayer.getCurrentDraw() != null) {
                    layoutCustomLayer.flipCurrentDraw(DrawKey.FLIP_VERTICALLY)
                } else {
                    showToast(R.string.please_select_item)
                }
            }

            // Draw button
            btnDraw.tap {
                enterDrawMode()
            }

            // Draw mode toolbar buttons
            drawBinding.btnBackDraw.tap {
                exitDrawModeAndCancel()
            }

            drawBinding.btnDoneDraw.tap {
                exitDrawModeAndSave()
            }

            // Paint / Eraser toggle
            drawBinding.btnPaintDraw.tap {
                drawBinding.dv.eraser(false)
                drawBinding.btnPaintDraw.setBackgroundResource(R.drawable.bg_selected)
                drawBinding.btnEraserDraw.setBackgroundResource(0)
                if (drawBinding.layoutColorPickerDraw.visibility == View.VISIBLE) {
                    drawBinding.layoutColorPickerDraw.gone()
                } else {
                    drawBinding.layoutColorPickerDraw.visible()
                    drawBinding.layoutColorPickerDraw.post {
                        // Select center to initialize sliders with default color (first time only)
                        if (!isColorPickerInitialized) {
                            drawBinding.colorPicker.selectCenter()
                            isColorPickerInitialized = true
                        }
                        drawBinding.sbAlphaSlideBar.invalidate()
                        drawBinding.sbBrightnessSlide.invalidate()
                    }
                }
            }

            drawBinding.btnEraserDraw.tap {
                drawBinding.dv.eraser(true)
                drawBinding.btnEraserDraw.setBackgroundResource(R.drawable.bg_selected)
                drawBinding.btnPaintDraw.setBackgroundResource(0)
                drawBinding.layoutColorPickerDraw.gone()
            }

            // Size slider
            drawBinding.sbSizeDraw.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    val strokeWidth = (progress * 2).coerceAtLeast(10)
                    drawBinding.dv.setStrokeWidth(strokeWidth)
                    drawBinding.dv.setStrokeWidthEraser(strokeWidth)
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })

            // Color picker
            drawBinding.colorPicker.attachBrightnessSlider(drawBinding.sbBrightnessSlide)
            drawBinding.colorPicker.attachAlphaSlider(drawBinding.sbAlphaSlideBar)
            drawBinding.colorPicker.setColorListener(object : ColorEnvelopeListener {
                override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                    drawBinding.dv.setColor(envelope.color)
                }
            })

            // Text button
            btnText.tap {
                showTextDialog()
            }

            // Layer button
            btnLayer.tap {
                showLayerBottomSheet()
            }
        }

        navigationAdapter.onItemClick = { position ->
            checkInternetAndExecute {
                loadLayerData(position)
            }
        }

        layerAdapter.onItemLoadError = { item ->
            if (failedUrls.add(item.imageUrl)) {
                val currentList = layerAdapter.currentList.filter { it.imageUrl !in failedUrls }
                layerAdapter.submitList(currentList)
            }
        }

        layerAdapter.onItemClick = { item ->
            checkInternetAndExecute {
                val category = categories[currentCategoryIndex]
                val imageUrl = item.imageUrl

                android.util.Log.d("EmojiCustom", "========================================")
                android.util.Log.d("EmojiCustom", "categoryIndex: $currentCategoryIndex")
                android.util.Log.d("EmojiCustom", "categoryName: ${category.name}")
                android.util.Log.d("EmojiCustom", "imageUrl: $imageUrl")
                android.util.Log.d("EmojiCustom", "isSelected: ${item.isSelected}")
                android.util.Log.d("EmojiCustom", "========================================")

                // Add new item (cho phép chọn nhiều lần)
                Glide.with(this)
                    .asDrawable()
                    .load(imageUrl)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            // Create new DrawableDraw
                            val drawableDraw = DrawableDraw(resource, imageUrl)
                            binding.layoutCustomLayer.addDraw(drawableDraw)

                            // Assign UUID for tracking
                            val id = UUID.randomUUID().toString()
                            drawIdMap[drawableDraw] = id

                            // Save reference to list
                            if (selectedDraws[category.name] == null) {
                                selectedDraws[category.name] = mutableListOf()
                            }
                            selectedDraws[category.name]?.add(drawableDraw)

                            // Update UI
                            loadLayerData(currentCategoryIndex)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // Do nothing
                        }
                    })
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setImageActionBar(btnActionBarCenter, R.drawable.ic_reset)
            btnActionBarRightText.visible()
            btnActionBarRight.invisible()
            btnActionBarCenter.visible()

            // Show Undo/Redo buttons
            btnActionBarCenterLeft.visible()
            btnActionBarCenterRight.visible()
            bgBtnActionBar.setBackgroundResource(R.drawable.bg_actionbar)

            // Set initial state (disabled until first action)
            btnActionBarCenterLeft.alpha = 0.3f
            btnActionBarCenterRight.alpha = 0.3f
            btnActionBarCenterLeft.isEnabled = false
            btnActionBarCenterRight.isEnabled = false
        }
    }

    /**
     * Handle Undo button click
     */
    private fun handleUndo() {
        binding.layoutCustomLayer.undo()
        loadLayerData(currentCategoryIndex)
    }

    /**
     * Handle Redo button click
     */
    private fun handleRedo() {
        binding.layoutCustomLayer.redo()
        loadLayerData(currentCategoryIndex)
    }

    /**
     * Confirm reset all draws
     */
    private fun confirmReset() {
        if (binding.layoutCustomLayer.getDraws().isEmpty()) {
            showToast(R.string.please_select_item)
            return
        }

        val dialog = YesNoDialog(
            this,
            R.string.reset,
            R.string.change_your_whole_design_are_you_sure,
            dialogType = DialogType.RESET
        )
        dialog.show()

        dialog.onNoClick = {
            dialog.dismiss()
            hideNavigation(true)
        }

        dialog.onYesClick = {
            dialog.dismiss()
            hideNavigation(true)
            handleReset()
        }
    }

    /**
     * Reset all draws and clear selected items
     */
    private fun handleReset() {
        // Remove all draws from canvas
        binding.layoutCustomLayer.removeAllDraw()

        // Clear selected draws map
        selectedDraws.clear()

        // Clear draw ID map
        drawIdMap.clear()

        // Reload current category to refresh UI
        loadLayerData(currentCategoryIndex)
    }

    private fun confirmExit() {
        val dialog = YesNoDialog(
            this, R.string.exit, R.string.do_you_want_to_exit,
            isError = false, dialogType = DialogType.DELETE_EXIT
        )
        LanguageHelper.setLocale(this)
        dialog.show()
        dialog.onYesClick = {
            dialog.dismiss()
            showInterAll { finish() }
        }
        dialog.onNoClick = {
            dialog.dismiss()
        }
    }

    private fun handleSave() {
        // Check if there are any items
        if (binding.layoutCustomLayer.getDraws().isEmpty()) {
            showToast(R.string.please_select_item)
            return
        }

        // Hide selection before save
        binding.layoutCustomLayer.hideSelect()

        lifecycleScope.launch(Dispatchers.IO) {
            showLoading()
            delay(200)

            val bitmap = BitmapHelper.createBimapFromView(binding.layoutCustomLayer)
            MediaHelper.saveBitmapToInternalStorage(
                this@EmojiCustomActivity,
                ValueKey.DOWNLOAD_ALBUM,
                bitmap
            ).collect { result ->
                when (result) {
                    is SaveState.Loading -> showLoading()

                    is SaveState.Error -> {
                        dismissLoading(true)
                        withContext(Dispatchers.Main) {
                            showToast(R.string.save_failed_please_try_again)
                        }
                    }

                    is SaveState.Success -> {
                        // Save edit data based on mode
                        when (statusFrom) {
                            ValueKey.EDIT -> updateEmojiEditList(result.path)
                            else -> addEmojiToEditList(result.path)
                        }

                        val intent = Intent(this@EmojiCustomActivity, SuccessActivity::class.java)
                        intent.putExtra(IntentKey.INTENT_KEY, result.path)
                        intent.putExtra(IntentKey.TAB_INDEX_KEY, ValueKey.EMOJI_TYPE)
                        val options = ActivityOptions.makeCustomAnimation(
                            this@EmojiCustomActivity,
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                        dismissLoading(true)
                        withContext(Dispatchers.Main) {
                            showInterAll {
                                startActivity(intent, options.toBundle())
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun showTextDialog() {
        val textFontList = DataLocal.getTextFontDefault().also { it[0].isSelected = true }
        val textColorList = DataLocal.getTextColorDefault(this).also { it[1].isSelected = true }
        var selectedFontRes = textFontList[0].color
        var selectedColor = textColorList[1].color

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val bindingDialog = EmDialogTextStickerBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

        val window = dialog.window ?: return
        window.setGravity(Gravity.CENTER)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        bindingDialog.apply {
            edtText.isFocusableInTouchMode = true
            edtText.isFocusable = true
            edtText.requestFocus()
            edtText.setFont(selectedFontRes)
            edtText.setTextColor(selectedColor)

            edtText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tvLength.text = "${s.toString().length}/30"
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            val textFontAdapter = TextFontAdapter(this@EmojiCustomActivity, isEmojiCustom = true)
            textFontAdapter.onTextFontClick = { font, position ->
                edtText.setFont(font)
                selectedFontRes = font
                textFontList.forEachIndexed { index, item -> item.isSelected = index == position }
                textFontAdapter.submitItem(position, textFontList)
            }

            val textColorAdapter = TextColorEmojiAdapter()
            textColorAdapter.onTextColorClick = { color, position ->
                edtText.setTextColor(color)
                selectedColor = color
                textColorList.forEachIndexed { index, item -> item.isSelected = index == position }
                textColorAdapter.submitItem(position, textColorList)
            }

            rcvTextFont.adapter = textFontAdapter
            rcvTextFont.itemAnimator = null
            textFontAdapter.submitListReset(textFontList)

            rcvTextColor.adapter = textColorAdapter
            rcvTextFont.visible()
            rcvTextColor.itemAnimator = null
            textColorAdapter.submitListReset(textColorList)
            rcvTextColor.gone()

            btnTextFont.tap {
                Glide.with(this@EmojiCustomActivity).load(R.drawable.ic_text_font_selected).into(btnTextFont)
                Glide.with(this@EmojiCustomActivity).load(R.drawable.ic_color_picker_unselected).into(btnColorPicker)
                rcvTextFont.visible()
                rcvTextColor.gone()
            }

            btnColorPicker.tap {
                Glide.with(this@EmojiCustomActivity).load(R.drawable.ic_text_font_unselected).into(btnTextFont)
                Glide.with(this@EmojiCustomActivity).load(R.drawable.ic_color_picker_selected).into(btnColorPicker)
                rcvTextFont.gone()
                rcvTextColor.visible()
            }

            btnCancel.tap {
                dialog.dismiss()
            }

            btnDone.tap {
                val text = edtText.text.toString().trim()
                if (text.isEmpty()) {
                    showToast(R.string.please_enter_text)
                    return@tap
                }

                val size = 512
                val transparentBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val transparentDrawable = BitmapDrawable(resources, transparentBitmap)

                val textDraw = TextDraw(this@EmojiCustomActivity, transparentDrawable, "text_${System.currentTimeMillis()}")
                textDraw.setText(text)
                textDraw.setTextColor(selectedColor)
                textDraw.setTypeface(ResourcesCompat.getFont(this@EmojiCustomActivity, selectedFontRes) ?: Typeface.DEFAULT)
                textDraw.setTextAlign(Layout.Alignment.ALIGN_CENTER)
                textDraw.resizeText()

                binding.layoutCustomLayer.addDraw(textDraw)

                val id = UUID.randomUUID().toString()
                drawIdMap[textDraw] = id

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showLayerBottomSheet() {
        val drawList = binding.layoutCustomLayer.getDraws()

        if (drawList.isEmpty()) {
            showToast(R.string.please_select_item)
            return
        }

        val bottomSheetBinding = DialogLayerBinding.inflate(layoutInflater)
        val bottomSheet = BottomSheetDialog(this)
        bottomSheet.setContentView(bottomSheetBinding.root)

        var drawSelect: DrawableDraw? = null
        var isSwipe = false
        val paint = Paint()

        bottomSheetBinding.apply {
            val adapter = LayerAdapter(binding.layoutCustomLayer) { draw, position ->
                // Only select if draw is not hidden
                if (!draw.isHide) {
                    binding.layoutCustomLayer.selectCurrentDraw(draw)
                    drawSelect = draw
                    isSwipe = true
                }
            }

            rcv.adapter = adapter
            adapter.submitList(drawList)

            // Restore selected position from current draw
            val currentDraw = binding.layoutCustomLayer.getCurrentDraw()
            if (currentDraw != null) {
                val currentIndex = drawList.indexOf(currentDraw)
                if (currentIndex != -1) {
                    adapter.setSelectedPosition(currentIndex)
                }
            }

            if (drawList.isNotEmpty()) {
                tvLayerEmpty.visibility = View.GONE
            } else {
                tvLayerEmpty.visibility = View.VISIBLE
            }

            // ItemTouchHelper for drag to reorder and swipe to delete
            ItemTouchHelper(object : ItemTouchHelper.Callback() {
                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    return makeMovementFlags(dragFlags, 0)
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val fromPosition = viewHolder.adapterPosition
                    val toPosition = target.adapterPosition
                    adapter.onItemMove(fromPosition, toPosition)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    if (direction == ItemTouchHelper.LEFT && isSwipe && drawSelect != null) {
                        binding.layoutCustomLayer.remove(drawSelect)

                        val position = viewHolder.adapterPosition
                        val updatedList = drawList.toMutableList()
                        updatedList.removeAt(position)

                        adapter.resetItemSelected()
                        adapter.submitList(updatedList)

                        if (updatedList.isEmpty()) {
                            tvLayerEmpty.visibility = View.VISIBLE
                            bottomSheet.dismiss()
                        }

                        isSwipe = false
                        drawSelect = null
                    }
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    if (isSwipe && actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        val itemView: View = viewHolder.itemView
                        val height = itemView.bottom.toFloat() - itemView.top.toFloat()
                        val width = height / 3

                        paint.color = ContextCompat.getColor(this@EmojiCustomActivity, android.R.color.holo_red_dark)
                        val background = RectF(
                            itemView.right.toFloat() + dX,
                            itemView.top.toFloat(),
                            itemView.right.toFloat(),
                            itemView.bottom.toFloat()
                        )
                        c.drawRect(background, paint)

                        val icon = BitmapFactory.decodeResource(resources, R.drawable.ic_delete)
                        val iconDest = RectF(
                            itemView.right.toFloat() - 2 * width,
                            itemView.top.toFloat() + width,
                            itemView.right.toFloat() - width,
                            itemView.bottom.toFloat() - width
                        )
                        c.drawBitmap(icon, null, iconDest, paint)
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }).attachToRecyclerView(rcv)
        }

        bottomSheet.show()
    }

    private fun enterDrawMode() {
        binding.apply {
            // Hide other controls
            btnFlipH.invisible()
            btnFlipV.invisible()
            btnText.invisible()
            btnLayer.invisible()
            // Lock the main DrawView to prevent interaction
            layoutCustomLayer.setLocked(true)
            // Hide action bar to prevent accidental save
            actionBar.root.gone()
        }

        // Reset color picker initialization flag
        isColorPickerInitialized = false

        // Show draw overlay and initialize
        drawBinding.apply {
            layoutDraw.visible()
            tvSize.select()

            // Initialize paint settings
            dv.setColor(Color.BLACK)
            dv.setStrokeWidth(50)
            dv.eraser(false)

            // Reset UI to default paint mode with size visible
            btnPaintDraw.setBackgroundResource(R.drawable.bg_selected)
            btnEraserDraw.setBackgroundResource(0)
            layoutColorPickerDraw.gone()
            layoutSize.visible()
        }
    }

    private fun exitDrawModeAndSave() {
        val bitmap = drawBinding.dv.save()

        // null = chưa vẽ gì (pathList rỗng)
        if (bitmap == null) {
            showToast(R.string.please_draw_something)
            return
        }

        // Vẽ rồi xóa hết bằng tẩy -> pathList có paths nhưng bitmap toàn transparent
        // Check alpha channel instead of full pixel value
        var hasVisiblePixel = false
        val sampleSize = 20 // Sample every 20 pixels for performance
        outerLoop@ for (y in 0 until bitmap.height step sampleSize) {
            for (x in 0 until bitmap.width step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel shr 24) and 0xff
                if (alpha > 0) {
                    hasVisiblePixel = true
                    break@outerLoop
                }
            }
        }

        if (!hasVisiblePixel) {
            showToast(R.string.please_draw_something)
            return
        }

        val filePath = saveFreehandBitmapToFile(bitmap)
        val drawable = BitmapDrawable(resources, bitmap)
        val drawableDraw = DrawableDraw(drawable, filePath)
        binding.layoutCustomLayer.addDraw(drawableDraw)

        val id = UUID.randomUUID().toString()
        drawIdMap[drawableDraw] = id

        drawBinding.dv.clearAll()

        // Hide draw overlay
        drawBinding.layoutDraw.gone()

        // Show other controls
        binding.apply {
            btnFlipH.visible()
            btnFlipV.visible()
            btnText.visible()
            btnLayer.visible()
            layoutCustomLayer.setLocked(false)
            actionBar.root.visible()
        }
    }

    private fun exitDrawModeAndCancel() {
        drawBinding.dv.clearAll()

        // Hide draw overlay
        drawBinding.layoutDraw.gone()

        // Show other controls
        binding.apply {
            btnFlipH.visible()
            btnFlipV.visible()
            btnText.visible()
            btnLayer.visible()
            layoutCustomLayer.setLocked(false)
            actionBar.root.visible()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // If in drawing mode, exit drawing mode first
        if (drawBinding.layoutDraw.visibility == View.VISIBLE) {
            exitDrawModeAndCancel()
        } else {
            confirmExit()
        }
    }
}

// Data classes
data class EmojiNavItem(
    val name: String,
    val imageUrl: String,
    val isSelected: Boolean = false
)

data class EmojiLayerItem(
    val imageUrl: String,
    val isSelected: Boolean = false
)
