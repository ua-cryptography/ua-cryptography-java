package io.github.marketkernel.uacryptography

import java.time.Instant

/** The verification of one signature, or of an ASiC container's structure ([isContainer]). */
public data class SignatureSummary(
    /** What was verified, e.g. `CAdES signature`. */
    val title: String,
    /** The format and level found, e.g. `XAdES-B-LT` or `CAdES-T`. */
    val level: String?,
    /** `detached` or `enveloped`. */
    val packaging: String?,
    /** The signature algorithm. */
    val signatureAlgorithm: NamedAlgorithm?,
    /** The hash the signature is computed over. */
    val digestAlgorithm: NamedAlgorithm?,
    /** The signing time the signer claims. */
    val signingTime: Instant?,
    /** The time of the signature time-stamp, when present and valid. */
    val timestampTime: Instant?,
    /** The time the certificates were validated at: the time-stamp, else the claimed time, else now. */
    val validationTime: Instant?,
    /** No check failed. */
    val isValid: Boolean,
    /** A check ended with a warning. */
    val hasWarnings: Boolean,
    /** The signer's certificate, when found. */
    val signer: CertificateDetails?,
    /** What the signature covers. */
    val references: List<SignedReference>,
    /** Every check in the order made. */
    val checks: List<VerificationCheck>,
) {
    /** The structure report of an ASiC container, not a signature. */
    public val isContainer: Boolean get() = title == CONTAINER_TITLE

    public companion object {
        /** The title the library gives the structure report of a container. */
        public const val CONTAINER_TITLE: String = "container"
    }
}
