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
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.util.HumanReadables

import org.hamcrest.Matcher

import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom

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
}
