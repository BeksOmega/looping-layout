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

    internal var TAG = "ExampleInstrumentedTest"
    // The width of the item associated with adapter index 0.
    private val sizeOfZeroItem = 100;
    // The visible portion of the second item associated with the adapter index 0. (half visible)
    private val visiblePortionOfSecondZeroItem = sizeOfZeroItem / 2;


    @get:Rule
    var activityRule = ActivityTestRule(ActivityGeneric::class.java)

    @Test
    fun defaultHorizontalClosestFirstInLayout() {
        setUpAdapter(RecyclerView.HORIZONTAL)
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyLeftOf(withText("1")))
    }

    @Test
    fun defaultHorizontalClosestLastInLayout() {
        setUpAdapter(RecyclerView.HORIZONTAL)
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        // Scroll the recycler so that the second zero item is completely visible, and as such
        // closer to the middle.
        onView(withId(R.id.recycler))
                .perform(scrollBy(x = visiblePortionOfSecondZeroItem))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyRightOf(withText("1")))
    }

    @Test
    fun reverseHorizontalClosestFirstInLayout() {
        setUpAdapter(RecyclerView.HORIZONTAL)
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyRightOf(withText("1")))
    }

    @Test
    fun reverseHorizontalClosestLastInLayout() {
        setUpAdapter(RecyclerView.HORIZONTAL)
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        // Scroll the recycler so that the second zero item is completely visible, and as such
        // closer to the middle.
        onView(withId(R.id.recycler))
                .perform(scrollBy(x = -visiblePortionOfSecondZeroItem))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyLeftOf(withText("1")))
    }

    @Test
    fun defaultHorizontalRtlClosestFirstInLayout() {
        setRtl()
        setUpAdapter(RecyclerView.HORIZONTAL)
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyRightOf(withText("1")))
    }

    @Test
    fun defaultHorizontalRtlClosestLastInLayout() {
        setRtl()
        setUpAdapter(RecyclerView.HORIZONTAL)
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, false)
        // Scroll the recycler so that the second zero item is completely visible, and as such
        // closer to the middle.
        
        onView(withId(R.id.recycler))
                .perform(scrollBy(x = -visiblePortionOfSecondZeroItem))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyLeftOf(withText("1")))
    }

    @Test
    fun reverseHorizontalRtlClosestFirstInLayout() {
        setRtl()
        setUpAdapter(RecyclerView.HORIZONTAL)
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyLeftOf(withText("1")))
    }

    @Test
    fun reverseHorizontalRtlClosestLastInLayout() {
        setRtl()
        setUpAdapter(RecyclerView.HORIZONTAL)
        setLayoutManager(LoopingLayoutManager.HORIZONTAL, true)
        // Scroll the recycler so that the second zero item is completely visible, and as such
        // closer to the middle.
        onView(withId(R.id.recycler))
                .perform(scrollBy(x = visiblePortionOfSecondZeroItem))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyRightOf(withText("1")))
    }

    @Test
    fun defaultVerticalClosestFirstInLayout() {
        setUpAdapter(RecyclerView.VERTICAL)
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyAbove(withText("1")))
    }

    @Test
    fun defaultVerticalClosestLastInLayout() {
        setUpAdapter(RecyclerView.VERTICAL)
        setLayoutManager(LoopingLayoutManager.VERTICAL, false)
        // Scroll the recycler so that the second zero item is completely visible, and as such
        // closer to the middle.
        onView(withId(R.id.recycler))
                .perform(scrollBy(y = visiblePortionOfSecondZeroItem))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyBelow(withText("1")))
    }

    @Test
    fun reverseVerticalClosestFirstInLayout() {
        setUpAdapter(RecyclerView.VERTICAL)
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyBelow(withText("1")))
    }

    @Test
    fun reverseVerticalClosestLastInLayout() {
        setUpAdapter(RecyclerView.VERTICAL)
        setLayoutManager(LoopingLayoutManager.VERTICAL, true)
        // Scroll the recycler so that the second zero item is completely visible, and as such
        // closer to the middle.
        onView(withId(R.id.recycler))
                .perform(scrollBy(y = -visiblePortionOfSecondZeroItem))

        onView(RecyclerViewMatcher(R.id.recycler).atAdapterPosViaManager(0))
                .check(isCompletelyAbove(withText("1")))
    }

    private fun setUpAdapter(orientation: Int) {
        val sizeOfOneView = calculateSizeOfOneView(orientation)
        setAdapter(arrayOf("0", "1"), arrayOf(sizeOfZeroItem, sizeOfOneView))
    }

    private fun calculateSizeOfOneView(orientation: Int): Int {
        val activity = activityRule.activity
        val recycler = activity.findViewById<RecyclerView>(R.id.recycler) ?: return 0
        return if (orientation == RecyclerView.HORIZONTAL) {
            recycler.width - (sizeOfZeroItem + visiblePortionOfSecondZeroItem)
        } else {
            recycler.height - (sizeOfZeroItem + visiblePortionOfSecondZeroItem)
        }
    }
}