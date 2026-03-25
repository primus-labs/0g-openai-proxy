package com.pado;

import cn.hutool.json.JSONUtil;
import com.pado.bean.response.CheckParamResponse;
import com.pado.bean.response.DataSignatureResponse;
import com.pado.handler.impl.callback.CheckParamsCallMethodHandler;
import com.pado.local.LocalSignatureService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalModeSignatureTest {
    @BeforeAll
    void setUp() throws Exception {
        Path tempDir = Files.createTempDirectory("pado-attestation-wallet");
        String walletFileName = WalletUtils.generateFullNewWalletFile("testpass", tempDir.toFile());
        File walletFile = tempDir.resolve(walletFileName).toFile();

        System.setProperty("keystorePath", walletFile.getAbsolutePath());
        System.setProperty("keystorePass", "testpass");
        System.setProperty("PADO_SCHEMA_PROFILE", "dev");
    }

    @Test
    void shouldBypassCheckParamsInLocalMode() {
        CheckParamsCallMethodHandler handler = new CheckParamsCallMethodHandler();
        CheckParamResponse response = handler.call("{\"rawParam\":{\"schemaType\":\"Token Holdings\"}}");
        assertTrue(response.isResult());
        assertNull(response.getExtraData());
    }

    @Test
    void shouldSignPrimusRequestInLocalMode() throws Exception {
        String body = readResource("do_sign.json");

        String responseBody = LocalSignatureService.getInstance().sign(body);
        DataSignatureResponse response = JSONUtil.toBean(responseBody, DataSignatureResponse.class);

        assertNotNull(response.getResult());
        assertNull(response.getErrorCode());
        assertEquals("1700732539862", response.getResult().getRequestid());
        assertTrue(response.getResult().getEncodedData().contains("\"attestors\""));
        assertTrue(response.getResult().getSignature().startsWith("0x"));
        assertNotNull(response.getResult().getAuthUseridHash());
    }

    @Test
    void shouldRejectLegacyRequestInLocalMode() throws Exception {
        String body = readResource("test_token_holding.json");
        String params = JSONUtil.parseObj(body).getJSONObject("params").toString();
        String responseBody = LocalSignatureService.getInstance().sign(body);
        if (responseBody.contains("\"Missing rawParam\"")) {
            responseBody = LocalSignatureService.getInstance().sign(params);
        }
        DataSignatureResponse response = JSONUtil.toBean(responseBody, DataSignatureResponse.class);

        assertNull(response.getResult());
        assertEquals(-1102, response.getErrorCode());
        assertTrue(response.getErrorMsg().contains("Unsupported local signature request"));
    }

    private String readResource(String fileName) throws Exception {
        Path path = Path.of("src", "test", "resources", fileName);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
