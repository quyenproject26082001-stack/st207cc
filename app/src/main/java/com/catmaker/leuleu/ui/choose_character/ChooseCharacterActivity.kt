package com.catmaker.leuleu.ui.choose_character

import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.event.AdmobEvent
import com.lvt.ads.util.Admob
import com.catmaker.leuleu.R
import com.catmaker.leuleu.ui.customize.CustomizeCharacterActivity
import com.catmaker.leuleu.ui.home.DataViewModel
import com.catmaker.leuleu.ui.random_character.RandomCharacterActivity
import com.catmaker.leuleu.core.base.BaseActivity
import com.catmaker.leuleu.core.extensions.handleBackLeftToRight
import com.catmaker.leuleu.core.extensions.hideNavigation
import com.catmaker.leuleu.core.extensions.loadNativeCollabAds
import com.catmaker.leuleu.core.extensions.select
import com.catmaker.leuleu.core.extensions.setImageActionBar
import com.catmaker.leuleu.core.extensions.setTextActionBar
import com.catmaker.leuleu.core.extensions.showInterAll
import com.catmaker.leuleu.core.extensions.tap
import com.catmaker.leuleu.core.extensions.startIntentRightToLeft
import com.catmaker.leuleu.core.extensions.visible
import com.catmaker.leuleu.core.helper.InternetHelper
import com.catmaker.leuleu.core.utils.key.IntentKey
import com.catmaker.leuleu.core.utils.key.ValueKey
import com.catmaker.leuleu.core.utils.state.HandleState
import com.catmaker.leuleu.databinding.ActivityChooseCharacterBinding
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

    private fun checkInternetForAPICharacters(data: ArrayList<com.catmaker.leuleu.data.model.custom.CustomizeModel>) {
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
                    val dialog = com.catmaker.leuleu.dialog.YesNoDialog(
                        this@ChooseCharacterActivity,
                        R.string.notification,
                        R.string.internet_required_for_more_characters,
                        isError = true,  // Shows only OK button
                        dialogType = com.catmaker.leuleu.dialog.DialogType.INTERNET
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

            if (needsInternet) {
                android.util.Log.d("ChooseCharacter", "API character - checking internet...")
                InternetHelper.checkInternet(this) { state ->
                    if (state == HandleState.SUCCESS) {
                        showInterAll { navigateToCustomize(position) }
                    } else {
                        // Show No Internet dialog
                        val dialog = com.catmaker.leuleu.dialog.YesNoDialog(
                            this@ChooseCharacterActivity,
                            R.string.no_internet,
                            R.string.please_check_your_internet,
                            isError = true,
                            dialogType = com.catmaker.leuleu.dialog.DialogType.INTERNET
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
            setTextActionBar(tvCenter, getString(R.string.pony_maker))
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
        // loadNativeCollabAds(R.string.native_cl_category, binding.flNativeCollab, binding.rcvCharacter)
    }

//    override fun initAds() {
//        initNativeCollab()
//        Admob.getInstance().loadNativeAd(
//            this,
//            getString(R.string.native_category),
//            binding.nativeAds,
//            R.layout.ads_native_banner
//        )
//    }

    override fun onRestart() {
        super.onRestart()
        initNativeCollab()
    }

}