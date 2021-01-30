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

    internal var TAG = "EstimateShortestTest"
    // The width of the item associated with adapter index 0.
    private val TARGET_SIZE = 100
    // A constant size to use for views that are not the filler view, and are not the target view.
    private val OTHER_SIZE = 100
    // Makes the view that fills the screen a little bit wider than the recycler so that
    // the target is totally hidden.
    private val EXTRA_FILLER_SIZE = 100

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
     *    visible. The estimateShortestDistance decider has different behavior in these cases.
     * 5) anchor/optAnchor: Refers to which side want the target to be aligned with after the scroll
     *    is completed. If "anchor" that means we want the view to be aliged with the side where the
     *    0 adapter item was originally laid out. If "optAnchor", the opposite.
     * 6) overSeam/inLoop: Whether in scrolling the items we should pass over the seam (where the
     *    where the 0 item touches the last item), or stay within the loop. Only applies if notVis.
     */

    /*
     * Note that for doing the "partVis" tests we only need two items.
     * But for the "notVis" tests we need 3, because the decider assumes all views are the same size.
     */

    /*
     * View naming info:
     * T: Target view.
     * F: Filler view
     *   FB: Big filler (takes up screen or more)
     *   FS: Small filler (takes up equal to target)
     */

    /*
     * View diagram info:
     * |: Denotes the edges of the screen. Eg |---- A ----| Shows that the "A" view takes up exactly
     *    the size of the screen.
     * -: Represents the size of the view. Eg ----- A ----- B - Shows that "A is a large view and
     *    "B" is a small one.
     *
     * Note that although some views are shown to be off screen, they may or may not actually exist.
     *
     * For testing purposes you can kind of imagine the layout having an infinite number of views
     * (in order) in either direction.
     */

    @Test
    fun horiz_ltr_notRev_partVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        // |- T ------ F -|----
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Make T partially visible on the left.
                // -|T ------ F --|---
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE / 2))
                // |- T ------ F -|----
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_ltr_notRev_partVis_optAnchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        // |- T ------ F -|----
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Make T partially visible on the right.
                // ---|-- F ------ T|-
                .perform(RecyclerViewActions.scrollBy(x = (TARGET_SIZE + TARGET_SIZE / 2)))
                // ----|- F ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_notRev_notVis_anchor_inLoop() {
        setAdapter(arrayOf("T", "FB", "FS"),
                arrayOf(TARGET_SIZE, fillerSize_notVis(HORIZ), OTHER_SIZE))
        // |- T ------ FB -|---- FS -
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // - T |------ FB -----| FS -
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE))
                // |- T ------ FB -|---- FS -
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_ltr_notRev_notVis_anchor_overSeam() {
        setAdapter(arrayOf("FB", "FS", "T"),
                arrayOf(fillerSize_notVis(HORIZ), OTHER_SIZE, TARGET_SIZE))
        // |------ FB ----|-- FS - T -
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Should move items right, crossing over the seam between FB (0) and T (2)
                // |- T ------ FB -|---- FS
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_notRev_notVis_optAnchor_inLoop() {
        setAdapter(arrayOf("FS", "FB", "T"),
                arrayOf(OTHER_SIZE, fillerSize_notVis(HORIZ), OTHER_SIZE))
        // |- FS ------ FB -|------ T -
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Should move items left, even though this is technically further.
                // - FS ----|- FB ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun horiz_ltr_notRev_notVis_optAnchor_overSeam() {
        setAdapter(arrayOf("T", "FS", "FB"),
                arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize_notVis(HORIZ)))
        // |- T - FS ----|- FB ------
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // - T - FS | ----- FB ------|
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE + OTHER_SIZE))
                // Should move items left, crossing over the seam between FB (2) and T (0)
                // - FS ----|- FB ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_rev_partVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        // ----|- F ------ T -|
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Make T only partially visible on the right.
                // --|--- F ------ T|-
                .perform(RecyclerViewActions.scrollBy(x = -TARGET_SIZE / 2))
                // ----|- F ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_rev_partVis_optAnchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        val layoutManager = setLayoutManager(HORIZ, true)
        // ----|- F ------ T -|
        onView(withId(R.id.recycler))
                // Make T partially visible on the left.
                // -|T ------ F ---|--
                .perform(RecyclerViewActions.scrollBy(x = -(TARGET_SIZE + TARGET_SIZE / 2)))
                // |- T ------ F -|----
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_ltr_rev_notVis_anchor_inLoop() {
        setAdapter(arrayOf("T", "FB", "FS"),
                arrayOf(TARGET_SIZE, fillerSize_notVis(HORIZ), OTHER_SIZE))
        // - FS ----|- FB ------ T -|
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // - FS --|--- FB ------| T -
                .perform(RecyclerViewActions.scrollBy(x = -TARGET_SIZE))
                // - FS ----|- FB ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_ltr_rev_notVis_anchor_overSeam() {
        setAdapter(arrayOf("FB", "FS", "T"),
                arrayOf(fillerSize_notVis(HORIZ), OTHER_SIZE, TARGET_SIZE))
        //  T - FS --|--- FB ------|
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Should move items left, crossing over the seam between FB (0) and T (2)
                // FS ----|- FB ------ T|
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun horiz_ltr_rev_notVis_optAnchor_inLoop() {
        setAdapter(arrayOf("FS", "FB", "T"),
                arrayOf(OTHER_SIZE, fillerSize_notVis(HORIZ), TARGET_SIZE))
        // T -----|- FB ------ FS - |
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Should move items right, even though the other way is technically shorter.
                // |- T ------ FB -|---- FS
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_ltr_rev_notVis_optAnchor_overSeam() {
        setAdapter(arrayOf("T", "FS", "FB"),
                arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize_notVis(HORIZ)))
        // ------ FB --|--- FS - T -|
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // |----- FB -----| - FS - T -
                .perform(RecyclerViewActions.scrollBy(x = -(TARGET_SIZE + OTHER_SIZE)))
                // Should move items right, crossing over the seam between FB (2) and T (0)
                // |- T ------ FB -|---- FS -
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun horiz_rtl_notRev_partVis_anchor() {
        setRtl()
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        // ----|- F ------ T -|
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Make T partially visible on the right.
                // --|--- F ----- T|-
                .perform(RecyclerViewActions.scrollBy(x = -TARGET_SIZE / 2))
                // ----|- F ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_rtl_notRev_partVis_optAnchor() {
        setRtl()
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        // ----|- F ------ T -|
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Make T partially visible on the left.
                // -|T ------ F ---|--
                .perform(RecyclerViewActions.scrollBy(x = -(TARGET_SIZE + TARGET_SIZE / 2)))
                // |- T ------ F -|----
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_rtl_notRev_notVis_anchor_inLoop() {
        setRtl()
        setAdapter(arrayOf("T", "FB", "FS"),
                arrayOf(TARGET_SIZE, fillerSize_notVis(HORIZ), OTHER_SIZE))
        // - FS ----|- FB ------ T -|
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // - FS --|--- FB -----| T -
                .perform(RecyclerViewActions.scrollBy(x = -TARGET_SIZE))
                // - FS ----|- FB ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_rtl_notRev_notVis_anchor_overSeam() {
        setRtl()
        setAdapter(arrayOf("FB", "FS", "T"),
                arrayOf(fillerSize_notVis(HORIZ), OTHER_SIZE, TARGET_SIZE))
        // - T - FS ---|--- FB ------|
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Should move items right, crossing over the seam between FB (0) and T (2)
                // - FS ----|- FB ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun horiz_rtl_notRev_notVis_optAnchor_inLoop() {
        setRtl()
        setAdapter(arrayOf("FS", "FB", "T"),
                arrayOf(OTHER_SIZE, fillerSize_notVis(HORIZ), TARGET_SIZE))
        val layoutManager = setLayoutManager(HORIZ, false)
        // - T -----|- FB ------ Fs -|
        onView(withId(R.id.recycler))
                // Should move items right, even though that is technically further.
                // |- T ------ FB -|---- FS -
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_rtl_notRev_notVis_optAnchor_overSeam() {
        setRtl()
        setAdapter(arrayOf("T", "FS", "FB"),
                arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize_notVis(HORIZ)))
        // ------ FB -|---- FS - T -|
        val layoutManager = setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // --|--- FB ------| FS - T -
                .perform(RecyclerViewActions.scrollBy(x = -(TARGET_SIZE + OTHER_SIZE)))
                // Should move items right, crossing over the seam between FB (2) and T (0)
                // |- T ------ FB -|---- FS
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun horiz_rtl_rev_partVis_anchor() {
        setRtl()
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        val layoutManager = setLayoutManager(HORIZ, true)
        // |- T ------ F -|----
        onView(withId(R.id.recycler))
                // - T -|------ FB ------|
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE / 2))
                // |- T ------ F -|----
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_rtl_rev_partVis_optAnchor() {
        setRtl()
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(HORIZ)))
        val layoutManager = setLayoutManager(HORIZ, true)
        // |- T ------ F -|----
        onView(withId(R.id.recycler))
                // Make T partially visible on the right.
                // ---|-- F ------ T|-
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE + TARGET_SIZE / 2))
                // ----|- F ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_rtl_rev_notVis_anchor_inLoop() {
        setRtl()
        setAdapter(arrayOf("T", "FB", "FS"),
                arrayOf(TARGET_SIZE, fillerSize_notVis(HORIZ), OTHER_SIZE))
        // |- T ------ FB -|---- FS -
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // - T -|---- FB ------|- FS -
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE))
                // |- T ------ FB -|---- FS -
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun horiz_rtl_rev_notVis_anchor_overSeam() {
        setRtl()
        setAdapter(arrayOf("FB", "FS", "T"),
                arrayOf(fillerSize_notVis(HORIZ), OTHER_SIZE, TARGET_SIZE))
        val layoutManager = setLayoutManager(HORIZ, true)
        // |------ FB ---|-- FS - T -
        onView(withId(R.id.recycler))
                // Should move items right, crossing over the seam between FB (0) and T (2)
                // |- T ------ F -|---- FS -
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isLeftAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun horiz_rtl_rev_notVis_optAnchor_inLoop() {
        setRtl()
        setAdapter(arrayOf("FS", "FB", "T"),
                arrayOf(OTHER_SIZE, fillerSize_notVis(HORIZ), TARGET_SIZE))
        // |- FS ------ FB -|---- T -
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // - FS -|----- FB -----|- T -
                .perform(RecyclerViewActions.scrollBy(x = OTHER_SIZE))
                // FS ----|- FB ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun horiz_rtl_rev_notVis_optAnchor_overSeam() {
        setRtl()
        setAdapter(arrayOf("T", "FS", "FB"),
                arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize_notVis(HORIZ)))
        // |- T - FS ---|-- FB ------
        val layoutManager = setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // - T - FS -|----- FB --|---
                .perform(RecyclerViewActions.scrollBy(x = TARGET_SIZE + OTHER_SIZE ))
                // Should move items left, crossing over the seam between FB (2) and T (0)
                // - FS ----|- FB ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isRightAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun vert_notRev_partVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(VERT)))
        // |- T ------ F -|----
        val layoutManager = setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                // Make T partially visible at the top.
                // -|T ------ F --|---
                .perform(RecyclerViewActions.scrollBy(y = TARGET_SIZE / 2))
                // |- T ------ F -|----
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun vert_notRev_partVis_optAnchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(VERT)))
        val layoutManager = setLayoutManager(VERT, false)
        // |- T ------ F -|----
        onView(withId(R.id.recycler))
                // Make T partially visible at the bottom.
                // ---|-- F ------ T|-
                .perform(RecyclerViewActions.scrollBy(y = TARGET_SIZE + TARGET_SIZE / 2))
                // ----|- F ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun vert_notRev_notVis_anchor_inLoop() {
        setAdapter(arrayOf("T", "FB", "FS"),
                arrayOf(TARGET_SIZE, fillerSize_notVis(VERT), OTHER_SIZE))
        // |- T ------ FB -|---- FS -
        val layoutManager = setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                // - T |------ FB -----| FS -
                .perform(RecyclerViewActions.scrollBy(y = TARGET_SIZE))
                // |- T ------ FB -|---- FS -
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun vert_notRev_notVis_anchor_overSeam() {
        setAdapter(arrayOf("FB", "FS", "T"),
                arrayOf(fillerSize_notVis(VERT), OTHER_SIZE, TARGET_SIZE))
        // |------ FB ----|-- FS - T -
        val layoutManager = setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                // |- T ------ FB -|---- FS
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun vert_notRev_notVis_optAnchor_inLoop() {
        setAdapter(arrayOf("FS", "FB", "T"),
                arrayOf(OTHER_SIZE, fillerSize_notVis(VERT), TARGET_SIZE))
        // | - FS ------ FB -|------ T -
        val layoutManager = setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                // Should move items up, even though this is technically further.
                // - FS ----|- FB ------ T -|
                .perform(RecyclerViewActions.scrollBy(y = OTHER_SIZE))
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun vert_notRev_notVis_optAnchor_overSeam() {
        setAdapter(arrayOf("T", "FS", "FB"),
                arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize_notVis(VERT)))
        // |- T - FS ----|- FB ------
        val layoutManager = setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                // - T - FS | ----- FB ------|
                // Should move items up, crossing over the seam between FB (2) and T (0)
                // - FS ----|- FB ------ T -|
                .perform(RecyclerViewActions.scrollBy(y = TARGET_SIZE + OTHER_SIZE))
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun vert_rev_partVis_anchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(VERT)))
        val layoutManager = setLayoutManager(VERT, true)
        // ----|- F ------ T -|
        onView(withId(R.id.recycler))
                // Make T only partially visible at the bottom.
                // --|--- F ------ T|-
                .perform(RecyclerViewActions.scrollBy(y = -TARGET_SIZE / 2))
                // ----|- F ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun vert_rev_partVis_optAnchor() {
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, fillerSize_partVis(VERT)))
        // ----|- F ------ T -|
        val layoutManager = setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                // Make T partially visible at the top.
                // -|T ------ F ---|--
                .perform(RecyclerViewActions.scrollBy(y = -(TARGET_SIZE + TARGET_SIZE / 2)))
                // |- T ------ F -|----
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun vert_rev_notVis_anchor_inLoop() {
        setAdapter(arrayOf("T", "FB", "FS"),
                arrayOf(TARGET_SIZE, fillerSize_notVis(VERT), OTHER_SIZE))
        // - FS ----|- FB ------ T -|
        val layoutManager = setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                // - FS --|--- FB ------| T -
                .perform(RecyclerViewActions.scrollBy(y = -TARGET_SIZE))
                // - FS ----|- FB ------ T -|
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 1 && layoutManager.bottomRightIndex == 0)
    }

    @Test
    fun vert_rev_notVis_anchor_overSeam() {
        setAdapter(arrayOf("FB", "FS", "T"),
                arrayOf(fillerSize_notVis(VERT), OTHER_SIZE, TARGET_SIZE))
        //  T - FS --|--- FB ------|
        val layoutManager = setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                // Should move items up, crossing over the seam between FB (0) and T (2)
                // FS ----|- FB ------ T|
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isBottomAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 0 && layoutManager.bottomRightIndex == 2)
    }

    @Test
    fun vert_rev_notVis_optAnchor_inLoop() {
        setAdapter(arrayOf("FS", "FB", "T"),
                arrayOf(OTHER_SIZE, fillerSize_notVis(VERT), TARGET_SIZE))
        // T -----|- FB ------ FS - |
        val layoutManager = setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                // Should move items down, even though the other way is technically shorter.
                // |- T ------ FB -|---- FS
                .perform(RecyclerViewActions.scrollToPositionViaManager(2))

        onView(withText("T"))
                .check((::isTopAlignedWithPadding)(withId(R.id.recycler)))
        assert(layoutManager.topLeftIndex == 2 && layoutManager.bottomRightIndex == 1)
    }

    @Test
    fun vert_rev_notVis_optAnchor_overSeam() {
        setAdapter(arrayOf("T", "FS", "FB"),
                arrayOf(TARGET_SIZE, OTHER_SIZE, fillerSize_notVis(VERT)))
        // ------ FB --|--- FS - T -|
        val layoutManager = setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                // |----- FB -----| - FS - T -
                .perform(RecyclerViewActions.scrollBy(y = -(TARGET_SIZE + OTHER_SIZE )))
                // Should move items down, crossing over the seam between FB (2) and T (0)
                // |- T ------ FB -|---- FS -
                .perform(RecyclerViewActions.scrollToPositionViaManager(0))

        onView(withText("T"))
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
            linearLayout.width + EXTRA_FILLER_SIZE
        } else {
            linearLayout.height + EXTRA_FILLER_SIZE
        }
    }
}
