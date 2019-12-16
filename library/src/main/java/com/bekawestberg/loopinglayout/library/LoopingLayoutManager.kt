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
import android.graphics.Rect
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class LoopingLayoutManager : LayoutManager {

    /**
     * When LayoutManager needs to scroll to a position, it sets this variable and requests a
     * layout which will check this variable and re-layout accordingly.
     */
    private var mPendingScrollPosition = RecyclerView.NO_POSITION
    /**
     * When the layout manager needs to scroll to a position, it needs some method to decide which
     * direction to scroll in. This variable stores that method.
     */
    private var mPendingScrollStrategy: (Int, LoopingLayoutManager, RecyclerView.State) -> Int =
            ::defaultDecider

    /**
     * @return A Rect populated with the positions of the static edges of the layout. I.e. right
     * and left in horizontal mode, top and bottom in vertical mode.
     */
    private val nonScrollingEdges: Rect
        get() {
            val layoutRect = Rect()
            if (orientation == VERTICAL) {
                layoutRect.left = paddingLeft
                layoutRect.right = width - paddingRight
            } else {
                layoutRect.top = paddingTop
                layoutRect.bottom = height - paddingBottom
            }
            return layoutRect
        }
    
    /**
     * Describes the adapter index of the view in the top/left -most position.
     */
    var topLeftIndex = 0
            private set
    /**
     * Describes the adapter index of the view in the bottom/right -most position.
     */
    var bottomRightIndex = 0
            private set
    
    /**
     * Creates a LoopingLayout manager with the given orientation and reverse layout option.
     * @param context Current context, will be used to access resources.
     * @param orientation Layout orientation. Should be [.HORIZONTAL] or [                      ][.VERTICAL].
     * @param reverseLayout When set to true, lays out items in the opposite direction from default.
     */
    @JvmOverloads
    constructor(context: Context, orientation: Int = VERTICAL, reverseLayout: Boolean = false) {
        this.orientation = orientation
        this.reverseLayout = reverseLayout
    }

    /**
     * Constructor used when layout manager is set in XML by RecyclerView attribute
     * "layoutManager". Defaults to vertical orientation.
     */
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) {
        val properties = getProperties(context, attrs, defStyleAttr, defStyleRes)
        orientation = properties.orientation
        reverseLayout = properties.reverseLayout
    }

    /**
     * Describes the current orientation of this layout manager. Either [.HORIZONTAL] or [.VERTICAL].
     */
    var orientation: Int = 0
        set(orientation) {
            require(orientation == HORIZONTAL || orientation == VERTICAL) {
                "invalid orientation:$orientation"
            }
            if (orientation == this.orientation) {
                return
            }
            assertNotInLayoutOrScroll(null)
            field = orientation
            requestLayout()
        }

    /**
     * Describes whether the views are laid out in the opposite order they would normally be laid
     * out in.
     *
     * The "normal order" is based on the orientation and RTL/LTR state. For example if this
     * was in a RTL Horizontal state, and this property were true, the views would be laid out
     * from left to right.
     */
    var reverseLayout = false
        set(reverseLayout) {
            if (reverseLayout == this.reverseLayout) {
                return
            }
            assertNotInLayoutOrScroll(null)
            field = reverseLayout
            requestLayout()
        }

    /**
     * @return True if we are in RTL mode, false otherwise.
     */
    val isLayoutRTL: Boolean
        get() = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL

    override fun isAutoMeasureEnabled(): Boolean {
        return true;
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (mPendingScrollPosition != RecyclerView.NO_POSITION) {
            layoutToPosition(recycler, state)
            return
        }
        layoutAnew(recycler, state)
    }

    /**
     * Lays out the views by scrapping the current ones (if any exist) and starting from scratch.
     */
    private fun layoutAnew(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        detachAndScrapAttachedViews(recycler)
        var layoutRect = nonScrollingEdges

        val direction = getMovementDirectionFromAdapterDirection(TOWARDS_HIGHER_INDICES)
        var prevItem: ListItem? = null
        val size = if (orientation == HORIZONTAL) width else height
        var sizeFilled = 0
        var index = 0
        while (sizeFilled < size) {
            val view = createViewForIndex(index, direction, recycler)
            val item = getItemForView(direction, view)
            layoutRect = prevItem?.getPositionOfItemFollowingSelf(item, layoutRect) ?:
                    item.getPositionOfSelfAsFirst(layoutRect)
            layoutDecorated(view, layoutRect.left, layoutRect.top,
                    layoutRect.right, layoutRect.bottom)

            index = index.loopedIncrement(state.itemCount)
            sizeFilled += item.size
            prevItem = item
        }

        if (direction == TOWARDS_TOP_LEFT) {
            bottomRightIndex = 0
            topLeftIndex = index.loopedDecrement(state.itemCount)
        } else {
            topLeftIndex = 0
            bottomRightIndex = index.loopedDecrement(state.itemCount)
        }
    }

    /**
     * Lays out items until it reaches the target index ([.mPendingScrollPosition]).
     *
     * The position new items are added in is determined by the [.mPendingScrollStrategy].
     *
     * @see [.scrollToPosition]
     */
    private fun layoutToPosition(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (mPendingScrollPosition < 0 || mPendingScrollPosition > state.itemCount) {
            return;
        }

        var layoutRect = nonScrollingEdges

        val direction = mPendingScrollStrategy(mPendingScrollPosition, this, state)
        var index = getInitialIndex(direction)
        val initialView = if (direction == TOWARDS_TOP_LEFT) getChildAt(0)
                else getChildAt(childCount - 1)
        var selectedItem = getItemForView(direction, initialView!!)

        val initialHiddenSize = selectedItem.hiddenSize
        offsetChildren(initialHiddenSize * -direction)

        while (index != mPendingScrollPosition) {
            index = stepIndex(index, direction, state)
            val newView = createViewForIndex(index, direction, recycler)
            val newItem = getItemForView(direction, newView)
            layoutRect = selectedItem.getPositionOfItemFollowingSelf(newItem, layoutRect)
            layoutDecorated(newView, layoutRect.left, layoutRect.top,
                    layoutRect.right, layoutRect.bottom)
            selectedItem = newItem
            val hiddenSize = selectedItem.hiddenSize
            offsetChildren(hiddenSize * -direction)
        }
        recycleViews(direction, recycler, state)
    }

    override fun canScrollVertically(): Boolean {
        return orientation == VERTICAL
    }

    /**
     * Scroll vertically by dy pixels in screen coordinates and return the distance traveled.
     * The default implementation does nothing and returns 0.
     *
     * @param dy distance to scroll in pixels. Y increases as scroll position
     *         approaches the bottom.
     * @param recycler Recycler to use for fetching potentially cached views for a
     *         position
     * @param state Transient state of RecyclerView
     * @return The actual distance scrolled. This will be equal to delta unless the layout manager
     * does not have children, in which case it will be zero. Other layout managers may
     * return less than the delta if they hit a bound, but the LoopingLayoutManager has no
     * bounds.
     */
    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        return scrollBy(dy, recycler, state)
    }

    override fun canScrollHorizontally(): Boolean {
        return orientation == HORIZONTAL
    }

    /**
     * Scroll horizontally by dx pixels in screen coordinates and return the distance traveled.
     * The default implementation does nothing and returns 0.
     *
     * @param dx distance to scroll by in pixels. X increases as scroll position
     *          approaches the right.
     * @param recycler Recycler to use for fetching potentially cached views for a
     *          position
     * @param state Transient state of RecyclerView
     * @return The actual distance scrolled. This will be equal to delta unless the layout manager
     * does not have children, in which case it will be zero. Other layout managers may
     * return less than the delta if they hit a bound, but the LoopingLayoutManager has no
     * bounds.
     */
    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        return scrollBy(dx, recycler, state)
    }

    /**
     * Scrolls the list of views by the given delta. Creates and binds new views if necessary.
     * Whether to scroll horizontally or vertically is determined by the orientation.
     * @param delta The amount to move the views by.
     * @param recycler The recycler this LayoutManager is attached to.
     * @param state The current recycler view state.
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
        var layoutRect = nonScrollingEdges

        val direction = Integer.signum(delta)
        val absDelta = abs(delta)
        var amountScrolled = 0
        var index = getInitialIndex(direction)
        val initialView = if (direction == TOWARDS_TOP_LEFT) getChildAt(0)
                else getChildAt(childCount - 1)
        // initialView should never be null, so we'll just ask for an exception.
        var selectedItem = getItemForView(direction, initialView!!)
        while (amountScrolled < absDelta) {
            val hiddenSize = selectedItem.hiddenSize
            // Scroll just enough to complete the scroll, or bring the view fully into view.
            val amountToScroll = hiddenSize.coerceAtMost(absDelta - amountScrolled)
            amountScrolled += amountToScroll
            offsetChildren(amountToScroll * -direction)
            if (amountScrolled < absDelta) {
                index = stepIndex(index, direction, state)
                val newView = createViewForIndex(index, direction, recycler)
                val newItem = getItemForView(direction, newView)
                layoutRect = selectedItem.getPositionOfItemFollowingSelf(newItem, layoutRect)
                layoutDecorated(newView, layoutRect.left, layoutRect.top,
                        layoutRect.right, layoutRect.bottom)
                selectedItem = newItem
            }
        }
        recycleViews(direction, recycler, state)
        return amountScrolled * direction
    }

    /**
     * Creates, measures, and inserts a view into the recycler. This prepares it to be properly
     * positioned.
     * @param index The adapter index we want to associate a new view with.
     * @param direction The current direction the view is moving in. Either [.TOWARDS_TOP_LEFT]
     * or [.TOWARDS_BOTTOM_RIGHT].
     * @param recycler The RecyclerView this LayoutManager is attached to.
     * @return A newly created view that is ready to be positioned.
     */
    private fun createViewForIndex(
        index: Int,
        direction: Int,
        recycler: RecyclerView.Recycler
    ): View {
        val newView = recycler.getViewForPosition(index)
        if (direction == TOWARDS_TOP_LEFT) {
            addView(newView, 0)
        } else {
            addView(newView)
        }
        measureChildWithMargins(newView, 0, 0)
        return newView
    }

    /**
     * Moves all child views by the given amount (can be positive or negative). Determines whether
     * they are moved horizontally or vertically based on the orientation.
     * @param amount The amount to move the views by.
     */
    private fun offsetChildren(amount: Int) {
        if (orientation == HORIZONTAL) {
            offsetChildrenHorizontal(amount)
        } else {
            offsetChildrenVertical(amount)
        }
    }

    /**
     * Returns the adapter index of the view closest to where new views will be shown. For example,
     * if the user is trying to see new views at the top, this will return the adapter index of the
     * top-most view.
     * @param direction The direction the list is being scrolled in. Either [.TOWARDS_TOP_LEFT]
     * or [.TOWARDS_BOTTOM_RIGHT]
     * @return The adapter index of the view closest to where new views will be shown.
     */
    private fun getInitialIndex(direction: Int): Int {
        return if (direction == TOWARDS_TOP_LEFT) {
            topLeftIndex
        } else {
            bottomRightIndex
        }
    }

    /**
     * Increments/decrements the provided adapter index based on the direction the list is being
     * moved in. For example, if the list is being scrolled towards items with higher adapter
     * indices the index will be incremented.
     *
     * Also handles updating [.topLeftIndex] or [.bottomRightIndex] to reflect the
     * newest view.
     * @param index The adapter index of the view closest to where new views will be shown.
     * @param direction The direction the list is being scrolled in. Either [.TOWARDS_TOP_LEFT]
     * or [.TOWARDS_BOTTOM_RIGHT]
     * @param state The current state of the RecyclerView this LayoutManager is attached to.
     * @return The stepped index.
     */
    private fun stepIndex(index: Int, direction: Int, state: RecyclerView.State): Int {
        val adapterDirection = getAdapterDirectionFromMovementDirection(direction)
        val count = state.itemCount

        val isTowardsTopLeft = direction == TOWARDS_TOP_LEFT
        val isTowardsBottomRight = direction == TOWARDS_BOTTOM_RIGHT
        val isTowardsHigherIndices = adapterDirection == TOWARDS_HIGHER_INDICES
        val isTowardsLowerIndices = adapterDirection == TOWARDS_LOWER_INDICES

        when {
            isTowardsTopLeft && isTowardsHigherIndices -> {
                topLeftIndex = index.loopedIncrement(count)
                return topLeftIndex
            }
            isTowardsTopLeft && isTowardsLowerIndices -> {
                topLeftIndex = index.loopedDecrement(count)
                return topLeftIndex
            }
            isTowardsBottomRight && isTowardsHigherIndices -> {
                bottomRightIndex = index.loopedIncrement(count)
                return bottomRightIndex
            }
            isTowardsBottomRight && isTowardsLowerIndices -> {
                bottomRightIndex = index.loopedDecrement(count)
                return bottomRightIndex
            }
            else -> throw IllegalStateException("Invalid move & adapter direction combination.")
        }
    }

    /**
     * Returns the direction we are moving through the adapter (Either
     * [.TOWARDS_HIGHER_INDICES] or [.TOWARDS_LOWER_INDICES]) based on the direction
     * the list is being scrolled in, and the current layout settings.
     * @param direction The direction the list is being scrolled in. Either [.TOWARDS_TOP_LEFT]
     * or [.TOWARDS_BOTTOM_RIGHT]
     * @return the direction we are moving through the adapter. Either
     * [.TOWARDS_HIGHER_INDICES] or [.TOWARDS_LOWER_INDICES].
     */
    private fun getAdapterDirectionFromMovementDirection(direction: Int): Int {
        val isVertical = orientation == VERTICAL
        val isHorizontal = !isVertical
        val isTowardsTopLeft = direction == TOWARDS_TOP_LEFT
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
     * Converts an adapter direction ([.TOWARDS_HIGHER_INDICES] or [.TOWARDS_LOWER_INDICES]) to
     * a movement direction. A movement direction tells us which direction we should traverse
     * the views in (first -> last or last -> first) so that we are traversing in the given
     * adapter direction.
     * @param direction The direction we want to traverse the adapter indices in.
     * Either [.TOWARDS_HIGHER_INDICES] or [.TOWARDS_LOWER_INDICES].
     * @return The direction we need to traverse the views in to get to adapter indices in the
     * given direction.
     */
    fun convertAdapterDirToMovementDir(direction: Int): Int {
        return getMovementDirectionFromAdapterDirection(direction)
    }

    /**
     * Returns the direction we need to move the views in to get to adapter indices in the
     * given direction.
     * @param direction The direction we want to traverse the adapter indices in.
     * Either [.TOWARDS_HIGHER_INDICES] or [.TOWARDS_LOWER_INDICES].
     * @return The direction we need to move the views in to get to adapter indices in the
     * given direction.
     */
    private fun getMovementDirectionFromAdapterDirection(direction: Int): Int {
        val isVertical = orientation == VERTICAL
        val isHorizontal = !isVertical
        val isTowardsHigher = direction == TOWARDS_HIGHER_INDICES
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

    /**
     * Returns the view wrapped in the correct ListItem based on the movement direction and
     * configuration of the LayoutManager.
     *
     * ListItems give the view an interface that's more usable when writing a LayoutManager.
     * @param direction The direction the view is moving in either @link{TOWARDS_TOP_LEFT}
     * or @link{TOWARDS_BOTTOM_RIGHT}
     * @param view The view to wrap.
     * @return A ListItem that wrapps the view.
     */
    private fun getItemForView(direction: Int, view: View): ListItem {
        val isVertical = orientation == VERTICAL
        val isHorizontal = !isVertical
        val isTowardsTopLeft = direction == TOWARDS_TOP_LEFT
        val isTowardsBottomRight = !isTowardsTopLeft

        return when {
            isVertical && isTowardsTopLeft -> LeadingBottomListItem(view)
            isVertical && isTowardsBottomRight -> LeadingTopListItem(view, height - paddingBottom)
            isHorizontal && isTowardsTopLeft -> LeadingRightListItem(view)
            isHorizontal && isTowardsBottomRight -> LeadingLeftListItem(view, width - paddingRight)
            else -> throw IllegalStateException("Invalid movement state.")
        }
    }

    /**
     * Recycles views that are no longer visible given the direction of the scroll that was just
     * completed.
     *
     * @param direction The direction the recycler is being scrolled in.
     * @param recycler The recycler we are removing views from.
     * @param state The state of the recycler.
     */
    private fun recycleViews(
        direction: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ) {
        val initialIndex = getInitialIndex(direction)
        // We want to loop through the views in the order opposite the direction of movement.
        val oppositeMoveDir = direction * -1
        // The start item will bump us to zero.
        var distanceFromStart = -1
        var foundHiddenView = false

        val range = if (oppositeMoveDir == TOWARDS_BOTTOM_RIGHT) {
            0 until childCount
        } else {
            childCount-1 downTo 0
        }

        for (i in range) {
            val view = getChildAt(i) ?: break
            if (viewIsVisible(view)) {
                distanceFromStart++
            } else {
                foundHiddenView = true
                removeAndRecycleViewAt(i, recycler)
            }
        }

        if (!foundHiddenView) {
            // If we didn't find anything that needed to be disposed, no indices need to be updated.
            return
        }

        // Since we already flipped the direction, the adapter direction will be flipped as well.
        val adapterDirection = getAdapterDirectionFromMovementDirection(oppositeMoveDir)
        val changeInPosition = adapterDirection * distanceFromStart
        val count = state.itemCount
        if (direction == TOWARDS_TOP_LEFT) {
            bottomRightIndex = initialIndex.loop(changeInPosition, count)
        } else {
            topLeftIndex = initialIndex.loop(changeInPosition, count)
        }
    }

    /**
     * Checks if any part of the view is within the visible bounds of the recycler.
     * @param view The view to check the visibility of.
     * @return True if the view is at least partially visible, false otherwise.
     */
    private fun viewIsVisible(view: View): Boolean {
        return if (orientation == HORIZONTAL) {
            getDecoratedRight(view) >= 0 && getDecoratedLeft(view) <= width
        } else {
            getDecoratedBottom(view) >= 0 && getDecoratedTop(view) <= height
        }
    }

    /**
     * Finds the view with the given adapter position.
     *
     * If there are multiple views representing the same adapter position, this returns the
     * view whose middle is closest to the middle of the recycler. If you would like to use a
     * different tie-breaker you may pass a function to do so.
     * @param adapterIndex The adapter index of the view we want to find.
     * @return A view with the given adapter position.
     */
    override fun findViewByPosition(adapterIndex: Int): View? {
        return findViewByPosition(adapterIndex, ::childClosestToMiddle);
    }

    /**
     * Finds the view with the given adapter position. You must provide a function to decide which
     * view to return in the case that there are multiple views associated with the same adapter
     * position.
     * @param adapterIndex The adapter index of the view we want to find.
     * @param strategy The strategy for determining which view to return.
     * @return A view with the given adapter position.
     */
    fun findViewByPosition(
            adapterIndex: Int,
            strategy: (targetIndex: Int, layoutManager: LoopingLayoutManager) -> View?
    ): View? {
        return strategy(adapterIndex, this)
    }

    /**
     * @return All views associated with the given adapter index.
     */
    private fun findAllViewsWithPosition(adapterIndex: Int): Iterable<View> {
        val views = mutableListOf<View>()
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view != null && getPosition(view) == adapterIndex) {
                views += view
            }
        }
        return views;
    }


    /**
     * Scrolls the layout to make the given position visible. If a view associated with the index
     * is already entirely visible, nothing will change.
     *
     * By default this will estimate the shortest distance needed to make the view visible. But if
     * some views are smaller or larger than others, the estimation may be incorrect.
     *
     * Note that this change will not be reflected until the next layout call.
     * @param adapterIndex The adapter index to make visible.
     */
    override fun scrollToPosition(adapterIndex: Int) {
        scrollToPosition(adapterIndex, ::defaultDecider)
    }

    /**
     * Scrolls the layout to make the given position visible. If a view associated with the index
     * is already entirely visible, nothing will change.
     *
     * The views could be scrolled in either direction to make the target visible, so you must pass
     * a function to determine which direction the recycler should be moved in. It should return
     * either [.TOWARD_TOP_LEFT] or [.TOWARD_BOTTOM_RIGHT].
     * @param adapterIndex The adapter index to make visible.
     * @param strategy The strategy used to determine which direction to move the views in.
     */
    fun scrollToPosition(
            adapterIndex: Int,
            strategy: (
                    targetIndex: Int,
                    layoutManager: LoopingLayoutManager,
                    state: RecyclerView.State
            ) -> Int
    ) {
        val views = findAllViewsWithPosition(adapterIndex)
        for (view in views) {
            val left = getDecoratedLeft(view)
            val top = getDecoratedTop(view)
            val right = getDecoratedRight(view)
            val bottom = getDecoratedBottom(view)

            val rRight = width - paddingRight
            val rBottom = height - paddingBottom
            Log.v(TAG, "$left $top $right $bottom \n $paddingLeft $paddingTop $rRight $rBottom")
            if (getDecoratedLeft(view) >= paddingLeft &&
                    getDecoratedTop(view) >= paddingTop &&
                    getDecoratedRight(view) <= width - paddingRight &&
                    getDecoratedBottom(view) <= height - paddingBottom) {
                return;  // At least one view is fully visible.
            }
        }
        mPendingScrollPosition = adapterIndex
        mPendingScrollStrategy = strategy
        requestLayout()
    }

    /**
     * Defines a better interface for interacting with views in the context of the LayoutManager.
     */
    private abstract inner class ListItem(protected val view: View) {
        // The "leading edge" refers to the edge that appears first when scrolling.
        // In the case of a vertical list where you are trying to see items /lower/ in the list,
        // it would be the top edge of the bottom-most view.
        // The "following edge" is the opposite, in the case above that would be the bottom edge.

        /**
         * Returns the size of the part of the view that is hidden (scrolled off screen). Should
         * never be negative.
         * @return The size of the part of the view that is hidden.
         */
        abstract val hiddenSize: Int

        /**
         * Returns the location of the edge of the view that is coming into view first.
         * @return The location of the leading edge of the view.
         */
        abstract val leadingEdge: Int

        /**
         * Returns the location of the edge of the view that is coming into view last.
         * @return The location of the following edge of the view.
         */
        abstract val followingEdge: Int

        /**
         * Returns the size of the view in the movement direction (i.e. the width in horizontal
         * mode, the height in vertical mode).
         * @return The size of the view in the movement direction.
         */
        abstract val size: Int

        /**
         * Calculates a rect defining the position the provided item would have if it was
         * position "after" this item.
         *
         * After is defined as towards the top for a bottom leading item, towards the left for a
         * right leading item, etc.
         * @param item The item to position "after" this item.
         * @param rect A rect defining the static edges of the layout (i.e. left and right for a
         * vertical layout, top and bottom for a horizontal one).
         * @return A rect defining what the position of the provided item should be.
         */
        abstract fun getPositionOfItemFollowingSelf(item: ListItem, rect: Rect): Rect

        /**
         * Calculates a rect defining the position this item would have if it was positioned at
         * the "start" of the layout.
         *
         * Start is defined as the Leading edge aligned with the same edge of the recycler.
         * This means the top aligned with the top for a top leading item, or the left aligned
         * with the left for a left leading item.
         * @param rect A rect defining the static edges of the layout (i.e. left and right for a
         * vertical layout, top and bottom for a horizontal one).
         * @return A rect defining what the position of this item should be.
         */
        abstract fun getPositionOfSelfAsFirst(rect: Rect): Rect
    }

    private inner class LeadingLeftListItem(
        view: View,
        private val mListRight: Int
    ) : ListItem(view) {

        override val hiddenSize: Int
            get() = (getDecoratedRight(view) - mListRight).coerceAtLeast(0)

        override val leadingEdge: Int
            get() = getDecoratedLeft(view)

        override val followingEdge: Int
            get() = getDecoratedRight(view)

        override val size: Int
            get() = getDecoratedMeasuredWidth(view)

        override fun getPositionOfItemFollowingSelf(item: ListItem, rect: Rect): Rect {
            rect.left = followingEdge
            rect.right = rect.left + item.size
            return rect
        }

        override fun getPositionOfSelfAsFirst(rect: Rect): Rect {
            rect.left = paddingLeft
            rect.right = rect.left + size
            return rect
        }
    }

    private inner class LeadingTopListItem(
        view: View,
        private val mListBottom: Int
    ) : ListItem(view) {

        override val hiddenSize: Int
            get() = (getDecoratedBottom(view) - mListBottom).coerceAtLeast(0)

        override val leadingEdge: Int
            get() = getDecoratedTop(view)

        override val followingEdge: Int
            get() = getDecoratedBottom(view)

        override val size: Int
            get() = getDecoratedMeasuredHeight(view)


        override fun getPositionOfItemFollowingSelf(item: ListItem, rect: Rect): Rect {
            rect.top = followingEdge
            rect.bottom = rect.top + item.size
            return rect
        }

        override fun getPositionOfSelfAsFirst(rect: Rect): Rect {
            rect.top = paddingTop
            rect.bottom = rect.top + size
            return rect
        }
    }

    private inner class LeadingRightListItem(view: View) : ListItem(view) {

        override val hiddenSize: Int
            get() = (-getDecoratedLeft(view)).coerceAtLeast(0)

        override val leadingEdge: Int
            get() = getDecoratedRight(view)

        override val followingEdge: Int
            get() = getDecoratedLeft(view)

        override val size: Int
            get() = getDecoratedMeasuredWidth(view)

        override fun getPositionOfItemFollowingSelf(item: ListItem, rect: Rect): Rect {
            rect.right = followingEdge
            rect.left = rect.right - item.size
            return rect
        }

        override fun getPositionOfSelfAsFirst(rect: Rect): Rect {
            rect.right = width - paddingRight
            rect.left = rect.right - size
            return rect
        }
    }

    private inner class LeadingBottomListItem(view: View) : ListItem(view) {

        override val hiddenSize: Int
            get() = (-getDecoratedTop(view)).coerceAtLeast(0)

        override val leadingEdge: Int
            get() = getDecoratedBottom(view)

        override val followingEdge: Int
            get() = getDecoratedTop(view)

        override val size: Int
            get() = getDecoratedMeasuredHeight(view)

        override fun getPositionOfItemFollowingSelf(item: ListItem, rect: Rect): Rect {
            rect.bottom = followingEdge
            rect.top = rect.bottom - item.size
            return rect
        }

        override fun getPositionOfSelfAsFirst(rect: Rect): Rect {
            rect.bottom = height - paddingBottom
            rect.top = rect.bottom - size
            return rect
        }
    }

    companion object {

        private const val TAG = "LoopingLayoutManager"
        private const val DEBUG = false

        const val HORIZONTAL = OrientationHelper.HORIZONTAL
        const val VERTICAL = OrientationHelper.VERTICAL

        /**
         * Describes the user scrolling towards the top/left of the screen. NOTE: this does *not*
         * describe the direction views are moving in. The user is trying to see new views at the
         * top/left.
         */
        const val TOWARDS_TOP_LEFT = -1
        /**
         * Describes the user scrolling towards the bottom/right of the screen. NOTE: this does
         * *not* describe the direction views are moving in. The user is trying to see new views
         * at the bottom/right.
         */
        const val TOWARDS_BOTTOM_RIGHT = 1

        /**
         * Describes the direction we need to traverse view indices in to get to larger adapter indices.
         * In this case we need to traverse the views backwards (Max -> 0) to get to higher adapter
         * indices.
         */
        const val TOWARDS_LOWER_INDICES = -1
        /**
         * Describes the direction we need to traverse view indices in to get to larger adapter indices.
         * In this case we need to traverse the views forwards (0 -> Max) to get to higher adapter
         * indices.
         */
        const val TOWARDS_HIGHER_INDICES = 1
    }

}
