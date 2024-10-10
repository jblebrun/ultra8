package com.emerjbl.ultra8

import com.emerjbl.ultra8.chip8.graphics.StandardChip8Font
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.chip8.runner.Chip8ThreadRunner
import com.emerjbl.ultra8.chip8.sound.Chip8Sound
import com.emerjbl.ultra8.chip8.sound.Pattern
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import strikt.assertions.isSuccess
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class FakeSound : Chip8Sound {
    override fun play(ticks: Int) {}
    override fun setPattern(pattern: Pattern) {}
    override fun setPatternRate(patternRate: Int) {}
}

@RunWith(RobolectricTestRunner::class)
class ScreenThreadTest {
    private val runner = Chip8ThreadRunner().apply {
        cyclesPerTick = 2000
    }

    val newMachine = { program: ByteArray ->
        Chip8(
            Chip8Keys(),
            FakeSound(),
            StandardChip8Font,
            TimeSource.Monotonic,
            program
        )
    }

    private val breakoutProgram = javaClass.classLoader
        ?.getResourceAsStream("breakout")
        ?.readBytes()!!

    @Test
    fun changeProgram_keepsRunningOnResume() {
        for (attempt in 0..10000) {
            val machine = newMachine(breakoutProgram)
            strikt.api.expectCatching {
                runner.run(machine)
                runner.pause()
                runner.running.filter { !it }.timeout(1.seconds).first()
                runner.run(machine)
                runner.running.filter { it }.timeout(1.seconds).first()
                runner.pause()
                runner.running.filter { !it }.timeout(1.seconds).first()
                runner.run(machine)
                runner.running.filter { it }.timeout(1.seconds).first()
                runner.run(newMachine(breakoutProgram))
                runner.running.filter { it }.timeout(1.seconds).first()
            }.describedAs("During attempt $attempt").isSuccess()
        }
    }
}
