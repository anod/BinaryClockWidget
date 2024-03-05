package info.anodsplace.binaryclockwidget

object BinaryTime {
    fun convert(input: Int): Array<ByteArray> {
        return arrayOf(
            (input / 10).toBinaryArray(),
            (input % 10).toBinaryArray(),
        )
    }
}

private fun Int.toBinaryArray(): ByteArray {
    val byteArray = this
        .toString(2)
        .padStart(4, '0')
        .map { (it.code - '0'.code).toByte() }
        .take(4)
        .toByteArray()
    return byteArray
}