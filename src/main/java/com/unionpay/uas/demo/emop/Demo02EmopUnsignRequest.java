package com.unionpay.uas.demo.emop;

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
 * 解约申请       bizMethod：upntCbs.emop.unsign.request
 * 声明：以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己需要，按照技术文档编写。该代码仅供参考，不提供编码，性能，规范性等方面的保障<br>
 */
public class Demo02EmopUnsignRequest {

    private static final Logger logger = Logger.getLogger(Demo02EmopUnsignRequest.class);

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
        reqData.put(SDKConstants.param_bizMethod, "upntCbs.emop.unsign.request");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "2.1.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "987654321011111");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("accessCode", ""); //接入方代码:由银联分配。在交易报文中标识的，向银联系统直接发送交易请求或通知报文的机构对应的机构代码
        bodyMap.put("txnDate", "20241022"); //交易日期 格式： YYYYMMDD，如20240622
        bodyMap.put("txnNo", "");  //交易流水号:由交易发送机构生成，同一交易日期内不可重复 String(15..35)
        bodyMap.put("sndTime", "112434"); //发送时间:hhmmss
        /*
         * 付款企业证件类型
         * 01：营业执照
         * 02：事业单位法人证书
         * 03：统一社会信用代码
         * 04：其他证明文件
         * 05：民办非企业单位登记证书
         * 06：基金会法人登记证书
         * 07：组织机构代码证
         * */
        bodyMap.put("licTp", ""); //付款企业证件类型
        bodyMap.put("licNo", ""); //付款企业证件号码
        /*
         * 凭据变更类型
         * 10：账户（单位结算账户）
         * 00：卡（单位结算卡、商务卡）
         * */
        bodyMap.put("payerAcctType", ""); //凭据变更类型
        bodyMap.put("remark", ""); //附言

        //bodyMap.put("acqReserved", buildAcqReservedMap()); //接入方保留域

        // 需要加密的敏感信息报文 根据实际业务填写,商户入网配置敏感信息加密时使用如下注释代码
        Map<String, Object> encDataMap = new HashMap<>();
        encDataMap.put("payerAcctNo", ""); //付款方账号
        String toBeEncData = JSONObject.wrap(encDataMap).toString(); // 待加密报文串
        logger.info("加密前的敏感信息明文:" + toBeEncData);

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
        reqData.put(SDKConstants.param_bizMethod, "upntCbs.emop.unsign.request");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "2.1.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "00010000");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("accessCode", ""); //接入方代码:由银联分配。在交易报文中标识的，向银联系统直接发送交易请求或通知报文的机构对应的机构代码
        bodyMap.put("txnDate", "20241022"); //交易日期 格式： YYYYMMDD，如20240622
        bodyMap.put("txnNo", "");  //交易流水号:由交易发送机构生成，同一交易日期内不可重复 String(15..35)
        bodyMap.put("sndTime", "112434"); //发送时间:hhmmss
        /*
         * 付款企业证件类型
         * 01：营业执照
         * 02：事业单位法人证书
         * 03：统一社会信用代码
         * 04：其他证明文件
         * 05：民办非企业单位登记证书
         * 06：基金会法人登记证书
         * 07：组织机构代码证
         * */
        bodyMap.put("licTp", ""); //付款企业证件类型
        bodyMap.put("licNo", ""); //付款企业证件号码
        /*
         * 凭据变更类型
         * 10：账户（单位结算账户）
         * 00：卡（单位结算卡、商务卡）
         * */
        bodyMap.put("payerAcctType", ""); //凭据变更类型
        bodyMap.put("remark", ""); //附言

        //bodyMap.put("acqReserved", buildAcqReservedMap()); //接入方保留域

        // 需要加密的敏感信息报文 根据实际业务填写,商户入网配置敏感信息加密时使用如下注释代码
        Map<String, Object> encDataMap = new HashMap<>();
        encDataMap.put("payerAcctNo", ""); //付款方账号
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

            if (rspBody.get("respInfo") != null) {
                String respInfo = (String) rspBody.get("respInfo");
                logger.info("应答信息域为：" + respInfo + "\n");
            }
        } catch (JSONException e) {
            logger.info("应答体解json失败。");
        }

    }

    private static Map buildAcqReservedMap() {
        Map<String, Object> acqReservedMap = new HashMap<String, Object>();
        acqReservedMap.put("key1", ""); //key1根据需要自行修改
        acqReservedMap.put("kye2", ""); //key2根据需要自行修改
        return acqReservedMap;
    }
}
