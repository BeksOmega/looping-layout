package com.bekawestberg.loopinglayout.library

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun defaultDecider(
        adapterIndex: Int,
        layoutManager: LoopingLayoutManager,
        state: RecyclerView.State
): Int {
    return estimateShortestRoute(adapterIndex, layoutManager, state)
}

fun addViewsAtAnchorEdge(
        adapterIndex: Int,
        layoutManager: LoopingLayoutManager,
        state: RecyclerView.State
): Int {
    val dir = layoutManager.convertAdapterDirToMovementDir(LoopingLayoutManager.TOWARDS_LOWER_INDICES)
    Log.v("DirectionDecider", "$dir")
    return dir
}

fun addViewsAtOptAnchorEdge(
        adapterIndex: Int,
        layoutManager: LoopingLayoutManager,
        state: RecyclerView.State
): Int {
    return layoutManager.convertAdapterDirToMovementDir(LoopingLayoutManager.TOWARDS_HIGHER_INDICES)
}

fun estimateShortestRoute(
        adapterIndex: Int,
        layoutManager: LoopingLayoutManager,
        state: RecyclerView.State
): Int {
    // Special case the view being partially visible.
    if (layoutManager.topLeftIndex == adapterIndex) {
        Log.v("DirectionalDeciders", "topLeft is equal")
        return LoopingLayoutManager.TOWARDS_TOP_LEFT
    } else if (layoutManager.bottomRightIndex == adapterIndex) {
        Log.v("DirectionalDeciders", "bottomRight is equal")
        return LoopingLayoutManager.TOWARDS_BOTTOM_RIGHT
    }

    Log.v("DirectionalDeciders", "neither is equal")

    val (topLeftInLoopDist, topLeftOverSeamDist) = calculateDistances(
            adapterIndex, layoutManager.topLeftIndex, state.itemCount)
    val topLeftTargetSmaller = adapterIndex < layoutManager.topLeftIndex

    val (bottomRightInLoopDist, bottomRightOverSeamDist) = calculateDistances(
            adapterIndex, layoutManager.bottomRightIndex, state.itemCount)
    val bottomRightTargetSmaller = adapterIndex < layoutManager.bottomRightIndex

    Log.v("DirectionDeciders", "$topLeftInLoopDist $topLeftOverSeamDist $bottomRightInLoopDist $bottomRightOverSeamDist")

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

    Log.v("DirectionDeciders", "is in loop? $minDistIsInLoop target smaller? $targetIsSmaller")

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
