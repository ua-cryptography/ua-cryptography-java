package io.github.marketkernel.uacryptography

/** The format of a signature. */
public enum class SignatureFormat {
    /** CMS SignedData (`.p7s`, PrivatBank's `.sig`), enveloped or detached; signs one file. */
    CADES,

    /** A detached XML signature with one reference per file (`.xml`). */
    XADES,

    /** A ZIP container with the file and a detached CAdES (`.asics`); signs one file. */
    ASIC_S,

    /** A ZIP container with the files and a XAdES signature (`.asice`), as «Дія.Підпис» makes it. */
    ASIC_E,
}
