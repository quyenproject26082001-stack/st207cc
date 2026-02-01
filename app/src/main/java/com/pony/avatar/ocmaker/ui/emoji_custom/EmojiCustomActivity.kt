package com.pony.avatar.ocmaker.ui.emoji_custom

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
import com.pony.avatar.ocmaker.R
import com.pony.avatar.ocmaker.core.base.BaseActivity
import com.pony.avatar.ocmaker.core.extensions.handleBackLeftToRight
import com.pony.avatar.ocmaker.core.extensions.setImageActionBar
import com.pony.avatar.ocmaker.core.extensions.showInterAll
import com.pony.avatar.ocmaker.core.extensions.tap
import com.pony.avatar.ocmaker.core.extensions.visible
import com.pony.avatar.ocmaker.core.extensions.invisible
import com.pony.avatar.ocmaker.core.helper.EmojiApiHelper
import com.pony.avatar.ocmaker.core.helper.LanguageHelper
import com.pony.avatar.ocmaker.core.utils.key.DrawKey
import com.pony.avatar.ocmaker.core.utils.key.EmojiApiConfig
import com.pony.avatar.ocmaker.core.utils.key.EmojiCategory
import com.pony.avatar.ocmaker.databinding.ActivityEmojiCustomBinding
import com.pony.avatar.ocmaker.databinding.DialogLayerBinding
import com.pony.avatar.ocmaker.data.model.draw.Draw
import com.pony.avatar.ocmaker.data.model.draw.DrawableDraw
import com.pony.avatar.ocmaker.data.model.draw.TextDraw
import com.pony.avatar.ocmaker.dialog.DialogType
import com.pony.avatar.ocmaker.dialog.YesNoDialog
import com.pony.avatar.ocmaker.listener.listenerdraw.OnDrawListener
import com.pony.avatar.ocmaker.ui.emoji_custom.adapter.LayerAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmojiCustomActivity : BaseActivity<ActivityEmojiCustomBinding>() {

    private val navigationAdapter by lazy { EmojiNavigationAdapter() }
    private val layerAdapter by lazy { EmojiLayerAdapter() }

    private var currentCategoryIndex = 0
    private val categories = EmojiApiHelper.getAllCategories()

    // Lưu trữ selected DrawableDraw cho mỗi category
    private val selectedDraws = mutableMapOf<String, DrawableDraw?>()

    override fun setViewBinding(): ActivityEmojiCustomBinding {
        return ActivityEmojiCustomBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        initDrawView()
        initRcv()
        loadNavigationData()
        loadLayerData(0)
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
                selectedDraws[category.name] = null
            } else {
                // Select - load image and add to DrawView
                Glide.with(this)
                    .asDrawable()
                    .load(imageUrl)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            // Remove old draw if exists
                            currentDraw?.let { binding.layoutCustomLayer.remove(it) }

                            // Create new DrawableDraw
                            val drawableDraw = DrawableDraw(resource, imageUrl)
                            binding.layoutCustomLayer.addDraw(drawableDraw)

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
        if (selectedDraws.values.all { it == null }) {
           // showToast(R.string.please_select_item)
            return
        }

        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    binding.layoutCustomLayer.save()
                }
                // TODO: Save bitmap to gallery or share
                showToast("Saved successfully!")
            } catch (e: Exception) {
                showToast("Save failed: ${e.message}")
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
                    // Create transparent background drawable
                    val transparentDrawable = ColorDrawable(Color.TRANSPARENT)

                    // Create TextDraw
                    val textDraw = TextDraw(this@EmojiCustomActivity, transparentDrawable, "text_${System.currentTimeMillis()}")
                    textDraw.setText(text)
                    textDraw.setTextColor(selectedColor)
                    textDraw.setTypeface(selectedTypeface)
                    textDraw.setTextAlign(Layout.Alignment.ALIGN_CENTER)
                    textDraw.resizeText()

                    // Add to DrawView
                    binding.layoutCustomLayer.addDraw(textDraw)

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
                // Convert bitmap to drawable
                val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)

                // Create DrawableDraw and add to main DrawView
                val drawableDraw = DrawableDraw(drawable, "draw_${System.currentTimeMillis()}")
                layoutCustomLayer.addDraw(drawableDraw)

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
