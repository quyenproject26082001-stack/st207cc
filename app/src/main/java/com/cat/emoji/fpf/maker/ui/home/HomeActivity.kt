package com.cat.emoji.fpf.maker.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.util.Admob
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseActivity
import com.cat.emoji.fpf.maker.core.extensions.hideNavigation
import com.cat.emoji.fpf.maker.core.extensions.loadNativeCollabAds
import com.cat.emoji.fpf.maker.core.extensions.rateApp
import com.cat.emoji.fpf.maker.core.extensions.select
import com.cat.emoji.fpf.maker.core.extensions.setImageActionBar
import com.cat.emoji.fpf.maker.core.extensions.showInterAll
import com.cat.emoji.fpf.maker.core.extensions.startIntentRightToLeft
import com.cat.emoji.fpf.maker.core.helper.InternetHelper
import com.cat.emoji.fpf.maker.core.helper.LanguageHelper
import com.cat.emoji.fpf.maker.core.helper.MediaHelper
import com.cat.emoji.fpf.maker.dialog.DialogType
import com.cat.emoji.fpf.maker.dialog.YesNoDialog
import com.cat.emoji.fpf.maker.core.utils.key.ValueKey
import com.cat.emoji.fpf.maker.core.utils.state.RateState
import com.cat.emoji.fpf.maker.databinding.ActivityHomeBinding
import com.cat.emoji.fpf.maker.ui.SettingsActivity
import com.cat.emoji.fpf.maker.ui.my_creation.MyCreationActivity
import com.cat.emoji.fpf.maker.ui.cat_emoji_maker.CatEmojiMakerActivity
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.core.extensions.strings
import com.cat.emoji.fpf.maker.ui.random_character.RandomCharacterActivity
import com.cat.emoji.fpf.maker.ui.emoji_custom.EmojiCustomActivity
import com.cat.emoji.fpf.maker.ui.emoji_sticker.EmojiStickerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    override fun setViewBinding(): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        sharePreference.setCountBack(sharePreference.getCountBack() + 1)
        deleteTempFolder()
        binding.tv1.isSelected = true
        binding.tv3.isSelected = true
        binding.tv2.isSelected = true
        binding.tvEmojiCustom.isSelected = true
        binding.tvStickers.isSelected = true

        // Apply elastic bounce animation to app name
        val elasticBounce = AnimationUtils.loadAnimation(this, R.anim.elastic_bounce)
        binding.imvAppName.startAnimation(elasticBounce)
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarRight.tap(800) { startIntentRightToLeft(SettingsActivity::class.java) }
            btnCreate.tap(800) { startIntentRightToLeft(CatEmojiMakerActivity::class.java) }
            btnMyAlbum.tap(800) { showInterAll { startIntentRightToLeft(MyCreationActivity::class.java) } }
            btnQuickMaker.tap(800) { startIntentRightToLeft(RandomCharacterActivity::class.java) }
            btnEmojiCustom.tap(800) {
                if (InternetHelper.isInternetAvailable(this@HomeActivity)) {
                    startIntentRightToLeft(EmojiCustomActivity::class.java)
                } else {
                    val dialog = YesNoDialog(
                        this@HomeActivity,
                        R.string.no_internet,
                        R.string.please_check_your_internet,
                        isError = true,
                        dialogType = DialogType.INTERNET
                    )
                    dialog.show()
                    dialog.onYesClick = { dialog.dismiss() }
                }
            }
            btnStickers.tap(800) {
                if (InternetHelper.isInternetAvailable(this@HomeActivity)) {
                    startIntentRightToLeft(EmojiStickerActivity::class.java)
                } else {
                    val dialog = YesNoDialog(
                        this@HomeActivity,
                        R.string.no_internet,
                        R.string.please_check_your_internet,
                        isError = true,
                        dialogType = DialogType.INTERNET
                    )
                    dialog.show()
                    dialog.onYesClick = { dialog.dismiss() }
                }
            }
        }
    }

    override fun initText() {
        super.initText()
        binding.actionBar.tvCenter.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarRight, R.drawable.ic_settings)
        }
    }

    // Enable background music for HomeActivity

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        if (!sharePreference.getIsRate(this) && sharePreference.getCountBack() % 2 == 0) {
            rateApp(sharePreference) { state ->
                if (state != RateState.CANCEL) {
                    showToast(R.string.have_rated)
                }
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        delay(1000)
                        exitProcess(0)
                    }
                }
            }
        } else {
            exitProcess(0)
        }
    }

    private fun deleteTempFolder() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dataTemp = MediaHelper.getImageInternal(this@HomeActivity, ValueKey.RANDOM_TEMP_ALBUM)
            if (dataTemp.isNotEmpty()) {
                dataTemp.forEach {
                    val file = File(it)
                    file.delete()
                }
            }
        }
    }

    private fun updateText() {
        binding.apply {
            tv1.text = strings(R.string.cat_emoji_maker)
            tv2.text = strings(R.string.quick_maker)
            tv3.text = strings(R.string.my_work)
            tvStickers.text = strings(R.string.cat_sticker)
            tvEmojiCustom.text = strings(R.string.cat_emoji_customizer)
        }
    }

    override fun onRestart() {
        super.onRestart()
        deleteTempFolder()
        LanguageHelper.setLocale(this)
        updateText()
        //initNativeCollab()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
        startStaggeredAnimations()

        }
    }

    private fun startStaggeredAnimations() {
        // Card 1: Slide from right (no delay)
        val slideFromRight1 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right_home)
        binding.btnCreate.startAnimation(slideFromRight1)
        binding.tv1.startAnimation(slideFromRight1)


        // Card 2: Slide from left (200ms delay)
        val slideFromLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_home)
//        binding.btnQuickMaker.postDelayed({
//            binding.btnQuickMaker.startAnimation(slideFromLeft)
//            binding.tv2.startAnimation(slideFromLeft)
//        }, 200)

        // Card 2: Slide from left (200ms delay)
        binding.btnEmojiCustom.postDelayed({
            binding.btnEmojiCustom.startAnimation(slideFromLeft)
            binding.tvEmojiCustom.startAnimation(slideFromLeft)
        }, 200)


        // Card 2: Slide from left (200ms delay)
        binding.btnEmojiCustom.postDelayed({
            binding.btnStickers.startAnimation(slideFromLeft)
            binding.tvStickers.startAnimation(slideFromLeft)
        }, 200)


        // Card 3: Slide from right (400ms delay)
        val slideFromRight2 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right_home)
        binding.btnMyAlbum.postDelayed({
            binding.btnMyAlbum.startAnimation(slideFromRight2)
            binding.tv3.startAnimation(slideFromRight2)
        }, 400)
    }

//    fun initNativeCollab() {
//        loadNativeCollabAds(R.string.native_cl_home, binding.flNativeCollab, binding.scvMain)
//    }

//    override fun initAds() {
//        initNativeCollab()
//        Admob.getInstance().loadInterAll(this, getString(R.string.inter_all))
//        Admob.getInstance().loadNativeAll(this, getString(R.string.native_all))
//    }
}