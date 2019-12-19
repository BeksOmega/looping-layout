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
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import kotlin.math.abs

class LoopingLayoutManager : LayoutManager, RecyclerView.SmoothScroller.ScrollVectorProvider {

    /**
     * When LayoutManager needs to scroll to a position, it sets this variable and requests a
     * layout which will check this variable and re-layout accordingly.
     */
    private var mPendingScrollPosition = RecyclerView.NO_POSITION
    /**
     * When the layout manager needs to scroll to a position (via scroll to position), it needs
     * some method to decide which direction to scroll in. This variable stores that method.
     */
    private var mPendingScrollStrategy: (Int, LoopingLayoutManager, Int) -> Int = ::defaultDecider

    /**
     * The amount of extra (i.e. not visible) space to fill up with views after we have filled up
     * the visible space. This is used during smooth scrolling, so that the target view can be found
     * before it becomes visible (helps with smooth deceleration).
     */
    private var extraLayoutSpace = 0

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
     * When the layout manager needs to scroll to a position (via smooth scrolling) it needs some
     * method to decide which direction to scroll in. This variable stores that method.
     */
    var smoothScrollDirectionDecider: (Int, LoopingLayoutManager, Int) -> Int = ::defaultDecider
    
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
        if (mPendingScrollPosition < 0 || mPendingScrollPosition >= state.itemCount) {
            return;
        }

        var layoutRect = nonScrollingEdges

        // Movement direction. Either TOWARDS_TOP_LEFT or TOWARDS_BOTTOM_RIGHT.
        val direction = mPendingScrollStrategy(mPendingScrollPosition, this, state.itemCount)
        var index = getInitialIndex(direction)
        var selectedItem = getInitialItem(direction)

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
        mPendingScrollPosition = RecyclerView.NO_POSITION
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
        scrapNonVisibleViews(recycler)
        val absDelta = abs(delta)
        var amountScrolled = 0
        var index = getInitialIndex(direction)
        var selectedItem = getInitialItem(direction)
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

        // The amount of extra (i.e not visible) space currently covered by views.
        var viewSpace = selectedItem.hiddenSize
        while(viewSpace < extraLayoutSpace) {
            // We don't want the topLeftIndex or bottomRightIndex to reflect non-visible views.
            index = stepIndex(index, direction, state, updateIndex = false)
            val newView = createViewForIndex(index, direction, recycler)
            val newItem = getItemForView(direction, newView)
            layoutRect = selectedItem.getPositionOfItemFollowingSelf(newItem, layoutRect)
            layoutDecorated(newView, layoutRect.left, layoutRect.top,
                    layoutRect.right, layoutRect.bottom)
            selectedItem = newItem
            viewSpace += selectedItem.size
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
     * Returns the view (wrapped in a ListItem) closest to where new views will be shown.
     * For example, if the user is trying to see new views at the top, this will return the
     * top-most view.
     * @param direction The direction the list is being scrolled in. Either [.TOWARDS_TOP_LEFT]
     * or [.TOWARDS_BOTTOM_RIGHT]
     * @return The view (wrapped in a ListItem) closest to where new views will be shown.
     */
    private fun getInitialItem(direction: Int): ListItem {
        val initialView = if (direction == TOWARDS_TOP_LEFT) getChildAt(0)
        else getChildAt(childCount - 1)
        // initialView should never be null, so we'll just ask for an exception.
        return getItemForView(direction, initialView!!)
    }

    /**
     * Increments/decrements the provided adapter index based on the direction the list is being
     * moved in. For example, if the list is being scrolled towards items with higher adapter
     * indices the index will be incremented.
     *
     * Also (by default) handles updating [.topLeftIndex] or [.bottomRightIndex] to reflect the
     * newest view. This functionality can be disabled by passing "false" to the updateIndex parameter.
     *
     * @param index The adapter index of the view closest to where new views will be shown.
     * @param direction The direction the list is being scrolled in. Either [.TOWARDS_TOP_LEFT]
     * or [.TOWARDS_BOTTOM_RIGHT]
     * @param state The current state of the RecyclerView this LayoutManager is attached to.
     * @param updateIndex If true, the [.topLeftIndex] or [.bottomRightIndex] will be updated to
     * reflect the new index. If false, they will not be updated. True by default.
     * @return The stepped index.
     */
    private fun stepIndex(
            index: Int,
            direction: Int,
            state: RecyclerView.State,
            updateIndex: Boolean = true
    ): Int {
        val adapterDirection = getAdapterDirectionFromMovementDirection(direction)
        val count = state.itemCount

        val isTowardsTopLeft = direction == TOWARDS_TOP_LEFT
        val isTowardsBottomRight = direction == TOWARDS_BOTTOM_RIGHT
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
     * Sends any currently non-visible (i.e. not within the visible bounds of the recycler) views
     * to the scrap heap. Used by scrollBy to make sure we're only dealing with visible views before
     * adding new ones.
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
        // The first visible item will bump us to zero.
        var distanceFromStart = -1
        var foundVisibleView = false
        var foundHiddenView = false

        // We want to loop through the views in the order opposite the direction of movement.
        val range = if (direction == TOWARDS_TOP_LEFT) {
            0 until childCount
        } else {
            childCount-1 downTo 0
        }

        // Ignore hidden views at the start. Only recycle hidden views at the end.
        for (i in range) {
            val view = getChildAt(i) ?: break
            if (viewIsVisible(view)) {
                if (!foundVisibleView) {
                    foundVisibleView = true
                }
                distanceFromStart++
            } else if (foundVisibleView){
                foundHiddenView = true
                removeAndRecycleViewAt(i, recycler)
            }
        }

        if (!foundHiddenView) {
            // If we didn't find anything that needed to be disposed, no indices need to be updated.
            return
        }

        // We need to flip the direction, since we looped through views in the opposite order.
        // When we flip the movement direction, the adapter direction will be flipped as well.
        val adapterDirection = getAdapterDirectionFromMovementDirection(direction * -1)
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
     * Checks if the view is fully within the visible bounds of the recycler (along the layout
     * axis - fully visible horizontally in horizontal mode, fully visible vertically in vertical
     * mode).
     * @param view The view to check the visibility of.
     * @return True if the view is fully visible along the layout axis, false otherwise.
     */
    private fun viewIsFullyVisible(view: View): Boolean {
        return if (orientation == HORIZONTAL) {
            getDecoratedLeft(view) >= paddingLeft && getDecoratedRight(view) <= width - paddingRight
        } else {
            getDecoratedTop(view) >= paddingTop && getDecoratedBottom(view) <= height - paddingBottom
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
     * Calculates the vector that points to where the target position can be found.
     *
     * By default it tries to return the direction that will require the least amount of scrolling
     * to get to, but if some views are larger or smaller than other views this may be incorrect.
     *
     * A different function may be provided by assigning it to the smoothScrollDirectionDecider
     * property of the LoopingLayoutManager.
     *
     * This method is used by the LayoutManager's SmoothScroller to initiate a scroll towards the
     * target position.
     * @param targetPosition The target position to which the returned vector should point
     * @return The vector which points towards the given target position.
     */
    override fun computeScrollVectorForPosition(targetPosition: Int): PointF {
        return computeScrollVectorForPosition(targetPosition, itemCount)
    }

    /**
     * Calculates the vector that points to where the target position can be found.
     *
     * By default it tries to return the direction that will require the least amount of scrolling
     * to get to, but if some views are larger or smaller than other views this may be incorrect.
     *
     * A different function may be provided by assigning it to the smoothScrollDirectionDecider
     * property of the LoopingLayoutManager.
     *
     * This method is used by the LayoutManager's SmoothScroller to initiate a scroll towards the
     * target position.
     * @param targetPosition The target position to which the returned vector should point
     * @param count The current state.itemCount.
     * @return The vector which points towards the given target position.
     */
    fun computeScrollVectorForPosition(targetPosition: Int, count: Int): PointF {
        val direction = smoothScrollDirectionDecider(targetPosition, this, count)
        return if (orientation == HORIZONTAL) {
            PointF(direction.toFloat(), 0F)
        } else {
            PointF(0F, direction.toFloat())
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
        return findViewByPosition(adapterIndex, ::defaultPicker)
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
     * @param adapterIndex The adapter index we want to find views associated with.
     * @return All views associated with the given adapter index.
     */
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
                    itemCount: Int
            ) -> Int
    ) {
        if (viewWithIndexIsFullyVisible(adapterIndex)) return
        mPendingScrollPosition = adapterIndex
        mPendingScrollStrategy = strategy
        requestLayout()
    }

    /**
     * @return True if there is at least one view associated with the given index that is fully
     * visible. False otherwise.
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

    override fun smoothScrollToPosition(
            recyclerView: RecyclerView,
            state: RecyclerView.State,
            position: Int
    ) {
        val loopingSmoothScroller = LoopingSmoothScroller(recyclerView.context, state)
        loopingSmoothScroller.targetPosition = position
        startSmoothScroll(loopingSmoothScroller)
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

    /**
     * A smooth scroller that supports the looping layout managers two (at the time of writing) quirks:
     *    1) By default the layout manager only lays out visible views.
     *    2) The layout manager must be given the state.itemCount to properly calculate
     *       a scroll vector.
     */
    private inner class LoopingSmoothScroller(
            val context: Context,
            val state: RecyclerView.State
    ) : LinearSmoothScroller(context) {

        override fun onStart() {
            // Based on the Material Design Guidelines, 500 ms should be plenty of time to decelerate.
            val rate = calculateSpeedPerPixel(context.resources.displayMetrics)  // MS/Pixel
            val time = 500  // MS.
            (layoutManager as LoopingLayoutManager).extraLayoutSpace = (rate * time).toInt()
        }

        override fun onStop() {
            (layoutManager as LoopingLayoutManager).extraLayoutSpace = 0
        }

        override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
            val layoutManager = layoutManager  // Enables smart cast.
            if (layoutManager is LoopingLayoutManager) {
                return layoutManager.computeScrollVectorForPosition(targetPosition, state.itemCount)
            }
            Log.w(TAG, "A LoopingSmoothScroller should only be attached to a LoopingLayoutManager.")
            return null
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
