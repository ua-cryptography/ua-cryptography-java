package io.github.marketkernel.uacryptography

import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

// The JSON reports of the native library (crates/ua-cryptography-operations/src/models/reports of
// ua-cryptography-rust) read into the models. A report of another shape throws a JSONException naming the field.

/** The report `ua_verify` writes. */
internal fun readVerificationSummary(json: ByteArray): VerificationSummary =
    readVerificationSummary(JSONObject(decodeUtf8(json)))

/** The array `ua_get_key_store_entries` writes. */
internal fun readKeyStoreEntries(json: ByteArray): List<KeyStoreEntry> =
    JSONArray(decodeUtf8(json)).objects(::readKeyStoreEntry)

/** The object `ua_get_certificate_details` writes. */
internal fun readCertificateDetails(json: ByteArray): CertificateDetails =
    readCertificateDetails(JSONObject(decodeUtf8(json)))

private fun readVerificationSummary(json: JSONObject) = VerificationSummary(
    format = json.getString("format"),
    isValid = json.getBoolean("is_valid"),
    signatureCount = json.getInt("signature_count"),
    warningCount = json.getInt("warning_count"),
    failureCount = json.getInt("failure_count"),
    verifiedAt = json.time("verified_at"),
    signedFiles = json.strings("signed_files"),
    signatures = json.getJSONArray("signatures").objects(::readSignatureSummary),
)

private fun readSignatureSummary(json: JSONObject) = SignatureSummary(
    title = json.getString("title"),
    level = json.optionalString("level"),
    packaging = json.optionalString("packaging"),
    signatureAlgorithm = json.optionalObject("signature_algorithm")?.let(::readNamedAlgorithm),
    digestAlgorithm = json.optionalObject("digest_algorithm")?.let(::readNamedAlgorithm),
    signingTime = json.optionalTime("signing_time"),
    timestampTime = json.optionalTime("timestamp_time"),
    validationTime = json.optionalTime("validation_time"),
    isValid = json.getBoolean("is_valid"),
    hasWarnings = json.getBoolean("has_warnings"),
    signer = json.optionalObject("signer")?.let(::readCertificateDetails),
    references = json.getJSONArray("references").objects(::readSignedReference),
    checks = json.getJSONArray("checks").objects(::readVerificationCheck),
)

private fun readCertificateDetails(json: JSONObject) = CertificateDetails(
    subject = json.getString("subject"),
    issuer = json.getString("issuer"),
    serial = json.getString("serial"),
    validFrom = json.time("valid_from"),
    validTo = json.time("valid_to"),
    signatureAlgorithm = readNamedAlgorithm(json.getJSONObject("signature_algorithm")),
    isCa = json.getBoolean("is_ca"),
    ocspUrls = json.strings("ocsp_urls"),
    caIssuerUrls = json.strings("ca_issuer_urls"),
    timeStampUrls = json.strings("time_stamp_urls"),
)

private fun readKeyStoreEntry(json: JSONObject) = KeyStoreEntry(
    alias = json.getString("alias"),
    kind = readKeyStoreEntryKind(json.getString("kind")),
    created = json.time("created"),
    certificates = json.getJSONArray("certificates").objects(::readCertificateDetails),
)

private fun readNamedAlgorithm(json: JSONObject) = NamedAlgorithm(
    identifier = json.getString("identifier"),
    name = json.getString("name"),
)

private fun readSignedReference(json: JSONObject) = SignedReference(
    name = json.getString("name"),
    digestAlgorithm = readNamedAlgorithm(json.getJSONObject("digest_algorithm")),
    digest = json.getString("digest"),
)

private fun readVerificationCheck(json: JSONObject) = VerificationCheck(
    name = json.getString("name"),
    status = readCheckOutcome(json.getString("status")),
    details = json.getString("details"),
)

private fun readKeyStoreEntryKind(kind: String): KeyStoreEntryKind = when (kind) {
    "private_key" -> KeyStoreEntryKind.PRIVATE_KEY
    "trusted_certificate" -> KeyStoreEntryKind.TRUSTED_CERTIFICATE
    else -> throw IllegalStateException("Unknown key store entry kind \"$kind\"")
}

private fun readCheckOutcome(status: String): CheckOutcome = when (status) {
    "passed" -> CheckOutcome.PASSED
    "warning" -> CheckOutcome.WARNING
    "failed" -> CheckOutcome.FAILED
    else -> throw IllegalStateException("Unknown check status \"$status\"")
}

private fun decodeUtf8(json: ByteArray): String = String(json, Charsets.UTF_8)

// The reports write times as yyyy-MM-ddTHH:mm:ssZ.
private fun JSONObject.time(key: String): Instant = Instant.parse(getString(key))

private fun JSONObject.optionalString(key: String): String? = if (isNull(key)) null else getString(key)

private fun JSONObject.optionalTime(key: String): Instant? = optionalString(key)?.let(Instant::parse)

private fun JSONObject.optionalObject(key: String): JSONObject? = if (isNull(key)) null else getJSONObject(key)

private fun JSONObject.strings(key: String): List<String> {
    val array = getJSONArray(key)
    return List(array.length()) { index -> array.getString(index) }
}

private fun <T> JSONArray.objects(read: (JSONObject) -> T): List<T> = List(length()) { index -> read(getJSONObject(index)) }
