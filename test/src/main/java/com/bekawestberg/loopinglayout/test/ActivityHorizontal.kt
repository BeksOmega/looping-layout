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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import com.bekawestberg.loopinglayout.library.LoopingSnapHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ActivityHorizontal : AppCompatActivity() {
    private lateinit var mRecyclerView: RecyclerView
    private var mAdapter: AdapterGeneric = AdapterGeneric(
            Array(16) { i -> i.toString()},
            Array(16) { i -> 250})
    private var mLayoutManager =
            LoopingLayoutManager(this, RecyclerView.HORIZONTAL, true)
    private var snapHelper = LoopingSnapHelper();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mRecyclerView = findViewById(R.id.recycler)

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.adapter = mAdapter
        /*mLayoutManager.smoothScrollDirectionDecider = ::addViewsAtOptAnchorEdge*/

        snapHelper?.attachToRecyclerView(mRecyclerView)

        val button = findViewById<FloatingActionButton>(R.id.fab)
        button.setOnClickListener {
            mRecyclerView.scrollBy(250, 0)
            /*mAdapter.updateData(arrayOf("0", "1", "2"), Array(16) { i -> 250 })
            mAdapter.notifyDataSetChanged()*/
        }
    }
}
