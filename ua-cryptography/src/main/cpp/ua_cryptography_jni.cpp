// The native methods of io.github.marketkernel.uacryptography.NativeLibrary over the C ABI of ua_cryptography:
// each lends its inputs, calls one C function and hands back its output or throws the refusal.
#include <jni.h>

#include <cstdint>

#include "jni_marshalling.h"
#include "ua_cryptography.h"

using ua_jni::LentBytes;
using ua_jni::LentBytesList;
using ua_jni::LentFiles;
using ua_jni::LentText;
using ua_jni::ReceivedBuffer;
using ua_jni::run_guarded;
using ua_jni::throw_on_refusal;

namespace {

// The JVM class of byte[].
const char kByteArrayClass[] = "[B";

// The output of a call that ended with `status`, or nullptr with its refusal thrown.
jbyteArray finish_call(JNIEnv* env, jclass native_library, UaStatus status, const ReceivedBuffer& error_message,
                       const ReceivedBuffer& output) {
  if (throw_on_refusal(env, native_library, status, error_message)) {
    return nullptr;
  }
  return output.to_java(env);
}

// The arguments are Kotlin Ints checked as non-negative before the call.
uint32_t to_unsigned(jint value) { return static_cast<uint32_t>(value); }

}  // namespace

extern "C" {

JNIEXPORT jstring JNICALL Java_io_github_marketkernel_uacryptography_NativeLibrary_getVersion(JNIEnv* env, jclass) {
  return env->NewStringUTF(ua_get_version());
}

JNIEXPORT jbyteArray JNICALL Java_io_github_marketkernel_uacryptography_NativeLibrary_dstu7564CalculateHash(
    JNIEnv* env, jclass native_library, jbyteArray data, jint hash_bits) {
  return run_guarded<jbyteArray>(env, [&]() -> jbyteArray {
    const LentBytes lent_data(env, data);
    if (env->ExceptionCheck()) {
      return nullptr;
    }
    ReceivedBuffer hash;
    ReceivedBuffer error_message;
    const UaStatus status =
        ua_dstu7564_calculate_hash(lent_data.bytes(), to_unsigned(hash_bits), hash.out(), error_message.out());
    return finish_call(env, native_library, status, error_message, hash);
  });
}

// byte[2]: the key store, then its certificate.
JNIEXPORT jobjectArray JNICALL Java_io_github_marketkernel_uacryptography_NativeLibrary_createKeyStore(
    JNIEnv* env, jclass native_library, jbyteArray subject, jbyteArray alias, jint m, jint validity_days,
    jint signature_hash, jbyteArray password) {
  return run_guarded<jobjectArray>(env, [&]() -> jobjectArray {
    const LentText lent_subject(env, subject);
    const LentText lent_alias(env, alias);
    const LentText lent_password(env, password);
    if (env->ExceptionCheck()) {
      return nullptr;
    }
    const UaKeyStoreRequest request{lent_subject.text(), lent_alias.text(), to_unsigned(m),
                                    to_unsigned(validity_days), to_unsigned(signature_hash)};
    ReceivedBuffer key_store;
    ReceivedBuffer certificate;
    ReceivedBuffer error_message;
    const UaStatus status =
        ua_create_key_store(&request, lent_password.text(), key_store.out(), certificate.out(), error_message.out());
    if (throw_on_refusal(env, native_library, status, error_message)) {
      return nullptr;
    }
    jclass byte_array_class = env->FindClass(kByteArrayClass);
    if (byte_array_class == nullptr) {
      return nullptr;
    }
    jobjectArray created = env->NewObjectArray(2, byte_array_class, nullptr);
    jbyteArray key_store_bytes = key_store.to_java(env);
    jbyteArray certificate_bytes = certificate.to_java(env);
    if (created == nullptr || key_store_bytes == nullptr || certificate_bytes == nullptr) {
      return nullptr;
    }
    env->SetObjectArrayElement(created, 0, key_store_bytes);
    env->SetObjectArrayElement(created, 1, certificate_bytes);
    return created;
  });
}

JNIEXPORT jbyteArray JNICALL Java_io_github_marketkernel_uacryptography_NativeLibrary_getCertificate(
    JNIEnv* env, jclass native_library, jbyteArray key_store, jbyteArray password, jbyteArray alias) {
  return run_guarded<jbyteArray>(env, [&]() -> jbyteArray {
    const LentBytes lent_key_store(env, key_store);
    const LentText lent_password(env, password);
    const LentText lent_alias(env, alias);
    if (env->ExceptionCheck()) {
      return nullptr;
    }
    ReceivedBuffer certificate;
    ReceivedBuffer error_message;
    const UaStatus status = ua_get_certificate(lent_key_store.bytes(), lent_password.text(), lent_alias.text(),
                                               certificate.out(), error_message.out());
    return finish_call(env, native_library, status, error_message, certificate);
  });
}

JNIEXPORT jbyteArray JNICALL Java_io_github_marketkernel_uacryptography_NativeLibrary_getKeyStoreEntries(
    JNIEnv* env, jclass native_library, jbyteArray key_store, jbyteArray password) {
  return run_guarded<jbyteArray>(env, [&]() -> jbyteArray {
    const LentBytes lent_key_store(env, key_store);
    const LentText lent_password(env, password);
    if (env->ExceptionCheck()) {
      return nullptr;
    }
    ReceivedBuffer entries_json;
    ReceivedBuffer error_message;
    const UaStatus status = ua_get_key_store_entries(lent_key_store.bytes(), lent_password.text(),
                                                     entries_json.out(), error_message.out());
    return finish_call(env, native_library, status, error_message, entries_json);
  });
}

JNIEXPORT jbyteArray JNICALL Java_io_github_marketkernel_uacryptography_NativeLibrary_getCertificateDetails(
    JNIEnv* env, jclass native_library, jbyteArray certificate) {
  return run_guarded<jbyteArray>(env, [&]() -> jbyteArray {
    const LentBytes lent_certificate(env, certificate);
    if (env->ExceptionCheck()) {
      return nullptr;
    }
    ReceivedBuffer details_json;
    ReceivedBuffer error_message;
    const UaStatus status =
        ua_get_certificate_details(lent_certificate.bytes(), details_json.out(), error_message.out());
    return finish_call(env, native_library, status, error_message, details_json);
  });
}

JNIEXPORT jbyteArray JNICALL Java_io_github_marketkernel_uacryptography_NativeLibrary_sign(
    JNIEnv* env, jclass native_library, jbyteArray key_store, jbyteArray password, jbyteArray alias,
    jobjectArray file_names, jobjectArray file_contents, jobjectArray certificates, jint format, jint level, jint hash,
    jboolean is_detached, jboolean has_content_time_stamp, jbyteArray tsa_url, jbyteArray ocsp_url) {
  return run_guarded<jbyteArray>(env, [&]() -> jbyteArray {
    const LentBytes lent_key_store(env, key_store);
    const LentText lent_password(env, password);
    const LentText lent_alias(env, alias);
    const LentFiles lent_files(env, file_names, file_contents);
    const LentBytesList lent_certificates(env, certificates);
    const LentText lent_tsa_url(env, tsa_url);
    const LentText lent_ocsp_url(env, ocsp_url);
    if (env->ExceptionCheck()) {
      return nullptr;
    }
    const UaSigningOptions options{to_unsigned(format),     to_unsigned(level),  to_unsigned(hash),
                                   is_detached == JNI_TRUE, has_content_time_stamp == JNI_TRUE,
                                   lent_tsa_url.text(),     lent_ocsp_url.text()};
    ReceivedBuffer signature;
    ReceivedBuffer error_message;
    const UaStatus status = ua_sign(lent_key_store.bytes(), lent_password.text(), lent_alias.text(),
                                    lent_files.data(), lent_files.count(), lent_certificates.data(),
                                    lent_certificates.count(), &options, signature.out(), error_message.out());
    return finish_call(env, native_library, status, error_message, signature);
  });
}

JNIEXPORT jbyteArray JNICALL Java_io_github_marketkernel_uacryptography_NativeLibrary_verify(
    JNIEnv* env, jclass native_library, jbyteArray signature, jobjectArray data_names, jobjectArray data_contents,
    jobjectArray trust_anchors, jobjectArray certificates, jboolean is_online) {
  return run_guarded<jbyteArray>(env, [&]() -> jbyteArray {
    const LentBytes lent_signature(env, signature);
    const LentFiles lent_data(env, data_names, data_contents);
    const LentBytesList lent_trust_anchors(env, trust_anchors);
    const LentBytesList lent_certificates(env, certificates);
    if (env->ExceptionCheck()) {
      return nullptr;
    }
    ReceivedBuffer report_json;
    ReceivedBuffer error_message;
    const UaStatus status =
        ua_verify(lent_signature.bytes(), lent_data.data(), lent_data.count(), lent_trust_anchors.data(),
                  lent_trust_anchors.count(), lent_certificates.data(), lent_certificates.count(),
                  is_online == JNI_TRUE, report_json.out(), error_message.out());
    return finish_call(env, native_library, status, error_message, report_json);
  });
}

}  // extern "C"
