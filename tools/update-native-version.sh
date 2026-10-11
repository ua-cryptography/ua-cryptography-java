#!/usr/bin/env bash
# Pins the library to a release of ua-cryptography-rust: rewrites ua-cryptography/native-release.properties with the
# version and the SHA-256 of the bare Android libraries from the release's SHA256SUMS, and replaces the vendored
# header with the one of that release's tag.
#
#   tools/update-native-version.sh 0.1.7
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: tools/update-native-version.sh <version>" >&2
  exit 1
fi
version="$1"
repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
module="$repository_root/ua-cryptography"
release_url="https://github.com/ua-cryptography/ua-cryptography-rust/releases/download/v$version"
header_url="https://raw.githubusercontent.com/ua-cryptography/ua-cryptography-rust/v$version/crates/ua-cryptography-ffi/include/ua_cryptography.h"

sums="$(curl --fail --silent --show-error --location "$release_url/SHA256SUMS")"
# The bare file of a target: ua-cryptography-<version>-android-<arch>.so.
digests="$(awk -v prefix="ua-cryptography-$version-" '
  index($2, prefix) == 1 && $2 ~ /-android-[a-z0-9]+\.so$/ {
    target = substr($2, length(prefix) + 1); sub(/\.so$/, "", target); print "sha256." target "=" $1
  }' <<<"$sums" | LC_ALL=C sort -t= -k1,1)"
if [[ -z "$digests" ]]; then
  echo "The release v$version has no bare Android libraries in its SHA256SUMS." >&2
  exit 1
fi

cat >"$module/native-release.properties" <<PROPERTIES
# The pinned release of ua-cryptography-rust. Written by tools/update-native-version.sh; do not edit.
# The library of each ABI is downloaded from that release and checked against the SHA-256 here, taken from the
# release's SHA256SUMS when the version was pinned; src/main/cpp/include/ua_cryptography.h is its header.
version=$version
$digests
PROPERTIES

curl --fail --silent --show-error --location "$header_url" --output "$module/src/main/cpp/include/ua_cryptography.h"

echo "Pinned $version:"
echo "$digests"
echo "Review the header's diff: a changed C ABI needs the JNI bindings changed too."
