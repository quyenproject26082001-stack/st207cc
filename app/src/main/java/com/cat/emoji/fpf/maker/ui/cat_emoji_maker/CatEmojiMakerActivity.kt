package com.cat.emoji.fpf.maker.ui.cat_emoji_maker

import android.view.LayoutInflater
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseActivity
import com.cat.emoji.fpf.maker.core.extensions.handleBackLeftToRight
import com.cat.emoji.fpf.maker.core.extensions.select
import com.cat.emoji.fpf.maker.core.extensions.setImageActionBar
import com.cat.emoji.fpf.maker.core.extensions.setTextActionBar
import com.cat.emoji.fpf.maker.core.extensions.showInterAll
import com.cat.emoji.fpf.maker.core.extensions.startIntentRightToLeft
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.core.utils.key.IntentKey
import com.cat.emoji.fpf.maker.databinding.ActivityCatEmojiMakerBinding
import com.cat.emoji.fpf.maker.ui.choose_character.ChooseCharacterActivity
import com.cat.emoji.fpf.maker.ui.emoji_custom.EmojiCustomActivity

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
            setTextActionBar(tvCenter, getString(R.string.cat_emoji_maker))
            tvCenter.select()
        }
    }
}
