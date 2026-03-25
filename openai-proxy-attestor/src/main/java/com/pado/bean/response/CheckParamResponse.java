package com.pado.bean.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author xuda
 * @Type CheckAddressResponse.java
 * @Desc
 * @date 2023/5/24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckParamResponse {
    private boolean result;
    private Map<String,Object> extraData;

}
