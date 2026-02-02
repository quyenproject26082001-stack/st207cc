package com.pony.avatar.ocmaker.ui.view

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pony.avatar.ocmaker.R
import com.pony.avatar.ocmaker.core.base.BaseActivity
import com.pony.avatar.ocmaker.core.extensions.checkPermissions
import com.pony.avatar.ocmaker.core.extensions.goToSettings
import com.pony.avatar.ocmaker.core.extensions.gone
import com.pony.avatar.ocmaker.core.extensions.handleBackLeftToRight
import com.pony.avatar.ocmaker.core.extensions.hideNavigation
import com.pony.avatar.ocmaker.core.extensions.invisible
import com.pony.avatar.ocmaker.core.extensions.loadImage
import com.pony.avatar.ocmaker.core.extensions.loadImageFromFile
import com.pony.avatar.ocmaker.core.extensions.requestPermission
import com.pony.avatar.ocmaker.core.extensions.select
import com.pony.avatar.ocmaker.core.extensions.setImageActionBar
import com.pony.avatar.ocmaker.core.extensions.setTextActionBar
import com.pony.avatar.ocmaker.core.extensions.strings
import com.pony.avatar.ocmaker.core.extensions.tap
import com.pony.avatar.ocmaker.core.helper.LanguageHelper
import com.pony.avatar.ocmaker.core.helper.UnitHelper
import com.pony.avatar.ocmaker.core.utils.key.IntentKey
import com.pony.avatar.ocmaker.core.utils.key.RequestKey
import com.pony.avatar.ocmaker.core.utils.key.ValueKey
import com.pony.avatar.ocmaker.core.utils.state.HandleState
import com.pony.avatar.ocmaker.databinding.ActivityViewBinding
import com.pony.avatar.ocmaker.dialog.YesNoDialog
import com.pony.avatar.ocmaker.ui.customize.CustomizeCharacterActivity
import com.pony.avatar.ocmaker.ui.emoji_custom.EmojiCustomActivity
import com.pony.avatar.ocmaker.ui.home.DataViewModel
import com.pony.avatar.ocmaker.ui.my_creation.fragment.MyAvatarFragment
import com.pony.avatar.ocmaker.ui.my_creation.MyCreationActivity
import com.pony.avatar.ocmaker.ui.my_creation.view_model.MyAvatarViewModel
import com.pony.avatar.ocmaker.ui.permission.PermissionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewActivity : BaseActivity<ActivityViewBinding>() {
    private val viewModel: ViewViewModel by viewModels()
    private val myAvatarViewModel: MyAvatarViewModel by viewModels()
    private val dataViewModel: DataViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()

    override fun setViewBinding(): ActivityViewBinding {
        return ActivityViewBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        dataViewModel.ensureData(this)
        viewModel.setPath(intent.getStringExtra(IntentKey.INTENT_KEY)!!)
        viewModel.updateStatusFrom(intent.getIntExtra(IntentKey.STATUS_KEY, ValueKey.AVATAR_TYPE))

        setButtonBackgrounds()
        setupUI()
    }

    private fun setButtonBackgrounds() {

    }

    private fun setupUI() {
        binding.apply {
            actionBar.apply {
             //   setTextActionBar(tvCenter, getString(R.string.my_work))
                //  setImageActionBar(btnActionBarNextRight, R.drawable.ic_edit_view)
                setImageActionBar(btnActionBarRight, R.drawable.ic_edit_view)

                // Handle edit button visibility based on source
                when (viewModel.statusFrom) {
                    ValueKey.AVATAR_TYPE -> {
                        // Avatar tab - always show edit button (avatars and emojis are editable)
                        // Keep visible (default)
                    }
                    ValueKey.MY_DESIGN_TYPE -> {
                        // Design tab - only show edit button if it's an editable emoji
                        checkAndShowEditButtonForDesign()
                    }
                }
            }

            // Set scaleType based on content type
            if (viewModel.statusFrom == ValueKey.AVATAR_TYPE) {
                // For avatars, use fitCenter to show full character without cropping
                imvImage.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            } else {
                // For designs, use center to maintain original size
                imvImage.scaleType = android.widget.ImageView.ScaleType.CENTER
            }
        }
    }

    /**
     * Check if the design item is an editable emoji and show/hide edit button accordingly
     */
    private fun checkAndShowEditButtonForDesign() {
        lifecycleScope.launch(Dispatchers.IO) {
            val pathInternal = viewModel.pathInternal.value
            val isEditableEmoji = try {
                val emojiEditList = com.pony.avatar.ocmaker.core.helper.MediaHelper
                    .readListFromFile<com.pony.avatar.ocmaker.data.model.custom.EmojiEditModel>(
                        this@ViewActivity,
                        ValueKey.EMOJI_EDIT_FILE_INTERNAL
                    )
                emojiEditList.any { it.pathInternalEdit == pathInternal }
            } catch (e: Exception) {
                false
            }

            withContext(Dispatchers.Main) {
                if (isEditableEmoji) {
                    binding.actionBar.btnActionBarRight.visibility = android.view.View.VISIBLE
                } else {
                    binding.actionBar.btnActionBarRight.invisible()
                }
            }
        }
    }

    private val editLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val newPath =
                    result.data?.getStringExtra("NEW_PATH") ?: return@registerForActivityResult
                viewModel.setPath(newPath)
                binding.imvImage.loadImageFromFile(newPath)
            }
        }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pathInternal.collect { path ->
                    loadImage(this@ViewActivity, path, binding.imvImage)
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarLeft.tap { handleBack() }
                btnActionBarRight.tap { handleEditClick(viewModel.pathInternal.value) }
            }

            includeLayoutBottom.btnWhatsapp.tap(2590) {
                viewModel.shareFiles(this@ViewActivity)
            }
            includeLayoutBottom.btnTelegram.tap(2000) {
                checkStoragePermission()
            }
            includeLayoutBottom.btnDelete.tap { handleDelete() }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
           // tvCenter.select()

            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            handleDownload()
        } else {
            val perms = permissionViewModel.getStoragePermissions()
            if (checkPermissions(perms)) {
                handleDownload()
            } else if (permissionViewModel.needGoToSettings(sharePreference, true)) {
                goToSettings()
            } else {
                requestPermission(perms, RequestKey.STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun handleDownload() {
        lifecycleScope.launch {
            viewModel.downloadFiles(this@ViewActivity).collect { state ->
                when (state) {
                    HandleState.LOADING -> showLoading()
                    HandleState.SUCCESS -> {
                        dismissLoading()
                        showToast(R.string.download_success)
                    }

                    else -> {
                        dismissLoading()
                        showToast(R.string.download_failed_please_try_again_later)
                    }
                }
            }
        }
    }

    private fun handleDelete() {
        val dialog =
            YesNoDialog(this, R.string.delete, R.string.are_you_sure_want_to_delete_this_item)
        LanguageHelper.setLocale(this)
        dialog.show()
        dialog.onNoClick = {
            dialog.dismiss()
            hideNavigation()
        }
        dialog.onYesClick = {
            dialog.dismiss()
            lifecycleScope.launch {
                viewModel.deleteFile(this@ViewActivity, viewModel.pathInternal.value)
                    .collect { state ->
                        when (state) {
                            HandleState.LOADING -> showLoading()
                            HandleState.SUCCESS -> {
                                dismissLoading()
                                resetMyCreationSelectionMode()

                                setResult(Activity.RESULT_OK, Intent().apply {
                                    putExtra("DELETED_PATH", viewModel.pathInternal.value)
                                })
                                finish()
                            }

                            else -> {
                                dismissLoading()
                                showToast(R.string.delete_failed_please_try_again)
                            }
                        }
                    }
            }
        }
    }

    private fun handleBack() {
        resetMyCreationSelectionMode()
        handleBackLeftToRight()
    }

    private fun resetMyCreationSelectionMode() {
        val myCreationActivity = MyCreationActivity.getInstance()
        if (myCreationActivity != null) {
            android.util.Log.d("ViewActivity", "Resetting selection mode in MyCreationActivity")

            val designFragment =
                myCreationActivity.supportFragmentManager.findFragmentByTag("MyDesignFragment")
            if (designFragment is com.pony.avatar.ocmaker.ui.my_creation.fragment.MyDesignFragment) {
                designFragment.resetSelectionMode()
            }

            val avatarFragment =
                myCreationActivity.supportFragmentManager.findFragmentByTag("MyAvatarFragment")
            if (avatarFragment is MyAvatarFragment) {
                avatarFragment.resetSelectionMode()
            }

            myCreationActivity.exitSelectionMode()
        } else {
            android.util.Log.w(
                "ViewActivity",
                "MyCreationActivity instance not found - unable to reset selection mode"
            )
        }
    }

    private fun handleEditClick(pathInternal: String) {
        // For design tab, it's always emoji (we only show edit button for emojis)
        if (viewModel.statusFrom == ValueKey.MY_DESIGN_TYPE) {
            handleEmojiEditClick(pathInternal)
            return
        }

        // For avatar tab, check if this is an emoji or avatar by reading from file directly
        // (Cannot use myAvatarViewModel.isEmoji() because ViewActivity has its own ViewModel instance
        // which doesn't have the avatar list loaded)
        lifecycleScope.launch(Dispatchers.IO) {
            val isEmoji = try {
                val emojiEditList = com.pony.avatar.ocmaker.core.helper.MediaHelper
                    .readListFromFile<com.pony.avatar.ocmaker.data.model.custom.EmojiEditModel>(
                        this@ViewActivity,
                        ValueKey.EMOJI_EDIT_FILE_INTERNAL
                    )
                emojiEditList.any { it.pathInternalEdit == pathInternal }
            } catch (e: Exception) {
                false
            }

            withContext(Dispatchers.Main) {
                if (isEmoji) {
                    handleEmojiEditClick(pathInternal)
                } else {
                    handleAvatarEditClick(pathInternal)
                }
            }
        }
    }

    private fun handleAvatarEditClick(pathInternal: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            showLoading()
            myAvatarViewModel.editItem(this@ViewActivity, pathInternal, dataViewModel.allData.value)

            withContext(Dispatchers.Main) {
                dismissLoading()

                myAvatarViewModel.checkDataInternet(this@ViewActivity) {
                    val intent =
                        Intent(this@ViewActivity, CustomizeCharacterActivity::class.java).apply {
                            putExtra(IntentKey.INTENT_KEY, myAvatarViewModel.positionCharacter)
                            putExtra(IntentKey.STATUS_FROM_KEY, ValueKey.EDIT)
                        }

                    editLauncher.launch(intent)
                    overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right)
                }
            }
        }
    }

    private fun handleEmojiEditClick(pathInternal: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            showLoading()
            val success = myAvatarViewModel.prepareEmojiEdit(this@ViewActivity, pathInternal)

            withContext(Dispatchers.Main) {
                dismissLoading()

                if (success) {
                    val intent = Intent(this@ViewActivity, EmojiCustomActivity::class.java).apply {
                        putExtra(IntentKey.STATUS_FROM_KEY, ValueKey.EDIT)
                    }
                    editLauncher.launch(intent)
                    overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right)
                } else {
                    showToast(R.string.error)
                }
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
                handleDownload()
            } else {
                permissionViewModel.updateStorageGranted(sharePreference, false)
            }
        }
    }

    @android.annotation.SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        handleBack()
    }
}
