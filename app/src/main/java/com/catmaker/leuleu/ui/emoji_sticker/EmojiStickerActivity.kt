package com.catmaker.leuleu.ui.emoji_sticker

import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.catmaker.leuleu.R
import com.catmaker.leuleu.core.base.BaseActivity
import com.catmaker.leuleu.core.extensions.handleBackLeftToRight
import com.catmaker.leuleu.core.extensions.select
import com.catmaker.leuleu.core.extensions.setImageActionBar
import com.catmaker.leuleu.core.extensions.setTextActionBar
import com.catmaker.leuleu.core.extensions.showInterAll
import com.catmaker.leuleu.core.extensions.tap
import com.catmaker.leuleu.core.service.RetrofitClient
import com.catmaker.leuleu.core.service.RetrofitPreventive
import com.catmaker.leuleu.core.utils.key.IntentKey
import com.catmaker.leuleu.databinding.ActivityEmojiStickerBinding
import com.catmaker.leuleu.ui.emoji_sticker.adapter.CatEmojiStickerAdapter
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
            val intent = android.content.Intent(this, EmojiStickerListActivity::class.java)
            intent.putExtra(IntentKey.STICKER_CATEGORY_NAME, item.category)
            intent.putExtra(IntentKey.STICKER_CATEGORY_ID, item.id)
            intent.putExtra(IntentKey.STICKER_QUANTITY, item.quantity)
            val option = android.app.ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
            startActivity(intent, option.toBundle())
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, getString(R.string.cat_sticker))
            tvCenter.select()
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
