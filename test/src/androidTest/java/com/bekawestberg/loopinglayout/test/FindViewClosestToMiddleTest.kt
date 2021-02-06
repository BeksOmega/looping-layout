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
import com.bekawestberg.loopinglayout.test.androidTest.utils.*
import com.bekawestberg.loopinglayout.test.androidTest.utils.RecyclerViewActions.scrollBy
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
class FindViewClosestToMiddleTest {

    internal var TAG = "FindViewClosestToMiddleTest"
    // The width of the item associated with adapter index 0.
    private val TARGET_SIZE = 100;

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
     * 4) closestFirst/closestLast: Whether the target item we want to select is closer to the
     *    start of the layout (where the 0 item will have initially been laid out) or closer to
     *    the end of the layout.
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
    fun horiz_ltr_notRev_closestFirst() {
        setUpAdapter(HORIZ)
        // |- T -- F -- - T|-
        setLayoutManager(HORIZ, false)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyLeftOf(withText("F")))
    }

    @Test
    fun horiz_ltr_notRev_closestLast() {
        setUpAdapter(HORIZ)
        // |- T -- F -- T|-
        setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Scroll the recycler so the last T is closer to the middle.
                // -|T -- F -- T -|
                .perform(scrollBy(x = TARGET_SIZE / 2))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyRightOf(withText("F")))
    }

    @Test
    fun horiz_ltr_rev_closestFirst() {
        setUpAdapter(HORIZ)
        // -|T -- F -- T -|
        setLayoutManager(HORIZ, true)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyRightOf(withText("F")))
    }

    @Test
    fun horiz_ltr_rev_closestLast() {
        setUpAdapter(HORIZ)
        // -|T -- F -- T -|
        setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Scroll the recycler so the last T is closer to the middle.
                // |- T -- F -- T|-
                .perform(scrollBy(x = -TARGET_SIZE / 2))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyLeftOf(withText("F")))
    }

    @Test
    fun horiz_rtl_notRev_closestFirst() {
        setRtl()
        setUpAdapter(HORIZ)
        // -|T -- F -- T -|
        setLayoutManager(HORIZ, false)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyRightOf(withText("F")))
    }

    @Test
    fun horiz_rtl_notRev_closestLast() {
        setRtl()
        setUpAdapter(HORIZ)
        // -|T -- F -- T -|
        setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                // Scroll the recycler so the last T is closer to the middle.
                // |- T -- F -- T|-
                .perform(scrollBy(x = -TARGET_SIZE / 2))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyLeftOf(withText("F")))
    }

    @Test
    fun horiz_rtl_rev_closestFirst() {
        setRtl()
        setUpAdapter(HORIZ)
        // |- T -- F -- T|-
        setLayoutManager(HORIZ, true)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyLeftOf(withText("F")))
    }

    @Test
    fun horiz_rtl_rev_closestLast() {
        setRtl()
        setUpAdapter(HORIZ)
        // |- T -- F -- T|-
        setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                // Scroll the recycler so the last T is closer to the middle.
                // -|T -- F -- T -|
                .perform(scrollBy(x = TARGET_SIZE / 2))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyRightOf(withText("F")))
    }

    @Test
    fun vert_notRev_closestFirst() {
        setUpAdapter(VERT)
        // |- T -- F -- T|-
        setLayoutManager(VERT, false)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyAbove(withText("F")))
    }

    @Test
    fun vert_notRev_closestLast() {
        setUpAdapter(VERT)
        // |- T -- F -- T|-
        setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                // Scroll the recycler so the last T is closer to the middle.
                // -|T -- F -- T -|
                .perform(scrollBy(y = TARGET_SIZE / 2))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyBelow(withText("F")))
    }

    @Test
    fun vert_rev_closestFirst() {
        setUpAdapter(VERT)
        // -|T -- F -- T -|
        setLayoutManager(VERT, true)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyBelow(withText("F")))
    }

    @Test
    fun vert_rev_closestLast() {
        setUpAdapter(VERT)
        // -|T -- F -- T -|
        setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                // Scroll the recycler so the last T is closer to the middle.
                // |- T -- F -- T|-
                .perform(scrollBy(y = -TARGET_SIZE / 2))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyAbove(withText("F")))
    }

    private fun setUpAdapter(orientation: Int) {
        val sizeOfFiller = calculateSizeOfFiller(orientation)
        setAdapter(arrayOf("T", "F"), arrayOf(TARGET_SIZE, sizeOfFiller))
    }

    private fun calculateSizeOfFiller(orientation: Int): Int {
        val activity = activityRule.activity
        val linearLayout = activity.findViewById<LinearLayout>(R.id.main_activity)
        return if (orientation == HORIZ) {
            linearLayout.width - TARGET_SIZE - TARGET_SIZE / 2
        } else {
            linearLayout.height - TARGET_SIZE - TARGET_SIZE / 2
        }
    }
}