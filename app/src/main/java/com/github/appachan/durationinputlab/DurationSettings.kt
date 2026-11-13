package com.github.appachan.durationinputlab

/**
 * 入力できる値の設定。値は 0:00 から maxHour 時 (60 - minuteStep) 分までを、minuteStep 分刻みで持つ。
 */
data class DurationSettings(
    val maxHour: Int,
    val minuteStep: Int,
    val nextDayLabel: Boolean,
) {
    val hours: List<Int> get() = (0..maxHour).toList()
    val minutes: List<Int> get() = (0 until 60 step minuteStep).toList()

    /** 選べる値をすべて、分で返す。 */
    val values: List<Int> get() = hours.flatMap { h -> minutes.map { m -> h * 60 + m } }

    fun valueLabel(totalMinutes: Int): String {
        val minute = "%02d".format(totalMinutes % 60)
        return "${hourLabel(totalMinutes / 60)}:$minute"
    }

    fun hourLabel(hour: Int): String = if (nextDayLabel && hour >= 24) "翌${hour - 24}" else "$hour"

    fun minuteLabel(minute: Int): String = "%02d".format(minute)

    /** TalkBack に読ませる時の文字。 */
    fun hourSpeech(hour: Int): String = if (nextDayLabel) "${hourLabel(hour)}時" else "${hour}時間"

    fun minuteSpeech(minute: Int): String = "${minute}分"
}
