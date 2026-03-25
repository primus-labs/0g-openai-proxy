package com.pado.bean.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @Author xuda
 * @Date 2023/11/27 15:20
 */
@NoArgsConstructor
@Data
public class DataSignatureFromAPIResponse {

    @JsonProperty("result")
    private DataSignatureResponse.ResultDTO result;

    @JsonProperty("errorCode")
    private String errorCode;

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
        @JsonProperty("additionData")
        private Map<String,Object> additionData;
    }
}
