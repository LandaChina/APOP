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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.Deflater;

/**
 *  cbs 直连商户 demo-订单交易（批量）
 *  本交易在订单支付产品中，由商户（ME）向银联（UP）发起，本交易应用于批量发起订单的场景，最多包含1000笔订单信息。
 * 本交易为同步+异步应答模式。银联收到商户的请求后，会实时返回同步应答（仅告知指令受理情况）；后续银联处理完指令中所有的订单后主动向商户发送订单交易（批量）处理完成通知
 */
public class DemoOrderBatch {

    private static final Logger logger = Logger.getLogger(DemoOrderBatch.class);

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
        reqData.put(SDKConstants.param_bizMethod, "pay.b2b.000121");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 具体字段含义及要求参见规范
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("acqInsCode", "CBS12345"); //1机构，0商户
        bodyMap.put("merId","123456789012345");
        bodyMap.put("batchNo", DemoUtil.getTxnTime().substring(10)); //商户号，请修改为自己的商户号
        bodyMap.put("txnDate", DemoUtil.getTxnTime().substring(0,8)); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("sndTime", DemoUtil.getTxnTime().substring(8)); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("totalQty", "1"); //
        bodyMap.put("fileType", "01"); //
        bodyMap.put("fileContent",buildFileContent((String) bodyMap.get("merId")));

        bodyMap.put("backUrl","http://xxx.xxx.xxx.xxx:port/xx");
        bodyMap.put("remark","我是remark123");
        bodyMap.put("summary","我是summary123");
        bodyMap.put("reqReserved",buildReservd());
        bodyMap.put("reserved",buildReservd());

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
        reqData.put(SDKConstants.param_bizMethod, "pay.b2b.000121");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 具体字段含义及要求参见规范
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("acqInsCode", "CBS12345"); //1机构，0商户
        bodyMap.put("merId","123456789012345");
        bodyMap.put("batchNo", DemoUtil.getTxnTime().substring(10)); //商户号，请修改为自己的商户号
        bodyMap.put("txnDate", DemoUtil.getTxnTime().substring(0,8)); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("sndTime", DemoUtil.getTxnTime().substring(8)); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("totalQty", "1"); //
        bodyMap.put("fileType", "01"); //
        bodyMap.put("fileContent",buildFileContent((String) bodyMap.get("merId")));

        bodyMap.put("backUrl","http://xxx.xxx.xxx.xxx:port/xx");
        bodyMap.put("remark","我是remark123");
        bodyMap.put("summary","我是summary123");
        bodyMap.put("reqReserved",buildReservd());
        bodyMap.put("reserved",buildReservd());

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
    private static String buildFileContent(String merId){
        String fileHeader = "1.0.0,1,1,reqReser1,reqReser2\n" ;
        String fileEnd = "END,1\n" ;

        String fileContent1 = fileHeader +  buildFilelines(merId) + fileEnd;
        String base64FileContent1 = "";
        try {
            byte[] bFileContent1 = CbsUtils.deflateEncode(fileContent1.getBytes("utf-8"));
            base64FileContent1 = new String(bFileContent1,"utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return base64FileContent1;
    }

    public static String buildFilelines(String merId){
        Map<String,Object> body = buildFileLineMap(merId);
        StringBuilder title = new StringBuilder();
        StringBuilder fileLines = new StringBuilder();
        body.keySet().forEach(k->{
            title.append(k).append(",");
            fileLines.append(body.get(k)).append(",");
        });
        String t = title.toString().substring(0,title.length()-1) + "\n";
        String f = fileLines.toString().substring(0,fileLines.length()-1) + "\n";

        return t + f;
    }


    protected static Map<String,Object> buildFileLineMap(String merId) {
        Map<String,Object> body = new HashMap<>();
        body.put("acqInsCode","CBS12345");
        body.put("txnNo", CbsUtils.getCbsTxnNo(merId));
        body.put("txnDate", DemoUtil.getTxnTime().substring(0,8));
        body.put("sndTime", DemoUtil.getTxnTime().substring(8));
        body.put("platformOrderNo", DemoUtil.getOrderId());
        body.put("payeeInfo",buildPayeeInfo());
        body.put("payerInfo",buildPayerInfo());
        body.put("currencyCode","156");
        body.put("txnAmt","1");
        body.put("orderExpTime", LocalDate.now().plusDays(10).format(DateTimeFormatter.ofPattern("yyyyMMdd")) + DemoUtil.getTxnTime().substring(8));
        body.put("backUrl","http://xxx.xxx.xxx.xxx:port/xx");
        body.put("txnPlatform","01");
        body.put("orderType","03");
        body.put("freezeModule","1");
        body.put("remark","我是remark123");
        body.put("summary","我是summary123");
        body.put("merName","测试商户");
        body.put("merId","123456789012345");
        body.put("mcc","9999");
        body.put("mchntRegionCd","1234");
        body.put("merchantType","101");
        body.put("subMerInfo", buildSubMerInfo());
        body.put("riskInfo",buildRiskInfo());
        body.put("reqReserved",buildLineReservd());
        body.put("reserved",buildLineReservd());

        return body;
    }

    private static String buildRiskInfo(){
        HashMap<String,String> riskInfo = new HashMap<>();
        riskInfo.put("deviceModel","EMUI1237.24sdfu999.1");
        riskInfo.put("deviceLang","111");
        riskInfo.put("deviceIPAddr","127.0.0.1");
        riskInfo.put("deviceMACAddr","00-24-7E-0A-6C-2E");
        riskInfo.put("deviceSerialNo","73445678901278901562389012123456");
        riskInfo.put("deviceGPSPos","+37.12/-121.23");
        riskInfo.put("deviceSIMNo","1358888888818966666666");
        riskInfo.put("deviceSIMQty","2");
        riskInfo.put("userId","1000000001");
        riskInfo.put("riskPoint","9999");
        riskInfo.put("riskMsg","优质客户");
        riskInfo.put("regTimeAcq",DemoUtil.getTxnTime());
        riskInfo.put("regMailAcq","5778342@163.com");
        riskInfo.put("shipProv","22");
        riskInfo.put("shipCity","33");
        riskInfo.put("productType","1");
        String str = JSONObject.wrap(riskInfo).toString();
        String base64 = Base64.encodeBase64String(str.getBytes(StandardCharsets.UTF_8));

        return base64;
    }

    private static String buildSubMerInfo(){
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
        subMerUnits.put("subMerTotalAmt","123456789012345678");
        subMerUnits.put("subMerTotalKindsQty","12345");
        subMerUnits.put("prodUnitQty","4");
        subMerUnits.put("prodUnits",prodUnitsList);

        ArrayList<Map> subMerUnitsList = new ArrayList<>();
        subMerUnitsList.add(subMerUnits);
        subMerUnitsList.add(subMerUnits);
//        subMerUnitsList.add(subMerUnits);
//        subMerUnitsList.add(subMerUnits);

        HashMap<String,Object> subMerInfo = new HashMap<>();
        subMerInfo.put("subMerTotalQty","9999");
        subMerInfo.put("subMerShowQty","9");
        subMerInfo.put("subMerUnits",subMerUnitsList);

        String str = JSONObject.wrap(subMerInfo).toString();
        String base64 = Base64.encodeBase64String(str.getBytes(StandardCharsets.UTF_8));

        return base64;
    }

    private static String  buildPayeeInfo(){
        Map<String,Object> payeeInfo = new HashMap<>();
        payeeInfo.put("bankNo","000123455543");
        payeeInfo.put("bankName","测试银行名超长超长超长超超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长");
        payeeInfo.put("acctNo","000123455543");
        payeeInfo.put("acctName","收款方账户名称");
        payeeInfo.put("acctType","01");
        payeeInfo.put("contactName","收款人联系人明细");
        payeeInfo.put("contactPhone","15317256068");
        payeeInfo.put("unifmCode","15317256068asdCSWF");
        String str = JSONObject.wrap(payeeInfo).toString();

        String base64 = Base64.encodeBase64String(str.getBytes(StandardCharsets.UTF_8));

        return base64;

    }

    private static String buildPayerInfo(){
        Map<String,Object> payerInfo = new HashMap<>();
        payerInfo.put("bankNo","000123455543");
        payerInfo.put("bankName","测试银行名超长超长超长超超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长超长");
        payerInfo.put("acctNo","000123455543");
        payerInfo.put("acctName","付款人账户名称");
        payerInfo.put("acctType","01");
        payerInfo.put("contactName","付款人联系姓名");
        payerInfo.put("contactPhone","15317256068");
        payerInfo.put("unifmCode","15317256068aSWdddd");
        String str = JSONObject.wrap(payerInfo).toString();

        String base64 = Base64.encodeBase64String(str.getBytes(StandardCharsets.UTF_8));

        return base64;
    }

    private static String buildLineReservd(){
        HashMap<String,Object> map = new HashMap<>();
        map.put("reserved1","我是保留域1");
        map.put("reserved2","我是保留域2");
        map.put("reserved3","我是保留域3");
//        map.put("天知道这是啥",buildPayeeInfo());
        String str = JSONObject.wrap(map).toString();

        String base64 = Base64.encodeBase64String(str.getBytes(StandardCharsets.UTF_8));

        return base64;

    }
    private  static Map buildReservd(){
        HashMap<String,Object> map = new HashMap<>();
        map.put("reserved1","我是保留域1");
        map.put("reserved2","我是保留域2");
        map.put("reserved3","我是保留域3");
//        map.put("天知道这是啥",buildPayeeInfo());

        return map;
    }


}
