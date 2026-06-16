package com.unionpay.uas.demo.cbs;

import com.unionpay.uas.demo.cbs.util.CbsUtils;
import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConfig;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.UasService;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUasService;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *  cbs 直连商户 demo- 交易状态查询（单笔）
 *  如商户在5分钟内未收到订单解冻交易、订单批次支付申请交易应答、订单批次支付结果通知或收到交易超时应答，商户可主动发起交易状态查询，查询交易结果。建议每次查询间隔为5分钟，查询5次后仍未收到结果建议转人工处理。
 * 在交易状态查询应答报文中，“应答码”即code字段，表示的是查询交易本身的应答，即查询这个动作是否成功，不代表被查询交易的状态；若查询动作成功，即应答码为0000000000，则根据“原交易应答码”即origCode来判断被查询交易是否成功。此时若origCode为0000000000，则表示被查询交易成功。
 */
public class DemoTransStateQuery {

    private static final Logger logger = Logger.getLogger(DemoTransStateQuery.class);

    public static void main(String[] args) {
        // 使用国密算法调用此方法
        GmDemo();

        // 使用国际算法调用此方法
//        RsaDemo();
    }

    public static void GmDemo() {

        // 初始化配置文件，读取证书
        GmSDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "pay.b2b.000903");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 具体字段含义及要求参见规范
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("acqInsCode", "CBS12345"); //1机构，0商户
        bodyMap.put("merId","123456789012345");
        bodyMap.put("txnNo", CbsUtils.getCbsTxnNo((String) bodyMap.get("merId"))); //商户号，请修改为自己的商户号
        bodyMap.put("txnDate", DemoUtil.getTxnTime().substring(0,8)); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("sndTime", DemoUtil.getTxnTime().substring(8)); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值

        /*******************原订单交易信息**************************/
        // 原订单交易txnNo
        bodyMap.put("origTxnNo", "123456789012345muXkk0olRLpcyhfnts0v");
        // 原订单交易txnDate
        bodyMap.put("origTxnDate", "20220704");
        // 原订单交易
        bodyMap.put("origBizMethod", "pay.b2b.000105");

        bodyMap.put("reqReserved",buildReservd());
        bodyMap.put("reserved",buildReservd());
        bodyMap.put("acqReserved",buildReservd());

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
        reqData.put(SDKConstants.param_bizMethod, "pay.b2b.000903");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 具体字段含义及要求参见规范
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("acqInsCode", "CBS12345"); //1机构，0商户
        bodyMap.put("merId","123456789012345");
        bodyMap.put("txnNo", CbsUtils.getCbsTxnNo((String) bodyMap.get("merId"))); //商户号，请修改为自己的商户号
        bodyMap.put("txnDate", DemoUtil.getTxnTime().substring(0,8)); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("sndTime", DemoUtil.getTxnTime().substring(8)); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值

        /*******************原订单交易信息**************************/
        // 原订单交易txnNo
        bodyMap.put("origTxnNo", "123456789012345muXkk0olRLpcyhfnts0v");
        // 原订单交易txnDate
        bodyMap.put("origTxnDate", "20220704");
        // 原订单交易
        bodyMap.put("origBizMethod", "pay.b2b.000105");

        bodyMap.put("reqReserved",buildReservd());
        bodyMap.put("reserved",buildReservd());
        bodyMap.put("acqReserved",buildReservd());

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
        if (rspBody.get("billDetailInfo") != null) {
            String billDetailInfo = new String(Base64.decodeBase64((String) rspBody.get("billDetailInfo")));
            logger.info("查询应答账单域为：" + billDetailInfo + "\n");
        }

    }


    private static Map buildReservd(){
        HashMap<String,Object> map = new HashMap<>();
        map.put("reserved1","我是保留域1");
        map.put("reserved2","我是保留域2");
        map.put("reserved3","我是保留域3");
//        map.put("天知道这是啥",buildPayeeInfo());

        return map;
    }
}
