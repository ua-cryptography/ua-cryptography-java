#include "jni_marshalling.h"

#include <algorithm>

namespace ua_jni {
namespace {

// What a required input points to when empty: the C ABI reads a NULL data as an absent optional input.
const uint8_t kNoBytes = 0;

// The terminator of a C string.
const size_t kNulBytes = 1;

// NativeLibrary.refusalOf(int status, byte[] message): Throwable.
const char kRefusalOfName[] = "refusalOf";
const char kRefusalOfSignature[] = "(I[B)Ljava/lang/Throwable;";

// The bytes of `array` followed by `extra_zeros` zero bytes.
std::vector<uint8_t> copy_byte_array(JNIEnv* env, jbyteArray array, size_t extra_zeros) {
  const jsize length = env->GetArrayLength(array);
  std::vector<uint8_t> bytes(static_cast<size_t>(length) + extra_zeros, 0);
  env->GetByteArrayRegion(array, 0, length, reinterpret_cast<jbyte*>(bytes.data()));
  return bytes;
}

// The element `index` of a byte[][]; a local reference the caller deletes.
jbyteArray get_byte_array(JNIEnv* env, jobjectArray arrays, jsize index) {
  return static_cast<jbyteArray>(env->GetObjectArrayElement(arrays, index));
}

}  // namespace

ZeroedBytes::~ZeroedBytes() {
  // Volatile, so the stores are not dropped as dead before the release.
  volatile uint8_t* bytes = content.data();
  for (size_t index = 0; index < content.size(); ++index) {
    bytes[index] = 0;
  }
}

LentBytes::LentBytes(JNIEnv* env, jbyteArray array) : is_present_(array != nullptr) {
  if (is_present_) {
    copy_.content = copy_byte_array(env, array, 0);
  }
}

UaBytes LentBytes::bytes() const {
  if (copy_.content.empty()) {
    return UaBytes{&kNoBytes, 0};
  }
  return UaBytes{copy_.content.data(), copy_.content.size()};
}

UaBytes LentBytes::optional_bytes() const {
  if (!is_present_) {
    return UaBytes{nullptr, 0};
  }
  return bytes();
}

LentText::LentText(JNIEnv* env, jbyteArray utf8) : is_present_(utf8 != nullptr) {
  if (is_present_) {
    copy_.content = copy_byte_array(env, utf8, kNulBytes);
  }
}

const char* LentText::text() const {
  if (!is_present_) {
    return nullptr;
  }
  return reinterpret_cast<const char*>(copy_.content.data());
}

LentBytesList::LentBytesList(JNIEnv* env, jobjectArray arrays) {
  const jsize count = env->GetArrayLength(arrays);
  items_.reserve(static_cast<size_t>(count));
  for (jsize index = 0; index < count; ++index) {
    jbyteArray array = get_byte_array(env, arrays, index);
    items_.emplace_back(env, array);
    env->DeleteLocalRef(array);
  }
  // Pointers into the items are taken once every item is in place.
  lent_.reserve(items_.size());
  for (const LentBytes& item : items_) {
    lent_.push_back(item.bytes());
  }
}

const UaBytes* LentBytesList::data() const { return lent_.empty() ? nullptr : lent_.data(); }

size_t LentBytesList::count() const { return lent_.size(); }

LentFiles::LentFiles(JNIEnv* env, jobjectArray names, jobjectArray contents) {
  const jsize count = std::min(env->GetArrayLength(names), env->GetArrayLength(contents));
  names_.reserve(static_cast<size_t>(count));
  contents_.reserve(static_cast<size_t>(count));
  for (jsize index = 0; index < count; ++index) {
    jbyteArray name = get_byte_array(env, names, index);
    names_.emplace_back(env, name);
    env->DeleteLocalRef(name);
    jbyteArray content = get_byte_array(env, contents, index);
    contents_.emplace_back(env, content);
    env->DeleteLocalRef(content);
  }
  lent_.reserve(names_.size());
  for (size_t index = 0; index < names_.size(); ++index) {
    lent_.push_back(UaNamedBytes{names_[index].text(), contents_[index].bytes()});
  }
}

const UaNamedBytes* LentFiles::data() const { return lent_.empty() ? nullptr : lent_.data(); }

size_t LentFiles::count() const { return lent_.size(); }

ReceivedBuffer::~ReceivedBuffer() { ua_free_buffer(buffer_); }

UaBuffer* ReceivedBuffer::out() { return &buffer_; }

jbyteArray ReceivedBuffer::to_java(JNIEnv* env) const {
  const auto length = static_cast<jsize>(buffer_.len);
  jbyteArray array = env->NewByteArray(length);
  if (array != nullptr && length > 0) {
    env->SetByteArrayRegion(array, 0, length, reinterpret_cast<const jbyte*>(buffer_.data));
  }
  return array;
}

bool throw_on_refusal(JNIEnv* env, jclass native_library, UaStatus status, const ReceivedBuffer& error_message) {
  if (status == UA_STATUS_OK) {
    return false;
  }
  jbyteArray message = error_message.to_java(env);
  if (message == nullptr) {
    return true;
  }
  jmethodID refusal_of = env->GetStaticMethodID(native_library, kRefusalOfName, kRefusalOfSignature);
  if (refusal_of == nullptr) {
    return true;
  }
  auto refusal = static_cast<jthrowable>(
      env->CallStaticObjectMethod(native_library, refusal_of, static_cast<jint>(status), message));
  if (refusal != nullptr && !env->ExceptionCheck()) {
    env->Throw(refusal);
  }
  return true;
}

void throw_java(JNIEnv* env, const char* class_name, const char* message) {
  jclass exception_class = env->FindClass(class_name);
  if (exception_class != nullptr) {
    env->ThrowNew(exception_class, message);
  }
}

}  // namespace ua_jni
