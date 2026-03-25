package com.pado.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author xuda
 * @Type BaseCallMethodHandler.java
 * @Desc
 * @date 2023/5/24
 */
public abstract class BaseCallMethodHandler<P,R> implements ICallMethodHandler<R>{
    protected Log log = Log.get(BaseCallMethodHandler.class);

    protected static final String CALLBACK_URL;
    static {
        String callUrl = System.getenv("CALL_URL");
        if (StrUtil.isNotEmpty(callUrl)) {
            CALLBACK_URL = callUrl;
        } else {
            CALLBACK_URL = System.getenv("PADO_CALLBACK_URL");
        }
        if (StrUtil.isEmpty(CALLBACK_URL)) {
            System.out.println("CALL_URL and PADO_CALLBACK_URL are not configured, local callback mode enabled");
        }
    }


    @Override
    public String invoke(String p) {
        P param;
        if (String.class.equals(pClass())) {
            param = (P) p;
        } else {
            param = (P) JSONUtil.toBean(p, pClass());
        }
        R rsp = call(param);
        if (rsp instanceof String) {
            return (String) rsp;
        }
        return JSONUtil.toJsonStr(rsp);
    }

    protected boolean useRemoteCallback() {
        return StrUtil.isNotEmpty(CALLBACK_URL);
    }

    protected static String appendUrl(String baseUrl, String path, Map<String,String> paramMap){
        if(!baseUrl.endsWith("/")){
            baseUrl = baseUrl+"/";
        }
        if(path.startsWith("/")){
            path = StrUtil.replaceFirst(path,"/","");
        }
        if(paramMap == null){
            return baseUrl+path;
        }
        String query = URLUtil.buildQuery(paramMap, Charset.defaultCharset());
        return baseUrl+path+"?"+query;
    }


    public abstract Class pClass();

    public abstract R call(P p);

    public abstract String method();

}
