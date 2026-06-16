package com.unionpay.uas.demo.acp.bke;

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

import java.util.*;

/**
 * 营销活动列表查询   bizMethod：upntAcp.discount.list.query
 * 声明：以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己需要，按照技术文档编写。该代码仅供参考，不提供编码，性能，规范性等方面的保障<br>
 */
public class Demo19DiscountListQuery {

    private static final Logger logger = Logger.getLogger(Demo19DiscountListQuery.class);

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
        reqData.put(SDKConstants.param_bizMethod, "upntAcp.discount.list.query");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "7.0.1");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "987654321011111");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("accessType", "0"); // 0：普通商户直连接入; 1：收单机构接入; 2：平台类商户接入
        bodyMap.put("merId", "987654321011111"); //商户号，请修改为自己的商户号
        bodyMap.put("orderId", DemoUtil.getOrderId()); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("txnTime", DemoUtil.getTxnTime()); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("reqReserved", "这是请求方保留域"); //请求方保留域
        bodyMap.put("txnAmt", ""); //交易金额
        bodyMap.put("currencyCode", "156"); //交易币种
        bodyMap.put("transScene", ""); //交易场景

        // 注意：
        String accessType = (String) bodyMap.get("accessType");
        // 仅当accessType=1时，接入类型为收单机构时上送acqInsCode、merInfo
        if ("1".equals(accessType)) {
            bodyMap.put("acqInsCode", ""); // 收单机构号,根据实际接入方填写
            bodyMap.put("merInfo", buildMerInfo());    // 商户信息域,根据实际接入方填写
        } else if ("2".equals(accessType)) {
            // 仅当accessType=2时，接入类型为平台类商户接入时上送subMerInfo
            bodyMap.put("subMerInfo", buildSubMerInfo());
        }

        // 组装账户验证信息域，仅提供组装方法的参考，需要根据实际进行修改;
        Map<String, Object> accountVerifyInfo = new HashMap<>();
        accountVerifyInfo.put("certifTp", "");//证件类型
        accountVerifyInfo.put("certifId", "");//证件号码
        accountVerifyInfo.put("customerNm", "");//姓名
        accountVerifyInfo.put("smsCode", "");//短信验证码
        accountVerifyInfo.put("accNo", "");//账号
        accountVerifyInfo.put("phoneNo", "");//手机号
        accountVerifyInfo.put("expired", "");//有效期
        accountVerifyInfo.put("cvn2", "");
        accountVerifyInfo.put("accType", "");//账号类型(卡介质),取值范围 01：银行卡;05：token; 08：苹果PAY设备卡号; 09：安卓PAY设备卡号
        //商户入网配置敏感信息不加密时平铺上送
        bodyMap.put("accountVerifyInfo", Base64.encodeBase64String(JSONObject.wrap(accountVerifyInfo).toString().getBytes()));//账户验证信息域

        // 需要加密的敏感信息报文 根据实际业务填写,商户入网配置敏感信息加密时使用如下注释代码
        /*Map<String, Object> encDataMap = new HashMap<>();
        encDataMap.put("accountVerifyInfo", Base64.encodeBase64String(JSONObject.wrap(accountVerifyInfo).toString().getBytes()));

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
        // 如果不需要携带敏感信息并由网关解密, 则注释这行代码
        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap); */

        //Token信息域
        Map<String, Object> tokenInfoMap = new HashMap<>();
        tokenInfoMap.put("trId", ""); //标记请求者ID
        tokenInfoMap.put("tokenType", ""); //token类型
        tokenInfoMap.put("token", "");
        tokenInfoMap.put("tokenEnd", ""); //标记失效时间格式为yyyyMMddHHmmss
        bodyMap.put("tokenInfo", tokenInfoMap);

        //组装营销信息域，需要根据实际进行填写字段
        //bodyMap.put("discountInfo", buildDiscountInfo());

        //组装商户控制信息域
        //bodyMap.put("merCtrlInfo", buildMerCtrlInfo());

        bodyMap.put("maskedCardNo", "");//掩码卡号
        bodyMap.put("realCityCd", ""); //城市地区代码
        bodyMap.put("varietyInfoFlag", ""); //查询叠加组活动标识
        bodyMap.put("payAppId", ""); //支付工具代码

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
                    String decodeStr = GmUasService.gmDec(decData, decKey, certId);
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
        reqData.put(SDKConstants.param_bizMethod, "upntAcp.discount.list.query");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "7.0.1");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "00010000");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("accessType", "0"); // 0：普通商户直连接入; 1：收单机构接入; 2：平台类商户接入
        bodyMap.put("merId", "987654321011111"); //商户号，请修改为自己的商户号
        bodyMap.put("orderId", DemoUtil.getOrderId()); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("txnTime", DemoUtil.getTxnTime()); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("reqReserved", "这是请求方保留域"); //请求方保留域
        bodyMap.put("txnAmt", ""); //交易金额
        bodyMap.put("currencyCode", "156"); //交易币种
        bodyMap.put("transScene", ""); //交易场景

        // 注意：
        String accessType = (String) bodyMap.get("accessType");
        // 仅当accessType=1时，接入类型为收单机构时上送acqInsCode、merInfo
        if ("1".equals(accessType)) {
            bodyMap.put("acqInsCode", ""); // 收单机构号,根据实际接入方填写
            bodyMap.put("merInfo", buildMerInfo());    // 商户信息域,根据实际接入方填写
        } else if ("2".equals(accessType)) {
            // 仅当accessType=2时，接入类型为平台类商户接入时上送subMerInfo
            bodyMap.put("subMerInfo", buildSubMerInfo());
        }

        // 组装账户验证信息域，仅提供组装方法的参考，需要根据实际进行修改;
        Map<String, Object> accountVerifyInfo = new HashMap<>();
        accountVerifyInfo.put("certifTp", "");//证件类型
        accountVerifyInfo.put("certifId", "");//证件号码
        accountVerifyInfo.put("customerNm", "");//姓名
        accountVerifyInfo.put("smsCode", "");//短信验证码
        accountVerifyInfo.put("accNo", "");//账号
        accountVerifyInfo.put("phoneNo", "");//手机号
        accountVerifyInfo.put("expired", "");//有效期
        accountVerifyInfo.put("cvn2", "");
        accountVerifyInfo.put("accType", "");//账号类型(卡介质),取值范围 01：银行卡;05：token; 08：苹果PAY设备卡号; 09：安卓PAY设备卡号
        //商户入网配置敏感信息不加密时平铺上送
        bodyMap.put("accountVerifyInfo", Base64.encodeBase64String(JSONObject.wrap(accountVerifyInfo).toString().getBytes()));//账户验证信息域

        // 需要加密的敏感信息报文 根据实际业务填写,商户入网配置敏感信息加密时使用如下注释代码
        /*Map<String, Object> encDataMap = new HashMap<>();
        encDataMap.put("accountVerifyInfo", Base64.encodeBase64String(JSONObject.wrap(accountVerifyInfo).toString().getBytes()));

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
        // 如果不需要携带敏感信息并由网关解密, 则注释这行代码
        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap); */

        //Token信息域
        Map<String, Object> tokenInfoMap = new HashMap<>();
        tokenInfoMap.put("trId", ""); //标记请求者ID
        tokenInfoMap.put("tokenType", ""); //token类型
        tokenInfoMap.put("token", "");
        tokenInfoMap.put("tokenEnd", ""); //标记失效时间格式为yyyyMMddHHmmss
        bodyMap.put("tokenInfo", tokenInfoMap);

        //组装营销信息域，需要根据实际进行填写字段
        //bodyMap.put("discountInfo", buildDiscountInfo());

        //组装商户控制信息域
        //bodyMap.put("merCtrlInfo", buildMerCtrlInfo());

        bodyMap.put("maskedCardNo", "");//掩码卡号
        bodyMap.put("realCityCd", ""); //城市地区代码
        bodyMap.put("varietyInfoFlag", ""); //查询叠加组活动标识
        bodyMap.put("payAppId", ""); //支付工具代码

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

    private static Map buildMerInfo() {
        Map<String, Object> merInfo = new HashMap<String, Object>(); //接入类型为收单机构接入时需上送
        merInfo.put("merCatCode", ""); //接入类型为收单机构接入时需上送，填写MCC码
        merInfo.put("merName", ""); //接入类型为收单机构接入时需上送，不支持换行符等不可见字符
        merInfo.put("merAbbr", ""); //接入类型为收单机构接入时需上送，最长8位，不支持换行符等不可见字符
        return merInfo;
    }

    private static Map buildSubMerInfo() {
        Map<String, Object> subMerInfo = new HashMap<String, Object>(); //接入类型为平台类接入时需上送
        subMerInfo.put("subMerId", ""); //二级商户代码
        subMerInfo.put("subMerName", ""); //二级商户名称
        subMerInfo.put("subMerAbbr", ""); //二级商户简称
        return subMerInfo;
    }

    private static Map buildMerCtrlInfo() {
        Map<String, Object> merCtrlInfo = new HashMap<String, Object>();
        merCtrlInfo.put("noPromotionFlag", ""); //不参与营销标识
        return merCtrlInfo;
    }

    private static Map buildDiscountInfo() {
        // 营销信息域	discountInfo,本demo仅提供部分组装示例，根据自己需要组装对应域
        Map<String, Object> discountInfoMap = new HashMap<>();

        // 单品信息	goodsDetail  按需填写
        List<Map<String, Object>> goodsDetailList = new ArrayList<>();
        Map<String, Object> goodsDetail = new HashMap<String, Object>();
        goodsDetail.put("id", "");//商品编号
        goodsDetail.put("name", "");//商品名称
        goodsDetail.put("price", "");//商品单价
        goodsDetail.put("quantity", "");//商品数量
        goodsDetail.put("category", "");//商品类目
        goodsDetail.put("addnInfo", "");//附加信息
        goodsDetailList.add(goodsDetail);

        // 商品优惠信息	dctDetail  按需填写
        List<Map<String, Object>> dctDetailList = new ArrayList<>();
        Map<String, Object> dctDetail = new HashMap<String, Object>();
        dctDetail.put("id", "");//商品编号
        dctDetail.put("dctQuantity", "");//商品优惠数量
        dctDetail.put("dctPrice", "");//商品优惠金额
        dctDetail.put("dctId", "");//优惠活动编号
        dctDetailList.add(dctDetail);

        //营销活动信息	couponInfo 按需填写
        Map<String, Object> couponInfo = new HashMap<String, Object>();
        couponInfo.put("discountAmt", "");//总优惠金额
        couponInfo.put("merDiscountAmt", "");//商户出资金额
        couponInfo.put("activityId", "");//活动ID
        couponInfo.put("activityNm", "");//活动名称
        couponInfo.put("addnPrintInfo", "");//附加信息
        couponInfo.put("extInsId", "");//出资机构代码
        couponInfo.put("discountAtIns", "");//机构出资金额
        couponInfo.put("activityBalanceRate", "");//营销活动当天余额百分比
        couponInfo.put("availableForToday", "");//营销活动是否今日可用

        //营销活动列表	discountList 按需填写
        List<Map<String, Object>> discountList = new ArrayList<>();
        Map<String, Object> discount = new HashMap<String, Object>();
        discount.put("discountTp", "");//折扣类型
        discount.put("activityMark", "");//活动标识
        discount.put("description", "");//折扣活动描述
        discount.put("sku", "");//是否单品活动
        discount.put("discountId", "");//立减活动ID
        discount.put("activityNm", "");//立减活动名称
        discount.put("discountAmt", "");//立减活动金额
        discount.put("insmentDiscountAt", "");//手续费折扣金额
        discount.put("insmentPodList", "");//支持期数
        discount.put("interRateList", "");//期数费率
        discount.put("interRatePer", "");//分期手续费折扣比例
        discount.put("pointAmt", "");//积分额
        discount.put("varietyInfoList", "");//叠加营销活动组
        discountList.add(discount);

        // 叠加营销活动列表	subCouponList
        List<Map<String, Object>> subCouponList = new ArrayList<>();
        Map<String, Object> coupon = new HashMap<String, Object>();
        coupon.put("activityId", "");//活动ID
        coupon.put("discountAmt", "");//总优惠金额
        coupon.put("merDiscountAmt", "");//商户出资金额
        coupon.put("discountAtIns", "");//机构出资金额
        coupon.put("extInsId", "");//出资机构代码
        subCouponList.add(coupon);

        discountInfoMap.put("goodsDetail", goodsDetailList);
        //discountInfoMap.put("dctDetail", dctDetailList);
        //discountInfoMap.put("couponInfo", couponInfo);
        discountInfoMap.put("discountId", "");
        discountInfoMap.put("discountCode", "");
        discountInfoMap.put("productNum", "");
        //discountInfoMap.put("discountList", discountList);
        //discountInfoMap.put("subCouponList", subCouponList);

        return discountInfoMap;
    }

}
