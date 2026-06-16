package com.unionpay.uas.demo.acp.fsas;

import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConfig;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.UasService;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUasService;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.unionpay.uas.sdk.SDKUtil.inflater;

/**
 * 批量交易状态查询示例demo:
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
public class DemoBatchQuery {
    private static final Logger logger = Logger.getLogger(DemoBatchQuery.class);

    public static void main(String[] args) {
        // 使用国密算法调用此方法
        GmDemo();

        // 使用国际算法调用此方法
        //RsaDemo();
    }

    public static void GmDemo() {

        // 初始化配置文件，读取证书
        GmSDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "upntFsas.trade.batch.query");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "7.0.1");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("batchType","");//批量交易类型;12：ACS账户充值
        bodyMap.put("acqInsCode","");//收单机构代码
        bodyMap.put("batchNo","");//原交易批次号
        bodyMap.put("txnTime", ""); //原批量代收请求的交易时间
        bodyMap.put("reqReserved","");//请求方保留域
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
                    //decodeInflate
                    Map<String, Object> encryptDataMap = DemoUtil.unwarpJson(new JSONObject(decodeStr));
                    if(encryptDataMap.containsKey("fileContent")){
                        String fileContent = new String(decodeInflate((String)(encryptDataMap.get("fileContent"))),"UTF-8");
                        logger.info("批量查询结果文件内容：" + fileContent + "\n");
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            }

            Map<String, Object> rspBodyMap = DemoUtil.unwarpJson(new JSONObject(rspBody));
            if(rspBodyMap.containsKey("fileContent")){
                String fileContent = (String)rspBodyMap.get("fileContent");
                String fileContentDeflateDecode = new String(decodeInflate(fileContent),"UTF-8");
                logger.info("批量查询结果文件内容：" + fileContentDeflateDecode);
            }

        } catch (JSONException e) {
            logger.info("应答体解json失败。");
        } catch (Exception e) {
            logger.info("出现异常。",e);
        }

    }

    public static void RsaDemo() {

        // 初始化配置文件，读取证书
        SDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "upntFsas.trade.batch.query");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "7.0.1");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("batchType","");//批量交易类型;12：ACS账户充值
        bodyMap.put("acqInsCode","");//收单机构代码
        bodyMap.put("batchNo","");//原交易批次号
        bodyMap.put("txnTime", ""); //原批量代收请求的交易时间
        bodyMap.put("reqReserved","");//请求方保留域
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
                    //decodeInflate
                    Map<String, Object> encryptDataMap = DemoUtil.unwarpJson(new JSONObject(decodeStr));
                    if(encryptDataMap.containsKey("fileContent")){
                        String fileContent = new String(decodeInflate((String)(encryptDataMap.get("fileContent"))),"UTF-8");
                        logger.info("批量查询结果文件内容：" + fileContent + "\n");
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            }
            Map<String, Object> rspBodyMap = DemoUtil.unwarpJson(new JSONObject(rspBody));
            if(rspBodyMap.containsKey("fileContent")){
                String fileContent = (String)rspBodyMap.get("fileContent");
                String fileContentDeflateDecode = new String(decodeInflate(fileContent),"UTF-8");
                logger.info("批量查询结果文件内容：" + fileContentDeflateDecode);
            }

        } catch (JSONException e) {
            logger.info("应答体解json失败。");
        }catch (Exception e) {
            logger.info("出现异常。",e);
        }


    }
    public static byte[] decodeInflate(String raw) throws IOException {
        if (raw == null || raw.length() == 0) {
            throw new IllegalArgumentException("Decode and inflate error with null raw byte[]");
        }
        byte[] tmpByte = Base64.decodeBase64(raw.getBytes(Charset.forName("UTF-8")));
        return inflater(tmpByte);
    }

}
