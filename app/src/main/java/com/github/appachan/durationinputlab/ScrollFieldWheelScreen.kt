package com.github.appachan.durationinputlab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollField
import androidx.compose.material3.ScrollFieldDefaults
import androidx.compose.material3.ScrollFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberScrollFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** パターン 3: ホイール。material3 の ScrollField を使う。 */
@Composable
fun ScrollFieldWheelScreen(settings: DurationSettings) {
    // rememberScrollFieldState は項目数が変わってもページを引き継ぎ、選んでいる項目が変わる。
    // 設定を変えたら選んでいる項目を 0 に戻すため、作り直す
    key(settings.maxHour, settings.minuteStep) {
        ScrollFieldWheels(settings)
    }
}

@Composable
private fun ScrollFieldWheels(settings: DurationSettings) {
    val hourState = rememberScrollFieldState(itemCount = settings.hours.size)
    val minuteState = rememberScrollFieldState(itemCount = settings.minutes.size)
    Column {
        MaterialTheme {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledScrollField(
                    state = hourState,
                    contentDescription = "時",
                    speech = { settings.hourSpeech(settings.hours[it]) },
                    label = { settings.hourLabel(settings.hours[it]) },
                    testTag = "hour_wheel",
                )
                Text(":", style = MaterialTheme.typography.displayMedium, modifier = Modifier.semantics { hideFromAccessibility() })
                LabeledScrollField(
                    state = minuteState,
                    contentDescription = "分",
                    speech = { settings.minuteSpeech(settings.minutes[it]) },
                    label = { settings.minuteLabel(settings.minutes[it]) },
                    testTag = "minute_wheel",
                )
            }
        }
        SelectedValue(
            settings,
            settings.hours[hourState.selectedOption] * 60 + settings.minutes[minuteState.selectedOption],
        )
    }
}

@Composable
private fun LabeledScrollField(
    state: ScrollFieldState,
    contentDescription: String,
    speech: (Int) -> String,
    label: (Int) -> String,
    testTag: String,
) {
    ScrollField(
        state = state,
        contentDescription = contentDescription,
        modifier = Modifier.size(width = 120.dp, height = ScrollFieldDefaults.ScrollFieldHeight).testTag(testTag),
        fieldAccessibilityDescription = speech,
        field = { index, selected, _ ->
            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text(
                    label(index),
                    style = if (selected) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium,
                )
            }
        },
    )
}
