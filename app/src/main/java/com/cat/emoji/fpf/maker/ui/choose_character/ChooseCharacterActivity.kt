package com.cat.emoji.fpf.maker.ui.choose_character

import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.event.AdmobEvent
import com.lvt.ads.util.Admob
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.ui.customize.CustomizeCharacterActivity
import com.cat.emoji.fpf.maker.ui.home.DataViewModel
import com.cat.emoji.fpf.maker.ui.random_character.RandomCharacterActivity
import com.cat.emoji.fpf.maker.core.base.BaseActivity
import com.cat.emoji.fpf.maker.core.extensions.handleBackLeftToRight
import com.cat.emoji.fpf.maker.core.extensions.hideNavigation
import com.cat.emoji.fpf.maker.core.extensions.loadNativeCollabAds
import com.cat.emoji.fpf.maker.core.extensions.select
import com.cat.emoji.fpf.maker.core.extensions.setImageActionBar
import com.cat.emoji.fpf.maker.core.extensions.setTextActionBar
import com.cat.emoji.fpf.maker.core.extensions.showInterAll
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.core.extensions.startIntentRightToLeft
import com.cat.emoji.fpf.maker.core.extensions.visible
import com.cat.emoji.fpf.maker.core.helper.InternetHelper
import com.cat.emoji.fpf.maker.core.utils.key.IntentKey
import com.cat.emoji.fpf.maker.core.utils.key.ValueKey
import com.cat.emoji.fpf.maker.core.utils.state.HandleState
import com.cat.emoji.fpf.maker.databinding.ActivityChooseCharacterBinding
import kotlinx.coroutines.launch

class ChooseCharacterActivity : BaseActivity<ActivityChooseCharacterBinding>() {
    private val viewModel: ChooseCharacterViewModel by viewModels()
    private val dataViewModel: DataViewModel by viewModels()
    private val chooseCharacterAdapter by lazy { ChooseCharacterAdapter() }
    private var hasCheckedInternet = false  // Flag to check internet only once
    private var currentDataType = IntentKey.DATA_TYPE_DEFAULT

    override fun setViewBinding(): ActivityChooseCharacterBinding {
        return ActivityChooseCharacterBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Show loading when activity starts
        lifecycleScope.launch {
            showLoading()
        }
        initRcv()

        // Check if dataType is passed (Cat/Emoji)
        currentDataType = intent.getIntExtra(IntentKey.DATA_TYPE_KEY, IntentKey.DATA_TYPE_DEFAULT)
        if (currentDataType != IntentKey.DATA_TYPE_DEFAULT) {
            dataViewModel.loadDataByType(this, currentDataType)
        } else {
            dataViewModel.ensureData(this)
        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            dataViewModel.allData.collect { data ->
                if (data.isNotEmpty()) {
                    chooseCharacterAdapter.submitList(data)

                    // Dismiss loading when data is loaded
                    dismissLoading()

                    // Check if there are API characters and user has no internet
                    checkInternetForAPICharacters(data)
                }
            }
        }
    }

    private fun checkInternetForAPICharacters(data: ArrayList<com.cat.emoji.fpf.maker.data.model.custom.CustomizeModel>) {
        // Only check once per activity lifecycle
        if (hasCheckedInternet) return
        hasCheckedInternet = true

        android.util.Log.d("ChooseCharacter", "========================================")
        android.util.Log.d("ChooseCharacter", "checkInternetForAPICharacters called")
        android.util.Log.d("ChooseCharacter", "Total characters in data: ${data.size}")

        // Check if API characters are already loaded
        val hasAPICharacters = data.any { it.isFromAPI }
        val apiCount = data.count { it.isFromAPI }
        val localCount = data.count { !it.isFromAPI }

        android.util.Log.d("ChooseCharacter", "API characters: $apiCount")
        android.util.Log.d("ChooseCharacter", "Local characters: $localCount")
        android.util.Log.d("ChooseCharacter", "hasAPICharacters: $hasAPICharacters")

        // Only show notification if API characters are NOT loaded yet
        if (!hasAPICharacters) {
            android.util.Log.d("ChooseCharacter", "No API characters - checking internet...")
            InternetHelper.checkInternet(this) { state ->
                android.util.Log.d("ChooseCharacter", "Internet check result: $state")
                if (state != HandleState.SUCCESS) {
                    android.util.Log.d("ChooseCharacter", "❌ No internet - SHOWING DIALOG")
                    // No internet and no API characters loaded - notify user
                    val dialog = com.cat.emoji.fpf.maker.dialog.YesNoDialog(
                        this@ChooseCharacterActivity,
                        R.string.notification,
                        R.string.internet_required_for_more_characters,
                        isError = true,  // Shows only OK button
                        dialogType = com.cat.emoji.fpf.maker.dialog.DialogType.INTERNET
                    )
                    dialog.show()
                    dialog.onYesClick = {
                        dialog.dismiss()
                        hideNavigation()
                    }
                } else {
                    android.util.Log.d("ChooseCharacter", "✓ Has internet - no dialog")
                }
            }
        } else {
            android.util.Log.d("ChooseCharacter", "✓ API characters already loaded - no dialog")
        }
        android.util.Log.d("ChooseCharacter", "========================================")
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { showInterAll { handleBackLeftToRight() } }
        }
        chooseCharacterAdapter.onItemClick = { position ->
            AdmobEvent.logEvent(this@ChooseCharacterActivity, "click_item_$position", null)

            android.util.Log.d("ChooseCharacter", "========================================")
            android.util.Log.d("ChooseCharacter", "Item clicked: position $position")

            // ✅ FIX: Use isFromAPI flag from character data instead of position
            val selectedCharacter = dataViewModel.allData.value.getOrNull(position)
            val needsInternet = selectedCharacter?.isFromAPI ?: false

            android.util.Log.d("ChooseCharacter", "Character isFromAPI: $needsInternet")
            android.util.Log.d("ChooseCharacter", "Character name: ${selectedCharacter?.dataName}")
            android.util.Log.d("ChooseCharacter", "Character dataType: ${selectedCharacter?.dataType}")
            android.util.Log.d("ChooseCharacter", "Avatar URL: ${selectedCharacter?.avatar}")
            selectedCharacter?.layerList?.forEachIndexed { index, layer ->
                android.util.Log.d("ChooseCharacter", "Layer[$index] nav: ${layer.imageNavigation}")
                layer.layer.firstOrNull()?.let {
                    android.util.Log.d("ChooseCharacter", "Layer[$index] image: ${it.image}")
                }
            }
            android.util.Log.d("ChooseCharacter", "========================================")

            if (needsInternet) {
                android.util.Log.d("ChooseCharacter", "API character - checking internet...")
                InternetHelper.checkInternet(this) { state ->
                    if (state == HandleState.SUCCESS) {
                        showInterAll { navigateToCustomize(position) }
                    } else {
                        // Show No Internet dialog
                        val dialog = com.cat.emoji.fpf.maker.dialog.YesNoDialog(
                            this@ChooseCharacterActivity,
                            R.string.no_internet,
                            R.string.please_check_your_internet,
                            isError = true,
                            dialogType = com.cat.emoji.fpf.maker.dialog.DialogType.INTERNET
                        )
                        dialog.show()
                        dialog.onYesClick = {
                            dialog.dismiss()
                            hideNavigation()
                        }
                    }
                }
            } else {
                android.util.Log.d("ChooseCharacter", "Local character - navigating directly")
                android.util.Log.d("ChooseCharacter", "========================================")
                showInterAll { navigateToCustomize(position) }
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            val title = when (currentDataType) {
                IntentKey.DATA_TYPE_CAT -> getString(R.string.cat_maker)
                IntentKey.DATA_TYPE_EMOJI -> getString(R.string.emoji_maker)
                else -> getString(R.string.pony_maker)
            }
            setTextActionBar(tvCenter, title)
            tvCenter.select()
        }
    }

    private fun initRcv() {
        binding.rcvCharacter.apply {
            adapter = chooseCharacterAdapter
            itemAnimator = null
        }
    }

    private fun navigateToCustomize(position: Int) {
        val intent = android.content.Intent(this, CustomizeCharacterActivity::class.java)
        intent.putExtra(IntentKey.INTENT_KEY, position)
        intent.putExtra(IntentKey.DATA_TYPE_KEY, currentDataType)
        val option = android.app.ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
        startActivity(intent, option.toBundle())
    }

    fun initNativeCollab() {
        Admob.getInstance().loadNativeCollapNotBanner(this,getString(R.string.native_cl_categoryMaker), binding.flNativeCollab)
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

    override fun onRestart() {
        super.onRestart()
        initNativeCollab()
    }

}