package com.bekawestberg.loopinglayout.test

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder

class SimpleAdapter : RecyclerView.Adapter<SimpleAdapter.SimpleViewHolder>() {



    var items: List<DataItem> = emptyList()
        set(value) {
            val diffCallback = DiffCallback(field, value)
            field = value
            DiffUtil.calculateDiff(diffCallback).dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        val v = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.simple_image_item, parent, false)
        val color = Color.rgb(
            (Math.random() * 200).toInt() + 55,
            (Math.random() * 200).toInt() + 55,
            (Math.random() * 200).toInt() + 55)
        v.setBackgroundColor(color)
        return SimpleViewHolder(v)
    }

    override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
        if (items.count() > 0) {
            holder.bind(items[position])
        } else {
            holder.bind(DataItem("https://cdn.dribbble.com/users/634336/screenshots/2246883/_____.png"))
        }
        Log.v(TAG, "binding holder, width: ${holder.itemView.width}")
    }

    override fun getItemCount(): Int {
        return if (items.count() == 0) 1 else items.count()
    }

    override fun onViewRecycled(holder: SimpleViewHolder) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    data class DataItem(val url: String)

    class SimpleViewHolder(view: View) : RecyclerView.ViewHolder(view) {

//        fun bind(/*int: pos*/) {
//
//        }
//
//        fun unbind() { }

        private val imageView = view.findViewById<AppCompatImageView>(R.id.image)

        fun bind(dataItem: DataItem) {
            Glide
                .with(imageView.context)
                .load(dataItem.url)
                .into(imageView)
        }

        fun unbind() {
//            Glide.with(imageView.context).clear(imageView)
        }

    }

    class DiffCallback(val oldItems: List<DataItem>, val newItems: List<DataItem>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldItems.size
        }

        override fun getNewListSize(): Int {
            return newItems.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return true
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }

    }

    companion object {
        private val TAG = "AdapterSimple"
    }
}