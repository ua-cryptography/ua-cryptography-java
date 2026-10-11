package io.github.marketkernel.uacryptography

/** What a signature covers: a XAdES reference, or `content` in CAdES. */
public data class SignedReference(
    /** The reference URI, or `content`. */
    val name: String,
    /** The hash of the digest. */
    val digestAlgorithm: NamedAlgorithm,
    /** The digest, upper-case hex. */
    val digest: String,
)
