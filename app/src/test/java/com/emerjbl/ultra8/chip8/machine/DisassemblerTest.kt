package com.emerjbl.ultra8.chip8.machine

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

@RunWith(JUnit4::class)
class DisassemblerTest {

    @Test
    fun disassemble_instructionsOnly() {
        val bytes = ubyteArrayOf(
            0xD3u, 0x45u,
            0x80u, 0x23u,
            0x12u, 0x34u,
        )

        val disassembly = disassemble(bytes)

        strikt.api.expectThat(disassembly.segments) {
            containsExactly(
                Segment.Code(
                    listOf(
                        Chip8Instruction(0xD3, 0x45),
                        Chip8Instruction(0x80, 0x23),
                        Chip8Instruction(0x12, 0x34)
                    )
                )
            )
        }
    }

    @Test
    fun disassemble_instructionsWithDataAligned() {
        val bytes = ubyteArrayOf(
            0xD3u, 0x45u,
            0x80u, 0x23u,
            0x12u, 0x34u,
            0x99u, 0x99u,
            0x98u, 0x98u,
            0x96u, 0x96u,
            0x95u, 0x95u,
            0x94u, 0x94u,
            0xD7u, 0x83u,
            0x45u, 0x56u,
        )

        val disassembly = disassemble(bytes)

        strikt.api.expectThat(disassembly.segments) {
            containsExactly(
                Segment.Code(
                    listOf(
                        Chip8Instruction(0xD3, 0x45),
                        Chip8Instruction(0x80, 0x23),
                        Chip8Instruction(0x12, 0x34)
                    )
                ),
                Segment.Data(
                    listOf(
                        0x99u, 0x99u, 0x98u, 0x98u, 0x96u, 0x96u, 0x95u, 0x95u, 0x94u, 0x94u
                    )
                ),
                Segment.Code(
                    listOf(
                        Chip8Instruction(0xD7, 0x83),
                        Chip8Instruction(0x45, 0x56),
                    )
                ),
            )
        }
    }

    @Test
    fun disassemble_instructionsAndDataAlignedToString() {
        val bytes = ubyteArrayOf(
            0xD3u, 0x45u,
            0x80u, 0x23u,
            0x12u, 0x34u,
            0x99u, 0x99u,
            0x98u, 0x98u,
            0x96u, 0x96u,
            0x95u, 0x95u,
            0x94u, 0x94u,
            0xD7u, 0x83u,
            0x45u, 0x56u,
        )

        val disassembly = disassemble(bytes)
        strikt.api.expectThat(disassembly.toString()).isEqualTo(
            """
            CODE:
              DRW V3, V4
              XOR V0, V2
              JP 0x0234
            
            DATA:
              0x99 0x99 0x98 0x98 0x96 0x96 0x95 0x95
              0x94 0x94
            
            CODE:
              DRW V7, V8
              SNE V5, 0x56
        """.trimIndent()
        )
    }
}
