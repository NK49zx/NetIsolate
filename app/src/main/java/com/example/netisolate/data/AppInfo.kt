package com.example.netisolate.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    var isQuarantined: Boolean = false,
    val uid: Int
)
