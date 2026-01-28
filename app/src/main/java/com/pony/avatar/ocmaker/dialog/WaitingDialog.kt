package com.pony.avatar.ocmaker.dialog

import android.app.Activity
import android.graphics.drawable.AnimationDrawable
import com.pony.avatar.ocmaker.R
import com.pony.avatar.ocmaker.core.base.BaseDialog
import com.pony.avatar.ocmaker.core.extensions.setBackgroundConnerSmooth
import com.pony.avatar.ocmaker.databinding.DialogLoadingBinding

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