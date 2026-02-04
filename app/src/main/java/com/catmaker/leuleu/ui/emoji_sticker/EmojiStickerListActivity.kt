package com.catmaker.leuleu.ui.emoji_sticker

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.catmaker.leuleu.R
import com.catmaker.leuleu.core.extensions.checkPermissions
import com.catmaker.leuleu.core.extensions.goToSettings
import com.catmaker.leuleu.core.extensions.handleBackLeftToRight
import com.catmaker.leuleu.core.extensions.hideNavigation
import com.catmaker.leuleu.core.extensions.requestPermission
import com.catmaker.leuleu.core.extensions.select
import com.catmaker.leuleu.core.extensions.setImageActionBar
import com.catmaker.leuleu.core.extensions.setTextActionBar
import com.catmaker.leuleu.core.extensions.showInterAll
import com.catmaker.leuleu.core.extensions.tap
import com.catmaker.leuleu.core.helper.LanguageHelper
import com.catmaker.leuleu.core.helper.MediaHelper
import com.catmaker.leuleu.core.utils.key.DomainKey
import com.catmaker.leuleu.core.utils.key.IntentKey
import com.catmaker.leuleu.core.utils.key.RequestKey
import com.catmaker.leuleu.core.utils.share.telegram.TelegramSharing
import com.catmaker.leuleu.core.utils.share.whatsapp.IdGenerator
import com.catmaker.leuleu.core.utils.share.whatsapp.StickerBook
import com.catmaker.leuleu.core.utils.share.whatsapp.StickerPack
import com.catmaker.leuleu.core.utils.share.whatsapp.WhatsappSharingActivity
import com.catmaker.leuleu.core.utils.state.HandleState
import com.catmaker.leuleu.databinding.ActivityEmojiStickerListBinding
import com.catmaker.leuleu.dialog.CreateNameDialog
import com.catmaker.leuleu.ui.emoji_sticker.adapter.CatEmojiStickerListAdapter
import com.catmaker.leuleu.ui.permission.PermissionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.URLEncoder

class EmojiStickerListActivity : WhatsappSharingActivity<ActivityEmojiStickerListBinding>() {

    private val adapter = CatEmojiStickerListAdapter()
    private var categoryName = ""
    private val okHttpClient = OkHttpClient()
    private val permissionViewModel: PermissionViewModel by viewModels()
    private var pendingDownloadUrls: List<String>? = null

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
        setupBackPressHandler()
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.tap {
            if (adapter.isSelectMode) {
                exitSelectMode()
            } else {
                showInterAll { handleBackLeftToRight() }
            }
        }

        // Long press để vào select mode
        adapter.onItemLongClick = { _ ->
            enterSelectMode()
        }

        // Download single sticker
        adapter.onDownloadClick = { url ->
            handleDownloadSingle(url)
        }

        // Callback khi số lượng selected thay đổi
        adapter.onSelectionChanged = { count ->
            updateActionBarTitle(count)
            updateSelectAllIcon()
            if (count == 0 && adapter.isSelectMode) {
                exitSelectMode()
            }
        }

        // Select All button
        binding.actionBar.btnActionBarRight.tap {
            handleSelectAll()
        }

        // Bottom buttons
        binding.lnlBottom.findViewById<View>(R.id.btnDownload)?.tap {
            handleDownload()
        }
        binding.lnlBottom.findViewById<View>(R.id.btnWhatsapp)?.tap {
            handleWhatsappClick()
        }
        binding.lnlBottom.findViewById<View>(R.id.btnTelegram)?.tap {
            handleTelegramClick()
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, categoryName)
            tvCenter.select()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapter.isSelectMode) {
                    exitSelectMode()
                } else {
                    showInterAll { handleBackLeftToRight() }
                }
            }
        })
    }

    private fun enterSelectMode() {
        adapter.enterSelectMode()
        binding.lnlBottom.visibility = View.VISIBLE
        binding.actionBar.btnActionBarLeft.setImageResource(R.drawable.ic_close_1)
        // Show Select All button
        binding.actionBar.btnActionBarRight.visibility = View.VISIBLE
        binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_not_select_all)
        updateActionBarTitle(adapter.getSelectedCount())
    }

    private fun exitSelectMode() {
        adapter.exitSelectMode()
        binding.lnlBottom.visibility = View.GONE
        binding.actionBar.btnActionBarLeft.setImageResource(R.drawable.ic_back)
        // Hide Select All button
        binding.actionBar.btnActionBarRight.visibility = View.INVISIBLE
        binding.actionBar.tvCenter.text = categoryName
    }

    private fun handleSelectAll() {
        if (adapter.isAllSelected()) {
            adapter.deselectAll()
        } else {
            adapter.selectAll()
        }
    }

    private fun updateSelectAllIcon() {
        binding.actionBar.btnActionBarRight.setImageResource(
            if (adapter.isAllSelected()) R.drawable.ic_select_all else R.drawable.ic_not_select_all
        )
    }

    private fun updateActionBarTitle(count: Int) {
        binding.actionBar.tvCenter.text = if (count > 0) {
            getString(R.string.selected_count, count)
        } else {
            getString(R.string.select_stickers)
        }
    }

    // ==================== DOWNLOAD ====================
    private fun handleDownloadSingle(url: String) {
        checkStoragePermissionForDownload(listOf(url))
    }

    private fun handleDownload() {
        val selectedUrls = adapter.getSelectedItems()
        if (selectedUrls.isEmpty()) {
            showToast(R.string.please_select_stickers)
            return
        }
        checkStoragePermissionForDownload(selectedUrls)
    }

    private fun checkStoragePermissionForDownload(urls: List<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ không cần quyền WRITE_EXTERNAL_STORAGE
            performDownload(urls)
        } else {
            // Android 8-9 cần check quyền
            val perms = permissionViewModel.getStoragePermissions()
            if (checkPermissions(perms)) {
                performDownload(urls)
            } else if (permissionViewModel.needGoToSettings(sharePreference, true)) {
                goToSettings()
            } else {
                // Lưu lại list để download sau khi được cấp quyền
                pendingDownloadUrls = urls
                requestPermission(perms, RequestKey.STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun performDownload(urls: List<String>) {
        lifecycleScope.launch {
            showLoading()
            val bitmaps = downloadBitmapsFromUrls(urls)
            if (bitmaps.isEmpty()) {
                dismissLoading()
                showToast(R.string.download_failed_please_try_again_later)
                return@launch
            }

            // Save to external storage
            var allSuccess = true
            for (bitmap in bitmaps) {
                MediaHelper.saveBitmapToExternal(this@EmojiStickerListActivity, bitmap)
                    .flowOn(Dispatchers.IO)
                    .collect { state ->
                        if (state == HandleState.FAIL) {
                            allSuccess = false
                        }
                    }
            }

            dismissLoading()
            hideNavigation()
            if (allSuccess) {
                showToast(R.string.download_success)
                // Chỉ exit select mode nếu đang ở select mode
                if (adapter.isSelectMode) {
                    exitSelectMode()
                }
            } else {
                showToast(R.string.download_failed_please_try_again_later)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RequestKey.STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                permissionViewModel.updateStorageGranted(sharePreference, true)
                showToast(R.string.granted_storage)
                // Thực hiện download sau khi được cấp quyền
                pendingDownloadUrls?.let { urls ->
                    performDownload(urls)
                    pendingDownloadUrls = null
                }
            } else {
                permissionViewModel.updateStorageGranted(sharePreference, false)
                pendingDownloadUrls = null
            }
        }
    }

    // ==================== WHATSAPP ====================
    private fun handleWhatsappClick() {
        val selectedUrls = adapter.getSelectedItems()
        if (selectedUrls.size < 3) {
            showToast(R.string.limit_3_items)
            return
        }
        if (selectedUrls.size > 30) {
            showToast(R.string.limit_30_items)
            return
        }

        val dialog = CreateNameDialog(this)
        LanguageHelper.setLocale(this)
        dialog.show()

        fun dismissDialog() {
            dialog.dismiss()
            hideNavigation()
        }

        dialog.onNoClick = { dismissDialog() }
        dialog.onDismissClick = { dismissDialog() }

        dialog.onYesClick = { packageName ->
            dismissDialog()
            processWhatsApp(packageName, selectedUrls)
        }
    }

    private fun processWhatsApp(packageName: String, urls: List<String>) {
        lifecycleScope.launch {
            showLoading()
            val localPaths = downloadImagesToCache(urls)
            if (localPaths.isEmpty()) {
                dismissLoading()
                showToast(R.string.download_failed_please_try_again_later)
                return@launch
            }

            val uriList = getUrisFromPaths(localPaths)
            val packId = IdGenerator.generateIdFromUrl(
                this@EmojiStickerListActivity,
                com.catmaker.leuleu.core.helper.StringHelper.generateRandomString(10)
            )
            val stickerPack = StickerPack(packId, packageName, uriList, this@EmojiStickerListActivity)
            StickerBook.addPackIfNotAlreadyAdded(stickerPack)

            dismissLoading()
            addToWhatsapp(stickerPack)
            exitSelectMode()
        }
    }

    // ==================== TELEGRAM ====================
    private fun handleTelegramClick() {
        val selectedUrls = adapter.getSelectedItems()
        if (selectedUrls.isEmpty()) {
            showToast(R.string.please_select_stickers)
            return
        }

        lifecycleScope.launch {
            showLoading()
            val localPaths = downloadImagesToCache(selectedUrls)
            if (localPaths.isEmpty()) {
                dismissLoading()
                showToast(R.string.download_failed_please_try_again_later)
                return@launch
            }

            val uriList = getUrisFromPaths(localPaths)
            dismissLoading()
            TelegramSharing.importToTelegram(this@EmojiStickerListActivity, uriList)
            exitSelectMode()
        }
    }

    // ==================== HELPER FUNCTIONS ====================
    private suspend fun downloadBitmapsFromUrls(urls: List<String>): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        for (url in urls) {
            try {
                val inputStream = URL(url).openStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (bitmap != null) {
                    bitmaps.add(bitmap)
                }
            } catch (e: Exception) {
                Log.e("StickerList", "Error downloading bitmap: $url", e)
            }
        }
        bitmaps
    }

    private suspend fun downloadImagesToCache(urls: List<String>): List<String> {
        val stickerCacheDir = File(cacheDir, "stickers")
        return withContext(Dispatchers.IO) {
            val paths = mutableListOf<String>()
            if (!stickerCacheDir.exists()) stickerCacheDir.mkdirs()

            // Clear old cache
            stickerCacheDir.listFiles()?.forEach { it.delete() }

            for ((index, url) in urls.withIndex()) {
                try {
                    val inputStream = URL(url).openStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    if (bitmap != null) {
                        // Resize to 512x512 for sticker
                        val resizedBitmap = bitmap.scale(512, 512)
                        val file = File(stickerCacheDir, "sticker_$index.png")
                        FileOutputStream(file).use { out ->
                            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        paths.add(file.absolutePath)
                        if (bitmap != resizedBitmap) bitmap.recycle()
                        resizedBitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.e("StickerList", "Error downloading image: $url", e)
                }
            }
            paths
        }
    }

    private fun getUrisFromPaths(paths: List<String>): ArrayList<Uri> {
        val uris = ArrayList<Uri>()
        for (path in paths) {
            val file = File(path)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
                uris.add(uri)
            }
        }
        return uris
    }

    // ==================== LOAD STICKER LIST ====================
    private fun loadStickerList() {
        val encodedCategory = URLEncoder.encode(categoryName, "UTF-8").replace("+", "%20")
        val baseUrl = "${DomainKey.BASE_URL}${DomainKey.SUB_DOMAIN_CAT_STICKER}/Sticker/$encodedCategory"

        lifecycleScope.launch {
            val urls = mutableListOf<String>()
            var index = 1
            val maxLimit = 200

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
                .head()
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("StickerList", "Error checking URL: $url", e)
            false
        }
    }
}
