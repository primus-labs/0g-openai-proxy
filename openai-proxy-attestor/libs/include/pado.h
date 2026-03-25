#pragma once
#include "IPadoCallbackLib.h"

// JNI api(s)
bool start(int port, IPadoCallbackLib* cb);
void stop();

// Not JNI api(s)
