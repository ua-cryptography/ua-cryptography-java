package io.github.marketkernel.uacryptography.example.files

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/** Any type: the document providers do not know the MIME types of .sig, .asice or .jks. */
private val ANY_TYPE = arrayOf("*/*")

/** A launcher of the system picker for several files; [onPicked] gets their URIs, empty when cancelled. */
@Composable
fun rememberFilesPicker(onPicked: (List<Uri>) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments(), onPicked)
    return { launcher.launch(ANY_TYPE) }
}

/** A launcher of the system picker for one file; [onPicked] gets its URI, not called when cancelled. */
@Composable
fun rememberFilePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onPicked(uri)
        }
    }
    return { launcher.launch(ANY_TYPE) }
}

/** Offers bytes for saving under a file name through the system "save as" dialog, and tells how it ended. */
@Composable
fun rememberFileSaver(): (fileName: String, bytes: ByteArray) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BINARY_TYPE)) { uri ->
        val bytes = pendingBytes
        pendingBytes = null
        if (uri != null && bytes != null) {
            scope.launch {
                val outcome = runCatching { writeBytes(context, uri, bytes) }
                val message = outcome.fold({ "Saved." }, { "Not saved: ${it.message}" })
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    return { fileName, bytes ->
        pendingBytes = bytes
        launcher.launch(fileName)
    }
}

private const val BINARY_TYPE = "application/octet-stream"
