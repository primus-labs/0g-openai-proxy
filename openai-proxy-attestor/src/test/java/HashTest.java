//import org.web3j.abi.datatypes.Utf8String;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.Arrays;
//import java.util.List;
//
//import org.web3j.abi.DefaultFunctionEncoder;
//import org.web3j.abi.datatypes.Address;
//import org.web3j.abi.datatypes.Bool;
//import org.web3j.abi.datatypes.Type;
//import org.web3j.abi.datatypes.Utf8String;
//import org.web3j.abi.datatypes.generated.Bytes32;
//import org.web3j.abi.datatypes.generated.Uint64;
//import org.web3j.crypto.StructuredData;
//import org.web3j.crypto.StructuredDataEncoder;
//import org.web3j.utils.Numeric;
//public class HashTest {
//    //Sepolia: {
//    String rpcUrl = "";
//    String easContact = "0xC2679fBD37d54388Ce493F1DB75320D236e1815e";
//    String schemaUid = "0x72785c9098718a320672387465aba432ea1f2a40e7c2acc67f61ee5d8e7f5b09";
//    //}
//    public static void main(String[] args) throws IOException {
//
//        List<Type> types = Arrays.asList(
//                new Utf8String("okx"),
//                new Bytes32(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000")),
//                new Bytes32(Numeric.hexStringToByteArray("0x1234567890123456789012345678901234567890123456789012345678901234")),
//                new Address(160, "0x7ab44DE0156925fe0c24482a2cDe48C465e47573"),
//                new Uint64(1234567890),
//                new Uint64(1234567890),
//                new Bool(true)
//        );
//        //byte[] input = Numeric.hexStringToByteArray(new DefaultFunctionEncoder().encodeParameters(types));
//        String input = new DefaultFunctionEncoder().encodeParameters(types);
//        System.out.println("input="+input);
//
//        String jsonMessageString = new String(
//                Files.readAllBytes(Paths.get("/home/xuda/workspace/pado-server/pado-attestation-server/src/test/resources/test.json").toAbsolutePath()),
//                StandardCharsets.UTF_8
//        );
//        System.out.println("jsonMessageString="+jsonMessageString);
//        StructuredDataEncoder dataEncoder = new StructuredDataEncoder(jsonMessageString);
//
//        byte[] res = dataEncoder.hashStructuredData();
//
//        System.out.println("res="+Numeric.toHexString(res));
//        System.out.println(dataEncoder.jsonMessageObject);
//    }
//}
