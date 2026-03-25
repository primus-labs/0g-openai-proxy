package com.pado.handler.impl.callback;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.pado.bean.CommonResponse;
import com.pado.bean.response.CheckParamResponse;
import com.pado.handler.BaseCallMethodHandler;

import java.util.HashMap;
import java.util.Map;

public class CheckParamsCallMethodHandler extends BaseCallMethodHandler<String, CheckParamResponse> {
    @Override
    public Class pClass() {
        return String.class;
    }

    @Override
    public CheckParamResponse call(String rawParamParams) {
        if (!useRemoteCallback()) {
            CheckParamResponse checkParamResponse = new CheckParamResponse();
            checkParamResponse.setResult(true);
            return checkParamResponse;
        }

        String url = appendUrl(CALLBACK_URL, "/attestation/params/check-new", null);
        HttpRequest httpRequest = HttpRequest.of(url);
        httpRequest.setMethod(Method.POST);
        httpRequest.body(rawParamParams);
        HttpResponse execute = httpRequest.execute();
        String body = execute.body();
        execute.close();
        log.info("check param result is:{}", body);
        CommonResponse<Boolean> result = JSONUtil.toBean(body, CommonResponse.class, false);
        Integer rc = result.getRc();
        Map<String, Object> extraData = new HashMap<>();
        CheckParamResponse rsp = new CheckParamResponse();
        if (rc != 0) {
            extraData.put("errorCode", result.getMc());
            extraData.put("errorMsg", result.getMsg());
            rsp.setResult(false);
            rsp.setExtraData(extraData);
        } else {
            rsp.setResult(true);
        }
        log.info("check result:{}", JSONUtil.toJsonStr(rsp));
        return rsp;
    }

    @Override
    public String method() {
        return "checkParams";
    }
}
