package io.github.marketkernel.uacryptography

/** The kind of a JKS entry. */
public enum class KeyStoreEntryKind {
    /** A protected private key with its chain. */
    PRIVATE_KEY,

    /** A trusted certificate. */
    TRUSTED_CERTIFICATE,
}
