//import java.io.IOException;
//import java.math.BigInteger;
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
//import org.web3j.crypto.Credentials;
//import org.web3j.crypto.StructuredDataEncoder;
//import org.web3j.protocol.Web3j;
//import org.web3j.protocol.http.HttpService;
//import org.web3j.tx.gas.ContractGasProvider;
//import org.web3j.tx.gas.DefaultGasProvider;
//import org.web3j.utils.Numeric;
//
//public class SignTeest {
//
//    //Sepolia: {
//    static String rpcUrl = "https://sepolia.infura.io/v3/b6bf7d3508c941499b10025c0776eaf8";
//    static String easContact = "0xC2679fBD37d54388Ce493F1DB75320D236e1815e";
//    static String schemaUid = "0x72785c9098718a320672387465aba432ea1f2a40e7c2acc67f61ee5d8e7f5b09";
//    //}
//    public static void main(String[] args) throws IOException {
//
//        List<Type> types = Arrays.asList(
//                new Utf8String("okx"),
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
//                Files.readAllBytes(Paths.get("/Users/fksyuan/work/code/pado-labs/pado-server/pado-attestation-server/src/main/java/com/pado/server/test.json").toAbsolutePath()),
//                StandardCharsets.UTF_8
//        );
//        System.out.println("jsonMessageString="+jsonMessageString);
//        StructuredDataEncoder dataEncoder = new StructuredDataEncoder(jsonMessageString);
//        byte[] res = dataEncoder.hashStructuredData();
//        System.out.println("res="+Numeric.toHexString(res));
//        System.out.println(dataEncoder.jsonMessageObject);
//
//        Web3j web3j = Web3j.build(new HttpService(rpcUrl));
//        Credentials crendentails = Credentials.create("2729a22ee92c555848bc1183ace6a757b3db44edd10aab9cbe18cb8e69b56f02");
//        ContractGasProvider contractGasProvider = new DefaultGasProvider();
////        Eas eas = Eas.load(easContact, web3j, crendentails, contractGasProvider);
////        try {
////            BigInteger nonce = eas.getNonce("0x2A46883d79e4Caf14BCC2Fbf18D9f12A8bB18D07").send();
////            System.out.println("nonce="+nonce);
////        } catch (Exception e) {
////            System.out.println("Exception="+e.toString());
////            throw new RuntimeException(e);
////        }
//
//        //Credentials crendentails2 = WalletUtils.loadCredentials(password, File.createTempFile());
//        //crendentails2.getEcKeyPair().sign(res);
//    }
//}