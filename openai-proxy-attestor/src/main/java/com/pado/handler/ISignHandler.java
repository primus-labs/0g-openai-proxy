package com.pado.handler;


import com.pado.bean.param.DataSignatureParams;
import com.pado.bean.response.DataSignatureResponse;

public interface ISignHandler {
    DataSignatureResponse getSignature(DataSignatureParams dataSignatureParams);
}
