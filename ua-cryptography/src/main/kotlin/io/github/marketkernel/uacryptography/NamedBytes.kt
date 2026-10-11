package io.github.marketkernel.uacryptography

/** A file: its name, as a XAdES reference or an ASiC entry names it, and its content. */
public class NamedBytes(
    /** The file name, e.g. `contract.pdf`. */
    public val name: String,
    /** The content. */
    public val content: ByteArray,
)
