package com.cat.emoji.fpf.maker.ui.my_creation

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.room.util.findColumnIndexBySuffix
import com.lvt.ads.util.Admob
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseActivity
import com.cat.emoji.fpf.maker.core.extensions.checkPermissions
import com.cat.emoji.fpf.maker.core.extensions.goToSettings
import com.cat.emoji.fpf.maker.core.extensions.gone
import com.cat.emoji.fpf.maker.core.extensions.hideNavigation
import com.cat.emoji.fpf.maker.core.extensions.invisible
import com.cat.emoji.fpf.maker.core.extensions.loadNativeCollabAds
import com.cat.emoji.fpf.maker.core.extensions.requestPermission
import com.cat.emoji.fpf.maker.core.extensions.select
import com.cat.emoji.fpf.maker.core.extensions.setImageActionBar
import com.cat.emoji.fpf.maker.core.extensions.setTextActionBar
import com.cat.emoji.fpf.maker.core.extensions.tap

import com.cat.emoji.fpf.maker.core.extensions.startIntentWithClearTop
import com.cat.emoji.fpf.maker.core.extensions.visible
import com.cat.emoji.fpf.maker.core.helper.LanguageHelper
import com.cat.emoji.fpf.maker.core.helper.UnitHelper
import com.cat.emoji.fpf.maker.core.utils.key.IntentKey
import com.cat.emoji.fpf.maker.core.utils.key.RequestKey
import com.cat.emoji.fpf.maker.core.utils.key.ValueKey
import com.cat.emoji.fpf.maker.core.utils.share.whatsapp.WhatsappSharingActivity
import com.cat.emoji.fpf.maker.core.utils.state.HandleState
import com.cat.emoji.fpf.maker.databinding.ActivityAlbumBinding
import com.cat.emoji.fpf.maker.dialog.YesNoDialog
import com.cat.emoji.fpf.maker.ui.home.HomeActivity
import com.cat.emoji.fpf.maker.ui.view.ViewActivity
import com.cat.emoji.fpf.maker.databinding.PopupMyAlbumBinding
import com.cat.emoji.fpf.maker.dialog.CreateNameDialog
import com.cat.emoji.fpf.maker.ui.my_creation.adapter.MyAvatarAdapter
import com.cat.emoji.fpf.maker.ui.my_creation.adapter.TypeAdapter
import com.cat.emoji.fpf.maker.ui.my_creation.fragment.MyAvatarFragment
import com.cat.emoji.fpf.maker.ui.my_creation.fragment.MyDesignFragment
import com.cat.emoji.fpf.maker.ui.my_creation.fragment.MyEmojiFragment
import com.cat.emoji.fpf.maker.ui.my_creation.view_model.MyAvatarViewModel
import com.cat.emoji.fpf.maker.ui.my_creation.view_model.MyCreationViewModel
import com.cat.emoji.fpf.maker.ui.permission.PermissionViewModel
import kotlinx.coroutines.launch
import kotlin.text.replace

class MyCreationActivity : WhatsappSharingActivity<ActivityAlbumBinding>() {
    companion object {
        private var instanceRef: java.lang.ref.WeakReference<MyCreationActivity>? = null

        fun getInstance(): MyCreationActivity? = instanceRef?.get()
    }

    private val viewModel: MyCreationViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()

    private var myAvatarFragment: MyAvatarFragment? = null
    private var myEmojiFragment: MyEmojiFragment? = null
    private var myDesignFragment: MyDesignFragment? = null
    private var isInSelectionMode = false
    private var isAllSelected = false
    private var pendingDownloadList: ArrayList<String>? = null

    override fun setViewBinding(): ActivityAlbumBinding {
        return ActivityAlbumBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Store instance reference for ViewActivity to access
        instanceRef = java.lang.ref.WeakReference(this)

        // Check if tab index is passed from SuccessActivity
        val tabIndex = intent.getIntExtra(IntentKey.TAB_INDEX_KEY, -1)
        val initialTab = if (tabIndex != -1) tabIndex else ValueKey.AVATAR_TYPE
        viewModel.setTypeStatus(initialTab)
        viewModel.setStatusFrom(intent.getBooleanExtra(IntentKey.FROM_SAVE, false))

        // Hide action bar buttons by default (only show in selection mode)
        binding.actionBar.apply {
            btnActionBarRight.gone()
            btnActionBarNextRight.gone()
        }
        binding.lnlBottom.isSelected = true
    }

    override fun dataObservable() {
        binding.apply {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.CREATED) {
                    launch {
                        viewModel.typeStatus.collect { type ->
                            if (type != -1) {
                                when (type) {
                                    ValueKey.AVATAR_TYPE -> {
                                        binding.cvType.setBackgroundResource(R.drawable.maker_selected)
                                        setupSelectedTab(btnMyPixel, tvSpace, imvFocusMyAvatar, subTabMyAvatar, isLeftTab = true)
                                        setupUnselectedTab(btnEmoji, tvEmoji, imvFocusEmoji, subTabBgEmoji, isLeftTab = false)
                                        setupUnselectedTab(btnMyDesign, tvMyDesign, imvFocusMyDesign, subTabMyDesign, isLeftTab = false)
                                    }
                                    ValueKey.EMOJI_TYPE -> {
                                        binding.cvType.setBackgroundResource(R.drawable.emoji_selected)
                                        setupUnselectedTab(btnMyPixel, tvSpace, imvFocusMyAvatar, subTabMyAvatar, isLeftTab = true)
                                        setupSelectedTab(btnEmoji, tvEmoji, imvFocusEmoji, subTabBgEmoji, isLeftTab = false)
                                        setupUnselectedTab(btnMyDesign, tvMyDesign, imvFocusMyDesign, subTabMyDesign, isLeftTab = false)
                                    }
                                    ValueKey.MY_DESIGN_TYPE -> {
                                        binding.cvType.setBackgroundResource(R.drawable.design_selected)
                                        setupUnselectedTab(btnMyPixel, tvSpace, imvFocusMyAvatar, subTabMyAvatar, isLeftTab = true)
                                        setupUnselectedTab(btnEmoji, tvEmoji, imvFocusEmoji, subTabBgEmoji, isLeftTab = false)
                                        setupSelectedTab(btnMyDesign, tvMyDesign, imvFocusMyDesign, subTabMyDesign, isLeftTab = false)
                                    }
                                }
                                showFragment(type)
                                updateBottomButtonsVisibility()
                            }
                        }
                    }
                    launch {
                        viewModel.downloadState.collect { state ->
                            when (state) {
                                HandleState.LOADING -> {
                                    showLoading()
                                }

                                HandleState.SUCCESS -> {
                                    dismissLoading()
                                    hideNavigation()
                                    showToast(R.string.download_success)
                                    // Auto-click back button to exit selection mode
                                    binding.actionBar.btnActionBarLeft.performClick()
                                }

                                else -> {
                                    dismissLoading()
                                    hideNavigation()
                                    showToast(R.string.download_failed_please_try_again_later)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarLeft.tap {
                    if (isInSelectionMode) {
                        resetVisibleFragmentSelectionMode()
                    } else {
                        startIntentWithClearTop(HomeActivity::class.java)
                    }
                }

                // Select All button
                btnActionBarRight.tap {
                    handleSelectAllFromCurrentFragment()
                }

                // Delete All button
                btnActionBarNextRight.tap {
                    handleDeleteSelectedFromCurrentFragment()
                }
            }

            btnMyPixel.tap { viewModel.setTypeStatus(ValueKey.AVATAR_TYPE) }
            btnEmoji.tap { viewModel.setTypeStatus(ValueKey.EMOJI_TYPE) }
            btnMyDesign.tap { viewModel.setTypeStatus(ValueKey.MY_DESIGN_TYPE) }

            // WhatsApp, Telegram, and Download buttons in lnlBottom
            val layoutBottom = lnlBottom.getChildAt(0)
            layoutBottom.findViewById<View>(R.id.btnWhatsapp)?.tap(2500) {
                val selectedPaths = getSelectedPathsFromCurrentFragment()
                handleAddToWhatsApp(selectedPaths)
            }
            layoutBottom.findViewById<View>(R.id.btnTelegram)?.tap(2500) {
                val selectedPaths = getSelectedPathsFromCurrentFragment()
                handleAddToTelegram(selectedPaths)
            }
            layoutBottom.findViewById<View>(R.id.btnDownload)?.tap(2500) {
                handleDownloadFromCurrentFragment()
            }

            // Delete button in deleteSection         }
        }
    }

    private fun handleShareFromCurrentFragment() {
        val selectedPaths = getSelectedPathsFromCurrentFragment()
        handleShare(selectedPaths)
    }

    private fun handleDownloadFromCurrentFragment() {
        val selectedPaths = getSelectedPathsFromCurrentFragment()
        if (selectedPaths.isEmpty()) {
            showToast(R.string.please_select_an_image)
            return
        }
        checkStoragePermissionForDownload(selectedPaths)
    }

    private fun checkStoragePermissionForDownload(list: ArrayList<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ không cần quyền WRITE_EXTERNAL_STORAGE
            handleDownload(list)
        } else {
            // Android 8-9 cần check quyền
            val perms = permissionViewModel.getStoragePermissions()
            if (checkPermissions(perms)) {
                handleDownload(list)
            } else if (permissionViewModel.needGoToSettings(sharePreference, true)) {
                goToSettings()
            } else {
                // Lưu lại list để download sau khi được cấp quyền
                pendingDownloadList = list
                requestPermission(perms, RequestKey.STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun handleSelectAllFromCurrentFragment() {
        val shouldSelect = !isAllSelected
        when {
            getVisibleAvatarFragment() != null -> getVisibleAvatarFragment()!!.let {
                if (shouldSelect) it.selectAllItems() else it.deselectAllItems()
            }
            getVisibleEmojiFragment() != null -> getVisibleEmojiFragment()!!.let {
                if (shouldSelect) it.selectAllItems() else it.deselectAllItems()
            }
            getVisibleDesignFragment() != null -> getVisibleDesignFragment()!!.let {
                if (shouldSelect) it.selectAllItems() else it.deselectAllItems()
            }
        }
        isAllSelected = shouldSelect
        binding.actionBar.btnActionBarRight.setImageResource(
            if (shouldSelect) R.drawable.ic_select_all else R.drawable.ic_not_select_all
        )
    }

    private fun handleDeleteSelectedFromCurrentFragment() {
        getVisibleAvatarFragment()?.deleteSelectedItems()
            ?: getVisibleEmojiFragment()?.deleteSelectedItems()
            ?: getVisibleDesignFragment()?.deleteSelectedItems()
    }

    private fun getSelectedPathsFromCurrentFragment(): ArrayList<String> {
        return getVisibleAvatarFragment()?.getSelectedPaths()
            ?: getVisibleEmojiFragment()?.getSelectedPaths()
            ?: getVisibleDesignFragment()?.getSelectedPaths()
            ?: arrayListOf()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, getString(R.string.my_work))
            tvCenter.select()

            // Select All button (btnActionBarRight) - resize to 24dp for select all icons
            val size24dp = (24 * resources.displayMetrics.density).toInt()
            val params = btnActionBarRight.layoutParams
            params.width = size24dp
            params.height = size24dp
            btnActionBarRight.layoutParams = params

            btnActionBarRight.setImageResource(R.drawable.ic_not_select_all)
            btnActionBarRight.gone()

            // Delete All button - hidden initially, only shown in selection mode
            btnActionBarNextRight.setImageResource(R.drawable.ic_delete_item)
            btnActionBarNextRight.gone()
        }
    }

    override fun initText() {
        binding.apply {
            tvSpace.select()
            tvEmoji.select()
            tvMyDesign.select()
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
                pendingDownloadList?.let { list ->
                    handleDownload(list)
                    pendingDownloadList = null
                }
            } else {
                permissionViewModel.updateStorageGranted(sharePreference, false)
                pendingDownloadList = null
            }
        }
    }

    fun handleShare(list: ArrayList<String>) {
        if (list.isEmpty()) {
            showToast(R.string.please_select_an_image)
            return
        }
        viewModel.shareImages(this, list)
    }

    fun handleAddToTelegram(list: ArrayList<String>) {
        if (list.isEmpty()) {
            showToast(R.string.please_select_an_image)
            return
        }
        viewModel.addToTelegram(this, list)
        // Auto-click back button to exit selection mode
        binding.actionBar.btnActionBarLeft.performClick()
    }

    fun handleAddToWhatsApp(list: ArrayList<String>) {
        if (list.size < 3) {
            showToast(R.string.limit_3_items)
            return
        }
        if (list.size > 30) {
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
        dialog.onNoClick = {
            dismissDialog()
        }
        dialog.onDismissClick = {
            dismissDialog()
        }

        dialog.onYesClick = { packageName ->
            dismissDialog()
            viewModel.addToWhatsapp(this, packageName, list) { stickerPack ->
                if (stickerPack != null) {
                    addToWhatsapp(stickerPack)
                    // Auto-click back button to exit selection mode
                    binding.actionBar.btnActionBarLeft.performClick()
                }
            }
        }
    }

    private fun handleDownload(list: ArrayList<String>) {
        viewModel.downloadFiles(this, list)
    }

    private fun showFragment(type: Int) {
        val transaction = supportFragmentManager.beginTransaction()

        if (myAvatarFragment == null) {
            myAvatarFragment = MyAvatarFragment()
            transaction.add(R.id.frmList, myAvatarFragment!!, "MyAvatarFragment")
        }
        if (myEmojiFragment == null) {
            myEmojiFragment = MyEmojiFragment()
            transaction.add(R.id.frmList, myEmojiFragment!!, "MyEmojiFragment")
        }
        if (myDesignFragment == null) {
            myDesignFragment = MyDesignFragment()
            transaction.add(R.id.frmList, myDesignFragment!!, "MyDesignFragment")
        }

        when (type) {
            ValueKey.AVATAR_TYPE -> {
                myAvatarFragment?.let { transaction.show(it) }
                myEmojiFragment?.let { transaction.hide(it) }
                myDesignFragment?.let { transaction.hide(it) }
            }
            ValueKey.EMOJI_TYPE -> {
                myAvatarFragment?.let { transaction.hide(it) }
                myEmojiFragment?.let { transaction.show(it) }
                myDesignFragment?.let { transaction.hide(it) }
            }
            ValueKey.MY_DESIGN_TYPE -> {
                myAvatarFragment?.let { transaction.hide(it) }
                myEmojiFragment?.let { transaction.hide(it) }
                myDesignFragment?.let { transaction.show(it) }
            }
        }

        transaction.commit()
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        startIntentWithClearTop(HomeActivity::class.java)
    }

//    fun initNativeCollab() {
//        loadNativeCollabAds(R.string.native_cl_myCharactor, binding.flNativeCollab, binding.lnlBottom)
//    }
//    override fun initAds() {
//        initNativeCollab()
//        Admob.getInstance().loadNativeAd(
//            this,
//            getString(R.string.native_myCharactor),
//            binding.nativeAds,
//            R.layout.ads_native_banner
//        )
//    }

    override fun onRestart() {
        super.onRestart()
        android.util.Log.w(
            "MyCreationActivity",
            "🔄 onRestart() called - Activity restarting after being stopped"
        )
        android.util.Log.w("MyCreationActivity", "Selection mode: $isInSelectionMode")

        // Exit selection mode when returning from another activity
        if (isInSelectionMode) {
            resetVisibleFragmentSelectionMode()
            exitSelectionMode()
        }

        initNativeCollab()
        android.util.Log.w("MyCreationActivity", "🔄 onRestart() END")
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.w("MyCreationActivity", "🔵 onStart() called - Activity becoming visible")
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.w("MyCreationActivity", "🟢 onResume() called - Activity in foreground")
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.w("MyCreationActivity", "🟡 onPause() called - Activity losing focus")
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.w("MyCreationActivity", "🔴 onStop() called - Activity no longer visible")
    }

    fun enterSelectionMode() {
        isInSelectionMode = true
        isAllSelected = false
        binding.actionBar.apply {
            // Show select all and delete all buttons
            btnActionBarRight.setImageResource(R.drawable.ic_not_select_all)
            btnActionBarRight.visible()
            btnActionBarNextRight.visible()
        }
        updateBottomButtonsVisibility()
        android.util.Log.d("MyCreationActivity", "enterSelectionMode called - showing buttons")
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        isAllSelected = false
        binding.actionBar.apply {
            // Hide select all and delete all buttons
            btnActionBarRight.gone()
            btnActionBarNextRight.gone()
        }

        updateBottomButtonsVisibility()
        android.util.Log.d("MyCreationActivity", "exitSelectionMode called - hiding buttons")
    }

    private fun updateBottomButtonsVisibility() {
        val layoutBottom = binding.lnlBottom.getChildAt(0)
        val btnWhatsapp = layoutBottom.findViewById<View>(R.id.btnWhatsapp)
        val btnTelegram = layoutBottom.findViewById<View>(R.id.btnTelegram)
        val btnDownload = layoutBottom.findViewById<View>(R.id.btnDownload)

        if (!isInSelectionMode) {
            btnWhatsapp?.gone()
            btnTelegram?.gone()
            btnDownload?.gone()
        } else if (viewModel.typeStatus.value == ValueKey.MY_DESIGN_TYPE||viewModel.typeStatus.value == ValueKey.EMOJI_TYPE) {
            btnWhatsapp?.gone()
            btnTelegram?.gone()
            btnDownload?.visible()
        } else {
            // Avatar or Emoji tab: show WhatsApp and Telegram
            btnWhatsapp?.visible()
            btnTelegram?.visible()
            btnDownload?.gone()
        }
    }

    private fun setupSelectedTab(
        tabView: View,
        textView: android.widget.TextView,
        focusImage: android.widget.ImageView,
        subTab: View,
        isLeftTab: Boolean
    ) {
        val params = tabView.layoutParams as android.widget.LinearLayout.LayoutParams
        params.weight = 1.0f
        params.topMargin = 0

        // nếu vẫn cần overlap thì giữ, còn không thì set về 0
        if (isLeftTab) params.marginEnd = 0 else params.marginStart = 0
        tabView.layoutParams = params

        // Text selected
        textView.textSize = 16f
        textView.paint.shader = null
        textView.setTextColor(Color.WHITE)

        // ❌ Không dùng background tab nữa
        focusImage.gone()
        subTab.gone()
    }


    private fun setupUnselectedTab(
        tabView: View,
        textView: android.widget.TextView,
        focusImage: android.widget.ImageView,
        subTab: View,
        isLeftTab: Boolean
    ) {
        val params = tabView.layoutParams as android.widget.LinearLayout.LayoutParams
        params.weight = 1f
        params.topMargin = 0

        // nếu vẫn cần overlap thì giữ, còn không thì set về 0
        if (isLeftTab) params.marginEnd = 0 else params.marginStart = 0
        tabView.layoutParams = params

        // Text unselected
        textView.textSize = 16f
        textView.paint.shader = null
        textView.setTextColor(Color.parseColor("#7AAB36"))

        // ❌ Không dùng background tab nữa
        focusImage.gone()
        subTab.gone()
    }

    // Public method to update select all icon based on selection state
    fun updateSelectAllIcon(allSelected: Boolean) {
        isAllSelected = allSelected
        if (allSelected) {
            binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_select_all)
        } else {
            binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_not_select_all)
        }
    }

    private fun getVisibleAvatarFragment(): MyAvatarFragment? {
        val f = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
        return if (f is MyAvatarFragment && f.isVisible) f else null
    }

    private fun getVisibleEmojiFragment(): MyEmojiFragment? {
        val f = supportFragmentManager.findFragmentByTag("MyEmojiFragment")
        return if (f is MyEmojiFragment && f.isVisible) f else null
    }

    private fun getVisibleDesignFragment(): MyDesignFragment? {
        val f = supportFragmentManager.findFragmentByTag("MyDesignFragment")
        return if (f is MyDesignFragment && f.isVisible) f else null
    }

    private fun resetVisibleFragmentSelectionMode() {
        getVisibleAvatarFragment()?.resetSelectionMode()
        getVisibleEmojiFragment()?.resetSelectionMode()
        getVisibleDesignFragment()?.resetSelectionMode()
    }

    fun initNativeCollab() {
        Admob.getInstance().loadNativeCollapNotBanner(this,getString(R.string.native_cl_myWork), binding.flNativeCollab)
    }

    override fun initAds() {
        initNativeCollab()
        Admob.getInstance().loadNativeAd(
            this,
            getString(R.string.native_categoryMaker),
            binding.nativeAds,
            R.layout.ads_native_banner
        )
    }

}