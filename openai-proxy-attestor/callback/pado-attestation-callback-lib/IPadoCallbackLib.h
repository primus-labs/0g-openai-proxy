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
     *        "token": "token-xxxxxxxxxxxx",
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
     * 3 . report message
     *  param:
     *  {
     *      "method": "reportMessage",
     *      "params": {
     *          "message": "fill your message here"
     *      }
     *  }
     * return:
     *  {
     *      "result": true
     *  }
     *
     * 4. data signature
     * param:
     *  {
     *    "method": "dataSignature",
     *    "params": {
     *      "rawParam": {
     *        "requestid": "1",
     *        "version": "1.0.0",
     *        "source": "okx",
     *        "baseName": "www.okx.com",
     *        "baseUrl": "104.18.2.151:443",
     *        "padoUrl": "127.0.0.1:8081",
     *        "proxyUrl": "127.0.0.1:9000",
     *        "cipher": "",
     *        "getdatatime": "1685092402879",
     *        "exchange": {
     *          "apikey": "xxx",
     *          "apisecret": "xxx",
     *          "apipassword": "xxx"
     *        },
     *        "sigFormat": "EAS-Ethereum",
     *        "schemaType": "exchange-balance",
     *        "schema": [
     *          {
     *            "name": "source",
     *            "type": "string"
     *          },
     *          {
     *            "name": "useridhash",
     *            "type": "string"
     *          },
     *          {
     *            "name": "address",
     *            "type": "string"
     *          },
     *          {
     *            "name": "getdatatime",
     *            "type": "string"
     *          },
     *          {
     *            "name": "baseValue",
     *            "type": "string"
     *          },
     *          {
     *            "name": "balanceGreaterBaseValue",
     *            "type": "string"
     *          }
     *        ],
     *        "user": {
     *          "userid": "0123456789",
     *          "address": "0x2A46883d79e4Caf14BCC2Fbf18D9f12A8bB18D07",
     *          "token": "xxx"
     *        },
     *        "baseValue": "1000",
     *        "greaterThanBaseValue": null,
     *        "ext": {
     *          "parseSchema": "OKX_ACCOUNT_BALANCE",
     *          "extRequests": {
     *            "orders": [
     *              "account-balance"
     *            ],
     *            "account-balance": {
     *              "url": "https://www.okx.com/api/v5/account/balance",
     *              "method": "GET",
     *              "headers": {
     *                "OK-ACCESS-KEY": "8a236275-eedc-46d9-a592-485fb38d1dfe",
     *                "OK-ACCESS-PASSPHRASE": "Padopado@2022",
     *                "OK-ACCESS-SIGN": "LGCcfSvL00ejKcXLQ7KUCVS68AeUX8RN9htSzBcvxDM=",
     *                "OK-ACCESS-TIMESTAMP": "2023-05-19T07:21:26.379Z"
     *              },
     *              "body": {}
     *            }
     *          }
     *        }
     *      },
     *      "greaterThanBaseValue": true/false
     *    }
     *  }
     * return:
     *{
     *  "result": {
     *    "requestid":"1",
     *    "getDataTime":"1685092402879",
     *    "useridhash":"0x8e8493b70bcebe5f974697f8ad9fe39653bb227e59c805e517788e9056d60874",
     *    "encodedData": "000000000000036f6b780000000000000000000000000000000000000000000000000000000000",
     *    "signature": "0xe20047bae74674c117d36af76ea5745c4711824c713cac065996ddad8eef6f9a"
     *  }
     *}
     *
     *
     */
    virtual string call(const string& param) = 0;
};