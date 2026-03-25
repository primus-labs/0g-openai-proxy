#include "IPadoCallbackLib.h"

class PadoCallbackLib : public IPadoCallbackLib{
public:
    PadoCallbackLib();
    string call(const string& param);
};

