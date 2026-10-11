package io.github.marketkernel.uacryptography

/** One verification step. */
public data class VerificationCheck(
    /** What was checked. */
    val name: String,
    /** How it ended. */
    val status: CheckOutcome,
    /** What was found. */
    val details: String,
)
