package com.emerjbl.ultra8.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.chip8.machine.Chip8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Using a Nullable type causes the DataStore to crash. */
sealed interface MaybeState {
    class Yes(val state: Chip8.State) : MaybeState
    data object No : MaybeState
}

/** Serialize Chip8State with a simple DataStream approach. */
object Chip8StateSerializer : Serializer<MaybeState> {
    override val defaultValue: MaybeState = MaybeState.No

    override suspend fun readFrom(input: InputStream): MaybeState {
        val dis = DataInputStream(input)
        return try {
            withContext(Dispatchers.IO) {
                Chip8.State(
                    v = dis.readIntArray(16),
                    hp = dis.readIntArray(16),
                    stack = dis.readIntArray(64),
                    mem = run {
                        // Support variable memory lengths.
                        val len = dis.readInt()
                        dis.readByteArray(len)
                    },
                    i = dis.readInt(),
                    sp = dis.readInt(),
                    pc = dis.readInt(),
                    targetPlane = dis.readInt(),
                    gfx = run {
                        val hires = dis.readBoolean()
                        val width = dis.readInt()
                        val height = dis.readInt()
                        val data = dis.readIntArray(width * height)
                        FrameManager(hires, FrameManager.Frame(width, height, data))
                    }
                ).let { MaybeState.Yes(it) }
            }
        } catch (e: Exception) {
            Log.w("Chip8", "Failed to read in the stored Chip8 state", e)
            MaybeState.No
        }
    }

    override suspend fun writeTo(
        t: MaybeState,
        output: OutputStream
    ) {
        val state = when (t) {
            MaybeState.No -> {
                Log.i("Chip8", "Clearing saved state")
                return
            }

            is MaybeState.Yes -> t.state
        }

        val dos = DataOutputStream(output)
        withContext(Dispatchers.IO) {
            dos.writeIntArray(state.v)
            dos.writeIntArray(state.hp)
            dos.writeIntArray(state.stack)
            dos.writeInt(state.mem.size)
            dos.write(state.mem)
            dos.writeInt(state.i)
            dos.writeInt(state.sp)
            dos.writeInt(state.pc)
            dos.writeInt(state.targetPlane)
            dos.writeBoolean(state.gfx.hires)
            val frame = state.gfx.nextFrame(null)
            dos.writeInt(frame.width)
            dos.writeInt(frame.height)
            dos.writeIntArray(frame.data)
        }
    }

    private fun DataOutputStream.writeIntArray(vals: IntArray) {
        vals.forEach { writeInt(it) }
    }

    private fun DataInputStream.read(ints: IntArray) {
        repeat(ints.size) { ints[it] = readInt() }
    }

    private fun DataInputStream.readIntArray(size: Int) =
        IntArray(size).apply { read(this) }

    private fun DataInputStream.readByteArray(size: Int) =
        ByteArray(size).apply { readFully(this) }
}

val Context.chip8StateStore: DataStore<MaybeState> by dataStore(
    fileName = "chip8state.u8b",
    serializer = Chip8StateSerializer
)
