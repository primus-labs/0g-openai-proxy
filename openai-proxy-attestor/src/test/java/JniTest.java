import com.pado.jni.JNI;

/**
 * @author xuda
 * @Type JniTest.java
 * @Desc
 * @date 2023/5/25
 */
public class JniTest {
    public static void main(String[] args) {

        JNI jni = new JNI();
//        jni.start(8081);
//        String call = jni.call("  {\n" +
//                "          \"method\": \"reportMessage\",\n" +
//                "          \"params\": {\n" +
//                "              \"message\": \"fill your message here\"\n" +
//                "          }\n" +
//                "      }");
//        System.out.println(call);
        String call = jni.call("{\"method\":\"checkWalletAndUserId\",\"params\":{\"address\":\"0x2A46883d79e4Caf14BCC2Fbf18D9f12A8bB18D07\",\"token\":\"\",\"userId\":\"0123456789\"}}");
        System.out.println(call);

    }
}
