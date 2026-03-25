package com.pado.server;

import cn.hutool.log.Log;
import com.pado.jni.JNI;


/**
 * @author xuda
 */
public class AttestationServer {

    static Log log = Log.get(AttestationServer.class);

    public boolean startServer(int port) {
        JNI jni = new JNI();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            //stop the attestation server if system shutdown
            log.error("Error occurred, Shut down the attestation service!");
            jni.stop();
            log.error("Shut down successfully!");
        }));
        return jni.start(port);
    }
}