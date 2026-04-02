#include "PadoCallbackLib.h"
#include <codecvt>
#include <cstdlib>
#include <iomanip>
#include <iostream>
#include <locale>
#include <sstream>
#include <string>

using namespace std;

namespace {
std::string bytes_to_hex(const std::string& value) {
    std::ostringstream oss;
    oss << std::hex << std::setfill('0');
    for (unsigned char ch : value) {
        oss << std::setw(2) << static_cast<int>(ch);
    }
    return oss.str();
}

std::string utf16_units_to_hex(const jchar* chars, jsize length) {
    std::ostringstream oss;
    oss << std::hex << std::setfill('0');
    for (jsize i = 0; i < length; ++i) {
        oss << std::setw(4) << static_cast<unsigned int>(chars[i]);
    }
    return oss.str();
}

std::string jstring_to_utf8(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return "";
    }

    const jchar* chars = env->GetStringChars(value, nullptr);
    if (chars == nullptr) {
        return "";
    }

    const jsize length = env->GetStringLength(value);
    std::u16string utf16;
    utf16.reserve(static_cast<size_t>(length));
    for (jsize i = 0; i < length; ++i) {
        utf16.push_back(static_cast<char16_t>(chars[i]));
    }
    env->ReleaseStringChars(value, chars);

    std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;
    return converter.to_bytes(utf16);
}
}  // namespace

PadoCallbackLib::PadoCallbackLib(JNIEnv* tenv) {
    tenv->GetJavaVM(&global_jvm);
}

std::string PadoCallbackLib::call(const std::string& param) {
    std::cout << "param is: " << param << std::endl;
    std::cout << "param bytes(hex): " << bytes_to_hex(param) << std::endl;

    int attach = 0;
    JNIEnv* env = get_env(&attach);
    if (env == nullptr) {
        std::cerr << "Failed to get JNIEnv!" << std::endl;
        return "";
    }

    const char* fullname = std::getenv("JAVA_FULL_NAME");
    if (fullname == nullptr) {
        std::cerr << "JAVA_FULL_NAME environment variable not set!" << std::endl;
        return "";
    }

    jclass cls = env->FindClass(fullname);
    if (cls == nullptr) {
        std::cerr << "Class not found: " << fullname << std::endl;
        return "";
    }
    jmethodID method = env->GetMethodID(cls, "call", "([B)Ljava/lang/String;");
    if (method == nullptr) {
        std::cerr << "Method not found: call" << std::endl;
        return "";
    }

    jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");
    if (constructor == nullptr) {
        std::cerr << "Constructor not found!" << std::endl;
        return "";
    }

    jobject padoCallbackLib = env->NewObject(cls, constructor);
    if (padoCallbackLib == nullptr) {
        std::cerr << "Failed to create Java object!" << std::endl;
        return "";
    }

    const char* cppBytes = param.c_str();
    jsize length = static_cast<jsize>(param.size());

    jbyteArray javaByteArray = env->NewByteArray(length);
    if (javaByteArray == nullptr) {
        std::cerr << "Failed to create Java byte array!" << std::endl;
        return "";
    }

    env->SetByteArrayRegion(javaByteArray, 0, length, reinterpret_cast<const jbyte*>(cppBytes));

    jobject resultObj = env->CallObjectMethod(padoCallbackLib, method, javaByteArray);
    if (resultObj == nullptr) {
        std::cerr << "Java method returned null!" << std::endl;
        return "";
    }

    jstring result = static_cast<jstring>(resultObj);

    const jchar* utf16Chars = env->GetStringChars(result, nullptr);
    if (utf16Chars == nullptr) {
        std::cerr << "Failed to access Java UTF-16 chars!" << std::endl;
        env->DeleteLocalRef(javaByteArray);
        env->DeleteLocalRef(result);
        env->DeleteLocalRef(padoCallbackLib);
        return "";
    }
    const jsize utf16Length = env->GetStringLength(result);
    const std::string utf16Hex = utf16_units_to_hex(utf16Chars, utf16Length);
    env->ReleaseStringChars(result, utf16Chars);

    const char* modifiedUtf8Chars = env->GetStringUTFChars(result, nullptr);
    std::string modifiedUtf8;
    if (modifiedUtf8Chars != nullptr) {
        modifiedUtf8 = modifiedUtf8Chars;
        env->ReleaseStringUTFChars(result, modifiedUtf8Chars);
    }

    std::string str;
    try {
        str = jstring_to_utf8(env, result);
    } catch (const std::exception& ex) {
        std::cerr << "Failed to convert Java UTF-16 to UTF-8: " << ex.what() << std::endl;
        str = modifiedUtf8;
    }

    env->DeleteLocalRef(javaByteArray);
    env->DeleteLocalRef(result);
    env->DeleteLocalRef(padoCallbackLib);

    if (attach == 1) {
        del_env();
    }

    std::cout << "java result utf16 length: " << utf16Length << std::endl;
    std::cout << "java result utf16 units(hex): " << utf16Hex << std::endl;
    std::cout << "java result modified utf8 bytes(hex): " << bytes_to_hex(modifiedUtf8) << std::endl;
    std::cout << "result str utf8 bytes(hex): " << bytes_to_hex(str) << std::endl;
    std::cout << "result str is: " << str << std::endl;

    return str;
}

JNIEnv* PadoCallbackLib::get_env(int* attach) {
    if (global_jvm == NULL) return NULL;

    *attach = 0;
    JNIEnv* jni_env = NULL;

    int status = global_jvm->GetEnv((void**)&jni_env, JNI_VERSION_1_6);

    if (status == JNI_EDETACHED || jni_env == NULL) {
        status = global_jvm->AttachCurrentThread(reinterpret_cast<void**>(&jni_env), NULL);
        if (status < 0) {
            jni_env = NULL;
        } else {
            *attach = 1;
        }
    }
    return jni_env;
}

void PadoCallbackLib::del_env() {
    global_jvm->DetachCurrentThread();
}

int main() {
    JavaVM* jvm;
    JNIEnv* env;
    JavaVMInitArgs vm_args;
    JavaVMOption options[1];
    const char* classpath = std::getenv("JAVA_CLASS_PATH");
    if (classpath == nullptr) {
        cout << "please set JAVA_CLASS_PATH!" << endl;
        exit(1);
    }

    cout << "class path is : " << classpath << endl;

    options[0].optionString = const_cast<char*>(classpath);
    vm_args.version = JNI_VERSION_1_8;
    vm_args.nOptions = 1;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = JNI_TRUE;

    jint res = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
    if (res != JNI_OK) {
        printf("cant create jvm");
        exit(1);
    }
    printf("jvm create successfully");
    PadoCallbackLib* obj = new PadoCallbackLib(env);
    string test2 = obj->call("test");
    std::cout << " get btc in binance price is: " << test2 << std::endl;
    std::cout << "end" << std::endl;
    return 0;
}
