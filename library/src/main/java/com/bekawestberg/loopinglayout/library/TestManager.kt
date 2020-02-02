package com.bekawestberg.loopinglayout.library

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RestrictTo
import androidx.core.os.TraceCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.LinearLayoutManager.INVALID_OFFSET
import androidx.recyclerview.widget.RecyclerView.Recycler

class TestManager(context: Context, orientation: Int, reverse: Boolean)
    : LinearLayoutManager(context, orientation, reverse) {
    override fun onInitializeAccessibilityEvent(recycler: RecyclerView.Recycler, state: RecyclerView.State, event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(recycler, state, event)
        val eventString = event.toString()
        Log.v(TAG, eventString);
        Log.v(TAG, "initialize event!")
    }

    override fun onInitializeAccessibilityNodeInfo(recycler: Recycler, state: RecyclerView.State, info: AccessibilityNodeInfoCompat) {
        super.onInitializeAccessibilityNodeInfo(recycler, state, info)
        //Log.v(TAG, "Initialize node info!")
    }

    override fun onInitializeAccessibilityNodeInfoForItem(recycler: RecyclerView.Recycler, state: RecyclerView.State, host: View, info: AccessibilityNodeInfoCompat) {
        super.onInitializeAccessibilityNodeInfoForItem(recycler, state, host, info)
        //Log.v(TAG, "Initialize info for item!")
    }

    override fun performAccessibilityAction(recycler: Recycler, state: RecyclerView.State, action: Int, args: Bundle?): Boolean {
        //Log.v(TAG, "perform action!", Exception())
        return super.performAccessibilityAction(recycler, state, action, args)
    }

    override fun performAccessibilityActionForItem(recycler: RecyclerView.Recycler, state: RecyclerView.State, view: View, action: Int, args: Bundle?): Boolean {
        //Log.v(TAG, "perform action for item")
        return super.performAccessibilityActionForItem(recycler, state, view, action, args)
    }

    companion object {
        const val TAG = "TestManager"
    }
}