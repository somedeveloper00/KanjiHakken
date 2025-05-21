package com.somedeveloper.kanjihakken.panes

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.somedeveloper.kanjihakken.ui.theme.KanjiHakkenTheme

@Composable
fun MangaDetails(
    modifier: Modifier = Modifier,
    images: List<Bitmap>,
    texts: List<String?>
) {
    var scrollState = rememberScrollState()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(images) { index, bitmap ->
            PageResult(
                bitmap = bitmap,
                text = texts[index]
            )
        }
    }
}

@Composable
private fun PageResult(
    bitmap: Bitmap,
    text: String?
) {
    var showText by remember { mutableStateOf(false) }
    var textScrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape = MaterialTheme.shapes.small),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondaryContainer),
        onClick = { showText = !showText }
    ) {
        Box {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
            )
            androidx.compose.animation.AnimatedVisibility(showText) {
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.secondary)){
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

@Preview
@Composable
private fun MangaDetailsPreview() {
    KanjiHakkenTheme {
        MangaDetails(
            images = emptyList(),
            texts = emptyList()
        )
    }
}