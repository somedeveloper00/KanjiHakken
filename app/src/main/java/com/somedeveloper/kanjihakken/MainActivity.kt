package com.somedeveloper.kanjihakken

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.somedeveloper.kanjihakken.Utils.extractKanjiListFromCbzUriJob
import com.somedeveloper.kanjihakken.Utils.extractKanjiListFromWebsiteUriJob
import com.somedeveloper.kanjihakken.Utils.extractKanjiListFromYouTubeUrlJob
import com.somedeveloper.kanjihakken.Utils.getAppMemoryUsage
import com.somedeveloper.kanjihakken.panes.KanjiList
import com.somedeveloper.kanjihakken.panes.LoadingDialog
import com.somedeveloper.kanjihakken.panes.MangaDetails
import com.somedeveloper.kanjihakken.panes.SourceTypeSelector
import com.somedeveloper.kanjihakken.ui.theme.KanjiHakkenTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.net.URL


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

    // for manga
    var mangaImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    // for website
    var websiteUrl by remember { mutableStateOf("") }
    var websiteWebView by remember { mutableStateOf<WebView?>(null) }
    // for YouTube
    var youtubeUrl by remember { mutableStateOf("") }
    var youtubeWebView by remember { mutableStateOf<WebView?>(null) }

    var loadingJob by remember { mutableStateOf<Job>(Job().apply { complete() }) }
    var loadingProgresses by remember { mutableStateOf<List<Pair<Float, String>>>(emptyList<Pair<Float, String>>()) }
    var loadingTitle by remember { mutableStateOf<String>("") }

    var texts by remember { mutableStateOf<List<String>>(emptyList()) }
    var kanjiList by remember { mutableStateOf<List<Pair<String, List<Pair<String, List<Int>>>>>>(emptyList<Pair<String, List<Pair<String, List<Int>>>>>()) }
    var kanjiListState = rememberLazyListState()
    var mangaListState = rememberLazyListState()

    var isInInitialState = remember { derivedStateOf { kanjiList.isEmpty() && !loadingJob.isActive } }
    var isLoading = remember { derivedStateOf { loadingJob.isActive } }
    var pagerState = rememberPagerState(pageCount = { 2 })

    fun resetPage() {
        mangaImageUris = emptyList()
        kanjiList = emptyList()
        texts = emptyList()
        websiteWebView?.stopLoading()
        youtubeWebView?.stopLoading()
        loadingJob.cancelChildren()
        loadingJob.cancel()
        loadingJob = Job().apply { complete() } // to ensure the re-composition will have .isActive set to true
    }

    BackHandler {
        if (!isInInitialState.value) {
            resetPage()
            return@BackHandler
        }
        onKillRequested()
    }

    AnimatedVisibility(isInInitialState.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            SourceTypeSelector(
                onMangaUriSelected = {
                    loadingJob = extractKanjiListFromCbzUriJob(
                        context = context,
                        scope = scope,
                        uri = it,
                        onProgressReport = { loadingProgresses = it },
                        onFinished = { resultKanjiList, resultBitmaps, resultTexts ->
                            mangaImageUris = resultBitmaps
                            texts = resultTexts
                            kanjiList = resultKanjiList
                            loadingJob = Job().apply { complete() }
                        }
                    )
                },
                onWebsiteUriSelected = {
                    websiteUrl = it
                    loadingJob = extractKanjiListFromWebsiteUriJob(
                        context = context,
                        scope = scope,
                        url = URL(it),
                        onProgressReport = { loadingProgresses = it },
                        onFinished = { resultKanjiList ->
                            kanjiList = resultKanjiList
                            loadingJob = Job().apply { complete() }
                        }
                    )
                },
                onYouTubeUriSelected = {
                    youtubeUrl = it
                    loadingJob = extractKanjiListFromYouTubeUrlJob(
                        context = context,
                        scope = scope,
                        url = it,
                        onProgressReport = { loadingProgresses = it },
                        onFinished = { resultKanjiList ->
                            kanjiList = resultKanjiList
                            loadingJob = Job().apply { complete() }
                        }
                    )

                }
            )
        }
    }

    AnimatedVisibility(isLoading.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            LoadingDialog(
                modifier = Modifier.fillMaxWidth(),
                title = loadingTitle,
                progresses = loadingProgresses,
                onCancel = { resetPage() }
            )
        }
    }
    AnimatedVisibility(kanjiList.isNotEmpty()) {
        Column(Modifier.fillMaxSize()) {
            BackButton(onClick = { resetPage() })
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (mangaImageUris.isNotEmpty()) {
                            MangaDetails(
                                modifier = Modifier.padding(10.dp),
                                imageUris = mangaImageUris,
                                texts = texts,
                                lazyListState = mangaListState
                            )
                        } else if (websiteUrl.isNotEmpty()) {
                            DrawWebView(websiteWebView, websiteUrl, {
                                websiteWebView = WebView(context)
                                websiteWebView!!
                            })
                        } else if (youtubeUrl.isNotEmpty()) {
                            DrawWebView(youtubeWebView, youtubeUrl, {
                                youtubeWebView = WebView(context)
                                youtubeWebView!!
                            })
                        }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DrawWebView(
    webView: WebView?,
    url: String,
    onCreateWebViewRequest: () -> WebView
) {
    var progress by remember { mutableFloatStateOf(0f) }

    Column {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { webView?.reload() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                text = if (webView != null && webView.title != null) webView.title!! else url,
                style = MaterialTheme.typography.titleMedium,
                softWrap = true,
                textAlign = TextAlign.Right,
                maxLines = 2
            )
        }
        LinearWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        AndroidView(factory = {
            if (webView == null) {
                val newWebView = onCreateWebViewRequest()
                newWebView.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        progress = 0f
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        progress = 1f
                    }

                    override fun onLoadResource(view: WebView?, url: String?) {
                        progress = if (view != null) view.progress / 100f else 0f
                    }
                }
                newWebView.settings.javaScriptEnabled = true
                newWebView.settings.builtInZoomControls = true
                newWebView.loadUrl(url)
                newWebView
            } else
                webView!!
        })
    }
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

@Preview(showSystemUi = true, showBackground = true, locale = "ja")
@Composable
private fun MainPreview() {
    KanjiHakkenTheme {
        MainPage() {}
    }
}