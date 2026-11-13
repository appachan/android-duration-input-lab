package com.github.appachan.durationinputlab

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DropdownsTest : PatternTest("Dropdowns") {
    private fun enterTime(target: String, hour: Int, minute: Int) = enter(target, "click+swipe") {
        "hour:${selectOption("hour_dropdown", hour)} minute:${selectOption("minute_dropdown", minute)}"
    }

    @Test
    fun entersValuesInRange25() {
        enterTime("25:30", 25, 30)
        enterTime("0:00", 0, 0)
        enterTime("25:50", 25, 50)
    }

    @Test
    fun entersMaxValueInRange48() {
        selectMaxHour48()
        enterTime("48:50", 48, 50)
    }
}
