package com.github.appachan.durationinputlab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun SettingsPanel(settings: DurationSettings, onChange: (DurationSettings) -> Unit) {
    Column {
        ChoiceRow(
            title = "時の範囲",
            options = listOf(25, 48),
            selected = settings.maxHour,
            optionLabel = { "0〜$it" },
            tagPrefix = "max_hour",
            onSelect = { onChange(settings.copy(maxHour = it)) },
        )
        ChoiceRow(
            title = "分の刻み",
            options = listOf(5, 10, 15),
            selected = settings.minuteStep,
            optionLabel = { "$it 分" },
            tagPrefix = "minute_step",
            onSelect = { onChange(settings.copy(minuteStep = it)) },
        )
        Row(
            modifier = Modifier
                .testTag("next_day_label")
                .toggleable(
                    value = settings.nextDayLabel,
                    role = Role.Switch,
                    onValueChange = { onChange(settings.copy(nextDayLabel = it)) },
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("24 時以降を「翌」で表示", modifier = Modifier.weight(1f))
            Switch(checked = settings.nextDayLabel, onCheckedChange = null)
        }
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    options: List<Int>,
    selected: Int,
    optionLabel: (Int) -> String,
    tagPrefix: String,
    onSelect: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.padding(end = 8.dp))
        Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .testTag("${tagPrefix}_$option")
                        .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == selected, onClick = null)
                    Text(optionLabel(option))
                }
            }
        }
    }
}

/** 選んだ値。UIAutomator2 のテストは testTag の selected_value で読む。 */
@Composable
fun SelectedValue(settings: DurationSettings, totalMinutes: Int) {
    Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("選んだ値: ", style = MaterialTheme.typography.subtitle1)
        Text(
            settings.valueLabel(totalMinutes),
            modifier = Modifier.testTag("selected_value"),
            style = MaterialTheme.typography.h5,
        )
    }
}
