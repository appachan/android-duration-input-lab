package com.github.appachan.durationinputlab

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ItemHeight = 48.dp
private const val VisibleItemCount = 5

/** パターン 3: ホイール。M2 の範囲で、ScrollField と同じ semantics を付けて自作する。 */
@Composable
fun CustomWheelScreen(settings: DurationSettings) {
    val hourState = remember(settings.maxHour) { LazyListState() }
    val minuteState = remember(settings.minuteStep) { LazyListState() }
    val hourIndex by hourState.centerIndex()
    val minuteIndex by minuteState.centerIndex()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Wheel(
                state = hourState,
                labels = settings.hours.map(settings::hourLabel),
                contentDescription = "時",
                speech = { settings.hourSpeech(settings.hours[it]) },
                testTag = "hour_wheel",
            )
            Text(":", style = MaterialTheme.typography.h4, modifier = Modifier.semantics { hideFromAccessibility() })
            Wheel(
                state = minuteState,
                labels = settings.minutes.map(settings::minuteLabel),
                contentDescription = "分",
                speech = { settings.minuteSpeech(settings.minutes[it]) },
                testTag = "minute_wheel",
            )
        }
        SelectedValue(settings, settings.hours[hourIndex] * 60 + settings.minutes[minuteIndex])
    }
}

/** 中央の項目の番号。上下の contentPadding で、番号 i の項目が先頭に来たとき中央に表示される。 */
@Composable
private fun LazyListState.centerIndex() = remember(this) {
    derivedStateOf {
        val itemHeightPx = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
        firstVisibleItemIndex + if (firstVisibleItemScrollOffset > itemHeightPx / 2) 1 else 0
    }
}

@Composable
private fun Wheel(
    state: LazyListState,
    labels: List<String>,
    contentDescription: String,
    speech: (Int) -> String,
    testTag: String,
) {
    val scope = rememberCoroutineScope()
    val selected by state.centerIndex()
    // setProgress で動かしている間は、行き先を値として公開する（ScrollField の targetOption と同じ）。
    // 中央の項目が変わるまで待つと、値の変化が 150ms ほど遅れ、TalkBack がフォーカスのヒントを
    // 読んでいる最中に重なりやすい。重なると、値の読み上げが TalkBack のキューから消される
    var target by remember(state) { mutableStateOf<Int?>(null) }
    val current = target ?: selected
    Box(contentAlignment = Alignment.Center) {
        // 中央の枠
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(ItemHeight)
                .border(1.dp, MaterialTheme.colors.primary),
        )
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(vertical = ItemHeight * (VisibleItemCount / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(96.dp)
                .height(ItemHeight * VisibleItemCount)
                .testTag(testTag)
                .clearAndSetSemantics {
                    this.contentDescription = contentDescription
                    stateDescription = speech(current)
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = current.toFloat(),
                        range = 0f..labels.lastIndex.toFloat(),
                        steps = labels.size - 2,
                    )
                    setProgress { value ->
                        val index = value.roundToInt().coerceIn(0, labels.lastIndex)
                        if (index == current) return@setProgress false
                        target = index
                        scope.launch {
                            // 指で触って止められたときも、中央の項目の番号に戻す
                            try {
                                state.animateScrollToItem(index)
                            } finally {
                                target = null
                            }
                        }
                        true
                    }
                },
        ) {
            itemsIndexed(labels) { index, label ->
                Box(modifier = Modifier.height(ItemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = if (index == selected) MaterialTheme.typography.h4 else MaterialTheme.typography.h6,
                    )
                }
            }
        }
    }
}
