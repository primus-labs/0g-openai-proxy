package com.pado.handler.impl.callback;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.pado.bean.CommonResponse;
import com.pado.bean.response.DataSignatureFromAPIResponse;

class DataSignatureCallMethodHandlerTest {
    public static void main(String[] args) {
        String body = "{\n"
            + "    \"greaterThanBaseValue\": true,\n"
            + "    \"sourceUseridHash\": \"\",\n"
            + "    \"extendedData\": \"{\\\"screen_name\\\":\\\"goose_eggsss\\\"}\",\n"
            + "    \"rawParam\": {\n"
            + "        \"isUserClick\": \"true\",\n"
            + "        \"source\": \"x\",\n"
            + "        \"requestid\": \"d7f40644-2451-4845-83cc-b2af10dcd6cc\",\n"
            + "        \"padoUrl\": \"wss://api-dev.padolabs.org/algorithm-proxy\",\n"
            + "        \"modelType\": \"proxytls\",\n"
            + "        \"proxyUrl\": \"wss://api-dev.padolabs.org/algoproxy\",\n"
            + "        \"errLogUrl\": \"wss://api.padolabs.org/logs\",\n"
            + "        \"cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\",\n"
            + "        \"getdatatime\": \"1773651484067\",\n"
            + "        \"credVersion\": \"1.0.5\",\n"
            + "        \"user\": {\n"
            + "            \"userid\": \"2032362090919886848\",\n"
            + "            \"address\": \"0xB12a1f7035FdCBB4cC5Fa102C01346BD45439Adf\",\n"
            + "            \"token\": \"eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJwYWRvbGFicy5vcmciLCJzdWIiOiIweDlBYkFhMDIwQmY4ZThBNjJmZWYxMUFDODEwNjM3NjFmNTMzNWRDNjEiLCJleHAiOjQ5MjY5ODc4NTYsInVzZXItaWQiOjIwMzIzNjIwOTA5MTk4ODY4NDgsInNjb3BlIjoiYXV0aCJ9.irXnKADX3JJWjPz0tv7rSuAAgZJ9lDeDbWFJr-ZsdqmeKCNSSeecE2LfIrELPLoMwCGwE5FIGo7zfXDVP5RFSGA9KdYrQGh-bjsxPel_rC1iYNdvAsP4-NQ9sWaKXbZAEK0Y87GnwPEEwk2fXO1dL0ystgDSrx47bD8R3e7HHaF3vb3F5ginwaygxJbl_gA3GjB6ifVWmW7Ge9I9p9yeM38g_6_fDxaKdkhsvCgHVmZfy3Muf7C-K-FF17o83SHDexEMYhTsB7cAezCQgoSD6CEuR-qFHf9T6qex0BKC7CcGrrSdMWWko65tlerO31GVmwVzd1VEj1EoCBhv6vd_kQ\"\n"
            + "        },\n"
            + "        \"authUseridHash\": \"0xf2b3a5b00979cce54cec58045c7e276bd920723bda6d8a7eda88a7009eed547f\",\n"
            + "        \"setHostName\": \"true\",\n"
            + "        \"appParameters\": {\n"
            + "            \"appId\": \"0x668c71321acab7b4eb5d65f20d351cee08a3101d\",\n"
            + "            \"appSignParameters\": \"{\\\"appId\\\":\\\"0x668c71321acab7b4eb5d65f20d351cee08a3101d\\\",\\\"attTemplateID\\\":\\\"2dbbc11e-afc2-4a3e-a120-9eb2a456e027\\\",\\\"userAddress\\\":\\\"0xB12a1f7035FdCBB4cC5Fa102C01346BD45439Adf\\\",\\\"timestamp\\\":1773651479807,\\\"attMode\\\":{\\\"algorithmType\\\":\\\"proxytls\\\",\\\"resultType\\\":\\\"plain\\\"},\\\"requestid\\\":\\\"0b3ad274-acbf-4d99-ad52-3d81103693de\\\",\\\"backUrl\\\":\\\"\\\",\\\"computeMode\\\":\\\"normal\\\",\\\"noProxy\\\":true,\\\"allJsonResponseFlag\\\":\\\"false\\\"}\",\n"
            + "            \"appSignature\": \"0xf205fd8a5dd69a605988558915a8f4c466d35849378389bcaec9b97a28e5a7bf040e6e50e2e2f5d2692b407c768ec23cfd494d5a92b8ea0dfc07447bc1254fb41b\",\n"
            + "            \"additionParams\": \"\"\n"
            + "        },\n"
            + "        \"specialTask\": \"\",\n"
            + "        \"getAllJsonResponse\": \"false\",\n"
            + "        \"reqType\": \"web\",\n"
            + "        \"templateId\": \"2013902573337055232\",\n"
            + "        \"PADOSERVERURL\": \"https://api-dev.padolabs.org\",\n"
            + "        \"padoExtensionVersion\": \"0.3.46\",\n"
            + "        \"version\": \"1.4.15\",\n"
            + "        \"padoVersion\": \"1.4.16\",\n"
            + "        \"baseName\": \"api.x.com\",\n"
            + "        \"baseValue\": \"0\",\n"
            + "        \"ext\": {\n"
            + "            \"extRequests\": {\n"
            + "                \"sdk-0\": {\n"
            + "                    \"name\": \"sdk-0\",\n"
            + "                    \"url\": \"https://api.x.com/1.1/account/settings.json?include_ext_sharing_audiospaces_listening_data_with_followers=true&include_mention_filter=true&include_nsfw_user_flag=true&include_nsfw_admin_flag=true&include_ranked_timeline=true&include_alt_text_compose=true&include_ext_dm_av_call_settings=true&ext=ssoConnections&include_country_code=true&include_ext_dm_nsfw_media_filter=true\",\n"
            + "                    \"urlType\": \"REGX\",\n"
            + "                    \"method\": \"GET\",\n"
            + "                    \"calculationType\": \"MULTI_CONDITION\",\n"
            + "                    \"parseSchema\": \"{\\\"conditions\\\":{\\\"type\\\":\\\"CONDITION_EXPANSION\\\",\\\"op\\\":\\\"BOOLEAN_AND\\\",\\\"subconditions\\\":[{\\\"field\\\":\\\"$.screen_name\\\",\\\"op\\\":\\\"REVEAL_STRING\\\",\\\"type\\\":\\\"FIELD_REVEAL\\\",\\\"reveal_id\\\":\\\"screen_name\\\"}]}}\"\n"
            + "                },\n"
            + "                \"orders\": [\n"
            + "                    \"sdk-0\"\n"
            + "                ]\n"
            + "            }\n"
            + "        }\n"
            + "    }\n"
            + "}";
        DataSignatureCallMethodHandler dataSignatureCallMethodHandler = new DataSignatureCallMethodHandler();
        String call = dataSignatureCallMethodHandler.call(body);
        System.out.println(call);
    }

}