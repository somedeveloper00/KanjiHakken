package com.somedeveloper.kanjihakken.Utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.ui.util.lerp
import com.atilika.kuromoji.ipadic.Tokenizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.somedeveloper.kanjihakken.R
import com.somedeveloper.kanjihakken.formatAsLocalizedPercentage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.util.stream.Collectors
import java.util.zip.ZipInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun extractKanjiListFromCbzUriJob(
    context: Context,
    scope: CoroutineScope,
    uri: Uri,
    onProgressReport: (List<Pair<Float, String>>) -> Unit,
    onFinished: (List<Pair<String, List<Pair<String, List<Int>>>>>, List<Bitmap>, List<String>) -> Unit
): Job {
    return scope.launch(Dispatchers.IO) {

        Log.d("Kanji", "extractKanjiListFromCbzUriJob: image extraction")
        // image extraction
        var progress0 = 0f to context.getString(R.string.counting_total_images)
        onProgressReport(listOf(progress0))
        val bitmaps = extractImagesFromCbzUri(context, uri) { progress ->
            progress0 = lerp(0.1f, 1f, progress) to context.getString(R.string.extracting_images, formatAsLocalizedPercentage(progress))
            onProgressReport(listOf(progress0))
            ensureActive()
        }
        progress0 = 1f to context.getString(R.string.extracted_images)

        // text extraction
        Log.d("Kanji", "extractKanjiListFromCbzUriJob: text extraction")
        val texts = mutableListOf<String>()
        var progress1 = 0f to context.getString(R.string.extracting_text_from_images, formatAsLocalizedPercentage(0f))
        onProgressReport(listOf(progress0, progress1))
        val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        ensureActive()
        for (bitmap in bitmaps) {
            texts.add(extractTextFromBitmap(recognizer, bitmap))
            progress1 = texts.size / bitmaps.size.toFloat() to context.getString(R.string.extracting_text_from_images, formatAsLocalizedPercentage(texts.size / bitmaps.size.toFloat()))
            onProgressReport(listOf(progress0, progress1))
            ensureActive()
        }
        progress1 = 1f to context.getString(R.string.extracted_texts)

        // extract words
        Log.d("Kanji", "extractKanjiListFromCbzUriJob: word extraction")
        var progress2 = 0f to context.getString(R.string.tokenizing_texts)
        onProgressReport(listOf(progress0, progress1, progress2))
        val tokenizer = Tokenizer.Builder().build()
        ensureActive()
        val tokensMap = texts.mapIndexed { index, text ->
            val tokens = extractTokens(tokenizer, text)
            progress2 = progress2.copy(first = index.toFloat() / texts.size)
            onProgressReport(listOf(progress0, progress1, progress2))
            ensureActive()
            tokens
        }
        progress2 = 1f to context.getString(R.string.extracted_words)

        // create kanji list
        Log.d("Kanji", "extractKanjiListFromCbzUriJob: kanji list processing")
        var progress3 = 0f to context.getString(R.string.creating_kanji_list)
        val result = createKanjiList(tokensMap) {
            progress3 = progress3.copy(first = it)
            ensureActive()
        }
        progress3 = 1f to context.getString(R.string.created_kanji_list)
        onProgressReport(listOf(progress0, progress1, progress2, progress3))

        // prepare results
        val resultList = result.map { (kanji, occurrences) ->
            Pair(kanji, occurrences.map { (word, indices) -> Pair(word, indices) }.sortedByDescending { it.second.size })
        }.sortedByDescending { it.second.sumOf { it.second.size } }
        onFinished(resultList, bitmaps, texts)
    }
}

private fun createKanjiList(
    texts: List<List<String>>,
    onProgressReport: (Float) -> Unit
): Map<String, Map<String, List<Int>>> {
    val result: MutableMap<String, MutableMap<String, MutableList<Int>>> = mutableMapOf()
    for ((index, words) in texts.withIndex()) {
        for (word in words)
            for (kanji in extractKanjis(word))
                result.getOrPut(kanji) { mutableMapOf() }
                    .getOrPut(word) { mutableListOf() }
                    .add(index)
        onProgressReport((index.toFloat() + 1) / texts.size.toFloat())
    }
    return result.mapValues { (_, value) ->
        value.mapValues { (_, occurrences) ->
            occurrences
        }
    }
}

private fun extractKanjis(str: String): List<String> = str.codePoints()
    .filter { isKanji(it) }
    .mapToObj { Character.toChars(it).concatToString() }
    .collect(Collectors.toList())

private fun isKanji(codePoint: Int): Boolean {
    return when (codePoint) {
        in 0x3400..0x4DBF,    // CJK Unified Ideographs Extension A
        in 0x4E00..0x9FFF,    // CJK Unified Ideographs
        in 0xF900..0xFAFF,    // CJK Compatibility Ideographs
        in 0x20000..0x2A6DF,  // Extension B
        in 0x2A700..0x2B73F,  // Extension C
        in 0x2B740..0x2B81F,  // Extension D
        in 0x2B820..0x2CEAF,  // Extension E
        in 0x2CEB0..0x2EBEF,  // Extension F
        in 0x30000..0x3134F,  // Extension G
        in 0x31350..0x323AF   // Extension H
            -> true

        else -> false
    }
}

private fun extractImagesFromCbzUri(
    context: Context,
    cbzUri: Uri,
    onProgress: (Float) -> Unit,
): MutableList<Bitmap> {
    val contentResolver = context.contentResolver
    var totalImageCount = 0

    // counting pass
    contentResolver.openInputStream(cbzUri)?.use { inputStream ->
        ZipInputStream(BufferedInputStream(inputStream)).use { zipStream ->
            while (true) {
                val entry = zipStream.nextEntry ?: break
                if (!entry.isDirectory && entry.name.lowercase().endsWithAny(".jpg", ".jpeg", ".png"))
                    totalImageCount++
            }
            zipStream.close()
        }
    }

    val bitmaps = mutableListOf<Bitmap>()

    // extraction pass
    contentResolver.openInputStream(cbzUri)?.use { inputStream ->
        ZipInputStream(BufferedInputStream(inputStream)).use { zipStream ->
            while (true) {
                val entry = zipStream.nextEntry ?: break
                if (!entry.isDirectory && entry.name.lowercase().endsWithAny(".jpg", ".jpeg", ".png")) {
                    val bytes = zipStream.readBytes()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null)
                        bitmaps.add(bitmap)
                    onProgress(bitmaps.size.toFloat() / totalImageCount.toFloat())
                }
            }
            zipStream.close()
        }
    }
    return bitmaps
}

private suspend fun extractTextFromBitmap(
    recognizer: TextRecognizer,
    bitmap: Bitmap
): String = suspendCoroutine { continuation ->
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    recognizer.process(inputImage)
        .addOnSuccessListener { continuation.resume(it.text) }
        .addOnFailureListener { continuation.resume("") }
}

fun String.endsWithAny(vararg suffixes: String): Boolean {
    return suffixes.any { this.endsWith(it, ignoreCase = true) }
}

private fun extractTokens(
    tokenizer: Tokenizer,
    newText: String,
): List<String> {
    return tokenizer.tokenize(newText).mapNotNull { it.surface }
}