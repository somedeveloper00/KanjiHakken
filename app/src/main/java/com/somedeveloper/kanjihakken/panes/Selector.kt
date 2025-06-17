package com.somedeveloper.kanjihakken.panes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.somedeveloper.kanjihakken.R
import com.somedeveloper.kanjihakken.SourceType
import com.somedeveloper.kanjihakken.ui.theme.KanjiHakkenTheme

@Composable
fun SourceTypeSelector(
    onSourceTypeSelected: (SourceType) -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.select_source_type),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSourceTypeSelected(SourceType.Manga) },
        ) {
            Text(
                text = stringResource(R.string.manga),
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            onClick = { onSourceTypeSelected(SourceType.Website) }
        ) {
            Text(text = stringResource(R.string.website))
        }
    }
}

@Composable
fun MangaSelector(
    modifier: Modifier = Modifier,
    selected: (Uri) -> Unit = {}
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null)
                selected(uri)
        })
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.select_manga_text),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                launcher.launch(arrayOf("application/zip", "application/octec-stream", "*/*"))
            },
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.select_a_manga),
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

@Preview(
    showSystemUi = true,
    showBackground = true,
    locale = "ja",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun MangaSelectorPreview() {
    KanjiHakkenTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(30.dp), contentAlignment = Alignment.Center
        ) {
            MangaSelector()
        }
    }
}

@Preview(
    showSystemUi = true,
    showBackground = true,
    locale = "ja",
//    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SourceSelectorDialogDark() {
    var selectedSourceType by remember { mutableStateOf<SourceType?>(null) }
    KanjiHakkenTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(30.dp), contentAlignment = Alignment.Center
        ) {
            SourceTypeSelector(
                onSourceTypeSelected = { selectedSourceType = it }
            )
        }
    }
}