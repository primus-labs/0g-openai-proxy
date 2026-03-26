package com.pado.handler.impl.callback;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.pado.bean.CommonResponse;
import com.pado.bean.response.DataSignatureFromAPIResponse;
import com.pado.bean.response.DataSignatureResponse;
import com.pado.handler.BaseCallMethodHandler;
import com.pado.local.LocalSignatureService;

import java.util.HashMap;
import java.util.Map;

public class DataSignatureCallMethodHandler extends BaseCallMethodHandler<String, String> {

    @Override
    public Class pClass() {
        return String.class;
    }

    @Override
    public String call(String rawRequestBody) {
        try {
            String responseBody;
            if (useRemoteCallback()) {
                UrlBuilder urlBuilder = UrlBuilder.of(appendUrl(CALLBACK_URL, "/attestation/signature", null));
                HttpRequest httpRequest = new HttpRequest(urlBuilder);
                httpRequest.setMethod(Method.POST);
                httpRequest.body(rawRequestBody);
                HttpResponse execute = httpRequest.execute();
                responseBody = execute.body();
                execute.close();
                CommonResponse<DataSignatureFromAPIResponse> commonResponse =
                    JSONUtil.toBean(responseBody, new TypeReference<CommonResponse<DataSignatureFromAPIResponse>>() {}, true);
                if (commonResponse.getRc() != null && commonResponse.getRc() == 1) {
                    DataSignatureResponse dataSignatureResponse = new DataSignatureResponse();
                    DataSignatureResponse.ResultDTO resultDTO = new DataSignatureResponse.ResultDTO();
                    resultDTO.setSignature(StrUtil.EMPTY);
                    resultDTO.setEncodedData(commonResponse.getMsg());
                    String mc = commonResponse.getMc();
                    Integer mcInteger = -1;
                    try {
                        mcInteger = Integer.parseInt(mc);
                    } catch (Exception e) {
                        log.error("Data convert failed, class:{} ,value:{}", String.class, mc);
                    }
                    resultDTO.setEncodedData(commonResponse.getMsg());
                    Map<String, Object> extraData = new HashMap<>();
                    extraData.put("errorCode", mcInteger);
                    extraData.put("errorMsg", commonResponse.getMsg());
                    resultDTO.setExtraData(JSONUtil.toJsonStr(extraData));

                    dataSignatureResponse.setResult(resultDTO);
                    dataSignatureResponse.setErrorCode(mcInteger);
                    dataSignatureResponse.setErrorMsg(commonResponse.getMsg());
                    String rspBody = JSONUtil.toJsonStr(dataSignatureResponse);
                    log.info("dataSignatureResponse:{}", rspBody);
                    return rspBody;
                }
            } else {
                log.info("Use LocalSignatureService");
                responseBody = LocalSignatureService.getInstance().sign(rawRequestBody);
            }

            log.info("dataSignatureResponse:{}", responseBody);
            return shouldAttachPhalaQuote(responseBody) ? phalaTdxQuote(responseBody) : responseBody;
        } catch (Exception e) {
            log.error("sign data error,detail:{}", e.getMessage(), e);
            DataSignatureResponse dataSignatureResponse = new DataSignatureResponse();
            dataSignatureResponse.setErrorCode(-1101);
            dataSignatureResponse.setErrorMsg(e.getMessage());
            return JSONUtil.toJsonStr(dataSignatureResponse);
        }
    }

    private boolean shouldAttachPhalaQuote(String attestationStr) {
        try {
            DataSignatureFromAPIResponse response =
                JSONUtil.toBean(attestationStr, DataSignatureFromAPIResponse.class);
            return response != null && response.getResult() != null
                && StrUtil.isNotEmpty(response.getResult().getEncodedData());
        } catch (Exception e) {
            log.warn("Skip phala quote because response parsing failed");
            return false;
        }
    }

    public String phalaTdxQuote(String attestationStr){
        DataSignatureFromAPIResponse commonResponse =
            JSONUtil.toBean(attestationStr, DataSignatureFromAPIResponse.class);
        String usePhalaTee = System.getenv("USE_PHALA_TEE");
        log.info("Run in TEE:{}",usePhalaTee);
        if(StrUtil.isEmpty(usePhalaTee)){
            return attestationStr;
        }
        String jsInternalService = System.getenv("JS_INTERNAL_SERVICE");
        if(StrUtil.isEmpty(jsInternalService)){
            log.warn("Run in TEE but jsInternalEndpoint is empty, will skip");
            return attestationStr;
        }
        String tdxQuoteEndpoint = jsInternalService +"/phala/tdxQuote";
        Map<String,Object> map = new HashMap<>();
        map.put("reportData",commonResponse.getResult().getEncodedData());
        try {
            HttpRequest request = HttpRequest.post(tdxQuoteEndpoint);
            request.body(JSONUtil.toJsonStr(map));
            HttpResponse execute = request.execute();
            String body = execute.body();
            execute.close();
            CommonResponse bean = JSONUtil.toBean(body, CommonResponse.class);
            if(bean.getRc() == 0){
                log.info("tdxQuote success:{}",JSONUtil.toJsonStr(bean));
                Map<String,Object> extraData = new HashMap<>();
                extraData.put("phalaQuote",bean.getResult());
                DataSignatureResponse.ResultDTO result = commonResponse.getResult();
                result.setAdditionData(extraData);
                commonResponse.setResult(result);
                return JSONUtil.toJsonStr(commonResponse);
            }else{
                log.info("tdxQuote failed!");
                return attestationStr;
            }
        }catch (Exception e){
            log.error("tdxQuote error,detail:{}",e.getMessage());
        }
        return attestationStr;
    }


    @Override
    public String method() {
        return "dataSignature";
    }
}
