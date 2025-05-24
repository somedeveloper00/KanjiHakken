package com.somedeveloper.kanjihakken.panes

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.somedeveloper.kanjihakken.R
import com.somedeveloper.kanjihakken.ui.theme.KanjiHakkenTheme
import kotlinx.coroutines.delay

@Composable
fun LoadingDialog(
    modifier: Modifier = Modifier,
    title: String,
    progresses: List<Pair<Float, String>>,
    onCancel: () -> Unit = {},
) {
    var elapsedTime by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedTime += 1
        }
    }

    Column(
        modifier = modifier
            .width(300.dp)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.elapsed_time_s, elapsedTime),
            style = MaterialTheme.typography.bodyMedium
        )
        CircularProgressIndicator()
        progresses.forEach {
            Text(
                modifier = Modifier.height(30.dp),
                text = it.second,
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { it.first }
            )
        }
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            ),
            onClick = { onCancel() }
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    locale = "ja",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun LoadingPreviewDark() {
    KanjiHakkenTheme {
        LoadingDialog(
            modifier = Modifier.fillMaxSize(),
            title = "Processing Website...",
            progresses = listOf(
                Pair(1f, "Downloading Website"),
                Pair(0.4f, "Extracting Texts")
            )
        )
    }
}