package com.somedeveloper.kanjihakken

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.text.NumberFormat
import java.util.Locale

fun formatAsLocalizedPercentage(value: Float, locale: Locale = Locale.getDefault()): String {
    val percentFormat = NumberFormat.getPercentInstance(locale)
    percentFormat.maximumFractionDigits = 0 // optional: control decimals
    return percentFormat.format(value)
}

fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    }
}