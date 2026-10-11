# ua-cryptography-android example

Verifies and signs Ukrainian qualified electronic signatures with `io.github.marketkernel:ua-cryptography-android` from
Maven Central, depended on as any app does (`build.gradle.kts`, the version in `gradle/libs.versions.toml`):

```kotlin
dependencies {
    implementation("io.github.marketkernel:ua-cryptography-android:0.1.0")
}
```

The three tabs follow the example of the Dart package `ua_cryptography`:

- **Verify** — choose a signed file (`.sig` / `.p7s` CAdES, `.asice` / `.asics` ASiC, `.xml` XAdES), and for a
  detached signature the signed files with it. The report shows the signer, the signing and time-stamp times, every
  check and the signed digests. The online check (off by default) asks the OCSP servers the certificates name.
- **Sign** — open a `.jks` key store with its password (or create a synthetic test key), choose a document, a format
  (CAdES, XAdES, ASiC-S, ASiC-E), a level (B offline; T and LT when the certificate names a time-stamping service) and a
  hash. The signature is verified on the spot and can be saved.
- **Certificates** — the trusted roots (ЦЗО, НБУ) and intermediates («Дія», PrivatBank and its bundle) built in from
  `src/main/assets/certificates`, all public; more can be added (DER, PEM or `.p7b`) and are kept on the device.

```sh
./gradlew :example:installDebug                 # or installRelease: minified by R8, signed with the debug key
./gradlew :example:connectedDebugAndroidTest    # the trust store and the screens on a device
./gradlew :example:installDebug -PuaCryptography.exampleFromSource=true  # with the unreleased library of this repository
```

The manifest asks for `android.permission.INTERNET`: levels T and LT and online verification reach network services.

Jetpack Compose and Material 3; Android 8.0 (API 26) and later. Files are chosen and saved through the system's
document picker; nothing leaves the device except, at levels T and LT and online verification, the requests to the
time-stamping and OCSP services.
