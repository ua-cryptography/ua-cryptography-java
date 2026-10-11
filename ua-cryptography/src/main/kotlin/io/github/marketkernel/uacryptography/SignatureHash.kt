package io.github.marketkernel.uacryptography

/** The hash a PKI signature and a certificate are computed over. */
public enum class SignatureHash {
    /** DSTU 7564-256 (Kupyna), the hash of the current national PKI. */
    DSTU7564,

    /** GOST 34.311-95 with the DKE of the key. */
    GOST34311,
}
