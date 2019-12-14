/*
 * Copyright 2019 Looping Layout
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.bekawestberg.loopinglayout.library

import android.view.View
import kotlin.math.abs

/**
 * @return A view with the given adapter index. If there are multiple views associated with the
 * given index, this returns the view closest to the anchor edge.
 *
 * The anchor edge is the edge the view associated with index 0 would be initially laid out
 * against. For example: In a RTL horizontal layout, the anchor edge would be the right edge.
 */
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

/**
 * @return A view with the given adapter index. If there are multiple views associated with the
 * given index, this returns the view whose middle is closest to the middle of the layout.
 */
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
