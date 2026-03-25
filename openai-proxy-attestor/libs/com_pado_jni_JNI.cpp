#include "com_pado_jni_JNI.h"
#include "include/pado.h"
#include "include/PadoCallbackLib.h"
/*
 * Class:     com_pado_jni_JNI
 * Method:    start
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_pado_jni_JNI_start
  (JNIEnv * env, jobject, jint a)
{
    printf("start");
    IPadoCallbackLib* iPadoCallbackLib = new PadoCallbackLib(env);
    start(a,iPadoCallbackLib);
    return true;
}

/*
 * Class:     com_pado_jni_JNI
 * Method:    stop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_pado_jni_JNI_stop
  (JNIEnv *, jobject)
{
    stop();
    printf("stop");
}


JNIEXPORT jstring JNICALL Java_com_pado_jni_JNI_call
(JNIEnv* env, jobject obj, jstring param) {
    IPadoCallbackLib* iPadoCallbackLib = new PadoCallbackLib(env);
    const char* cParam = env->GetStringUTFChars(param, NULL);
    string str = iPadoCallbackLib->call(cParam);
    string str2 = iPadoCallbackLib->call(cParam);
    env->ReleaseStringUTFChars(param, cParam); // release

//    printf(str);
    return env->NewStringUTF(str.c_str());
}