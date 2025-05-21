package com.somedeveloper.kanjihakken.Utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.util.lerp
import com.atilika.kuromoji.ipadic.Tokenizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.somedeveloper.kanjihakken.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun extractKanjiListFromCbzUriJob(
    context: Context,
    scope: CoroutineScope,
    uri: Uri,
    onProgressReport: (Float) -> Unit,
    onMessageReport: (String) -> Unit,
    onFinished: (Map<Char, Map<String, List<Int>>>) -> Unit
): Job {
    return scope.launch(Dispatchers.IO) {

        // image extraction
        onMessageReport(context.getString(R.string.counting_total_images))
        onProgressReport(0f)
        val bitmaps = mutableListOf<Bitmap>()
        extractImagesFromCbzUri(
            context = context,
            cbzUri = uri,
            onImageExtracted = { bitmap, count ->
                if (bitmaps.isEmpty())
                    onMessageReport(context.getString(R.string.extracting_images))
                bitmaps.add(bitmap)
                onProgressReport(lerp(0.1f, 0.25f, bitmaps.size.toFloat() / count.toFloat()))
                ensureActive()
            }
        )

        // text extraction
        val texts = mutableListOf<String>()
        onMessageReport(context.getString(R.string.extracting_text_from_images))
        onProgressReport(0.5f)
        val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        ensureActive()
        for (bitmap in bitmaps) {
            texts.add(
                extractTextFromBitmap(
                    recognizer = recognizer,
                    bitmap = bitmap
                )
            )
            onProgressReport(lerp(0.25f, 0.5f, texts.size / bitmaps.size.toFloat()))
            ensureActive()
        }

        // extract words
        onMessageReport(context.getString(R.string.tokenizing_texts))
        onProgressReport(0.25f)
        val tokenizer = Tokenizer.Builder().build()
        val tokensMap = mutableListOf<List<String>>()
        ensureActive()
        texts.forEachIndexed { index, text ->
            tokensMap.add(extractTokens(tokenizer, text))
            onProgressReport(lerp(0.5f, 0.75f, index.toFloat() / texts.size))
            ensureActive()
        }

        // create kanji list
        onMessageReport(context.getString(R.string.creating_kanji_list))
        TODO("Turn into a decent format and return it. DONT FORGET TO YIELD()!")
    }
}

private fun extractImagesFromCbzUri(
    context: Context,
    cbzUri: Uri,
    onImageExtracted: (Bitmap, Int) -> Unit,
) {
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

    // extraction pass
    contentResolver.openInputStream(cbzUri)?.use { inputStream ->
        ZipInputStream(BufferedInputStream(inputStream)).use { zipStream ->
            while (true) {
                val entry = zipStream.nextEntry ?: break
                if (!entry.isDirectory && entry.name.lowercase().endsWithAny(".jpg", ".jpeg", ".png")) {
                    val bytes = zipStream.readBytes()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null)
                        onImageExtracted(bitmap, totalImageCount)
                }
            }
            zipStream.close()
        }
    }
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