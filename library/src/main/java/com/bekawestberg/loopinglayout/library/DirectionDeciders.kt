package com.bekawestberg.loopinglayout.library

import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The default decider used when one is not provided.
 * @return A movement direction that should be used to "scroll" to the given adapter index.
 */
public fun defaultDecider(
        adapterIndex: Int,
        layoutManager: LoopingLayoutManager,
        itemCount: Int
): Int {
    return estimateShortestRoute(adapterIndex, layoutManager, itemCount)
}

/**
 * Returns a movement direction that should be used to "scroll" to the given [adapterIndex].
 * This function always returns the direction associated with creating views at the
 * anchor edge. The anchor edge being the edge the 0 indexed view was aligned with when
 * the recycler was initially laid out.
 */
public fun addViewsAtAnchorEdge(
        adapterIndex: Int,
        layoutManager: LoopingLayoutManager,
        itemCount: Int
): Int {
    return layoutManager.convertAdapterDirToMovementDir(LoopingLayoutManager.TOWARDS_LOWER_INDICES)
}

/**
 * Returns a movement direction that should be used to "scroll" to the given [adapterIndex].
 * This function always returns the direction associated with creating views at the edge
 * opposite the anchor edge. The anchor edge being the edge the 0 indexed view was aligned with when
 * the recycler was initially laid out.
 */
public fun addViewsAtOptAnchorEdge(
        adapterIndex: Int,
        layoutManager: LoopingLayoutManager,
        itemCount: Int
): Int {
    return layoutManager.convertAdapterDirToMovementDir(LoopingLayoutManager.TOWARDS_HIGHER_INDICES)
}

/**
 * Returns a movement direction that should be used to "scroll" to the given [adapterIndex].
 * This function estimates which direction puts the view on screen with the least amount
 * of scrolling. It is an estimation because the function assumes all views are the same
 * size. If some views are larger or smaller than others, this may not return the correct
 * direction.
 */
public fun estimateShortestRoute(
        adapterIndex: Int,
        layoutManager: LoopingLayoutManager,
        itemCount: Int
): Int {
    // Special case the view being partially visible.
    if (layoutManager.topLeftIndex == adapterIndex) {
        return LoopingLayoutManager.TOWARDS_TOP_LEFT
    } else if (layoutManager.bottomRightIndex == adapterIndex) {
        return LoopingLayoutManager.TOWARDS_BOTTOM_RIGHT
    }

    val (topLeftInLoopDist, topLeftOverSeamDist) = calculateDistances(
            adapterIndex, layoutManager.topLeftIndex, itemCount)
    val topLeftTargetSmaller = adapterIndex < layoutManager.topLeftIndex

    val (bottomRightInLoopDist, bottomRightOverSeamDist) = calculateDistances(
            adapterIndex, layoutManager.bottomRightIndex, itemCount)
    val bottomRightTargetSmaller = adapterIndex < layoutManager.bottomRightIndex

    val minDist = arrayOf(topLeftInLoopDist, topLeftOverSeamDist,
            bottomRightInLoopDist, bottomRightOverSeamDist).min()
    val minDistIsInLoop = when(minDist) {
        topLeftInLoopDist, bottomRightInLoopDist -> true
        topLeftOverSeamDist, bottomRightOverSeamDist -> false
        else -> throw IllegalStateException()  // Should never happen.
    }
    val minDistIsOverSeam = !minDistIsInLoop
    val targetIsSmaller = when(minDist) {
        topLeftInLoopDist, topLeftOverSeamDist -> topLeftTargetSmaller
        bottomRightInLoopDist, bottomRightOverSeamDist -> bottomRightTargetSmaller
        else -> throw IllegalStateException()  // Should never happen.
    }
    val targetIsLarger = !targetIsSmaller

    val adapterDir = when {
        targetIsSmaller && minDistIsInLoop -> LoopingLayoutManager.TOWARDS_LOWER_INDICES
        targetIsSmaller && minDistIsOverSeam -> LoopingLayoutManager.TOWARDS_HIGHER_INDICES
        targetIsLarger && minDistIsInLoop -> LoopingLayoutManager.TOWARDS_HIGHER_INDICES
        targetIsLarger && minDistIsOverSeam -> LoopingLayoutManager.TOWARDS_LOWER_INDICES
        else -> throw IllegalStateException()  // Should never happen.
    }
    return layoutManager.convertAdapterDirToMovementDir(adapterDir)
}

internal fun calculateDistances(adapterIndex: Int, anchorIndex: Int, count: Int): Pair<Int, Int> {
    val inLoopDist = abs(adapterIndex - anchorIndex)
    val smallerIndex = min(adapterIndex, anchorIndex)
    val largerIndex = max(adapterIndex, anchorIndex)
    val overSeamDist = (count - largerIndex) + smallerIndex
    return Pair(inLoopDist, overSeamDist)
}
