package com.bekawestberg.loopinglayout.test

import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
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

    internal var TAG = "ScrollToAtAnchor"
    // The width of the item associated with adapter index 0.
    private val TARGET_SIZE = 100
    // Makes the filler view a little bit wider than the recycler so that
    // the target is totally hidden.
    private val EXTRA_FILLER_SIZE = 50

    private val HORIZ = RecyclerView.HORIZONTAL;
    private val VERT = RecyclerView.VERTICAL;


    @get:Rule
    var activityRule = ActivityTestRule(ActivityGeneric::class.java)

    /*
     * Test naming info:
     * 1) horiz/vert: Whether the test is for a horizontal or vertical layout.
     * 2) ltr/rtl: Whether the test is for a left-to-right or right-to-left layout.
     * 3) notRev/rev: Whether the layout is "reverse" from how it would normally layout. Eg in ltr
     *    mode the side where the adapter item at index 0 would normally be laid out is left. But
     *    in reversed mode, it is laid out on the right.
     * 4) partVis/notVis: Whether the target item we want to scroll to is partially visible, or not
     *    visible. There is no different logic for these cases. this is just intended to match the
     *    estimateShortestDistance tests.
     * 5) anchor/optAnchor: Refers to which size the view is on when we initiate the scroll to it.
     *    If "anchor" that means we want the view to be aligned with the side where the 0 adapter
     *    item was originally laid out. If "optAnchor", the opposite.
     */

    /*
     * View diagram info:
     * |: Denotes the edges of the screen. Eg |---- A ----| Shows that the "A" view takes up exactly
     *    the size of the screen.
     * -: Represents the size of the view. Eg ----- A ----- B - Shows that "A" is a large view and
     *    "B" is a small one.
     *
     * Notes:
     *   - Vertical recyclers are also diagrammed horizontally. Left = Top, Right = Bottom.
     */

    @Test
    fun horiz_ltr_notRev_partVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        // |- T ---- F -|--
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Make T only partially visible on the left.
                // -|T ---- F --|-
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE / 2))
                // |- T ---- F -|--
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_ltr_notRev_partVis_optAnchor() {
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_partVis(HORIZ), TARGET_SIZE))
        // |---- F ---- T|-
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // |- T ---- F -|--
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_notRev_notVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_notVis(HORIZ)))
        // |- T ----- F -|---
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Make T just outside the right edge.
                // |----- F ----|- T -
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE + EXTRA_FILLER_SIZE))
                // |- T ----- F -|---
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_ltr_notRev_notVis_optAnchor() {
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_notVis(HORIZ), TARGET_SIZE))
        // |----- F ----|- T -
        val layoutManager = setLayoutManager(HORIZ, false)

        onView(withId(R.id.recycler))
                // |- T ----- F -|---
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_rev_partVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        // --|- F ---- T -|
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Make T only partially visible on the right.
                // -|-- F ---- T|-
                .perform(RecyclerViewActions.scrollBy(x = -TARGET_SIZE / 2))
                // --|- F ---- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_rev_partVis_optAnchor() {
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_partVis(HORIZ), TARGET_SIZE))
        // -|T ---- F ----|
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // --|- F ---- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_ltr_rev_notVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_notVis(HORIZ)))
        // ---|- F ----- T -|
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Make T just outside the left edge.
                // - T -|---- F -----|
                .perform(RecyclerViewActions.scrollBy(x = -(TARGET_SIZE + EXTRA_FILLER_SIZE)))
                // ---|- F ----- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_rev_notVis_optAnchor() {
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_notVis(HORIZ), TARGET_SIZE))
        // - T -|---- F -----|
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // ---|- F ----- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_rtl_notRev_partVis_anchor() {
        setRtl()
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        // --|- F ---- T -|
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Make T only partially visible on the right.
                // -|-- F ---- T|-
                .perform(RecyclerViewActions.scrollBy(x = -TARGET_SIZE / 2))
                // --|- F ---- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_rtl_notRev_partVis_optAnchor() {
        setRtl()
        // -|T ---- F ----|
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_partVis(HORIZ), TARGET_SIZE))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // --|- F ---- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_rtl_notRev_notVis_anchor() {
        setRtl()
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_notVis(HORIZ)))
        // ---|- F ----- T -|
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Make T just outside the left edge.
                // - T -|---- F -----|
                .perform(RecyclerViewActions.scrollBy(x = -(TARGET_SIZE + EXTRA_FILLER_SIZE)))
                // ---|- F ----- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_rtl_notRev_notVis_optAnchor() {
        setRtl()
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_notVis(HORIZ), TARGET_SIZE))
        // - T -|---- F -----|
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // ---|- F ----- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_rtl_rev_partVis_anchor() {
        setRtl()
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        // |- T ---- F -|--
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Make T only partially visible on the left.
                // -|T ---- F --|-
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE / 2))
                // |- T ---- F -|--
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_rtl_rev_partVis_optAnchor() {
        setRtl()
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_partVis(HORIZ), TARGET_SIZE))
        val layoutManager = setLayoutManager(HORIZ, true)
        // |---- F ---- T|-
        onView(withId(R.id.recycler))
                // |- T ---- F -|--
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_rtl_rev_notVis_anchor() {
        setRtl()
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_notVis(HORIZ)))
        // |- T ----- F -|---
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Make T just outside the right edge.
                // |----- F ----|- T -
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE + EXTRA_FILLER_SIZE))
                // |- T ----- F -|---
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_rtl_rev_notVis_optAnchor() {
        setRtl()
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_notVis(HORIZ), TARGET_SIZE))
        val layoutManager = setLayoutManager(HORIZ, true)
        // |----- F ----|- T -
        onView(withId(R.id.recycler))
                // |- T ----- F -|---
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun vert_notRev_partVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(VERT)))
        // |- T ---- F -|--
        val layoutManager = setLayoutManager(VERT, false)

        onView(withId(R.id.recycler))
                // Make T only partially visible at the top.
                // -|T ---- F --|-
                .perform(RecyclerViewActions.scrollBy(y = TARGET_SIZE / 2))
                // |- T ---- F -|--
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun vert_notRev_partVis_optAnchor() {
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_partVis(VERT), TARGET_SIZE))
        // |---- F ---- T|-
        val layoutManager = setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                // |- T ---- F -|--
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun vert_notRev_notVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_notVis(VERT)))
        // |- T ----- F -|---
        val layoutManager = setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                // Make T just outside the bottom edge.
                // |----- F ----|- T -
                .perform(RecyclerViewActions.scrollBy(y = EXTRA_FILLER_SIZE))
                // |- T ----- F -|---
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun vert_notRev_notVis_optAnchor() {
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_notVis(VERT), TARGET_SIZE))
        // |----- F ----|- T -
        val layoutManager = setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                // |- T ----- F -|---
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun vert_rev_partVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(VERT)))
        // --|- F ---- T -|
        val layoutManager = setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                // Make T only partially visible on the bottom.
                // -|-- F ---- T|-
                .perform(RecyclerViewActions.scrollBy(y = -TARGET_SIZE / 2))
                // --|- F ---- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun vert_rev_partVis_optAnchor() {
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_partVis(VERT), TARGET_SIZE))
        // -|T ---- F ----|
        val layoutManager = setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                // --|- F ---- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun vert_rev_notVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_notVis(VERT)))
        // ---|- F ----- T -|
        val layoutManager = setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                // Make T just outside the top edge.
                // - T -|---- F -----|
                .perform(RecyclerViewActions.scrollBy(y = -(TARGET_SIZE + EXTRA_FILLER_SIZE)))
                // ---|- F ----- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun vert_rev_notVis_optAnchor() {
        setAdapter(arrayOf("F", "T"), arrayOf(fillerSize_notVis(VERT), TARGET_SIZE))
        // - T -|---- F -----|
        val layoutManager = setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                // ---|- F ----- T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(1, ::addViewsAtAnchorEdge))

        onView(withText("T"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    fun fillerSize_partVis(orientation: Int): Int {
        val activity = activityRule.activity
        val linearLayout = activity.findViewById<LinearLayout>(R.id.main_activity) ?: return 0
        return if (orientation == HORIZ) {
            linearLayout.width - TARGET_SIZE / 2
        } else {
            linearLayout.height - TARGET_SIZE / 2
        }
    }

    fun fillerSize_notVis(orientation: Int): Int {
        val activity = activityRule.activity
        val linearLayout = activity.findViewById<LinearLayout>(R.id.main_activity) ?: return 0
        return if (orientation == HORIZ) {
            linearLayout.width + EXTRA_FILLER_SIZE
        } else {
            linearLayout.height + EXTRA_FILLER_SIZE
        }
    }
}
