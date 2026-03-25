#include <string>
using namespace std;


class IPadoCallbackLib {
public:
    virtual string call(const string& param) = 0;
};