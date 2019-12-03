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


package com.bekawestberg.loopinglayout.test.androidTest.utils

import android.view.View

import androidx.recyclerview.widget.RecyclerView

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
                    val recyclerView = view.rootView.findViewById<View>(recyclerViewId) as RecyclerView
                    val vh = recyclerView.findViewHolderForAdapterPosition(position)
                    if (vh != null) correctView = vh.itemView else return false
                }
                return view === correctView;
            }
        }
    }

    companion object {
        fun inRecycler(recyclerViewId: Int): RecyclerViewMatcher {
            return RecyclerViewMatcher(recyclerViewId)
        }
    }
}
