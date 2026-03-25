package com.pado.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Deprecated
/**
 * save provider's properties as map
 */
public class AttestationProvider {

    public static Map<String, String> getProvider(String provider) {
        if ("ETHEREUM".equalsIgnoreCase(provider)) {
            return EAS_ETHEREUM;
        }
        if ("ARBITRUMONE".equalsIgnoreCase(provider)) {
            return EAS_ARBITRUMONE;
        }
        if ("SEPOLIA".equalsIgnoreCase(provider)) {
            return EAS_SEPOLIA;
        }
        throw new RuntimeException("Unsupport attestation provider:"+provider);
    }

    /**
     * Ethereum
     */
    private static final Map<String, String> EAS_ETHEREUM = new HashMap<String,String>() {{
        put("rpcUrl", "https://mainnet.infura.io/v3/b6bf7d3508c941499b10025c0776eaf8");
        put("easContact", "0xA1207F3BBa224E2c9c3c6D5aF63D0eb1582Ce587");
        put("schemaUid", "");
        put("chainId", null);
    }};

    /**
     * ARBITRUMONE
     */
    private static final Map<String, String> EAS_ARBITRUMONE = new HashMap<String,String>() {{
        put("rpcUrl", "https://arb1.arbitrum.io/rpc");
        put("easContact", "0xbD75f629A22Dc1ceD33dDA0b68c546A1c035c458");
        put("schemaUid", "");
        put("chainId", null);
    }};

    /**
     * Sepolia
     */
    private static final Map<String, String> EAS_SEPOLIA = new HashMap<String,String>() {{
        put("rpcUrl", "https://sepolia.infura.io/v3/b6bf7d3508c941499b10025c0776eaf8");
        //this value is Deprecated
        put("easContact", "0xC2679fBD37d54388Ce493F1DB75320D236e1815e");
        //easProxyContact is deployed by pado
        put("easProxyContact", "0x2884e43b48c2cc623a19c0c3d260dd8f398fd5f3");
        put("schemaUid", "0x45316fbaa4070445d3ed1b041c6161c844e80e89c368094664ed756c649413a9");
        //
        put("schemaUidAssetProof", "0x45316fbaa4070445d3ed1b041c6161c844e80e89c368094664ed756c649413a9");
        put("schemaUidTokenHolding", "0xe4c12be3c85cada725c600c1f2cde81d7cc15f957537e5756742acc3f5859084");
        put("chainId", "11155111");
    }};
}
