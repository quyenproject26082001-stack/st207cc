package com.cat.emoji.fpf.maker.ui.intro

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import androidx.viewpager2.widget.ViewPager2
import com.lvt.ads.util.Admob
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseActivity
import com.cat.emoji.fpf.maker.core.extensions.gone
import com.cat.emoji.fpf.maker.core.utils.DataLocal
import com.cat.emoji.fpf.maker.databinding.ActivityIntroBinding
import com.cat.emoji.fpf.maker.ui.home.HomeActivity
import com.cat.emoji.fpf.maker.ui.permission.PermissionActivity
import com.cat.emoji.fpf.maker.core.extensions.tap
import com.cat.emoji.fpf.maker.core.extensions.visible
import kotlin.system.exitProcess

class IntroActivity : BaseActivity<ActivityIntroBinding>() {
    private val introAdapter by lazy { IntroAdapter(this) }

    override fun setViewBinding(): ActivityIntroBinding {
        return ActivityIntroBinding.inflate(LayoutInflater.from(this))
    }


    override fun initView() {
        initVpg()
    }


    override fun viewListener() {
        binding.btnNext.tap { handleNext() }

        binding.vpgTutorial.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 1) {
                   // binding.nativeAds.gone()
                } else {
                   // binding.nativeAds.visible()
                }
            }
        })
    }

    override fun initText() {}

    override fun initActionBar() {}

    private fun initVpg() {
        binding.apply {
            binding.vpgTutorial.adapter = introAdapter
            binding.dotsIndicator.attachTo(binding.vpgTutorial)
            introAdapter.submitList(DataLocal.itemIntroList)
        }
    }

    private fun handleNext() {
        binding.apply {
            val nextItem = binding.vpgTutorial.currentItem + 1
            if (nextItem < DataLocal.itemIntroList.size) {
                vpgTutorial.setCurrentItem(nextItem, true)
            } else {
                val intent =
                    if (sharePreference.getIsFirstPermission()) {
                        Intent(this@IntroActivity, PermissionActivity::class.java)
                    } else {
                        Intent(this@IntroActivity, HomeActivity::class.java)
                    }
                startActivity(intent)
                finishAffinity()
            }
        }
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() { exitProcess(0) }

    override fun shouldPlayBackgroundMusic(): Boolean = false

//    override fun initAds() {
//        Admob.getInstance().loadNativeAd(this, getString(R.string.native_intro), binding.nativeAds, R.layout.ads_native_medium_btn_bottom)
//    }
}