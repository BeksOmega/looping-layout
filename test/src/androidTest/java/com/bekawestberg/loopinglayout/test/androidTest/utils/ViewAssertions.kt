package com.bekawestberg.loopinglayout.test.androidTest.utils

import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.test.espresso.AmbiguousViewMatcherException
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.assertion.PositionAssertions
import androidx.test.espresso.core.internal.deps.guava.base.Preconditions
import androidx.test.espresso.core.internal.deps.guava.base.Predicate
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.core.internal.deps.guava.collect.Iterators
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables.breadthFirstViewTraversal
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matcher
import org.hamcrest.StringDescription
import java.util.*

object ViewAssertions {

    enum class Position {
        LEFT_PADDING, TOP_PADDING, RIGHT_PADDING, BOTTOM_PADDING
    }

    fun isLeftAlignedWithPadding(matcher: Matcher<View>): ViewAssertion {
        return relativePositionOf(matcher, Position.LEFT_PADDING)
    }

    fun isTopAlignedWithPadding(matcher: Matcher<View>): ViewAssertion {
        return relativePositionOf(matcher, Position.TOP_PADDING)
    }

    fun isRightAlignedWithPadding(matcher: Matcher<View>): ViewAssertion {
        return relativePositionOf(matcher, Position.RIGHT_PADDING)
    }

    fun isBottomAlignedWithPadding(matcher: Matcher<View>): ViewAssertion {
        return relativePositionOf(matcher, Position.BOTTOM_PADDING)
    }

    internal fun relativePositionOf(
            viewMatcher: Matcher<View>, position: Position): ViewAssertion {
        checkNotNull(viewMatcher)
        return ViewAssertion { foundView, noViewException ->
            val description = StringDescription()
            if (noViewException != null) {
                description.appendText(
                        String.format(
                                Locale.ROOT,
                                "' check could not be performed because view '%s' was not found.\n",
                                noViewException.viewMatcherDescription))
                throw noViewException
            } else {
                description
                        .appendText("View:")
                        .appendText(HumanReadables.describe(foundView))
                        .appendText(" is not ")
                        .appendText(position.toString())
                        .appendText(" view ")
                        .appendText(viewMatcher.toString())
                assertThat<Boolean>(
                        description.toString(),
                        isRelativePosition(
                                foundView, findView(viewMatcher, getTopViewGroup(foundView)), position),
                        `is`<Boolean>(true))
            }
        }
    }

    internal fun findView(toView: Matcher<View>, root: View?): View {
        Preconditions.checkNotNull(toView)
        Preconditions.checkNotNull(root)
        val viewPredicate = Predicate<View> { input -> toView.matches(input) }
        val matchedViewIterator = Iterables.filter(breadthFirstViewTraversal(root!!), viewPredicate).iterator()
        var matchedView: View? = null
        while (matchedViewIterator.hasNext()) {
            if (matchedView != null) {
                // Ambiguous!
                throw AmbiguousViewMatcherException.Builder()
                        .withRootView(root)
                        .withViewMatcher(toView)
                        .withView1(matchedView)
                        .withView2(matchedViewIterator.next())
                        .withOtherAmbiguousViews(*Iterators.toArray(matchedViewIterator, View::class.java))
                        .build()
            } else {
                matchedView = matchedViewIterator.next()
            }
        }
        if (matchedView == null) {
            throw NoMatchingViewException.Builder()
                    .withViewMatcher(toView)
                    .withRootView(root)
                    .build()
        }
        return matchedView
    }

    private fun getTopViewGroup(view: View): ViewGroup? {
        var currentParent: ViewParent? = view.parent
        var topView: ViewGroup? = null
        while (currentParent != null) {
            if (currentParent is ViewGroup) {
                topView = currentParent
            }
            currentParent = currentParent.parent
        }
        return topView
    }

    internal fun isRelativePosition(view1: View, view2: View, position: Position): Boolean {
        val location1 = IntArray(2)
        val location2 = IntArray(2)
        view1.getLocationOnScreen(location1)
        view2.getLocationOnScreen(location2)

        val rect1 = Rect(
                location1[0] + view1.paddingLeft,
                location1[1] + view1.paddingTop,
                location1[0] + view1.width - view1.paddingRight,
                location1[1] + view1.height - view1.paddingBottom)

        val rect2 = Rect(
                location2[0] + view2.paddingLeft,
                location2[1] + view2.paddingTop,
                location2[0] + view2.width - view2.paddingRight,
                location2[1] + view2.height - view2.paddingBottom)

        when (position) {
            Position.LEFT_PADDING -> return rect1.left == rect2.left
            Position.TOP_PADDING -> return rect1.top == rect2.top
            Position.RIGHT_PADDING -> return rect1.right == rect2.right
            Position.BOTTOM_PADDING -> return rect1.bottom == rect2.bottom
            else -> return false
        }
    }
}