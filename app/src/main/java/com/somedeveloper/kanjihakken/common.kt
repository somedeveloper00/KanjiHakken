package com.somedeveloper.kanjihakken
import java.text.NumberFormat
import java.util.Locale

fun formatAsLocalizedPercentage(value: Float, locale: Locale = Locale.getDefault()): String {
    val percentFormat = NumberFormat.getPercentInstance(locale)
    percentFormat.maximumFractionDigits = 0 // optional: control decimals
    return percentFormat.format(value)
}
