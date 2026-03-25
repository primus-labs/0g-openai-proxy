import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.pado.bean.CommonResponse;
import com.pado.bean.response.DataSignatureResponse;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * @Author xuda
 * @Date 2023/11/24 11:38
 */
public class Test {
    public static void main(String[] args) {
        Log log = Log.get();
//        UrlBuilder urlBuilder = UrlBuilder.of(appendUrl("http://127.0.0.1:8080", "/attestation/signature", null));
//        HttpRequest httpRequest = new HttpRequest(urlBuilder);
//        httpRequest.setMethod(Method.POST);
//        httpRequest.body(FileUtil.readString(new File("/Users/xuda/workspace/padolabs/pado-server/pado-attestation-server/src/test/resources/do_sign.json"),Charset.defaultCharset()));
//        HttpResponse execute = httpRequest.execute();
//        String body = execute.body();
//        execute.close();
//        String body = "{\n" + "    \"rc\": 1,\n" + "    \"mc\": -10001,\n" + "    \"msg\": \"The sourceUserIdHash(92b2aeefe25f0c630bec9976d5d9ee8286440ad91a6a8d67d57c5588954b2eea) of binance has bind address([0xd34ee53f3e0d5362356eb81deac84c01b8f14bd8, 0xd34ee53f3e0d5362356eb81deac84c01b8f14b10])\",\n" + "    \"result\": null\n" + "}";
        String body = "{\n" + "    \"rc\": 1,\n" + "    \"mc\": \"EVENT_10001\",\n" + "    \"msg\": \"The sourceUserIdHash(92b2aeefe25f0c630bec9976d5d9ee8286440ad91a6a8d67d57c5588954b2eea) of binance has bind address([0xd34ee53f3e0d5362356eb81deac84c01b8f14bd8, 0xd34ee53f3e0d5362356eb81deac84c01b8f14b10])\",\n" + "    \"result\": null\n" + "}";
        CommonResponse<DataSignatureResponse> commonResponse =
            JSONUtil.toBean(body, new TypeReference<CommonResponse<DataSignatureResponse>>() {}, true);

//        if (commonResponse.getRc() !=null && commonResponse.getRc() == 1) {
//            DataSignatureResponse dataSignatureResponse = new DataSignatureResponse();
//            DataSignatureResponse.ResultDTO resultDTO = new DataSignatureResponse.ResultDTO();
//            resultDTO.setSignature(StrUtil.EMPTY);
//            resultDTO.setEncodedData(commonResponse.getMsg());
//            String mc = commonResponse.getMc();
//            Integer mcInteger = -1;
//            try {
//                mcInteger = Integer.parseInt(mc);
//            }catch (Exception e){
//                log.error("Data Convert failed, class:{} ,value:{}",String.class,mc);
//                mcInteger = -1;
//            }
//            resultDTO.setEncodedData(commonResponse.getMsg());
//            dataSignatureResponse.setResult(resultDTO);
//            dataSignatureResponse.setErrorCode(mcInteger);
//            dataSignatureResponse.setErrorMsg(commonResponse.getMsg());
//            log.info(JSONUtil.toJsonStr(dataSignatureResponse));
//        }
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
}
