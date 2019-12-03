/*
 * Copyright 2018 Looping Layout
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

    @get:Rule
    var activityRule = ActivityTestRule(ActivityGeneric::class.java)

    @Test
    fun defaultHorizontalSwipeLeft() {
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun defaultHorizontalSwipeRight() {
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun defaultHorizontalSwipeBoth() {
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun reverseHorizontalSwipeLeft() {
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun reverseHorizontalSwipeRight() {
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun reverseHorizontalSwipeBoth() {
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun defaultHorizontalRTLSwipeLeft() {
        setRtl()
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun defaultHorizontalRTLSwipeRight() {
        setRtl()
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun defaultHorizontalRTLSwipeBoth() {
        setRtl()
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun reverseHorizontalRTLSwipeLeft() {
        setRtl()
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun reverseHorizontalRTLSwipeRight() {
        setRtl()
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun reverseHorizontalRTLSwipeBoth() {
        setRtl()
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        onView(withId(R.id.recycler))
                .perform(swipeLeft())
        stopSwipe()
        onView(withId(R.id.recycler))
                .perform(swipeRight())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun defaultVerticalSwipeUp() {
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(swipeUp())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun defaultVerticalSwipeDown() {
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(swipeDown())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun defaultVerticalSwipeBoth() {
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        onView(withId(R.id.recycler))
                .perform(swipeUp())
        stopSwipe()
        onView(withId(R.id.recycler))
                .perform(swipeDown())
        stopSwipe()

        assertOrder(Int::loopedIncrement)
    }

    @Test
    fun reverseVerticalSwipeUp() {
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(swipeUp())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun reverseVerticalSwipeDown() {
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        onView(withId(R.id.recycler))
                .perform(swipeDown())
        stopSwipe()

        assertOrder(Int::loopedDecrement)
    }

    @Test
    fun reverseVerticalSwipeBoth() {
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)
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