package com.pado.handler.impl.callback;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.pado.bean.param.CurrencyPriceParams;
import com.pado.bean.response.CurrencyPriceResponse;
import com.pado.handler.BaseCallMethodHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xuda
 * @Type CurrencyPriceCallMethodHandler.java
 * @Desc
 * @date 2023/5/24
 */
public class CurrencyPriceCallMethodHandler extends BaseCallMethodHandler<CurrencyPriceParams, CurrencyPriceResponse> {

    protected Log log = Log.get(CurrencyPriceCallMethodHandler.class);


    @Override
    public Class pClass() {
        return CurrencyPriceParams.class;
    }

    @Override
    public CurrencyPriceResponse call(CurrencyPriceParams currencyPriceParams) {
        if (!useRemoteCallback()) {
            return CurrencyPriceResponse.builder().result(new ArrayList<>()).build();
        }
        List<String> currency = currencyPriceParams.getCurrency();
        String source = currencyPriceParams.getSource();
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("source",source.toUpperCase());
        paramMap.put("currency",String.join(",",currency));
        UrlBuilder urlBuilder = UrlBuilder.of(appendUrl(CALLBACK_URL,"/public/curency/price",paramMap));
        HttpRequest httpRequest = new HttpRequest(urlBuilder);
        httpRequest.setMethod(Method.GET);
        HttpResponse execute = httpRequest.execute();
        Map<String,String> prices = JSONUtil.toBean(execute.body(),Map.class);
        List<CurrencyPriceResponse.ResultDTO> resultDTOList = new ArrayList<>();
        execute.close();

        for (String s : currency) {
            CurrencyPriceResponse.ResultDTO resultDTO = new CurrencyPriceResponse.ResultDTO();
            resultDTO.setCurrency(s);
            resultDTO.setPrice(prices.get(s));
            resultDTOList.add(resultDTO);
        }
        return CurrencyPriceResponse.builder().result(resultDTOList).build();
    }

    @Override
    public String method() {
        return "getCurrencyPrice";
    }
}
