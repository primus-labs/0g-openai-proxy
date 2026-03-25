package com.pado.handler;

/**
 * @author xuda
 * @Type ICallMethodHandler.java
 * @Desc
 * @date 2023/5/24
 */
public interface ICallMethodHandler<R> {
    /**
     * invoke method
     * @param p
     * @return
     */
    String invoke(String p);

}
