package com.emerjbl.ultra8

import com.emerjbl.ultra8.chip8.graphics.SimpleGraphics
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.runner.Chip8ThreadRunner
import com.emerjbl.ultra8.chip8.sound.Chip8Sound
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class FakeSound : Chip8Sound {
    override fun play(ticks: Int) {}
}

@RunWith(RobolectricTestRunner::class)
class ScreenThreadTest {
    val gfx = SimpleGraphics()

    val runner = Chip8ThreadRunner(
        Chip8Keys(),
        gfx,
        FakeSound(),
        TimeSource.Monotonic
    ).apply {
        cyclesPerSecond = 10000
    }

    val breakoutProgram = javaClass.classLoader
        ?.getResourceAsStream("breakout")
        ?.readBytes()!!

    @Test
    fun reloadProgram_screenStartsClear() {
        for (attempt in 0..1000) {
            runner.load(breakoutProgram)
            runner.resume()
            Thread.sleep(10)
            runner.load(breakoutProgram)
            runner.pause()
            val frame = gfx.nextFrame()
            for (i in 0 until frame.data.size) {
                strikt.api
                    .expectThat(frame.data[i])
                    .describedAs("The pixel at $i in attempt #$attempt")
                    .isEqualTo(0)
            }
        }
    }

    @OptIn(FlowPreview::class)
    @Test
    fun changeProgram_keepsRunningOnResume() {
        for (attempt in 0..10000) {
            strikt.api.expectCatching {
                runner.load(breakoutProgram)
                runner.pause()
                runner.running.filter { !it }.timeout(1.seconds).first()
                runner.resume()
                runner.running.filter { it }.timeout(1.seconds).first()
                runner.pause()
                runner.running.filter { !it }.timeout(1.seconds).first()
                runner.resume()
                runner.running.filter { it }.timeout(1.seconds).first()
                runner.load(breakoutProgram)
                runner.running.filter { it }.timeout(1.seconds).first()
            }.describedAs("During attempt $attempt").isSuccess()
        }
    }
}
