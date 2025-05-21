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
fun KankiList(
    modifier: Modifier = Modifier,
    entries: List<KanjiEntry>,
    onExampleClicked: (String, Int) -> Unit
) {
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        entries.forEachIndexed { index, entry ->
            KanjiEntryView(
                LocalContext.current,
                kanjiEntry = entry,
                isExpanded = expandedIndex != null && index == expandedIndex!!,
                onExpandChanged = { expandedIndex = if (it) index else null },
                onExampleClicked = onExampleClicked
            )
        }
    }
}

@Composable
private fun KanjiEntryView(
    context: Context,
    kanjiEntry: KanjiEntry,
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
                    text = kanjiEntry.kanji,
                    style = MaterialTheme.typography.displayLarge,
                    fontFamily = FontFamily.Serif
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(R.string.occurrences, kanjiEntry.examples.size),
                        textAlign = TextAlign.Right,
                        fontFamily = FontFamily.Serif
                    )
                    Button(
                        modifier = Modifier.padding(start = 10.dp),
                        onClick = {
                            copyToClipboardSafe(kanjiEntry.kanji, context)
                            Toast.makeText(
                                context,
                                context.getString(R.string.copied_to_clipboard, kanjiEntry.kanji),
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
                kanjiEntry.examples.forEachIndexed { index, example ->
                    val shape = when (index) {
                        0 -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                        kanjiEntry.examples.lastIndex -> RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
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
                                copyToClipboardSafe(example.first, context)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.copied_to_clipboard, example.first),
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
                            text = example.first,
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
                            example.second.forEach({
                                Button(
                                    onClick = { onExampleClicked(example.first, it) }
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

class KanjiEntry(kanji: String, examples: List<Pair<String, List<Int>>>) {
    var kanji: String = kanji
    var examples: List<Pair<String, List<Int>>> = examples
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewKanjiListDark() {
    KanjiHakkenTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            KankiList(
                modifier = Modifier.padding(20.dp),
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
                onExampleClicked = { kanji, index -> println("Clicked on $kanji at index $index") }
            )
        }
    }
}
