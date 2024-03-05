package info.anodsplace.binaryclockwidget

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class BinaryTimeUnitTest {
    @Test
    fun testBinaryTimeConvertHour() {
        val result = BinaryTime.convert(24)
        assertArrayEquals(byteArrayOf(0, 0, 1, 0), result[0])
        assertArrayEquals(byteArrayOf(0, 1, 0, 0), result[1])
    }

    @Test
    fun testBinaryTimeConvertSec() {
        val result = BinaryTime.convert(59)
        assertArrayEquals(byteArrayOf(0, 1, 0, 1), result[0])
        assertArrayEquals(byteArrayOf(1, 0, 0, 1), result[1])
    }

    @Test
    fun testBinaryTimeConvertPreview() {
        val hour = BinaryTime.convert(19)
        assertArrayEquals(byteArrayOf(0, 0, 0, 1), hour[0])
        assertArrayEquals(byteArrayOf(1, 0, 0, 1), hour[1])
        val minute = BinaryTime.convert(59)
        assertArrayEquals(byteArrayOf(0, 1, 0, 1), minute[0])
        assertArrayEquals(byteArrayOf(1, 0, 0, 1), minute[1])
        val second = BinaryTime.convert(6)
        assertArrayEquals(byteArrayOf(0, 0, 0, 0), second[0])
        assertArrayEquals(byteArrayOf(0, 1, 1, 0), second[1])
    }

    @Test
    fun testBinaryTimeConvertZero() {
        val result = BinaryTime.convert(0)
        assertArrayEquals(byteArrayOf(0, 0, 0, 0), result[0])
        assertArrayEquals(byteArrayOf(0, 0, 0, 0), result[1])
    }
}