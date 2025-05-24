package com.somedeveloper.kanjihakken

import android.graphics.Bitmap
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.somedeveloper.kanjihakken.Utils.extractKanjiListFromCbzUriJob
import com.somedeveloper.kanjihakken.Utils.getAppMemoryUsage
import com.somedeveloper.kanjihakken.panes.KanjiList
import com.somedeveloper.kanjihakken.panes.LoadingDialog
import com.somedeveloper.kanjihakken.panes.MangaDetails
import com.somedeveloper.kanjihakken.panes.MangaSelector
import com.somedeveloper.kanjihakken.panes.SourceTypeSelector
import com.somedeveloper.kanjihakken.ui.theme.KanjiHakkenTheme
import kotlinx.coroutines.Job

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KanjiHakkenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    MainPage(onKillRequested = { finish() })
                }
            }
        }
    }
}

@Composable
fun MainPage(
    onKillRequested: () -> Unit,
) {
    var context = LocalContext.current
    var scope = rememberCoroutineScope()

    var selectedSourceType by remember { mutableStateOf<SourceType?>(null) }
    var images by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var texts by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingJob by remember { mutableStateOf<Job>(Job().apply { complete() }) }
    var loadingProgresses by remember { mutableStateOf<List<Pair<Float, String>>>(emptyList<Pair<Float, String>>()) }
    var loadingTitle by remember { mutableStateOf<String>("") }
    var pagerState = rememberPagerState(pageCount = { 2 })
    var kanjiList by remember { mutableStateOf<List<Pair<String, List<Pair<String, List<Int>>>>>>(emptyList<Pair<String, List<Pair<String, List<Int>>>>>()) }

    BackHandler {
        if (selectedSourceType != null)
            selectedSourceType = null
        else
            onKillRequested()
    }

    AnimatedVisibility(!loadingJob.isActive && images.isEmpty() && selectedSourceType == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            SourceTypeSelector(onSourceTypeSelected = { selectedSourceType = it })
        }
    }

    AnimatedVisibility(!loadingJob.isActive && images.isEmpty() && selectedSourceType == SourceType.Manga) {
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
                        loadingTitle = context.getString(R.string.processing_manga)
                        loadingJob = extractKanjiListFromCbzUriJob(
                            context = context,
                            uri = uri,
                            scope = scope,
                            onProgressReport = { loadingProgresses = it },
                            onFinished = { resultKanjiList, resultBitmaps, resultTexts ->
                                images = resultBitmaps
                                texts = resultTexts
                                kanjiList = resultKanjiList
                            }
                        )
                    }
                )
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
        if (images.isNotEmpty())
            BackButton(
                modifier = Modifier.align(Alignment.Start),
                onClick = {
                    loadingJob.cancel()
                    images = emptyList()
                    texts = emptyList()
                    kanjiList = emptyList()
                    selectedSourceType = null
                }
            )
        if (loadingJob.isActive) {
            LoadingDialog(
                modifier = Modifier.fillMaxWidth(),
                title = loadingTitle,
                progresses = loadingProgresses,
                onCancel = {
                    loadingJob.cancel()
                    images = emptyList()
                    texts = emptyList()
                    kanjiList = emptyList()
                    selectedSourceType = null
                    System.gc()
                    Log.d("Kanji", "MainPage: cancelled job")
                }
            )
        }

        if (kanjiList.isNotEmpty()) {
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(pagerState) {
                    if (it == 0) {
                        KanjiList(
                            modifier = Modifier.padding(10.dp),
                            entries = kanjiList,
                            onExampleClicked = { kanji, index ->
                                {
                                    Toast.makeText(context, "Opening page at $index for \"$kanji\"", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    if (it == 1) {
                        MangaDetails(
                            modifier = Modifier.padding(10.dp),
                            images = images,
                            texts = texts
                        )
                    }
                }
            }
        }
    }

    Text(
        text = "RAM: ${Formatter.formatShortFileSize(context, getAppMemoryUsage())}",
        fontFamily = FontFamily.Monospace
    )
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier,
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

@Preview(showSystemUi = true, showBackground = true, locale = "ja")
@Composable
private fun MainPreview() {
    KanjiHakkenTheme {
        MainPage {}
    }
}