/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.recyclerview.widget

import android.content.Context
import android.graphics.PointF
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager

/**
 * [RecyclerView.SmoothScroller] implementation which uses a [LinearInterpolator] until
 * the target position becomes a child of the RecyclerView and then uses a
 * [DecelerateInterpolator] to slowly approach to target position.
 *
 *
 * If the [RecyclerView.LayoutManager] you are using does not implement the
 * [RecyclerView.SmoothScroller.ScrollVectorProvider] interface, then you must override the
 * [.computeScrollVectorForPosition] method. All the LayoutManagers bundled with
 * the support library implement this interface.
 */
open class TestSmoothScroller(context: Context) : RecyclerView.SmoothScroller() {

    private val TAG = "LoopingLayoutManager"

    protected val mLinearInterpolator = LinearInterpolator()

    protected val mDecelerateInterpolator = DecelerateInterpolator()

    protected var mTargetVector: PointF? = null

    private val mDisplayMetrics: DisplayMetrics
    private var mHasCalculatedMillisPerPixel = false
    private var mMillisPerPixel: Float = 0.toFloat()

    // Temporary variables to keep track of the interim scroll target. These values do not
    // point to a real item position, rather point to an estimated location pixels.
    protected var mInterimTargetDx = 0
    protected var mInterimTargetDy = 0

    var millisPerInch = MILLISECONDS_PER_INCH

    private val speedPerPixel: Float
        get() {
            if (!mHasCalculatedMillisPerPixel) {
                mMillisPerPixel = calculateSpeedPerPixel(mDisplayMetrics)
                mHasCalculatedMillisPerPixel = true
            }
            return mMillisPerPixel
        }

    /**
     * When scrolling towards a child view, this method defines whether we should align the left
     * or the right edge of the child with the parent RecyclerView.
     *
     * @return SNAP_TO_START, SNAP_TO_END or SNAP_TO_ANY; depending on the current target vector
     * @see .SNAP_TO_START
     *
     * @see .SNAP_TO_END
     *
     * @see .SNAP_TO_ANY
     */
    protected val horizontalSnapPreference: Int
        get() = if (mTargetVector == null || mTargetVector!!.x == 0f)
            SNAP_TO_ANY
        else if (mTargetVector!!.x > 0) SNAP_TO_END else SNAP_TO_START

    /**
     * When scrolling towards a child view, this method defines whether we should align the top
     * or the bottom edge of the child with the parent RecyclerView.
     *
     * @return SNAP_TO_START, SNAP_TO_END or SNAP_TO_ANY; depending on the current target vector
     * @see .SNAP_TO_START
     *
     * @see .SNAP_TO_END
     *
     * @see .SNAP_TO_ANY
     */
    protected val verticalSnapPreference: Int
        get() = if (mTargetVector == null || mTargetVector!!.y == 0f)
            SNAP_TO_ANY
        else if (mTargetVector!!.y > 0) SNAP_TO_END else SNAP_TO_START

    init {
        mDisplayMetrics = context.resources.displayMetrics
    }

    /**
     * {@inheritDoc}
     */
    override fun onStart() {

    }

    /**
     * {@inheritDoc}
     */
    override fun onTargetFound(targetView: View, state: RecyclerView.State, action: RecyclerView.SmoothScroller.Action) {
        val dx = calculateDxToMakeVisible(targetView, horizontalSnapPreference)
        val dy = calculateDyToMakeVisible(targetView, verticalSnapPreference)
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toInt()
        val time = calculateTimeForDeceleration(distance)
        if (time > 0) {
            action.update(-dx, -dy, time, mDecelerateInterpolator)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onSeekTargetStep(dx: Int, dy: Int, state: RecyclerView.State, action: RecyclerView.SmoothScroller.Action) {
        // TODO(b/72745539): Is there ever a time when onSeekTargetStep should be called when
        // getChildCount returns 0?  Should this logic be extracted out of this method such that
        // this method is not called if getChildCount() returns 0?
        if (childCount == 0) {
            stop()
            return
        }

        check(!(DEBUG && mTargetVector != null
                && (mTargetVector!!.x * dx < 0 || mTargetVector!!.y * dy < 0))) { "Scroll happened in the opposite direction" + " of the target. Some calculations are wrong" }
        mInterimTargetDx = clampApplyScroll(mInterimTargetDx, dx)
        mInterimTargetDy = clampApplyScroll(mInterimTargetDy, dy)

        if (mInterimTargetDx == 0 && mInterimTargetDy == 0) {
            updateActionForInterimTarget(action)
        } // everything is valid, keep going

    }

    /**
     * {@inheritDoc}
     */
    override fun onStop() {
        mInterimTargetDy = 0
        mInterimTargetDx = mInterimTargetDy
        mTargetVector = null
    }

    /**
     * Calculates the scroll speed.
     *
     *
     * By default, LinearSmoothScroller assumes this method always returns the same value and
     * caches the result of calling it.
     *
     * @param displayMetrics DisplayMetrics to be used for real dimension calculations
     * @return The time (in ms) it should take for each pixel. For instance, if returned value is
     * 2 ms, it means scrolling 1000 pixels with LinearInterpolation should take 2 seconds.
     */
    protected open fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
        return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
    }

    /**
     *
     * Calculates the time for deceleration so that transition from LinearInterpolator to
     * DecelerateInterpolator looks smooth.
     *
     * @param dx Distance to scroll
     * @return Time for DecelerateInterpolator to smoothly traverse the distance when transitioning
     * from LinearInterpolation
     */
    protected fun calculateTimeForDeceleration(dx: Int): Int {
        // we want to cover same area with the linear interpolator for the first 10% of the
        // interpolation. After that, deceleration will take control.
        // area under curve (1-(1-x)^2) can be calculated as (1 - x/3) * x * x
        // which gives 0.100028 when x = .3356
        // this is why we divide linear scrolling time with .3356
        return Math.ceil(calculateTimeForScrolling(dx) / .3356).toInt()
    }

    /**
     * Calculates the time it should take to scroll the given distance (in pixels)
     *
     * @param dx Distance in pixels that we want to scroll
     * @return Time in milliseconds
     * @see .calculateSpeedPerPixel
     */
    protected open fun calculateTimeForScrolling(dx: Int): Int {
        // In a case where dx is very small, rounding may return 0 although dx > 0.
        // To avoid that issue, ceil the result so that if dx > 0, we'll always return positive
        // time.
        return Math.ceil((Math.abs(dx) * speedPerPixel).toDouble()).toInt()
    }

    /**
     * When the target scroll position is not a child of the RecyclerView, this method calculates
     * a direction vector towards that child and triggers a smooth scroll.
     *
     * @see .computeScrollVectorForPosition
     */
    protected open fun updateActionForInterimTarget(action: RecyclerView.SmoothScroller.Action) {
        // find an interim target position
        val scrollVector = computeScrollVectorForPosition(targetPosition)
        if (scrollVector == null || scrollVector.x == 0f && scrollVector.y == 0f) {
            val target = targetPosition
            action.jumpTo(target)
            stop()
            return
        }
        normalize(scrollVector)
        mTargetVector = scrollVector

        mInterimTargetDx = (TARGET_SEEK_SCROLL_DISTANCE_PX * scrollVector.x).toInt()
        mInterimTargetDy = (TARGET_SEEK_SCROLL_DISTANCE_PX * scrollVector.y).toInt()
        val time = calculateTimeForScrolling(TARGET_SEEK_SCROLL_DISTANCE_PX)
        // To avoid UI hiccups, trigger a smooth scroll to a distance little further than the
        // interim target. Since we track the distance travelled in onSeekTargetStep callback, it
        // won't actually scroll more than what we need.
        val one = mInterimTargetDx * TARGET_SEEK_EXTRA_SCROLL_RATIO
        val two = mInterimTargetDy * TARGET_SEEK_EXTRA_SCROLL_RATIO
        val three = time * TARGET_SEEK_EXTRA_SCROLL_RATIO
        Log.v(TAG, "x: $one y: $two time: three")
        action.update((mInterimTargetDx * TARGET_SEEK_EXTRA_SCROLL_RATIO).toInt(),
                (mInterimTargetDy * TARGET_SEEK_EXTRA_SCROLL_RATIO).toInt(),
                (time * TARGET_SEEK_EXTRA_SCROLL_RATIO).toInt(), mLinearInterpolator)
    }

    private fun clampApplyScroll(tmpDt: Int, dt: Int): Int {
        var tmpDt = tmpDt
        val before = tmpDt
        tmpDt -= dt
        return if (before * tmpDt <= 0) { // changed sign, reached 0 or was 0, reset
            0
        } else tmpDt
    }

    /**
     * Helper method for [.calculateDxToMakeVisible] and
     * [.calculateDyToMakeVisible]
     */
    fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
        when (snapPreference) {
            SNAP_TO_START -> return boxStart - viewStart
            SNAP_TO_END -> return boxEnd - viewEnd
            SNAP_TO_ANY -> {
                val dtStart = boxStart - viewStart
                if (dtStart > 0) {
                    return dtStart
                }
                val dtEnd = boxEnd - viewEnd
                if (dtEnd < 0) {
                    return dtEnd
                }
            }
            else -> throw IllegalArgumentException("snap preference should be one of the" + " constants defined in SmoothScroller, starting with SNAP_")
        }
        return 0
    }

    /**
     * Calculates the vertical scroll amount necessary to make the given view fully visible
     * inside the RecyclerView.
     *
     * @param view           The view which we want to make fully visible
     * @param snapPreference The edge which the view should snap to when entering the visible
     * area. One of [.SNAP_TO_START], [.SNAP_TO_END] or
     * [.SNAP_TO_ANY].
     * @return The vertical scroll amount necessary to make the view visible with the given
     * snap preference.
     */
    fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
        val layoutManager = layoutManager
        if (layoutManager == null || !layoutManager.canScrollVertically()) {
            return 0
        }
        val params = view.layoutParams as RecyclerView.LayoutParams
        val top = layoutManager.getDecoratedTop(view) - params.topMargin
        val bottom = layoutManager.getDecoratedBottom(view) + params.bottomMargin
        val start = layoutManager.paddingTop
        val end = layoutManager.height - layoutManager.paddingBottom
        return calculateDtToFit(top, bottom, start, end, snapPreference)
    }

    /**
     * Calculates the horizontal scroll amount necessary to make the given view fully visible
     * inside the RecyclerView.
     *
     * @param view           The view which we want to make fully visible
     * @param snapPreference The edge which the view should snap to when entering the visible
     * area. One of [.SNAP_TO_START], [.SNAP_TO_END] or
     * [.SNAP_TO_END]
     * @return The vertical scroll amount necessary to make the view visible with the given
     * snap preference.
     */
    fun calculateDxToMakeVisible(view: View, snapPreference: Int): Int {
        val layoutManager = layoutManager
        if (layoutManager == null || !layoutManager.canScrollHorizontally()) {
            return 0
        }
        val params = view.layoutParams as RecyclerView.LayoutParams
        val left = layoutManager.getDecoratedLeft(view) - params.leftMargin
        val right = layoutManager.getDecoratedRight(view) + params.rightMargin
        val start = layoutManager.paddingLeft
        val end = layoutManager.width - layoutManager.paddingRight
        return calculateDtToFit(left, right, start, end, snapPreference)
    }

    companion object {

        private val DEBUG = false

        private val MILLISECONDS_PER_INCH = 25f

        private val TARGET_SEEK_SCROLL_DISTANCE_PX = 10000

        /**
         * Align child view's left or top with parent view's left or top
         *
         * @see .calculateDtToFit
         * @see .calculateDxToMakeVisible
         * @see .calculateDyToMakeVisible
         */
        val SNAP_TO_START = -1

        /**
         * Align child view's right or bottom with parent view's right or bottom
         *
         * @see .calculateDtToFit
         * @see .calculateDxToMakeVisible
         * @see .calculateDyToMakeVisible
         */
        val SNAP_TO_END = 1

        /**
         *
         * Decides if the child should be snapped from start or end, depending on where it
         * currently is in relation to its parent.
         *
         * For instance, if the view is virtually on the left of RecyclerView, using
         * `SNAP_TO_ANY` is the same as using `SNAP_TO_START`
         *
         * @see .calculateDtToFit
         * @see .calculateDxToMakeVisible
         * @see .calculateDyToMakeVisible
         */
        val SNAP_TO_ANY = 0

        // Trigger a scroll to a further distance than TARGET_SEEK_SCROLL_DISTANCE_PX so that if target
        // view is not laid out until interim target position is reached, we can detect the case before
        // scrolling slows down and reschedule another interim target scroll
        private val TARGET_SEEK_EXTRA_SCROLL_RATIO = 1.2f
    }
}
