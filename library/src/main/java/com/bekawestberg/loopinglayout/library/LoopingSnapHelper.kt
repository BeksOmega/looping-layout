package com.bekawestberg.loopinglayout.library

import android.graphics.PointF
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import java.lang.Thread.sleep

class LoopingSnapHelper : LinearSnapHelper() {

    private val INVALID_DISTANCE = 1f

    private var mHorizontalHelper: OrientationHelper? = null

    private var mVerticalHelper: OrientationHelper? = null

    private lateinit var mRecyclerView: RecyclerView


    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        Log.v(TAG, "attached")
        mRecyclerView = recyclerView!!
        super.attachToRecyclerView(recyclerView)
    }

    override fun findTargetSnapPosition(layoutManager: RecyclerView.LayoutManager, velocityX: Int,
                                        velocityY: Int): Int {
        Log.v(TAG, "called.")
        val itemCount = layoutManager.itemCount
        if (itemCount == 0) {
            return RecyclerView.NO_POSITION
        }

        val currentView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION

        val currentPosition = layoutManager.getPosition(currentView)
        if (currentPosition == RecyclerView.NO_POSITION) {
            return RecyclerView.NO_POSITION
        }

        layoutManager as LoopingLayoutManager
        // deltaJumps sign comes from the velocity which may not match the order of children in
        // the LayoutManager.
        val endDirection = layoutManager.convertAdapterDirToMovementDir(
                LoopingLayoutManager.TOWARDS_HIGHER_INDICES)

        val deltaJump = if (layoutManager.canScrollHorizontally()) {
            estimateNextPositionDiffForFling(layoutManager,
                    getHorizontalHelper(layoutManager), velocityX, 0) * endDirection
        } else {
            estimateNextPositionDiffForFling(layoutManager,
                    getVerticalHelper(layoutManager), 0, velocityY) * endDirection
        }

        if (deltaJump == 0) {
            return RecyclerView.NO_POSITION
        }

        if (deltaJump < 0) {
            layoutManager.smoothScrollDirectionDecider = ::addViewsAtTopLeftEdge
        } else {
            layoutManager.smoothScrollDirectionDecider = ::addViewsAtBottomRightEdge
        }

        //var targetPos = currentPosition + deltaJump

        var targetPos = currentPosition + deltaJump/*currentPosition.loop(deltaJump, layoutManager.itemCount)*/
        Log.v(TAG, "currentPos: $currentPosition delta: $deltaJump targetpos: $targetPos")
        // Input doesn't matter because we set the return to be static above.

        /*if (targetPos < 0) {
            targetPos = 0
        }
        if (targetPos >= itemCount) {
            targetPos = itemCount - 1
        }*/
        //Log.v(TAG, "targetpos after: $targetPos")
        // var targetPos = currentPosition.loop(deltaJump, layoutManager.itemCount)

        return targetPos
    }

    private fun estimateNextPositionDiffForFling(layoutManager: RecyclerView.LayoutManager,
                                                 helper: OrientationHelper, velocityX: Int, velocityY: Int): Int {
        Log.v(TAG, "called.")
        val distances = calculateScrollDistance(velocityX, velocityY)
        val distancePerChild = computeDistancePerChild(layoutManager, helper)
        if (distancePerChild <= 0) {
            return 0
        }
        val distance = if (Math.abs(distances[0]) > Math.abs(distances[1])) distances[0] else distances[1]
        return Math.round(distance / distancePerChild)
    }

    private fun getVerticalHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper {
        Log.v(TAG, "called.")
            mVerticalHelper = OrientationHelper.createVerticalHelper(layoutManager)
        return mVerticalHelper!!
    }

    private fun getHorizontalHelper(
            layoutManager: RecyclerView.LayoutManager): OrientationHelper {
        Log.v(TAG, "called.")
            mHorizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager)
        return mHorizontalHelper!!
    }

    private fun computeDistancePerChild(layoutManager: RecyclerView.LayoutManager,
                                        helper: OrientationHelper): Float {
        Log.v(TAG, "called.")
        var minPosView: View? = null
        var maxPosView: View? = null
        var minPos = Integer.MAX_VALUE
        var maxPos = Integer.MIN_VALUE
        val childCount = layoutManager.childCount
        if (childCount == 0) {
            return INVALID_DISTANCE
        }

        for (i in 0 until childCount) {
            val child = layoutManager.getChildAt(i)
            val pos = layoutManager.getPosition(child!!)
            if (pos == RecyclerView.NO_POSITION) {
                continue
            }
            if (pos < minPos) {
                minPos = pos
                minPosView = child
            }
            if (pos > maxPos) {
                maxPos = pos
                maxPosView = child
            }
        }
        if (minPosView == null || maxPosView == null) {
            return INVALID_DISTANCE
        }
        val start = Math.min(helper.getDecoratedStart(minPosView),
                helper.getDecoratedStart(maxPosView))
        val end = Math.max(helper.getDecoratedEnd(minPosView),
                helper.getDecoratedEnd(maxPosView))
        val distance = end - start
        return if (distance == 0) {
            INVALID_DISTANCE
        } else 1f * distance / (maxPos - minPos + 1)
    }

    override fun createScroller(layoutManager: RecyclerView.LayoutManager): RecyclerView.SmoothScroller? {
        if (layoutManager !is LoopingLayoutManager) {
            return null  // TODO: Log!
        }

        val smoothScroller = layoutManager.LoopingSmoothScroller(mRecyclerView.context)
        smoothScroller.layoutManager = layoutManager
        return smoothScroller
    }

    companion object {
        private const val TAG = "SnapHelper"
    }
}

