package com.unionpay.uas.demo.cloudpayplatform.ysf;

import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConfig;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.UasService;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUasService;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * 统一接入网关的接入示例demo:
 *  1. 根据平台规范组装请求报文头
 *  2. 根据实际业务组装请求报文体
 *  3. 若有敏感信息则对敏感明文做加密
 *  4. 根据平台规范对请求数据进行签名
 *  5. 向统一接入网关发起POST请求
 *  6. 对应答报文进行验签
 *  7. 若有敏感信息则对敏感密文做解密
 *
 *  签名验签方式有国际和国密两种方式 推荐使用国密算法
 *
 *  本demo代码需要jdk版本为1.8.0_161及以上版本
 */
public class DemoBindUser {

    private static final Logger logger = Logger.getLogger(DemoBindUser.class);

    public static void gm(){

        GmSDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "cusrCqpos.user.bindUser");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        // reqData.put(SDKConstants.param_signMethod, "SM2-CERT");               // 签名方法
        reqData.put(SDKConstants.param_appId, "00030000");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "01");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("cardAuthNo", "9999");

        Map<String, Object> encDataMap = new HashMap<>(); // 需要加密的敏感信息报文 根据实际业务填写
        // 用户信息
        Map<String, Object> userIdInfo = new HashMap<>();
        userIdInfo.put("cntryPhoneCd", "86");
        userIdInfo.put("mobile", "18581000666");
        userIdInfo.put("realNm", "大军");
        userIdInfo.put("certType", "01");
        userIdInfo.put("certifId", "110101199001080099");
        //卡信息
        Map<String, Object> userAccInfo = new HashMap<>();
        userAccInfo.put("cardno", "622114848254898609");
        userAccInfo.put("accType", "01");
        userAccInfo.put("presetMobile", "18581000666");
        List<Map> userAccInfoList = new ArrayList<>();
        userAccInfoList.add(userAccInfo);
        // 待加密字段
//        encDataMap.put("indUsrId", "y00001");
        encDataMap.put("indUsrId", System.currentTimeMillis()); //银行userId，据实填写，此处为了避免重复运行时报错，用时间戳代替随机数
        encDataMap.put("userIdInfo", userIdInfo);
        encDataMap.put("userAccInfo", userAccInfoList);
        encDataMap.put("areaCode", "0000");
        encDataMap.put("channelType", "h");

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

        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap); // 如果不需要携带敏感信息并由网关解密, 则注释这行代码

        reqData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

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
        reqData.put(SDKConstants.param_bizMethod, "cusrCqpos.user.bindUser");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");              // 签名方法
        // reqData.put(SDKConstants.param_signMethod, "RSA2-CERT");              // 签名方法
        reqData.put(SDKConstants.param_appId, "00020000");               // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "01");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("cardAuthNo", "9999");

        Map<String, Object> encDataMap = new HashMap<>(); // 需要加密的敏感信息报文 根据实际业务填写
        // 用户信息
        Map<String, Object> userIdInfo = new HashMap<>();
        userIdInfo.put("cntryPhoneCd", "86");
        userIdInfo.put("mobile", "18581000666");
        userIdInfo.put("realNm", "大军");
        userIdInfo.put("certType", "01");
        userIdInfo.put("certifId", "110101199001080099");
        //卡信息
        Map<String, Object> userAccInfo = new HashMap<>();
        userAccInfo.put("cardno", "622114848254898609");
        userAccInfo.put("accType", "01");
        userAccInfo.put("presetMobile", "18581000666");
        List<Map> userAccInfoList = new ArrayList<>();
        userAccInfoList.add(userAccInfo);
        // 待加密字段
//        encDataMap.put("indUsrId", "y00001");
        encDataMap.put("indUsrId", System.currentTimeMillis());//银行userId，据实填写，此处为了避免重复运行时报错，用时间戳代替随机数
        encDataMap.put("userIdInfo", userIdInfo);
        encDataMap.put("userAccInfo", userAccInfoList);
        encDataMap.put("areaCode", "0000");
        encDataMap.put("channelType", "h");

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

        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap); // 如果不需要携带敏感信息并由网关解密, 则注释这行代码

        reqData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

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
//        rsa();
    }

}
