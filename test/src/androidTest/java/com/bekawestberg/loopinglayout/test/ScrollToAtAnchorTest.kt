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
import com.bekawestberg.loopinglayout.library.addViewsAtAnchorEdge
import com.bekawestberg.loopinglayout.test.androidTest.utils.RecyclerViewActions
import com.bekawestberg.loopinglayout.test.androidTest.utils.ViewAssertions.isBottomAlignedWithPadding
import com.bekawestberg.loopinglayout.test.androidTest.utils.ViewAssertions.isLeftAlignedWithPadding
import com.bekawestberg.loopinglayout.test.androidTest.utils.ViewAssertions.isRightAlignedWithPadding
import com.bekawestberg.loopinglayout.test.androidTest.utils.ViewAssertions.isTopAlignedWithPadding
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
class ScrollToAtAnchorTest {

    internal var TAG = "ExampleInstrumentedTest"
    // The width of the item associated with adapter index 0.
    private val targetSize = 100
    // Only show half the item when testing partial visibility.
    private val targetVisiblePortion = targetSize / 2
    // Makes the non-target view a little bit wider than the recycler so that
    // the target is totally hidden.
    private val nonTargetExtraPortion = 50


    @get:Rule
    var activityRule = ActivityTestRule(ActivityGeneric::class.java)

    @Test
    fun defaultHorizontalPartiallyVisibleAtAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun defaultHorizontalPartiallyVisibleAtOptAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultHorizontalNotVisibleAtAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = targetSize + nonTargetExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun defaultHorizontalNotVisibleAtOptAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)

        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalPartiallyVisibleAtAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalPartiallyVisibleAtOptAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun reverseHorizontalNotVisibleAtAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(targetSize + nonTargetExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalNotVisibleAtOptAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun defaultHorizontalRtlPartiallyVisibleAtAnchor() {
        setRtl()
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultHorizontalRtlPartiallyVisibleAtOptAnchor() {
        setRtl()
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun defaultHorizontalRtlNotVisibleAtAnchor() {
        setRtl()
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(targetSize + nonTargetExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultHorizontalRtlNotVisibleAtOptAnchor() {
        setRtl()
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun reverseHorizontalRtlPartiallyVisibleAtAnchor() {
        setRtl()
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun reverseHorizontalRtlPartiallyVisibleAtOptAnchor() {
        setRtl()
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalRtlNotVisibleAtAnchor() {
        setRtl()
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = nonTargetExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun reverseHorizontalRtlNotVisibleAtOptAnchor() {
        setRtl()
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.HORIZONTAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultVerticalPartiallyVisibleAtAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)

        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun defaultVerticalPartiallyVisibleAtOptAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultVerticalNotVisibleAtAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = nonTargetExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun defaultVerticalNotVisibleAtOptAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseVerticalPartiallyVisibleAtAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseVerticalPartiallyVisibleAtOptAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenPartiallyVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun reverseVerticalNotVisibleAtAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(targetSize, nonTargetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -(targetSize + nonTargetExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("0"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseVerticalNotVisibleAtOptAnchor() {
        val nonTargetSize = calculateNonTargetSizeWhenNotVisible(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(nonTargetSize, targetSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("1"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    fun calculateNonTargetSizeWhenPartiallyVisible(orientation: Int): Int {
        val activity = activityRule.activity
        val recycler = activity.findViewById<RecyclerView>(R.id.recycler) ?: return 0
        return if (orientation == RecyclerView.HORIZONTAL) {
            recycler.width - targetVisiblePortion
        } else {
            recycler.height - targetVisiblePortion
        }
    }

    fun calculateNonTargetSizeWhenNotVisible(orientation: Int): Int {
        val activity = activityRule.activity
        val recycler = activity.findViewById<RecyclerView>(R.id.recycler) ?: return 0
        return if (orientation == RecyclerView.HORIZONTAL) {
            recycler.width + nonTargetExtraPortion
        } else {
            recycler.height + nonTargetExtraPortion
        }
    }
}
