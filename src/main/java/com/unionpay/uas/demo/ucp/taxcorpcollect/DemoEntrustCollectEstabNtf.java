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
 * 内容网关税费对公代收业务，签约通知接口demo
 * 声明：以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己需要，按照技术文档编写。该代码仅供参考，不提供编码，性能，规范性等方面的保障<br>
 */
public class DemoEntrustCollectEstabNtf {

    private static final Logger logger = Logger.getLogger(DemoEntrustCollectEstabNtf.class);

    public static void main(String[] args) {
        // 使用国密算法调用此方法
        GmDemo();

        // 使用国际算法调用此方法
        //RsaDemo();
    }

    public static void GmDemo() {

        // 初始化配置文件，读取证书
        GmSDKConfig.loadPropertiesFromSrc();
        // TODO 模拟已解析接出网关发起的HTTP请求头和请求报文

        // 注：在某些特殊场景(如HTTP 2.0)下收到的头字段可能为全小写, 此时需要自行解析时做兼容处理
        Map<String, String> reqHeader = new HashMap<>();
        //  收到的body的报文如，具体字段含义见规范
        String reqBodyStr = "{\"bussInfo\":\"16627868347755953\",\"contractDate\":\"20230222\",\"orderId\":\"20230227165709000001\",\"origOrderId\":\"16627868347755953\",\"contractResult\":\"81\",\"accessType\":\"0\",\"origTxnDate\":\"20230223\",\"actReserved\":\"这是账户方保留域\",\"subCode\":\"0000000000\",\"encryptData\":{\"encryptMethod\":\"SM4\",\"certId\":\"1\",\"data\":\"akAvNhPaMQlOv+A2PGuUz4rwajfpCdbNL0nQZFIfvENZdMScG4RkdaZ5tWEy+YLpKiiaWmIyGLVUQpR5woKu2Cr8s5421p+RbvHrCKZEQm7FgDZzLuFrOevmRORiC25gVfhJBI3rksvZN4Is/UMljc+hQuu7RSbk2YWhV6+41szS+qq84VzjRBiXXYIrQ13GUk6qVq9jnXFWYjhC2ZEBPRrY5ukCAzSV5NC1+Ofsn9F1LA8rcT1al3Jrjx2ysY+qPATw4SVRbNL8zP0ramsEE3X4j/QPVyNqVYlk/9kHp3TQmxtgVdtgDQAY9URkiVql1LRknQYQIiVpOB/lzA1nSvh/213ckiti456S7xQX+fgzRP14pCWhKpALY1X5ucBu2GL50AMNug9SlRNrZtyieA==\",\"key\":\"BH/UoCg3kcZpJuyc3eVHovSddi5n5xyKi/UGdLdYd5LbdlAbC24D4n5d7x/gebOP6qCIdix/eThNbGlDYTISPC+ZtW7vbLgL8U/dwBzSd+IWJywXSU+KeDDYaPMmVaf3BQXe8VryaonbA5hhWfbFPms=\"},\"reqReserved\":\"这是请求方保留域\",\"txnTime\":\"20230227165709\",\"merId\":\"987654321011111\",\"subMsg\":\"成功\",\"contractAcctInfo\":\"eyJlcENlcnRpZlR5cGUiOiIwMSIsImFjY3RObyI6IjExMTEyMjIyMzMzMzQ0NDQ1NTUiLCJjZXJ0aWZObyI6IjQxMDUwMDE5OTYwOTEwNjMzMiIsImVwQ2VydGlmTm8iOiIxMTc3Mjg4Mzg0ODQiLCJiYW5rTm8iOiIxMTExMjIyMjMzMzMiLCJhY2N0VHlwZSI6IjEwIiwibmFtZSI6IuWwj+W8oCIsImNlcnRpZlR5cGUiOiIwMSIsImFjY3ROYW1lIjoi5bCP546LIn0=\",\"contractScene\":\"01\"}";

        // 组装验签报文
        Map<String, String> verifyMap = new HashMap<>();
        verifyMap.put(SDKConstants.param_signId, reqHeader.get(SDKConstants.param_signId));
        verifyMap.put(SDKConstants.param_sign, reqHeader.get(SDKConstants.param_sign));
        verifyMap.put(SDKConstants.param_bizMethod, reqHeader.get(SDKConstants.param_bizMethod));
        verifyMap.put(SDKConstants.param_version, reqHeader.get(SDKConstants.param_version));
        verifyMap.put(SDKConstants.param_signMethod, reqHeader.get(SDKConstants.param_signMethod));
        verifyMap.put(SDKConstants.param_appId, reqHeader.get(SDKConstants.param_appId));
        verifyMap.put(SDKConstants.param_reqId, reqHeader.get(SDKConstants.param_reqId));
        verifyMap.put(SDKConstants.param_signPubKeyCert, reqHeader.get(SDKConstants.param_signPubKeyCert));
        verifyMap.put(SDKConstants.param_body, reqBodyStr);

        if (!GmUasService.validate(verifyMap)) {
            logger.info("验证银联签名失败\n");
            return;
        } else {
            logger.info("验证银联签名成功\n");
        }

        Map<String, Object> repBody;
        try {
            logger.info("从银联获得HTTP请求报文为：" + reqBodyStr + "\n");
            repBody = DemoUtil.unwarpJson(new JSONObject(reqBodyStr));
            if (repBody.containsKey("encryptData")) {
                Map<String, Object> encryptData = (Map<String, Object>) repBody.get(SDKConstants.param_encryptData);
                String decKey = (String) encryptData.get(SDKConstants.param_key);
                String decData = (String) encryptData.get(SDKConstants.param_data);
                String certId = (String) encryptData.get(SDKConstants.param_certId);
                try {
                    String decodeStr = GmUasService.gmDec(decData, decKey, certId);
                    logger.info("解银联加密后的敏感信息的报文：" + decodeStr);
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        } catch (JSONException e) {
            logger.info("应答体解json失败。");
        }

        /***********************返回应答示例****************************/

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "upntAcp.ucp.tax.corporate.entrust.collect.notify");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "7.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "987654321011111");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据通知业务参数填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("accessType", "0"); //1机构，0商户
        bodyMap.put("merId", "987654321011111"); //商户号，请修改为自己的商户号
        bodyMap.put("orderId", DemoUtil.getOrderId()); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("txnTime", DemoUtil.getTxnTime()); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("contractScene", "01"); //01 受理机构主动发起的签约 02 账户机构主动发起的签约
        bodyMap.put("contractResult", "81"); //81：签约成功 80：签约失败
        bodyMap.put("origOrderId", "16627868347755953"); //当签约场景为01时必须出现填写原签约交易交易流水号
        bodyMap.put("origTxnDate", "20230223"); //当签约场景为01时必须出现

        // 签约账户信息域 contractAcctInfo
        Map<String, Object> contractAcctInfoMap = new HashMap<>();
        // 签约交易时必填字段 bankNo、acctNo、acctType、acctName
        contractAcctInfoMap.put("bankNo", "111122223333"); //开户行行号
        contractAcctInfoMap.put("acctNo", "1111222233334444555"); //账号
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

        bodyMap.put("bussInfo", "16627868347755953"); //填写原签约交易中的业务信息
        bodyMap.put("contractDate", "20230222"); //当签约结果为81时出现
        bodyMap.put("subCode", "0000000000"); //当签约结果为80时出现
        bodyMap.put("subMsg", "成功"); //当签约结果为80时出现
        bodyMap.put("reqReserved", "这是请求方保留域"); //可选
        bodyMap.put("actReserved", "这是账户方保留域"); //可选，供账户机构向商户传输信息使用

        reqData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());
        logger.info("从银联获得通知报文为：" + JSONObject.wrap(bodyMap).toString() + "\n");

        Map<String, String> reqMap = GmUasService.sign(reqData);

        // 最后将数据应答给银联接出网关


    }

    public static void RsaDemo() {

        // 初始化配置文件，读取证书
        SDKConfig.loadPropertiesFromSrc();

        // TODO 模拟已解析接出网关发起的HTTP请求头和请求报文
        // 注：在某些特殊场景(如HTTP 2.0)下收到的头字段可能为全小写, 此时需要自行解析时做兼容处理
        Map<String, String> reqHeader = new HashMap<>();
        // 收到的body的报文如，具体字段含义见规范
        String respBodyStr = "{\"bussInfo\":\"16627868347755953\",\"contractDate\":\"20230222\",\"orderId\":\"20230227170837000001\",\"origOrderId\":\"16627868347755953\",\"contractResult\":\"81\",\"accessType\":\"0\",\"origTxnDate\":\"20230223\",\"actReserved\":\"这是账户方保留域\",\"subCode\":\"0000000000\",\"encryptData\":{\"encryptMethod\":\"AES\",\"certId\":\"2\",\"data\":\"aI7fk7sNonWLkmyLaHk+IU6j9+NvR3egGib7CpZUND6JzRrGPvnPEH8zLIgk9EeiHdKq9MlRC29LIea/3JnqWnFy8P6S33Vdtbz9iH5eq+lOkloMcgoyQ5M/zFjKjCurFFs87w4yTGLOrjP8wzUNpTBFA9JhxVRCT9RL24qRLknIxGbi/0qlL1WW8sXfkziXcrnA0sA+//JRgqH2xgFzRbob8KYj4Mpqp8jNxoyjRMirKdTxSYkBnUW6YLY2VfEZW29l993B8ReXVMcMFBvRPluF12NVkFgShZQwdo2xJFNBKq3kwnKj2XRzEm+CDL1ltXCHrPc9RdRyxL2d3QKWmvMqevEpNzEEHRwQ98Dt+9OTK0963vZTv8G154vhfAesUDPlgqio0KFiCzdYQhZF0A==\",\"key\":\"jumhNjIfZS5DKq0YNL7nDkLBRsA/LLtVt5j8MwEkgL3CvzQ5+1jYCwT0WoK+R7AwWgxNQ3gB43kz1zoPL50rSPkQ9wI+UbDBC+yj8mokNWNCSwEeRDgHp5G7jdSbySmBG7xkU4J6KMcUoUYNWmmvZGxKvsRtKXRl7xpws2bYKLOBhxFKBZuGlK7DTUoA/ZE398tTUgKoQ+foimACrXOK++SIezoD7Iq7e7XFfZXtPsNVzbDCC3iER/LOPMQPeIznxr7BRPWI/BgZ8QKk30B3SQC6ppimZM3pcZqxGHwsTt6jOM9BHQY+PYxNQcSaBfjsCg16SWsUNRceWwF41wckhA==\"},\"reqReserved\":\"这是请求方保留域\",\"txnTime\":\"20230227170837\",\"merId\":\"987654321011111\",\"subMsg\":\"成功\",\"contractAcctInfo\":\"eyJlcENlcnRpZlR5cGUiOiIwMSIsImFjY3RObyI6IjExMTEyMjIyMzMzMzQ0IiwiY2VydGlmTm8iOiI0MTA1MDAxOTk2MDkxMDYzMzIiLCJlcENlcnRpZk5vIjoiMTE3NzI4ODM4NDg0IiwiYmFua05vIjoiMTExMTIyMjIzMzMzIiwiYWNjdFR5cGUiOiIxMCIsIm5hbWUiOiLlsI/lvKAiLCJjZXJ0aWZUeXBlIjoiMDEiLCJhY2N0TmFtZSI6IuWwj+eOiyJ9\",\"contractScene\":\"01\"}";
        // 组装验签报文
        Map<String, String> verifyMap = new HashMap<>();
        verifyMap.put(SDKConstants.param_signId, reqHeader.get(SDKConstants.param_signId));
        verifyMap.put(SDKConstants.param_sign, reqHeader.get(SDKConstants.param_sign));
        verifyMap.put(SDKConstants.param_bizMethod, reqHeader.get(SDKConstants.param_bizMethod));
        verifyMap.put(SDKConstants.param_version, reqHeader.get(SDKConstants.param_version));
        verifyMap.put(SDKConstants.param_signMethod, reqHeader.get(SDKConstants.param_signMethod));
        verifyMap.put(SDKConstants.param_appId, reqHeader.get(SDKConstants.param_appId));
        verifyMap.put(SDKConstants.param_reqId, reqHeader.get(SDKConstants.param_reqId));
        verifyMap.put(SDKConstants.param_signPubKeyCert, reqHeader.get(SDKConstants.param_signPubKeyCert));
        verifyMap.put(SDKConstants.param_body, respBodyStr);

        if(!UasService.validate(verifyMap)) {
            logger.info("验证银联签名失败\n");
            return;
        }else {
            logger.info("验证银联签名成功\n");
        }

        Map<String, Object> respBody;
        try {
            logger.info("从银联获得HTTP请求报文为：" + respBodyStr + "\n");
            respBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
            if (respBody.containsKey("encryptData")) {
                Map<String, Object> encryptData = (Map<String, Object>) respBody.get(SDKConstants.param_encryptData);
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
        } catch (JSONException e) {
            logger.info("解析JSON失败");
        }


        /***********************返回应答示例****************************/
        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "upntAcp.ucp.tax.corporate.entrust.collect.notify");       // 交易类型 根据实际业务填写
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
        bodyMap.put("contractScene", "01"); //01 受理机构主动发起的签约 02 账户机构主动发起的签约
        bodyMap.put("contractResult", "81"); //81：签约成功 80：签约失败
        bodyMap.put("origOrderId", "16627868347755953"); //当签约场景为01时必须出现填写原签约交易交易流水号
        bodyMap.put("origTxnDate", "20230223"); //当签约场景为01时必须出现

        // 签约账户信息域 contractAcctInfo
        Map<String, Object> contractAcctInfoMap = new HashMap<>();
        // 签约交易时必填字段 bankNo、acctNo、acctType、acctName
        contractAcctInfoMap.put("bankNo", "111122223333"); //开户行行号
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
        // 如果不需要携带敏感信息并由网关解密, 则注释这行代码
        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap);

        bodyMap.put("bussInfo", "16627868347755953"); //填写原签约交易中的业务信息
        bodyMap.put("contractDate", "20230222"); //当签约结果为81时出现
        bodyMap.put("subCode", "0000000000"); //当签约结果为80时出现
        bodyMap.put("subMsg", "成功"); //当签约结果为80时出现
        bodyMap.put("reqReserved", "这是请求方保留域"); //可选
        bodyMap.put("actReserved", "这是账户方保留域"); //可选，供账户机构向商户传输信息使用

        reqData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());
        logger.info("从银联获得通知报文为：" + JSONObject.wrap(bodyMap).toString());

        Map<String, String> reqMap = UasService.sign(reqData);

        // 最后将数据应答给银联接出网关


    }
}
