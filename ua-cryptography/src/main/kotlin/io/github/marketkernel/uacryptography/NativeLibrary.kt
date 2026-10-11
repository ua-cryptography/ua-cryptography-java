package io.github.marketkernel.uacryptography

/**
 * The JNI bindings of `libua_cryptography_jni.so` over the C ABI of `libua_cryptography.so`
 * (`src/main/cpp/include/ua_cryptography.h`). Strings travel as UTF-8 bytes; the JNI library throws the refusal of a
 * status other than OK as the exception [refusalOf] makes.
 */
internal object NativeLibrary {
    init {
        // The JNI library needs the release library; loaded first, it is found whatever the loader's search path.
        System.loadLibrary("ua_cryptography")
        System.loadLibrary("ua_cryptography_jni")
    }

    // A failed check leaves it false, so a later call checks again.
    @Volatile
    private var isVersionChecked = false

    /**
     * These bindings, once the loaded library is known to be the release this package pins.
     *
     * Throws an [IllegalStateException] naming both versions otherwise.
     */
    fun checked(): NativeLibrary {
        if (!isVersionChecked) {
            checkVersion()
            isVersionChecked = true
        }
        return this
    }

    @JvmStatic
    external fun getVersion(): String

    @JvmStatic
    external fun dstu7564CalculateHash(data: ByteArray, hashBits: Int): ByteArray

    /** The key store, then its certificate. */
    @JvmStatic
    external fun createKeyStore(
        subject: ByteArray,
        alias: ByteArray,
        m: Int,
        validityDays: Int,
        signatureHash: Int,
        password: ByteArray,
    ): Array<ByteArray>

    @JvmStatic
    external fun getCertificate(keyStore: ByteArray, password: ByteArray, alias: ByteArray?): ByteArray

    /** The JSON array of the entries. */
    @JvmStatic
    external fun getKeyStoreEntries(keyStore: ByteArray, password: ByteArray): ByteArray

    /** The JSON object of the details. */
    @JvmStatic
    external fun getCertificateDetails(certificate: ByteArray): ByteArray

    /** [fileNames] and [fileContents] are the files index by index. */
    @JvmStatic
    external fun sign(
        keyStore: ByteArray,
        password: ByteArray,
        alias: ByteArray?,
        fileNames: Array<ByteArray>,
        fileContents: Array<ByteArray>,
        certificates: Array<ByteArray>,
        format: Int,
        level: Int,
        hash: Int,
        isDetached: Boolean,
        hasContentTimeStamp: Boolean,
        tsaUrl: ByteArray?,
        ocspUrl: ByteArray?,
    ): ByteArray

    /** The JSON report; [dataNames] and [dataContents] are the files index by index. */
    @JvmStatic
    external fun verify(
        signature: ByteArray,
        dataNames: Array<ByteArray>,
        dataContents: Array<ByteArray>,
        trustAnchors: Array<ByteArray>,
        certificates: Array<ByteArray>,
        isOnline: Boolean,
    ): ByteArray

    /** The exception of a [status] other than OK, with the library's UTF-8 [message]; the JNI library throws it. */
    @JvmStatic
    fun refusalOf(status: Int, message: ByteArray): Throwable {
        val text = String(message, Charsets.UTF_8)
        return when (status) {
            NativeStatus.INVALID_ARGUMENT -> InvalidArgumentException(text)
            NativeStatus.UNREADABLE_INPUT -> UnreadableInputException(text)
            NativeStatus.WRONG_PASSWORD -> WrongPasswordException(text)
            NativeStatus.AUTHENTICATION_FAILED -> AuthenticationFailedException(text)
            NativeStatus.SERVICE_FAILURE -> ServiceFailureException(text)
            NativeStatus.INTERNAL -> InternalException(text)
            else -> IllegalStateException("ua_cryptography returned the unknown status $status: $text")
        }
    }

    private fun checkVersion() {
        val loadedVersion = getVersion()
        check(loadedVersion == BuildConfig.NATIVE_LIBRARY_VERSION) {
            "ua-cryptography-android needs the native library ${BuildConfig.NATIVE_LIBRARY_VERSION}, but " +
                "$loadedVersion was loaded: the library of uaCryptography.localLibraryDir is of another release."
        }
    }
}
