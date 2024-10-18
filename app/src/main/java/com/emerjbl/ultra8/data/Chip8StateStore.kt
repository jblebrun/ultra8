package com.emerjbl.ultra8.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.chip8.machine.Halt
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** Manages the save states for Chip8 instances. */
class Chip8StateStore(
    private val db: Ultra8Database,
) {
    /** Save the provided [Chip8.State] associated with the program [name]. */
    suspend fun saveState(name: String, state: Chip8.State) {
        db.chip8StateDao().saveState(Chip8ProgramState(name, state.toDbState()))
    }

    /** Look for a previously saved [Chip8.State] for the provided [name]. */
    suspend fun findState(name: String): Chip8.State? {
        return db.chip8StateDao().findByName(name)?.state?.toMachineState()
    }
}

@Dao
interface Chip8ProgramStateDao {
    @Upsert
    suspend fun saveState(state: Chip8ProgramState)

    @Query("SELECT * FROM chip8ProgramState WHERE name == :name LIMIT 1")
    suspend fun findByName(name: String): Chip8ProgramState?
}

/** A program state stored along with the program name. */
@Entity
class Chip8ProgramState(
    @PrimaryKey val name: String,
    @Embedded val state: Chip8State
)

/** An embedding representation of a Chip8.State. */
@TypeConverters(IntArrayTypeConverter::class, HaltTypeConverter::class)
class Chip8State(
    val halt: Halt?,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val vRegisters: IntArray,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val hpRegisters: IntArray,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val stack: IntArray,
    val mem: ByteArray,
    val i: Int,
    val sp: Int,
    val pc: Int,
    @Embedded val gfx: Chip8GraphicsState,
)

class Chip8GraphicsState(
    val hires: Boolean,
    val targetPlane: Int,
    val width: Int,
    val height: Int,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val plane1Data: ByteArray,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val plane2Data: ByteArray,
)

private fun Chip8.State.toDbState(): Chip8State {
    return Chip8State(
        halt = halted,
        vRegisters = v,
        hpRegisters = hp,
        stack = stack,
        mem = mem,
        i = i,
        sp = sp,
        pc = pc,
        gfx = gfx.toDbState()
    )
}

private fun FrameManager.toDbState(): Chip8GraphicsState {
    val frame = nextFrame(null)
    return Chip8GraphicsState(
        hires = hires,
        targetPlane = targetPlane,
        width = frame.width,
        height = frame.height,
        plane1Data = frame.plane1.data.also {
            check(it.size == frame.width * frame.height)
        },
        plane2Data = frame.plane2.data.also {
            check(it.size == frame.width * frame.height)
        }
    )
}

private fun Chip8GraphicsState.toFrameManager(): FrameManager = FrameManager(
    hires,
    targetPlane,
    FrameManager.Frame(width, height, plane1Data, plane2Data)
)

private fun Chip8State.toMachineState(): Chip8.State {
    return Chip8.State(
        halted = halt,
        v = vRegisters,
        hp = hpRegisters,
        stack = stack,
        mem = mem,
        i = i,
        sp = sp,
        pc = pc,
        gfx = gfx.toFrameManager(),
    )
}

@ProvidedTypeConverter
class IntArrayTypeConverter {
    @TypeConverter
    fun intArrayToByteArray(intArray: IntArray): ByteArray {
        val bos = ByteArrayOutputStream(intArray.size * 4)
        val dos = DataOutputStream(bos)
        intArray.forEach { dos.writeInt(it) }
        return bos.toByteArray()
    }

    @TypeConverter
    fun byteArrayToIntArray(byteArray: ByteArray): IntArray {
        val dis = DataInputStream(ByteArrayInputStream(byteArray))
        return IntArray(byteArray.size / 4).apply {
            repeat(this.size) {
                this[it] = dis.readInt()
            }
        }
    }
}

@ProvidedTypeConverter
class HaltTypeConverter {
    @TypeConverter
    fun toString(halt: Halt?): String {
        return when (halt) {
            null -> ""
            is Halt.Exit -> "Exit,${halt.pc}"
            is Halt.Spin -> "Spin,${halt.pc}"
            is Halt.IllegalOpcode -> "IllegalOpcode,${halt.pc},${halt.opcode}"
            is Halt.StackUnderflow -> "StackUnderflow,${halt.pc}"
            is Halt.StackOverflow -> "StackOverflow,${halt.pc}"
            is Halt.InvalidBitPlane -> "InvalidBitPlane,${halt.pc},${halt.x}"
        }
    }

    @TypeConverter
    fun fromString(data: String): Halt? {
        val fields = data.split(",")
        return when (fields[0]) {
            "" -> null
            "Exit" -> Halt.Exit(fields[1].toInt())
            "Spin" -> Halt.Spin(fields[1].toInt())
            "IllegalOpcode" -> Halt.IllegalOpcode(fields[1].toInt(), fields[2].toInt())
            "StackUnderflow" -> Halt.StackUnderflow(fields[1].toInt())
            "StackOverflow" -> Halt.StackOverflow(fields[1].toInt())
            "InvalidBitPlane" -> Halt.InvalidBitPlane(fields[1].toInt(), fields[2].toInt())
            else -> throw IllegalStateException("Unknown halt type $fields[0]")
        }
    }
}
