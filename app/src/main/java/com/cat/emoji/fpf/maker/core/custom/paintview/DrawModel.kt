package com.cat.emoji.fpf.maker.core.custom.paintview

import android.graphics.Path

data class DrawModel(
    val path: Path,
    val color: Int,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)
