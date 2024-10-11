package com.emerjbl.ultra8.util

class SimpleStats(
    val unit: String,
    val actionInterval: Int,
    val action: (SimpleStats) -> Unit
) {
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
        count++
        total += value
        min = minOf(min, value)
        max = maxOf(max, value)
        average = (total + value) / count
        if (count >= actionInterval) {
            action(this)
            clear()
        }
    }

    private fun clear() {
        count = 0L
        total = 0L
        min = Long.MAX_VALUE
        max = 0L
        average = 0L
    }

    override fun toString() =
        "min = $min$unit; max = $max$unit; avg = $average$unit; count = $count"
}
