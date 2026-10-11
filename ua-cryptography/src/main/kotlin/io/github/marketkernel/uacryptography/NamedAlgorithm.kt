package io.github.marketkernel.uacryptography

/** An algorithm as a signature or a certificate names it, with its readable name. */
public data class NamedAlgorithm(
    /** An OID in CMS and X.509, a URI in XML-DSig. */
    val identifier: String,
    /** E.g. `DSTU 7564-256`; the identifier itself when it has no name. */
    val name: String,
) {
    override fun toString(): String = name
}
