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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import com.bekawestberg.loopinglayout.test.androidTest.utils.*
import com.bekawestberg.loopinglayout.test.androidTest.utils.RecyclerViewMatcher.Companion.inRecycler
import com.bekawestberg.loopinglayout.test.androidTest.utils.loopedDecrement
import com.bekawestberg.loopinglayout.test.androidTest.utils.loopedIncrement
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import org.junit.Rule
import org.junit.Test
import java.lang.Thread.sleep

class SwipeTest {

    val TAG = "SwipeTest"
    
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
     * 4) left/right/up/down/both: The direction of the swipe. For example if it is "left" the views
     *    are moving toward the left. If the value it is both, the test swipes in both valid
     *    directions for the given orientation.
     */

    @Test
    fun horiz_ltr_notRev_left() {
        setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun horiz_ltr_notRev_right() {
        setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun horiz_ltr_notRev_both() {
        setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun horiz_ltr_rev_left() {
        setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun horiz_ltr_rev_right() {
        setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun horiz_ltr_rev_both() {
        setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun horiz_rtl_notRev_left() {
        setRtl()
        setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun horiz_rtl_notRev_right() {
        setRtl()
        setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun horiz_rtl_notRev_both() {
        setRtl()
        setLayoutManager(HORIZ, false)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun horiz_rtl_rev_left() {
        setRtl()
        setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun horiz_rtl_rev_right() {
        setRtl()
        setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun horiz_rtl_rev_both() {
        setRtl()
        setLayoutManager(HORIZ, true)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun VERT_notRev_up() {
        setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                .perform(swipeUp())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun VERT_notRev_down() {
        setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                .perform(swipeDown())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun VERT_notRev_both() {
        setLayoutManager(VERT, false)
        onView(withId(R.id.recycler))
                .perform(swipeUp())
        stopSwipe()
        onView(withId(R.id.recycler))
                .perform(swipeDown())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun VERT_rev_up() {
        setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                .perform(swipeUp())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun VERT_rev_down() {
        setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                .perform(swipeDown())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun VERT_rev_both() {
        setLayoutManager(VERT, true)
        onView(withId(R.id.recycler))
                .perform(swipeUp())
        stopSwipe()
        onView(withId(R.id.recycler))
                .perform(swipeDown())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    private fun stopSwipe(time: Long = 250) {
        sleep(time)
        onView(withId(R.id.recycler))
                .perform(click())
    }

    private fun assertOrder(change: Int.(Int) -> Int) {
        val activity = activityRule.activity
        val recycler = activity.findViewById<RecyclerView>(R.id.recycler)
        val adapter = recycler.adapter ?: return

        val initialText = (recycler.getChildAt(0) as TextView).text
        var index = initialText.toString().toInt()
        for (i in 1 until recycler.childCount) {
            index = index.change(adapter.itemCount)
            onView(inRecycler(R.id.recycler).atChildPosition(i))
                    .check(matches(withText(index.toString())))
        }
    }
}