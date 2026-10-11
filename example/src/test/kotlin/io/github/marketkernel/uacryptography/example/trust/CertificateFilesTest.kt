package io.github.marketkernel.uacryptography.example.trust

import java.io.File
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CertificateFilesTest {
    // The public certificates the app bundles; the tests run in the module's directory.
    private val assets = File("src/main/assets/certificates")
    private val nbuRoot = File(assets, "roots/nbu-root-2036-dstu7564.cer").readBytes()

    @Test
    fun aDerCertificateIsItself() {
        assertContentEquals(nbuRoot, splitCertificates(nbuRoot).single())
    }

    @Test
    fun pemBlocksAreDecodedEachOne() {
        val pem = listOf(nbuRoot, nbuRoot).joinToString("\n") {
            "-----BEGIN CERTIFICATE-----\n${Base64.getMimeEncoder().encodeToString(it)}\n-----END CERTIFICATE-----"
        }
        val certificates = splitCertificates(pem.toByteArray())
        assertEquals(2, certificates.size)
        assertContentEquals(nbuRoot, certificates.last())
    }

    @Test
    fun aPkcs7BundleIsSplitIntoItsCertificates() {
        val bundle = File(assets, "intermediates/privatbank-PB-2026-NBU.p7b").readBytes()
        val certificates = splitCertificates(bundle)
        assertTrue(certificates.size > 2, "${certificates.size} certificates")
        // Each is a DER SEQUENCE of its own.
        assertTrue(certificates.all { it[0] == 0x30.toByte() })
    }

    @Test
    fun bytesThatAreNoDerAreRefused() {
        assertFailsWith<IllegalArgumentException> { splitCertificates(byteArrayOf(0x30, 0x7F)) }
    }
}
