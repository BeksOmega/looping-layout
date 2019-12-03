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

import androidx.core.view.ViewCompat
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import com.bekawestberg.loopinglayout.test.R

internal fun setLayoutManager(direction: Int, reverseLayout: Boolean) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    Espresso.onView(ViewMatchers.withId(R.id.recycler))
            .perform(RecyclerViewActions.setLayoutManager(
                    LoopingLayoutManager(context, direction, reverseLayout)))
}

internal fun setRtl() {
    Espresso.onView(ViewMatchers.withId(R.id.main_activity)).perform(
            LayoutDirectionActions.setLayoutDirection(ViewCompat.LAYOUT_DIRECTION_RTL))
}

// Replicated int extensions for testing.
internal fun Int.loop(amount: Int, count: Int): Int {
    var newVal = this + amount;
    newVal %= count;
    if (newVal < 0)
        newVal += count
    return newVal
}

internal fun Int.loopedIncrement(count: Int): Int {
    return this.loop(1, count)
}

internal fun Int.loopedDecrement(count: Int): Int {
    return this.loop(-1, count)
}
