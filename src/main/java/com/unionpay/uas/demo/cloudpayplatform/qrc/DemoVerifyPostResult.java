package com.unionpay.uas.demo.cloudpayplatform.qrc;

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
 * 云闪付网络支付平台-交易验证结果通知接口
 */
public class DemoVerifyPostResult {

    private static final Logger logger = Logger.getLogger(DemoVerifyPostResult.class);

    public static void main(String[] args) {
        // 使用国密算法调用此方法
        //GmDemo();

        // 使用国际算法调用此方法
        RsaDemo();
    }

    public static void GmDemo() {

        // 初始化配置文件，读取证书
        GmSDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_version, "1.0.0");                // 根据前序“交易验证策略查询”交易中的“二维码交易场景（qrScene）”字段取值不同而定：如qrScene = 03，取值：7.1.0其余情况下，取值：1.0.0
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_bizMethod, "upntQrc.verify.postResult");       
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();

        bodyMap.put("verifyOrderNo", "1234545");//验证流水号
        bodyMap.put("verifyResult", "");//验证结果
        bodyMap.put("verifyMethod", "");//验证方式
        bodyMap.put("verifyTime", "");//验证时间
        bodyMap.put("issVerifyId", "345465768");//付款方验证标识

        Map<String, String> riskInfoMap = new HashMap<>();
        //标识 APP 机构的风险防范能力等级，银联将该字段透传给发卡行，用于发卡行控制交易限额。
        //取值：
        //A - 采用包括数字证书或电子签名在内的两类（含）以上有效要素进行验证
        //B - 采用不包括数字证书、电子签名在内的两类（含）以上有效要素进行验证
        //C - 采用不足两类有效要素进行验证。
        riskInfoMap.put("riskLevel", "");//风险防范能力等级
        bodyMap.put("riskInfo", JSONObject.wrap(riskInfoMap));//风控信息
        bodyMap.put("reqReserved", "abcd");//请求保留域,整体进行Base64编码,将交易验证策略查询接口中该字段原样返回

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

    public static void RsaDemo() {

        // 初始化配置文件，读取证书
        SDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_version, "1.0.0");                // 根据前序“交易验证策略查询”交易中的“二维码交易场景（qrScene）”字段取值不同而定：如qrScene = 03，取值：7.1.0其余情况下，取值：1.0.0
        reqData.put(SDKConstants.param_appType, "01");                   // 01机构、02商户
        reqData.put(SDKConstants.param_appId, "00020000");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_bizMethod, "upntQrc.verify.postResult");       //根据前序“交易验证策略查询”交易中的“二维码交易场景（qrScene）”字段取值不同而定：如qrScene = 03，取值：upntAcp.verify.postResult;其余情况下，取值：upntQrc.verify.postResult
        reqData.put(SDKConstants.param_signMethod, "RSA2");               // 签名方法
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();

        bodyMap.put("verifyOrderNo", "1234545");//验证流水号
        bodyMap.put("verifyResult", "01");//验证结果
        bodyMap.put("verifyMethod", "02");//验证方式
        bodyMap.put("verifyTime", "20220901");//验证时间
        bodyMap.put("issVerifyId", "345465768");//付款方验证标识
        Map<String, String> riskInfoMap = new HashMap<>();
        //标识 APP 机构的风险防范能力等级，银联将该字段透传给发卡行，用于发卡行控制交易限额。
        //取值：
        //A - 采用包括数字证书或电子签名在内的两类（含）以上有效要素进行验证
        //B - 采用不包括数字证书、电子签名在内的两类（含）以上有效要素进行验证
        //C - 采用不足两类有效要素进行验证。
        riskInfoMap.put("riskLevel", "A");//风险防范能力等级
        bodyMap.put("riskInfo", JSONObject.wrap(riskInfoMap));//风控信息
        bodyMap.put("reqReserved", "abcd");//请求保留域,整体进行Base64编码,将交易验证策略查询接口中该字段原样返回

        reqData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

        Map<String, String> reqMap = UasService.sign(reqData);

        // 发送请求报文并接受同步应答
        // 这里调用签名方法sign之后 调用post之前不能对map中的键值对做任何修改 如果修改会导致验签不通过
        Map<String, Object> rspMap = UasService.post(reqMap, SDKConfig.getConfig().getTransUrl());

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

        if (!UasService.validate(verifyMap)) {
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
}
