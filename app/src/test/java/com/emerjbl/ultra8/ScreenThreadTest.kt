package com.emerjbl.ultra8

import androidx.core.graphics.get
import com.emerjbl.ultra8.chip8.graphics.FadeBitmapChip8Graphics
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
        val gfx = FadeBitmapChip8Graphics()
        val runner = Chip8ThreadRunner(
            Chip8Keys(),
            gfx,
            FakeSound(),
            TimeSource.Monotonic
        )

        val program = javaClass.classLoader
            ?.getResourceAsStream("breakout")
            ?.readBytes()!!

        for (i in 0..100) {
            runner.load(program)
            runner.period = Chip8Runner.Period(0, 10)
            runner.resume()
            Thread.sleep(10)
            runner.load(program)
            runner.pause()
            val bitmap = gfx.frameBuffer.nextFrame(0)
            for (x in 0 until bitmap.width) {
                for (y in 0 until bitmap.height) {
                    strikt.api
                        .expectThat(bitmap[x, y])
                        .describedAs("The pixel at $x $y in attempt #$i")
                        .isEqualTo(0)
                }
            }
        }
    }
}
