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
 * cbs 直连商户 demo- 订单交易
 *
 *  订单结果通知交易用于向商户通知订单状态。根据订单的付款方式与状态变化，订单结果通知交易的发起可能存在以下几种场景：
 * 1.账户机构完成付款且付款成功后，账户机构主动发起订单结果通知，告知商户订单已付款。（账户机构主动发起）
 * 2.账户机构检测到订单已过期，账户机构主动发起订单结果通知，告知商户订单已过期。（账户机构主动发起）
 * 3.账户机构检测到异常（包括不限于制单复核异常、付款失败且拒绝再次发起付款等）无法完成后续订单处理时，账户机构主动发起订单结果通知，告知商户订单失败。（账户机构主动发起）
 * 4.付款方在审核页面主动拒绝支付/授权，账户机构应主动发起订单结果通知，告知商户订单已拒绝支付。（账户机构主动发起）
 * 5.冻结模式下，账户机构完成资金冻结后，应主动发起订单结果通知，告知商户订单已冻结。（账户机构主动发起）
 *
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
public class DemoOrderResultNotify {

    private static final Logger logger = Logger.getLogger(DemoOrderResultNotify.class);

    public static void gm(){

        GmSDKConfig.loadPropertiesFromSrc();


        // TODO 模拟已解析接出网关发起的HTTP请求头和请求报文
        // 注：在某些特殊场景(如HTTP 2.0)下收到的头字段可能为全小写, 此时需要自行解析时做兼容处理
        Map<String, String> reqHeader = new HashMap<>();
        // 收到的body的报文如，具体字段含义见规范
        String respBodyStr = "{\"merId\":\"123456789012345\",\"acqInsCode\":\"CBS12345\",\"txnNo\":\"CBS022070609591122e3011830001001\",\"txnDate\":\"20220706\",\"sndTime\":\"095911\",\"origTxnNo\":\"123456789012345A0023544113\",\"origTxnDate\":\"20220706\",\"origPlatformOrderNo\":\"OrderUPSIMNo2022070610000023544114\",\"orderStatus\":\"06\",\"settleDate\":\"20220629\",\"actReserved\":{\"sampleKeyA\":\"A\",\"sampleKeyB\":\"B\"}}";

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
        resData.put(SDKConstants.param_bizMethod, "pay.b2b.000102");       // 交易类型 根据实际业务填写
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
        // 收到的body的报文如，具体字段含义见规范
        String respBodyStr = "{\"merId\":\"123456789012345\",\"acqInsCode\":\"CBS12345\",\"txnNo\":\"CBS022070609591122e3011830001001\",\"txnDate\":\"20220706\",\"sndTime\":\"095911\",\"origTxnNo\":\"123456789012345A0023544113\",\"origTxnDate\":\"20220706\",\"origPlatformOrderNo\":\"OrderUPSIMNo2022070610000023544114\",\"orderStatus\":\"06\",\"settleDate\":\"20220629\",\"actReserved\":{\"sampleKeyA\":\"A\",\"sampleKeyB\":\"B\"}}";


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
        resData.put(SDKConstants.param_bizMethod, "pay.b2b.000102");       // 交易类型 根据实际业务填写
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
