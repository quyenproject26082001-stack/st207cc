package com.cat.emoji.fpf.maker.ui.emoji_sticker

import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseActivity
import com.cat.emoji.fpf.maker.core.extensions.handleBackLeftToRight
import com.cat.emoji.fpf.maker.core.extensions.hideNavigation
import com.cat.emoji.fpf.maker.core.extensions.select
import com.cat.emoji.fpf.maker.core.extensions.setImageActionBar
import com.cat.emoji.fpf.maker.core.extensions.setTextActionBar
import com.cat.emoji.fpf.maker.core.extensions.showInterAll
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.core.helper.InternetHelper
import com.cat.emoji.fpf.maker.core.service.RetrofitClient
import com.cat.emoji.fpf.maker.core.service.RetrofitPreventive
import com.cat.emoji.fpf.maker.core.utils.key.IntentKey
import com.cat.emoji.fpf.maker.databinding.ActivityEmojiStickerBinding
import com.cat.emoji.fpf.maker.dialog.DialogType
import com.cat.emoji.fpf.maker.dialog.YesNoDialog
import com.cat.emoji.fpf.maker.ui.emoji_sticker.adapter.CatEmojiStickerAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class EmojiStickerActivity : BaseActivity<ActivityEmojiStickerBinding>() {

    private val adapter = CatEmojiStickerAdapter()

    override fun setViewBinding(): ActivityEmojiStickerBinding {
        return ActivityEmojiStickerBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        binding.rcvSticker.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@EmojiStickerActivity.adapter
        }
        loadCategories()
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.tap { showInterAll { handleBackLeftToRight() } }

        adapter.onItemClick = { item ->
            if (!InternetHelper.isInternetAvailable(this)) {
                showNoInternetDialog()
            } else {
                val intent = android.content.Intent(this, EmojiStickerListActivity::class.java)
                intent.putExtra(IntentKey.STICKER_CATEGORY_NAME, item.category)
                intent.putExtra(IntentKey.STICKER_CATEGORY_ID, item.id)
                intent.putExtra(IntentKey.STICKER_QUANTITY, item.quantity)
                val option = android.app.ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
                startActivity(intent, option.toBundle())
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, getString(R.string.cat_emoji_sticker))
            tvCenter.select()
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
            hideNavigation()
            onDismiss?.invoke()
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            showLoading()
            val response = withTimeoutOrNull(5_000) {
                try {
                    RetrofitClient.api.getStickerCategories()
                } catch (e: Exception) { null }
            } ?: withTimeoutOrNull(5_000) {
                try {
                    RetrofitPreventive.api.getStickerCategories()
                } catch (e: Exception) { null }
            }

            dismissLoading()
            if (response?.isSuccessful == true && response.body() != null) {
                val sorted = response.body()!!.sortedBy { it.level }
                adapter.submitList(sorted)
            }
        }
    }
}
