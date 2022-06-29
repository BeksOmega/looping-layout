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

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import java.lang.Math.min
import kotlin.math.abs

class LoopingLayoutManager : LayoutManager, RecyclerView.SmoothScroller.ScrollVectorProvider {

    /**
     * Describes the way the layout should be... laid out. Anchor index, anchor edge, and scroll
     * offset. Used for triggering scrollTo, updating after an adapter change, and orientation
     * changes.
     */
    private var layoutRequest = LayoutRequest(anchorIndex = 0)

    /**
     * The amount of extra (i.e. not visible) space to fill up with views after we have filled up
     * the visible space. This is used during smooth scrolling, so that the target view can be found
     * before it becomes visible (helps with smooth deceleration).
     */
    private var extraLayoutSpace = 0

    /** Helps with some layout calculations. Mainly getting the non-scrolling edges of each view. */
    private lateinit var orientationHelper: OrientationHelper

    /**
     * The width of the layout - not the recycler.
     * AKA the width of the recycler, minus the padding on the left and right.
     */
    public val layoutWidth: Int
        get() = width - paddingLeft - paddingRight
    /**
     * The height of the layout - not the recycler.
     * AKA the height of the recycler, minus the padding on the top and bottom.
     */
    public val layoutHeight: Int
        get() = height - paddingTop - paddingBottom
    
    /** Describes the adapter index of the view in the top/left -most position. */
    public var topLeftIndex = 0
            private set
    /** Describes the adapter index of the view in the bottom/right -most position. */
    public var bottomRightIndex = 0
            private set

    /**
     * The adapter index of the view at the edge of the layout where the 0th item was
     * originally laid out.
     */
    public val anchorIndex
        get() = getInitialIndex(getMovementDirectionFromAdapterDirection(TOWARDS_LOWER_INDICES))

    /**
     * The adapter index of the view at the edge of the layout *opposite* the edge where the 0th
     * item was originally laid out.
     */
    public val optAnchorIndex
        get() = getInitialIndex(getMovementDirectionFromAdapterDirection(TOWARDS_HIGHER_INDICES))

    /**
     * When the layout manager needs to scroll to a position (via smooth scrolling) it needs some
     * method to decide which movement direction to scroll in. This variable stores that method.
     */
    public var smoothScrollDirectionDecider: (Int, LoopingLayoutManager, Int) -> Int = ::defaultDecider
    
    /**
     * Creates a LoopingLayout manager with the given orientation and reverse layout option.
     * @param context Current context, will be used to access resources.
     * @param orientation Layout orientation. Should be [HORIZONTAL] or [VERTICAL].
     * @param reverseLayout When set to true, lays out items in the opposite direction from default.
     */
    @JvmOverloads
    public constructor(context: Context, orientation: Int = VERTICAL, reverseLayout: Boolean = false) {
        this.orientation = orientation
        this.reverseLayout = reverseLayout
    }

    /**
     * Constructor used when layout manager is set in XML by RecyclerView attribute
     * "layoutManager". Defaults to vertical orientation and not reversed.
     */
    public constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) {
        val properties = getProperties(context, attrs, defStyleAttr, defStyleRes)
        orientation = properties.orientation
        reverseLayout = properties.reverseLayout
    }

    /**
     * Describes the current orientation of this layout manager. Either [HORIZONTAL] or [VERTICAL].
     */
    public var orientation: Int = 0
        set(orientation) {
            require(orientation == HORIZONTAL || orientation == VERTICAL) {
                "invalid orientation:$orientation"
            }
            if (orientation != this.orientation) {
                orientationHelper = OrientationHelper.createOrientationHelper(this, orientation)
                assertNotInLayoutOrScroll(null)
                field = orientation
                requestLayout()
                return
            }
            if (!::orientationHelper.isInitialized) {
                orientationHelper = OrientationHelper.createOrientationHelper(this, orientation)
            }
        }

    /**
     * Describes whether the views are laid out in the opposite order they would normally be laid
     * out in.
     *
     * The "normal order" is based on the orientation and RTL/LTR state. For example if this
     * was in a RTL Horizontal state, and this property were true, the views would be laid out
     * from left to right.
     */
    public var reverseLayout = false
        set(reverseLayout) {
            if (reverseLayout == this.reverseLayout) {
                return
            }
            assertNotInLayoutOrScroll(null)
            field = reverseLayout
            requestLayout()
        }

    /**
     * Returns true if we are in RTL mode, false otherwise.
     */
    public val isLayoutRTL: Boolean
        get() = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL

    public override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    public override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    public override fun onSaveInstanceState(): Parcelable? {
        // All of this information is based on keeping the item currently at the anchor edge
        // at the anchor edge.
        val direction = getMovementDirectionFromAdapterDirection(TOWARDS_LOWER_INDICES)
        return if (childCount == 0) {  // Occurs if the screen is off on startup.
            null
        } else {
            LayoutRequest(
                    anchorIndex = getInitialIndex(direction),
                    scrollOffset = getInitialItem(direction).hiddenSize)
        }
    }

    public override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is LayoutRequest) {
            layoutRequest = state
        }
    }

    public override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        layoutRequest.initialize(this, state)

        detachAndScrapAttachedViews(recycler)

        // A) We want to layout the item at the adapter index first, so that we can set the scroll offset.
        // B) We want the item to be laid out /at/ the edge associated with the adapter direction.
        // This means after it gets laid out we need to move /away/ from that edge.
        // Hence the direction is inverted.
        val movementDir = getMovementDirectionFromAdapterDirection(-layoutRequest.adapterDirection)
        var prevItem: ListItem? = null
        val size = if (orientation == HORIZONTAL) layoutWidth else layoutHeight
        var sizeFilled = 0
        var index = min(layoutRequest.anchorIndex, state.itemCount - 1)
        while (sizeFilled < size) {
            val view = createViewForIndex(index, movementDir, recycler)
            val item = getItemForView(movementDir, view)
            var layoutRect = getNonScrollingEdges(view)
            layoutRect = prevItem?.getPositionOfItemFollowingSelf(item, layoutRect) ?:
                    item.getPositionOfSelfAsFirst(layoutRect, layoutRequest.scrollOffset)
            layoutDecorated(view, layoutRect.left, layoutRect.top,
                    layoutRect.right, layoutRect.bottom)

            index = stepIndex(index, movementDir, state, false)
            if (sizeFilled == 0) {
                sizeFilled = if (orientation == HORIZONTAL) {
                    layoutRect.left
                } else {
                    layoutRect.top
                }
            }
            sizeFilled += item.size
            prevItem = item
        }

        if (movementDir == TOWARDS_TOP_LEFT) {
            bottomRightIndex = layoutRequest.anchorIndex
            topLeftIndex = stepIndex(index, -movementDir, state, false)
        } else {
            topLeftIndex = layoutRequest.anchorIndex
            bottomRightIndex = stepIndex(index, -movementDir, state, false)
        }
    }

    public override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        layoutRequest.finishProcessing();
    }

    public override fun canScrollVertically(): Boolean {
        return orientation == VERTICAL
    }

    public override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        return scrollBy(dy, recycler, state)
    }

    public override fun canScrollHorizontally(): Boolean {
        return orientation == HORIZONTAL
    }

    public override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        return scrollBy(dx, recycler, state)
    }

    /**
     * Scrolls the list of views by the given [delta]. Creates and binds new views if necessary.
     * Whether to scroll horizontally or vertically is determined by the orientation.
    * @return The actually distance scrolled. This will equal to delta unless the layout manager
     * does not have children, in which case it will be zero. Other layout managers may
     * return less than the delta if they hit a bound, but the LoopingLayoutManager has no
     * bounds.
     */
    private fun scrollBy(
        delta: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (childCount == 0 || delta == 0) {
            return 0
        }

        val movementDir = Integer.signum(delta)
        scrapNonVisibleViews(recycler)
        val absDelta = abs(delta)
        var amountScrolled = 0
        var index = getInitialIndex(movementDir)
        var selectedItem = getInitialItem(movementDir)
        while (amountScrolled < absDelta) {
            val hiddenSize = selectedItem.hiddenSize
            // Scroll just enough to complete the scroll, or bring the view fully into view.
            val amountToScroll = hiddenSize.coerceAtMost(absDelta - amountScrolled)
            amountScrolled += amountToScroll
            offsetChildren(amountToScroll * -movementDir)
            if (amountScrolled < absDelta) {
                index = stepIndex(index, movementDir, state)
                val newView = createViewForIndex(index, movementDir, recycler)
                val newItem = getItemForView(movementDir, newView)
                var layoutRect = getNonScrollingEdges(newView)
                layoutRect = selectedItem.getPositionOfItemFollowingSelf(newItem, layoutRect)
                layoutDecorated(newView, layoutRect.left, layoutRect.top,
                        layoutRect.right, layoutRect.bottom)
                selectedItem = newItem
            }
        }

        // The amount of extra (i.e not visible) space currently covered by views.
        var viewSpace = selectedItem.hiddenSize
        while(viewSpace < extraLayoutSpace) {
            // We don't want the topLeftIndex or bottomRightIndex to reflect non-visible views.
            index = stepIndex(index, movementDir, state, updateIndex = false)
            val newView = createViewForIndex(index, movementDir, recycler)
            val newItem = getItemForView(movementDir, newView)
            var layoutRect = getNonScrollingEdges(newView)
            layoutRect = selectedItem.getPositionOfItemFollowingSelf(newItem, layoutRect)
            layoutDecorated(newView, layoutRect.left, layoutRect.top,
                    layoutRect.right, layoutRect.bottom)
            selectedItem = newItem
            viewSpace += selectedItem.size
        }

        recycleViews(movementDir, recycler, state)
        return amountScrolled * movementDir
    }

    /**
     * Returns a rect populated with the positions of the static edges of the view. I.e. right and
     * left in horizontal mode, top and bottom in vertical mode.
     */
    private fun getNonScrollingEdges(view: View): Rect {
        val layoutRect = Rect()
        val isVertical = orientation == VERTICAL
        // In LTR we align vertical layouts with the left edge, and in RTL the right edge.
        when {
            isVertical && isLayoutRTL -> {
                layoutRect.right = width - paddingRight
                layoutRect.left = layoutRect.right -
                        orientationHelper.getDecoratedMeasurementInOther(view)
            }
            isVertical && !isLayoutRTL -> {
                layoutRect.left = paddingLeft
                layoutRect.right = layoutRect.left +
                        orientationHelper.getDecoratedMeasurementInOther(view)
            }
            else -> {  // Horizontal
                layoutRect.top = paddingTop
                layoutRect.bottom = layoutRect.top +
                        orientationHelper.getDecoratedMeasurementInOther(view)
            }
        }
        return layoutRect
    }

    /**
     * Creates, measures, and inserts a view into the recycler at the given [adapterIndex].
     * The view is then returned so it can be properly positioned.
     * @param movementDir The current direction the views are being scrolled in.
     */
    private fun createViewForIndex(
        adapterIndex: Int,
        movementDir: Int,
        recycler: RecyclerView.Recycler
    ): View {
        val newView = recycler.getViewForPosition(adapterIndex)
        if (movementDir == TOWARDS_LOWER_INDICES) {
            addView(newView, 0)
        } else {
            addView(newView)
        }
        measureChildWithMargins(newView, 0, 0)
        return newView
    }

    /**
     * Moves all child views by the given [amount] (can be positive or negative). Determines whether
     * they are moved horizontally or vertically based on the orientation.
     */
    private fun offsetChildren(amount: Int) {
        if (orientation == HORIZONTAL) {
            offsetChildrenHorizontal(amount)
        } else {
            offsetChildrenVertical(amount)
        }
    }

    /**
     * Returns the adapter index of the view closest to where new views will be shown.
     * This is determined based on the [movementDir] views are being scrolled in.
     * For example, if the user is trying to see new views at the top, this will return the adapter
     * index of the top-most view.
     */
    private fun getInitialIndex(movementDir: Int): Int {
        return if (movementDir == TOWARDS_TOP_LEFT) {
            topLeftIndex
        } else {
            bottomRightIndex
        }
    }

    /**
     * Returns the view (wrapped in a ListItem) closest to where new views will be shown.
     * This is determined based on the [movementDir] views are being scrolled in.
     * For example, if the user is trying to see new views at the top, this will return the
     * top-most view.
     */
    private fun getInitialItem(movementDir: Int): ListItem {
        val initialView = if (movementDir == TOWARDS_LOWER_INDICES) {
            getChildAt(0)
        } else {
            getChildAt(childCount - 1)
        }
        // initialView should never be null, so we'll just ask for an exception.
        return getItemForView(movementDir, initialView!!)
    }

    /**
     * Increments/decrements and returns the provided [index] based on the [movementDir] the list is
     * being moved in. For example, if the list is being scrolled towards items with higher adapter
     * indices the index will be incremented.
     *
     * Also (by default) handles updating [topLeftIndex] or [bottomRightIndex] to reflect the
     * newest view. This functionality can be disabled by passing "false" to the [updateIndex] parameter.
     */
    private fun stepIndex(
            index: Int,
            movementDir: Int,
            state: RecyclerView.State,
            updateIndex: Boolean = true
    ): Int {
        val adapterDirection = getAdapterDirectionFromMovementDirection(movementDir)
        val count = state.itemCount

        val isTowardsTopLeft = movementDir == TOWARDS_TOP_LEFT
        val isTowardsBottomRight = movementDir == TOWARDS_BOTTOM_RIGHT
        val isTowardsHigherIndices = adapterDirection == TOWARDS_HIGHER_INDICES
        val isTowardsLowerIndices = adapterDirection == TOWARDS_LOWER_INDICES

        val newIndex: Int
        when {
            isTowardsTopLeft && isTowardsHigherIndices -> {
                newIndex = index.loopedIncrement(count)
                if (updateIndex) topLeftIndex = newIndex
            }
            isTowardsTopLeft && isTowardsLowerIndices -> {
                newIndex = index.loopedDecrement(count)
                if (updateIndex)  topLeftIndex = newIndex
            }
            isTowardsBottomRight && isTowardsHigherIndices -> {
                newIndex = index.loopedIncrement(count)
                if (updateIndex) bottomRightIndex = newIndex
            }
            isTowardsBottomRight && isTowardsLowerIndices -> {
                newIndex = index.loopedDecrement(count)
                if (updateIndex) bottomRightIndex = newIndex
            }
            else -> throw IllegalStateException("Invalid move & adapter direction combination.")
        }
        return newIndex
    }

    /**
     * Returns the [view] wrapped in the correct ListItem based on the [movementDir] and
     * configuration of the LayoutManager.
     *
     * ListItems give the view an interface that's more usable when writing a LayoutManager.
     */
    private fun getItemForView(movementDir: Int, view: View): ListItem {
        val isVertical = orientation == VERTICAL
        val isHorizontal = !isVertical
        val isTowardsTopLeft = movementDir == TOWARDS_TOP_LEFT
        val isTowardsBottomRight = !isTowardsTopLeft

        return when {
            isVertical && isTowardsTopLeft -> LeadingBottomListItem(view)
            isVertical && isTowardsBottomRight -> LeadingTopListItem(view)
            isHorizontal && isTowardsTopLeft -> LeadingRightListItem(view)
            isHorizontal && isTowardsBottomRight -> LeadingLeftListItem(view)
            else -> throw IllegalStateException("Invalid movement state.")
        }
    }

    /**
     * Sends any currently non-visible (i.e. views completely outside the visible bounds of the
     * recycler) views to the scrap heap. Used by scrollBy to make sure we're only dealing with
     * visible views before adding new ones.
     */
    private fun scrapNonVisibleViews(recycler: RecyclerView.Recycler) {
        for (i in (childCount - 1) downTo 0) {
            val view = getChildAt(i) ?: continue
            if (!viewIsVisible(view)) {
                detachAndScrapView(view, recycler)
            }
        }
    }

    /**
     * Recycles views that are no longer visible given the [movementDir] of the scroll that was just
     * completed.
     */
    private fun recycleViews(
        movementDir: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ) {
        val initialIndex = getInitialIndex(movementDir)
        // The first visible item will bump us to zero.
        var distanceFromStart = -1
        var foundVisibleView = false
        val viewsToRemove = mutableListOf<Int>()

        // We want to loop through the views in the order opposite the direction of movement so that
        // we remove views that have become hidden because of scrolling.
        val range = if (movementDir == TOWARDS_TOP_LEFT) {
            0 until childCount
        } else {
            childCount-1 downTo 0
        }

        // Ignore hidden views at the start of the range.
        // Only recycle hidden views at the end of the range.
        for (i in range) {
            val view = getChildAt(i)!!
            if (viewIsVisible(view)) {
                if (!foundVisibleView) {
                    foundVisibleView = true
                }
                distanceFromStart++
            } else if (foundVisibleView){
                viewsToRemove.add(i)
            }
        }

        // Removing the views after collecting them and putting them in order from greatest -> least
        // makes sure we don't get null errors. See #19.
        viewsToRemove.sortedDescending().forEach { i ->
            removeAndRecycleViewAt(i, recycler)
        }

        if (viewsToRemove.count() == 0) {
            // If we didn't find anything that needed to be disposed, no indices need to be updated.
            return
        }

        // We need to flip the direction, since we looped through views in the opposite order.
        // When we flip the movement direction, the adapter direction will be flipped as well.
        val adapterDirection = getAdapterDirectionFromMovementDirection(movementDir * -1)
        val changeInPosition = adapterDirection * distanceFromStart
        val count = state.itemCount
        if (movementDir == TOWARDS_TOP_LEFT) {
            bottomRightIndex = initialIndex.loop(changeInPosition, count)
        } else {
            topLeftIndex = initialIndex.loop(changeInPosition, count)
        }
    }

    /**
     * Returns true if any part of the [view] is within the visible bounds of the recycler.
     * False otherwise.
     */
    private fun viewIsVisible(view: View): Boolean {
        // Note for future: Making these checks or= breaks extraLayoutSpacing because (I think) if
        // the hidden view's edge is aligned with the recycler edge, it isn't scrapped when it
        // should be.
        return if (orientation == HORIZONTAL) {
            getDecoratedRight(view) > paddingLeft && getDecoratedLeft(view) < width - paddingRight
        } else {
            getDecoratedBottom(view) > paddingTop && getDecoratedTop(view) < height - paddingBottom
        }
    }

    /**
     * Returns true if the [view] is fully within the visible bounds of the recycler along the
     * layout axis. This means fully visible horizontally in horizontal mode, and fully visible
     * vertically in vertical mode. False otherwise.
     */
    private fun viewIsFullyVisible(view: View): Boolean {
        return if (orientation == HORIZONTAL) {
            getDecoratedLeft(view) >= paddingLeft && getDecoratedRight(view) <= width - paddingRight
        } else {
            getDecoratedTop(view) >= paddingTop && getDecoratedBottom(view) <= height - paddingBottom
        }
    }

    /**
     * Converts the [movementDir] ([TOWARDS_TOP_LEFT] or [TOWARDS_BOTTOM_RIGHT]) to an adapter
     * direction ([TOWARDS_HIGHER_INDICES] OR [TOWARDS_LOWER_INDICES]). The adapter direction
     * tells us which direction we would be traversing the views in if we moved in the given
     * movement direction.
     */
    public fun convertMovementDirToAdapterDir(movementDir: Int): Int {
        return getMovementDirectionFromAdapterDirection(movementDir)
    }

    /**
     * Converts the [movementDir] ([TOWARDS_TOP_LEFT] or [TOWARDS_BOTTOM_RIGHT]) to an adapter
     * direction ([TOWARDS_HIGHER_INDICES] OR [TOWARDS_LOWER_INDICES]). The adapter direction
     * tells us which direction we would be traversing the views in if we moved in the given
     * movement direction.
     *
     * If you are working inside the layout manager this is what you should call!
     */
    private fun getAdapterDirectionFromMovementDirection(movementDir: Int): Int {
        val isVertical = orientation == VERTICAL
        val isHorizontal = !isVertical
        val isTowardsTopLeft = movementDir == TOWARDS_TOP_LEFT
        val isTowardsBottomRight = !isTowardsTopLeft
        val isRTL = isLayoutRTL
        val isLTR = !isRTL
        val isReversed = reverseLayout
        val isNotReversed = !isReversed

        return when {
            isVertical && isTowardsTopLeft && isNotReversed -> TOWARDS_LOWER_INDICES
            isVertical && isTowardsTopLeft && isReversed -> TOWARDS_HIGHER_INDICES
            isVertical && isTowardsBottomRight && isNotReversed -> TOWARDS_HIGHER_INDICES
            isVertical && isTowardsBottomRight && isReversed -> TOWARDS_LOWER_INDICES
            isHorizontal && isTowardsTopLeft && isLTR && isNotReversed -> TOWARDS_LOWER_INDICES
            isHorizontal && isTowardsTopLeft && isLTR && isReversed -> TOWARDS_HIGHER_INDICES
            isHorizontal && isTowardsTopLeft && isRTL && isNotReversed -> TOWARDS_HIGHER_INDICES
            isHorizontal && isTowardsTopLeft && isRTL && isReversed -> TOWARDS_LOWER_INDICES
            isHorizontal && isTowardsBottomRight && isLTR && isNotReversed -> TOWARDS_HIGHER_INDICES
            isHorizontal && isTowardsBottomRight && isLTR && isReversed -> TOWARDS_LOWER_INDICES
            isHorizontal && isTowardsBottomRight && isRTL && isNotReversed -> TOWARDS_LOWER_INDICES
            isHorizontal && isTowardsBottomRight && isRTL && isReversed -> TOWARDS_HIGHER_INDICES
            else -> throw IllegalStateException("Invalid movement state.")
        }
    }

    /**
     * Converts the [adapterDir] ([TOWARDS_HIGHER_INDICES] OR [TOWARDS_LOWER_INDICES]) to a movement
     * direction ([TOWARDS_TOP_LEFT] or [TOWARDS_BOTTOM_RIGHT]). The movement direction tells us
     * which direction we need to scroll the views in to move towards the given adapter direction.
     */
    public fun convertAdapterDirToMovementDir(adapterDir: Int): Int {
        return getMovementDirectionFromAdapterDirection(adapterDir)
    }

    /**
     * Converts the [adapterDir] ([TOWARDS_HIGHER_INDICES] OR [TOWARDS_LOWER_INDICES]) to a movement
     * direction ([TOWARDS_TOP_LEFT] or [TOWARDS_BOTTOM_RIGHT]). The movement direction tells us
     * which direction we need to scroll the views in to move towards the given adapter direction.
     *
     * If you are working inside the layout manager this is what you should call!
     */
    private fun getMovementDirectionFromAdapterDirection(movementDir: Int): Int {
        val isVertical = orientation == VERTICAL
        val isHorizontal = !isVertical
        val isTowardsHigher = movementDir == TOWARDS_HIGHER_INDICES
        val isTowardsLower = !isTowardsHigher
        val isRTL = isLayoutRTL
        val isLTR = !isRTL
        val isReversed = reverseLayout
        val isNotReversed = !isReversed

        return when {
            isVertical && isTowardsHigher && isNotReversed -> TOWARDS_BOTTOM_RIGHT
            isVertical && isTowardsHigher && isReversed -> TOWARDS_TOP_LEFT
            isVertical && isTowardsLower && isNotReversed -> TOWARDS_TOP_LEFT
            isVertical && isTowardsLower && isReversed -> TOWARDS_BOTTOM_RIGHT
            isHorizontal && isTowardsHigher && isLTR && isNotReversed -> TOWARDS_BOTTOM_RIGHT
            isHorizontal && isTowardsHigher && isLTR && isReversed -> TOWARDS_TOP_LEFT
            isHorizontal && isTowardsHigher && isRTL && isNotReversed -> TOWARDS_TOP_LEFT
            isHorizontal && isTowardsHigher && isRTL && isReversed -> TOWARDS_BOTTOM_RIGHT
            isHorizontal && isTowardsLower && isLTR && isNotReversed -> TOWARDS_TOP_LEFT
            isHorizontal && isTowardsLower && isLTR && isReversed -> TOWARDS_BOTTOM_RIGHT
            isHorizontal && isTowardsLower && isRTL && isNotReversed -> TOWARDS_BOTTOM_RIGHT
            isHorizontal && isTowardsLower && isRTL && isReversed -> TOWARDS_TOP_LEFT
            else -> throw IllegalStateException("Invalid adapter state.")
        }
    }

    public override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        return computeScrollOffset()
    }

    public override fun computeVerticalScrollRange(state: RecyclerView.State): Int {
        return computeScrollRange()
    }

    public override fun computeVerticalScrollExtent(state: RecyclerView.State): Int {
        return computeScrollExtent()
    }

    public override fun computeHorizontalScrollOffset(state: RecyclerView.State): Int {
        return computeScrollOffset()
    }

    public override fun computeHorizontalScrollRange(state: RecyclerView.State): Int {
        return computeScrollRange()
    }

    public override fun computeHorizontalScrollExtent(state: RecyclerView.State): Int {
        return computeScrollExtent()
    }

    /**
     * Returns the "offset" the view is from the top. This is documented as needed to support
     * scrollbars (which the LoopingLayoutManager does not support), but it is also needed to
     * support TalkBack accessibility gestures. This function returns a constant to ensure that
     * the layout is always scrollable.
     */
    private fun computeScrollOffset(): Int {
        if (childCount == 0) {
            return 0
        }
        return SCROLL_OFFSET
    }

    /**
     * Returns the "range" of scrolling the view supports. This is documented as needed to support
     * scrollbars (which the LoopingLayoutManager does not support), but it is also needed to
     * support TalkBack accessibility gestures. This function returns a constant to ensure that
     * the layout is always scrollable.
     */
    private fun computeScrollRange(): Int {
        if (childCount == 0) {
            return 0
        }
        return SCROLL_RANGE
    }

    /**
     * Returns the "extent" of the view. This is documented as needed to support scrollbars (which
     * the LoopingLayoutManager does not support), but it is also needed to support TalkBack
     * accessibility gestures. This function returns a constant to ensure that the layout is always
     * scrollable.
     */
    private fun computeScrollExtent(): Int {
        return 0
    }

    /**
     * Adds details to the accessibility [event] about the range of views that are being shown
     * so that it can be told to the user.
     */
    public override fun onInitializeAccessibilityEvent(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        event: AccessibilityEvent
    ) {
        super.onInitializeAccessibilityEvent(recycler, state, event)
        if (childCount > 0) {
            event.fromIndex = topLeftIndex
            event.toIndex = bottomRightIndex
        }
    }

    /**
     * Returns the vector that points to where the [targetPosition] can be found.
     *
     * By default it tries to return the direction that will require the least amount of scrolling
     * to get to, but if some views are larger or smaller than other views this may be incorrect.
     *
     * A different function may be provided by assigning it to the [smoothScrollDirectionDecider]
     * property of the [LoopingLayoutManager].
     *
     * This method is used by the [RecyclerView.SmoothScroller] to initiate a scroll towards the
     * target position.
     */
    public override fun computeScrollVectorForPosition(targetPosition: Int): PointF {
        return computeScrollVectorForPosition(targetPosition, itemCount)
    }

    private fun computeScrollVectorForPosition(targetPosition: Int, count: Int): PointF {
        val movementDir = smoothScrollDirectionDecider(targetPosition, this, count)
        return if (orientation == HORIZONTAL) {
            PointF(movementDir.toFloat(), 0F)
        } else {
            PointF(0F, movementDir.toFloat())
        }
    }

    /**
     * Returns the view with the given [adapterIndex].
     *
     * If there are multiple views representing the same adapter position, by default this returns
     * the view whose middle is closest to the middle of the recycler. If you would like to use a
     * different tie-breaker you may pass a function to do so.
     */
    public override fun findViewByPosition(adapterIndex: Int): View? {
        return findViewByPosition(adapterIndex, ::defaultPicker)
    }

    /**
     * Returns the view with the given [adapterIndex]. You must provide a [strategy] to decide which
     * view to return in the case that there are multiple views associated with the same adapter
     * position.
     */
    public fun findViewByPosition(
            adapterIndex: Int,
            strategy: (targetIndex: Int, layoutManager: LoopingLayoutManager) -> View?
    ): View? {
        return strategy(adapterIndex, this)
    }

    /** Returns all laid out views that are associated with the given [adapterIndex]. */
    private fun findAllViewsWithPosition(adapterIndex: Int): Iterable<View> {
        // This could be optimized using the state.itemCount to jump over views we know are not
        // associated with the index. But given the number of views that will be visible at once,
        // that's an over-optimization at this time.
        val views = mutableListOf<View>()
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view != null && getPosition(view) == adapterIndex) {
                views += view
            }
        }
        return views
    }

    /**
     * Returns the adapter index of the view with the *lowest* adapter index that is even slightly
     * visible.
     */
    public fun findFirstVisibleItemPosition(): Int {
        var lowestIndex = Int.MAX_VALUE;
        for (i in 0 until childCount) {
            val view = getChildAt(i);
            if (view != null && getPosition(view) < lowestIndex && viewIsVisible(view))  {
                lowestIndex = getPosition(view)
            }
        }
        return lowestIndex;
    }

    /**
     * Returns the adapter index of the view with the *lowest* adapter index that is fully visible.
     */
    public fun findFirstCompletelyVisibleItemPosition(): Int {
        var lowestIndex = Int.MAX_VALUE;
        for (i in 0 until childCount) {
            val view = getChildAt(i);
            if (view != null && getPosition(view) < lowestIndex && viewIsFullyVisible(view))  {
                lowestIndex = getPosition(view)
            }
        }
        return lowestIndex;
    }

    /**
     * Returns the adapter index of the view with the *highest* adapter index that is even slightly
     * visible.
     */
    public fun findLastVisibleItemPosition(): Int {
        var highestIndex = 0;
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view != null && getPosition(view) > highestIndex && viewIsVisible(view))  {
                highestIndex = getPosition(view)
            }
        }
        return highestIndex;
    }

    /**
     * Returns the adapter index of the view with the *highest* adapter index that is fully visible.
     */
    public fun findLastCompletelyVisibleItemPosition(): Int {
        var highestIndex = 0;
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view != null && getPosition(view) > highestIndex && viewIsFullyVisible(view))  {
                highestIndex = getPosition(view)
            }
        }
        return highestIndex;
    }

    /**
     * Scrolls the layout to make the given [adapterIndex] visible. If a view associated with the
     * index is already entirely visible, nothing will change.
     *
     * By default this will estimate the shortest distance needed to make the view visible. But if
     * some views are smaller or larger than others, the estimation may be incorrect.
     *
     * Note that this change will not be reflected until the next layout call.
     */
    public override fun scrollToPosition(adapterIndex: Int) {
        scrollToPosition(adapterIndex, ::defaultDecider)
    }

    /**
     * Scrolls the layout to make the given [adapterIndex] visible. If a view associated with the
     * index is already entirely visible, nothing will change.
     *
     * The views could be scrolled in either direction to make the target visible, so you must pass
     * a [strategy] to determine which direction the recycler should be moved in. It should return
     * either [TOWARD_TOP_LEFT] or [TOWARD_BOTTOM_RIGHT].
     *
     * Note that this change will not be reflected until the next layout call.
     */
    public fun scrollToPosition(
            adapterIndex: Int,
            strategy: (
                    targetIndex: Int,
                    layoutManager: LoopingLayoutManager,
                    itemCount: Int
            ) -> Int
    ) {
        if (viewWithIndexIsFullyVisible(adapterIndex)) return
        layoutRequest = LayoutRequest(anchorIndex = adapterIndex, scrollStrategy = strategy)
        requestLayout()
    }

    /**
     * Returns true if there is at least one view associated with teh given adapter index that is
     * full visible. False otherwise.
     */
    private fun viewWithIndexIsFullyVisible(adapterIndex: Int): Boolean {
        val views = findAllViewsWithPosition(adapterIndex)
        for (view in views) {
            if (viewIsFullyVisible(view)) {
                return true
            }
        }
        return false
    }

    public override fun smoothScrollToPosition(
            recyclerView: RecyclerView,
            state: RecyclerView.State,
            position: Int
    ) {
        val loopingSmoothScroller = LoopingSmoothScroller(recyclerView.context, state)
        loopingSmoothScroller.targetPosition = position
        startSmoothScroll(loopingSmoothScroller)
    }

    /** Defines a better interface for interacting with views in the context of the LayoutManager. */
    private abstract inner class ListItem(protected val view: View) {
        // The "leading edge" refers to the edge that appears first when scrolling.
        // In the case of a vertical list where you are trying to see items /lower/ in the list,
        // it would be the top edge of the bottom-most view.
        // The "following edge" is the opposite, in the case above that would be the bottom edge.

        /**
         * Returns the size of the part of the view that is hidden (scrolled off screen). Should
         * never be negative.
         */
        public abstract val hiddenSize: Int

        /**
         * Returns the location of the edge of the view that is coming into view first.
         */
        public abstract val leadingEdge: Int

        /**
         * Returns the location of the edge of the view that is coming into view last.
         */
        public abstract val followingEdge: Int

        /**
         * Returns the size of the view along the layout axis (i.e. the width in horizontal
         * mode, the height in vertical mode).
         */
        public abstract val size: Int

        /**
         * Returns a rect defining the position the provided [item] would have if it was
         * position "after" this item.
         *
         * After is defined as towards the top for a bottom leading item, towards the left for a
         * right leading item, etc.
         * @param rect A rect defining the static edges of the layout (i.e. left and right for a
         * vertical layout, top and bottom for a horizontal one).
         */
        public abstract fun getPositionOfItemFollowingSelf(item: ListItem, rect: Rect): Rect

        /**
         * Returns a rect defining the position this item would have if it was positioned at
         * the "start" of the layout.
         *
         * Start is defined as the Leading edge aligned with the same edge of the recycler.
         * This means the top aligned with the top for a top leading item, or the left aligned
         * with the left for a left leading item.
         * @param rect A rect defining the static edges of the layout (i.e. left and right for a
         * vertical layout, top and bottom for a horizontal one).
         * @param hiddenAmount The amount of the view that should be hidden beyond the edge it
         * is being aligned with.
         */
        public abstract fun getPositionOfSelfAsFirst(rect: Rect, hiddenAmount: Int): Rect
    }

    private inner class LeadingLeftListItem(
        view: View
    ) : ListItem(view) {

        public override val hiddenSize: Int
            get() = (getDecoratedRight(view) - (width - paddingRight)).coerceAtLeast(0)

        public override val leadingEdge: Int
            get() = getDecoratedLeft(view)

        public override val followingEdge: Int
            get() = getDecoratedRight(view)

        public override val size: Int
            get() = getDecoratedMeasuredWidth(view)

        public override fun getPositionOfItemFollowingSelf(item: ListItem, rect: Rect): Rect {
            rect.left = followingEdge
            rect.right = rect.left + item.size
            return rect
        }

        public override fun getPositionOfSelfAsFirst(rect: Rect, hiddenAmount: Int): Rect {
            rect.left = paddingLeft - hiddenAmount
            rect.right = rect.left + size
            return rect
        }
    }

    private inner class LeadingTopListItem(
        view: View
    ) : ListItem(view) {

        public override val hiddenSize: Int
            get() = (getDecoratedBottom(view) - (height - paddingBottom)).coerceAtLeast(0)

        public override val leadingEdge: Int
            get() = getDecoratedTop(view)

        public override val followingEdge: Int
            get() = getDecoratedBottom(view)

        public override val size: Int
            get() = getDecoratedMeasuredHeight(view)


        public override fun getPositionOfItemFollowingSelf(item: ListItem, rect: Rect): Rect {
            rect.top = followingEdge
            rect.bottom = rect.top + item.size
            return rect
        }

        public override fun getPositionOfSelfAsFirst(rect: Rect, hiddenAmount: Int): Rect {
            rect.top = paddingTop - hiddenAmount
            rect.bottom = rect.top + size
            return rect
        }
    }

    private inner class LeadingRightListItem(view: View) : ListItem(view) {

        public override val hiddenSize: Int
            get() = (paddingLeft - getDecoratedLeft(view)).coerceAtLeast(0)

        public override val leadingEdge: Int
            get() = getDecoratedRight(view)

        public override val followingEdge: Int
            get() = getDecoratedLeft(view)

        public override val size: Int
            get() = getDecoratedMeasuredWidth(view)

        public override fun getPositionOfItemFollowingSelf(item: ListItem, rect: Rect): Rect {
            rect.right = followingEdge
            rect.left = rect.right - item.size
            return rect
        }

        public override fun getPositionOfSelfAsFirst(rect: Rect, hiddenAmount: Int): Rect {
            rect.right = (width - paddingRight) + hiddenAmount
            rect.left = rect.right - size
            return rect
        }
    }

    private inner class LeadingBottomListItem(view: View) : ListItem(view) {

        public override val hiddenSize: Int
            get() = (paddingTop - getDecoratedTop(view)).coerceAtLeast(0)

        public override val leadingEdge: Int
            get() = getDecoratedBottom(view)

        public override val followingEdge: Int
            get() = getDecoratedTop(view)

        public override val size: Int
            get() = getDecoratedMeasuredHeight(view)

        public override fun getPositionOfItemFollowingSelf(item: ListItem, rect: Rect): Rect {
            rect.bottom = followingEdge
            rect.top = rect.bottom - item.size
            return rect
        }

        public override fun getPositionOfSelfAsFirst(rect: Rect, hiddenAmount: Int): Rect {
            rect.bottom = (height - paddingBottom) + hiddenAmount
            rect.top = rect.bottom - size
            return rect
        }
    }

    /**
     * A smooth scroller that supports the LoopingLayoutManager's two (at the time of writing) quirks:
     *    1) By default the layout manager only lays out visible views.
     *    2) The layout manager must be given the state.itemCount to properly calculate
     *       a scroll vector.
     */
    private inner class LoopingSmoothScroller(
            val context: Context,
            val state: RecyclerView.State
    ) : LinearSmoothScroller(context) {

        /**
         * Tells the LoopingLayoutManager to start laying out extra (i.e. not visible) views. This
         * allows the target view to be found before it becomes visible, which helps with smooth
         * deceleration.
         */
        public override fun onStart() {
            // Based on the Material Design Guidelines, 500 ms should be plenty of time to decelerate.
            val rate = calculateSpeedPerPixel(context.resources.displayMetrics)  // MS/Pixel
            val time = 500  // MS.
            (layoutManager as LoopingLayoutManager).extraLayoutSpace = (rate * time).toInt()
        }

        /**
         * Tells the LoopingLayoutManager to stop laying out extra views, b/c there's no need
         * to lay out views the user can't see.
         */
        public override fun onStop() {
            (layoutManager as LoopingLayoutManager).extraLayoutSpace = 0
        }

        public override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
            val layoutManager = layoutManager  // Enables smart cast.
            if (layoutManager is LoopingLayoutManager) {
                return layoutManager.computeScrollVectorForPosition(targetPosition, state.itemCount)
            }
            Log.w(TAG, "A LoopingSmoothScroller should only be attached to a LoopingLayoutManager.")
            return null
        }
    }

    /**
     * Holds the data necessary to re-layout the recycler.
     */
    private class LayoutRequest() : Parcelable {
        /**
         * The target adapter index we want to layout at the edge associated with the adapterDirection
         */
        public var anchorIndex: Int = RecyclerView.NO_POSITION
            get() {
                if (!hasBeenInitialized) throw Exception("LayoutRequest has not been initialized.")
                return field
            }
            private set

        /**
         * The amount (in pixels) of the view associated with the anchorIndex that should be hidden.
         */
        public var scrollOffset: Int = 0
            get() {
                if (!hasBeenInitialized) throw Exception("LayoutRequest has not been initialized.")
                return field
            }
            private set

        /**
         * Tells us which edge the view associated with the [anchorIndex] should be laid out at. If
         * it is [TOWARDS_LOWER_INDICES] the view will be laid out at the edge where the view
         * associated with the zero adapter index was originally laid out at. If it is
         * [TOWARDS_HIGHER_INDICES] it will be the opposite edge.
         */
        public var adapterDirection: Int = TOWARDS_LOWER_INDICES
            get() {
                if (!hasBeenInitialized) throw Exception("LayoutRequest has not been initialized.")
                return field
            }
            private set

        /**
         * A directional decider used to pick a direction to "move" in if one was not provided
         * explicitly.
         *
         * This value cannot be parceled.
         */
        private var scrollStrategy: ((Int, LoopingLayoutManager, Int) -> Int)? = null

        /**
         * Has the layout request been initialized to make sure all of its public vars are valid?
         */
        private var hasBeenInitialized = false

        public constructor(parcel: Parcel) : this() {
            anchorIndex = parcel.readInt()
            scrollOffset = parcel.readInt()
            adapterDirection = parcel.readInt()
        }

        public constructor(
                anchorIndex: Int = RecyclerView.NO_POSITION,
                scrollOffset: Int = 0,
                adapterDirection: Int = TOWARDS_LOWER_INDICES,
                scrollStrategy: ((Int, LoopingLayoutManager, Int) -> Int)? = null,
                layoutManager: LoopingLayoutManager? = null,
                state: RecyclerView.State? = null
        ) : this() {
            this.anchorIndex = anchorIndex
            this.scrollOffset = scrollOffset
            this.adapterDirection = adapterDirection
            this.scrollStrategy = scrollStrategy

            if (layoutManager != null && state != null) initialize(layoutManager, state)

            if (!hasBeenInitialized
                    && anchorIndex != RecyclerView.NO_POSITION
                    && scrollStrategy == null) {
                hasBeenInitialized = true
            }
        }

        /**
         * Makes sure that all of this LayoutRequests public variables are valid.
         */
        public fun initialize(layoutManager: LoopingLayoutManager, state: RecyclerView.State) {
            if (hasBeenInitialized) return
            hasBeenInitialized = true
            // If this is executing a scrollTo, the anchorIndex will be set, but the
            // adapterDirection still needs to be decided.
            adapterDirection = scrollStrategy?.invoke(anchorIndex, layoutManager, state.itemCount)?.let {
                    layoutManager.getAdapterDirectionFromMovementDirection(it) }
                    ?: adapterDirection
            // If this is an adapter data update, the adapterDirection will be set but the
            // anchorIndex and scrollOffset still need to be decided.
            if (anchorIndex == RecyclerView.NO_POSITION) {
                if (layoutManager.childCount == 0) {
                    anchorIndex = 0
                } else {
                    val direction = layoutManager.getMovementDirectionFromAdapterDirection(adapterDirection)
                    anchorIndex = layoutManager.getInitialIndex(direction)
                    scrollOffset = layoutManager.getInitialItem(direction).hiddenSize
                }
            }
        }

        /**
         * Resets this layout request to a default layout request so that the information can be
         * re-initialized if onLayoutChildren gets called.
         */
        public fun finishProcessing() {
            anchorIndex = RecyclerView.NO_POSITION
            scrollOffset = 0
            adapterDirection = TOWARDS_LOWER_INDICES
            scrollStrategy = null
            hasBeenInitialized = false
        }

        public override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(anchorIndex)
            parcel.writeInt(scrollOffset)
            parcel.writeInt(adapterDirection)
        }

        public override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<LayoutRequest> {
            public override fun createFromParcel(parcel: Parcel): LayoutRequest {
                return LayoutRequest(parcel)
            }

            public override fun newArray(size: Int): Array<LayoutRequest?> {
                return Array(size) { LayoutRequest() }
            }
        }
    }

    companion object {

        private const val TAG = "LoopingLayoutManager"
        private const val DEBUG = false

        public const val HORIZONTAL = OrientationHelper.HORIZONTAL
        public const val VERTICAL = OrientationHelper.VERTICAL

        /**
         * Describes the user scrolling towards the top/left of the screen. NOTE: this does *not*
         * describe the direction views are moving in. The user is trying to see new views at the
         * top/left.
         */
        public const val TOWARDS_TOP_LEFT = -1
        /**
         * Describes the user scrolling towards the bottom/right of the screen. NOTE: this does
         * *not* describe the direction views are moving in. The user is trying to see new views
         * at the bottom/right.
         */
        public const val TOWARDS_BOTTOM_RIGHT = 1

        /**
         * Describes the direction we need to traverse view indices in to get to larger adapter indices.
         * In this case we need to traverse the views backwards (Max -> 0) to get to higher adapter
         * indices.
         */
        public const val TOWARDS_LOWER_INDICES = -1
        /**
         * Describes the direction we need to traverse view indices in to get to larger adapter indices.
         * In this case we need to traverse the views forwards (0 -> Max) to get to higher adapter
         * indices.
         */
        public const val TOWARDS_HIGHER_INDICES = 1

        /**
         * A constant returned by the [computeScrollOffset] function so that accessibility knows
         * the layout is always scrollable.
         *
         */
        private const val SCROLL_OFFSET = 100
        /**
         * A constant returned by the [computeScrollRange] function so that accessibility knows
         * the layout is always scrollable.
         */
        private const val SCROLL_RANGE = 200
    }

}
