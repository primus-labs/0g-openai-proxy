package com.pado.bean;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * @author xuda
 */
@Data
public class CommonResponse<T> {
    private final static Integer RC_SUCCESS = 0;
    private final static Integer RC_FAILURE = 1;

    private final static String MC_SUCCESS = "SUCCESS";
    private final static String MC_FAILURE = "FAILURE";


    private static CommonResponse<Object> SUCCESS = new CommonResponse<>(RC_SUCCESS, MC_SUCCESS, StrUtil.EMPTY, null);
    private static CommonResponse<Object> FAILURE = new CommonResponse<>(RC_FAILURE, MC_FAILURE, StrUtil.EMPTY, null);

    private Integer rc;

    private String mc;

    private String msg;

    private T result;


    private CommonResponse(Integer rc, String mc, String msg, T result) {
        this.rc = rc;
        this.mc = mc;
        this.msg = msg;
        this.result = result;
    }

    /* SUCCESS ----------------------------------------------- */

    public static CommonResponse<Object> success() {
        return SUCCESS;
    }

    public static <T> CommonResponse<T> success(String mc) {
        return new CommonResponse<>(RC_SUCCESS, mc, StrUtil.EMPTY, null);
    }

    public static <T> CommonResponse<T> success(String mc, String msg) {
        return new CommonResponse<>(RC_SUCCESS, mc, StrUtil.EMPTY, null);
    }

    public static <T> CommonResponse<T> success(String mc, String msg, T result) {
        return new CommonResponse<>(RC_SUCCESS, mc, StrUtil.EMPTY, result);
    }

    public static <T> CommonResponse<T> successOf(T result) {
        return new CommonResponse<>(RC_SUCCESS, MC_SUCCESS, StrUtil.EMPTY, result);
    }


    /* FAILURE ----------------------------------------------- */

    public static CommonResponse<Object> failure() {
        return FAILURE;
    }

    public static <T> CommonResponse<T> failure(String mc) {
        return new CommonResponse<>(RC_FAILURE, mc, StrUtil.EMPTY, null);
    }

    public static <T> CommonResponse<T> failure(String mc, String msg) {
        return new CommonResponse<>(RC_FAILURE, mc, msg, null);
    }


    /* Custom ----------------------------------------------- */

    public static <T> CommonResponse<T> custom(Integer rc, String mc, String msg, T result) {
        return new CommonResponse<>(rc, mc, msg, result);
    }

}
