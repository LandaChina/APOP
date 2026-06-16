package com.unionpay.uas.demo.maps;

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
 * 银行缴款通知书验章demo
 * 声明：以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己需要，按照技术文档编写。该代码仅供参考，不提供编码，性能，规范性等方面的保障<br>
 */
public class DemoSealVerification {

    private static final Logger logger = Logger.getLogger(DemoSealVerification.class);

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
        reqData.put(SDKConstants.param_bizMethod, "upntMaps.seal.verification");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "898530193998881");        // 发送方系统索引号 根据实际业务填写,可商户号，可为机构代码
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();

        bodyMap.put("orderId", DemoUtil.getOrderId()); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("txnTime", DemoUtil.getTxnTime()); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("bussCd", "S0_7300_T001"); //与通知书推送收到的同名字段保持一致
        bodyMap.put("bussInfo", "53402"); //与通知书推送收到的同名字段保持一致（地区码加行别号）

        // 根据报文要求和实际业务需求组装reqInfo字段
        Map<String, Object> Info = new HashMap<>();
        Info.put("dzsphm", "0100000000000000000");//电子税票号码
        Info.put("pdfStr", "*************************");//交易中心签章内容

        //将reqInfo塞入报文体
        bodyMap.put("reqInfo", JSONObject.wrap(Info).toString());
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
        // 组装验签报文，请以实际接口内的字段顺序为准
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
        Map<String,Object> rspBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
        if (rspBody.get("billDetailInfo") != null) {
            String billDetailInfo = new String(Base64.decodeBase64((String) rspBody.get("billDetailInfo")));
            logger.info("查询应答账单域为：" + billDetailInfo + "\n");
        }

    }

    public static void RsaDemo() {

        // 初始化配置文件，读取证书
        SDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "upntMaps.seal.verification");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "898530193998881");        // 发送方系统索引号 根据实际业务填写,可为商户号，可为机构代码
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();

        bodyMap.put("orderId", DemoUtil.getOrderId()); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("txnTime", DemoUtil.getTxnTime()); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("bussCd", "S0_7300_T001"); //与通知书推送收到的同名字段保持一致
        bodyMap.put("bussInfo", "53402"); //与通知书推送收到的同名字段保持一致（地区码加行别号）

        // 根据报文要求和实际业务需求组装reqInfo字段，实际内容请以接口和业务为准
        Map<String, Object> Info = new HashMap<>();
        Info.put("dzsphm", "0100000000000000000");//电子税票号码
        Info.put("pdfStr", "*************************");//交易中心签章内容

        //将reqInfo塞入报文体
        bodyMap.put("reqInfo", JSONObject.wrap(Info).toString());
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

        // 组装验签报文，请以实际接口内的字段顺序为准
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
        Map<String,Object> rspBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
        if (rspBody.get("billDetailInfo") != null) {
            String billDetailInfo = new String(Base64.decodeBase64((String) rspBody.get("billDetailInfo")));
            logger.info("查询应答账单域为：" + billDetailInfo + "\n");
        }

    }
}
