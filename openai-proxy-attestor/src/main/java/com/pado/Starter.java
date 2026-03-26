package com.pado;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.pado.local.LocalSignatureService;
import com.pado.server.AttestationServer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

import static com.pado.constant.SysParam.SERVER_PORT;

/**
 * @author xuda
 * @Type Starter.java
 * @Desc
 * @date 2023/4/19
 */
public class Starter {
    static Log log = Log.get(Starter.class);

    public static void main(String[] args) throws Exception {
        String homeDir = System.getProperty("user.dir");
        log.info("Run at :{}", homeDir);
        log.info("Current version :{}", 1);
        if (StrUtil.length(homeDir) > 1 && homeDir.endsWith("/")) {
            homeDir = StrUtil.replaceLast(homeDir, "/", "");
        }
        if (!FileUtil.exist(homeDir)) {
            throw new Exception("cipher file not exists in work directory!");
        }
        String serverPort = System.getenv(SERVER_PORT);
        if (StrUtil.isEmpty(serverPort)) {
            throw new RuntimeException("server_port is empty, start failed!");
        }
        log.info("Attestor callback mode: {}", LocalSignatureService.usesLocalSigning() ? "local-signature" : "remote-callback");
        LocalSignatureService.warmupIfEnabled();
        PadoCallback padoCallback = new PadoCallback();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AttestationServer server = new AttestationServer();
        boolean started = server.startServer(Integer.valueOf(serverPort));
        if (started) {
            log.info("Server listen at {}!", serverPort);
            countDownLatch.await();
        } else {
            countDownLatch.countDown();
        }
        log.error("Bye!");
    }
}
