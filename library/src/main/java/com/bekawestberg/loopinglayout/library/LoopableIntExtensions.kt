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


package com.bekawestberg.loopinglayout.library


/**
 * Returns the value of the Int plus the given [amount], but "loops" the value to keep it between
 * the [count] and zero.
 */
internal fun Int.loop(amount: Int, count: Int): Int {
    var newVal = this + amount;
    newVal %= count;
    if (newVal < 0)
        newVal += count
    return newVal
}

/**
 * Returns the value of the Int plus one, but "loops" the value to keep it between the [count] and zero.
 */
internal fun Int.loopedIncrement(count: Int): Int {
    return this.loop(1, count)
}

/**
 * Returns the value of the Int minus one, but "loops" the value to keep it between the [count] and zero.
 */
internal fun Int.loopedDecrement(count: Int): Int {
    return this.loop(-1, count)
}