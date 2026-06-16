package com.unionpay.uas.demo.ucp.taxcorpcollect;

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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 内容网关税费对公代收业务，签约状态查询接口demo
 * 声明：以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己需要，按照技术文档编写。该代码仅供参考，不提供编码，性能，规范性等方面的保障<br>
 */
public class DemoEntrustCollectQuery {

    private static final Logger logger = Logger.getLogger(DemoEntrustCollectQuery.class);

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
        reqData.put(SDKConstants.param_bizMethod, "upntAcp.ucp.tax.corporate.entrust.collect.query");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "7.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "987654321011111");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("accessType", "0"); //1机构，0商户
        bodyMap.put("merId", "987654321011111"); //商户号，请修改为自己的商户号
        bodyMap.put("orderId", DemoUtil.getOrderId()); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("txnTime", DemoUtil.getTxnTime()); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("bussMerId", "111112222233333"); //不上送征收机关代码时，必填
        bodyMap.put("colOrganCd", "222222"); //不上送征收机关代码时，必填
        bodyMap.put("bussCode", "J1_9800_XJ03"); //业务代码
        bodyMap.put("bussInfo", "1112334"); //纳税人识别号

        // 签约账户信息域 contractAcctInfo
        Map<String, Object> contractAcctInfoMap = new HashMap<>();
        // 签约交易时必填字段 bankNo、acctNo、acctType、acctName
        contractAcctInfoMap.put("bankNo", "111122334455"); //开户行行号
        contractAcctInfoMap.put("acctNo", "11112222333344"); //账号
        contractAcctInfoMap.put("acctType", "10");//账户名称
        contractAcctInfoMap.put("acctName", "小王"); //账户名称
        // 签约交易时选填字段 epCertifType、epCertifNo、name、certifType、certifNo
        contractAcctInfoMap.put("epCertifType", "01"); //企业证件类型 取值：01：社会统一信用证 02：营业执照
        contractAcctInfoMap.put("epCertifNo", "117728838484"); //企业证件号码  企业证件类型为01时填入社会统一信用代码 企业证件类型为02时填入营业执照号码
        contractAcctInfoMap.put("name", "小张"); //填写账户方法定代表人姓名
        /*
        01:身份证
        02:军官证
        03:护照
        04:港澳居民来往内地通行证
        05:台湾居民来往大陆通行证
        06:警官证
        07:士兵证
        08:中华人民共和国旅行证（境外使用）
        09:外国护照（境外使用）
        10:外国人永久居留证（境外使用）
        11:中华人民共和国入出境通行证（境外使用）
        12:港澳居民居住证
        13:台湾居民居住证
        99:其它证件
        */
        contractAcctInfoMap.put("certifType", "01"); //个人证件类型
        contractAcctInfoMap.put("certifNo", "410500199609106332"); //个人证件号码

        // 当商户入网配置敏感信息不加密时上送
        bodyMap.put("contractAcctInfo", Base64.encodeBase64String(JSONObject.wrap(contractAcctInfoMap).toString().getBytes())); //签约账户信息域

        // 当商户入网配置敏感信息加密时，在敏感信息域中上送
        // 组装encryptData
        Map<String, Object> encryptDataMap = new HashMap<>();
        encryptDataMap.put("contractAcctInfo", Base64.encodeBase64String(JSONObject.wrap(contractAcctInfoMap).toString().getBytes())); //签约账户信息域

        String toBeEncData = JSONObject.wrap(encryptDataMap).toString(); // 待加密报文串
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

        bodyMap.put("countryCode", "156"); //国家代码,当地区代码出现时必填
        bodyMap.put("areaCode", "1100"); //地区代码
        bodyMap.put("remark", "这是附言"); //附言
        bodyMap.put("summary", "这是摘要"); //业务摘要
        bodyMap.put("reqReserved", "这是请求方保留域"); //请求方保留域

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
                    String decodeStr = UasService.rsaDec(decData, decKey, certId);
                    logger.info("解银联加密后的敏感信息的报文：" + decodeStr);
                } catch (Exception e) {
                    logger.error(e);
                }
            }

            if (rspBody.get("respInfo") != null) {
                String respInfo = (String) rspBody.get("respInfo");
                logger.info("应答信息域为：" + respInfo + "\n");
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
        reqData.put(SDKConstants.param_bizMethod, "upntAcp.ucp.tax.corporate.entrust.collect.query");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "7.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "00010000");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("accessType", "0"); //1机构，0商户
        bodyMap.put("merId", "987654321011111"); //商户号，请修改为自己的商户号
        bodyMap.put("orderId", DemoUtil.getOrderId()); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("txnTime", DemoUtil.getTxnTime()); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("bussMerId", "111112222233333"); //不上送征收机关代码时，必填
        bodyMap.put("colOrganCd", "222222"); //不上送征收机关代码时，必填
        bodyMap.put("bussCode", "J1_9800_XJ03"); //业务代码
        bodyMap.put("bussInfo", "1112334"); //纳税人识别号

        // 签约账户信息域 contractAcctInfo
        Map<String, Object> contractAcctInfoMap = new HashMap<>();
        // 签约交易时必填字段 bankNo、acctNo、acctType、acctName
        contractAcctInfoMap.put("bankNo", "111122334455"); //开户行行号
        contractAcctInfoMap.put("acctNo", "11112222333344"); //账号
        contractAcctInfoMap.put("acctType", "10");//账户名称
        contractAcctInfoMap.put("acctName", "小王"); //账户名称
        // 签约交易时选填字段 epCertifType、epCertifNo、name、certifType、certifNo
        contractAcctInfoMap.put("epCertifType", "01"); //企业证件类型 取值：01：社会统一信用证 02：营业执照
        contractAcctInfoMap.put("epCertifNo", "117728838484"); //企业证件号码  企业证件类型为01时填入社会统一信用代码 企业证件类型为02时填入营业执照号码
        contractAcctInfoMap.put("name", "小张"); //填写账户方法定代表人姓名
        /*
        01:身份证
        02:军官证
        03:护照
        04:港澳居民来往内地通行证
        05:台湾居民来往大陆通行证
        06:警官证
        07:士兵证
        08:中华人民共和国旅行证（境外使用）
        09:外国护照（境外使用）
        10:外国人永久居留证（境外使用）
        11:中华人民共和国入出境通行证（境外使用）
        12:港澳居民居住证
        13:台湾居民居住证
        99:其它证件
        */
        contractAcctInfoMap.put("certifType", "01"); //个人证件类型
        contractAcctInfoMap.put("certifNo", "410500199609106332"); //个人证件号码

        // 当商户入网配置敏感信息不加密时上送
        bodyMap.put("contractAcctInfo", Base64.encodeBase64String(JSONObject.wrap(contractAcctInfoMap).toString().getBytes())); //签约账户信息域

        // 当商户入网配置敏感信息加密时，在敏感信息域中上送
        // 组装encryptData
        Map<String, Object> encryptDataMap = new HashMap<>();
        encryptDataMap.put("contractAcctInfo", Base64.encodeBase64String(JSONObject.wrap(contractAcctInfoMap).toString().getBytes())); //签约账户信息域


        String toBeEncData = JSONObject.wrap(encryptDataMap).toString(); // 待加密报文串
        logger.info("加密前的敏感信息明文: " + toBeEncData);

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

        bodyMap.put("countryCode", "156"); //国家代码,当地区代码出现时必填
        bodyMap.put("areaCode", "1100"); //地区代码
        bodyMap.put("remark", "这是附言"); //附言
        bodyMap.put("summary", "这是摘要"); //业务摘要
        bodyMap.put("reqReserved", "这是请求方保留域"); //请求方保留域

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

            if (rspBody.get("respInfo") != null) {
                String respInfo = (String) rspBody.get("respInfo");
                logger.info("应答信息域为：" + respInfo + "\n");
            }
        } catch (JSONException e) {
            logger.info("应答体解json失败。");
        }

    }
}
