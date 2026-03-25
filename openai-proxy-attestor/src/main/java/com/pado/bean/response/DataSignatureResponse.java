package com.pado.bean.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@Data
public class DataSignatureResponse {

    @JsonProperty("result")
    private ResultDTO result;

    @JsonProperty("errorCode")
    private Integer  errorCode;

    @JsonProperty("errorMsg")
    private String errorMsg;

    @NoArgsConstructor
    @Data
    public static class ResultDTO {
        @JsonProperty("requestid")
        private String requestid;
        @JsonProperty("getDataTime")
        private String getDataTime;
        @JsonProperty("encodedData")
        private String encodedData;
        @JsonProperty("signature")
        private String signature;
        @JsonProperty("authUseridHash")
        private String authUseridHash;
        @JsonProperty("extraData")
        private String extraData;
        @JsonProperty("additionData")
        private Map<String,Object> additionData;

    }
}
