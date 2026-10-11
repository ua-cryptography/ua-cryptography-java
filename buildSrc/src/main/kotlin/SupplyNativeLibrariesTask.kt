import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Supplies `libua_cryptography.so` of the pinned ua-cryptography-rust release for every Android ABI, as
 * `<outputDirectory>/<abi>/libua_cryptography.so`: downloaded from the GitHub release and checked against the pinned
 * SHA-256, or copied from [localLibraryDirectory] for offline builds and unreleased native code.
 */
abstract class SupplyNativeLibrariesTask : DefaultTask() {
    /** The pinned release, e.g. `0.1.6`. */
    @get:Input
    abstract val nativeLibraryVersion: Property<String>

    /** The SHA-256 of each bare release file by its target (`android-arm64`, …), as the release's SHA256SUMS lists it. */
    @get:Input
    abstract val sha256ByTarget: MapProperty<String, String>

    /** A directory of release files or of `<abi>/libua_cryptography.so`; absent: download. */
    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localLibraryDirectory: DirectoryProperty

    /** Receives `<abi>/libua_cryptography.so`. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun supply() {
        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        val localDirectory = localLibraryDirectory.orNull?.asFile
        for ((target, sha256) in sha256ByTarget.get()) {
            val abi = abiOf(target)
            val library = File(output, "$abi/$LIBRARY_FILE_NAME")
            library.parentFile.mkdirs()
            if (localDirectory != null) {
                findLocalLibrary(localDirectory, target, abi).copyTo(library)
            } else {
                downloadReleaseLibrary(target, sha256, library)
            }
        }
    }

    // A release file named as the release names it, else cargo-ndk's output layout (`<abi>/libua_cryptography.so`).
    private fun findLocalLibrary(directory: File, target: String, abi: String): File {
        val candidates = listOf(File(directory, releaseFileName(target)), File(directory, "$abi/$LIBRARY_FILE_NAME"))
        return candidates.firstOrNull(File::isFile)
            ?: throw GradleException(
                "No native library for $abi in $directory: expected one of ${candidates.joinToString { it.name }}.",
            )
    }

    // The digest is checked against the pinned one, never against the release's own SHA256SUMS.
    private fun downloadReleaseLibrary(target: String, expectedSha256: String, library: File) {
        val url = "$RELEASE_DOWNLOAD_URL/v${nativeLibraryVersion.get()}/${releaseFileName(target)}"
        val request = HttpRequest.newBuilder(URI(url)).timeout(DOWNLOAD_TIMEOUT).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() != HTTP_OK) {
            throw GradleException("Downloading $url failed: HTTP ${response.statusCode()}.")
        }
        val actualSha256 = calculateSha256(response.body())
        if (actualSha256 != expectedSha256) {
            throw GradleException("$url has the SHA-256 $actualSha256, not the pinned $expectedSha256: refused.")
        }
        library.writeBytes(response.body())
    }

    private fun releaseFileName(target: String): String = "ua-cryptography-${nativeLibraryVersion.get()}-$target.so"

    private companion object {
        const val LIBRARY_FILE_NAME = "libua_cryptography.so"
        const val RELEASE_DOWNLOAD_URL = "https://github.com/ua-cryptography/ua-cryptography-rust/releases/download"
        const val HTTP_OK = 200
        val DOWNLOAD_TIMEOUT: Duration = Duration.ofMinutes(5)

        /** The Android ABI of each release target. */
        val ABI_BY_TARGET = mapOf(
            "android-arm64" to "arm64-v8a",
            "android-arm" to "armeabi-v7a",
            "android-x64" to "x86_64",
            "android-ia32" to "x86",
        )

        // GitHub serves release files by a redirect to its storage.
        val httpClient: HttpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

        fun abiOf(target: String): String =
            ABI_BY_TARGET[target] ?: throw GradleException("Unknown release target $target: ${ABI_BY_TARGET.keys}.")

        fun calculateSha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
