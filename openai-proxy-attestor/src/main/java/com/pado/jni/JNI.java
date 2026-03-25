package com.pado.jni;

import static com.pado.constant.SysParam.LD_LIBRARY_PATH;

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
        //Load so
        System.load(libraryPath+"/libpado_jni.so");
    }
}
