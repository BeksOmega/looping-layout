package com.bekawestberg.loopinglayout.test

import android.widget.LinearLayout
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
class ScrollToEstimatedShortestTest {

    internal var TAG = "ExampleInstrumentedTest"
    // The width of the item associated with adapter index 0.
    private val TARGET_SIZE = 100
    // Makes the view that fills the screen a little bit wider than the recycler so that
    // the target is totally hidden.
    private val extraFillerSize = 100
    // A constant size to use for views that are not the filler view, and are not the target view.
    private val OTHER_SIZE = 100

    private val HORIZ = RecyclerView.HORIZONTAL;
    
    // Only show half the item when testing partial visibility.
    private val targetVisiblePortion = TARGET_SIZE / 2
    // Makes the view that fills the screen a little bit wider than the recycler so that
    // the target is totally hidden.
    private val fillerViewExtraPortion = 100
    // Used to center the filler view so that there is equal extra on either side.
    private val halfFillerViewExtraPortion = fillerViewExtraPortion / 2


    @get:Rule
    var activityRule = ActivityTestRule(ActivityGeneric::class.java)

    /*
     * Test Naming info:
     * 1) horiz/vert: Whether the test is for a horizontal or vertical layout.
     * 2) ltr/rtl: Whether the test is for a left-to-right or right-to-left layout.
     * 3) notRev/rev: Whether the layout is "reverse" from how it would normally layout. Eg in ltr
     *    mode the side where the adapter item at index 0 would normally be laid out is left. But
     *    in reversed mode, it is laid out on the right.
     * 4) partVis/noVis: Whether the target item we want to scroll to is partially visible, or not
     *    visible. The estimateShortestDistance decider has different behavior in these cases.
     * 5) anchor/optAnchor: Refers to which side want the target to be aligned with after the scroll
     *    is completed. If "anchor" that means we want the view to be aliged with the side where the
     *    0 adapter item was originally laid out. If "optAnchor", the opposite.
     * 6) overSeam/inLoop: Whether in scrolling the items we should pass over the seam (where the
     *    where the 0 item touches the last item), or stay within the loop. Only applies if noVis.
     */

    /*
     * Note that for doing the "partVis" tests we only need two items.
     * But for the "noVis" tests we need 3, because the decider assumes all views are the same size.
     */

    /*
     * View naming info:
     * T: Target view.
     * F: Filler view
     *   FB: Big filler (takes up screen or more)
     *   FS: Small filler (takes up equal to target)
     */

    @Test
    fun horiz_ltr_notRev_partVis_anchor() {
        // T = index 0, F = index 1
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Move the items left so half the target is obscured.
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE / 2))
                // Focus on the target again (should move items right).
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_ltr_notRev_partVis_optAnchor() {
        // T = index 0, F = index 1
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Move the items left so that the target is not visible, then half the width again
                // so that the target becomes visible on the right
                .perform(RecyclerViewActions.scrollBy(x = (TARGET_SIZE + TARGET_SIZE / 2)))
                // Focus on the target again (should move items left).
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_notRev_notVis_anchor_overSeam() {
        // FB = index 0, FS = index 1, T = index 2
        setAdapter(arrayOf("FB", "FS", "T"),
                arrayOf(fillerSize_notVis(HORIZ), OTHER_SIZE, TARGET_SIZE))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Focus on the target view. Should move items right, over the seam between F1 (0)
                // and T (2), because of F2 in the other direction.
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_notRev_notVis_anchor_inLoop() {
        // T = index 0, FB = index 1, FS = index 2
        setAdapter(arrayOf("T", "FB", "FS"),
                arrayOf(TARGET_SIZE, fillerSize_notVis(HORIZ), OTHER_SIZE))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Move the items left so that the target is not visible.
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE))
                // Focus on the target view. Should move items right (T <- FB) instead of
                // (FB -> FS -> T).
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_ltr_notRev_noVis_optAnchor_overSeam() {
        // T = index 0, FS = index 1, FB = index 2
        setAdapter(arrayOf("T", "FS", "FB"),
                arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize_notVis(HORIZ)))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Move items left so that that target and the small filler are not visible.
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE + OTHER_SIZE))
                // Focus on the target view. Should move items left (over the seam between FB
                // and T) instead of (T <- FS <- FB).
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        // Top left should be FB = index 2
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_notRev_notVis_optAnchor_inLoop() {
        // FS = index 0, FB = index 1, T = index 2
        setAdapter(arrayOf("FS", "FB", "T"),
                arrayOf(OTHER_SIZE, fillerSize_notVis(HORIZ), OTHER_SIZE))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Focus on the target view. Should move items left (FB -> T) instead of
                // (T <- FS <- FB), even though this is technically further due to the FB's extra
                // size.
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        // Top left should be FB = index 2
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun horiz_ltr_rev_partVis_anchor() {
        // T = index 0, F = index 1
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        // T is initially on the right.
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Move items right so that the target is only partially visible.
                .perform(RecyclerViewActions.scrollBy(x = -TARGET_SIZE / 2))
                // Focus on the target view. Should move items left.
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_rev_partVis_optAnchor() {
        // T = index 0, F = index 1
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        val layoutManager = setLayoutManager(HORIZ, true)
        // T is initially on the right
        onView(withId(R.id.recycler))
                // Move the items left so that the target is not visible, then half the width again
                // so that the target becomes visible on the left.
                .perform(RecyclerViewActions.scrollBy(x = -(TARGET_SIZE + TARGET_SIZE / 2)))
                // Focus on the target view. Should move items right.
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_ltr_rev_noVis_anchor_inLoop() {
        // T = index 0, FB = index 1, FS = index 2
        setAdapter(arrayOf("T", "FB", "FS"),
                arrayOf(TARGET_SIZE, fillerSize_notVis(HORIZ), OTHER_SIZE))
        // T is initially on the right
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Move items right so the target is not visible.
                .perform(RecyclerViewActions.scrollBy(x = -TARGET_SIZE))
                // Focus on the target view. Should move items left.
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_rev_noVis_anchor_overSeam() {
        // FB = index 0, FS = index 1, T = index 2
        setAdapter(arrayOf("FB", "FS", "T"),
                arrayOf(fillerSize_notVis(HORIZ), OTHER_SIZE, TARGET_SIZE))
        // Layout from left-to-right looks like T - FS ---|----- FB --------|
        // With T and FS hidden.
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Focus on the target view. Should move items left, crossing over the seam between
                // FB (index 0) and T (index 2)
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        // Layout should look like FS -----|-- FB -------- T|
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun horiz_ltr_rev_noVis_optAnchor_inLoop() {
        // FS = index 0, FB = index 1, T = index 2
        setAdapter(arrayOf("FS", "FB", "T"),
                arrayOf(OTHER_SIZE, fillerSize_notVis(HORIZ), TARGET_SIZE))
        // Layout should look like T -----|-- FB -------- FS|
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Move items right so that FS is not visible.
                // Layout should look like T ---|---- FB --------| FS
                .perform(RecyclerViewActions.scrollBy(x = -OTHER_SIZE))
                // Focus on the target view. Should move items right.
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        // Layout should look like |T -------- FB --|----- FS
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun reverseHorizontalNotVisibleOptAnchorSeam() {
        val fillerSize = fillerSize_notVis(HORIZ)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize))
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        x = -(TARGET_SIZE + OTHER_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun defaultHorizontalRtlPartiallyVisibleAtAnchor() {
        setRtl()
        val fillerSize = fillerSize_partVis(HORIZ)
        setAdapter(arrayOf("0", "1"), arrayOf(TARGET_SIZE, fillerSize))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultHorizontalRtlPartiallyVisibleAtOptAnchor() {
        setRtl()
        val fillerSize = fillerSize_partVis(HORIZ)
        setAdapter(arrayOf("0", "1"), arrayOf(TARGET_SIZE, fillerSize))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(TARGET_SIZE + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun defaultHorizontalRtlNotVisibleAnchorWithin() {
        setRtl()
        val fillerSize = fillerSize_notVis(HORIZ)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(TARGET_SIZE, fillerSize, OTHER_SIZE))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(TARGET_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultHorizontalRtlNotVisibleAnchorSeam() {
        setRtl()
        val fillerSize = fillerSize_notVis(HORIZ)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, OTHER_SIZE, TARGET_SIZE))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun defaultHorizontalRtlNotVisibleOptAnchorWithin() {
        setRtl()
        val fillerSize = fillerSize_notVis(HORIZ)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(OTHER_SIZE, fillerSize, TARGET_SIZE))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = -(OTHER_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultHorizontalRtlNotVisibleOptAnchorSeam() {
        setRtl()
        val fillerSize = fillerSize_notVis(HORIZ)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize))
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        x = -(TARGET_SIZE + OTHER_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun reverseHorizontalRtlPartiallyVisibleAtAnchor() {
        setRtl()
        val fillerSize = fillerSize_partVis(HORIZ)
        setAdapter(arrayOf("0", "1"), arrayOf(TARGET_SIZE, fillerSize))
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun reverseHorizontalRtlPartiallyVisibleAtOptAnchor() {
        setRtl()
        val fillerSize = fillerSize_partVis(HORIZ)
        setAdapter(arrayOf("0", "1"), arrayOf(TARGET_SIZE, fillerSize))
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (TARGET_SIZE + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalRtlNotVisibleAnchorWithin() {
        setRtl()
        val fillerSize = fillerSize_notVis(HORIZ)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(TARGET_SIZE, fillerSize, OTHER_SIZE))
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (TARGET_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun reverseHorizontalRtlNotVisibleAnchorSeam() {
        setRtl()
        val fillerSize = fillerSize_notVis(HORIZ)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, OTHER_SIZE, TARGET_SIZE))
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseHorizontalRtlNotVisibleOptAnchorWithin() {
        setRtl()
        val fillerSize = fillerSize_notVis(HORIZ)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(OTHER_SIZE, fillerSize, TARGET_SIZE))
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(x = (OTHER_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun reverseHorizontalRtlNotVisibleOptAnchorSeam() {
        setRtl()
        val fillerSize = fillerSize_notVis(HORIZ)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize))
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        x = (TARGET_SIZE + OTHER_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultVerticalPartiallyVisibleAtAnchor() {
        val fillerSize = fillerSize_partVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(TARGET_SIZE, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun defaultVerticalPartiallyVisibleAtOptAnchor() {
        val fillerSize = fillerSize_partVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(TARGET_SIZE, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = (TARGET_SIZE + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultVerticalNotVisibleAnchorWithin() {
        val fillerSize = fillerSize_notVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(TARGET_SIZE, fillerSize, OTHER_SIZE))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = (TARGET_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun defaultVerticalNotVisibleAnchorSeam() {
        val fillerSize = fillerSize_notVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, OTHER_SIZE, TARGET_SIZE))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun defaultVerticalNotVisibleOptAnchorWithin() {
        val fillerSize = fillerSize_notVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(OTHER_SIZE, fillerSize, TARGET_SIZE))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = (OTHER_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun defaultVerticalNotVisibleOptAnchorSeam() {
        val fillerSize = fillerSize_notVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        y = (TARGET_SIZE + OTHER_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseVerticalPartiallyVisibleAtAnchor() {
        val fillerSize = fillerSize_partVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(TARGET_SIZE, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -targetVisiblePortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseVerticalPartiallyVisibleAtOptAnchor() {
        val fillerSize = fillerSize_partVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1"), arrayOf(TARGET_SIZE, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -(TARGET_SIZE + targetVisiblePortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun reverseVerticalNotVisibleAnchorWithin() {
        val fillerSize = fillerSize_notVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(TARGET_SIZE, fillerSize, OTHER_SIZE))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -(TARGET_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseVerticalNotVisibleAnchorSeam() {
        val fillerSize = fillerSize_notVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(fillerSize, OTHER_SIZE, TARGET_SIZE))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -halfFillerViewExtraPortion))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun reverseVerticalNotVisibleOptAnchorWithin() {
        val fillerSize = fillerSize_notVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(OTHER_SIZE, fillerSize, TARGET_SIZE))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(y = -(OTHER_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("2"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun reverseVerticalNotVisibleOptAnchorSeam() {
        val fillerSize = fillerSize_notVis(RecyclerView.VERTICAL)
        setAdapter(arrayOf("0", "1", "2"), arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize))
        val layoutManager = setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(RecyclerViewActions.scrollBy(
                        y = -(TARGET_SIZE + OTHER_SIZE + halfFillerViewExtraPortion)))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("0"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    fun fillerSize_partVis(orientation: Int): Int {
        val activity = activityRule.activity
        val linearLayout = activity.findViewById<LinearLayout>(R.id.main_activity) ?: return 0
        return if (orientation == HORIZ) {
            linearLayout.width
        } else {
            linearLayout.height
        }
    }

    fun fillerSize_notVis(orientation: Int): Int {
        val activity = activityRule.activity
        val linearLayout = activity.findViewById<LinearLayout>(R.id.main_activity) ?: return 0
        return if (orientation == HORIZ) {
            linearLayout.width + extraFillerSize
        } else {
            linearLayout.height + extraFillerSize
        }
    }
}
