package com.github.appachan.durationinputlab

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** パターン 2: 値をすべて並べた 1 つのリスト。 */
@Composable
fun SingleListScreen(settings: DurationSettings) {
    var value by remember(settings.maxHour, settings.minuteStep) { mutableIntStateOf(0) }
    Column {
        OptionDropdown(
            label = "時間",
            options = settings.values,
            selected = value,
            optionLabel = settings::valueLabel,
            onSelect = { value = it },
            testTag = "value_dropdown",
            modifier = Modifier.fillMaxWidth(),
        )
        SelectedValue(settings, value)
    }
}
