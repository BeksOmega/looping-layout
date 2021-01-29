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

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.assertion.PositionAssertions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import com.bekawestberg.loopinglayout.test.androidTest.utils.ViewAssertions.isBottomAlignedWithPadding
import com.bekawestberg.loopinglayout.test.androidTest.utils.ViewAssertions.isLeftAlignedWithPadding
import com.bekawestberg.loopinglayout.test.androidTest.utils.ViewAssertions.isRightAlignedWithPadding
import com.bekawestberg.loopinglayout.test.androidTest.utils.ViewAssertions.isTopAlignedWithPadding
import com.bekawestberg.loopinglayout.test.androidTest.utils.setAdapter
import com.bekawestberg.loopinglayout.test.androidTest.utils.setLayoutManager
import com.bekawestberg.loopinglayout.test.androidTest.utils.setRtl
import org.hamcrest.Matcher
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
class InitialLayoutTest {

    internal var TAG = "ExampleInstrumentedTest"

    @get:Rule var activityRule = ActivityTestRule(ActivityGeneric::class.java)

    @Test
    fun defaultHorizontal() {
        setUpAdapter()
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        assertStartsLeft()
    }

    @Test
    fun reverseHorizontal() {
        setUpAdapter()
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        assertStartsRight()
    }

    @Test
    fun defaultHorizontalRtl() {
        setRtl()
        setUpAdapter()
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        assertStartsRight()
    }

    @Test
    fun reverseHorizontalRTL() {
        setRtl()
        setUpAdapter()
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        assertStartsLeft()
    }

    @Test
    fun defaultVertical() {
        setUpAdapter()
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        assertStartsTop()
    }

    @Test
    fun reverseVertical() {
        setUpAdapter()
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        assertStartsBottom()
    }

    private fun setUpAdapter() {
        setAdapter(arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"))
    }

    private fun matchExists(matcher: Matcher<View>): Boolean {
        try {
            onView(matcher).check(matches(isDisplayed()))
        } catch (e: NoMatchingViewException) {
            return false
        }
        return true
    }

    private fun assertPlacedCorrectly(
        isAlignedCorrectly: (matcher: Matcher<View>) -> ViewAssertion,
        isCorrectRelativeTo: (matcher: Matcher<View>) -> ViewAssertion
    ) {
        onView(withText("0"))
                .check(isAlignedCorrectly(withId(R.id.recycler)))

        var checkIndex = 0
        while (true) {
            val checkMatcher = withText(checkIndex.toString())
            if (!matchExists(checkMatcher)) return
            val viewInteraction = onView(checkMatcher)
            var altIndex = checkIndex + 1
            while (true) {
                val altMatcher = withText(altIndex.toString())
                if (!matchExists(altMatcher)) break
                viewInteraction.check(isCorrectRelativeTo(altMatcher))
                altIndex++
            }
            checkIndex++
        }
    }

    private fun assertStartsLeft() {
        assertPlacedCorrectly(::isLeftAlignedWithPadding, ::isCompletelyLeftOf)
    }

    private fun assertStartsRight() {
        assertPlacedCorrectly(::isRightAlignedWithPadding, ::isCompletelyRightOf)
    }

    private fun assertStartsTop() {
        assertPlacedCorrectly(::isTopAlignedWithPadding, ::isCompletelyAbove)
    }

    private fun assertStartsBottom() {
        assertPlacedCorrectly(::isBottomAlignedWithPadding, ::isCompletelyBelow)
    }
}
