#include <string>
using namespace std;


class IPadoCallbackLib {
public:
    /**
     * 1. check wallet and userId
     * param:
     *    {
     *      "method": "checkWalletAndUserId",
     *      "params": {
     *        "address": "0x123344",//wallet address
     *        "userId": "132"  //userId
     *      }
     *    }
     * return:
     *    {
     *       "result": true/false
     *    }
     * 2. get currency price
     * param:
     *    {
     *        "method": "getCurrencyPrice",
     *        "params": {
     *            "currency": ["BTC","ETH"],
     *            "source": "BINANCE"
     *        }
     *    }
     * source support: BINANCE,OKX,KUCOIN,COINBASE,BYBIT,GATE,HUOBI,BITGET,MEXC
     * return:
     *   {
     *       "result": [
     *           {"currency":"BTC","price": "28170.00"},
     *           {"currency":"ETH","price": "18799.00"}
     *       ]
     *   }
     */
    virtual string call(const string& param) = 0;
};