package com.emerjbl.ultra8.chip8.sound

import org.junit.Test
import strikt.assertions.isEqualTo

class SynthesizerTest {

    @Test
    fun pattern_halfDutyCycle() {
        val pattern = Pattern(0UL, 0xFFFFFFFFFFFFFFFFUL)
        for (i in 0 until 64) {
            strikt.api.expectThat(pattern.bit(i)).describedAs("bit $i").isEqualTo(-0.5)
        }
        for (i in 64 until 128) {
            strikt.api.expectThat(pattern.bit(i)).describedAs("bit $i").isEqualTo(0.5)
        }
    }

    @Test
    fun wave_halfDutyCycle() {
        val pattern = Pattern(0UL, 0xFFFFFFFFFFFFFFFFUL)
        val wave = pattern.render(4000, 48000)
        strikt.api.expectThat(wave.size).isEqualTo(1536)
        for (i in 0 until 768) {
            strikt.api.expectThat(wave[i]).describedAs("sample $i").isEqualTo(-0.5f)
        }
        for (i in 768 until 1536) {
            strikt.api.expectThat(wave[i]).describedAs("sample $i").isEqualTo(0.5f)
        }
    }
}
