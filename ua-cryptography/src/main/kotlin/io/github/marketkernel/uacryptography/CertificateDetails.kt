package io.github.marketkernel.uacryptography

import java.time.Instant

/** What a certificate says. */
public data class CertificateDetails(
    /** The subject, e.g. `CN=Name, O=Organization, C=UA`. */
    val subject: String,
    /** The issuer. */
    val issuer: String,
    /** The serial number, upper-case hex without leading zeros. */
    val serial: String,
    /** The start of the validity period. */
    val validFrom: Instant,
    /** The end of the validity period. */
    val validTo: Instant,
    /** The algorithm the issuer signed the certificate with. */
    val signatureAlgorithm: NamedAlgorithm,
    /** basicConstraints cA: the certificate of a certification authority. */
    val isCa: Boolean,
    /** The OCSP responders of authorityInfoAccess. */
    val ocspUrls: List<String>,
    /** The caIssuers addresses of authorityInfoAccess. */
    val caIssuerUrls: List<String>,
    /** The time-stamping services of subjectInfoAccess. */
    val timeStampUrls: List<String>,
) {
    /** The common name of the subject (`CN=`), else the whole subject. */
    public val subjectName: String get() = findNameAttribute(subject, COMMON_NAME) ?: subject

    /** The common name of the issuer (`CN=`), else the whole issuer. */
    public val issuerName: String get() = findNameAttribute(issuer, COMMON_NAME) ?: issuer

    /** The value of the attribute [type] (e.g. `O`, `SERIALNUMBER`) of the subject, or null. */
    public fun subjectAttribute(type: String): String? = findNameAttribute(subject, type)

    private companion object {
        const val COMMON_NAME = "CN"
        const val QUOTE = "\""
        const val ESCAPED_QUOTE = "\\\""

        // The attributes of a name as the reports write it: "TYPE=value" separated by ", ". A value with a comma is
        // quoted, a quote inside escaped.
        fun findNameAttribute(name: String, type: String): String? {
            val pattern = Regex("(?:^|, )${Regex.escape(type)}=(\"(?:[^\"\\\\]|\\\\.)*\"|[^,]*)")
            val value = pattern.find(name)?.groupValues?.get(1) ?: return null
            if (!value.startsWith(QUOTE)) {
                return value
            }
            return value.substring(1, value.length - 1).replace(ESCAPED_QUOTE, QUOTE)
        }
    }
}
