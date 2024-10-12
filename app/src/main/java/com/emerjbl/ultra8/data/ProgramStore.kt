package com.emerjbl.ultra8.data

import android.content.Context
import android.net.Uri
import com.emerjbl.ultra8.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** A single program entry. */
data class Program(
    /** The display name for the program. */
    val name: String,
    /** The URI for locating the program binary data. */
    val dataUri: Uri
)

/** The store of programs that Ultra8 can run. */
class ProgramStore(private val context: Context) {

    private val _programs = MutableStateFlow(
        R.raw::class.java.fields.map {
            val id = it.getInt(null)
            val uri = Uri.parse("android.resource://${context.packageName}/$id")
            Program(it.name, uri)
        }
    )

    val programs: StateFlow<List<Program>>
        get() = _programs.asStateFlow()

    suspend fun data(program: Program) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(program.dataUri)!!.use {
            it.readBytes()
        }
    }

    suspend fun addForUri(uri: Uri): Program =
        withContext(Dispatchers.IO) {
            val name = withContext(Dispatchers.IO) {
                context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    null
                )?.use {
                    println("Checking cursor")
                    if (it.moveToNext()) {
                        it.getString(0)
                    } else {
                        null
                    }
                } ?: "Unknown name"
            }
            val program = Program(name, uri)
            _programs.value = listOf(program) + _programs.value
            program
        }

    fun forName(name: String): Program? {
        println("Program $name? ${_programs.value.size}")
        return _programs.value.firstOrNull { println("Compare ${it.name} $name"); it.name == name }
    }
}
