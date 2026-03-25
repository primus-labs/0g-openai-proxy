//#include "IPadoCallbackLib.h"
#include "jni.h"

class PadoCallbackLib : public IPadoCallbackLib{
public:
    PadoCallbackLib(JNIEnv* tenv);
    string call(const string& param);
private:
    jobject padoCallbackLib;
    jclass cls;
    JNIEnv* env;
};

