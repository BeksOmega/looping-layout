package com.bekawestberg.loopinglayout.library

import android.view.View
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import java.lang.Exception

class LoopingSnapHelper : LinearSnapHelper() {

    // TODO: Put up issue for android. This just duplicates mRecyclerView in the parent.
    //  mRecyclerView should be public/protected not private.
    private lateinit var recyclerView: RecyclerView

    private var verticalHelper: OrientationHelper? = null
    private var horizontalHelper: OrientationHelper? = null

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (recyclerView == null) {
            return
        }
        if (recyclerView.layoutManager !is LoopingLayoutManager) {
            throw Exception("LoopingSnapHelper can only be attached to a RecyclerView with a " +
                    "LoopingLayoutManager. Be sure to attach the LayoutManager before attaching " +
                    "the snap helper.")
        }
        this.recyclerView = recyclerView
        super.attachToRecyclerView(recyclerView)
    }

    /**
     * Returns the un-looped adapter position that we want to snap to.
     * It returns the un-looped position so that the LoopingSmoothScroller can pass the item
     * associated with the given adapter index multiple times before stopping. This makes the
     * animation actually appear to match the fling velocity.
     */
    override fun findTargetSnapPosition(
            layoutManager: RecyclerView.LayoutManager,
            velocityX: Int,
            velocityY: Int
    ): Int {
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
        val endDirection = layoutManager.convertAdapterDirToMovementDir(
                LoopingLayoutManager.TOWARDS_HIGHER_INDICES)
        val deltaJump = if (layoutManager.canScrollVertically()) {
            estimateNextPositionDiffForFling(
                    layoutManager, getVerticalHelper(layoutManager), 0, velocityY) * endDirection
        } else {
            estimateNextPositionDiffForFling(
                    layoutManager, getHorizontalHelper(layoutManager), velocityX, 0) * endDirection
        }
        if (deltaJump == 0) {
            return RecyclerView.NO_POSITION
        }

        // Makes it so we always go in the direction of the fling.
        layoutManager.smoothScrollDirectionDecider = if (deltaJump < 0) {
            ::addViewsAtTopLeftEdge
        } else {
            ::addViewsAtBottomRightEdge
        }

        return currentPosition + deltaJump
    }

    override fun createScroller(layoutManager: RecyclerView.LayoutManager): RecyclerView.SmoothScroller {
        val smoothScroller = LoopingLayoutManager.LoopingSmoothScroller(recyclerView.context)
        smoothScroller.millisPerInch = 75f
        return smoothScroller
    }

    // TODO: This is an exact replicate of a function that is private in the superclass. Put in
    //   a request to AOSP to make it public/protected.
    /**
     * Estimates a position to which SnapHelper will try to scroll to in response to a fling.
     *
     * @param layoutManager The [RecyclerView.LayoutManager] associated with the attached
     * [RecyclerView].
     * @param helper        The [OrientationHelper] that is created from the LayoutManager.
     * @param velocityX     The velocity on the x axis.
     * @param velocityY     The velocity on the y axis.
     *
     * @return The diff between the target scroll position and the current position.
     */
    private fun estimateNextPositionDiffForFling(
            layoutManager: RecyclerView.LayoutManager,
            helper: OrientationHelper,
            velocityX: Int,
            velocityY: Int
    ): Int {
        val distances = calculateScrollDistance(velocityX, velocityY)
        val distancePerChild = computeDistancePerChild(layoutManager, helper)
        if (distancePerChild <= 0) {
            return 0
        }
        val distance = if (Math.abs(distances[0]) > Math.abs(distances[1])) distances[0] else distances[1]
        return Math.round(distance / distancePerChild)
    }

    // TODO: This is an exact replicate of a function that is private in the superclass. Put in
    //   a request to AOSP to make it public/protected.
    /**
     * Computes an average pixel value to pass a single child.
     * <p>
     * Returns a negative value if it cannot be calculated.
     *
     * @param layoutManager The {@link RecyclerView.LayoutManager} associated with the attached
     *                      {@link RecyclerView}.
     * @param helper        The relevant {@link OrientationHelper} for the attached
     *                      {@link RecyclerView.LayoutManager}.
     *
     * @return A float value that is the average number of pixels needed to scroll by one view in
     * the relevant direction.
     */
    private fun computeDistancePerChild(
            layoutManager: RecyclerView.LayoutManager,
            helper: OrientationHelper
    ): Float {
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

    // TODO: This is an exact replicate of a function that is private in the superclass. Put in
    //   a request to AOSP to make it public/protected.
    private fun getVerticalHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper {
        if (verticalHelper == null || verticalHelper!!.layoutManager != layoutManager) {
            verticalHelper = OrientationHelper.createVerticalHelper(layoutManager)
        }
        return verticalHelper!!
    }

    // TODO: This is an exact replicate of a function that is private in the superclass. Put in
    //   a request to AOSP to make it public/protected.
    private fun getHorizontalHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper {
        if (horizontalHelper == null || horizontalHelper!!.layoutManager != layoutManager) {
            horizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager)
        }
        return horizontalHelper!!
    }

    companion object {
        const val INVALID_DISTANCE = 1f
    }
}