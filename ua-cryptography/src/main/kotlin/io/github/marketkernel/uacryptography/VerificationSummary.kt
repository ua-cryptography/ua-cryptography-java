package io.github.marketkernel.uacryptography

import java.time.Instant

/** The verification of a signed file: the verdict and a summary per signature. */
public data class VerificationSummary(
    /** The format recognised, e.g. `ASiC-E`. */
    val format: String,
    /** Every signature holds and none failed a check. */
    val isValid: Boolean,
    /** The signatures, the container's structure report excluded. */
    val signatureCount: Int,
    /** The checks that ended with a warning. */
    val warningCount: Int,
    /** The checks that failed. */
    val failureCount: Int,
    /** When the verification was made. */
    val verifiedAt: Instant,
    /** The names of the files the signatures cover. */
    val signedFiles: List<String>,
    /** The structure report of a container first, then each signature. */
    val signatures: List<SignatureSummary>,
) {
    /** Valid, with a check that ended with a warning. */
    public val hasWarnings: Boolean get() = isValid && warningCount > 0
}
