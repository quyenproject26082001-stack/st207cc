package com.catmaker.leuleu.dialog

import android.app.Activity
import android.graphics.drawable.AnimationDrawable
import com.catmaker.leuleu.R
import com.catmaker.leuleu.core.base.BaseDialog
import com.catmaker.leuleu.core.extensions.setBackgroundConnerSmooth
import com.catmaker.leuleu.databinding.DialogLoadingBinding

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