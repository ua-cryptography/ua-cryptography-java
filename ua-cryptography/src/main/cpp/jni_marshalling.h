// Marshalling between Java arrays and the C ABI of ua_cryptography: inputs lent for one call, outputs received
// from it, and the refusal of a status other than UA_STATUS_OK thrown as the Kotlin exception of that status.
#pragma once

#include <jni.h>

#include <cstddef>
#include <cstdint>
#include <exception>
#include <new>
#include <vector>

#include "ua_cryptography.h"

namespace ua_jni {

// Bytes zeroed when released: they may be a key store or a password.
class ZeroedBytes {
 public:
  ZeroedBytes() = default;
  ZeroedBytes(ZeroedBytes&&) noexcept = default;
  ZeroedBytes& operator=(ZeroedBytes&&) noexcept = default;
  ZeroedBytes(const ZeroedBytes&) = delete;
  ZeroedBytes& operator=(const ZeroedBytes&) = delete;
  ~ZeroedBytes();

  std::vector<uint8_t> content;
};

// The bytes of a Java byte[] copied for one call; a null array is an absent optional input.
class LentBytes {
 public:
  LentBytes(JNIEnv* env, jbyteArray array);

  // The bytes; never a NULL data, which the C ABI reads as an absent optional input.
  UaBytes bytes() const;
  // The bytes, or a NULL data when absent.
  UaBytes optional_bytes() const;

 private:
  ZeroedBytes copy_;
  bool is_present_;
};

// A Java byte[] of UTF-8 as a NUL-terminated string for one call; a null array is an absent optional string.
// The Kotlin side refuses a NUL inside before it gets here.
class LentText {
 public:
  LentText(JNIEnv* env, jbyteArray utf8);

  // The string, or NULL when absent.
  const char* text() const;

 private:
  ZeroedBytes copy_;
  bool is_present_;
};

// A Java byte[][] as a C array of UaBytes; NULL when empty, as the C ABI takes "NULL with 0".
class LentBytesList {
 public:
  LentBytesList(JNIEnv* env, jobjectArray arrays);

  const UaBytes* data() const;
  size_t count() const;

 private:
  std::vector<LentBytes> items_;
  std::vector<UaBytes> lent_;
};

// Files as a C array of UaNamedBytes, the names (UTF-8) and the contents index by index; NULL when empty.
class LentFiles {
 public:
  LentFiles(JNIEnv* env, jobjectArray names, jobjectArray contents);

  const UaNamedBytes* data() const;
  size_t count() const;

 private:
  std::vector<LentText> names_;
  std::vector<LentBytes> contents_;
  std::vector<UaNamedBytes> lent_;
};

// A buffer the library writes, released with ua_free_buffer exactly once.
class ReceivedBuffer {
 public:
  ReceivedBuffer() = default;
  ReceivedBuffer(const ReceivedBuffer&) = delete;
  ReceivedBuffer& operator=(const ReceivedBuffer&) = delete;
  ~ReceivedBuffer();

  // Where the library writes the buffer.
  UaBuffer* out();
  // A Java copy of the bytes; nullptr with an OutOfMemoryError pending when it cannot be made.
  jbyteArray to_java(JNIEnv* env) const;

 private:
  UaBuffer buffer_{nullptr, 0};
};

// Throws the exception NativeLibrary.refusalOf makes of a status other than UA_STATUS_OK, carrying the library's
// message; returns whether the call was refused.
bool throw_on_refusal(JNIEnv* env, jclass native_library, UaStatus status, const ReceivedBuffer& error_message);

// Throws a Java exception of `class_name` with `message`.
void throw_java(JNIEnv* env, const char* class_name, const char* message);

// Runs a JNI call's body; a C++ exception becomes a Java one instead of unwinding into the JVM.
template <typename Result, typename Body>
Result run_guarded(JNIEnv* env, Body body) {
  try {
    return body();
  } catch (const std::bad_alloc&) {
    throw_java(env, "java/lang/OutOfMemoryError", "ua_cryptography_jni: out of native memory");
  } catch (const std::exception& error) {
    throw_java(env, "java/lang/IllegalStateException", error.what());
  }
  return Result{};
}

}  // namespace ua_jni
