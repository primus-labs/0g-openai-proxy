package com.pado.bean.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class DataSignatureParams {
    @JsonProperty("rawParam")
    private RawParamParams rawParam;
    @JsonProperty("errorCode")
    private String errorCode;
    @JsonProperty("errorMessage")
    private String errorMessage;
    @JsonProperty("greaterThanBaseValue")
    private Boolean greaterThanBaseValue;
    @JsonProperty("sourceUseridHash")
    private String sourceUseridHash;
    @JsonProperty("extendedData")
    private String extendedData;
}
