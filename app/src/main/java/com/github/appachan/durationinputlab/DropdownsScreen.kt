package com.github.appachan.durationinputlab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** パターン 1: 時と分の 2 つのドロップダウン。 */
@Composable
fun DropdownsScreen(settings: DurationSettings) {
    var hour by remember(settings.maxHour) { mutableIntStateOf(0) }
    var minute by remember(settings.minuteStep) { mutableIntStateOf(0) }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionDropdown(
                label = "時",
                options = settings.hours,
                selected = hour,
                optionLabel = settings::hourLabel,
                onSelect = { hour = it },
                testTag = "hour_dropdown",
                modifier = Modifier.weight(1f),
            )
            OptionDropdown(
                label = "分",
                options = settings.minutes,
                selected = minute,
                optionLabel = settings::minuteLabel,
                onSelect = { minute = it },
                testTag = "minute_dropdown",
                modifier = Modifier.weight(1f),
            )
        }
        SelectedValue(settings, hour * 60 + minute)
    }
}
