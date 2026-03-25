#include "IPadoCallbackLib.h"
#include "jni.h"

class PadoCallbackLib : public IPadoCallbackLib{
public:
    PadoCallbackLib(JNIEnv* tenv);
    string call(const string& param);
    JNIEnv *get_env(int *attach);
    void del_env();
private:
    JavaVM* global_jvm;
};

