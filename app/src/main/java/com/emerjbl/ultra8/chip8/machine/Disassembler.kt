package com.emerjbl.ultra8.chip8.machine

sealed interface Segment {
    data class Code(val instruction: List<Chip8Instruction>) : Segment
    data class Data(val bytes: List<UByte>) : Segment
}

private val byteFormat = HexFormat {
    number {
        this.prefix = "0x"
    }
}

class Disassembly(
    /** The disassembled segments. These represent the contiguous byte-code. */
    val segments: List<Segment>
) {
    override fun toString(): String = segments.joinToString("\n\n") { segment ->
        when (segment) {
            is Segment.Code -> {
                segment.instruction.joinToString(prefix = "CODE:\n", separator = "\n") { "  $it" }
            }

            is Segment.Data -> {
                segment.bytes.chunked(8)
                    .joinToString(prefix = "DATA:\n", separator = "\n") { chunk ->
                        "  ${chunk.joinToString(" ") { it.toHexString(byteFormat) }}"
                    }

            }
        }
    }
}

private sealed interface SegmentBuilder {
    fun build(): Segment
    class Code : SegmentBuilder {
        val instructions = mutableListOf<Chip8Instruction>()
        override fun build(): Segment.Code = Segment.Code(instructions)
    }

    class Data : SegmentBuilder {
        val bytes = mutableListOf<UByte>()
        override fun build(): Segment.Data = Segment.Data(bytes)
    }
}

private class DisassemblyBuilder {
    private val segments = mutableListOf<SegmentBuilder>()

    fun build(): Disassembly = Disassembly(segments.map { it.build() })

    fun data(): SegmentBuilder.Data = segments.lastOrNull().let { currentSegment ->
        when (currentSegment) {
            is SegmentBuilder.Data -> currentSegment
            else -> SegmentBuilder.Data().also { segments += it }
        }
    }

    fun code(): SegmentBuilder.Code = segments.lastOrNull().let { currentSegment ->
        when (currentSegment) {
            is SegmentBuilder.Code -> currentSegment
            else -> SegmentBuilder.Code().also { segments += it }
        }
    }
}

private sealed interface Item {
    class Instruction(val i: Chip8Instruction) : Item
    class DataByte(val b: UByte) : Item
}


private fun UByteArray.asItems(): Sequence<Item> {
    data class IndexedItem(val idx: Int, val item: Item?)
    return generateSequence(IndexedItem(0, null)) { (idx, _) ->
        when {
            // All done
            idx >= this.size -> null

            // Last byte, must be data byte
            idx + 1 >= this.size -> IndexedItem(idx + 1, Item.DataByte(this[idx]))

            // Could be instruction
            else -> {
                val inst = Chip8Instruction(this[idx].toInt(), this[idx + 1].toInt())
                if (inst.asm == null) {
                    // Not instruction, consume one byte
                    IndexedItem(idx + 1, Item.DataByte(this[idx]))
                } else {
                    // Instruction, consume both bytes
                    IndexedItem(idx + 2, Item.Instruction(inst))
                }
            }
        }
    }.mapNotNull { it.item }
}

/**
 * A fairly naive disassembler.
 *
 * It progresses through the byte array, attempting to interpret a two-byte sequence as an
 * instruction. When it can't, it switches to a data segment, until it detects valid opcodes again.
 *
 * This is not robust against a lot of structures, but it's a start.
 */
fun disassemble(code: UByteArray): Disassembly =
    code.asItems().fold(DisassemblyBuilder()) { disAssembly, item ->
        disAssembly.apply {
            when (item) {
                is Item.DataByte -> data().bytes += item.b
                is Item.Instruction -> code().instructions += item.i
            }
        }
    }.build()
