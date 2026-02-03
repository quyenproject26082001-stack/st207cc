package com.pony.avatar.ocmaker.ui.emoji_sticker

import android.util.Log
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class EmojiStickerListActivity : BaseActivity<ActivityEmojiStickerListBinding>() {

    private val adapter = CatEmojiStickerListAdapter()
    private var categoryName = ""
    private val okHttpClient = OkHttpClient()

    override fun setViewBinding(): ActivityEmojiStickerListBinding {
        return ActivityEmojiStickerListBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        categoryName = intent.getStringExtra(IntentKey.STICKER_CATEGORY_NAME) ?: ""

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
        val encodedCategory = URLEncoder.encode(categoryName, "UTF-8").replace("+", "%20")
        val baseUrl = "${DomainKey.BASE_URL}${DomainKey.SUB_DOMAIN_CAT_STICKER}/Sticker/$encodedCategory"

        lifecycleScope.launch {
            val urls = mutableListOf<String>()
            var index = 1
            val maxLimit = 200 // Giới hạn tối đa để tránh loop vô hạn

            withContext(Dispatchers.IO) {
                while (index <= maxLimit) {
                    val url = "$baseUrl/$index${DomainKey.LAYER_EXTENSION}"
                    if (checkUrlExists(url)) {
                        urls.add(url)
                        index++
                    } else {
                        Log.d("StickerList", "Stopped at index $index, total: ${urls.size}")
                        break
                    }
                }
            }

            adapter.submitList(urls)
        }
    }

    private fun checkUrlExists(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .head() // Chỉ lấy header, không download body
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful // true nếu 200-299
        } catch (e: Exception) {
            Log.e("StickerList", "Error checking URL: $url", e)
            false
        }
    }
}
