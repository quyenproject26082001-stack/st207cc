package com.cat.emoji.fpf.maker.dialog

import android.app.Activity
import android.graphics.drawable.AnimationDrawable
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseDialog
import com.cat.emoji.fpf.maker.core.extensions.setBackgroundConnerSmooth
import com.cat.emoji.fpf.maker.databinding.DialogLoadingBinding

class WaitingDialog(val context: Activity) :
    BaseDialog<DialogLoadingBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_loading
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    override fun initView() {
        // Start loading animation for dot

    }

    override fun initAction() {}

    override fun onDismissListener() {}

}