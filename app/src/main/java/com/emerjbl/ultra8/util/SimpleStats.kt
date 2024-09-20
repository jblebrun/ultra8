package com.emerjbl.ultra8.util

class SimpleStats {
    var count = 0L
        private set
    var total = 0L
        private set
    var min = Long.MAX_VALUE
        private set
    var max = 0L
        private set
    var average = 0L
        private set

    fun add(value: Long) {
        count++;
        total += value;
        min = minOf(min, value)
        max = maxOf(max, value)
        average = (total + value) / count
    }

    fun clear() {
        count = 0L
        total = 0L
        min = Long.MAX_VALUE
        max = 0L
        average = 0L
    }

    override fun toString() =
        "min = $min; max = $max; avg = $average; count = $count; total = $total"

    inline fun run_every(interval: Int, action: (SimpleStats) -> Unit) {
        if (count >= interval) {
            action(this)
            clear()
        }
    }
}
