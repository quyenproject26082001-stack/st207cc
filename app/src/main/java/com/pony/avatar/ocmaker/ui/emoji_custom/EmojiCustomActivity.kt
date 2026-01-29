package com.pony.avatar.ocmaker.ui.emoji_custom

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
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
import com.pony.avatar.ocmaker.dialog.DialogType
import com.pony.avatar.ocmaker.dialog.YesNoDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmojiCustomActivity : BaseActivity<ActivityEmojiCustomBinding>() {

    private val navigationAdapter by lazy { EmojiNavigationAdapter() }
    private val layerAdapter by lazy { EmojiLayerAdapter() }

    private var currentCategoryIndex = 0
    private val categories = EmojiApiHelper.getAllCategories()

    // Lưu trữ selected items cho mỗi category
    private val selectedItems = mutableMapOf<String, String?>()

    // ImageViews cho mỗi layer
    private val layerViews = mutableListOf<ImageView>()

    override fun setViewBinding(): ActivityEmojiCustomBinding {
        return ActivityEmojiCustomBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        initLayerViews()
        initRcv()
        loadNavigationData()
        loadLayerData(0)
    }

    private fun initLayerViews() {
        // Tạo ImageView cho mỗi category (layer)
        categories.forEachIndexed { index, _ ->
            val imageView = ImageView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            binding.layoutCustomLayer.addView(imageView)
            layerViews.add(imageView)
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
        val items = (1..category.count).map { index ->
            val imageUrl = EmojiApiConfig.getImageUrl(category.name, index)
            EmojiLayerItem(
                imageUrl = imageUrl,
                isSelected = selectedItems[category.name] == imageUrl
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
            if (selectedItems[category.name] == imageUrl) {
                // Deselect
                selectedItems[category.name] = null
                Glide.with(this).clear(layerViews[currentCategoryIndex])
            } else {
                // Select
                selectedItems[category.name] = imageUrl
                Glide.with(this)
                    .load(imageUrl)
                    .into(layerViews[currentCategoryIndex])
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
        // TODO: Implement save functionality
        showToast("Save emoji")
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
