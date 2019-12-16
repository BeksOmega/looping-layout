package com.bekawestberg.loopinglayout.test

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.PositionAssertions.*
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import com.bekawestberg.loopinglayout.library.addViewsAtAnchorEdge
import com.bekawestberg.loopinglayout.test.androidTest.utils.RecyclerViewActions
import com.bekawestberg.loopinglayout.test.androidTest.utils.setAdapter
import com.bekawestberg.loopinglayout.test.androidTest.utils.setLayoutManager
import com.bekawestberg.loopinglayout.test.androidTest.utils.setRtl
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep

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
    private val halfFillerViewExtraPortion = fillerViewExtraPortion / 2
    private val otherViewSize = 100


    @get:Rule
    var activityRule = ActivityTestRule(ActivityGeneric::class.java)

    @Test
    fun defaultHorizontalPartiallyVisibleAtAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultHorizontalPartiallyVisibleAtOptAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultHorizontalNotVisibleAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultHorizontalNotVisibleAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultHorizontalNotVisibleOptAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultHorizontalNotVisibleOptAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        x = (targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalPartiallyVisibleAtAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalPartiallyVisibleAtOptAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalNotVisibleAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalNotVisibleAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalNotVisibleOptAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalNotVisibleOptAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        x = -(targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultHorizontalRtlPartiallyVisibleAtAnchor() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultHorizontalRtlPartiallyVisibleAtOptAnchor() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultHorizontalRtlNotVisibleAnchorWithin() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultHorizontalRtlNotVisibleAnchorSeam() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultHorizontalRtlNotVisibleOptAnchorWithin() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultHorizontalRtlNotVisibleOptAnchorSeam() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        x = -(targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalRtlPartiallyVisibleAtAnchor() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalRtlPartiallyVisibleAtOptAnchor() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalRtlNotVisibleAnchorWithin() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalRtlNotVisibleAnchorSeam() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isLeftAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalRtlNotVisibleOptAnchorWithin() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseHorizontalRtlNotVisibleOptAnchorSeam() {
        setRtl()
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        x = (targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isRightAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultVerticalPartiallyVisibleAtAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultVerticalPartiallyVisibleAtOptAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = (targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultVerticalNotVisibleAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = (targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultVerticalNotVisibleAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultVerticalNotVisibleOptAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = (otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun defaultVerticalNotVisibleOptAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        y = (targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseVerticalPartiallyVisibleAtAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseVerticalPartiallyVisibleAtOptAnchor() {
        val fillerSize = calculateFillerSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -(targetSize + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseVerticalNotVisibleAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, fillerSize, otherViewSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -(targetSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseVerticalNotVisibleAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, otherViewSize, targetSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isBottomAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseVerticalNotVisibleOptAnchorWithin() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(otherViewSize, fillerSize, targetSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -(otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
    }

    @Test
    fun reverseVerticalNotVisibleOptAnchorSeam() {
        val fillerSize = calculateFillerSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(targetSize, otherViewSize, fillerSize))
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        y = -(targetSize + otherViewSize + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check(isTopAlignedWith(withId(R.id.recycler)))
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
