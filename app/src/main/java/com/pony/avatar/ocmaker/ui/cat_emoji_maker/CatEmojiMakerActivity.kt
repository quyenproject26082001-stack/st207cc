package com.pony.avatar.ocmaker.ui.cat_emoji_maker

import android.view.LayoutInflater
import com.pony.avatar.ocmaker.R
import com.pony.avatar.ocmaker.core.base.BaseActivity
import com.pony.avatar.ocmaker.core.extensions.handleBackLeftToRight
import com.pony.avatar.ocmaker.core.extensions.select
import com.pony.avatar.ocmaker.core.extensions.setImageActionBar
import com.pony.avatar.ocmaker.core.extensions.setTextActionBar
import com.pony.avatar.ocmaker.core.extensions.showInterAll
import com.pony.avatar.ocmaker.core.extensions.startIntentRightToLeft
import com.pony.avatar.ocmaker.core.extensions.tap
import com.pony.avatar.ocmaker.core.utils.key.IntentKey
import com.pony.avatar.ocmaker.databinding.ActivityCatEmojiMakerBinding
import com.pony.avatar.ocmaker.ui.choose_character.ChooseCharacterActivity
import com.pony.avatar.ocmaker.ui.emoji_custom.EmojiCustomActivity

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
                startIntentRightToLeft(EmojiCustomActivity::class.java)
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
