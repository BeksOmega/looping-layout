package com.bekawestberg.loopinglayout.library

import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class LoopingSnapHelper : LinearSnapHelper() {
    override fun findTargetSnapPosition(
            layoutManager: RecyclerView.LayoutManager?,
            velocityX: Int,
            velocityY: Int
    ): Int {
        return RecyclerView.NO_POSITION;
    }
}