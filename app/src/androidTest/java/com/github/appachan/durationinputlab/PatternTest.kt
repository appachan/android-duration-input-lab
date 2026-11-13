package com.github.appachan.durationinputlab

import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.waitForStable
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "DurationInputLab"
private const val TIMEOUT = 5_000L
private const val DRAG_STEPS = 6

/** パターンの画面を開き、値を入れて selected_value を確かめる。結果は logcat の DurationInputLab に出す。 */
abstract class PatternTest(private val pattern: String) {
    protected val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val lastSelected = mutableMapOf<String, Int>()

    @Before
    fun openPattern() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.waitFor(By.res("open_$pattern")).click()
        device.waitFor(By.res("selected_value"))
    }

    protected fun selectMaxHour48() {
        device.findObject(By.res("max_hour_48")).click()
        // Compose は Role.RadioButton の選択を checked として公開する
        device.waitFor(By.res("max_hour_48").checked(true))
    }

    /** block で値を入れ、selected_value が target になるまでの時間を記録する。block は記録する詳細を返す。 */
    protected fun enter(target: String, method: String, block: () -> String) {
        val start = SystemClock.elapsedRealtime()
        val details = block()
        val reached = device.wait(Until.hasObject(By.res("selected_value").text(target)), TIMEOUT)
        val elapsed = SystemClock.elapsedRealtime() - start
        assertTrue(
            "expected $target but was ${device.findObject(By.res("selected_value")).text}",
            reached,
        )
        Log.i(TAG, "pattern=$pattern target=$target method=$method $details ms=$elapsed")
    }

    /**
     * read-only の Exposed dropdown menu を開いて項目をタップする。項目が見えなければ一覧をスワイプして探す。
     * UiObject2.scrollUntil は、メニューから TYPE_VIEW_SCROLLED が届かず 5 回で止まるので使わない。
     */
    protected fun selectOption(dropdownTag: String, option: Int): String {
        device.findObject(By.res(dropdownTag)).click()
        val anyOption = By.res(Pattern.compile("${dropdownTag}_option_\\d+"))
        device.waitFor(anyOption)
        val selector = By.res("${dropdownTag}_option_$option")
        var swipes = 0
        // メニューは前回選んだ項目が見える位置で開くので、前回選んだ値と比べて向きを決める
        val forward = option > (lastSelected[dropdownTag] ?: 0)
        while (!device.hasObject(selector)) {
            val menu = device.findObject(By.scrollable(true))
            val distance = menu.visibleBounds.height() * 3 / 4
            menu.drag(if (forward) distance else -distance)
            swipes++
            assertTrue("option $option not found", swipes < 100)
        }
        // スクロールの直後にタップすると値が選ばれないことがあったので、画面が落ち着くのを待つ
        if (swipes > 0) device.findObject(By.scrollable(true)).waitForStable()
        device.findObject(selector).click()
        device.wait(Until.gone(anyOption), TIMEOUT)
        lastSelected[dropdownTag] = option
        return "swipes=$swipes"
    }

    /**
     * 指を distance px 上へ動かし、止めてから離す。フリングを起こさない。
     * UiObject2.scroll と違い、TYPE_VIEW_SCROLLED を待たない。
     * 中央を挟んで動かす。端から始めるとスクロールしないことがあった。
     */
    protected fun UiObject2.drag(distance: Int) {
        val bounds = visibleBounds
        val x = bounds.centerX()
        val startY = bounds.centerY() + distance / 2
        val endY = startY - distance
        // UiDevice.swipe は 1 ステップごとに次のフレームを待つので、かかる時間はステップの数で決まる。
        // 2 つめの区間は同じ点で、DRAG_STEPS - 1 フレームのあいだ止めてから離す
        device.swipe(arrayOf(Point(x, startY), Point(x, endY), Point(x, endY)), DRAG_STEPS)
        device.waitForIdle()
    }

    protected fun wheel(tag: String): UiObject2 = device.findObject(By.res(tag))

    protected fun UiObject2.rangeCurrent(): Int = accessibilityNodeInfo.rangeInfo.current.roundToInt()

    /** UiObject2.getAccessibilityNodeInfo() 経由で ACTION_SET_PROGRESS を実行する。 */
    protected fun UiObject2.setProgress(value: Int) {
        val arguments = Bundle().apply {
            putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, value.toFloat())
        }
        val performed = accessibilityNodeInfo.performAction(
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
            arguments,
        )
        assertTrue("ACTION_SET_PROGRESS was not performed", performed)
    }

    /** スワイプで index を target にする。wraps が true なら近い向きに回す。スワイプした回数を返す。 */
    protected fun swipeWheel(
        tag: String,
        target: Int,
        itemCount: Int,
        wraps: Boolean,
        itemHeightDp: Float,
        maxItemsPerSwipe: Int,
    ): Int {
        val itemHeightPx = (itemHeightDp * InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density).roundToInt()
        var swipes = 0
        while (true) {
            val wheel = wheel(tag)
            val current = wheel.rangeCurrent().let { if (wraps) it % itemCount else it }
            if (current == target) return swipes
            val forward = if (wraps) (target - current).mod(itemCount) <= itemCount / 2 else target > current
            val remaining = if (wraps) {
                if (forward) (target - current).mod(itemCount) else (current - target).mod(itemCount)
            } else {
                abs(target - current)
            }
            // 1 回のスワイプで、ホイールの高さの 8 割に収まるだけの項目を動かす
            val fit = wheel.visibleBounds.height() * 8 / 10 / itemHeightPx
            val items = minOf(remaining, fit, maxItemsPerSwipe).coerceAtLeast(1)
            wheel.drag(if (forward) items * itemHeightPx else -items * itemHeightPx)
            swipes++
            assertTrue("too many swipes on $tag", swipes < 200)
        }
    }

    protected fun tap(tag: String, times: Int): Int {
        repeat(times) { device.findObject(By.res(tag)).click() }
        return times
    }
}

fun UiDevice.waitFor(selector: BySelector): UiObject2 =
    requireNotNull(wait(Until.findObject(selector), TIMEOUT)) { "$selector not found" }
