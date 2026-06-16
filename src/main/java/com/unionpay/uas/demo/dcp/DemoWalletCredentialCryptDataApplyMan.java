package com.unionpay.uas.demo.dcp;

import com.unionpay.uas.demo.dcp.vo.*;
import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConfig;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.UasService;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUasService;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author lxjiang
 * @date 2023/5/8
 */
public class DemoWalletCredentialCryptDataApplyMan {

    private static final Logger logger = Logger.getLogger(DemoWalletCredentialCryptDataApplyMan.class);

    public static void gm() {

        GmSDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "dcp.wallet.getCrdl.applyCrdlCryptDataMan");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        // reqData.put(SDKConstants.param_signMethod, "SM2-CERT");               // 签名方法
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        JSONObject body = new JSONObject();
        body.put("cardWalletId", "00088880001");
        body.put("carrierId", "SU72AIDJ21394281347KHFABFVAI72KJDSAFHKADSFGO1");
        body.put("bankInsIdCd", "07000000011");
        body.put("transTm", "2023-05-05 09:34:54");
        body.put("transNo", "010200002023050509345412088806");

        AcctInfo acctInfo = new AcctInfo();
        acctInfo.setPriAcctNo("62352467868888");
        acctInfo.setCardTp("01");
        acctInfo.setContactNm("胡匪");
        acctInfo.setContactNmHash("f92544f866f42a27daffeb42008f7d93d16a1575289081abf0fa2ff8dce2964f");
        acctInfo.setContactMobileNo("86-15952923386");
        acctInfo.setContactMobileNoMask("86-159****3386");
        acctInfo.setCertifTp("01");
        acctInfo.setCertifId("32118320000117483209");
        acctInfo.setAcctUserId("f9asidf929");
        acctInfo.setAcctFaceId("facId12345678");
        JSONObject acctInfoJson = new JSONObject(acctInfo);
        String acctInfoStr = acctInfoJson.toString();

        UserInfo userInfo = new UserInfo();
        userInfo.setUserContactNm("端木碶子");
        userInfo.setUserContactMobileNo("86-15323239407");
        userInfo.setUserCertifTp("01");
        userInfo.setUserCertifId("332123199902541152");
        userInfo.setUserId("f9asidf929");
        JSONObject userInfoJson = new JSONObject(userInfo);
        String userInfoStr = userInfoJson.toString();

        DeviceEnvInfo deviceEnvInfo = new DeviceEnvInfo();
        deviceEnvInfo.setDevTp("01");
        deviceEnvInfo.setDevId("ahjkdsfhgasoaisdfhj");
        deviceEnvInfo.setDevLoc("+37.123456/-121.234567");
        deviceEnvInfo.setDevModel("SE525");
        deviceEnvInfo.setSimMobileNo("17625251212");
        deviceEnvInfo.setSimNo("11");
        deviceEnvInfo.setSourceIpAddr("127.0.0.1");
        deviceEnvInfo.setMacAddr("QB8F6ED44EAW");
        JSONObject deviceEnvInfoJson = new JSONObject(deviceEnvInfo);
        String deviceInfo = deviceEnvInfoJson.toString();
        body.put("deviceEnvInfo", new JSONObject(deviceInfo));

        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setTransLmt("000000005000");
        tokenInfo.setTransLmtCurrCd("156");
        tokenInfo.setTotalDayLmt("000000100000");
        tokenInfo.setUseNo("100");
        tokenInfo.setTotalMonLmt("000010000000");
        tokenInfo.setChnlBit("11111111");
        tokenInfo.setMchtRange("1");
        tokenInfo.setMchntCd("000111010202010");
        tokenInfo.setTokenExpiry("2502");
        tokenInfo.setCntryCd("156");
        JSONObject tokenInfoJson = new JSONObject(tokenInfo);
        String tokeninfo = tokenInfoJson.toString();
        body.put("tokenInfo", new JSONObject(tokeninfo));

        RiskInfo riskInfo = new RiskInfo();
        riskInfo.setRiskScore("5");
        riskInfo.setRiskStandardVersion("1.1");
        riskInfo.setDeviceScore("5");
        riskInfo.setAccountScore("5");
        riskInfo.setPhoneNumberScore("5");
        riskInfo.setRiskReasonCode("02,03,04,05,06");
        riskInfo.setApplyChannel("1");
        riskInfo.setCaptureMethod("1");
        riskInfo.setGoodsTp("00");
        JSONObject riskInfoJson = new JSONObject(riskInfo);
        String riskInfoStr = riskInfoJson.toString();
        body.put("riskInfo", new JSONObject(riskInfoStr));

        JSONObject subBody = new JSONObject();
        subBody.put("reqReserved-1","reqReserved-1");
        body.put("reqReserved", subBody);

        Map<String, Object> encDataMap = new HashMap<>(); // 需要加密的敏感信息报文 根据实际业务填写
        encDataMap.put("userInfo", new JSONObject(userInfoStr));
        encDataMap.put("acctInfo", new JSONObject(acctInfoStr));

        String toBeEncData = JSONObject.wrap(encDataMap).toString(); // 待加密报文串
        logger.info("加密前的敏感信息明文: " + toBeEncData);


        Map<String, String> toBeEncMap = null;
        try {
            // 随机生成sm4临时公钥16长度
            String key = DemoUtil.getRandomString(16);
            logger.info("随机生成的对称密钥key: " + key + "\n");
            toBeEncMap = GmUasService.gmEnc(toBeEncData, key, "utf-8");
        } catch (Exception e) {
            logger.error(e);
        }

        body.put(SDKConstants.param_encryptData, toBeEncMap);   // 如果不需要携带敏感信息并由网关解密, 则注释这行代码
        reqData.put(SDKConstants.param_body, body.toString());

        Map<String, String> reqMap = GmUasService.sign(reqData);

        // 发送请求报文并接受同步应答
        // 这里调用签名方法sign之后 调用post之前不能对map中的键值对做任何修改 如果修改会导致验签不通过
        Map<String, Object> rspMap = UasService.post(reqMap, GmSDKConfig.getConfig().getTransUrl());

        if (rspMap == null || rspMap.isEmpty()) {
            logger.info("POST请求服务端失败");
            return;
        }

        Map<String, String> respHeader = (Map<String, String>) rspMap.get(SDKConstants.param_header);
        String respBodyStr = (String) rspMap.get(SDKConstants.param_body);

        // 组装验签报文
        // 注：银联网关返回的应答头字段是驼峰式的 但在某些特殊场景(如HTTP 2.0)下头字段会转为小写; 此处为不区分大小写 在解析应答头时将所有字段名统一转换为小写;
        // 注：调用请求方在使用其他HTTP工具库时需要自行注意兼容头字段大小写问题
        Map<String, String> verifyMap = new HashMap<>();
        verifyMap.put(SDKConstants.param_signId, respHeader.get("signid"));
        verifyMap.put(SDKConstants.param_sign, respHeader.get("sign"));
        verifyMap.put(SDKConstants.param_bizMethod, respHeader.get("bizmethod"));
        verifyMap.put(SDKConstants.param_version, respHeader.get("version"));
        verifyMap.put(SDKConstants.param_signMethod, respHeader.get("signmethod"));
        verifyMap.put(SDKConstants.param_appId, respHeader.get("appid"));
        verifyMap.put(SDKConstants.param_reqId, respHeader.get("reqid"));
        verifyMap.put(SDKConstants.param_signPubKeyCert, respHeader.get("signpubkeycert"));
        verifyMap.put(SDKConstants.param_body, respBodyStr);

        if (!GmUasService.validate(verifyMap)) {
            logger.info("验证银联签名失败\n");
            return;
        } else {
            logger.info("验证银联签名成功\n");
        }

        Map<String, Object> rspBody;
        try {
            logger.info("从银联获得HTTP应答报文为：" + respBodyStr + "\n");
            rspBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
            if (rspBody.containsKey("encryptData")) {
                Map<String, Object> encryptData = (Map<String, Object>) rspBody.get(SDKConstants.param_encryptData);
                String decKey = (String) encryptData.get(SDKConstants.param_key);
                String decData = (String) encryptData.get(SDKConstants.param_data);
                String certId = (String) encryptData.get(SDKConstants.param_certId);
                try {
                    String decodeStr = GmUasService.gmDec(decData, decKey, certId);
                    logger.info("解银联加密后的敏感信息的报文：" + decodeStr);
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        } catch (JSONException e) {
            logger.info("应答体解json失败。");
        }
    }

    public static void rsa(){
        SDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装http请求报文头
        reqData.put(SDKConstants.param_bizMethod, "dcp.wallet.getCrdl.applyCrdlCryptDataMan");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");              // 签名方法
        // reqData.put(SDKConstants.param_signMethod, "RSA2-CERT");              // 签名方法
        reqData.put(SDKConstants.param_appId, "00010000");               // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        JSONObject body = new JSONObject();
        body.put("cardWalletId", "00088880001");
        body.put("carrierId", "SU72AIDJ21394281347KHFABFVAI72KJDSAFHKADSFGO1");
        body.put("bankInsIdCd", "07000000011");
        body.put("transTm", "2023-05-05 09:34:54");
        body.put("transNo", "010200002023050509345412088806");

        AcctInfo acctInfo = new AcctInfo();
        acctInfo.setPriAcctNo("62352467868888");
        acctInfo.setCardTp("01");
        acctInfo.setContactNm("胡匪");
        acctInfo.setContactNmHash("f92544f866f42a27daffeb42008f7d93d16a1575289081abf0fa2ff8dce2964f");
        acctInfo.setContactMobileNo("86-15952923386");
        acctInfo.setContactMobileNoMask("86-159****3386");
        acctInfo.setCertifTp("01");
        acctInfo.setCertifId("32118320000117483209");
        acctInfo.setAcctUserId("f9asidf929");
        acctInfo.setAcctFaceId("facId12345678");
        JSONObject acctInfoJson = new JSONObject(acctInfo);
        String acctInfoStr = acctInfoJson.toString();

        UserInfo userInfo = new UserInfo();
        userInfo.setUserContactNm("端木碶子");
        userInfo.setUserContactMobileNo("86-15323239407");
        userInfo.setUserCertifTp("01");
        userInfo.setUserCertifId("332123199902541152");
        userInfo.setUserId("f9asidf929");
        JSONObject userInfoJson = new JSONObject(userInfo);
        String userInfoStr = userInfoJson.toString();

        DeviceEnvInfo deviceEnvInfo = new DeviceEnvInfo();
        deviceEnvInfo.setDevTp("01");
        deviceEnvInfo.setDevId("ahjkdsfhgasoaisdfhj");
        deviceEnvInfo.setDevLoc("+37.123456/-121.234567");
        deviceEnvInfo.setDevModel("SE525");
        deviceEnvInfo.setSimMobileNo("17625251212");
        deviceEnvInfo.setSimNo("11");
        deviceEnvInfo.setSourceIpAddr("127.0.0.1");
        deviceEnvInfo.setMacAddr("QB8F6ED44EAW");
        JSONObject deviceEnvInfoJson = new JSONObject(deviceEnvInfo);
        String deviceInfo = deviceEnvInfoJson.toString();
        body.put("deviceEnvInfo", new JSONObject(deviceInfo));

        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setTransLmt("000000005000");
        tokenInfo.setTransLmtCurrCd("156");
        tokenInfo.setTotalDayLmt("000000100000");
        tokenInfo.setUseNo("100");
        tokenInfo.setTotalMonLmt("000010000000");
        tokenInfo.setChnlBit("11111111");
        tokenInfo.setMchtRange("1");
        tokenInfo.setMchntCd("000111010202010");
        tokenInfo.setTokenExpiry("2502");
        tokenInfo.setCntryCd("156");
        JSONObject tokenInfoJson = new JSONObject(tokenInfo);
        String tokeninfo = tokenInfoJson.toString();
        body.put("tokenInfo", new JSONObject(tokeninfo));

        RiskInfo riskInfo = new RiskInfo();
        riskInfo.setRiskScore("5");
        riskInfo.setRiskStandardVersion("1.1");
        riskInfo.setDeviceScore("5");
        riskInfo.setAccountScore("5");
        riskInfo.setPhoneNumberScore("5");
        riskInfo.setRiskReasonCode("02,03,04,05,06");
        riskInfo.setApplyChannel("1");
        riskInfo.setCaptureMethod("1");
        riskInfo.setGoodsTp("00");
        JSONObject riskInfoJson = new JSONObject(riskInfo);
        String riskInfoStr = riskInfoJson.toString();
        body.put("riskInfo", new JSONObject(riskInfoStr));

        JSONObject subBody = new JSONObject();
        subBody.put("reqReserved-1","reqReserved-1");
        body.put("reqReserved", subBody);

        Map<String, Object> encDataMap = new HashMap<>(); // 需要加密的敏感信息报文 根据实际业务填写
        encDataMap.put("userInfo", new JSONObject(userInfoStr));
        encDataMap.put("acctInfo", new JSONObject(acctInfoStr));

        String toBeEncData = JSONObject.wrap(encDataMap).toString(); // 待加密报文串
        logger.info("加密前的敏感信息明文:" + toBeEncData);

        Map<String, String> toBeEncMap = null;
        try {
            // 随机生成临时公钥32长度
            String key = DemoUtil.getRandomString(32);
            logger.info("随机生成的对称密钥key: " + key + "\n");
            toBeEncMap = UasService.rsaEnc(toBeEncData, key, "utf-8");
        } catch (Exception e) {
            logger.error(e);
        }

        body.put(SDKConstants.param_encryptData, toBeEncMap); // 如果不需要携带敏感信息并由网关解密, 则注释这行代码
        reqData.put(SDKConstants.param_body, body.toString());

        Map<String, String> reqMap = UasService.sign(reqData);

        // 发送请求报文并接受同步应答
        // 这里调用签名方法sign之后 调用post之前不能对map中的键值对做任何修改 如果修改会导致验签不通过
        Map<String, Object> rspMap = UasService.post(reqMap, SDKConfig.getConfig().getTransUrl());

        if(rspMap == null || rspMap.size() == 0) {
            logger.info("POST请求服务端失败");
            return;
        }

        Map<String, String> respHeader = (Map<String, String>)rspMap.get(SDKConstants.param_header);
        String respBodyStr = (String)rspMap.get(SDKConstants.param_body);

        // 组装验签报文
        // 注：银联网关返回的应答头字段是驼峰式的 但在某些特殊场景(如HTTP 2.0)下头字段会转为小写; 此处为不区分大小写 在解析应答头时将所有字段名统一转换为小写;
        // 注：调用请求方在使用其他HTTP工具库时需要自行注意兼容头字段大小写问题
        Map<String, String> verifyMap = new HashMap<>();
        verifyMap.put(SDKConstants.param_signId, respHeader.get("signid"));
        verifyMap.put(SDKConstants.param_sign, respHeader.get("sign"));
        verifyMap.put(SDKConstants.param_bizMethod, respHeader.get("bizmethod"));
        verifyMap.put(SDKConstants.param_version, respHeader.get("version"));
        verifyMap.put(SDKConstants.param_signMethod, respHeader.get("signmethod"));
        verifyMap.put(SDKConstants.param_appId, respHeader.get("appid"));
        verifyMap.put(SDKConstants.param_reqId, respHeader.get("reqid"));
        verifyMap.put(SDKConstants.param_signPubKeyCert, respHeader.get("signpubkeycert"));
        verifyMap.put(SDKConstants.param_body, respBodyStr);

        if(!UasService.validate(verifyMap)) {
            logger.info("验证银联签名失败\n");
            return;
        }else {
            logger.info("验证银联签名成功\n");
        }

        Map<String, Object> rspBody;
        try {
            logger.info("从银联获得HTTP应答报文为：" + respBodyStr + "\n");
            rspBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
            if (rspBody.containsKey("encryptData")) {
                Map<String, Object> encryptData = (Map<String, Object>) rspBody.get(SDKConstants.param_encryptData);
                String decKey = (String) encryptData.get(SDKConstants.param_key);
                String decData = (String) encryptData.get(SDKConstants.param_data);
                String certId = (String) encryptData.get(SDKConstants.param_certId);
                try {
                    String decodeStr = UasService.rsaDec(decData, decKey, certId);
                    logger.info("解银联加密后的敏感信息的报文：" + decodeStr);
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        } catch (JSONException e) {
            logger.info("应答体解json失败。");
        }
    }

    public static void main(String[] args) {
        // 使用国密方法
        gm();

        // 使用国际方法
        //rsa();
    }
}
