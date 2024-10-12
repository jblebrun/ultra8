package com.emerjbl.ultra8

import android.app.Application
import com.emerjbl.ultra8.data.Chip8StateStore
import com.emerjbl.ultra8.data.ProgramStore

interface Provider {
    val chip8StateStore: Chip8StateStore
    val programStore: ProgramStore
}

class Ultra8Application : Application() {
    val provider = object : Provider {
        override val chip8StateStore by lazy { Chip8StateStore(this@Ultra8Application) }
        override val programStore by lazy { ProgramStore(this@Ultra8Application) }
    }
}
