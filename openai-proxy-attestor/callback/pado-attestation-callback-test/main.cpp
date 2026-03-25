#include <iostream>
#include "PadoCallbackLib.h"

int main() {
    std::cout << "start!" << std::endl;
    IPadoCallbackLib* lib = new PadoCallbackLib();
    std::string jsonParam = R"({"method":"getCurrencyPrice","params":{"currency":["BTC","ETH"],"source":"BINANCE"}})";
    string test = lib->call(jsonParam);
    std::cout << "return:"<<test << std::endl;
    std::cout << "end!" << std::endl;
    return 0;
}
