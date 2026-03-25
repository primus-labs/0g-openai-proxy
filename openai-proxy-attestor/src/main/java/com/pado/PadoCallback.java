package com.pado;

import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.pado.bean.MethodTypeBean;
import com.pado.handler.ICallMethodHandler;
import com.pado.handler.impl.callback.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xuda
 * @Type PadoCallback.java
 * @Desc
 * @date 2023/5/22
 */
public class PadoCallback {
    static Log log = Log.get(PadoCallback.class);
    private static final Map<String, ICallMethodHandler> callers;

    static {
        callers = new HashMap<>();
        //add CheckAddressCallMethodHandler
        CheckAddressCallMethodHandler checkAddressCallMethodHandler = new CheckAddressCallMethodHandler();
        callers.put(checkAddressCallMethodHandler.method(),checkAddressCallMethodHandler);
        //add CurrencyPriceCallMethodHandler
        CurrencyPriceCallMethodHandler currencyPriceCallMethodHandler = new CurrencyPriceCallMethodHandler();
        callers.put(currencyPriceCallMethodHandler.method(),currencyPriceCallMethodHandler);
        //add MessageReportMethodHandler
        MessageReportMethodHandler messageReportMethodHandler = new MessageReportMethodHandler() ;
        callers.put(messageReportMethodHandler.method(),messageReportMethodHandler);
        //add DataSignatureCallMethodHandler
        DataSignatureCallMethodHandler dataSignatureCallMethodHandler = new DataSignatureCallMethodHandler();
        callers.put(dataSignatureCallMethodHandler.method(), dataSignatureCallMethodHandler);
        //add CheckParamsCallMethodHandler
        CheckParamsCallMethodHandler checkParamsCallMethodHandler = new CheckParamsCallMethodHandler();
        callers.put(checkParamsCallMethodHandler.method(),checkParamsCallMethodHandler);

        ReportAttErrorMethodHandler reportAttErrorMethodHandler = new ReportAttErrorMethodHandler();
        callers.put(reportAttErrorMethodHandler.method(),reportAttErrorMethodHandler);
    }


    /**
     * detail see at : https://github.com/pado-labs/pado-server/blob/main/pado-attestation-callback/pado-attestation-callback-lib/IPadoCallbackLib.h
     *
     * @param param
     * @return
     */
    public String call(byte[] param) {
        String str = new String(param, java.nio.charset.StandardCharsets.UTF_8);

        log.info("get param:{}",str);

        MethodTypeBean bean = JSONUtil.toBean(str, MethodTypeBean.class);
        String method = bean.getMethod();
        ICallMethodHandler iCallMethodHandler = callers.get(method);
        if(iCallMethodHandler == null){
            log.error("not supoort method:{}",method);
            return "";
        }
        return iCallMethodHandler.invoke(bean.getParams());
    }
}