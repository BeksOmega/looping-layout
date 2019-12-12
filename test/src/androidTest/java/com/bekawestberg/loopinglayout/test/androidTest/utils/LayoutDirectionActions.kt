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

import androidx.test.espresso.matcher.ViewMatchers.isDisplayed

import android.view.View

import androidx.core.view.ViewCompat
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction

import org.hamcrest.Matcher

object LayoutDirectionActions {
    /**
     * Sets layout direction on the view.
     */
    fun setLayoutDirection(layoutDirection: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isDisplayed()
            }

            override fun getDescription(): String {
                return "set layout direction"
            }

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadUntilIdle()

                val height = view.height
                val width = view.width
                ViewCompat.setLayoutDirection(view, layoutDirection)
                view.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                view.layout(view.left, view.top, view.right, view.bottom)

                uiController.loopMainThreadUntilIdle()
            }
        }
    }
}
