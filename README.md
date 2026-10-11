# ua-cryptography-android

Ukrainian national cryptography for Android: DSTU 7564 (Kupyna), JKS key stores with DSTU 4145 keys, and signing and
verifying the electronic signatures of the Ukrainian PKI — CAdES (`.p7s`, PrivatBank's `.sig`), XAdES (`.xml`),
ASiC-S (`.asics`) and ASiC-E (`.asice`, «Дія.Підпис») at levels B, T and LT.

The library binds the `ua_cryptography` native library of
[ua-cryptography-rust](https://github.com/ua-cryptography/ua-cryptography-rust), a port of the
[C# reference implementation](https://github.com/ua-cryptography/ua-cryptography-csharp), through a thin JNI layer.
The AAR carries both native libraries for every ABI: nothing to download or build in the app. Its API mirrors the
Dart package [`ua_cryptography`](https://pub.dev/packages/ua_cryptography).

## Requirements

| | |
|---|---|
| Android | API 26 (Android 8.0) or later |
| ABIs | arm64-v8a, armeabi-v7a, x86_64, x86; 16 KB pages |
| Kotlin | apps on Kotlin 2.0 or later; the library depends on `kotlin-stdlib` 2.1 and `kotlinx-coroutines-core` 1.10 |

The AAR is about 7 MB; an app bundle (AAB) delivers each device the libraries of its ABI only (~3.7 MB on arm64).

## Installation

```kotlin
dependencies {
    implementation("io.github.marketkernel:ua-cryptography-android:0.1.0")
}
```

from Maven Central (`mavenCentral()` in the repositories). R8 / ProGuard rules ship with the AAR.

## Usage

```kotlin
import io.github.marketkernel.uacryptography.*
```

### Verify a signature

```kotlin
val report = verify(
    signatureBytes,                              // .sig / .p7s, .xml, .asics, .asice
    data = listOf(NamedBytes("contract.pdf", pdf)), // only for a detached signature
    trustAnchors = listOf(czoRoot, nbuRoot),     // trusted roots: DER, PEM or a .p7b bundle each
    certificates = listOf(privatBankBundle),     // CA, time-stamping and OCSP certificates the signature lacks
    isOnline = true,                             // ask the OCSP responders the certificates name
)

println("${report.format}: ${if (report.isValid) "valid" else "NOT valid"}, ${report.warningCount} warnings")
for (signature in report.signatures.filterNot { it.isContainer }) {
    println("${signature.signer?.subjectName} at ${signature.timestampTime ?: signature.signingTime}")
    for (check in signature.checks.filter { it.status != CheckOutcome.PASSED }) {
        println("  ${check.status}: ${check.name}: ${check.details}")
    }
}
```

An invalid signature is a report with `isValid == false`, not an exception. A check that could not be established —
typically the revocation status of a certificate when the signature carries none and `isOnline` is off — is a
warning: the signature stays valid.

### Sign a document

```kotlin
val signature = sign(
    keyStoreBytes,                              // a JKS key store
    password,
    listOf(NamedBytes("contract.pdf", pdf)),    // CAdES and ASiC-S sign one file
    options = SigningOptions(
        format = SignatureFormat.ASIC_E,        // CADES (default), XADES, ASIC_S, ASIC_E
        level = SignatureLevel.T,               // B (default, offline), T, LT
        hash = SignatureHash.DSTU7564,          // or GOST34311
    ),
)
```

Level T asks the time-stamping service the signer's certificate names (or `SigningOptions.tsaUrl`) for a time-stamp
over the hash of the signature; level LT also embeds the CA certificates and OCSP responses. The document and the key
never leave the device. CAdES is enveloped unless `isDetached = true`. Levels T and LT and online verification need
the `android.permission.INTERNET` permission in the app's manifest.

### Key stores and certificates

```kotlin
// A JKS key store with a new DSTU 4145 key (curve m = 257 of table Г.1) and its self-signed certificate.
val created = createKeyStore(KeyStoreRequest(subject = "CN=Test signer,O=Test,C=UA"), password)

for (entry in getKeyStoreEntries(created.keyStore, password)) {
    println("${entry.alias}: ${entry.kind}, ${entry.certificates.first().subjectName}")
}

val certificate = getCertificate(created.keyStore, password) // DER of the signing key's certificate
val details = getCertificateDetails(certificate)              // DER, PEM or base64
println("${details.subjectName}, issued by ${details.issuerName}, valid to ${details.validTo}")
println("time-stamping: ${details.timeStampUrls}, OCSP: ${details.ocspUrls}")
```

### Hashes

```kotlin
val hash = dstu7564CalculateHash(data, 256) // Kupyna-256; also 384 and 512
```

### Errors

Every refusal of the native library is a `UaCryptographyException`, sealed, one subclass per status, with the
library's message:

| Exception | When |
|---|---|
| `InvalidArgumentException` | a wrong length, an unknown curve degree or hash size, options that do not fit the format |
| `UnreadableInputException` | a key store, certificate or signature that does not parse |
| `WrongPasswordException` | the key store or its key does not open with the password |
| `AuthenticationFailedException` | an authenticated ciphertext or a wrapped key is not the one tagged |
| `ServiceFailureException` | a time-stamping, OCSP or certificate download service failed |
| `InternalException` | a defect of the native library |

### Threads

`sign`, `verify` and `createKeyStore` are `suspend` functions that run on `Dispatchers.IO`: levels T and LT and online
verification reach network services over blocking sockets. The other functions are synchronous and fast. The native
library is thread-safe.

From Java, the functions are static methods of `UaCryptography`; the suspending ones take a `Continuation`, so a Java
app calls them through a Kotlin wrapper of its own.

## Example

[`example/`](example) is an app with the three tabs of the Dart package's example: **Verify** a signed file, **Sign**
a document with a `.jks` key (or a synthetic test key), and the trusted **Certificates** (the public roots of the ЦЗО
and the НБУ, the «Дія» and PrivatBank CAs, built in; more can be added). It depends on the published library from
Maven Central, as an app does:

```sh
./gradlew :example:installDebug                 # on a connected device or emulator
./gradlew :example:connectedDebugAndroidTest    # its tests there
./gradlew :example:installDebug -PuaCryptography.exampleFromSource=true  # with the library of this repository
```

## How it is built

- `ua-cryptography/native-release.properties` pins a release of ua-cryptography-rust and the SHA-256 of its bare
  Android libraries (`ua-cryptography-<v>-android-<arch>.so`); `src/main/cpp/include/ua_cryptography.h` is that
  release's header.
- The `supplyNativeLibraries` task downloads the four libraries from the GitHub release and refuses a file with
  another digest; they go into the AAR as they are.
- `src/main/cpp` is the JNI layer (`libua_cryptography_jni.so`, C++17): it copies the inputs out of the Java arrays
  (zeroed afterwards), calls the C ABI and throws the refusal of a status as the Kotlin exception of that status.
- At the first call the library compares `ua_get_version()` with the pinned version and refuses another with an
  `IllegalStateException`.

### Offline builds and unreleased native code

Point the Gradle property `uaCryptography.localLibraryDir` at a directory holding the libraries, relative to this
repository: release files named as in the release, or cargo-ndk's output layout (`<abi>/libua_cryptography.so`):

```sh
# in ../ua-cryptography-rust
cargo ndk --platform 21 -o target/android-jni-libs -t arm64-v8a -t armeabi-v7a -t x86 -t x86_64 \
  build --release -p ua-cryptography-ffi
# here
./gradlew :ua-cryptography:assembleRelease -PuaCryptography.localLibraryDir=../ua-cryptography-rust/target/android-jni-libs
```

The libraries must be of the pinned release: the first call refuses another version.

## Development

JDK 17 or later and the Android SDK with NDK 28.2 and CMake 3.22.1 (Gradle installs the missing ones when the SDK
licenses are accepted).

```sh
./gradlew :ua-cryptography:build                     # the AAR, lint, the JVM tests
./gradlew :ua-cryptography:connectedDebugAndroidTest # the JNI and native tests on a running emulator or device
./gradlew :ua-cryptography:publishToMavenLocal       # the publication, unsigned, in ~/.m2
tools/update-native-version.sh 0.1.7                 # pin another release: version, SHA-256 table, header
```

Tests use synthetic keys generated at run time: no real signature, key or certificate of a person is part of this
repository.

## Releasing

1. Set `VERSION_NAME` in `gradle.properties` and add the version to `CHANGELOG.md`.
2. Push the tag `v<VERSION_NAME>`. `.github/workflows/publish.yml` runs the tests, then waits for the approval of the
   `maven-central` environment.
3. Once approved, the deployment is uploaded to the [Central Portal](https://central.sonatype.com/publishing/deployments),
   signed and validated; **Publish** there releases it to Maven Central.
4. Once it is on Maven Central, set `ua-cryptography-android` in `gradle/libs.versions.toml` to it: the version the
   example app uses.

The workflow reads the secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` (a Central Portal user token),
`SIGNING_KEY` (ASCII-armored private key) and `SIGNING_KEY_PASSWORD`.

## Not yet

The native library also has GOST 28147-89, GOST 34.311-95, DSTU 7624 (Kalyna: block, ECB, CBC, CFB, OFB, CTR, CMAC,
CCM, GCM, GMAC, XTS, KW), DSTU 4145 primitives (key pairs, signing a hash, encoding) and hashes fed in parts; their
Kotlin API is to come, as in the Dart package.

## License

[MIT](LICENSE).
