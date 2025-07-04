package com.somedeveloper.kanjihakken

import android.net.Uri
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.launch

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
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var texts by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingJob by remember { mutableStateOf<Job>(Job().apply { complete() }) }
    var loadingProgresses by remember { mutableStateOf<List<Pair<Float, String>>>(emptyList<Pair<Float, String>>()) }
    var loadingTitle by remember { mutableStateOf<String>("") }
    var pagerState = rememberPagerState(pageCount = { 2 })
    var kanjiList by remember { mutableStateOf<List<Pair<String, List<Pair<String, List<Int>>>>>>(emptyList<Pair<String, List<Pair<String, List<Int>>>>>()) }
    var kanjiListState = rememberLazyListState()
    var mangaListState = rememberLazyListState()

    fun resetPage() {
        kanjiList = emptyList()
        imageUris = emptyList()
        texts = emptyList()
        selectedSourceType = null
        loadingJob.cancel()
    }

    BackHandler {
        if (kanjiList.isNotEmpty()) {
            resetPage()
            return@BackHandler
        }
        if (selectedSourceType != null) {
            resetPage()
            return@BackHandler
        }
        onKillRequested()
    }

    AnimatedVisibility(!loadingJob.isActive && imageUris.isEmpty() && selectedSourceType == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            SourceTypeSelector(onSourceTypeSelected = { selectedSourceType = it })
        }
    }

    AnimatedVisibility(!loadingJob.isActive && imageUris.isEmpty() && selectedSourceType == SourceType.Manga) {
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
                                imageUris = resultBitmaps
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
        if (imageUris.isNotEmpty())
            BackButton(
                modifier = Modifier.align(Alignment.Start),
                onClick = { resetPage() }
            )
        if (loadingJob.isActive) {
            LoadingDialog(
                modifier = Modifier.fillMaxWidth(),
                title = loadingTitle,
                progresses = loadingProgresses,
                onCancel = { resetPage() }
            )
        }

        if (kanjiList.isNotEmpty()) {
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(pagerState) {
                    if (it == 0) {
                        KanjiList(
                            modifier = Modifier.padding(10.dp),
                            entries = kanjiList,
                            lazyListState = kanjiListState,
                            onExampleClicked = { word, index ->
                                Log.d("Kanji", "MainPage: here in main activity!")
                                Toast.makeText(context, "Opening page at $index for \"$word\"", Toast.LENGTH_SHORT).show()
                                scope.launch {
                                    pagerState.animateScrollToPage(1)
                                    mangaListState.animateScrollToItem(index)
                                }
                            }
                        )
                    }
                    if (it == 1) {
                        MangaDetails(
                            modifier = Modifier.padding(10.dp),
                            imageUris = imageUris,
                            texts = texts,
                            lazyListState = mangaListState
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