package com.emerjbl.ultra8

import android.app.Application
import com.emerjbl.ultra8.data.Chip8StateStore

interface Provider {
    val chip8StateStore: Chip8StateStore
}

class Ultra8Application : Application() {
    val provider = object : Provider {
        override val chip8StateStore = Chip8StateStore(this@Ultra8Application)
    }
}
