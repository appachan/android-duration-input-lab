package com.github.appachan.durationinputlab

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SingleListTest : PatternTest("SingleList") {
    private fun enterTime(target: String, totalMinutes: Int) = enter(target, "click+swipe") {
        selectOption("value_dropdown", totalMinutes)
    }

    @Test
    fun entersValuesInRange25() {
        enterTime("25:30", 25 * 60 + 30)
        enterTime("0:00", 0)
        enterTime("25:50", 25 * 60 + 50)
    }

    @Test
    fun entersMaxValueInRange48() {
        selectMaxHour48()
        enterTime("48:50", 48 * 60 + 50)
    }
}
