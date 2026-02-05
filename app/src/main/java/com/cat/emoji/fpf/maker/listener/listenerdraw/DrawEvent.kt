package com.cat.emoji.fpf.maker.listener.listenerdraw

import android.view.MotionEvent
import com.cat.emoji.fpf.maker.core.custom.drawview.DrawView


interface DrawEvent {
    fun onActionDown(tattooView: DrawView?, event: MotionEvent?)
    fun onActionMove(tattooView: DrawView?, event: MotionEvent?)
    fun onActionUp(tattooView: DrawView?, event: MotionEvent?)
}