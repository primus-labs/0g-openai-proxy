package com.pado.jni;

import static com.pado.constant.SysParam.LD_LIBRARY_PATH;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author xuda
 * @Type JNI.java
 * @Desc
 * @date 2023/4/18
 */
public class JNI {
    public native boolean start(int port);
    public native void stop();
    public native String call(String param);

    static {
        String libraryPath = System.getenv(LD_LIBRARY_PATH);
        if (libraryPath != null && !libraryPath.isBlank()) {
            for (String entry : libraryPath.split(":")) {
                if (entry == null || entry.isBlank()) {
                    continue;
                }
                Path candidate = Path.of(entry, "libpado_jni.so");
                if (Files.isRegularFile(candidate)) {
                    System.load(candidate.toAbsolutePath().toString());
                    return;
                }
            }
        }
        System.loadLibrary("pado_jni");
    }
}
