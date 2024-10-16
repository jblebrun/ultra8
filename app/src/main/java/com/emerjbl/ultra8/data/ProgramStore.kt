package com.emerjbl.ultra8.data

import android.content.Context
import android.net.Uri
import com.emerjbl.ultra8.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ByteArrayWrapper(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return when (other) {
            is ByteArrayWrapper -> bytes.contentEquals(other.bytes)
            is ByteArray -> bytes.contentEquals(other)
            else -> throw IllegalArgumentException("Can only compare to ByteArray or ByteArrayWrapper")
        }
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

fun ByteArray.wrapped() = ByteArrayWrapper(this)

/** A single program entry. */
data class Program(
    /** The display name for the program. */
    val name: String,
    /** The actual program data. */
    val data: ByteArrayWrapper = ByteArrayWrapper(byteArrayOf())
)

/** The store of programs that Ultra8 can run. */
class ProgramStore(private val context: Context, initScope: CoroutineScope) {
    val initJob = initScope.launch {
        withContext(Dispatchers.IO) {
            _programs.value = R.raw::class.java.fields.map {
                val id = it.getInt(null)
                val bytes = context.resources.openRawResource(id).use {
                    it.readBytes().wrapped()
                }
                Program(it.name, bytes)
            }
        }
    }

    private val _programs = MutableStateFlow(emptyList<Program>())

    val programs: StateFlow<List<Program>>
        get() = _programs.asStateFlow()

    suspend fun addForUri(uri: Uri): Program =
        withContext(Dispatchers.IO) {
            initJob.join()
            val name = withContext(Dispatchers.IO) {
                context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    null
                )?.use {
                    if (it.moveToNext()) {
                        it.getString(0)
                    } else {
                        null
                    }
                } ?: "Unknown name"
            }
            val data = context.contentResolver.openInputStream(uri)!!.use {
                it.readBytes().wrapped()
            }
            val program = Program(name, data)
            if (_programs.value.none { it.name == name }) {
                _programs.value = listOf(program) + _programs.value
            }
            program
        }

    suspend fun forName(name: String): Program? {
        initJob.join()
        return _programs.value.firstOrNull { println("Compare ${it.name} $name"); it.name == name }
    }
}
