package com.somedeveloper.kanjihakken.Utils

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Debug

fun getAppMemoryUsage(): Long {
    return Debug.getNativeHeapAllocatedSize()
}

@Suppress("DEPRECATION")
@SuppressLint("ObsoleteSdkInt")
fun copyToClipboardSafe(string: String, context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(string, string)
        clipboardManager.setPrimaryClip(clipData)
    } else {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager
        clipboardManager.setText(string)
    }
}