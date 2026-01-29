package com.pony.avatar.ocmaker.ui.emoji_custom

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
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
import com.pony.avatar.ocmaker.core.utils.key.EmojiApiConfig
import com.pony.avatar.ocmaker.core.utils.key.EmojiCategory
import com.pony.avatar.ocmaker.databinding.ActivityEmojiCustomBinding
import com.pony.avatar.ocmaker.data.model.draw.Draw
import com.pony.avatar.ocmaker.data.model.draw.DrawableDraw
import com.pony.avatar.ocmaker.dialog.DialogType
import com.pony.avatar.ocmaker.dialog.YesNoDialog
import com.pony.avatar.ocmaker.listener.listenerdraw.OnDrawListener
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

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { confirmExit() }
            actionBar.btnActionBarRightText.tap { handleSave() }
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
        }
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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        confirmExit()
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
