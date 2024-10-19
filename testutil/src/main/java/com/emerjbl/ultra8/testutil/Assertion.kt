package com.emerjbl.ultra8.testutil

import com.emerjbl.ultra8.chip8.machine.Chip8
import strikt.api.Assertion
import strikt.assertions.contentEquals
import strikt.assertions.isEqualTo

fun Assertion.Builder<Chip8.State>.contentEquals(other: Chip8.State): Assertion.Builder<Chip8.State> =
    assert("State contents are the same") { subject ->
        strikt.api.expectThat(subject.v).describedAs("V: %s")
            .contentEquals(other.v)
        strikt.api.expectThat(subject.hp).describedAs("HP: %s")
            .contentEquals(other.hp)
        strikt.api.expectThat(subject.stack).describedAs("Stack: %s")
            .contentEquals(other.stack)
        strikt.api.expectThat(subject.mem).describedAs("Mem: %s")
            .contentEquals(other.mem)
        strikt.api.expectThat(subject.i).describedAs("I: %s")
            .isEqualTo(other.i)
        strikt.api.expectThat(subject.sp).describedAs("SP: %s")
            .isEqualTo(other.sp)
        strikt.api.expectThat(subject.pc).describedAs("PC: %s")
            .isEqualTo(other.pc)
        strikt.api.expectThat(subject.gfx.targetPlane).describedAs("TargetPlane: %s")
            .isEqualTo(other.gfx.targetPlane)
        val subjectFrame = subject.gfx.nextFrame(null)
        val otherFrame = other.gfx.nextFrame(null)
        strikt.api.expectThat(subjectFrame.width).isEqualTo(otherFrame.width)
        strikt.api.expectThat(subjectFrame.height).isEqualTo(otherFrame.height)
        strikt.api.expectThat(subjectFrame.plane1.data).contentEquals(otherFrame.plane1.data)
        strikt.api.expectThat(subjectFrame.plane2.data).contentEquals(otherFrame.plane2.data)

    }
