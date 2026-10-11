package io.github.marketkernel.uacryptography

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NativeCodesTest {
    @Test
    fun textTravelsAsUtf8() {
        assertContentEquals("Ключ 🔑".toByteArray(Charsets.UTF_8), nativeTextOf("Ключ 🔑", "The alias"))
    }

    @Test
    fun aNulInsideTextIsRefusedRatherThanCutShort() {
        val failure = assertFailsWith<InvalidArgumentException> { nativeTextOf("pass\u0000word", "The password") }
        assertEquals("The password contains a NUL character.", failure.message)
    }

    @Test
    fun aNegativeNumberIsRefusedRatherThanReadAsAHugeOne() {
        assertEquals(257, nativeUnsignedOf(257, "m"))
        assertFailsWith<InvalidArgumentException> { nativeUnsignedOf(-1, "The validity in days") }
    }

    @Test
    fun thePasswordBytesAreZeroedAfterTheCall() {
        var lent = ByteArray(0)
        withNativePassword("secret") { lent = it }
        assertContentEquals(ByteArray(6), lent)
    }

    @Test
    fun theCodesAreThoseOfTheHeader() {
        assertEquals(listOf(0, 1, 2, 3), SignatureFormat.entries.map(::nativeCodeOf))
        assertEquals(listOf(0, 1, 2), SignatureLevel.entries.map(::nativeCodeOf))
        assertEquals(listOf(0, 1), SignatureHash.entries.map(::nativeCodeOf))
    }
}
