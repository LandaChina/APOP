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
 *  cbs 直连商户 demo- 订单交易
 *  本交易在订单支付产品中，由商户（ME）向银联（UP）发起
 */
public class DemoOrder {

    private static final Logger logger = Logger.getLogger(DemoOrder.class);

    public static void main(String[] args) {
        // 使用国密算法调用此方法
        GmDemo();

        // 使用国际算法调用此方法
        RsaDemo();
    }

    public static void GmDemo() {

        // 初始化配置文件，读取证书
        GmSDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "pay.b2b.000101");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 填写商户号
        reqData.put(SDKConstants.param_appType, "02");                   // 02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写,具体字段含义及要求参见规范
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("acqInsCode", "CBS12345"); //1机构
        bodyMap.put("merId","123456789012345");// 商户号
        bodyMap.put("txnNo", CbsUtils.getCbsTxnNo((String) bodyMap.get("merId"))); //交易流水号，35位长度，必须以商户号开头
        bodyMap.put("txnDate", DemoUtil.getTxnTime().substring(0,8)); //订单发送时间 yyyyMMdd,8位长度
        bodyMap.put("sndTime", DemoUtil.getTxnTime().substring(8)); //格式为HHmmss,6位长度
        bodyMap.put("platformOrderNo", DemoUtil.getOrderId()); //平台订单号，8-40长度
        bodyMap.put("payeeInfo",buildPayeeInfo() ); // 收款方信息，存放的为对象（此处为举例为Map对象），非String格式
        bodyMap.put("payerInfo",buildPayerInfo() ); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("currencyCode","156");
        bodyMap.put("txnAmt","1");
        bodyMap.put("orderExpTime",DemoUtil.getTxnTime());
        bodyMap.put("backUrl","http://xxx.xxx.xxx.xxx:port/xx");
        bodyMap.put("txnPlatform","01");
        bodyMap.put("orderType","03");

        bodyMap.put("freezeModule","1");
        bodyMap.put("remark","我是remark123");
        bodyMap.put("summary","我是summary123");
        bodyMap.put("merName","测试商户");
        bodyMap.put("mcc","9999");
        bodyMap.put("mchntRegionCd","1234");
        bodyMap.put("merchantType","101");
        bodyMap.put("subMerInfo", buildSubMerInfo());
        bodyMap.put("riskInfo",buildRiskInfo());
        bodyMap.put("reqReserved",buildReservd());
        bodyMap.put("reserved",buildReservd());
        bodyMap.put("acqReserved",buildReservd());


        reqData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());
        logger.info("req=>" + reqData.get(SDKConstants.param_body));

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
        reqData.put(SDKConstants.param_bizMethod, "pay.b2b.000101");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写，具体字段含义及要求参见规范
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("acqInsCode", "CBS12345"); //1机构
        bodyMap.put("merId","123456789012345");// 商户号
        bodyMap.put("txnNo", CbsUtils.getCbsTxnNo((String) bodyMap.get("merId"))); //交易流水号，35位长度，必须以商户号开头
        bodyMap.put("txnDate", DemoUtil.getTxnTime().substring(0,8)); //订单发送时间 yyyyMMdd,8位长度
        bodyMap.put("sndTime", DemoUtil.getTxnTime().substring(8)); //格式为HHmmss,6位长度
        bodyMap.put("platformOrderNo", DemoUtil.getOrderId()); //平台订单号，8-40长度
        bodyMap.put("payeeInfo",buildPayeeInfo() ); // 收款方信息，存放的为对象（此处为举例为Map对象），非String格式
        bodyMap.put("payerInfo",buildPayerInfo() ); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("currencyCode","156");
        bodyMap.put("txnAmt","1");
        bodyMap.put("orderExpTime",DemoUtil.getTxnTime());
        bodyMap.put("backUrl","http://xxx.xxx.xxx.xxx:port/xx");
        bodyMap.put("txnPlatform","01");
        bodyMap.put("orderType","03");

        bodyMap.put("freezeModule","1");
        bodyMap.put("remark","我是remark123");
        bodyMap.put("summary","我是summary123");
        bodyMap.put("merName","测试商户");
        bodyMap.put("mcc","9999");
        bodyMap.put("mchntRegionCd","1234");
        bodyMap.put("merchantType","101");
        bodyMap.put("subMerInfo", buildSubMerInfo());
        bodyMap.put("riskInfo",buildRiskInfo());
        bodyMap.put("reqReserved",buildReservd());
        bodyMap.put("reserved",buildReservd());
        bodyMap.put("acqReserved",buildReservd());

        reqData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());
        logger.info("req=>" + reqData.get(SDKConstants.param_body));

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

    private static Map buildRiskInfo(){
        HashMap<String,String> riskInfo = new HashMap<>();
        riskInfo.put("deviceModel","EMUI1237.24sdfu999.1");
        riskInfo.put("deviceLang","111");
        riskInfo.put("deviceIPAddr","127.0.0.1");
        riskInfo.put("deviceMACAddr","00-24-7E-0A-6C-2E");
        riskInfo.put("deviceSerialNo","73445678901278901562389012123456");
        riskInfo.put("deviceGPSPos","+37.12/-121.23");
        riskInfo.put("deviceSIMNo","13588888888,18966666666");
        riskInfo.put("deviceSIMQty","2");
        riskInfo.put("userId","1000000001");
        riskInfo.put("riskPoint","9999");
        riskInfo.put("riskMsg","优质客户");
        riskInfo.put("regTimeAcq",DemoUtil.getTxnTime());
        riskInfo.put("regMailAcq","5778342@163.com");
        riskInfo.put("shipProv","22");
        riskInfo.put("shipCity","33");
        riskInfo.put("productType","1");


        return  riskInfo;
    }

    private static Map buildSubMerInfo(){
        HashMap<String,String> prodUnits = new HashMap<>();
        prodUnits.put("prodName","1234567890123456789012345678901234567890123456789012345678901234");
        prodUnits.put("prodPrice","123456789012345678");
        prodUnits.put("prodQty","1234567890");
        prodUnits.put("prodAmt","123456789012345678");

        ArrayList<Map> prodUnitsList = new ArrayList<>();
        prodUnitsList.add(prodUnits);
        prodUnitsList.add(prodUnits);
        prodUnitsList.add(prodUnits);
        prodUnitsList.add(prodUnits);

        HashMap<String,Object> subMerUnits = new HashMap<>();
        subMerUnits.put("subMerName","1234567890123456789012345678901234567890");
        subMerUnits.put("subMcc","9999");
        subMerUnits.put("subMerId","123456789012345");
        subMerUnits.put("subMerCertifType","01");
        subMerUnits.put("subMerCertifNo","12345678901234567890123456789012");
        subMerUnits.put("subMerTotalAmt","1");
        subMerUnits.put("subMerTotalKindsQty","12345");
        subMerUnits.put("prodUnitQty","4");
        subMerUnits.put("prodUnits",prodUnitsList);

        ArrayList<Map> subMerUnitsList = new ArrayList<>();
        subMerUnitsList.add(subMerUnits);
        subMerUnitsList.add(subMerUnits);


        HashMap<String,Object> subMerInfo = new HashMap<>();
        subMerInfo.put("subMerTotalQty","2");
        subMerInfo.put("subMerShowQty","2");
        subMerInfo.put("subMerUnits",subMerUnitsList);

        return subMerInfo;
    }

    private static Map buildPayeeInfo(){
        HashMap<String,String> payeeInfo = new HashMap<>();
        payeeInfo.put("bankNo","000123455543"); // 开户行行号，String[12]
        payeeInfo.put("bankName","测试bankName"); // 选填 ，开户行名称,英文支持最大140个字符/ 中文支持最大140个汉字
        payeeInfo.put("acctNo","000123455543"); // 账号,String(1..32)
        payeeInfo.put("acctName","收款方账户名称");// 账户名称,String(1..180)
        payeeInfo.put("acctType","01"); // 账号类型,String[2]
        payeeInfo.put("contactName","收款人联系人明细");// 选填，联系人姓名,String(1..180)
        payeeInfo.put("contactPhone","15317256068"); // 选填，联系人电话，String(1..21)
        payeeInfo.put("unifmCode","15317256068asdCSWF");// 选填， 统一社会信用代码，String[18]
        return payeeInfo;
    }

    private static Map buildPayerInfo(){
        HashMap<String,String> payerInfo = new HashMap<>();
        payerInfo.put("bankNo","000123455543");
        payerInfo.put("bankName","测试银行名超长超长超长超超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长");
        payerInfo.put("acctNo","000123455543");
        payerInfo.put("acctName","付款人账户名称");
        payerInfo.put("acctType","01");
        payerInfo.put("contactName","付款人联系姓名");
        payerInfo.put("contactPhone","15317256068");
        payerInfo.put("unifmCode","15317256068aSWdddd");

        return payerInfo;

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
