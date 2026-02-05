package com.cat.emoji.fpf.maker.listener.listenerdraw

import android.view.MotionEvent
import com.cat.emoji.fpf.maker.core.custom.drawview.DrawView
import com.cat.emoji.fpf.maker.core.utils.key.DrawKey


class FlipEvent : DrawEvent {
    override fun onActionDown(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionMove(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionUp(tattooView: DrawView?, event: MotionEvent?) {
        if (tattooView != null && tattooView.getStickerCount() > 0) tattooView.flipCurrentDraw(
            DrawKey.FLIP_HORIZONTALLY)
    }
}