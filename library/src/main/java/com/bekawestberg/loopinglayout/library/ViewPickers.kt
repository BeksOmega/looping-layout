package com.bekawestberg.loopinglayout.library

import android.view.View
import kotlin.math.abs

fun childClosestToAnchorEdge(
        targetAdapterIndex: Int,
        layoutManager: LoopingLayoutManager
): View? {
    val direction = layoutManager.convertAdapterDirToMovementDir(
            LoopingLayoutManager.TOWARDS_HIGHER_INDICES)
    val range = if (direction == LoopingLayoutManager.TOWARDS_BOTTOM_RIGHT) {
        0 until layoutManager.childCount
    } else {
        layoutManager.childCount-1 downTo 0
    }

    for (i in range) {
        val view = layoutManager.getChildAt(i) ?: break
        if (layoutManager.getPosition(view) == targetAdapterIndex) {
            return view
        }
    }
    return null
}

fun childClosestToMiddle(
        targetAdapterIndex: Int,
        layoutManager: LoopingLayoutManager
): View? {
    var minDistance = Int.MAX_VALUE
    var closestView: View? = null
    val layoutMiddle = if (layoutManager.orientation == LoopingLayoutManager.HORIZONTAL) {
        layoutManager.paddingLeft + (layoutManager.width / 2)
    } else {
        layoutManager.paddingTop + (layoutManager.height / 2)
    }
    for (i in 0 until layoutManager.childCount) {
        val view = layoutManager.getChildAt(i) ?: return null
        if (layoutManager.getPosition(view) != targetAdapterIndex) {
            continue
        }
        val childMiddle = if (layoutManager.orientation == LoopingLayoutManager.HORIZONTAL) {
            layoutManager.getDecoratedLeft(view) +
                    (layoutManager.getDecoratedMeasuredWidth(view) / 2)
        } else {
            layoutManager.getDecoratedTop(view) +
                    (layoutManager.getDecoratedMeasuredHeight(view) / 2)
        }
        val distance = abs(childMiddle - layoutMiddle)
        if (distance < minDistance) {
            minDistance = distance
            closestView = view
        }
    }
    return closestView
}
