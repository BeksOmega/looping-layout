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
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.util.HumanReadables
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import com.bekawestberg.loopinglayout.library.defaultDecider
import org.hamcrest.Matcher

object RecyclerViewActions {
    fun setLayoutManager(manager: RecyclerView.LayoutManager): ViewAction {
        return LayoutManagerAction(manager)
    }

    class LayoutManagerAction(private var mManager: RecyclerView.LayoutManager) : ViewAction {

        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(RecyclerView::class.java)
        }

        override fun getDescription(): String {
            return "Could not set the LayoutManager of the view."
        }

        override fun perform(uiController: UiController, view: View) {
            val recyclerView = view as RecyclerView
            try {
                recyclerView.layoutManager = mManager
            } catch (e: Exception) {
                throw PerformException.Builder()
                        .withActionDescription(this.description)
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(e)
                        .build()
            }

            uiController.loopMainThreadUntilIdle()
        }
    }

    fun setAdapter(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>): ViewAction {
        return AdapterAction(adapter)
    }

    class AdapterAction(
            private var mAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    ) : ViewAction {

        override fun getConstraints(): Matcher<View> {
           return isAssignableFrom(RecyclerView::class.java)
        }

        override fun getDescription(): String {
            return "Could not set the Adapter of the view."
        }

        override fun perform(uiController: UiController, view: View) {
            val recyclerView = view as RecyclerView
            try {
                recyclerView.adapter = mAdapter
            } catch (e: java.lang.Exception) {
                throw PerformException.Builder()
                        .withActionDescription(this.description)
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(e)
                        .build()
            }

            uiController.loopMainThreadUntilIdle()
        }
    }

    fun scrollBy(x: Int = 0, y: Int = 0): ViewAction {
        return ScrollByAction(x, y)
    }

    class ScrollByAction(val x: Int, val y: Int) : ViewAction {

        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(RecyclerView::class.java)
        }

        override fun getDescription(): String {
            return "Could not scroll the recycler."
        }

        override fun perform(uiController: UiController, view: View) {
            val recyclerView = view as RecyclerView
            try {
                recyclerView.scrollBy(x, y)
            } catch (e: java.lang.Exception) {
                throw PerformException.Builder()
                        .withActionDescription(this.description)
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(e)
                        .build()
            }
        }
    }

    fun scrollToPositionViaManager(
            position: Int,
            strategy: (Int, LoopingLayoutManager, RecyclerView.State) -> Int = ::defaultDecider
    ): ViewAction {
        return ScrollToPositionViaManagerAction(position, strategy)
    }

    class ScrollToPositionViaManagerAction(
            val position: Int,
            val strategy: (Int, LoopingLayoutManager, RecyclerView.State) -> Int
    ) : ViewAction {

        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(RecyclerView::class.java)
        }

        override fun getDescription(): String {
            return "Could not scroll the recycler."
        }

        override fun perform(uiController: UiController, view: View) {
            val recyclerView = view as RecyclerView
            (recyclerView.layoutManager as LoopingLayoutManager)
                    .scrollToPosition(position, strategy)
            uiController.loopMainThreadUntilIdle()
        }
    }
}
