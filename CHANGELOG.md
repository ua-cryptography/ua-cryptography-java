## 0.1.0

The first release, over the native library of [ua-cryptography-rust](https://github.com/ua-cryptography/ua-cryptography-rust)
0.1.6.

- Signing as CAdES, XAdES, ASiC-S and ASiC-E at levels B, T and LT (`sign`), and verification of any of them with a
  report of every check (`verify`).
- JKS key stores: a new one with a DSTU 4145 key and its self-signed certificate (`createKeyStore`), its entries
  (`getKeyStoreEntries`) and the certificate of its key (`getCertificate`); what a certificate says
  (`getCertificateDetails`).
- DSTU 7564 (Kupyna) of 256, 384 and 512 bits (`dstu7564CalculateHash`).
- The native library of every ABI (arm64-v8a, armeabi-v7a, x86_64, x86) inside the AAR, downloaded at build time from
  the GitHub release and checked against the pinned SHA-256; Android 8.0 (API 26) and later.
