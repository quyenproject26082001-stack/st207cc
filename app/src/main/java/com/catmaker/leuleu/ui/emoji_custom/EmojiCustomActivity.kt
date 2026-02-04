package com.catmaker.leuleu.ui.emoji_custom

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
import android.text.Layout
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.catmaker.leuleu.R
import com.catmaker.leuleu.core.base.BaseActivity
import com.catmaker.leuleu.core.extensions.handleBackLeftToRight
import com.catmaker.leuleu.core.extensions.hideNavigation
import com.catmaker.leuleu.core.extensions.setImageActionBar
import com.catmaker.leuleu.core.extensions.showInterAll
import com.catmaker.leuleu.core.extensions.tap
import com.catmaker.leuleu.core.extensions.visible
import com.catmaker.leuleu.core.extensions.invisible
import com.catmaker.leuleu.core.helper.BitmapHelper
import com.catmaker.leuleu.core.helper.EmojiApiHelper
import com.catmaker.leuleu.core.helper.LanguageHelper
import com.catmaker.leuleu.core.helper.MediaHelper
import com.catmaker.leuleu.core.utils.key.DrawKey
import com.catmaker.leuleu.core.utils.key.EmojiApiConfig
import com.catmaker.leuleu.core.utils.key.EmojiCategory
import com.catmaker.leuleu.core.utils.key.IntentKey
import com.catmaker.leuleu.core.utils.key.ValueKey
import com.catmaker.leuleu.core.utils.state.SaveState
import com.catmaker.leuleu.databinding.ActivityEmojiCustomBinding
import com.catmaker.leuleu.databinding.DialogLayerBinding
import com.catmaker.leuleu.data.model.custom.DrawItemModel
import com.catmaker.leuleu.data.model.custom.EmojiEditModel
import com.catmaker.leuleu.data.model.draw.Draw
import com.catmaker.leuleu.data.model.draw.DrawableDraw
import com.catmaker.leuleu.data.model.draw.TextDraw
import com.catmaker.leuleu.dialog.DialogType
import com.catmaker.leuleu.dialog.YesNoDialog
import com.catmaker.leuleu.listener.listenerdraw.OnDrawListener
import com.catmaker.leuleu.ui.emoji_custom.adapter.LayerAdapter
import com.catmaker.leuleu.ui.success.SuccessActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
    private val categories = EmojiApiHelper.getAllCategories()

    // Lưu trữ selected DrawableDraw cho mỗi category
    private val selectedDraws = mutableMapOf<String, DrawableDraw?>()

    // Edit mode support
    private var statusFrom = ValueKey.CREATE
    private var currentEditModel: EmojiEditModel? = null

    // Map DrawableDraw to its UUID for restore
    private val drawIdMap = mutableMapOf<DrawableDraw, String>()

    override fun setViewBinding(): ActivityEmojiCustomBinding {
        return ActivityEmojiCustomBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Get statusFrom from intent
        statusFrom = intent.getIntExtra(IntentKey.STATUS_FROM_KEY, ValueKey.CREATE)

        initDrawView()
        initRcv()
        loadNavigationData()
        loadLayerData(0)

        // If in EDIT mode, restore edit data
        if (statusFrom == ValueKey.EDIT) {
            restoreEditData()
        }
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
                    val category = categories.find { selectedDraws[it.name] == draw }
                    if (category != null) {
                        selectedDraws[category.name] = null
                        loadLayerData(currentCategoryIndex)
                    }
                    // Clean up drawIdMap
                    if (draw is DrawableDraw) {
                        drawIdMap.remove(draw)
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

        // Restore selectedDraws mapping
        editModel.selectedByCategory.forEach { (categoryName, savedId) ->
            if (savedId == itemModel.id) {
                selectedDraws[categoryName] = drawableDraw
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

        // Build selectedByCategory mapping
        selectedDraws.forEach { (categoryName, draw) ->
            selectedByCategory[categoryName] = draw?.let { drawIdMap[it] }
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
        currentCategoryIndex = categoryIndex
        val category = categories[categoryIndex]

        // Update navigation selection
        val navItems = categories.mapIndexed { index, cat ->
            EmojiNavItem(
                name = cat.name,
                imageUrl = EmojiApiConfig.getImageUrl(cat.name, 1),
                isSelected = index == categoryIndex
            )
        }
        navigationAdapter.submitList(navItems)

        // Load items cho category
        val selectedDraw = selectedDraws[category.name]
        val items = (1..category.count).map { index ->
            val imageUrl = EmojiApiConfig.getImageUrl(category.name, index)
            EmojiLayerItem(
                imageUrl = imageUrl,
                isSelected = selectedDraw?.drawablePath == imageUrl
            )
        }
        layerAdapter.submitList(items)
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
        binding.apply {
            actionBar.btnActionBarLeft.tap { confirmExit() }
            actionBar.btnActionBarRightText.tap { handleSave() }

            // Undo/Redo button listeners
            actionBar.btnActionBarCenterLeft.tap { handleUndo() }
            actionBar.btnActionBarCenterRight.tap { handleRedo() }

            // Flip buttons
            btnFlipH.tap {
                if (layoutCustomLayer.getDraws().isNotEmpty()) {
                    layoutCustomLayer.flipCurrentDraw(DrawKey.FLIP_HORIZONTALLY)
                }
            }

            btnFlipV.tap {
                if (layoutCustomLayer.getDraws().isNotEmpty()) {
                    layoutCustomLayer.flipCurrentDraw(DrawKey.FLIP_VERTICALLY)
                }
            }

            // Draw button
            btnDraw.tap {
                enterDrawMode()
            }

            // Drawing control buttons
            btnPen.tap {
                paintDrawView.eraser(false)
            }

            btnEraser.tap {
                paintDrawView.eraser(true)
            }

            btnDrawDone.tap {
                exitDrawModeAndSave()
            }

            btnDrawCancel.tap {
                exitDrawModeAndCancel()
            }

            // Size slider
            sbSize.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    val strokeWidth = (progress * 2).coerceAtLeast(10)
                    paintDrawView.setStrokeWidth(strokeWidth)
                    paintDrawView.setStrokeWidthEraser(strokeWidth)
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })

            // Color picker
            colorBlack.tap { paintDrawView.setColor(Color.BLACK) }
            colorRed.tap { paintDrawView.setColor(Color.RED) }
            colorBlue.tap { paintDrawView.setColor(Color.BLUE) }
            colorGreen.tap { paintDrawView.setColor(Color.GREEN) }
            colorYellow.tap { paintDrawView.setColor(Color.YELLOW) }

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
            loadLayerData(position)
        }

        layerAdapter.onItemClick = { position ->
            val category = categories[currentCategoryIndex]
            val imageUrl = EmojiApiConfig.getImageUrl(category.name, position + 1)

            // Toggle selection
            val currentDraw = selectedDraws[category.name]
            if (currentDraw?.drawablePath == imageUrl) {
                // Deselect - remove from DrawView
                binding.layoutCustomLayer.remove(currentDraw)
                drawIdMap.remove(currentDraw)
                selectedDraws[category.name] = null
            } else {
                // Select - load image and add to DrawView
                Glide.with(this)
                    .asDrawable()
                    .load(imageUrl)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            // Remove old draw if exists
                            currentDraw?.let {
                                binding.layoutCustomLayer.remove(it)
                                drawIdMap.remove(it)
                            }

                            // Create new DrawableDraw
                            val drawableDraw = DrawableDraw(resource, imageUrl)
                            binding.layoutCustomLayer.addDraw(drawableDraw)

                            // Assign UUID for tracking
                            val id = UUID.randomUUID().toString()
                            drawIdMap[drawableDraw] = id

                            // Save reference
                            selectedDraws[category.name] = drawableDraw
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // Do nothing
                        }
                    })
            }

            // Update UI
            loadLayerData(currentCategoryIndex)
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            btnActionBarRightText.visible()
            btnActionBarRight.invisible()

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
    }

    /**
     * Handle Redo button click
     */
    private fun handleRedo() {
        binding.layoutCustomLayer.redo()
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

    private fun showTextDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // EditText for text input
        val editText = EditText(this).apply {
            hint = "Enter text"
            maxLines = 2
            textSize = 18f
            setTextColor(Color.BLACK)
        }
        container.addView(editText)

        // Color selection
        val colorLabel = android.widget.TextView(this).apply {
            text = "Color:"
            setPadding(0, 16, 0, 8)
        }
        container.addView(colorLabel)

        val colorContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }

        val colors = listOf(
            Color.BLACK, Color.WHITE, Color.RED, Color.BLUE,
            Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA
        )
        var selectedColor = Color.BLACK

        colors.forEach { color ->
            val colorView = View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(60, 60).apply {
                    marginEnd = 8
                }
                setBackgroundColor(color)
                setOnClickListener {
                    selectedColor = color
                    editText.setTextColor(color)
                }
            }
            colorContainer.addView(colorView)
        }
        container.addView(colorContainer)

        // Font selection
        val fontLabel = android.widget.TextView(this).apply {
            text = "Font:"
            setPadding(0, 16, 0, 8)
        }
        container.addView(fontLabel)

        val fontContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }

        val fonts = listOf(
            Pair("Normal", Typeface.DEFAULT),
            Pair("Bold", Typeface.DEFAULT_BOLD),
            Pair("Mono", Typeface.MONOSPACE)
        )
        var selectedTypeface = Typeface.DEFAULT

        fonts.forEach { (name, typeface) ->
            val fontButton = android.widget.Button(this).apply {
                text = name
                setOnClickListener {
                    selectedTypeface = typeface
                    editText.typeface = typeface
                }
            }
            fontContainer.addView(fontButton)
        }
        container.addView(fontContainer)

        // Done button
        val doneButton = android.widget.Button(this).apply {
            text = "Done"
            setOnClickListener {
                val text = editText.text.toString().trim()
                if (text.isNotEmpty()) {
                    // Create transparent bitmap drawable with proper dimensions
                    // ColorDrawable has intrinsicWidth/Height = -1 which causes resizeText() to fail
                    val size = 512
                    val transparentBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val transparentDrawable = BitmapDrawable(resources, transparentBitmap)

                    // Create TextDraw
                    val textDraw = TextDraw(this@EmojiCustomActivity, transparentDrawable, "text_${System.currentTimeMillis()}")
                    textDraw.setText(text)
                    textDraw.setTextColor(selectedColor)
                    textDraw.setTypeface(selectedTypeface)
                    textDraw.setTextAlign(Layout.Alignment.ALIGN_CENTER)
                    textDraw.resizeText()

                    // Add to DrawView
                    binding.layoutCustomLayer.addDraw(textDraw)

                    // Assign UUID for tracking
                    val id = UUID.randomUUID().toString()
                    drawIdMap[textDraw] = id

                    dialog.dismiss()
                } else {
                    showToast("Please enter text")
                }
            }
        }
        container.addView(doneButton)

        dialog.setContentView(container)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
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
                binding.layoutCustomLayer.selectCurrentDraw(draw)
                drawSelect = draw
                isSwipe = true
            }

            rcv.adapter = adapter
            adapter.submitList(drawList)

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
                    val swipeFlags = ItemTouchHelper.LEFT
                    return makeMovementFlags(dragFlags, swipeFlags)
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
            // Show paint view and controls
            paintDrawView.visible()
            layoutDrawControls.visible()

            // Hide other controls
            btnFlipH.invisible()
            btnFlipV.invisible()
            btnText.invisible()
            btnLayer.invisible()

            // Lock the main DrawView to prevent interaction
            layoutCustomLayer.setLocked(true)

            // Initialize paint settings
            paintDrawView.setColor(Color.BLACK)
            paintDrawView.setStrokeWidth(50)
            paintDrawView.eraser(false)
        }
    }

    private fun exitDrawModeAndSave() {
        binding.apply {
            val bitmap = paintDrawView.save()

            if (bitmap != null) {
                // Save bitmap to file so it can be restored later
                val filePath = saveFreehandBitmapToFile(bitmap)

                // Convert bitmap to drawable
                val drawable = BitmapDrawable(resources, bitmap)

                // Create DrawableDraw with actual file path
                val drawableDraw = DrawableDraw(drawable, filePath)
                layoutCustomLayer.addDraw(drawableDraw)

                // Assign UUID for tracking
                val id = UUID.randomUUID().toString()
                drawIdMap[drawableDraw] = id

                // Clear paint view
                paintDrawView.clearAll()
            }

            // Hide drawing UI
            paintDrawView.invisible()
            layoutDrawControls.invisible()

            // Show other controls
            btnFlipH.visible()
            btnFlipV.visible()
            btnText.visible()
            btnLayer.visible()

            // Unlock main DrawView
            layoutCustomLayer.setLocked(false)
        }
    }

    private fun exitDrawModeAndCancel() {
        binding.apply {
            // Clear paint view
            paintDrawView.clearAll()

            // Hide drawing UI
            paintDrawView.invisible()
            layoutDrawControls.invisible()

            // Show other controls
            btnFlipH.visible()
            btnFlipV.visible()
            btnText.visible()
            btnLayer.visible()

            // Unlock main DrawView
            layoutCustomLayer.setLocked(false)
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // If in drawing mode, exit drawing mode first
        if (binding.paintDrawView.visibility == View.VISIBLE) {
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
