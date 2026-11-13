package com.github.appachan.durationinputlab

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** パターン 4: 時と分のステッパー。 */
@Composable
fun StepperScreen(settings: DurationSettings) {
    var hourIndex by remember(settings.maxHour) { mutableIntStateOf(0) }
    var minuteIndex by remember(settings.minuteStep) { mutableIntStateOf(0) }
    Column {
        Stepper(
            title = "時",
            label = settings.hourLabel(settings.hours[hourIndex]),
            speech = settings.hourSpeech(settings.hours[hourIndex]),
            decreaseDescription = "1 時間減らす",
            increaseDescription = "1 時間増やす",
            canDecrease = hourIndex > 0,
            canIncrease = hourIndex < settings.hours.lastIndex,
            onChange = { hourIndex += it },
            testTag = "hour",
        )
        Stepper(
            title = "分",
            label = settings.minuteLabel(settings.minutes[minuteIndex]),
            speech = settings.minuteSpeech(settings.minutes[minuteIndex]),
            decreaseDescription = "${settings.minuteStep} 分減らす",
            increaseDescription = "${settings.minuteStep} 分増やす",
            canDecrease = minuteIndex > 0,
            canIncrease = minuteIndex < settings.minutes.lastIndex,
            onChange = { minuteIndex += it },
            testTag = "minute",
        )
        SelectedValue(settings, settings.hours[hourIndex] * 60 + settings.minutes[minuteIndex])
    }
}

@Composable
private fun Stepper(
    title: String,
    label: String,
    speech: String,
    decreaseDescription: String,
    increaseDescription: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onChange: (Int) -> Unit,
    testTag: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.width(32.dp))
        OutlinedButton(
            onClick = { onChange(-1) },
            enabled = canDecrease,
            modifier = Modifier.testTag("${testTag}_decrease").semantics { contentDescription = decreaseDescription },
        ) { Text("−") }
        Text(
            label,
            style = MaterialTheme.typography.h4,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(120.dp)
                .testTag("${testTag}_value")
                .semantics {
                    contentDescription = speech
                    liveRegion = LiveRegionMode.Polite
                },
        )
        OutlinedButton(
            onClick = { onChange(1) },
            enabled = canIncrease,
            modifier = Modifier.testTag("${testTag}_increase").semantics { contentDescription = increaseDescription },
        ) { Text("+") }
    }
}
