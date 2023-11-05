package com.bekawestberg.loopinglayout.library

import android.view.View
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.LayoutManager.getDecoratedMeasuredWidthWithMargins(child: View): Int {
    val lp = child.layoutParams as RecyclerView.LayoutParams
    return getDecoratedMeasuredWidth(child) + lp.leftMargin + lp.rightMargin
}

fun RecyclerView.LayoutManager.getDecoratedMeasuredHeightWithMargins(child: View): Int {
    val lp = child.layoutParams as RecyclerView.LayoutParams
    return getDecoratedMeasuredHeight(child) + lp.topMargin + lp.bottomMargin
}