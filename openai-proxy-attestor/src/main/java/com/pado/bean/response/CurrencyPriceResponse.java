package com.pado.bean.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author xuda
 * @Type CurrencyPriceResponse.java
 * @Desc
 * @date 2023/5/24
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class CurrencyPriceResponse {

    private List<ResultDTO> result;

    @NoArgsConstructor
    @Data
    public static class ResultDTO {

        private String currency;

        private String price;
    }
}
