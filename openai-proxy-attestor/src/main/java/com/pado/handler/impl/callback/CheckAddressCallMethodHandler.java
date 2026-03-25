package com.pado.handler.impl.callback;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import cn.hutool.log.Log;
import com.pado.bean.response.CheckAddressResponse;
import com.pado.handler.BaseCallMethodHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xuda
 * @Type CheckAddressCallMethodHandler.java
 * @Desc
 * @date 2023/5/24
 */
public class CheckAddressCallMethodHandler extends BaseCallMethodHandler<Map,CheckAddressResponse>  {
    protected Log log = Log.get(CheckAddressCallMethodHandler.class);

    @Override
    public Class pClass() {
        return Map.class;
    }

    @Override
    public CheckAddressResponse call(Map checkAddressParams) {
        if (!useRemoteCallback()) {
            return CheckAddressResponse.builder().result(false).build();
        }
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("address", (String) checkAddressParams.get("address"));
        UrlBuilder urlBuilder = UrlBuilder.of(appendUrl(CALLBACK_URL,"/attestation/address/validate",paramMap));
        HttpRequest httpRequest = new HttpRequest(urlBuilder);
        if(checkAddressParams.containsKey("userId")) {
            httpRequest.header("userId", (String) checkAddressParams.get("userId"));
            httpRequest.header("token", (String) checkAddressParams.get("token"));
        }
        httpRequest.setMethod(Method.POST);
        HttpResponse execute = httpRequest.execute();
        String body = execute.body();
        execute.close();
        return CheckAddressResponse.builder().result(BooleanUtil.toBooleanObject(body)).build();
    }


    @Override
    public String method() {
        return "checkWalletAndUserId";
    }
}
