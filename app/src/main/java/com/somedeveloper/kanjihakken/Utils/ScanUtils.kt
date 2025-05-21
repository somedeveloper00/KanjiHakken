package com.somedeveloper.kanjihakken.Utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream

fun extractImagesFromCbzUriJob(
    scope: CoroutineScope,
    context: Context,
    cbzUri: Uri,
    onImageExtracted: (Bitmap, Int) -> Unit,
    onFinished: () -> Unit
): Job {
    return scope.launch(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var totalImageCount = 0

        // counting pass
        contentResolver.openInputStream(cbzUri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zipStream ->
                while (true) {
                    if (!isActive)
                        break
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
                    if (!isActive)
                        break
                    val entry = zipStream.nextEntry ?: break
                    if (!entry.isDirectory && entry.name.lowercase().endsWithAny(".jpg", ".jpeg", ".png")) {
                        val bytes = zipStream.readBytes()
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null)
                            withContext(Dispatchers.Main) {
                                onImageExtracted(bitmap, totalImageCount)
                            }
                    }
                }
                zipStream.close()
            }
        }
        withContext(Dispatchers.Main) {
            onFinished()
        }
    }
}

fun extractTextFromBitmap(
    bitmap: Bitmap,
    onSuccess: suspend (String) -> Unit = {},
    onFail: (Exception) -> Unit = {}
) {
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    recognizer.process(inputImage)
        .addOnSuccessListener {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    onSuccess(it.text)
                }
            }
        }
        .addOnFailureListener { onFail(it) }
}

fun String.endsWithAny(vararg suffixes: String): Boolean {
    return suffixes.any { this.endsWith(it, ignoreCase = true) }
}