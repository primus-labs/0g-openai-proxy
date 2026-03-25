import cn.hutool.json.JSONUtil;
import com.pado.PadoCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class SignHandlerTest {
    public static void main(String[] args) throws Exception {
        String body = "{\n" + "  \"recipient\": \"0xB12a1f7035FdCBB4cC5Fa102C01346BD45439Adf\",\n" + "  \"request\": {\n" + "    \"url\": \"https://api.x.com/1.1/account/settings.json?include_ext_sharing_audiospaces_listening_data_with_followers=true&include_mention_filter=true&include_nsfw_user_flag=true&include_nsfw_admin_flag=true&include_ranked_timeline=true&include_alt_text_compose=true&ext=ssoConnections&include_country_code=true&include_ext_dm_nsfw_media_filter=true\",\n" + "    \"header\": \"\",\n" + "    \"method\": \"GET\",\n" + "    \"body\": \"\"\n" + "  },\n" + "  \"reponseResolve\": [\n" + "    {\n" + "      \"keyName\": \"screen_name\",\n" + "      \"parseType\": \"\",\n" + "      \"parsePath\": \"$.screen_name\"\n" + "    }\n" + "  ],\n" + "  \"data\": \"{\\\"screen_name\\\":\\\"goose_eggsss\\\"}\",\n" + "  \"attConditions\": \"[{\\\"op\\\":\\\"REVEAL_STRING\\\",\\\"field\\\":\\\"$.screen_name\\\"}]\",\n" + "  \"timestamp\": 1738919224691,\n" + "  \"additionParams\": \"\",\n" + "  \"attestors\": [\n" + "    {\n" + "      \"attestorAddr\": \"0xdb736b13e2f522dbe18b2015d0291e4b193d8ef6\",\n" + "      \"url\": \"https://primuslabs.xyz\"\n" + "    }\n" + "  ],\n" + "  \"signatures\": [\n" + "    \"0x993530caba01c2c076375f9e13f740ce92059f5a6fae01a46dab300e013251440c4624f7a3343b5c0cb136a1deb2a7f4e98fc9a85d604625a74d73ce29d7b85b1c\"\n" + "  ]\n" + "}";
        System.out.println(JSONUtil.toJsonStr(JSONUtil.toBean(body, Map.class)));

    }

}
