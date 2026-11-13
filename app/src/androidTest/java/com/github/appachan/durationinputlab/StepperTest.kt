package com.github.appachan.durationinputlab

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** 分の刻みは 10 分なので、分の 1 タップは 10 分。 */
@RunWith(AndroidJUnit4::class)
class StepperTest : PatternTest("Stepper") {
    @Test
    fun entersValuesInRange25() {
        enter("25:30", "click") { "taps=${tap("hour_increase", 25) + tap("minute_increase", 3)}" }
        enter("0:00", "click") { "taps=${tap("hour_decrease", 25) + tap("minute_decrease", 3)}" }
        enter("25:50", "click") { "taps=${tap("hour_increase", 25) + tap("minute_increase", 5)}" }
    }

    @Test
    fun entersMaxValueInRange48() {
        selectMaxHour48()
        enter("48:50", "click") { "taps=${tap("hour_increase", 48) + tap("minute_increase", 5)}" }
    }
}
