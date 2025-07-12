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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.FileOpen
import androidx.compose.material.icons.twotone.ImageSearch
import androidx.compose.material.icons.twotone.Web
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.somedeveloper.kanjihakken.ui.theme.KanjiHakkenTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourceTypeSelector(
    onMangaUriSelected: (Uri) -> Unit,
    onWebsiteUriSelected: (String) -> Unit,
    onYouTubeUriSelected: (String) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.please_follow_one_of_the_cards),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                MangaSelector { onMangaUriSelected(it) }
            }
        }
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                WebsiteSelector { onWebsiteUriSelected(it) }
            }
        }
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                YouTubeSelector { onYouTubeUriSelected(it) }
            }
        }
    }
}

@Composable
fun MangaSelector(
    selected: (Uri) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null)
                selected(uri)
        })
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            launcher.launch(arrayOf("application/zip", "application/octec-stream", "*/*"))
        },
    ) {
        Icon(
            imageVector = Icons.TwoTone.FileOpen,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.select_a_manga),
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
    Text(
        text = stringResource(R.string.select_manga_text),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
fun WebsiteSelector(
    selected: (String) -> Unit
) {
    var url by remember { mutableStateOf<String>("") }
    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = url,
        onValueChange = { url = it },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        trailingIcon = {
            Button(
                onClick = { selected(url) }
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Web,
                    contentDescription = null
                )
            }
        },
    )
    Text(
        text = stringResource(R.string.select_manga_text),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
fun YouTubeSelector(
    selected: (String) -> Unit
) {
    var url by remember { mutableStateOf<String>("") }
    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = url,
        onValueChange = { url = it },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        trailingIcon = {
            Button(
                onClick = { selected(url) }
            ) {
                Icon(
                    imageVector = Icons.TwoTone.ImageSearch,
                    contentDescription = null
                )
            }
        },
    )
    Text(
        text = stringResource(R.string.select_youtube_text),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Preview(
    showSystemUi = true,
    showBackground = true,
    locale = "ja",
//    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SourceSelectorDialogDark() {
    KanjiHakkenTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(30.dp), contentAlignment = Alignment.Center
        ) {
            SourceTypeSelector({}, {}, {})
        }
    }
}