package io.github.marketkernel.uacryptography

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.json.JSONException

// The reports as crates/ua-cryptography-operations of ua-cryptography-rust serializes them.
private const val CERTIFICATE_JSON = """
{
  "subject": "CN=Тестовий підписувач, O=\"Synthetic, Test\", C=UA",
  "issuer": "CN=Тестовий підписувач, O=\"Synthetic, Test\", C=UA",
  "serial": "1A2B",
  "valid_from": "2026-01-01T12:00:25Z",
  "valid_to": "2027-01-01T12:00:25Z",
  "signature_algorithm": {"identifier": "1.2.804.2.1.1.1.1.3.6.1.1", "name": "DSTU 4145 with DSTU 7564-256"},
  "is_ca": false,
  "ocsp_urls": ["http://ocsp.example/"],
  "ca_issuer_urls": [],
  "time_stamp_urls": ["http://tsp.example/"]
}
"""

private const val VERIFICATION_JSON = """
{
  "format": "ASiC-E",
  "is_valid": true,
  "signature_count": 1,
  "warning_count": 1,
  "failure_count": 0,
  "verified_at": "2026-10-11T10:00:00Z",
  "signed_files": ["document.txt"],
  "signatures": [
    {
      "title": "container", "level": null, "packaging": null, "signature_algorithm": null,
      "digest_algorithm": null, "signing_time": null, "timestamp_time": null, "validation_time": null,
      "is_valid": true, "has_warnings": false, "signer": null, "references": [],
      "checks": [{"name": "Container structure", "status": "passed", "details": "mimetype first"}]
    },
    {
      "title": "XAdES signature", "level": "XAdES-B-B", "packaging": "detached",
      "signature_algorithm": {"identifier": "urn:oid:1.2.804.2.1.1.1.1.3.1.1", "name": "DSTU 4145"},
      "digest_algorithm": {"identifier": "urn:oid:1.2.804.2.1.1.1.1.2.2.1", "name": "DSTU 7564-256"},
      "signing_time": "2026-10-11T09:59:00Z", "timestamp_time": null, "validation_time": "2026-10-11T09:59:00Z",
      "is_valid": true, "has_warnings": true, "signer": $CERTIFICATE_JSON,
      "references": [
        {"name": "document.txt", "digest_algorithm": {"identifier": "urn:oid:1.2.804.2.1.1.1.1.2.2.1", "name": "DSTU 7564-256"}, "digest": "0AFF"}
      ],
      "checks": [
        {"name": "Signature value", "status": "passed", "details": "valid"},
        {"name": "Revocation", "status": "warning", "details": "no OCSP response"}
      ]
    }
  ]
}
"""

class ReportReadingTest {
    @Test
    fun aVerificationReportReadsIntoItsSummaries() {
        val summary = readVerificationSummary(VERIFICATION_JSON.toByteArray())

        assertEquals("ASiC-E", summary.format)
        assertTrue(summary.isValid)
        assertTrue(summary.hasWarnings)
        assertEquals(Instant.parse("2026-10-11T10:00:00Z"), summary.verifiedAt)
        assertEquals(listOf("document.txt"), summary.signedFiles)

        val (container, signature) = summary.signatures
        assertTrue(container.isContainer)
        assertNull(container.signer)
        assertFalse(signature.isContainer)
        assertEquals("XAdES-B-B", signature.level)
        assertEquals("DSTU 7564-256", signature.digestAlgorithm?.name)
        assertNull(signature.timestampTime)
        assertEquals("Тестовий підписувач", signature.signer?.subjectName)
        assertEquals("0AFF", signature.references.single().digest)
        assertEquals(listOf(CheckOutcome.PASSED, CheckOutcome.WARNING), signature.checks.map { it.status })
    }

    @Test
    fun certificateDetailsReadEveryField() {
        val details = readCertificateDetails(CERTIFICATE_JSON.toByteArray())

        assertEquals("1A2B", details.serial)
        assertEquals(Instant.parse("2027-01-01T12:00:25Z"), details.validTo)
        assertEquals("DSTU 4145 with DSTU 7564-256", details.signatureAlgorithm.toString())
        assertFalse(details.isCa)
        assertEquals(listOf("http://ocsp.example/"), details.ocspUrls)
        assertEquals(emptyList(), details.caIssuerUrls)
        assertEquals(listOf("http://tsp.example/"), details.timeStampUrls)
    }

    @Test
    fun keyStoreEntriesReadTheirKind() {
        val json = """[{"alias": "key", "kind": "private_key", "created": "2026-01-01T00:00:00Z", "certificates": [$CERTIFICATE_JSON]},
            {"alias": "root", "kind": "trusted_certificate", "created": "2026-01-02T00:00:00Z", "certificates": []}]"""

        val entries = readKeyStoreEntries(json.toByteArray())

        assertEquals(listOf(KeyStoreEntryKind.PRIVATE_KEY, KeyStoreEntryKind.TRUSTED_CERTIFICATE), entries.map { it.kind })
        assertEquals("Тестовий підписувач", entries.first().certificates.single().subjectName)
    }

    @Test
    fun aReportOfAnotherShapeIsRefusedNamingTheField() {
        val failure = assertFailsWith<JSONException> { readCertificateDetails("""{"subject": "CN=A"}""".toByteArray()) }
        assertTrue(failure.message.orEmpty().contains("issuer"), failure.message)
    }
}
