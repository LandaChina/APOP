package com.unionpay.uas.demo.ucp.uniontax;

import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConfig;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.UasService;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUasService;
import java.util.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * 税费合并业务，后台账单支付demo
 * 声明：以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己需要，按照技术文档编写。该代码仅供参考，不提供编码，性能，规范性等方面的保障<br>
 */
public class DemoBillPayBackend {

    private static final Logger logger = Logger.getLogger(DemoBillPay.class);

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
        reqData.put(SDKConstants.param_bizMethod, "ucp.trade.bill.pay.backend");       // 交易类型 根据实际业务填写
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
        bodyMap.put("txnAmt", "20000"); //交易金额，填写查询应答的总账单金额
        bodyMap.put("currencyCode", "156"); //币种，固定填写
        bodyMap.put("billPayType", "5"); //支付类型
        bodyMap.put("backUrl", "http://www.specialUrl.com"); //后台通知地址
        bodyMap.put("frontUrl", "http://www.specialUrl.com"); //前台跳转地址
        bodyMap.put("encryptCertId", GmUasService.getEncryptCertId()); //加密公钥证书的Serial Number
        // bodyMap.put("authCode", ""); //支付场景为条码被扫支付或者人脸后台支付时必送，此时需更改billPayType为6

        // 组装卡支付信息域，仅提供组装方法的参考，需要根据实际进行修改
        Map<String,String> cardInfo = new HashMap<>();
        Map<String,String> encryptedData = new HashMap<>();

        encryptedData.put("accNo", "6226090000000048");
        encryptedData.put("expired", "2102");
        encryptedData.put("track2Data", "6222316581794720=000380:7:>2?9:095=88");
        encryptedData.put("track3Data", "996222316581794720=1561560500050000000015010108214000000001=0218888885=000000000=040000788:7;91:;?164300");

        String encryptedDataStr = GmUasService.encryptData(JSONObject.wrap(encryptedData).toString(), "UTF-8");
        cardInfo.put("encryptedData", encryptedDataStr);
        cardInfo.put("icCardCondiCode", "1");
        cardInfo.put("icCardData", Base64.encodeBase64String("9F2608BF5CE558173182A19F2701809F101307020103A0A010010A0100000010003D82CC".getBytes()));
        cardInfo.put("icCardSeqNumber", "001");
        cardInfo.put("posEntryModeCode", "07");
        cardInfo.put("termEntryCap", "0");
        bodyMap.put("cardInfo",Base64.encodeBase64String(JSONObject.wrap(cardInfo).toString().getBytes()));

        // 组装查询账单域，仅提供组装方法的参考，需要根据实际账单进行修改
        List<Map<String, Object>> billList = new ArrayList<>();

        // 组装第一个账单报文
        Map<String, Object> billMap1 = new HashMap<>();
        billMap1.put("index", "01");
        billMap1.put("code", "S0_9800_0003");
        billMap1.put("billQueryId", "33242709233600164520  020220207");

        // 组装第一个账单的form
        Map<String, String> form1 = new HashMap<>();
        form1.put("usr_num", "11000021000149719015");
        form1.put("amount", "100");

        billMap1.put("form", form1);
        billList.add(billMap1);

        // 组装第二个账单报文
        Map<String, Object> billMap2 = new HashMap<>();
        billMap2.put("index", "02");
        billMap2.put("code", "S0_9800_0001");
        billMap2.put("billQueryId", "33225909174100164520  020220124");

        // 组装第二个账单的form
        Map<String, String> form2 = new HashMap<>();
        form2.put("usr_num", "91310114MA1GXBNG2T");
        form2.put("col_organ_cd", "21100000000");
        form2.put("col_voucher_no", "131011400002021010");
        form2.put("proc_flg", "03");
        form2.put("amount", "100");

        billMap2.put("form", form2);
        billList.add(billMap2);
        bodyMap.put("billDetailInfo", Base64.encodeBase64String(JSONObject.wrap(billList).toString().getBytes()));

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
        Map<String,Object> rspBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
        if (rspBody.get("billDetailInfo") != null) {
            String billDetailInfo = new String(Base64.decodeBase64((String) rspBody.get("billDetailInfo")));
            logger.info("应答账单域为：" + billDetailInfo + "\n");
        }

    }

    public static void RsaDemo() {

        // 初始化配置文件，读取证书
        SDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "ucp.trade.bill.pay.backend");       // 交易类型 根据实际业务填写
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
        bodyMap.put("txnAmt", "20000"); //交易金额，填写查询应答的总账单金额
        bodyMap.put("currencyCode", "156"); //币种，固定填写
        bodyMap.put("billPayType", "5"); //支付类型
        bodyMap.put("backUrl", "http://www.specialUrl.com"); //后台通知地址
        bodyMap.put("encryptCertId", UasService.getEncryptCertId()); //加密公钥证书的Serial Number

        // bodyMap.put("authCode", ""); //支付场景为条码被扫支付或者人脸后台支付时必送，此时需更改billPayType为6

        // 组装卡支付信息域，仅提供组装方法的参考，需要根据实际进行修改
        Map<String,String> cardInfo = new HashMap<>();
        Map<String,String> encryptedData = new HashMap<>();

        encryptedData.put("accNo", "6226090000000048");
        encryptedData.put("expired", "2102");
        encryptedData.put("track2Data", "6222316581794720=000380:7:>2?9:095=88");
        encryptedData.put("track3Data", "996222316581794720=1561560500050000000015010108214000000001=0218888885=000000000=040000788:7;91:;?164300");

        String encryptedDataStr = UasService.encryptData(JSONObject.wrap(encryptedData).toString(), "UTF-8");
        cardInfo.put("encryptedData", encryptedDataStr);
        cardInfo.put("icCardCondiCode", "1");
        cardInfo.put("icCardData", Base64.encodeBase64String("9F2608BF5CE558173182A19F2701809F101307020103A0A010010A0100000010003D82CC".getBytes()));
        cardInfo.put("icCardSeqNumber", "001");
        cardInfo.put("posEntryModeCode", "07");
        cardInfo.put("termEntryCap", "0");
        bodyMap.put("cardInfo",Base64.encodeBase64String(JSONObject.wrap(cardInfo).toString().getBytes()));

        // 组装查询账单域，仅提供组装方法的参考，需要根据实际账单进行修改
        List<Map<String, Object>> billList = new ArrayList<>();

        // 组装第一个账单报文
        Map<String, Object> billMap1 = new HashMap<>();
        billMap1.put("index", "01");
        billMap1.put("code", "S0_9800_0003");
        billMap1.put("billQueryId", "33243309395800164520  020220207");

        // 组装第一个账单的form
        Map<String, String> form1 = new HashMap<>();
        form1.put("usr_num", "11000021000149719015");
        form1.put("amount", "100");

        billMap1.put("form", form1);
        billList.add(billMap1);

        // 组装第二个账单报文
        Map<String, Object> billMap2 = new HashMap<>();
        billMap2.put("index", "02");
        billMap2.put("code", "S0_9800_0001");
        billMap2.put("billQueryId", "33225909174100164520  020220124");

        // 组装第二个账单的form
        Map<String, String> form2 = new HashMap<>();
        form2.put("usr_num", "91310114MA1GXBNG2T");
        form2.put("col_organ_cd", "21100000000");
        form2.put("col_voucher_no", "131011400002021010");
        form2.put("proc_flg", "03");
        form2.put("amount", "100");

        billMap2.put("form", form2);
        billList.add(billMap2);
        bodyMap.put("billDetailInfo", Base64.encodeBase64String(JSONObject.wrap(billList).toString().getBytes()));

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
        Map<String,Object> rspBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
        if (rspBody.get("billDetailInfo") != null) {
            String billDetailInfo = new String(Base64.decodeBase64((String) rspBody.get("billDetailInfo")));
            logger.info("应答账单域为：" + billDetailInfo + "\n");
        }

    }
}
