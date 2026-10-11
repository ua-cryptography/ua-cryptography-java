package io.github.marketkernel.uacryptography.example.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.marketkernel.uacryptography.NamedBytes
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The file at [uri], read whole, under the name the document provider gives it.
 *
 * Throws an [IllegalStateException] saying why when the provider does not give the file.
 */
suspend fun readNamedBytes(context: Context, uri: Uri): NamedBytes = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: UNNAMED
        val content = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("The file $name cannot be opened.")
        NamedBytes(name, content)
    } catch (failure: IOException) {
        throw IllegalStateException("The file cannot be read: ${failure.message}", failure)
    } catch (failure: SecurityException) {
        throw IllegalStateException("The file may not be read: ${failure.message}", failure)
    }
}

/** Writes [bytes] to the document at [uri], which the user created for them. */
suspend fun writeBytes(context: Context, uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        ?: throw IllegalStateException("The file cannot be written.")
}

private const val UNNAMED = "file"
