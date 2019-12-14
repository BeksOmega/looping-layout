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


package com.bekawestberg.loopinglayout.test.androidTest.utils

import android.view.View

import androidx.recyclerview.widget.RecyclerView
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

// From: https://stackoverflow.com/a/52773940
class RecyclerViewMatcher(private val recyclerViewId: Int) {

    fun atChildPosition(position: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            var correctView: View? = null

            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in recycler.")
            }

            public override fun matchesSafely(view: View): Boolean {
                if (correctView == null) {
                    val recyclerView = view.rootView.findViewById<RecyclerView>(recyclerViewId)
                    correctView = recyclerView.getChildAt(position)
                    if (correctView == null) return false
                }
                return view === correctView
            }
        }
    }

    fun atAdapterPosition(position: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            var correctView: View? = null

            override fun describeTo(description: Description) {
                description.appendText("Trying to find child at position $position in adapter.")
            }

            public override fun matchesSafely(view: View): Boolean {
                if (correctView == null) {
                    val recyclerView = view.rootView.findViewById<RecyclerView>(recyclerViewId)
                    val vh = recyclerView.findViewHolderForAdapterPosition(position)
                    if (vh != null) correctView = vh.itemView else return false
                }
                return view === correctView;
            }
        }
    }

    fun atAdapterPosViaManager(
            position: Int,
            strategy: ((pos: Int, layoutManager: LoopingLayoutManager) -> View?)? = null
    ): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            var correctView: View? = null

            override fun describeTo(description: Description) {
                description.appendText("Trying to find child at position $position in adapter via" +
                        "the layout manager.")
            }

            public override fun matchesSafely(view: View): Boolean {
                if (correctView == null) {
                    val recyclerView = view.rootView.findViewById<RecyclerView>(recyclerViewId)
                    val v = if (strategy != null && recyclerView.layoutManager is LoopingLayoutManager) {
                        (recyclerView.layoutManager as LoopingLayoutManager)
                                .findViewByPosition(position, strategy)
                    } else {
                        recyclerView.layoutManager?.findViewByPosition(position)
                    }
                    if (v != null) correctView = v else return false
                }
                return view === correctView
            }
        }
    }

    companion object {
        fun inRecycler(recyclerViewId: Int): RecyclerViewMatcher {
            return RecyclerViewMatcher(recyclerViewId)
        }
    }
}
