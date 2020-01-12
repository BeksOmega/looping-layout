package com.bekawestberg.loopinglayout.test

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.PositionAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import com.bekawestberg.loopinglayout.test.androidTest.utils.RecyclerViewActions
import com.bekawestberg.loopinglayout.test.androidTest.utils.setAdapter
import com.bekawestberg.loopinglayout.test.androidTest.utils.setLayoutManager
import com.bekawestberg.loopinglayout.test.androidTest.utils.setRtl
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ScrollToEstimatedShortestTest {

    internal var TAG = "ExampleInstrumentedTest"
    // The width of the item associated with adapter index 0.
    private val targetSize = 100
    // Only show half the item when testing partial visibility.
    private val targetVisiblePortion = targetSize / 2
    // Makes the view that fills the screen a little bit wider than the recycler so that
    // the target is totally hidden.
    private val fillerViewExtraPortion = 100
    // Used to center the filler view so that there is equal extra on either side.
    private val halfFillerViewExtraPortion = fillerViewExtraPortion / 2
    // A constant size to use for views that are not the filler view, and are not the target view.
    private val otherViewSize = 100


    @get:Rule
    var activityRule = ActivityTestRule(ActivityGeneric::class.java)

    @Test
    fun defaultHorizontalPartiallyVisibleAtAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun defaultHorizontalPartiallyVisibleAtOptAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultHorizontalNotVisibleAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun defaultHorizontalNotVisibleAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultHorizontalNotVisibleOptAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun defaultHorizontalNotVisibleOptAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        x = (targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalPartiallyVisibleAtAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalPartiallyVisibleAtOptAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun reverseHorizontalNotVisibleAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalNotVisibleAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun reverseHorizontalNotVisibleOptAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalNotVisibleOptAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        x = -(targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun defaultHorizontalRtlPartiallyVisibleAtAnchor() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultHorizontalRtlPartiallyVisibleAtOptAnchor() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun defaultHorizontalRtlNotVisibleAnchorWithin() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultHorizontalRtlNotVisibleAnchorSeam() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun defaultHorizontalRtlNotVisibleOptAnchorWithin() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultHorizontalRtlNotVisibleOptAnchorSeam() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        x = -(targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun reverseHorizontalRtlPartiallyVisibleAtAnchor() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun reverseHorizontalRtlPartiallyVisibleAtOptAnchor() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalRtlNotVisibleAnchorWithin() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun reverseHorizontalRtlNotVisibleAnchorSeam() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalRtlNotVisibleOptAnchorWithin() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun reverseHorizontalRtlNotVisibleOptAnchorSeam() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        x = (targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultVerticalPartiallyVisibleAtAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun defaultVerticalPartiallyVisibleAtOptAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = (targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultVerticalNotVisibleAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = (targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun defaultVerticalNotVisibleAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultVerticalNotVisibleOptAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = (otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun defaultVerticalNotVisibleOptAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        y = (targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseVerticalPartiallyVisibleAtAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseVerticalPartiallyVisibleAtOptAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -(targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun reverseVerticalNotVisibleAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -(targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseVerticalNotVisibleAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun reverseVerticalNotVisibleOptAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -(otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseVerticalNotVisibleOptAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        y = -(targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    fun calculateFillerSizeWhenPartiallyVisible(orientation: Int): Int {
        val activity = activityRule.activity
        val recycler = activity.findViewById<RecyclerView>(R.id.recycler) ?: return 0
        return if (orientation == RecyclerView.HORIZONTAL) {
            recycler.width
        } else {
            recycler.height
        }
    }

    fun calculateFillerSizeWhenNotVisible(orientation: Int): Int {
        val activity = activityRule.activity
        val recycler = activity.findViewById<RecyclerView>(R.id.recycler) ?: return 0
        return if (orientation == RecyclerView.HORIZONTAL) {
            recycler.width + fillerViewExtraPortion
        } else {
            recycler.height + fillerViewExtraPortion
        }
    }
}
