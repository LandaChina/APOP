package com.unionpay.uas.demo.tbs;

import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.UasService;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUasService;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * TBS系统的模拟服务商接入示例demo-身份证OCR
 */
public class DemoOcrCertifid {

    private static final Logger logger = Logger.getLogger(DemoOcrCertifid.class);

    public static void main(String[] args) {
        gm();
    }

    public static void gm(){
        GmSDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        String reqId = DemoUtil.getReqId();
        reqData.put(SDKConstants.param_bizMethod, "soluTbs.precredit.ocr.certifid");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");          // 签名方法
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, reqId);      // 发送方流水号，可以自行定制规则
        //reqData.put(SDKConstants.param_signId, "1");

        // 组装HTTP请求报文体 具体字段含义及要求参见规范
        Map<String, Object> commonMap = new HashMap<>();
        commonMap.put("thirdMerId","xxxxx");

        Map<String, Object> bizMap = new HashMap<>();
        bizMap.put("fileFormat","jpg");
        bizMap.put("contractId","C164032590079558278");

        // 需要加密的敏感信息报文 根据实际业务填写
        Map<String, Object> customInfo = new HashMap<>();
        customInfo.put("frontImage", "data:image/jpg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD");
        customInfo.put("backImage", "data:image/jpg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD");
        Map<String, Object> encDataMap = new HashMap<>();
        encDataMap.put("customInfo",customInfo);

        // 待加密报文串
        String toBeEncData = JSONObject.wrap(encDataMap).toString();
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

        Map<String, Object> reqDataMap = new HashMap<>();
        reqDataMap.put("commonContent", commonMap);
        reqDataMap.put("bizContent",bizMap);

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("issInsCode","CMBC0001");
        bodyMap.put("clientIp","127.0.0.1");
        bodyMap.put("transDate",DemoUtil.getTxnTime().substring(0,8));
        bodyMap.put("requestId",reqId);
        bodyMap.put("reqData", reqDataMap);
        // 如果不需要携带敏感信息并由网关解密, 则注释这行代码
        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap);

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

        logger.info("从银联获得HTTP应答报文为：" + respBodyStr + "\n");
    }
}
