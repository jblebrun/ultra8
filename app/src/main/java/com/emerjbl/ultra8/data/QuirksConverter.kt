package com.emerjbl.ultra8.data

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.emerjbl.ultra8.chip8.machine.Quirk
import com.emerjbl.ultra8.chip8.machine.Quirks

@ProvidedTypeConverter
class QuirksTypeConverter {
    @TypeConverter
    fun toString(quirks: Quirks): String {
        val serialized = listOf(
            quirks.shiftXOnly.serialize(),
            quirks.memoryIncrementByX.serialize(),
            quirks.memoryIUnchanged.serialize(),
            quirks.spriteWrapQuirk.serialize(),
            quirks.bxnnJumpQuirk.serialize(),
            quirks.vSyncDraw.serialize(),
            quirks.cosmacLogicQuirk.serialize(),
            quirks.overwriteVFQuirk.serialize()
        ).joinToString(",")
        println("QUIRKS: $serialized")
        return serialized
    }

    @TypeConverter
    fun fromString(data: String): Quirks {
        println("QUIRKS FROM $data")
        val items = data.split(",").map { it.toQuirkField() }
        return Quirks(
            shiftXOnly = Quirk.ShiftXOnly(items.findQuirk<Quirk.ShiftXOnly>()),
            memoryIncrementByX = Quirk.MemoryIncrementByX(items.findQuirk<Quirk.MemoryIncrementByX>()),
            memoryIUnchanged = Quirk.MemoryIUnchanged(items.findQuirk<Quirk.MemoryIUnchanged>()),
            spriteWrapQuirk = Quirk.SpriteWrapQuirk(items.findQuirk<Quirk.SpriteWrapQuirk>()),
            bxnnJumpQuirk = Quirk.BXNNJumpQuirk(items.findQuirk<Quirk.BXNNJumpQuirk>()),
            vSyncDraw = Quirk.VSyncDraw(items.findQuirk<Quirk.VSyncDraw>()),
            cosmacLogicQuirk = Quirk.CosmacLogicQuirk(items.findQuirk<Quirk.CosmacLogicQuirk>()),
            overwriteVFQuirk = Quirk.OverwriteVFQuirk(items.findQuirk<Quirk.OverwriteVFQuirk>())
        )
    }
}

data class QuirkField(val name: String, val enabled: Boolean)

inline fun <reified T : Quirk> List<QuirkField>.findQuirk(): Boolean {
    return firstOrNull { it.name == T::class.simpleName }?.enabled ?: false
}

fun String.toQuirkField(): QuirkField {
    val fields = split(":")
    if (fields.size != 2) {
        println("Couldn't understand quirk data $this")
        return QuirkField("", false)
    }
    return QuirkField(fields[0], if (fields[1] == "true") true else false)
}

fun Quirk.serialize(): String {
    val name = this::class.simpleName
    return "$name:$enabled"
}
