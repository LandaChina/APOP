package com.unionpay.uas.demo.cbs;

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
 *
 * cbs 直连商户 demo- 订单批次支付结果通知
 *
 订单批次支付结果通知适用于冻结支付场景中，是账户银行主动发起，用于通知商户资金支付的最终结果。
 商户发起“订单批次支付申请”交易后，账户银行先返回应答码为“IS10030000”（批次支付申请已受理并延迟反馈支付结果）的应答报文，账户银行完成资金支付后，还需主动发起“订单批次支付结果通知”(即本交易)告知商户最终的支付结果。

 *
 * 注意：
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
public class DemoOrderParPayResultNotify {

    private static final Logger logger = Logger.getLogger(DemoOrderParPayResultNotify.class);

    public static void gm(){

        GmSDKConfig.loadPropertiesFromSrc();


        // TODO 模拟已解析接出网关发起的HTTP请求头和请求报文
        // 注：在某些特殊场景(如HTTP 2.0)下收到的头字段可能为全小写, 此时需要自行解析时做兼容处理
        Map<String, String> reqHeader = new HashMap<>();
        // 收到的body报文如，具体字段含义见规范
        String respBodyStr = "{\"acqInsCode\":\"CBS12345\",\"txnNo\":\"123456789012345A0023544142\",\"txnDate\":\"20220706\",\"sndTime\":\"102323\",\"origTxnNo\":\"123456789012345A0023544135\",\"origTxnDate\":\"20220706\",\"origPlatformOrderNo\":\"OrderUPSIMNo2022070610000023544136\",\"origBizMethod\":\"pay.b2b.000101\",\"transferApplyTxnNo\":\"123456789012345A0023544139\",\"transferApplyTxnDate\":\"20220706\",\"transferApplyCode\":\"0000000000\",\"transferApplyMsg\":\"请求业务成功\",\"currencyCode\":\"156\",\"transferApplyAmt\":\"1\",\"frozenAvlbBal\":\"100\",\"actReserved\":{\"sampleKeyA\":\"A\",\"sampleKeyB\":\"B\"}}";
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
        verifyMap.put(SDKConstants.param_body, respBodyStr);

        if (!GmUasService.validate(verifyMap)) {
            logger.info("验证银联签名失败\n");
            return;
        } else {
            logger.info("验证银联签名成功\n");
        }

        Map<String, Object> respBody;
        try {
            logger.info("从银联获得HTTP请求报文为：" + respBodyStr + "\n");
            respBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
            /**
             *  根据解析出respBody,取出关心的字段
             */

            if (respBody.containsKey("encryptData")) {
                Map<String, Object> encryptData = (Map<String, Object>) respBody.get(SDKConstants.param_encryptData);
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


        /***********************返回应答示例****************************/

        Map<String, String> resData = new TreeMap<>();

        // 组装HTTP应答报文头
        resData.put(SDKConstants.param_bizMethod, "pay.b2b.000107");       // 交易类型 根据实际业务填写
        resData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        resData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        // resData.put(SDKConstants.param_signMethod, "SM2-CERT");               // 签名方法
        resData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        resData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        resData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        resData.put(SDKConstants.param_body, "");

        Map<String, String> resMap = GmUasService.sign(resData);

        // 最后将数据应答给银联接出网关

    }

    public static void rsa(){

        SDKConfig.loadPropertiesFromSrc();

        // TODO 模拟已解析接出网关发起的HTTP请求头和请求报文
        // 注：在某些特殊场景(如HTTP 2.0)下收到的头字段可能为全小写, 此时需要自行解析时做兼容处理
        Map<String, String> reqHeader = new HashMap<>();
        // 收到的body报文如，具体字段含义见规范
        String respBodyStr = "{\"acqInsCode\":\"CBS12345\",\"txnNo\":\"123456789012345A0023544142\",\"txnDate\":\"20220706\",\"sndTime\":\"102323\",\"origTxnNo\":\"123456789012345A0023544135\",\"origTxnDate\":\"20220706\",\"origPlatformOrderNo\":\"OrderUPSIMNo2022070610000023544136\",\"origBizMethod\":\"pay.b2b.000101\",\"transferApplyTxnNo\":\"123456789012345A0023544139\",\"transferApplyTxnDate\":\"20220706\",\"transferApplyCode\":\"0000000000\",\"transferApplyMsg\":\"请求业务成功\",\"currencyCode\":\"156\",\"transferApplyAmt\":\"1\",\"frozenAvlbBal\":\"100\",\"actReserved\":{\"sampleKeyA\":\"A\",\"sampleKeyB\":\"B\"}}";
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
        verifyMap.put(SDKConstants.param_body, respBodyStr);

        if(!UasService.validate(verifyMap)) {
            logger.info("验证银联签名失败\n");
            return;
        }else {
            logger.info("验证银联签名成功\n");
        }

        Map<String, Object> respBody;
        try {
            logger.info("从银联获得HTTP请求报文为：" + respBodyStr + "\n");
            respBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
            if (respBody.containsKey("encryptData")) {
                Map<String, Object> encryptData = (Map<String, Object>) respBody.get(SDKConstants.param_encryptData);
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



        /***********************返回应答示例****************************/

        Map<String, String> resData = new TreeMap<>();

        // 组装HTTP应答报文头
        resData.put(SDKConstants.param_bizMethod, "pay.b2b.000107");       // 交易类型 根据实际业务填写
        resData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        resData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        // resData.put(SDKConstants.param_signMethod, "SM2-CERT");               // 签名方法
        resData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        resData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        resData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        resData.put(SDKConstants.param_body, "");

        Map<String, String> resMap = GmUasService.sign(resData);

        // 最后将数据应答给银联接出网关

    }

    public static void main(String[] args) {
        // 使用国密方法
        gm();

        // 使用国际方法
        // rsa();
    }


}
