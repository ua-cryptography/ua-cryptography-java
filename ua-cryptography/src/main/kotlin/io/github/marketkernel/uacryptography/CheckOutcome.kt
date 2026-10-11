package io.github.marketkernel.uacryptography

/** The outcome of one verification step. */
public enum class CheckOutcome {
    /** The step holds. */
    PASSED,

    /** Something could not be established; the signature itself holds. */
    WARNING,

    /** The step fails: the signature is not valid. */
    FAILED,
}
