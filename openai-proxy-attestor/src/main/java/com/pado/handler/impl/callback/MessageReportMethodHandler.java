package com.pado.handler.impl.callback;

import cn.hutool.log.Log;
import com.pado.bean.param.MessageReportParams;
import com.pado.bean.response.MessageReportResponse;
import com.pado.handler.BaseCallMethodHandler;

/**
 * @author xuda
 * @Type MessageReportMethodHandler.java
 * @Desc
 * @date 2023/5/25
 */

public class MessageReportMethodHandler extends BaseCallMethodHandler<MessageReportParams, MessageReportResponse> {
    protected Log log = Log.get(MessageReportMethodHandler.class);


    @Override
    public Class pClass() {
        return MessageReportParams.class;
    }

    @Override
    public MessageReportResponse call(MessageReportParams messageReportParams) {
        log.warn("-------------------Get report message from pado_lib------------------");
        log.warn(messageReportParams.getMessage());
        log.warn("---------------------------------------------------------------------");
        return MessageReportResponse.builder().result(true).build();
    }

    @Override
    public String method() {
        return "reportMessage";
    }
}
