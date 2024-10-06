package com.emerjbl.ultra8.chip8.machine

import org.junit.Test
import strikt.assertions.isEqualTo

class Chip8InstructionTest {

    @Test
    fun chip8Instruction_hasCorrectFields() {
        val inst = Chip8Instruction(0x12, 0x34)
        strikt.api.expectThat(inst.word).isEqualTo(0x1234)
        strikt.api.expectThat(inst.nnn).isEqualTo(0x0234)
        strikt.api.expectThat(inst.x).isEqualTo(0x02)
        strikt.api.expectThat(inst.y).isEqualTo(0x03)
        strikt.api.expectThat(inst.majOp).isEqualTo(0x10)
        strikt.api.expectThat(inst.subOp).isEqualTo(0x04)
        strikt.api.expectThat(inst.n).isEqualTo(0x04)
    }
}
