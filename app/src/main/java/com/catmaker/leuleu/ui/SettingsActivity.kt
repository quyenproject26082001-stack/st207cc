package com.catmaker.leuleu.ui

import android.view.LayoutInflater
import com.catmaker.leuleu.R
import com.catmaker.leuleu.core.base.BaseActivity
import com.catmaker.leuleu.core.extensions.gone
import com.catmaker.leuleu.core.extensions.handleBackLeftToRight
import com.catmaker.leuleu.core.extensions.policy
import com.catmaker.leuleu.core.extensions.select
import com.catmaker.leuleu.core.extensions.setImageActionBar
import com.catmaker.leuleu.core.extensions.setTextActionBar
import com.catmaker.leuleu.core.extensions.shareApp
import com.catmaker.leuleu.core.extensions.startIntentRightToLeft
import com.catmaker.leuleu.core.extensions.visible
import com.catmaker.leuleu.core.utils.key.IntentKey
import com.catmaker.leuleu.core.utils.state.RateState
import com.catmaker.leuleu.databinding.ActivitySettingsBinding
import com.catmaker.leuleu.ui.language.LanguageActivity
import com.catmaker.leuleu.core.extensions.tap
import com.catmaker.leuleu.core.helper.MusicHelper
import com.catmaker.leuleu.core.helper.RateHelper
import kotlin.jvm.java

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {
    override fun setViewBinding(): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        binding.tvMusic.select()
        initRate()
        initMusic()
    }

    private fun initMusic() {
        updateMusicUI(sharePreference.isMusicEnabled())
    }

    private fun updateMusicUI(isEnabled: Boolean) {
        binding.btnMusic.setImageResource(
            if (isEnabled) R.drawable.ic_sw_on else R.drawable.ic_sw_off
        )
    }

    private fun toggleMusic() {
        val isEnabled = !sharePreference.isMusicEnabled()
        sharePreference.setMusicEnabled(!isEnabled)
        updateMusicUI(isEnabled)
        if (isEnabled) {
            MusicHelper.play()
        } else {
            MusicHelper.pause()
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { handleBackLeftToRight() }
            layoutMusic.tap { toggleMusic() }
            btnLang.tap { startIntentRightToLeft(LanguageActivity::class.java, IntentKey.INTENT_KEY) }
            btnShareApp.tap(1500) { shareApp() }
            btnRate.tap {
                RateHelper.showRateDialog(this@SettingsActivity, sharePreference){ state ->
                    if (state != RateState.CANCEL){
                        btnRate.gone()
                        showToast(R.string.have_rated)
                    }
                }
            }
            btnPolicy.tap(1500) { policy() }
        }
    }

    override fun initText() {
        binding.actionBar.tvCenter.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, getString(R.string.settings))
        }
    }

    private fun initRate() {
        if (sharePreference.getIsRate(this)) {
            binding.btnRate.gone()
        } else {
            binding.btnRate.visible()
        }
    }
}