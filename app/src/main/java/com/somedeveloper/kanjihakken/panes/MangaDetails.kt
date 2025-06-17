package com.somedeveloper.kanjihakken.panes

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.somedeveloper.kanjihakken.getBitmapFromUri
import com.somedeveloper.kanjihakken.ui.theme.KanjiHakkenTheme

@Composable
fun MangaDetails(
    modifier: Modifier = Modifier,
    imageUris: List<Uri>,
    texts: List<String?>,
    lazyListState: LazyListState
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = lazyListState
    ) {
        itemsIndexed(imageUris) { index, uri ->
            PageResult(
                imageUri = uri,
                text = texts[index]
            )
        }
    }
}

@Composable
private fun PageResult(
    imageUri: Uri,
    text: String?
) {
    var showText by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, imageUri) {
        value = getBitmapFromUri(context, imageUri)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondaryContainer),
        onClick = { showText = !showText }
    ) {
        Box {
            if (bitmapState.value == null) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(100.dp)
                )
            } else {
                Image(
                    bitmap = bitmapState.value!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            androidx.compose.animation.AnimatedVisibility(showText) {
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.secondary)) {
                    SelectionContainer {
                        Text(
                            text = text!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondary,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MangaDetailsPreview() {
    var state = rememberLazyListState()
    KanjiHakkenTheme {
        MangaDetails(
            imageUris = emptyList(),
            texts = emptyList(),
            lazyListState = state
        )
    }
}