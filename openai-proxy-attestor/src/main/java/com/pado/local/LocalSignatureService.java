package com.pado.local;

import cn.hutool.core.codec.Base58;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.jayway.jsonpath.JsonPath;
import com.pado.bean.response.DataSignatureResponse;
import com.pado.handler.BaseCallMethodHandler;
import org.web3j.abi.DefaultFunctionEncoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class LocalSignatureService {
    protected Log log = Log.get(LocalSignatureService.class);

    private static final String ERROR_UNSUPPORTED = "Unsupported local signature request";
    private static final String PRIMUS_URL = "https://primuslabs.xyz";
    private static final String SOLANA_X_FOLLOWER_TEMPLATE_ID = "07866310-b51d-4ba6-bc10-f6e9272475f8";

    private final Credentials credentials;

    private LocalSignatureService() {
        this.credentials = loadCredentials();
    }

    public static LocalSignatureService getInstance() {
        return Holder.INSTANCE;
    }

    public String sign(String rawRequestBody) {
        try {
            Map<String, Object> attestation = buildPrimusAttestation(rawRequestBody);
            boolean isSolana = isSolanaAddress(stringValue(attestation.get("recipient")));
            String signature = signAttestation(attestation, isSolana);

            List<String> signatures = new ArrayList<>();
            signatures.add(signature);
            attestation.put("signatures", signatures);

            DataSignatureResponse response = new DataSignatureResponse();
            DataSignatureResponse.ResultDTO result = new DataSignatureResponse.ResultDTO();
            result.setSignature(signature);
            result.setRequestid(getJsonData(rawRequestBody, "$.rawParam.requestid"));
            result.setAuthUseridHash(Hash.sha3(getJsonData(rawRequestBody, "$.rawParam.user.userid")));
            result.setEncodedData(JSONUtil.toJsonStr(attestation));
            result.setGetDataTime(getDataGenTime(rawRequestBody));
            response.setResult(result);
            return JSONUtil.toJsonStr(response);
        } catch (UnsupportedOperationException e) {
            return errorResponse(-1102, e.getMessage());
        } catch (Exception e) {
            return errorResponse(-1101, e.getMessage());
        }
    }

    private Map<String, Object> buildPrimusAttestation(String body) {
        String address = getJsonData(body, "$.rawParam.user.address");
        String sourceUserIdHash = firstNonEmpty(
            getJsonData(body, "$.sourceUseridHash"),
            getJsonData(body, "$.rawParam.sourceUseridHash")
        );
        String extendedData = getJsonData(body, "$.extendedData");
        String extRequestsStr = getJsonData(body, "$.rawParam.ext.extRequests");
        String additionParams = getJsonDataDefault(body, "$.rawParam.appParameters.additionParams");
        String modelType = getJsonDataDefault(body, "$.rawParam.modelType");
        String templateId = getJsonDataDefault(body, "$.rawParam.templateId");
        String appSignParameters = getJsonDataDefault(body, "$.rawParam.appParameters.appSignParameters");
        String attTemplateId = getJsonDataDefault(appSignParameters, "$.attTemplateID");

        if (StrUtil.isEmpty(extRequestsStr)) {
            throw new UnsupportedOperationException(ERROR_UNSUPPORTED);
        }

        Map<String, Object> extRequestMap = JSONUtil.toBean(extRequestsStr, new TypeReference<Map<String, Object>>() {}, false);
        Object ordersObj = extRequestMap.remove("orders");
        if (!(ordersObj instanceof JSONArray) && !(ordersObj instanceof List)) {
            throw new UnsupportedOperationException(ERROR_UNSUPPORTED);
        }

        List<Object> orders = ordersObj instanceof JSONArray
            ? ((JSONArray) ordersObj)
            : (List<Object>) ordersObj;
        if (CollectionUtil.isEmpty(orders)) {
            throw new UnsupportedOperationException(ERROR_UNSUPPORTED);
        }

        Map<String, Object> attestation = new LinkedHashMap<>();
        attestation.put("recipient", address);

        List<Map<String, Object>> conditionList = new ArrayList<>();
        Map<String, Object> flatRequestMap = new LinkedHashMap<>();
        Map<String, Object> extendedDataMap = parseMap(extendedData);
        List<Map<String, Object>> responseResolveList = new ArrayList<>();

        int index = 0;
        boolean isSolana = isSolanaAddress(address);
        for (Object order : orders) {
            Map<String, Object> requestConfig = castMap(extRequestMap.get(String.valueOf(order)));
            if (requestConfig == null) {
                throw new UnsupportedOperationException(ERROR_UNSUPPORTED);
            }

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("url", subUrlBefore(stringValue(requestConfig.get("url")), isSolana));
            request.put("method", stringValue(requestConfig.get("method")));
            request.put("body", StrUtil.emptyToDefault(stringValue(requestConfig.get("body")), ""));
            request.put("header", "");

            if (index == 0) {
                if (isSolanaAndXFollower(address, attTemplateId)) {
                    request.put("url", "x.com/i/api/graphql/UserByScreenName?");
                }
                attestation.put("request", request);
            } else {
                String requestKey = "requests[" + index + "].";
                flatRequestMap.put(requestKey + "url", request.get("url"));
                flatRequestMap.put(requestKey + "method", request.get("method"));
                flatRequestMap.put(requestKey + "body", request.get("body"));
                flatRequestMap.put(requestKey + "header", request.get("header"));
            }

            String parseSchemaStr = stringValue(requestConfig.get("parseSchema"));
            Map<String, Object> conditions = castMap(parseMap(parseSchemaStr).get("conditions"));
            List<Map<String, Object>> subconditions = castListOfMap(conditions == null ? null : conditions.get("subconditions"));
            List<Map<String, Object>> currentResolveList = new ArrayList<>();
            int subIndex = 0;
            for (Map<String, Object> subcondition : subconditions) {
                Map<String, Object> resolve = new LinkedHashMap<>();
                resolve.put("keyName", defaultString(stringValue(subcondition.get("reveal_id")), ""));
                resolve.put("parseType", "");
                resolve.put("parsePath", stringValue(subcondition.get("field")));
                currentResolveList.add(resolve);
                responseResolveList.add(resolve);

                if (index > 0) {
                    String responsesKey = "reponseResolves[" + index + "][" + subIndex + "]";
                    flatRequestMap.put(responsesKey + ".keyName", resolve.get("keyName"));
                    flatRequestMap.put(responsesKey + ".parseType", resolve.get("parseType"));
                    flatRequestMap.put(responsesKey + ".parsePath", resolve.get("parsePath"));
                }

                Map<String, Object> conditionMap = new LinkedHashMap<>();
                conditionMap.put("op", stringValue(subcondition.get("op")));
                conditionMap.put("field", stringValue(subcondition.get("field")));
                conditionMap.put("reveal_id", stringValue(subcondition.get("reveal_id")));
                if (StrUtil.isNotEmpty(stringValue(subcondition.get("value")))) {
                    conditionMap.put("value", stringValue(subcondition.get("value")));
                }
                List<Map<String, Object>> nestedSubconditions = castListOfMap(subcondition.get("subconditions"));
                if (CollectionUtil.isNotEmpty(nestedSubconditions)) {
                    for (Map<String, Object> nestedSubcondition : nestedSubconditions) {
                        Map<String, Object> subconditionMap = new LinkedHashMap<>();
                        subconditionMap.put("op", stringValue(nestedSubcondition.get("op")));
                        subconditionMap.put("field", stringValue(nestedSubcondition.get("field")));
                        if (StrUtil.isNotEmpty(stringValue(nestedSubcondition.get("value")))) {
                            subconditionMap.put("value", stringValue(nestedSubcondition.get("value")));
                        }
                        conditionMap.put("subconditions", subconditionMap);
                    }
                }

                if (StrUtil.equals("SHA256", stringValue(subcondition.get("op"))) && StrUtil.isNotEmpty(sourceUserIdHash)) {
                    extendedDataMap.put("SHA256(" + stringValue(subcondition.get("field")) + ")", sourceUserIdHash);
                }
                conditionList.add(conditionMap);
                subIndex++;
            }

            if (index == 0) {
                attestation.put("reponseResolve", isSolanaAndXFollower(address, attTemplateId)
                    ? new ArrayList<>()
                    : currentResolveList);
            }
            index++;
        }

        Map<String, Object> additionMap = parseMap(additionParams);
        if (StrUtil.isNotEmpty(modelType)) {
            additionMap.put("algorithmType", modelType);
        }
        if (index > 1) {
            additionMap.putAll(flatRequestMap);
        }

        attestation.put("data", JSONUtil.toJsonStr(extendedDataMap));
        attestation.put("attConditions", JSONUtil.toJsonStr(conditionList));
        attestation.put("timestamp", System.currentTimeMillis());
        attestation.put("additionParams", JSONUtil.toJsonStr(additionMap));

        List<Map<String, Object>> attestors = new ArrayList<>();
        Map<String, Object> attestor = new LinkedHashMap<>();
        attestor.put("attestorAddr", credentials.getAddress());
        attestor.put("url", PRIMUS_URL);
        attestors.add(attestor);
        attestation.put("attestors", attestors);
        attestation.put("signatures", new ArrayList<>());
        return attestation;
    }

    private List<Map<String, Object>> rewriteSolanaConditions(List<Map<String, Object>> conditionList) {
        List<Map<String, Object>> newConditionList = new ArrayList<>();
        for (Map<String, Object> condition : conditionList) {
            String field = stringValue(condition.get("field"));
            if (StrUtil.equals("$.screen_name", field)) {
                continue;
            }
            Map<String, Object> copied = new LinkedHashMap<>(condition);
            if (StrUtil.equals("STREQ", stringValue(copied.get("op")))) {
                copied.put("op", "E");
            }
            if (copied.containsKey("value")) {
                Object value = copied.remove("value");
                copied.put("vu", value);
            }
            if (StrUtil.equals("$.data.user.result.relationship_perspectives.following", field)) {
                copied.remove("field");
                copied.put("fd", "$.fw");
            }
            if (StrUtil.equals("$.data.user.result.core.screen_name", field)) {
                copied.remove("field");
                copied.put("fd", "$.sn");
            }
            newConditionList.add(copied);
        }
        return newConditionList;
    }


    private void renameKey(
        Map<String, Object> additionMap,
        String sourceKey,
        String targetKey,
        java.util.function.Function<Object, Object> converter
    ) {
        if (!additionMap.containsKey(sourceKey)) {
            return;
        }
        Object value = additionMap.remove(sourceKey);
        additionMap.put(targetKey, converter.apply(value));
    }

    private String signAttestation(Map<String, Object> attestation, boolean isSolana) {
        String attestationHash = encodeAttestation(attestation, isSolana);
        byte[] privateKeyBytes = credentials.getEcKeyPair().getPrivateKey().toByteArray();
        ECKeyPair ecKeyPair = ECKeyPair.create(privateKeyBytes);
        Sign.SignatureData signature =
            Sign.signMessage(Numeric.hexStringToByteArray(attestationHash), Credentials.create(ecKeyPair).getEcKeyPair(), false);
        return signatureToHex(signature);
    }

    private String encodeAttestation(Map<String, Object> attestation, boolean isSolana) {
        List<Type> types = new ArrayList<>();
        if (isSolana) {
            types.add(new Bytes32(Base58.decode(stringValue(attestation.get("recipient")))));
        } else {
            types.add(new Address(stringValue(attestation.get("recipient"))));
        }
        types.add(new Bytes32(Numeric.hexStringToByteArray(encodeRequest(castMap(attestation.get("request"))))));
        types.add(new Bytes32(Numeric.hexStringToByteArray(encodeResponse(castListOfMap(attestation.get("reponseResolve"))))));
        types.add(new Utf8String(stringValue(attestation.get("data"))));
        types.add(new Utf8String(stringValue(attestation.get("attConditions"))));
        types.add(new Uint64(longValue(attestation.get("timestamp"))));
        types.add(new Utf8String(stringValue(attestation.get("additionParams"))));
        return Hash.sha3("0x" + DefaultFunctionEncoder.encodeConstructorPacked(types));
    }

    private String encodeRequest(Map<String, Object> request) {
        List<Type> types = Arrays.asList(
            new Utf8String(stringValue(request.get("url"))),
            new Utf8String(stringValue(request.get("header"))),
            new Utf8String(stringValue(request.get("method"))),
            new Utf8String(stringValue(request.get("body")))
        );
        return Hash.sha3("0x" + DefaultFunctionEncoder.encodeConstructorPacked(types));
    }

    private String encodeResponse(List<Map<String, Object>> resolves) {
        String encodedData = "";
        for (Map<String, Object> resolve : resolves) {
            List<Type> types = new ArrayList<>();
            types.add(new DynamicBytes(Numeric.hexStringToByteArray(encodedData)));
            types.add(new Utf8String(stringValue(resolve.get("keyName"))));
            types.add(new Utf8String(stringValue(resolve.get("parseType"))));
            types.add(new Utf8String(stringValue(resolve.get("parsePath"))));
            StringBuilder nextEncodedData = new StringBuilder();
            for (Type parameter : types) {
                String encodedType = TypeEncoder.encodePacked(parameter);
                byte[] rawBytes = Numeric.hexStringToByteArray(encodedData);
                byte[] encodedBytes = Numeric.hexStringToByteArray(encodedType);
                if (parameter instanceof DynamicBytes) {
                    byte[] trimmed = Arrays.copyOf(encodedBytes, rawBytes.length);
                    nextEncodedData.append(Numeric.toHexString(trimmed));
                } else {
                    nextEncodedData.append(encodedType);
                }
            }
            encodedData = nextEncodedData.toString();
        }
        return Hash.sha3(encodedData);
    }

    private String signatureToHex(Sign.SignatureData signature) {
        byte[] r = signature.getR();
        byte[] s = signature.getS();
        byte[] v = signature.getV();
        byte[] signatureBytes = new byte[r.length + s.length + 1];
        System.arraycopy(r, 0, signatureBytes, 0, r.length);
        System.arraycopy(s, 0, signatureBytes, r.length, s.length);
        signatureBytes[signatureBytes.length - 1] = v[0];
        return Numeric.toHexString(signatureBytes);
    }

    private boolean isSolanaAndXFollower(String address, String templateId) {
        return StrUtil.equals(templateId, SOLANA_X_FOLLOWER_TEMPLATE_ID) && isSolanaAddress(address);
    }

    private boolean isSolanaAddress(String address) {
        return StrUtil.isNotEmpty(address) && !StrUtil.startWith(address, "0x") && isBase58(address);
    }

    private boolean isBase58(String input) {
        try {
            byte[] decoded = Base58.decode(input);
            return input.equals(Base58.encode(decoded));
        } catch (Exception e) {
            return false;
        }
    }

    private String subUrlBefore(String url, boolean isSolana) {
        if (!isSolana || StrUtil.isEmpty(url)) {
            return url;
        }
        int index = url.indexOf("?");
        return index == -1 ? url : url.substring(0, index + 1);
    }

    private Map<String, Object> parseMap(String json) {
        if (StrUtil.isEmpty(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return JSONUtil.toBean(json, new TypeReference<LinkedHashMap<String, Object>>() {}, false);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private List<Map<String, Object>> castListOfMap(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }
        List<?> list = (List<?>) value;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> map = castMap(item);
            if (map != null) {
                result.add(map);
            }
        }
        return result;
    }

    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    private String getDataGenTime(String body) {
        return firstNonEmpty(
            getJsonData(body, "$.rawParam.getdatatime"),
            getJsonData(body, "$.rawParam.getDataTime")
        );
    }

    private String getJsonData(String data, String expression) {
        try {
            Object read = JsonPath.read(data, expression);
            if (read instanceof Map) {
                return JSONUtil.toJsonStr(read);
            }
            return read == null ? null : String.valueOf(read);
        } catch (Exception e) {
            return null;
        }
    }

    private String getJsonDataDefault(String data, String expression) {
        return defaultString(getJsonData(data, expression), "");
    }

    private Credentials loadCredentials() {
        String keystorePath = env("KEYSTORE_PATH", "keystorePath");
        String keystorePass = env("KEYSTORE_PASS", "keystorePass");
        if (StrUtil.isEmpty(keystorePath) || StrUtil.isEmpty(keystorePass)) {
            throw new IllegalStateException("Missing KEYSTORE_PATH/keystorePath or KEYSTORE_PASS/keystorePass");
        }
        try {
            Credentials credentials1 = WalletUtils.loadCredentials(keystorePass, new File(keystorePath));
            log.info("Credentials address is : " + credentials1.getAddress());
            return credentials1;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load credentials: " + e.getMessage(), e);
        }
    }

    private String env(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (StrUtil.isNotEmpty(value)) {
                return value;
            }
            value = System.getProperty(name);
            if (StrUtil.isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (StrUtil.isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultString(String value, String defaultValue) {
        return StrUtil.isEmpty(value) ? defaultValue : value;
    }

    private long longValue(Object value) {
        String stringValue = stringValue(value);
        return StrUtil.isEmpty(stringValue) ? 0L : Long.parseLong(stringValue);
    }

    private String errorResponse(int errorCode, String errorMessage) {
        DataSignatureResponse response = new DataSignatureResponse();
        response.setErrorCode(errorCode);
        response.setErrorMsg(errorMessage);
        return JSONUtil.toJsonStr(response);
    }

    private static final class Holder {
        private static final LocalSignatureService INSTANCE = new LocalSignatureService();
    }

    private static final class VersionComparatorHolder {
        private static boolean isNotPrimus(String version) {
            if (StrUtil.isEmpty(version)) {
                return true;
            }
            return !"1.1.0".equals(version) && !"1.0.1".equals(version) && !"1.0.2".equals(version)
                && !"1.0.3".equals(version) && !"1.0.4".equals(version) && VersionComparatorLexical.compare(version, "1.0.0") <= 0;
        }
    }

    private static final class VersionComparatorLexical {
        private static int compare(String left, String right) {
            String[] leftParts = left.split("\\.");
            String[] rightParts = right.split("\\.");
            int size = Math.max(leftParts.length, rightParts.length);
            for (int i = 0; i < size; i++) {
                int leftValue = i < leftParts.length ? Integer.parseInt(leftParts[i]) : 0;
                int rightValue = i < rightParts.length ? Integer.parseInt(rightParts[i]) : 0;
                if (leftValue != rightValue) {
                    return Integer.compare(leftValue, rightValue);
                }
            }
            return 0;
        }
    }
}
