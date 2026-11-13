package com.github.appachan.durationinputlab

import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * read-only の Exposed dropdown menu。
 *
 * M2 の ExposedDropdownMenuBox は、メニューを開く onClick を focusable でない要素に付ける。TalkBack は中の
 * TextField にフォーカスし、ダブルタップしても TextField 自身の onClick が動くだけでメニューが開かない。
 * M3 の menuAnchor と同じく、TextField に Role.DropdownList とメニューを開く onClick を付ける。
 * 同じ要素では外側の modifier の semantics があとから適用されるので、TextField の onClick を上書きできる。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OptionDropdown(
    label: String,
    options: List<Int>,
    selected: Int,
    optionLabel: (Int) -> String,
    onSelect: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.testTag(testTag),
    ) {
        TextField(
            modifier = Modifier.semantics {
                role = Role.DropdownList
                onClick {
                    expanded = !expanded
                    true
                }
            },
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        // メニューは別のウィンドウ（Popup）に出るので、ルートの testTagsAsResourceId が届かない
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.semantics { testTagsAsResourceId = true },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    modifier = Modifier.testTag("${testTag}_option_$option"),
                ) {
                    Text(optionLabel(option))
                }
            }
        }
    }
}
