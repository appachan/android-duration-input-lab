package com.github.appachan.durationinputlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

/** パターンごとの画面。name を testTag に使う。 */
enum class Pattern(val title: String, val content: @Composable (DurationSettings) -> Unit) {
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    var pattern by rememberSaveable { mutableStateOf<Pattern?>(null) }
    var maxHour by rememberSaveable { mutableIntStateOf(25) }
    var minuteStep by rememberSaveable { mutableIntStateOf(10) }
    var nextDayLabel by rememberSaveable { mutableStateOf(false) }
    val settings = DurationSettings(maxHour, minuteStep, nextDayLabel)

    BackHandler(enabled = pattern != null) { pattern = null }

    Scaffold(
        // targetSdk 35 以降は edge-to-edge になるので、システムバーの分を空ける
        modifier = Modifier.safeDrawingPadding().semantics { testTagsAsResourceId = true },
        topBar = {
            TopAppBar(
                title = { Text(pattern?.title ?: "時間数入力のパターン") },
                navigationIcon = pattern?.let {
                    {
                        IconButton(
                            onClick = { pattern = null },
                            modifier = Modifier.testTag("back").semantics { contentDescription = "戻る" },
                        ) { Text("←") }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            when (val current = pattern) {
                null -> Home(onOpen = { pattern = it })
                else -> {
                    SettingsPanel(settings) {
                        maxHour = it.maxHour
                        minuteStep = it.minuteStep
                        nextDayLabel = it.nextDayLabel
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    current.content(settings)
                }
            }
        }
    }
}

@Composable
private fun Home(onOpen: (Pattern) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Pattern.entries.forEach { pattern ->
            Button(
                onClick = { onOpen(pattern) },
                modifier = Modifier.fillMaxWidth().testTag("open_${pattern.name}"),
            ) { Text(pattern.title) }
        }
    }
}
