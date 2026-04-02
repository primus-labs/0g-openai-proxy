#include "PadoCallbackLib.h"
#include <iostream>
#include <cstdlib>


using namespace std;


PadoCallbackLib::PadoCallbackLib(JNIEnv* tenv){
    tenv->GetJavaVM(&global_jvm);

}

std::string PadoCallbackLib::call(const std::string& param) {
    std::cout << "param is: " << param << std::endl;
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

    // Converts a string to a UTF-8 encoded byte array
    const char* cppBytes = param.c_str();
    jsize length = static_cast<jsize>(param.size());

    // Create a Java byte array
    jbyteArray javaByteArray = env->NewByteArray(length);
    if (javaByteArray == nullptr) {
        std::cerr << "Failed to create Java byte array!" << std::endl;
        return "";
    }

    // Copy a C++ byte array to a Java byte array
    env->SetByteArrayRegion(javaByteArray, 0, length, reinterpret_cast<const jbyte*>(cppBytes));

    // Call java method
    jobject resultObj = env->CallObjectMethod(padoCallbackLib, method, javaByteArray);
    if (resultObj == nullptr) {
        std::cerr << "Java method returned null!" << std::endl;
        return "";
    }

    jstring result = static_cast<jstring>(resultObj);
    const char* convertedValue = env->GetStringUTFChars(result, nullptr);
    std::string str = convertedValue;

    // Release resources
    env->ReleaseStringUTFChars(result, convertedValue);
    env->DeleteLocalRef(javaByteArray);
    env->DeleteLocalRef(result);
    env->DeleteLocalRef(padoCallbackLib);

    if (attach == 1) {
        del_env();
    }
    std::cout << "result str is: " << str << std::endl;

    return str;
}

JNIEnv* PadoCallbackLib:: get_env(int *attach) {
   if (global_jvm == NULL) return NULL;

   *attach = 0;
   JNIEnv* jni_env = NULL;

   int status = global_jvm->GetEnv((void **)&jni_env, JNI_VERSION_1_6);

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

void PadoCallbackLib:: del_env() {
   global_jvm->DetachCurrentThread();
}

int main() {
    JavaVM* jvm;
    JNIEnv* env;
    JavaVMInitArgs vm_args;
    JavaVMOption options[1];
    const char* classpath = std::getenv("JAVA_CLASS_PATH");
    if (classpath == nullptr) {
        cout<<"please set JAVA_CLASS_PATH!"<<endl;
        exit(1);
    }

    cout<<"class path is : "<<classpath<<endl;

    // set java evm
    //   -Djava.class.path=/home/xuda/CLionProjects/pado_callback_lib/callbacker-1.0.jar
    options[0].optionString = const_cast<char*>(classpath);
    vm_args.version = JNI_VERSION_1_8;
    vm_args.nOptions = 1;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = JNI_TRUE;

    // create java vm
    jint res = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
    if (res != JNI_OK) {
        printf("cant create jvm");
        exit(1);
    }
    printf("jvm create successfully");
    PadoCallbackLib* obj = new PadoCallbackLib(env);
    string test2 = obj->call("test");
    std::cout << " get btc in binance price is: "<< test2 << std::endl;
    std::cout << "end" << std::endl;
    return 0;
}

