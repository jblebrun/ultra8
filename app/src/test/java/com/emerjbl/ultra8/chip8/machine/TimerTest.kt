package com.emerjbl.ultra8.chip8.machine

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import strikt.assertions.isEqualTo
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

@RunWith(JUnit4::class)
class TimerTest {

    @Test
    fun timer_returnsExpectedValue() {
        val timeSource = TestTimeSource()
        val chip8Timer = Chip8Timer(timeSource)

        chip8Timer.value = 10

        strikt.api.expectThat(chip8Timer.value).isEqualTo(10)
        timeSource += 10.milliseconds
        strikt.api.expectThat(chip8Timer.value).isEqualTo(10)
        timeSource += 10.milliseconds
        strikt.api.expectThat(chip8Timer.value).isEqualTo(9)
        timeSource += 10.milliseconds
        strikt.api.expectThat(chip8Timer.value).isEqualTo(9)
        timeSource += 50.milliseconds
        strikt.api.expectThat(chip8Timer.value).isEqualTo(6)
        timeSource += 87.milliseconds
        strikt.api.expectThat(chip8Timer.value).isEqualTo(0)
        timeSource += 100.milliseconds
        strikt.api.expectThat(chip8Timer.value).isEqualTo(0)
    }
}
