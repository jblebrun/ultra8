package com.emerjbl.ultra8.testutil

import com.emerjbl.ultra8.data.MaybeState
import strikt.api.Assertion
import strikt.assertions.contentEquals
import strikt.assertions.isEqualTo

fun Assertion.Builder<MaybeState>.contentEquals(other: MaybeState): Assertion.Builder<MaybeState> =
    assert("State contents are the same") { maybeSubject ->
        if (maybeSubject == MaybeState.No) {
            strikt.api.expectThat(other).isEqualTo(MaybeState.No)
        } else {
            val subject = (maybeSubject as MaybeState.Yes).state
            val other = (other as MaybeState.Yes).state
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
            strikt.api.expectThat(subject.targetPlane).describedAs("TargetPlane: %s")
                .isEqualTo(other.targetPlane)
        }
    }
