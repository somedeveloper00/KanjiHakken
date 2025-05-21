package com.somedeveloper.kanjihakken.panes

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.somedeveloper.kanjihakken.R
import com.somedeveloper.kanjihakken.Utils.copyToClipboardSafe
import com.somedeveloper.kanjihakken.ui.theme.KanjiHakkenTheme

@Composable
fun KanjiList(
    modifier: Modifier = Modifier,
    entries: Map<Char, Map<String, List<Int>>>,
    onExampleClicked: (String, Int) -> Unit
) {
    var expandedKanji by remember { mutableStateOf<Char?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        entries.entries.forEach { (kanji, examples) ->
            KanjiEntryView(
                context = LocalContext.current,
                kanji = kanji,
                examples = examples,
                isExpanded = expandedKanji != null && expandedKanji!! == kanji,
                onExpandChanged = { expandedKanji = if (it) kanji else null },
                onExampleClicked = onExampleClicked
            )
        }
    }
}

@Composable
private fun KanjiEntryView(
    context: Context,
    kanji: Char,
    examples: Map<String, List<Int>>,
    isExpanded: Boolean,
    onExpandChanged: (Boolean) -> Unit,
    onExampleClicked: (String, Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape = MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer),
        onClick = { onExpandChanged(!isExpanded) }
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxSize()
                .animateContentSize()
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = kanji.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontFamily = FontFamily.Serif
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(R.string.occurrences, examples.size),
                        textAlign = TextAlign.Right,
                        fontFamily = FontFamily.Serif
                    )
                    Button(
                        modifier = Modifier.padding(start = 10.dp),
                        onClick = {
                            val kanjiStr = kanji.toString()
                            copyToClipboardSafe(kanjiStr, context)
                            Toast.makeText(
                                context,
                                context.getString(R.string.copied_to_clipboard, kanjiStr),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            if (isExpanded) {
                examples.entries.withIndex().forEach { (index, entry) ->
                    var word = entry.key
                    var references = entry.value
                    val shape = when (index) {
                        0 -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                        examples.size - 1 -> RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                copyToClipboardSafe(word, context)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.copied_to_clipboard, word),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = word,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Serif
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            itemVerticalAlignment = Alignment.Top,
                        ) {
                            references.forEach({
                                Button(
                                    onClick = { onExampleClicked(word, it) }
                                ) {
                                    Text(
                                        text = it.toString()
                                    )
                                }
                            })
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewKanjiListDark() {
    KanjiHakkenTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            KanjiList(
                modifier = Modifier.padding(20.dp),
                entries = mapOf(
                    '金' to mapOf(
                        "金曜日" to listOf(1, 2, 3),
                        "お金" to listOf(4, 5)
                    ),
                    '月' to mapOf(
                        "月曜日" to listOf(6, 7),
                        "お月見" to listOf(8)
                    ),
                    '火' to mapOf(
                        "火曜日" to listOf(9, 10),
                        "火山" to listOf(11)
                    ),
                    '水' to mapOf(
                        "水曜日" to listOf(12, 13),
                        "水族館" to listOf(14)
                    ),
                    '木' to mapOf(
                        "木曜日" to listOf(15, 16),
                        "木材" to listOf(17)
                    ),
                    '得' to mapOf(
                        "得点" to listOf(15, 16),
                        "得意" to listOf(17)
                    ),
                ),
                onExampleClicked = { kanji, index -> println("Clicked on $kanji at index $index") }
            )
        }
    }
}
