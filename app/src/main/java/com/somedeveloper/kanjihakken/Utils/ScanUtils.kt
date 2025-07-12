package com.somedeveloper.kanjihakken.Utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.util.lerp
import androidx.core.content.FileProvider
import com.atilika.kuromoji.ipadic.Tokenizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.somedeveloper.kanjihakken.R
import com.somedeveloper.kanjihakken.formatAsLocalizedPercentage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.zip.ZipInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
fun extractKanjiListFromCbzUriJob(
    context: Context,
    scope: CoroutineScope,
    uri: Uri,
    onProgressReport: (List<Pair<Float, String>>) -> Unit,
    onFinished: (List<Pair<String, List<Pair<String, List<Int>>>>>, List<Uri>, List<String>) -> Unit
): Job = scope.launch(Dispatchers.IO) {

    val cacheDir = File(context.cacheDir, "kanji_cbz_cache").apply {
        mkdirs()
    }

    // data for image extraction
    var progress0 = 0f to context.getString(R.string.counting_total_images)

    // data for text extraction
    var progress1Atomic = AtomicReference(0f to context.getString(R.string.extracting_text_from_images, formatAsLocalizedPercentage(0f)))
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    var ocrCount = AtomicInteger(0)
    var ocrJobs = mutableListOf<Deferred<String>>()
    var texts = AtomicReference(listOf<String>())

    // image extraction
    val imageUris = mutableListOf<Uri>()
    if (!isActive)
        recognizer.close()
    ensureActive()
    onProgressReport(listOf(progress0, progress1Atomic.get()))
    extractImagesFromCbzUri(context, uri) { bitmap, totalCount ->
        progress0 = lerp(0.1f, 1f, imageUris.size / totalCount.toFloat()) to context.getString(R.string.extracting_images, formatAsLocalizedPercentage(imageUris.size / totalCount.toFloat()))
        if (!isActive)
            recognizer.close()
        ensureActive()
        onProgressReport(listOf(progress0, progress1Atomic.get()))

        // text extraction (async)
        ocrJobs.add(async() {
            val text = extractTextFromBitmap(recognizer, bitmap)
            texts.set(texts.get() + text)
            ocrCount.set(ocrCount.get() + 1)
            progress1Atomic.set((ocrCount.get() + 1) / totalCount.toFloat() to context.getString(R.string.extracting_text_from_images, formatAsLocalizedPercentage((ocrCount.get() + 1) / totalCount.toFloat())))
            if (!isActive)
                recognizer.close()
            onProgressReport(listOf(progress0, progress1Atomic.get()))
            text
        })

        // cache to disk (and leave any references so GC will dealloc)
        val imageFile = File(cacheDir, "${imageUris.size}.jpg")
        imageUris.add(FileProvider.getUriForFile(context, "com.somedeveloper.kanjihakken.fileprovider", imageFile))
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageFile.outputStream())
    }
    progress0 = 1f to context.getString(R.string.extracted_images)

    // finish text extraction
    ocrJobs.toList().awaitAll()
    recognizer.close()
    progress1Atomic.set(1f to context.getString(R.string.extracted_texts))

    // extract words
    val tokenizer = Tokenizer.Builder().build()
    val tokensMap = texts.get().mapIndexed { index, text -> extractTokens(tokenizer, text) }

    // create kanji list
    val result = createKanjiList(tokensMap) {}

    // prepare results
    val resultList = result.map { (kanji, occurrences) ->
        Pair(kanji, occurrences.map { (word, indices) -> Pair(word, indices) }.sortedByDescending { it.second.size })
    }.sortedByDescending { it.second.sumOf { it.second.size } }
    onFinished(resultList, imageUris, texts.get())
}

@OptIn(ExperimentalCoroutinesApi::class)
fun extractKanjiListFromWebsiteUriJob(
    context: Context,
    scope: CoroutineScope,
    url: URL,
    onProgressReport: (List<Pair<Float, String>>) -> Unit,
    onFinished: (List<Pair<String, List<Pair<String, List<Int>>>>>) -> Unit
): Job = scope.launch(Dispatchers.Default) {

    var text = ""

    // download text by GET
    var progress0 = 0f to context.getString(R.string.downloading)
    with(url.openConnection() as HttpURLConnection) {
        ensureActive()
        onProgressReport(listOf(progress0))
        // send
        requestMethod = "GET"
        connectTimeout = 10000
        readTimeout = 10000
        connect()
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Toast.makeText(context, "Failed to read website. HTTP status code: ${responseCode}", Toast.LENGTH_SHORT).show()
            onFinished(emptyList())
        }
        ensureActive()
        progress0 = 0.1f to context.getString(R.string.downloading)
        onProgressReport(listOf(progress0))
        // read
        val reader = inputStream.bufferedReader()
        val length = contentLength
        var progress = 0f
        val sb = StringBuilder()
        while (true) {
            val line = reader.readLine() ?: break
            sb.append(line)
            progress += line.length
            ensureActive()
            progress0 = lerp(0.1f, 1f, progress / length) to context.getString(R.string.downloading)
            onProgressReport(listOf(progress0))
        }
        text = sb.toString()
    }
    progress0 = 1f to context.getString(R.string.downloading)
    ensureActive()
    onProgressReport(listOf(1f to context.getString(R.string.downloading)))

    // extract tokens
    val tokenizer = Tokenizer.Builder().build()
    val tokens = extractTokens(tokenizer, text)

    // create kanji list
    Log.d("Kanji", "extractKanjiListFromCbzUriJob: kanji list processing")
    val result = createKanjiList(listOf(tokens)) {}

    // prepare results
    val resultList = result.map { (kanji, occurrences) ->
        Pair(kanji, occurrences.map { (word, indices) -> Pair(word, indices) }.sortedByDescending { it.second.size })
    }.sortedByDescending { it.second.sumOf { it.second.size } }
    onFinished(resultList)
}

@OptIn(ExperimentalCoroutinesApi::class)
fun extractKanjiListFromYouTubeUrlJob(
    context: Context,
    scope: CoroutineScope,
    url: String,
    onProgressReport: (List<Pair<Float, String>>) -> Unit,
    onFinished: (List<Pair<String, List<Pair<String, List<Int>>>>>) -> Unit
): Job = scope.launch(Dispatchers.IO) {

    var text = ""

    // download subtitle by GET
    var progress0 = 0f to context.getString(R.string.downloading)

    val fullUrl = context.getString(R.string.supadataCaptionGetUrlFormat, url)
    Log.d("Kanji", "extractKanjiListFromYouTubeUrlJob: ${fullUrl}")
    with(URL(fullUrl).openConnection() as HttpURLConnection) {
        ensureActive()
        onProgressReport(listOf(progress0))
        // send
        requestMethod = "GET"
        connectTimeout = 10000
        readTimeout = 10000
        setRequestProperty("x-api-key", context.getString(R.string.supadataApiKey))
        Log.d("Kanji", "extractKanjiListFromYouTubeUrlJob: ${context.getString(R.string.supadataApiKey)}")
        connect()
        try {
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Toast.makeText(context, "Failed to read website. HTTP status code: ${responseCode}", Toast.LENGTH_SHORT).show()
                onFinished(emptyList())
                return@launch
            }
        } catch (ex: Exception) {
            Log.d("Kanji", "extractKanjiListFromYouTubeUrlJob: ${ex.toString()}")
            Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG).show()
            onFinished(emptyList())
            return@launch
        }
        ensureActive()
        progress0 = 0.1f to context.getString(R.string.downloading)
        onProgressReport(listOf(progress0))
        // read
        val reader = inputStream.bufferedReader()
        val length = contentLength
        var progress = 0f
        val sb = StringBuilder()
        while (true) {
            val line = reader.readLine() ?: break
            sb.append(line)
            progress += line.length
            ensureActive()
            progress0 = lerp(0.1f, 1f, progress / length) to context.getString(R.string.downloading)
            onProgressReport(listOf(progress0))
        }
        text = sb.toString()
    }
    progress0 = 1f to context.getString(R.string.downloading)
    ensureActive()
    onProgressReport(listOf(1f to context.getString(R.string.downloading)))

    // extract tokens
    val tokenizer = Tokenizer.Builder().build()
    val tokens = extractTokens(tokenizer, text)

    // create kanji list
    Log.d("Kanji", "extractKanjiListFromCbzUriJob: kanji list processing")
    val result = createKanjiList(listOf(tokens)) {}

    // prepare results
    val resultList = result.map { (kanji, occurrences) ->
        Pair(kanji, occurrences.map { (word, indices) -> Pair(word, indices) }.sortedByDescending { it.second.size })
    }.sortedByDescending { it.second.sumOf { it.second.size } }
    onFinished(resultList)
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
    onProgress: (Bitmap, Int) -> Unit,
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
                    if (bitmap != null) {
                        onProgress(bitmap, totalImageCount)
                    }
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