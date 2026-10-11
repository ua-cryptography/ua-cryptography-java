package io.github.marketkernel.uacryptography.example.sign

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marketkernel.uacryptography.NamedBytes
import io.github.marketkernel.uacryptography.SignatureFormat
import io.github.marketkernel.uacryptography.SignatureHash
import io.github.marketkernel.uacryptography.SignatureLevel
import io.github.marketkernel.uacryptography.example.files.rememberFilePicker
import io.github.marketkernel.uacryptography.example.files.rememberFileSaver
import io.github.marketkernel.uacryptography.example.report.VerificationReportView
import io.github.marketkernel.uacryptography.example.widgets.BannerTone
import io.github.marketkernel.uacryptography.example.widgets.ChoiceField
import io.github.marketkernel.uacryptography.example.widgets.SectionCard
import io.github.marketkernel.uacryptography.example.widgets.StatusBanner
import io.github.marketkernel.uacryptography.example.widgets.formatDay
import io.github.marketkernel.uacryptography.example.widgets.formatSize

/** Sign: open a JKS key store, choose a document, sign; the result can be saved and is verified on the spot. */
@Composable
fun SignScreen(viewModel: SignViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saveFile = rememberFileSaver()
    Column {
        SectionCard(title = "Key", step = 1) {
            val key = state.key
            if (key == null) {
                KeyForm(state, viewModel)
            } else {
                KeyInfo(key, viewModel, onSaveTestKey = { saveFile(key.fileName, key.keyStore) })
            }
        }
        SectionCard(title = "Document", step = 2) {
            DocumentSection(state, viewModel, onSaveSignature = { signed -> saveFile(signed.name, signed.content) })
        }
    }
}

@Composable
private fun KeyForm(state: SignState, viewModel: SignViewModel) {
    // The password stays in memory only: not in the saved instance state.
    var password by remember { mutableStateOf("") }
    val pickKeyFile = rememberFilePicker(viewModel::chooseKeyFile)
    val openKey = { viewModel.openKey(password) }
    OutlinedButton(onClick = pickKeyFile) {
        Icon(Icons.Filled.Lock, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(state.keyFileName ?: "Key store (.jks)…")
    }
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { openKey() }),
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = openKey, enabled = state.keyFileName != null && !state.isOpening) { Text("Open key") }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = { viewModel.createTestKey() }, enabled = !state.isOpening) {
        Text("Or create a synthetic test key (self-signed, for trying the app)")
    }
    if (state.isOpening) {
        StatusBanner(BannerTone.BUSY, "Opening the key…")
    }
    state.keyFailure?.let { StatusBanner(BannerTone.FAILURE, "The key does not open", lines = listOf(it)) }
}

@Composable
private fun KeyInfo(key: OpenedKey, viewModel: SignViewModel, onSaveTestKey: () -> Unit) {
    val details = key.details
    StatusBanner(
        tone = BannerTone.OK,
        title = details.subjectName,
        lines = listOfNotNull(
            "issued by ${details.issuerName} · valid ${formatDay(details.validFrom)} – ${formatDay(details.validTo)}",
            "${key.fileName} · key \"${key.alias}\" · ${details.signatureAlgorithm.name}",
            if (key.isSynthetic) "A synthetic test key: its certificate is trusted only for the check after signing here." else null,
        ),
        action = {
            if (key.aliases.size > 1) {
                ChoiceField("Key", key.alias, key.aliases, { it }, viewModel::chooseAlias)
            }
            if (key.isSynthetic) {
                OutlinedButton(onClick = onSaveTestKey) {
                    Text("Save ${key.fileName} (password \"${SignViewModel.TEST_KEY_PASSWORD}\")")
                }
            }
            TextButton(onClick = viewModel::closeKey) { Text("Close the key") }
        },
    )
}

@Composable
private fun DocumentSection(
    state: SignState,
    viewModel: SignViewModel,
    onSaveSignature: (NamedBytes) -> Unit,
) {
    val pickDocument = rememberFilePicker(viewModel::chooseDocument)
    val document = state.document
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (document == null) "Choose the document to sign" else "${document.name} (${formatSize(document.content.size)})",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = pickDocument) {
            Icon(Icons.Filled.Create, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose a file…")
        }
    }
    Spacer(Modifier.height(16.dp))
    ChoiceField("Format", state.format, SignatureFormat.entries, ::formatName, viewModel::setFormat)
    ChoiceField("Level", state.level.takeIf { it in state.levels } ?: SignatureLevel.B, state.levels, ::levelName, viewModel::setLevel)
    ChoiceField("Hash", state.hash, SignatureHash.entries, ::hashName, viewModel::setHash)
    if (state.format == SignatureFormat.CADES) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.isDetached, onCheckedChange = viewModel::setDetached)
            Text("Detached (.p7s without the document inside)")
        }
    }
    val key = state.key
    Text(
        when {
            key == null -> "Open a key first."
            key.hasTimeStampService ->
                "Levels T and LT send only the hash of the signature to the time-stamping service the certificate " +
                    "names (${key.details.timeStampUrls.first()}); the document and the key never leave this device."
            else -> "The certificate names no time-stamping service: the signature is made at level B, entirely offline."
        },
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(12.dp))
    Button(onClick = viewModel::signDocument, enabled = state.canSign) { Text("Sign") }
    Spacer(Modifier.height(12.dp))
    if (state.isSigning) {
        StatusBanner(BannerTone.BUSY, "Signing…")
    }
    state.signFailure?.let { StatusBanner(BannerTone.FAILURE, "The document is not signed", lines = listOf(it)) }
    state.signed?.let { signed ->
        StatusBanner(
            tone = BannerTone.OK,
            title = "Signed: ${signed.name}",
            lines = listOf("${formatSize(signed.content.size)}. Below is the verification of the new signature."),
            action = { Button(onClick = { onSaveSignature(signed) }) { Text("Save ${signed.name}") } },
        )
        Spacer(Modifier.height(16.dp))
        state.report?.let { VerificationReportView(it, signed.name) }
    }
}

private fun formatName(format: SignatureFormat) = when (format) {
    SignatureFormat.CADES -> "CAdES (.sig / .p7s)"
    SignatureFormat.XADES -> "XAdES (.xml)"
    SignatureFormat.ASIC_S -> "ASiC-S (.asics)"
    SignatureFormat.ASIC_E -> "ASiC-E (.asice)"
}

private fun levelName(level: SignatureLevel) = when (level) {
    SignatureLevel.B -> "B — offline"
    SignatureLevel.T -> "T — with a time-stamp"
    SignatureLevel.LT -> "LT — with time-stamp, OCSP and CAs"
}

private fun hashName(hash: SignatureHash) = when (hash) {
    SignatureHash.DSTU7564 -> "DSTU 7564-256 (Kupyna)"
    SignatureHash.GOST34311 -> "GOST 34.311-95"
}
