package com.cat.emoji.fpf.maker.ui.intro

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
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
import kotlin.text.toInt

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
                val params = binding.btnNext.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

                if (position == 0) {
                    binding.nativeAds.gone()
                    // Set lại width về 100dp
                    params.width = (140 * resources.displayMetrics.density).toInt()  // Chiều rộng
                    params.height = (45 * resources.displayMetrics.density).toInt() // Chiều cao
                    binding.btnNext.layoutParams = params
                    binding.btnNext.setTextColor(resources.getColor(R.color.white, null))
                    binding.btnNext.setBackgroundResource(R.drawable.bg_next_button)
                } else {
                    binding.nativeAds.visible()
                    // Đổi sang wrap_content
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    binding.btnNext.layoutParams = params
                    binding.btnNext.setTextColor(Color.parseColor("#7AAB36"))
                    binding.btnNext.background = null
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

    override fun initAds() {
        Admob.getInstance().loadNativeAd(this, getString(R.string.native_intro), binding.nativeAds, R.layout.ads_native_medium_btn_bottom)
    }
}