package com.pado.bean.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author xuda
 * @Type CurrencyPriceParams.java
 * @Desc
 * @date 2023/5/24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyPriceParams {

    private List<String> currency;

    private String source;
}
