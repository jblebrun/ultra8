package com.emerjbl.ultra8

import com.emerjbl.ultra8.chip8.graphics.SimpleGraphics
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.runner.Chip8Runner
import com.emerjbl.ultra8.chip8.runner.Chip8ThreadRunner
import com.emerjbl.ultra8.chip8.sound.Chip8Sound
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import strikt.assertions.isEqualTo
import kotlin.time.TimeSource

class FakeSound : Chip8Sound {
    override fun play(ticks: Int) {}
}

@RunWith(RobolectricTestRunner::class)
class ScreenThreadTest {

    @Test
    fun reloadProgram_screenStartsClear() {
        val gfx = SimpleGraphics()
        val runner = Chip8ThreadRunner(
            Chip8Keys(),
            gfx,
            FakeSound(),
            TimeSource.Monotonic
        )

        val program = javaClass.classLoader
            ?.getResourceAsStream("breakout")
            ?.readBytes()!!

        for (attempt in 0..100) {
            runner.load(program)
            runner.period = Chip8Runner.Period(0, 10)
            runner.resume()
            Thread.sleep(10)
            runner.load(program)
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
}
