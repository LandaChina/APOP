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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 统一接出网关的示例demo:
 *  1. 接收接出网关发起的请求,解析请求头和请求报文
 *  2. 根据平台规范对请求数据进行验签
 *  3. 若有敏感信息则对敏感密文做解密
 *  4. 若需要返回敏感信息则对敏感明文做加密
 *  5. 根据平台规范对返回数据进行签名
 *  6. 将数据应答给接出网关
 *
 *  签名验签方式有国际和国密两种方式, 推荐使用国密算法
 *  本demo代码需要jdk版本为1.8.0_161及以上版本
 *  以下代码流程仅为示例且无法直接运行, 需要根据实际业务场景进行改造
 */
public class DemoPostBindCard {

    private static final Logger logger = Logger.getLogger(DemoPostBindCard.class);

    public static void gm(){

        GmSDKConfig.loadPropertiesFromSrc();

        // TODO 模拟已解析接出网关发起的HTTP请求头和请求报文
        // 注：在某些特殊场景(如HTTP 2.0)下收到的头字段可能为全小写, 此时需要自行解析时做兼容处理
        Map<String, String> reqHeader = new HashMap<>();
        String reqBodyStr = "";

        // 组装验签报文
        Map<String, String> verifyMap = new HashMap<>();
        verifyMap.put(SDKConstants.param_signId, reqHeader.get(SDKConstants.param_signId));
        verifyMap.put(SDKConstants.param_sign, reqHeader.get(SDKConstants.param_sign));
        verifyMap.put(SDKConstants.param_bizMethod, reqHeader.get(SDKConstants.param_bizMethod));
        verifyMap.put(SDKConstants.param_version, reqHeader.get(SDKConstants.param_version));
        verifyMap.put(SDKConstants.param_signMethod, reqHeader.get(SDKConstants.param_signMethod));
        verifyMap.put(SDKConstants.param_appId, reqHeader.get(SDKConstants.param_appId));
        verifyMap.put(SDKConstants.param_reqId, reqHeader.get(SDKConstants.param_reqId));
        verifyMap.put(SDKConstants.param_signPubKeyCert, reqHeader.get(SDKConstants.param_signPubKeyCert));
        verifyMap.put(SDKConstants.param_body, reqBodyStr);

        if (!GmUasService.validate(verifyMap)) {
            logger.info("验证银联签名失败\n");
            return;
        } else {
            logger.info("验证银联签名成功\n");
        }

        Map<String, Object> repBody = null;
        try {
            logger.info("从银联获得HTTP请求报文为：" + reqBodyStr + "\n");
            repBody = DemoUtil.unwarpJson(new JSONObject(reqBodyStr));
            if (repBody.containsKey("encryptData")) {
                Map<String, Object> encryptData = (Map<String, Object>) repBody.get(SDKConstants.param_encryptData);
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

        // TODO 如果解析成功，获取消息体内业务字段进行业务处理
        if (null != repBody) {
            String origBindTime = (String) repBody.get("origBindTime"); //原交易时间
            String origBindOpr = (String) repBody.get("origBindOpr"); //原绑卡操作类型
            String bindSource = (String) repBody.get("bindSource"); //绑定来源
            // 其他加密字段根据具体结构获取.....
        }

        Map<String, String> resData = new TreeMap<>();

        // 组装HTTP应答报文头
        resData.put(SDKConstants.param_bizMethod, "cusrCqpos.card.postBindCard");       // 交易类型 根据实际业务填写
        resData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        resData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        // resData.put(SDKConstants.param_signMethod, "SM2-CERT");               // 签名方法
        resData.put(SDKConstants.param_appId, "00030000");        // 发送方系统索引号 根据实际业务填写
        resData.put(SDKConstants.param_appType, "01");                   // 01机构、02商户
        resData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP应答报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("code", "0000000000"); //银行实际开发时需要跟通用网关再次确认成功应答码为00还是0000000000
        bodyMap.put("msg", "成功");

//        Map<String, Object> encDataMap = new HashMap<>(); // 需要加密的敏感信息报文 根据实际业务填写
//        encDataMap.put("bodyKey4", "bodyEncValue4");
//        encDataMap.put("bodyKey5", "bodyEncValue5");

//        String toBeEncData = JSONObject.wrap(encDataMap).toString(); // 待加密报文串
//        logger.info("加密前的敏感信息明文: " + toBeEncData);

//        Map<String, String> toBeEncMap = null;
//        try {
            // 随机生成sm4临时公钥16长度
//            String key = DemoUtil.getRandomString(16);
//            logger.info("随机生成的对称密钥key: " + key + "\n");
//            toBeEncMap = GmUasService.gmEnc(toBeEncData, key, "utf-8");
//        } catch (Exception e) {
//            logger.error(e);
//        }

//        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap); // 如果不需要携带敏感信息并由网关解密, 则注释这行代码

        resData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

        Map<String, String> resMap = GmUasService.sign(resData);

        // 最后将数据应答给银联接出网关

    }

    public static void rsa(){

        SDKConfig.loadPropertiesFromSrc();

        // TODO 模拟已解析接出网关发起的HTTP请求头和请求报文
        // 注：在某些特殊场景(如HTTP 2.0)下收到的头字段可能为全小写, 此时需要自行解析时做兼容处理
        Map<String, String> reqHeader = new HashMap<>();
        String reqBodyStr = "";

        // 组装验签报文
        Map<String, String> verifyMap = new HashMap<>();
        verifyMap.put(SDKConstants.param_signId, reqHeader.get(SDKConstants.param_signId));
        verifyMap.put(SDKConstants.param_sign, reqHeader.get(SDKConstants.param_sign));
        verifyMap.put(SDKConstants.param_bizMethod, reqHeader.get(SDKConstants.param_bizMethod));
        verifyMap.put(SDKConstants.param_version, reqHeader.get(SDKConstants.param_version));
        verifyMap.put(SDKConstants.param_signMethod, reqHeader.get(SDKConstants.param_signMethod));
        verifyMap.put(SDKConstants.param_appId, reqHeader.get(SDKConstants.param_appId));
        verifyMap.put(SDKConstants.param_reqId, reqHeader.get(SDKConstants.param_reqId));
        verifyMap.put(SDKConstants.param_signPubKeyCert, reqHeader.get(SDKConstants.param_signPubKeyCert));
        verifyMap.put(SDKConstants.param_body, reqBodyStr);

        if(!UasService.validate(verifyMap)) {
            logger.info("验证银联签名失败\n");
            return;
        }else {
            logger.info("验证银联签名成功\n");
        }

        Map<String, Object> repBody = null;
        try {
            logger.info("从银联获得HTTP请求报文为：" + reqBodyStr + "\n");
            repBody = DemoUtil.unwarpJson(new JSONObject(reqBodyStr));
            if (repBody.containsKey("encryptData")) {
                Map<String, Object> encryptData = (Map<String, Object>) repBody.get(SDKConstants.param_encryptData);
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
            logger.info("解析JSON失败");
        }

        // TODO 如果解析成功，获取消息体内业务字段进行业务处理
        if (null != repBody) {
            String origBindTime = (String) repBody.get("origBindTime"); //原交易时间
            String origBindOpr = (String) repBody.get("origBindOpr"); //原绑卡操作类型
            String bindSource = (String) repBody.get("bindSource"); //绑定来源
            // 其他加密字段根据具体结构获取.....
        }

        Map<String, String> resData = new TreeMap<>();

        // 组装http请求报文头
        resData.put(SDKConstants.param_bizMethod, "cusrCqpos.card.postBindCard");       // 交易类型 根据实际业务填写
        resData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        resData.put(SDKConstants.param_signMethod, "RSA2");              // 签名方法
        // resData.put(SDKConstants.param_signMethod, "RSA2-CERT");              // 签名方法
        resData.put(SDKConstants.param_appId, "00020000");               // 发送方系统索引号 根据实际业务填写
        resData.put(SDKConstants.param_appType, "01");                   // 01机构、02商户
        resData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装http请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("code", "0000000000"); //银行实际开发时需要跟通用网关再次确认成功应答码为00还是0000000000
        bodyMap.put("msg", "成功");

//        Map<String, Object> encDataMap = new HashMap<>(); // 需要加密的敏感信息报文 根据实际业务填写
//        encDataMap.put("bodyKey4", "bodyEncValue4");
//        encDataMap.put("bodyKey5", "bodyEncValue5");
//
//        String toBeEncData = JSONObject.wrap(encDataMap).toString(); // 待加密报文串
//        logger.info("加密前的敏感信息明文:" + toBeEncData);
//
//        Map<String, String> toBeEncMap = null;
//        try {
//            // 随机生成临时公钥32长度
//            String key = DemoUtil.getRandomString(32);
//            logger.info("随机生成的对称密钥key: " + key + "\n");
//            toBeEncMap = UasService.rsaEnc(toBeEncData, key, "utf-8");
//        } catch (Exception e) {
//            logger.error(e);
//        }
//
//        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap); // 如果不需要携带敏感信息并由网关解密, 则注释这行代码

        resData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

        Map<String, String> resMap = UasService.sign(resData);

        // 最后将数据应答给银联接出网关
    }

    public static void main(String[] args) {
        // 使用国密方法
        gm();

        // 使用国际方法
//        rsa();
    }
}
