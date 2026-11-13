package com.github.appachan.durationinputlab

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ホイールの 2 つの実装に、setProgress とスワイプで値を入れる。
 * wraps は項目が循環するか。ScrollField は循環し、setProgress の値は項目の番号でなくページの番号になる。
 * itemHeightDp は項目の高さ。maxItemsPerSwipe は 1 回のスワイプで動かす項目の数の上限。
 */
abstract class WheelTest(
    pattern: String,
    private val wraps: Boolean,
    private val itemHeightDp: Float,
    private val maxItemsPerSwipe: Int,
) : PatternTest(pattern) {

    private fun setIndex(tag: String, index: Int, itemCount: Int) {
        val wheel = wheel(tag)
        val current = wheel.rangeCurrent()
        val value = if (wraps) current - current % itemCount + index else index
        if (value != current) wheel.setProgress(value)
    }

    private fun enterBySetProgress(target: String, hourIndex: Int, minuteIndex: Int, hourCount: Int) =
        enter(target, "setProgress") {
            setIndex("hour_wheel", hourIndex, hourCount)
            setIndex("minute_wheel", minuteIndex, MINUTE_COUNT)
            ""
        }

    private fun enterBySwipe(target: String, hourIndex: Int, minuteIndex: Int, hourCount: Int) =
        enter(target, "swipe") {
            val hourSwipes = swipeWheel("hour_wheel", hourIndex, hourCount, wraps, itemHeightDp, maxItemsPerSwipe)
            val minuteSwipes = swipeWheel("minute_wheel", minuteIndex, MINUTE_COUNT, wraps, itemHeightDp, maxItemsPerSwipe)
            "swipes=${hourSwipes + minuteSwipes}"
        }

    @Test
    fun entersValuesInRange25BySetProgress() {
        enterBySetProgress("25:30", 25, 3, 26)
        enterBySetProgress("0:00", 0, 0, 26)
        enterBySetProgress("25:50", 25, 5, 26)
    }

    @Test
    fun entersMaxValueInRange48BySetProgress() {
        selectMaxHour48()
        enterBySetProgress("48:50", 48, 5, 49)
    }

    @Test
    fun entersValuesInRange25BySwipe() {
        enterBySwipe("25:30", 25, 3, 26)
        enterBySwipe("0:00", 0, 0, 26)
        enterBySwipe("25:50", 25, 5, 26)
    }

    @Test
    fun entersMaxValueInRange48BySwipe() {
        selectMaxHour48()
        enterBySwipe("48:50", 48, 5, 49)
    }

    private companion object {
        const val MINUTE_COUNT = 6
    }
}

/**
 * 項目の高さは ScrollFieldDefaults の ItemHeight（internal）の 52dp。
 * ScrollField は VerticalPager に flingBehavior を渡さないので、PagerDefaults.flingBehavior の
 * PagerSnapDistance.atMost(1) により、1 回の操作で 1 ページまでしか動かない。
 */
@RunWith(AndroidJUnit4::class)
class ScrollFieldWheelTest : WheelTest("ScrollFieldWheel", wraps = true, itemHeightDp = 52f, maxItemsPerSwipe = 1)

@RunWith(AndroidJUnit4::class)
class CustomWheelTest : WheelTest("CustomWheel", wraps = false, itemHeightDp = 48f, maxItemsPerSwipe = Int.MAX_VALUE)
