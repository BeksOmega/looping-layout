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

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class AdapterDiffUtil (): RecyclerView.Adapter<AdapterDiffUtil.MyViewHolder>() {

    private val mDataset = ArrayList<Data>()

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class MyViewHolder(// each data item is just a string in this case
            var textView: TextView) : RecyclerView.ViewHolder(textView)

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        // create a new view
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.text_view_generic, parent, false) as TextView
        // (int)(Math.random() * 255)
        val color = Color.rgb(
                (Math.random() * 200).toInt() + 55,
                (Math.random() * 200).toInt() + 55,
                (Math.random() * 200).toInt() + 55)
        v.setBackgroundColor(color)

        return MyViewHolder(v)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element
        val v = holder.textView
        v.text = mDataset[position].name

        val params = v.layoutParams
        params.width = 250
        params.height =  250
        v.layoutParams = params

        Log.v(TAG, "binding")
    }

    fun setData(newData: List<Data>) {
        val diffCallback = MyDiffCallback(mDataset, newData)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        mDataset.clear()
        mDataset.addAll(newData)
        diffResult.dispatchUpdatesTo(this)
    }

    // Return the size of your data set (invoked by the layout manager)
    override fun getItemCount(): Int {
        return mDataset.size
    }

    companion object {
        private val TAG = "AdapterVertical"
    }

    public class Data(val index: Int, val name: String)

    class MyDiffCallback(private val oldList: List<Data>, private val newList: List<Data>): DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].index == newList[newItemPosition].index
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].name == newList[newItemPosition].name
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            return super.getChangePayload(oldItemPosition, newItemPosition)
        }
    }
}