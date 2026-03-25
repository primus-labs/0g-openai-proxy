package com.pado.bean.param;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author xuda
 * @Type CheckAddressParams.java
 * @Desc
 * @date 2023/5/24
 */
@Data
@NoArgsConstructor
public class CheckAddressParams {
    Map<String, String> params;
}
