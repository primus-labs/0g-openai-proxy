package com.pado.bean.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class EasEip712Message {

    @JsonProperty("schema")
    private String schema;
    @JsonProperty("recipient")
    private String recipient;
    @JsonProperty("expirationTime")
    private Integer expirationTime;
    @JsonProperty("revocable")
    private Boolean revocable;
    @JsonProperty("data")
    private String data;
    @JsonProperty("refUID")
    private String refUID;
    @JsonProperty("deadline")
    private Integer deadline;
}
