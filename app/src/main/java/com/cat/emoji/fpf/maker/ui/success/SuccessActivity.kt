package com.cat.emoji.fpf.maker.ui.success

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseActivity
import com.cat.emoji.fpf.maker.core.extensions.checkPermissions
import com.cat.emoji.fpf.maker.core.extensions.goToSettings
import com.cat.emoji.fpf.maker.core.extensions.gone
import com.cat.emoji.fpf.maker.core.extensions.handleBackLeftToRight
import com.cat.emoji.fpf.maker.core.extensions.invisible
import com.cat.emoji.fpf.maker.core.extensions.loadImage
import com.cat.emoji.fpf.maker.core.extensions.requestPermission
import com.cat.emoji.fpf.maker.core.extensions.select
import com.cat.emoji.fpf.maker.core.extensions.setImageActionBar
import com.cat.emoji.fpf.maker.core.extensions.setTextActionBar
import com.cat.emoji.fpf.maker.core.extensions.showInterAll
import com.cat.emoji.fpf.maker.core.extensions.startIntentRightToLeft
import com.cat.emoji.fpf.maker.core.extensions.startIntentWithClearTop
import com.cat.emoji.fpf.maker.core.extensions.strings
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.core.extensions.visible
import com.cat.emoji.fpf.maker.core.helper.UnitHelper
import com.cat.emoji.fpf.maker.core.utils.key.IntentKey
import com.cat.emoji.fpf.maker.core.utils.key.RequestKey
import com.cat.emoji.fpf.maker.core.utils.state.HandleState
import com.cat.emoji.fpf.maker.databinding.ActivitySuccessBinding
import com.cat.emoji.fpf.maker.ui.home.HomeActivity
import com.cat.emoji.fpf.maker.ui.my_creation.MyCreationActivity
import com.cat.emoji.fpf.maker.ui.permission.PermissionViewModel
import kotlinx.coroutines.launch

class SuccessActivity : BaseActivity<ActivitySuccessBinding>() {
    private val viewModel: SuccessViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()
    private var tabIndex = -1

    override fun setViewBinding(): ActivitySuccessBinding {
        return ActivitySuccessBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        viewModel.setPath(intent.getStringExtra(IntentKey.INTENT_KEY) ?: "")
        tabIndex = intent.getIntExtra(IntentKey.TAB_INDEX_KEY, -1)
        setButtonBackgrounds()
    }

    private fun setButtonBackgrounds() {
        binding.includeLayoutBottom.apply {
            
            tvMyWork.select()
            tvDownload.select()
            tvShare.select()

        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pathInternal.collect { path ->
                        if (path.isNotEmpty()) {
                            loadImage(this@SuccessActivity, path, binding.imvImage)
                        }
                    }
                }
            }
        }
    }

    private fun handleBack() {
        handleBackLeftToRight()
    }
    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarRight.tap {
                    showInterAll {
                        startIntentWithClearTop(HomeActivity::class.java)
                    }
                }
                btnActionBarLeft.tap { showInterAll { handleBack() } }

            }

            // My Album button
            includeLayoutBottom.btnWhatsapp.tap(2590) {
                showInterAll {
                    val intent = android.content.Intent(this@SuccessActivity, MyCreationActivity::class.java)
                    intent.putExtra(IntentKey.FROM_SAVE, true)
                    if (tabIndex != -1) {
                        intent.putExtra(IntentKey.TAB_INDEX_KEY, tabIndex)
                    }
                    val options = android.app.ActivityOptions.makeCustomAnimation(
                        this@SuccessActivity,
                        R.anim.slide_out_left,
                        R.anim.slide_in_right
                    )
                    startActivity(intent, options.toBundle())
                }
            }

            // Download button
            includeLayoutBottom.btnTelegram.tap(2000) {
                checkStoragePermission()
            }
            includeLayoutBottom.btnShare.tap(2000){
                    viewModel.shareFiles(this@SuccessActivity)
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setTextActionBar(tvCenter, getString(R.string.successfully))
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            tvCenter.visible()
            imgCenter.gone()
                setImageActionBar(btnActionBarRight, R.drawable.ic_home_ss)
            btnActionBarNextRight.invisible()
            tvCenter.select()

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
            viewModel.downloadFiles(this@SuccessActivity).collect { state ->
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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
        handleBackLeftToRight()
    }
}
