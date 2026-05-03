package info.anodsplace.binaryclock

object BinaryClockDigits {
    val bitValues = listOf(8, 4, 2, 1)

    fun digitBits(digit: Int): List<Boolean> {
        require(digit in 0..9) { "Digit must be between 0 and 9" }
        return bitValues.map { bit -> (digit and bit) == bit }
    }

    fun timeDigits(hour: Int, minute: Int, second: Int = -1): List<Int> {
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }

        val digits = mutableListOf(
            hour / 10,
            hour % 10,
            minute / 10,
            minute % 10,
        )
        if (second >= 0) {
            require(second in 0..59) { "Second must be between 0 and 59" }
            digits.add(second / 10)
            digits.add(second % 10)
        }
        return digits
    }
}
