package com.unionpay.uas.demo;


import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.ThreadLocalUtil;
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
 * 支持使用多套配置文件的示例代码
 * 默认的配置文件名为【uas_sdk.properties】
 * 允许在同目录下配置多套配置文件，命名格式为【uas_sdk_${type}.properties】
 * 其中${type} 为自定义的名称
 * 基于2024版通用规范（Q/CUP 083-2024）的Demo示例代码
 */
public class MultipleConfigGTS24Demo {

    private final static Logger logger = Logger.getLogger(RequestGmDemo.class);


    public static void init(){

        // 定义两套不同的配置文件名 分别对应【uas_sdk_type1.properties】、【uas_sdk_type2.properties】
        String[] types = {"type1", "type2"};

        // 初始化配置文件并加载到缓存中
        GmSDKConfig.loadPropertiesFromSrc(types);

    }


    public static void main(String[] args) {

        // 系统启动时加载 init()
        init();

        // 通过ThreadLocal选择使用哪套配置文件, 此处以【uas_sdk_type1.properties】配置文件为例
        // 务必确保文件目录下已经新增【uas_sdk_type1.properties】配置文件
        // 如果文件目录下没有【uas_sdk_type1.properties】配置文件，或者没有使用ThreadLocal设置type,则默认使用【uas_sdk.properties】配置文件
        ThreadLocalUtil.TypeSelector.set("type1");

        // 后续正常使用SDK组装请求数据
        Map<String, String> reqMap = buildReqData();

        // 签名
        reqMap = GmUasService.sign(reqMap);

        // 发起HTTP请求, 此处逻辑可替换为项目中实际使用的其他HTTP Client
        Map<String, Object> rspMap = UasService.post(reqMap, GmSDKConfig.getConfig().getTransUrl());  //发送请求报文并接受同步应答（默认连接超时时间30秒，读取返回结果超时时间30秒）;这里调用sign之后，调用post之前不能对map中的键值对做任何修改，如果修改会导致验签不通过

        if (rspMap == null || rspMap.isEmpty()) {
            logger.info("POST请求服务端失败");
            return;
        }

        // 注意：如果使用的HTTP Client为异步请求，响应线程做了切换,那么此处需要重新设置配置文件的type：
        // ThreadLocalUtil.TypeSelector.set("type1");

        // 解析响应头和报文并做验签
        Map<String, String> respHeader = (Map<String, String>) rspMap.get(SDKConstants.param_header);
        String respBodyStr = (String) rspMap.get(SDKConstants.param_body);
        verifySign(respHeader, respBodyStr);

        // 解析响应报文 如果有敏感信息则做解密处理
        decryptData(respBodyStr);

    }

    public static Map<String, String> buildReqData(){

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "uas.demo.biz");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        // reqData.put(SDKConstants.param_signMethod, "SM2-CERT");               // 签名方法
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则
        reqData.put(SDKConstants.param_gtsnb, "Q/CUP 083-2024");        // 通用技术规范版本号,使用新规范必填
        reqData.put(SDKConstants.param_signIssuer, "CFCA");             // 证书颁发者,无特出指定的情况下默认为CFCA

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("indMchntCd", "123456789012345");

        Map<String, Object> encDataMap = new HashMap<>(); // 需要加密的敏感信息报文 根据实际业务填写
        encDataMap.put("cardInfo", "encValue1");
        encDataMap.put("payerInfo", "encValue2");
        String toBeEncData = JSONObject.wrap(encDataMap).toString(); // 待加密报文串
        logger.info("加密前的敏感信息明文: " + toBeEncData);

        Map<String, String> toBeEncMap = null;
        try {
            //TODO sm4临时公钥16字节,请随机生成
            String key = DemoUtil.getRandomString(16);
            logger.info("随机生成的对称密钥key: " + key + "\n");
            toBeEncMap = GmUasService.gmEncGTS24(toBeEncData, key, "utf-8");
        } catch (Exception e) {
            logger.error(e);
        }

        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap); // 如果不需要携带敏感信息并由网关解密, 则注释这行代码

        reqData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

        return reqData;

    }

    public static void verifySign(Map<String, String> respHeader, String respBodyStr){
        // 组装验签报文
        // 注：网关返回的应答头字段是驼峰式的; 此处为不区分大小写 在解析应答头时将所有字段名统一转回为小写
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
        } else {
            logger.info("验证银联签名成功\n");
        }
    }

    public static void decryptData(String respBodyStr){
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
                    String decodeStr = GmUasService.gmDecGTS24(decData, decKey, certId);
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
