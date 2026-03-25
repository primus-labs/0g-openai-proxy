package com.pado.handler.impl.callback;

import cn.hutool.json.JSONUtil;
import com.pado.bean.param.CurrencyPriceParams;
import com.pado.bean.response.CurrencyPriceResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyPriceCallMethodHandlerTest {
    public static void main(String[] args) {
        byte[] bytes = "65494c6e8efa073e7fcbf2480c3a472ec965476eff9b7af1a816222d50bd994d5790c6fd88111c506e1f8d32bfabd064edd2d8608522723bdecda45bba2196ba1b".getBytes();
        CurrencyPriceCallMethodHandler currencyPriceCallMethodHandler = new CurrencyPriceCallMethodHandler();
        CurrencyPriceParams currencyPriceParams = JSONUtil.toBean("{\"currency\":[\"USDT\",\"ETH\",\"ETC\",\"LINK\",\"USD\",\"BTC\",\"BCH\",\"LTC\",\"ZRX\",\"BAT\",\"USDC\",\"ZEC\",\"LOOM\",\"MANA\",\"DNT\",\"CVC\",\"MKR\",\"DAI\",\"OMG\",\"GNT\",\"SNT\",\"KNC\",\"XRP\",\"REP\",\"XLM\",\"EOS\",\"DOGE\",\"XTZ\",\"ALGO\",\"DASH\",\"ATOM\",\"OXT\",\"COMP\",\"ENJ\",\"BAND\",\"NMR\",\"CGLD\",\"LPT\",\"LRC\",\"MLN\",\"UMA\",\"YFI\",\"UNI\",\"BAL\",\"REN\",\"WBTC\",\"NU\",\"YFII\",\"FIL\",\"AAVE\",\"BNT\",\"GRT\",\"SNX\",\"STORJ\",\"SUSHI\",\"ETH2\",\"MATIC\",\"SKL\",\"ADA\",\"ANKR\",\"CRV\",\"ICP\",\"NKN\",\"OGN\",\"1INCH\",\"FORTH\",\"CTSI\",\"TRB\",\"POLY\",\"MIR\",\"RLC\",\"SOL\",\"DOT\",\"GTC\",\"AMP\",\"SHIB\",\"CHZ\",\"KEEP\",\"QNT\",\"BOND\",\"RLY\",\"CLV\",\"FARM\",\"MASK\",\"ANT\",\"FET\",\"PAX\",\"ACH\",\"ASM\",\"PLA\",\"RAI\",\"TRIBE\",\"ORN\",\"IOTX\",\"UST\",\"QUICK\",\"AXS\",\"REQ\",\"WLUNA\",\"TRU\",\"RAD\",\"DDX\",\"SUKU\",\"RGT\",\"XYO\",\"COTI\",\"ZEN\",\"AUCTION\",\"BUSD\",\"WCFG\",\"JASMY\",\"BTRST\",\"AGLD\",\"AVAX\",\"ARPA\",\"BADGER\",\"KRL\",\"PERP\",\"RARI\",\"FX\",\"TRAC\",\"LCX\",\"CRO\",\"MTL\",\"ABT\",\"CVX\",\"AVT\",\"AST\",\"VGX\",\"MDT\",\"ALCX\",\"COVAL\",\"FOX\",\"MUSD\",\"GYEN\",\"INV\",\"LQTY\",\"PRO\",\"API3\",\"NCT\",\"SHPING\",\"UPI\",\"CELR\",\"GALA\",\"POWR\",\"SPELL\",\"ENS\",\"BLZ\",\"CTX\",\"ERN\",\"IDEX\",\"MCO2\",\"POLS\",\"SUPER\",\"UNFI\",\"DIA\",\"STX\",\"GODS\",\"IMX\",\"RBN\",\"BICO\",\"GFI\",\"ATA\",\"GLM\",\"MPL\",\"PLU\",\"SWFTC\",\"DESO\",\"FIDA\",\"ORCA\",\"GNO\",\"OCEAN\",\"SAND\",\"KSM\",\"CRPT\",\"QSP\",\"RNDR\",\"NEST\",\"PRQ\",\"HOPR\",\"JUP\",\"ALICE\",\"HIGH\",\"MATH\",\"SYN\",\"AIOZ\",\"WAMPL\",\"AERGO\",\"INDEX\",\"TONE\",\"APE\",\"MINA\",\"ROSE\",\"FLOW\",\"CBETH\",\"ELA\",\"DREP\",\"FORT\",\"ALEPH\",\"DEXT\",\"FIS\",\"BIT\",\"GMT\",\"GST\",\"MEDIA\",\"C98\",\"OP\",\"MUSE\",\"SYLO\",\"GUSD\",\"ARB\",\"TIME\",\"RPL\",\"MXC\",\"HBAR\",\"KAVA\",\"HNT\",\"SPA\",\"EGLD\",\"GHST\",\"NEAR\",\"INJ\",\"AUDIO\",\"MONA\",\"TVK\",\"POND\",\"DYP\",\"LDO\",\"LIT\",\"ILV\",\"PUNDIX\",\"PYR\",\"PNG\",\"METIS\",\"RARE\",\"QI\",\"MSOL\",\"OSMO\",\"XCN\",\"DAR\",\"MAGIC\",\"AURORA\",\"BOBA\",\"VOXEL\",\"OOKI\",\"MULTI\",\"LOKA\",\"T\",\"MNDE\",\"STG\",\"GAL\",\"00\",\"HFT\",\"DIMO\",\"EUROC\",\"BLUR\",\"APT\",\"WAXL\",\"LSETH\",\"SUI\",\"AXL\",\"ACS\",\"FLR\",\"PRIME\"],\"source\":\"coinbase\"}",CurrencyPriceParams.class);

        CurrencyPriceResponse call = currencyPriceCallMethodHandler.call(currencyPriceParams);
        System.out.println(call);
    }

}