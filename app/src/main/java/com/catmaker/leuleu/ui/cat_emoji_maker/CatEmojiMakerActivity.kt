package com.catmaker.leuleu.ui.cat_emoji_maker

import android.view.LayoutInflater
import com.catmaker.leuleu.R
import com.catmaker.leuleu.core.base.BaseActivity
import com.catmaker.leuleu.core.extensions.handleBackLeftToRight
import com.catmaker.leuleu.core.extensions.select
import com.catmaker.leuleu.core.extensions.setImageActionBar
import com.catmaker.leuleu.core.extensions.setTextActionBar
import com.catmaker.leuleu.core.extensions.showInterAll
import com.catmaker.leuleu.core.extensions.startIntentRightToLeft
import com.catmaker.leuleu.core.extensions.tap
import com.catmaker.leuleu.core.utils.key.IntentKey
import com.catmaker.leuleu.databinding.ActivityCatEmojiMakerBinding
import com.catmaker.leuleu.ui.choose_character.ChooseCharacterActivity
import com.catmaker.leuleu.ui.emoji_custom.EmojiCustomActivity

class CatEmojiMakerActivity : BaseActivity<ActivityCatEmojiMakerBinding>() {

    override fun setViewBinding(): ActivityCatEmojiMakerBinding {
        return ActivityCatEmojiMakerBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        binding.tv1.isSelected = true
        binding.tvEmojiCustom.isSelected = true
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { showInterAll { handleBackLeftToRight() } }
            btnCatMaker.tap(800) {
                startIntentRightToLeft(ChooseCharacterActivity::class.java, IntentKey.DATA_TYPE_KEY, IntentKey.DATA_TYPE_CAT)
            }
            btnEmojiCat.tap(800) {
                startIntentRightToLeft(ChooseCharacterActivity::class.java, IntentKey.DATA_TYPE_KEY, IntentKey.DATA_TYPE_EMOJI)
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
}
