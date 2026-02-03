package com.pony.avatar.ocmaker.ui.emoji_sticker

import android.view.LayoutInflater
import androidx.recyclerview.widget.GridLayoutManager
import com.pony.avatar.ocmaker.R
import com.pony.avatar.ocmaker.core.base.BaseActivity
import com.pony.avatar.ocmaker.core.extensions.handleBackLeftToRight
import com.pony.avatar.ocmaker.core.extensions.select
import com.pony.avatar.ocmaker.core.extensions.setImageActionBar
import com.pony.avatar.ocmaker.core.extensions.setTextActionBar
import com.pony.avatar.ocmaker.core.extensions.showInterAll
import com.pony.avatar.ocmaker.core.extensions.tap
import com.pony.avatar.ocmaker.core.utils.key.DomainKey
import com.pony.avatar.ocmaker.core.utils.key.IntentKey
import com.pony.avatar.ocmaker.databinding.ActivityEmojiStickerListBinding
import com.pony.avatar.ocmaker.ui.emoji_sticker.adapter.CatEmojiStickerListAdapter
import java.net.URLEncoder

class EmojiStickerListActivity : BaseActivity<ActivityEmojiStickerListBinding>() {

    private val adapter = CatEmojiStickerListAdapter()
    private var categoryName = ""
    private var quantity = 0

    override fun setViewBinding(): ActivityEmojiStickerListBinding {
        return ActivityEmojiStickerListBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        categoryName = intent.getStringExtra(IntentKey.STICKER_CATEGORY_NAME) ?: ""
        quantity = intent.getIntExtra(IntentKey.STICKER_QUANTITY, 0)

        binding.rcvStickerList.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@EmojiStickerListActivity.adapter
        }
        loadStickerList()
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.tap { showInterAll { handleBackLeftToRight() } }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, categoryName)
            tvCenter.select()
        }
    }

    private fun loadStickerList() {
        if (quantity <= 0) return

        val encodedCategory = URLEncoder.encode(categoryName, "UTF-8").replace("+", "%20")
        val baseUrl = "${DomainKey.BASE_URL}${DomainKey.SUB_DOMAIN_CAT_STICKER}/$encodedCategory"

        val urls = (1..quantity).map { index ->
            "$baseUrl/$index${DomainKey.LAYER_EXTENSION}"
        }
        adapter.submitList(urls)
    }
}
