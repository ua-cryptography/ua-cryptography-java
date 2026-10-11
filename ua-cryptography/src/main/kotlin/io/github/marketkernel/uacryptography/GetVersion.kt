@file:JvmName("UaCryptography")
@file:JvmMultifileClass

package io.github.marketkernel.uacryptography

/**
 * The version of the native ua_cryptography library, e.g. `0.1.6`.
 *
 * Throws an [IllegalStateException] when the loaded library is of another version than this package was built for.
 */
public fun getVersion(): String = NativeLibrary.checked().getVersion()
