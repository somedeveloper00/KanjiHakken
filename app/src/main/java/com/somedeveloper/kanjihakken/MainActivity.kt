package com.somedeveloper.kanjihakken

import android.graphics.Bitmap
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.somedeveloper.kanjihakken.Utils.extractImagesFromCbzUriJob
import com.somedeveloper.kanjihakken.Utils.extractTextFromBitmap
import com.somedeveloper.kanjihakken.Utils.getAppMemoryUsage
import com.somedeveloper.kanjihakken.panes.KanjiEntry
import com.somedeveloper.kanjihakken.panes.KankiList
import com.somedeveloper.kanjihakken.panes.LoadingDialog
import com.somedeveloper.kanjihakken.panes.MangaDetails
import com.somedeveloper.kanjihakken.panes.MangaSelector
import com.somedeveloper.kanjihakken.panes.SourceTypeSelector
import com.somedeveloper.kanjihakken.ui.theme.KanjiHakkenTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KanjiHakkenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    MainPage(
                        onKillRequested = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun MainPage(
    onKillRequested: () -> Unit
) {
    var context = LocalContext.current
    var scope = rememberCoroutineScope()
    var selectedSourceType by remember { mutableStateOf<SourceType?>(null) }
    var tab by remember { mutableStateOf(Tab.Main) }
    var images by remember { mutableStateOf<List<Bitmap>?>(null) }
    var texts by remember { mutableStateOf<List<String?>?>(null) }
    var loadingJob by remember { mutableStateOf<Job?>(null) }
    var loadingPercentage by remember { mutableStateOf(0f) }
    var loadingMessage by remember { mutableStateOf("") }
    var pagerState = rememberPagerState(pageCount = { 2 })

    BackHandler {
        if (selectedSourceType != null)
            selectedSourceType = null
        else
            onKillRequested()
    }

    AnimatedVisibility(loadingJob == null && images == null && selectedSourceType == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            SourceTypeSelector(onSourceTypeSelected = { selectedSourceType = it })
        }
    }

    AnimatedVisibility(loadingJob == null && images == null && selectedSourceType == SourceType.Manga) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column {
                BackButton({ selectedSourceType = null })
                MangaSelector(
                    selected = { uri ->
                        loadingMessage = context.getString(R.string.counting_total_images)
                        loadingPercentage = 0f
                        loadingJob = extractImagesFromCbzUriJob(
                            scope = scope,
                            context = context,
                            cbzUri = uri,
                            onImageExtracted = { bitmap, totalCount ->
                                images = images?.toMutableList()?.also { it.add(bitmap) } ?: listOf(bitmap)
                                loadingPercentage = lerp(0.1f, 0.5f, images!!.size.toFloat() / totalCount.toFloat())
                                val size = images!!.size
                                loadingMessage = context.getString(R.string.extracting_images)
                            },
                            onFinished = {
                                loadingJob = null
                                if (images == null) {
                                    return@extractImagesFromCbzUriJob
                                }
                                var totalExtractedTexts = 0
                                loadingJob = CoroutineScope(Dispatchers.IO).launch {
                                    loadingMessage = context.getString(R.string.extracting_text_from_images)
                                    loadingPercentage = 0.5f
                                    texts = List<String?>(images!!.size) { null }
                                    images!!.forEachIndexed { index, bitmap ->
                                        extractTextFromBitmap(
                                            bitmap = bitmap,
                                            onSuccess = { str ->
                                                withContext(Dispatchers.Main) {
                                                    texts = texts!!.toMutableList().also { it[index] = str }
                                                    totalExtractedTexts++
                                                    loadingPercentage = lerp(0.5f, 1f, totalExtractedTexts / images!!.size.toFloat())
                                                    if (totalExtractedTexts == images!!.size) {
                                                        loadingJob = null
                                                        loadingPercentage = 1f
                                                    }
                                                }
                                            },
                                            onFail = {
                                                Log.d("Kanji", "MainPage: could not parse image $index")
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    })
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loadingJob != null) {
            LoadingDialog(
                progress = { loadingPercentage.toFloat() },
                message = loadingMessage,
                onCancel = { loadingJob?.cancel() },
            )
        }

        if (images != null && texts != null && images!!.size == texts!!.size) {
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(pagerState) {
                    if (it == 0) {
                        KankiList(
                            modifier = Modifier.padding(10.dp),
                            entries = listOf(
                                KanjiEntry("学", listOf("学校" to listOf(0, 1), "学ぶ" to listOf(2, 3))),
                                KanjiEntry("生", listOf("学生" to listOf(0, 1), "生まれる" to listOf(2, 3))),
                                KanjiEntry("日", listOf("日本" to listOf(0, 1), "日曜日" to listOf(2, 3))),
                                KanjiEntry("月", listOf("月曜日" to listOf(0, 1), "月" to listOf(2, 3))),
                                KanjiEntry("火", listOf("火曜日" to listOf(0, 1), "火" to listOf(2, 3))),
                                KanjiEntry("水", listOf("水曜日" to listOf(0, 1), "水" to listOf(2, 3))),
                                KanjiEntry("木", listOf("木曜日" to listOf(0, 1), "木" to listOf(2, 3))),
                                KanjiEntry("金", listOf("金曜日" to listOf(0, 1), "金" to listOf(2, 3))),
                                KanjiEntry("土", listOf("土曜日" to listOf(0, 1), "土" to listOf(2, 3))),
                                KanjiEntry("山", listOf("山" to listOf(0, 1), "山登り" to listOf(2, 3))),
                                KanjiEntry("川", listOf("川" to listOf(0, 1), "川遊び" to listOf(2, 3))),
                                KanjiEntry("田", listOf("田" to listOf(0, 1), "田んぼ" to listOf(2, 3))),
                                KanjiEntry("空", listOf("空" to listOf(0, 1), "空港" to listOf(2, 3))),
                                KanjiEntry("海", listOf("海" to listOf(0, 1), "海岸" to listOf(2, 3))),
                                KanjiEntry("風", listOf("風" to listOf(0, 1), "風邪" to listOf(2, 3))),
                            ),
                            onExampleClicked = { kanji, index -> }
                        )
                    }
                    if (it == 1) {
                        MangaDetails(
                            modifier = Modifier.padding(10.dp),
                            images = images!!,
                            texts = texts!!
                        )
                    }
                }
            }
        }
    }

    Text(
        text = "RAM: ${Formatter.formatShortFileSize(context, getAppMemoryUsage(context))}",
        fontFamily = FontFamily.Monospace
    )
}

@Composable
fun BackButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.back),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

enum class SourceType {
    Manga, Website
}

enum class Tab {
    Main, Details
}

@Preview(showSystemUi = true, showBackground = true, locale = "ja")
@Composable
private fun MainPreview() {
    KanjiHakkenTheme {
        MainPage({})
    }
}