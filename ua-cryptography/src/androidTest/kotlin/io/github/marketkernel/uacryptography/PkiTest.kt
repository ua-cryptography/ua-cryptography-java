package io.github.marketkernel.uacryptography

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

// Synthetic keys only, generated per run: no real key, certificate or signature of a person.
private const val PASSWORD = "synthetic-password"
private const val SUBJECT = "CN=Тестовий підписувач,O=Synthetic Test,C=UA"
private const val SIGNER_NAME = "Тестовий підписувач"
private const val DOCUMENT_TEXT = "Тестовий документ\n"

@RunWith(AndroidJUnit4::class)
class PkiTest {
    private val document = NamedBytes("document.txt", DOCUMENT_TEXT.toByteArray())

    // A CAdES names what it signs "content"; XAdES and the containers name the file.
    private class FormatCase(val format: SignatureFormat, val level: String, val signedFile: String)

    private val formatCases = listOf(
        FormatCase(SignatureFormat.CADES, "CAdES-B", "content"),
        FormatCase(SignatureFormat.XADES, "XAdES-B", "document.txt"),
        FormatCase(SignatureFormat.ASIC_S, "CAdES-B", "document.txt"),
        FormatCase(SignatureFormat.ASIC_E, "XAdES-B", "document.txt"),
    )

    @Test
    fun aNewStoreHoldsOnePrivateKeyWithTheSelfSignedCertificate() {
        val entry = getKeyStoreEntries(created.keyStore, PASSWORD).single()
        assertEquals("key", entry.alias)
        assertEquals(KeyStoreEntryKind.PRIVATE_KEY, entry.kind)
        assertEquals(SIGNER_NAME, entry.certificates.single().subjectName)
        assertContentEquals(created.certificate, getCertificate(created.keyStore, PASSWORD))
        assertContentEquals(created.certificate, getCertificate(created.keyStore, PASSWORD, alias = "key"))
    }

    @Test
    fun theCertificateSaysWhatWasAsked() {
        val details = getCertificateDetails(created.certificate)
        assertEquals(SIGNER_NAME, details.subjectName)
        assertEquals("Synthetic Test", details.subjectAttribute("O"))
        assertEquals(details.subject, details.issuer)
        assertEquals(365, Duration.between(details.validFrom, details.validTo).toDays())
        assertTrue(details.signatureAlgorithm.name.contains("DSTU 4145"), details.signatureAlgorithm.name)
    }

    @Test
    fun aWrongPasswordIsRefused() {
        assertFailsWith<WrongPasswordException> { getKeyStoreEntries(created.keyStore, "wrong") }
    }

    @Test
    fun bytesThatAreNoKeyStoreAreUnreadable() {
        assertFailsWith<UnreadableInputException> { getKeyStoreEntries(ByteArray(16), PASSWORD) }
    }

    @Test
    fun aCurveDegreeTableG1DoesNotListIsAnInvalidArgument() {
        assertFailsWith<InvalidArgumentException> {
            runBlocking { createKeyStore(KeyStoreRequest(subject = SUBJECT, m = 256), PASSWORD) }
        }
    }

    @Test
    fun bytesThatAreNoCertificateAreUnreadable() {
        assertFailsWith<UnreadableInputException> { getCertificateDetails(ByteArray(8)) }
    }

    @Test
    fun everyFormatAtLevelBVerifiesAgainstItsOwnCertificateAndFailsTampered() = runBlocking {
        for (case in formatCases) {
            val signature = sign(created.keyStore, PASSWORD, listOf(document), options = SigningOptions(format = case.format))
            val data = if (case.format == SignatureFormat.XADES) listOf(document) else emptyList()

            val report = verify(signature, data = data, trustAnchors = listOf(created.certificate))
            assertTrue(report.isValid, "${case.format}: ${report.signatures.flatMap { it.checks }.map { it.details }}")
            val summary = report.signatures.first { !it.isContainer }
            assertTrue(summary.level.orEmpty().startsWith(case.level), "${case.format}: ${summary.level}")
            assertEquals(SIGNER_NAME, summary.signer?.subjectName)
            assertNotNull(summary.signingTime)
            assertTrue(case.signedFile in report.signedFiles, "${case.format}: ${report.signedFiles}")

            assertTamperedFails(case.format, signature)
        }
    }

    @Test
    fun aDetachedCadesVerifiesWithItsDataAndNotWithOtherData() = runBlocking {
        val options = SigningOptions(isDetached = true, hash = SignatureHash.GOST34311)
        val signature = sign(created.keyStore, PASSWORD, listOf(document), options = options)
        val report = verify(signature, data = listOf(document), trustAnchors = listOf(created.certificate))
        assertTrue(report.isValid)
        assertEquals("detached", report.signatures.single().packaging)
        assertTrue(report.signatures.single().digestAlgorithm?.name.orEmpty().contains("34.311"))

        val other = NamedBytes("document.txt", "other".toByteArray())
        assertFalse(verify(signature, data = listOf(other), trustAnchors = listOf(created.certificate)).isValid)
    }

    @Test
    fun withoutTheTrustAnchorThePathIsNotTrusted() = runBlocking {
        val signature = sign(created.keyStore, PASSWORD, listOf(document))
        val report = verify(signature)
        assertFalse(report.isValid && report.warningCount == 0)
    }

    @Test
    fun aTimeStampingServiceThatDoesNotAnswerIsAServiceFailure() {
        val options = SigningOptions(level = SignatureLevel.T, tsaUrl = "http://127.0.0.1:9/tsp")
        assertFailsWith<ServiceFailureException> {
            runBlocking { sign(created.keyStore, PASSWORD, listOf(document), options = options) }
        }
    }

    @Test
    fun twoFilesForCadesAreAnInvalidArgument() {
        val second = NamedBytes("second.txt", "second".toByteArray())
        assertFailsWith<InvalidArgumentException> {
            runBlocking { sign(created.keyStore, PASSWORD, listOf(document, second)) }
        }
    }

    @Test
    fun bytesOfNoSignatureFormatAreUnreadable() {
        assertFailsWith<UnreadableInputException> { runBlocking { verify("not a signature".toByteArray()) } }
    }

    private suspend fun assertTamperedFails(format: SignatureFormat, signature: ByteArray) {
        val trustAnchors = listOf(created.certificate)
        when (format) {
            SignatureFormat.XADES -> {
                val tamperedData = listOf(NamedBytes(document.name, "Інший документ\n".toByteArray()))
                val tampered = verify(signature, data = tamperedData, trustAnchors = trustAnchors)
                assertFalse(tampered.isValid)
                assertTrue(tampered.failureCount > 0)
            }
            SignatureFormat.CADES -> {
                val tampered = verify(flipDocumentByte(signature), trustAnchors = trustAnchors)
                assertFalse(tampered.isValid)
                assertTrue(tampered.failureCount > 0)
            }
            // The ZIP checksum of the entry gives a changed document away before any signature check.
            SignatureFormat.ASIC_S, SignatureFormat.ASIC_E -> assertFailsWith<UnreadableInputException> {
                verify(flipDocumentByte(signature), trustAnchors = trustAnchors)
            }
        }
    }

    // An enveloped CAdES and an ASiC container carry the document as is; one of its bytes changed.
    private fun flipDocumentByte(signature: ByteArray): ByteArray {
        val text = DOCUMENT_TEXT.toByteArray()
        val start = (0..signature.size - text.size).firstOrNull { index ->
            text.indices.all { offset -> signature[index + offset] == text[offset] }
        } ?: error("The signature does not carry the document uncompressed")
        return signature.copyOf().also { it[start] = (it[start].toInt() xor 1).toByte() }
    }

    companion object {
        private lateinit var created: CreatedKeyStore

        @JvmStatic
        @BeforeClass
        fun createStore() {
            created = runBlocking { createKeyStore(KeyStoreRequest(subject = SUBJECT), PASSWORD) }
        }
    }
}
