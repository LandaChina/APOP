package com.unionpay.uas.demo.ucp.cashInstalment;

import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConfig;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.UasService;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUasService;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.*;

/**
 * 内容网关现金分期业务，现金分期申请（渠道侧->银联）demo
 * 声明：以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己需要，按照技术文档编写。该代码仅供参考，不提供编码，性能，规范性等方面的保障<br>
 */
public class DemoApply {

    private static final Logger logger = Logger.getLogger(DemoApply.class);

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
        reqData.put(SDKConstants.param_bizMethod, "upntAcp.ucp.cash.instalment.apply");       // 交易类型 根据实际业务填写
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
        bodyMap.put("bussCode", "J1_9800_XJ03"); //业务代码

        // 组装encryptData
        Map<String, Object> encryptDataMap = new HashMap<>();
        encryptDataMap.put("usrNo", "77899887656677"); //用户号码:信用卡卡号
        encryptDataMap.put("debitCardId", "99899887656677"); //借记卡卡号,用户选择入账的借记卡卡号

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

        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap); // 如果不需要携带敏感信息并由网关解密, 则注释这行代码

        //请求信息域	reqInfo
        Map<String, Object> reqInfoMap = new HashMap<>();
        reqInfoMap.put("instalNum", "1"); //分期期数
        reqInfoMap.put("channelNo", "xxxhjshhdjj"); //渠道侧订单号:由渠道生成并上送，用于唯一标识这笔申请交易;编码规则：商户号+渠道侧订单号唯一确定一笔
        reqInfoMap.put("merIdName", "xx商户"); //渠道商户名
        reqInfoMap.put("instalCapital", "1000"); //申请现金分期金额
        reqInfoMap.put("transferModel", "0"); //到账方式：0：实时 1：普通到账 2：次日到账
        reqInfoMap.put("useDesc", "0"); //资金用途：0:旅游消费（默认）1:购车消费 2:装修建材 3:家具家电 4:医疗服务 5:婚庆服务 6:助学进修
        reqInfoMap.put("poundageType", "1"); //利息收取类型 1：每期收取利息 2：一次性收取利息
        reqInfoMap.put("smsKey", "23syyudhh"); //动态短信关联码 ：同“现金分期申请触发短信接口”信用卡中心返回的sms_key
        reqInfoMap.put("messageCode", "336788"); //短信验证码
        reqInfoMap.put("usedWithinLimit", "1"); //占用的额度内额度,可选
        reqInfoMap.put("usedBeyondLimit", "1"); //占用的额度外额度,可选
        bodyMap.put("reqInfo", JSONObject.wrap(reqInfoMap).toString());

        // 可选
        bodyMap.put("reserved", "this is reserved.."); //保留域

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
        Map<String, Object> rspBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
        if (rspBody.get("respInfo") != null) {
            String respInfo = (String) rspBody.get("respInfo");
            logger.info("应答信息域为：" + respInfo + "\n");
        }

    }

    public static void RsaDemo() {

        // 初始化配置文件，读取证书
        SDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "upntAcp.ucp.cash.instalment.apply");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "7.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "987654321011111");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("accessType", "0"); //1机构，0商户
        bodyMap.put("merId", "987654321011111"); //商户号，请修改为自己的商户号
        bodyMap.put("orderId", DemoUtil.getOrderId()); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("txnTime", DemoUtil.getTxnTime()); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("bussCode", "J1_9800_XJ03"); //业务代码

        // 组装encryptData
        Map<String, Object> encryptDataMap = new HashMap<>();
        encryptDataMap.put("usrNo", "77899887656677"); //用户号码:信用卡卡号
        encryptDataMap.put("debitCardId", "99899887656677"); //借记卡卡号,用户选择入账的借记卡卡号

        String toBeEncData = JSONObject.wrap(encryptDataMap).toString(); // 待加密报文串
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

        //请求信息域	reqInfo
        Map<String, Object> reqInfoMap = new HashMap<>();
        reqInfoMap.put("instalNum", "1"); //分期期数
        reqInfoMap.put("channelNo", "xxxhjshhdjj"); //渠道侧订单号:由渠道生成并上送，用于唯一标识这笔申请交易;编码规则：商户号+渠道侧订单号唯一确定一笔
        reqInfoMap.put("merIdName", "xx商户"); //渠道商户名
        reqInfoMap.put("instalCapital", "1000"); //申请现金分期金额
        reqInfoMap.put("transferModel", "0"); //到账方式：0：实时 1：普通到账 2：次日到账
        reqInfoMap.put("useDesc", "0"); //资金用途：0:旅游消费（默认）1:购车消费 2:装修建材 3:家具家电 4:医疗服务 5:婚庆服务 6:助学进修
        reqInfoMap.put("poundageType", "1"); //利息收取类型 1：每期收取利息 2：一次性收取利息
        reqInfoMap.put("smsKey", "23syyudhh"); //动态短信关联码 ：同“现金分期申请触发短信接口”信用卡中心返回的sms_key
        reqInfoMap.put("messageCode", "336788"); //短信验证码
        reqInfoMap.put("usedWithinLimit", "1"); //占用的额度内额度,可选
        reqInfoMap.put("usedBeyondLimit", "1"); //占用的额度外额度,可选
        bodyMap.put("reqInfo", JSONObject.wrap(reqInfoMap).toString());

        // 可选
        bodyMap.put("reserved", "this is reserved.."); //保留域

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

        logger.info("从银联获得HTTP应答报文为：" + respBodyStr + "\n");
        Map<String, Object> rspBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
        if (rspBody.get("respInfo") != null) {
            String respInfo = (String) rspBody.get("respInfo");
            logger.info("应答信息域为：" + respInfo + "\n");
        }

    }
}
