package io.github.marketkernel.uacryptography

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CertificateDetailsTest {
    private fun detailsOf(subject: String, issuer: String = subject) = CertificateDetails(
        subject = subject,
        issuer = issuer,
        serial = "1",
        validFrom = Instant.EPOCH,
        validTo = Instant.EPOCH,
        signatureAlgorithm = NamedAlgorithm("1.2.3", "Test"),
        isCa = false,
        ocspUrls = emptyList(),
        caIssuerUrls = emptyList(),
        timeStampUrls = emptyList(),
    )

    @Test
    fun theCommonNameIsTheSubjectName() {
        val details = detailsOf("CN=Іван Петренко, SERIALNUMBER=TINUA-1234567890, C=UA", issuer = "O=CA, CN=Root CA")
        assertEquals("Іван Петренко", details.subjectName)
        assertEquals("Root CA", details.issuerName)
        assertEquals("TINUA-1234567890", details.subjectAttribute("SERIALNUMBER"))
    }

    @Test
    fun aQuotedValueKeepsItsCommaAndEscapedQuote() {
        val details = detailsOf("O=\"ТОВ \\\"Тест\\\", Київ\", CN=Signer")
        assertEquals("ТОВ \"Тест\", Київ", details.subjectAttribute("O"))
        assertEquals("Signer", details.subjectName)
    }

    @Test
    fun anAbsentAttributeIsNullAndTheNameFallsBackToTheWholeSubject() {
        val details = detailsOf("O=Organization, C=UA")
        assertNull(details.subjectAttribute("SERIALNUMBER"))
        assertEquals("O=Organization, C=UA", details.subjectName)
        // An attribute type is matched whole, not as the end of another: "CN" is not found in "XCN".
        assertNull(detailsOf("XCN=Other").subjectAttribute("CN"))
    }
}
