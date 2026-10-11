package io.github.marketkernel.uacryptography

import java.time.Instant

/** One entry of a key store. */
public data class KeyStoreEntry(
    /** The alias. */
    val alias: String,
    /** A private key or a trusted certificate. */
    val kind: KeyStoreEntryKind,
    /** When the entry was created. */
    val created: Instant,
    /** The chain of a private key as stored, or the trusted certificate. */
    val certificates: List<CertificateDetails>,
)
