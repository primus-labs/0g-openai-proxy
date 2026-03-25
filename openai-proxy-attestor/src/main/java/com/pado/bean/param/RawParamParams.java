package com.pado.bean.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RawParamParams {

    @JsonProperty("requestid")
    private String requestid;
    @JsonProperty("version")
    private String version;
    @JsonProperty("padoExtensionVersion")
    private String padoExtensionVersion;
    @JsonProperty("credVersion")
    private String credVersion;
    @JsonProperty("source")
    private String source;
    @JsonProperty("modelType")
    private String modelType;
    @JsonProperty("baseName")
    private String baseName;
    @JsonProperty("specialTask")
    private String specialTask;
    @JsonProperty("holdingToken")
    private String holdingToken;
    @JsonProperty("baseUrl")
    private String baseUrl;
    @JsonProperty("padoUrl")
    private String padoUrl;
    @JsonProperty("proxyUrl")
    private String proxyUrl;
    @JsonProperty("cipher")
    private String cipher;
    @JsonProperty("getdatatime")
    private String getdatatime;
    @JsonProperty("exchange")
    private RawParamParams.ExchangeDTO exchange;
    @JsonProperty("sigFormat")
    private String sigFormat;
    @JsonProperty("appParameters")
    private Object appParameters;
    @JsonProperty("schemaType")
    private String schemaType;
    @JsonProperty("schema")
    private List<RawParamParams.SchemaDTO> schema;

    @JsonProperty("user")
    private RawParamParams.UserDTO user;
    @JsonProperty("baseValue")
    private String baseValue;

    @JsonProperty("greaterThanBaseValue")
    private Object greaterThanBaseValue;

    @JsonProperty("event")
    private String event;

    @JsonProperty("templateId")
    private String templateId;

    @JsonProperty("chatGPTExpression")
    private String chatGPTExpression;

    @JsonProperty("ext")
    private Object ext;

    @NoArgsConstructor
    @Data
    public static class ExchangeDTO {
        @JsonProperty("apikey")
        private String apikey;
        @JsonProperty("apisecret")
        private String apisecret;
        @JsonProperty("apipassword")
        private String apipassword;
    }

    @NoArgsConstructor
    @Data
    public static class UserDTO {
        @JsonProperty("userid")
        private String userid;
        @JsonProperty("address")
        private String address;
        @JsonProperty("token")
        private String token;
    }

    @NoArgsConstructor
    @Data
    public static class SchemaDTO {
        @JsonProperty("name")
        private String name;
        @JsonProperty("type")
        private String type;
    }
}
