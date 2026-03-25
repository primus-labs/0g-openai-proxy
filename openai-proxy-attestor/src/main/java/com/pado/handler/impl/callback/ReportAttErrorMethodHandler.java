package com.pado.handler.impl.callback;

import com.pado.bean.param.DataSignatureParams;
import com.pado.handler.BaseCallMethodHandler;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;

public class ReportAttErrorMethodHandler extends BaseCallMethodHandler<DataSignatureParams, String> {

    @Override
    public Class pClass() {
        return DataSignatureParams.class;
    }

    @Override
    public String call(DataSignatureParams dataSignatureParams) {
        try {
            if (!useRemoteCallback()) {
                return "save error success";
            }
            UrlBuilder urlBuilder = UrlBuilder.of(appendUrl(CALLBACK_URL, "/attestation/error", null));
            HttpRequest httpRequest = new HttpRequest(urlBuilder);
            httpRequest.setMethod(Method.POST);
            httpRequest.body(JSONUtil.toJsonStr(dataSignatureParams));
            HttpResponse execute = httpRequest.execute();
            String body = execute.body();
            execute.close();
            return "save error success";
        } catch (Exception e) {
            log.error("sign data error,detail:{}",e.getMessage());
            return "save error failed";
        }
    }

    @Override
    public String method() {
        return "reportAttError";
    }
}
