package com.catmaker.leuleu.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.util.Admob
import com.catmaker.leuleu.R
import com.catmaker.leuleu.core.base.BaseActivity
import com.catmaker.leuleu.core.extensions.hideNavigation
import com.catmaker.leuleu.core.extensions.loadNativeCollabAds
import com.catmaker.leuleu.core.extensions.rateApp
import com.catmaker.leuleu.core.extensions.select
import com.catmaker.leuleu.core.extensions.setImageActionBar
import com.catmaker.leuleu.core.extensions.showInterAll
import com.catmaker.leuleu.core.extensions.startIntentRightToLeft
import com.catmaker.leuleu.core.helper.LanguageHelper
import com.catmaker.leuleu.core.helper.MediaHelper
import com.catmaker.leuleu.core.utils.key.ValueKey
import com.catmaker.leuleu.core.utils.state.RateState
import com.catmaker.leuleu.databinding.ActivityHomeBinding
import com.catmaker.leuleu.ui.SettingsActivity
import com.catmaker.leuleu.ui.my_creation.MyCreationActivity
import com.catmaker.leuleu.ui.cat_emoji_maker.CatEmojiMakerActivity
import com.catmaker.leuleu.core.extensions.tap
import com.catmaker.leuleu.core.extensions.strings
import com.catmaker.leuleu.ui.random_character.RandomCharacterActivity
import com.catmaker.leuleu.ui.emoji_custom.EmojiCustomActivity
import com.catmaker.leuleu.ui.emoji_sticker.EmojiStickerActivity
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
            btnEmojiCustom.tap(800) { startIntentRightToLeft(EmojiCustomActivity::class.java) }
            btnStickers.tap(800) { startIntentRightToLeft(EmojiStickerActivity::class.java) }
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
            tv1.text = strings(R.string.pony_maker)
            tv2.text = strings(R.string.trending)
            tv3.text = strings(R.string.my_work)
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
        binding.btnQuickMaker.postDelayed({
            binding.btnQuickMaker.startAnimation(slideFromLeft)
            binding.tv2.startAnimation(slideFromLeft)
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