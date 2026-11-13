package com.github.appachan.durationinputlab

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * パターン 3: ホイール。M2 の範囲で、ScrollField と同じ semantics を付けて自作する。
 * 手触りを探すため、減衰、項目の高さ、見えている項目の数を画面で変えられる。
 */
@Composable
fun CustomWheelScreen(settings: DurationSettings) {
    val hourState = remember(settings.maxHour) { LazyListState() }
    val minuteState = remember(settings.minuteStep) { LazyListState() }
    val hourIndex by hourState.centerIndex()
    val minuteIndex by minuteState.centerIndex()

    // 既定は rememberSnapFlingBehavior と同じ splineBasedDecay。null でないときは exponentialDecay を使う
    var frictionMultiplier by rememberSaveable { mutableStateOf<Float?>(null) }
    var itemHeightDp by rememberSaveable { mutableFloatStateOf(48f) }
    var visibleItemCount by rememberSaveable { mutableIntStateOf(5) }
    val splineBasedDecay = rememberSplineBasedDecay<Float>()
    val decay: DecayAnimationSpec<Float> = remember(frictionMultiplier, splineBasedDecay) {
        frictionMultiplier?.let { exponentialDecay(frictionMultiplier = it) } ?: splineBasedDecay
    }
    val itemHeight = itemHeightDp.dp

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Wheel(
                state = hourState,
                labels = settings.hours.map(settings::hourLabel),
                contentDescription = "時",
                speech = { settings.hourSpeech(settings.hours[it]) },
                decay = decay,
                itemHeight = itemHeight,
                visibleItemCount = visibleItemCount,
                testTag = "hour_wheel",
            )
            Text(":", style = MaterialTheme.typography.h4, modifier = Modifier.semantics { hideFromAccessibility() })
            Wheel(
                state = minuteState,
                labels = settings.minutes.map(settings::minuteLabel),
                contentDescription = "分",
                speech = { settings.minuteSpeech(settings.minutes[it]) },
                decay = decay,
                itemHeight = itemHeight,
                visibleItemCount = visibleItemCount,
                testTag = "minute_wheel",
            )
        }
        SelectedValue(settings, settings.hours[hourIndex] * 60 + settings.minutes[minuteIndex])
        Divider()
        TuningPanel(
            frictionMultiplier = frictionMultiplier,
            onFrictionMultiplierChange = { frictionMultiplier = it },
            itemHeightDp = itemHeightDp,
            onItemHeightDpChange = { itemHeightDp = it },
            visibleItemCount = visibleItemCount,
            onVisibleItemCountChange = { visibleItemCount = it },
        )
    }
}

/** 手触りを変える欄。今の値を、コードに書き写せる形で表示する。 */
@Composable
private fun TuningPanel(
    frictionMultiplier: Float?,
    onFrictionMultiplierChange: (Float?) -> Unit,
    itemHeightDp: Float,
    onItemHeightDpChange: (Float) -> Unit,
    visibleItemCount: Int,
    onVisibleItemCountChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("手触り", style = MaterialTheme.typography.subtitle1)
        Text(
            buildString {
                appendLine(
                    if (frictionMultiplier == null) {
                        "decayAnimationSpec = rememberSplineBasedDecay()"
                    } else {
                        "decayAnimationSpec = exponentialDecay(frictionMultiplier = ${"%.1f".format(frictionMultiplier)}f)"
                    },
                )
                appendLine("itemHeight = ${itemHeightDp.roundToInt()}.dp")
                append("visibleItemCount = $visibleItemCount")
            },
            style = MaterialTheme.typography.body2,
            modifier = Modifier.testTag("wheel_tuning"),
        )
        Row(
            modifier = Modifier
                .toggleable(
                    value = frictionMultiplier != null,
                    role = Role.Switch,
                    onValueChange = { onFrictionMultiplierChange(if (it) 1f else null) },
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("exponentialDecay を使う", modifier = Modifier.weight(1f))
            Switch(checked = frictionMultiplier != null, onCheckedChange = null)
        }
        // frictionMultiplier は大きいほど早く止まる
        Text("摩擦（frictionMultiplier）")
        Slider(
            value = frictionMultiplier ?: 1f,
            onValueChange = { onFrictionMultiplierChange((it * 10).roundToInt() / 10f) },
            enabled = frictionMultiplier != null,
            valueRange = 0.1f..3f,
            steps = 28,
        )
        Text("項目の高さ（dp）")
        Slider(
            value = itemHeightDp,
            onValueChange = { onItemHeightDpChange(it.roundToInt().toFloat()) },
            valueRange = 32f..80f,
            steps = 11,
        )
        Text("見えている項目の数")
        Row(modifier = Modifier.selectableGroup()) {
            listOf(3, 5, 7, 9).forEach { count ->
                Row(
                    modifier = Modifier.selectable(
                        selected = count == visibleItemCount,
                        role = Role.RadioButton,
                        onClick = { onVisibleItemCountChange(count) },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = count == visibleItemCount, onClick = null)
                    Text("$count")
                }
            }
        }
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
    decay: DecayAnimationSpec<Float>,
    itemHeight: Dp,
    visibleItemCount: Int,
    testTag: String,
) {
    val scope = rememberCoroutineScope()
    val selected by state.centerIndex()
    // setProgress で動かしている間は、行き先を値として公開する（ScrollField の targetOption と同じ）。
    // 中央の項目が変わるまで待つと、値の変化が 150ms ほど遅れ、TalkBack がフォーカスのヒントを
    // 読んでいる最中に重なりやすい。重なると、値の読み上げが TalkBack のキューから消される
    var target by remember(state) { mutableStateOf<Int?>(null) }
    val current = target ?: selected
    // rememberSnapFlingBehavior(state) と同じ組み立てで、decayAnimationSpec だけを差し替える
    val flingBehavior: FlingBehavior = remember(state, decay) {
        snapFlingBehavior(
            snapLayoutInfoProvider = SnapLayoutInfoProvider(state, SnapPosition.Center),
            decayAnimationSpec = decay,
            snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        )
    }
    Box(contentAlignment = Alignment.Center) {
        // 中央の枠
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(itemHeight)
                .border(1.dp, MaterialTheme.colors.primary),
        )
        LazyColumn(
            state = state,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemCount / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(96.dp)
                .height(itemHeight * visibleItemCount)
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
                Box(modifier = Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = if (index == selected) MaterialTheme.typography.h4 else MaterialTheme.typography.h6,
                    )
                }
            }
        }
    }
}
