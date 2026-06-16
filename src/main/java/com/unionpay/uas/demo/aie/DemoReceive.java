package com.unionpay.uas.demo.aie;

import com.unionpay.uas.demo.ucp.uniontax.DemoBillPay;
import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConfig;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.UasService;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUasService;
import java.util.*;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>AIE系统的接收示例demo，模拟银联->机构:</p>
 * <pre>
 *  1. 接收银联发起的请求,解析请求头和请求报文
 *  2. 根据平台规范对请求数据进行验签
 *  3. 若有敏感信息则对敏感密文做解密
 *  4. 若需要返回敏感信息则对敏感明文做加密
 *  5. 根据平台规范对返回数据进行签名
 *  6. 将数据应答给银联
 *  </pre>
 *  <p>签名验签方式有国际和国密两种方式 推荐使用国密算法</p>
 *  <p>本demo代码需要jdk版本为1.8.0_161及以上版本</p>
 *
 *  <p><b>声明</b>：以下代码只是为了方便商户测试而提供的样例代码，机构可以根据自己需要，按照技术文档编写。该代码仅供参考，不提供编码，性能，规范性等方面的保障</p>
 *
 * @author chenxing2
 */
public class DemoReceive {

    private static final Logger logger = Logger.getLogger(DemoBillPay.class);

    public static void gm(){

        GmSDKConfig.loadPropertiesFromSrc();


        // TODO 模拟已解析接出AIE发起的HTTP请求头和请求报文
        // 注：在某些特殊场景(如HTTP 2.0)下收到的头字段可能为全小写, 此时需要自行解析时做兼容处理
        Map<String, String> reqHeader = new HashMap<>();
        String reqBodyStr = "";

        // 组装验签报文
        Map<String, String> verifyMap = new HashMap<>();
        verifyMap.put(SDKConstants.param_signId, reqHeader.get(SDKConstants.param_signId));
        //Note: 由于历史原因，签名字段名为“signature”，而不是sign。但验签时该sdk底层封装取的是sign
        verifyMap.put(SDKConstants.param_sign, reqHeader.get(SDKConstants.param_signature));
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

        Map<String, Object> repBody;
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



        Map<String, String> resData = new TreeMap<>();

        // 组装HTTP应答报文头
        resData.put(SDKConstants.param_bizMethod, "uas.demo.biz");       // 交易类型 根据实际业务填写
        resData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        resData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        // resData.put(SDKConstants.param_signMethod, "SM2-CERT");               // 签名方法
        resData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        resData.put(SDKConstants.param_appType, "01");                   // 01机构、02商户
        resData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP应答报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("bodyKey1", "bodyValue1");
        bodyMap.put("bodyKey2", "bodyValue2");
        bodyMap.put("bodyKey3", "bodyValue3");

        Map<String, Object> encDataMap = new HashMap<>(); // 需要加密的敏感信息报文 根据实际业务填写
        encDataMap.put("bodyKey4", "bodyEncValue4");
        encDataMap.put("bodyKey5", "bodyEncValue5");

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

        // 如果不需要携带敏感信息并由网关解密, 则注释这行代码
        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap);

        resData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

        Map<String, String> resMap = GmUasService.sign(resData);
        //Note: 由于历史原因，AIE签名字段名为“signature”，而不是sign。
        resMap.put("signature", resMap.remove("sign"));

        // 最后将数据同步应答给AIE

    }

    public static void rsa(){

        SDKConfig.loadPropertiesFromSrc();

        // TODO 模拟已解析接出AIE发起的HTTP请求头和请求报文
        Map<String, String> reqHeader = new HashMap<>();
        String reqBodyStr = "";

        // 组装验签报文
        Map<String, String> verifyMap = new HashMap<>();
        verifyMap.put(SDKConstants.param_signId, reqHeader.get(SDKConstants.param_signId));
        //Note: 由于历史原因，签名字段名为“signature”，而不是sign。但验签时该sdk底层封装取的是sign
        verifyMap.put(SDKConstants.param_sign, reqHeader.get(SDKConstants.param_signature));
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

        Map<String, Object> repBody;
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



        Map<String, String> resData = new TreeMap<>();

        // 组装http请求报文头
        resData.put(SDKConstants.param_bizMethod, "uas.demo.biz");       // 交易类型 根据实际业务填写
        resData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        // resData.put(SDKConstants.param_signMethod, "RSA2");           // 签名方法
        resData.put(SDKConstants.param_signMethod, "RSA2-CERT");         // 签名方法
        resData.put(SDKConstants.param_appId, "00010000");               // 发送方系统索引号 根据实际业务填写
        resData.put(SDKConstants.param_appType, "01");                   // 01机构、02商户
        resData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装http请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("bodyKey1", "bodyValue1");
        bodyMap.put("bodyKey2", "bodyValue2");
        bodyMap.put("bodyKey3", "bodyValue3");

        Map<String, Object> encDataMap = new HashMap<>(); // 需要加密的敏感信息报文 根据实际业务填写
        encDataMap.put("bodyKey4", "bodyEncValue4");
        encDataMap.put("bodyKey5", "bodyEncValue5");

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

        // 如果不需要携带敏感信息并由网关解密, 则注释这行代码
        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap);

        resData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

        Map<String, String> resMap = UasService.sign(resData);
        //Note: 由于历史原因，AIE签名字段名为“signature”，而不是sign。
        resMap.put("signature", resMap.remove("sign"));

        // 最后将数据同步应答给AIE
    }

    public static void main(String[] args) {
        // 使用国密方法
        gm();

        // 使用国际方法
        // rsa();
    }
}
