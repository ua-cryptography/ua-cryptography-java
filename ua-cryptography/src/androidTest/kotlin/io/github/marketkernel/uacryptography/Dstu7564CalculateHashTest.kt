package io.github.marketkernel.uacryptography

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Dstu7564CalculateHashTest {
    // The examples of DSTU 7564:2014 (as in BouncyCastle's DSTU7564Test), from ua-cryptography-dstu7564/tests/kupyna.rs
    // of ua-cryptography-rust: the message is the n bytes 00 01 02 … (n − 1), or FF.
    private class StandardExample(val hashBits: Int, val message: ByteArray, val expectedHash: String)

    private val standardExamples = listOf(
        StandardExample(256, sequentialBytes(64), "08F4EE6F1BE6903B324C4E27990CB24EF69DD58DBE84813EE0A52F6631239875"),
        StandardExample(256, sequentialBytes(128), "0A9474E645A7D25E255E9E89FFF42EC7EB31349007059284F0B182E452BDA882"),
        StandardExample(256, sequentialBytes(256), "D305A32B963D149DC765F68594505D4077024F836C1BF03806E1624CE176C08F"),
        StandardExample(256, byteArrayOf(0xFF.toByte()), "EA7677CA4526555680441C117982EA14059EA6D0D7124D6ECDB3DEEC49E890F4"),
        StandardExample(256, sequentialBytes(95), "1075C8B0CB910F116BDA5FA1F19C29CF8ECC75CAFF7208BA2994B68FC56E8D16"),
        StandardExample(256, sequentialBytes(0), "CD5101D1CCDF0D1D1F4ADA56E888CD724CA1A0838A3521E7131D4FB78D0F5EB6"),
        StandardExample(
            384,
            sequentialBytes(95),
            "D9021692D84E5175735654846BA751E6D0ED0FAC36DFBC0841287DCB0B5584C75016C3DECC2A6E47C50B2F3811E351B8",
        ),
        StandardExample(
            512,
            sequentialBytes(64),
            "3813E2109118CDFB5A6D5E72F7208DCCC80A2DFB3AFDFB02F46992B5EDBE536B" +
                "3560DD1D7E29C6F53978AF58B444E37BA685C0DD910533BA5D78EFFFC13DE62A",
        ),
        StandardExample(
            512,
            byteArrayOf(0xFF.toByte()),
            "871B18CF754B72740307A97B449ABEB32B64444CC0D5A4D65830AE5456837A72" +
                "D8458F12C8F06C98C616ABE11897F86263B5CB77C420FB375374BEC52B6D0292",
        ),
        StandardExample(
            512,
            sequentialBytes(0),
            "656B2F4CD71462388B64A37043EA55DBE445D452AECD46C3298343314EF04019" +
                "BCFA3F04265A9857F91BE91FCE197096187CEDA78C9C1C021C294A0689198538",
        ),
    )

    @Test
    fun theLoadedNativeLibraryIsThePinnedRelease() {
        assertEquals(BuildConfig.NATIVE_LIBRARY_VERSION, getVersion())
    }

    @Test
    fun kupynaGivesTheHashesOfTheStandard() {
        for (example in standardExamples) {
            assertEquals(
                example.expectedHash,
                toHex(dstu7564CalculateHash(example.message, example.hashBits)),
                "Kupyna-${example.hashBits} of ${example.message.size} bytes",
            )
        }
    }

    @Test
    fun aHashSizeTheStandardDoesNotDefineIsAnInvalidArgumentWithTheLibraryMessage() {
        val refusal = assertFailsWith<InvalidArgumentException> { dstu7564CalculateHash(ByteArray(0), 128) }
        assertTrue(refusal.message.isNotEmpty())
    }

    private fun sequentialBytes(count: Int) = ByteArray(count) { index -> index.toByte() }

    private fun toHex(bytes: ByteArray) = bytes.joinToString("") { "%02X".format(it) }
}
