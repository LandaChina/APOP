package com.unionpay.uas.demo.acp.fts;

import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConfig;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.UasService;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUasService;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class DemoApplyQr {

    private static final Logger logger = Logger.getLogger(DemoApplyQr.class);

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
        reqData.put(SDKConstants.param_bizMethod, "soluFts.fts.cis.applyQr");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("accessType", "0"); // 0：普通商户直连接入; 1：收单机构接入; 2：平台类商户接入
        bodyMap.put("merId", ""); //商户号，请修改为自己的商户号
        bodyMap.put("orderId", DemoUtil.getOrderId()); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("txnTime", DemoUtil.getTxnTime()); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("pnrInsIdCd", ""); //服务商机构标识码
        bodyMap.put("txnAmt", ""); //交易金额
        bodyMap.put("currencyCode", ""); //交易币种
        bodyMap.put("timeStart", "");//yyyyMMddHHmmss， 订单开始时间:未填写以订单发送时间开始生效，若填写则以订单开始时间生效
        bodyMap.put("timeExpired", ""); //yyyyMMddHHmmss，订单失效时间。动态码订单有效期最长半小时。
        bodyMap.put("maskedCardNo", ""); //卡号掩码 商户限定卡号掩码支付，需同时上送用户手机号码，仅在联合登陆场景下使用。（按照聚分期返回的掩码原样上送）
        bodyMap.put("reqReserved", ""); //请求方保留域
        bodyMap.put("sceneFlag", ""); //场景标志 01：保险实名认证 02：联合登陆 03：限定身份信息
        bodyMap.put("loginSt", ""); // 登录状态 联合登陆场景下上送登陆状态，表明 用户在商户侧的登陆状态 0-未登录 1-已登录 不上送默认为0

        ArrayList storeList = new ArrayList();
        Map storeInfoMap = new HashMap();
        storeInfoMap.put("storeName","");
        storeInfoMap.put("store","");
        storeList.add(storeInfoMap);

        bodyMap.put("storeList", storeList); //门店列表 用来标志线下商户的门店信息，storeName用于前端展示商户门店名称（需与 store一起上送该字段，不能单独上送），不能超过15个汉字和字符
        bodyMap.put("backUrl", "");//后台通知地址
        bodyMap.put("frontUrl", ""); // 前端回跳地址 付款完成后回跳至商户前端页面地址
        bodyMap.put("trxChnnel", ""); // 渠道类型 01：线下门店02：线上收银台
        bodyMap.put("reserved", ""); // 银联保留域
        bodyMap.put("supportFaceAuthFlag", ""); //是否支持人脸识别 0：不支持未上送默认代表支持注：若是商户app接入聚分期需支持聚分期H5调用“getUserMedia”权限，上线前需进行app兼容性验证
        bodyMap.put("termId", ""); // 终端号 商户存在真实终端号时上送

        // 注意：
        String accessType = (String)bodyMap.get("accessType");
        // 仅当accessType=1时，接入类型为收单机构时上送acqInsCode、merInfo
        if ("1".equals(accessType)) {
            bodyMap.put("acqInsCode", ""); // 收单机构号,根据实际接入方填写
            bodyMap.put("merInfo", buildMerInfo());    // 商户信息域,根据实际接入方填写
        } else if ("2".equals(accessType)) {
            // 仅当accessType=2时，接入类型为平台类商户接入时上送subMerInfo
            bodyMap.put("subMerInfo", buildSubMerInfo());
        }

        //限定银行 商户指定银行分期支付，则填上该值，用array格式上送。多个银行代码例如["ABC", "ACBC", "JCB"] 银行代码简称详见附录B 若上送了卡号或卡号掩码无需上送改字段。若上送需与卡号对应银行保持一致
        bodyMap.put("suppBankName", Arrays.asList("ABC","ACBC"));

        //限定分期期数 用于商户指定分期期数，用array格式上送，支持上送多个期数，例如["3","6","9","12"]
        bodyMap.put("limitNum", Arrays.asList("3", "6"));


        // 需要加密的敏感信息报文 根据实际业务填写,商户入网配置敏感信息加密时使用如下注释代码
        Map<String, Object> encDataMap = new HashMap<>();

        // 组装身份信息域，仅提供组装方法的参考，需要根据实际进行修改;
        Map<String, Object> customerInfoMap = new HashMap<>();
        customerInfoMap.put("certifTp", "");//证件类型
        customerInfoMap.put("certifId", "");//证件号码
        customerInfoMap.put("customerNm", "");//姓名
        encDataMap.put("customerInfo", customerInfoMap); // 场景标识为“01-实名认证”情况下， 必须上送实名信息； 场景标识为02或者03时可选上送该字段，如若送，支持送掩码证件号码或者不送，姓名必送；身份信息如： {“certifTp”:“01”,“certified”:“value”,“customerNm”:“value”}，需加密

        encDataMap.put("phoneNo", ""); //手机号 联合登陆场景下上送用户手机号 手机号需加密上送 （白名单商户才能 支持联登），需加密

        //标记化支付信息域	tokenPayData 商户若在交易完成后,需获取持卡人付款token号需上送,需加密
        Map<String, Object> tokenPayDataMap = new HashMap<>();
        tokenPayDataMap.put("trId", ""); //标记请求者ID
        tokenPayDataMap.put("tokenType", ""); //token类型
        encDataMap.put("tokenPayData", tokenPayDataMap);

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
        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap);

        // 组装风险控制信息信息域，仅提供组装方法的参考，
//        bodyMap.put("riskRateInfo", buildRiskRateInfo());

        //组装营销信息域，需要根据实际进行填写字段
//        bodyMap.put("discountInfo", buildDiscountInfo());

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
        reqData.put(SDKConstants.param_bizMethod, "soluFts.fts.cis.applyQr");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("accessType", ""); // 0：普通商户直连接入; 1：收单机构接入; 2：平台类商户接入
        bodyMap.put("merId", ""); //商户号，请修改为自己的商户号
        bodyMap.put("orderId", DemoUtil.getOrderId()); //商户订单号，8-32位数字字母，不能含“-”或“_”，可以自行定制规则，此处为演示方便默认取了个值
        bodyMap.put("txnTime", DemoUtil.getTxnTime()); //订单发送时间，格式为yyyyMMddHHmmss，取北京时间，此处为演示方便默认取了个值
        bodyMap.put("pnrInsIdCd", ""); //服务商机构标识码
        bodyMap.put("txnAmt", ""); //交易金额
        bodyMap.put("currencyCode", ""); //交易币种
        bodyMap.put("timeStart", "");//yyyyMMddHHmmss， 订单开始时间:未填写以订单发送时间开始生效，若填写则以订单开始时间生效
        bodyMap.put("timeExpired", ""); //yyyyMMddHHmmss，订单失效时间。动态码订单有效期最长半小时。
        bodyMap.put("maskedCardNo", ""); //卡号掩码 商户限定卡号掩码支付，需同时上送用户手机号码，仅在联合登陆场景下使用。（按照聚分期返回的掩码原样上送）
        bodyMap.put("reqReserved", ""); //请求方保留域
        bodyMap.put("sceneFlag", ""); //场景标志 01：保险实名认证 02：联合登陆 03：限定身份信息
        bodyMap.put("loginSt", ""); // 登录状态 联合登陆场景下上送登陆状态，表明 用户在商户侧的登陆状态 0-未登录 1-已登录 不上送默认为0

        ArrayList storeList = new ArrayList();
        Map storeInfoMap = new HashMap();
        storeInfoMap.put("storeName","");
        storeInfoMap.put("store","");
        storeList.add(storeInfoMap);
        bodyMap.put("storeList", storeList); //门店列表 用来标志线下商户的门店信息，storeName用于前端展示商户门店名称（需与 store一起上送该字段，不能单独上送），不能超过15个汉字和字符
        bodyMap.put("backUrl", "");//后台通知地址
        bodyMap.put("frontUrl", ""); // 前端回跳地址 付款完成后回跳至商户前端页面地址
        bodyMap.put("trxChnnel", ""); // 渠道类型 01：线下门店02：线上收银台
        bodyMap.put("reserved", ""); // 银联保留域
        bodyMap.put("supportFaceAuthFlag", ""); //是否支持人脸识别 0：不支持未上送默认代表支持注：若是商户app接入聚分期需支持聚分期H5调用“getUserMedia”权限，上线前需进行app兼容性验证
        bodyMap.put("termId", ""); // 终端号 商户存在真实终端号时上送

        // 注意：
        String accessType = (String)bodyMap.get("accessType");
        // 仅当accessType=1时，接入类型为收单机构时上送acqInsCode、merInfo
        if ("1".equals(accessType)) {
            bodyMap.put("acqInsCode", ""); // 收单机构号,根据实际接入方填写
            bodyMap.put("merInfo", buildMerInfo());    // 商户信息域,根据实际接入方填写
        } else if ("2".equals(accessType)) {
            // 仅当accessType=2时，接入类型为平台类商户接入时上送subMerInfo
            bodyMap.put("subMerInfo", buildSubMerInfo());
        }

        //限定银行 商户指定银行分期支付，则填上该值，用array格式上送。多个银行代码例如["ABC", "ACBC", "JCB"] 银行代码简称详见附录B 若上送了卡号或卡号掩码无需上送改字段。若上送需与卡号对应银行保持一致
        bodyMap.put("suppBankName", Arrays.asList("ABC","ACBC"));

        //限定分期期数 用于商户指定分期期数，用array格式上送，支持上送多个期数，例如["3","6","9","12"]
        bodyMap.put("limitNum", Arrays.asList("3", "6"));


        // 需要加密的敏感信息报文 根据实际业务填写,商户入网配置敏感信息加密时使用如下注释代码
        Map<String, Object> encDataMap = new HashMap<>();

        // 组装身份信息域，仅提供组装方法的参考，需要根据实际进行修改;
        Map<String, Object> customerInfoMap = new HashMap<>();
        customerInfoMap.put("certifTp", "");//证件类型
        customerInfoMap.put("certifId", "");//证件号码
        customerInfoMap.put("customerNm", "");//姓名
        encDataMap.put("customerInfo", customerInfoMap); // 场景标识为“01-实名认证”情况下， 必须上送实名信息； 场景标识为02或者03时可选上送该字段，如若送，支持送掩码证件号码或者不送，姓名必送；身份信息如： {“certifTp”:“01”,“certified”:“value”,“customerNm”:“value”}，需加密

        encDataMap.put("phoneNo", ""); //手机号 联合登陆场景下上送用户手机号 手机号需加密上送 （白名单商户才能 支持联登），需加密

        //标记化支付信息域	tokenPayData 商户若在交易完成后,需获取持卡人付款token号需上送,需加密
        Map<String, Object> tokenPayDataMap = new HashMap<>();
        tokenPayDataMap.put("trId", ""); //标记请求者ID
        tokenPayDataMap.put("tokenType", ""); //token类型
        encDataMap.put("tokenPayData", tokenPayDataMap);

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
        bodyMap.put(SDKConstants.param_encryptData, toBeEncMap);

        // 组装风险控制信息信息域，仅提供组装方法的参考，
        //bodyMap.put("riskRateInfo", buildRiskRateInfo());

        //组装营销信息域，需要根据实际进行填写字段
        //bodyMap.put("discountInfo", buildDiscountInfo());



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

    private static Map buildMerInfo(){
        Map<String, Object> merInfo = new HashMap<String, Object>(); //接入类型为收单机构接入时需上送
        merInfo.put("merCatCode", ""); //接入类型为收单机构接入时需上送，填写MCC码
        merInfo.put("merName", ""); //接入类型为收单机构接入时需上送，不支持换行符等不可见字符
        merInfo.put("merAbbr", ""); //接入类型为收单机构接入时需上送，最长8位，不支持换行符等不可见字符
        return merInfo;
    }

    private static Map buildSubMerInfo(){
        Map<String, Object> subMerInfo = new HashMap<String, Object>(); //接入类型为平台类接入时需上送
        subMerInfo.put("subMerId", ""); //二级商户代码
        subMerInfo.put("subMerName", ""); //二级商户名称
        subMerInfo.put("subMerAbbr", ""); //二级商户简称
        return subMerInfo;
    }


    private static Map buildRiskRateInfo(){
        // 需要根据实际进行填写字段的增加或者减少
        Map<String, Object> riskRateInfo = new HashMap<String, Object>();
        riskRateInfo.put("shippingFlag", "");//商品风险类别标识
        riskRateInfo.put("shippingCountryCode", "");//收货地址-国家
        riskRateInfo.put("shippingProvinceCode", "");//收货地址-省
        riskRateInfo.put("shippingCityCode", "");

        return riskRateInfo;
    }


    private static Map buildDiscountInfo(){
        // 营销信息域	discountInfo,本demo仅提供部分组装示例，根据自己需要组装对应域
        Map<String, Object> discountInfoMap = new HashMap<>();

        Map<String, Object> goodsInfoMap = new HashMap<String, Object>();
        goodsInfoMap.put("id","");
        goodsInfoMap.put("name","");
        goodsInfoMap.put("price","");
        goodsInfoMap.put("quantity","");
        goodsInfoMap.put("category","");
        goodsInfoMap.put("addnInfo","");

        ArrayList<Map> goodsInfoList= new ArrayList<>();
        goodsInfoList.add(goodsInfoMap);

        discountInfoMap.put("discountId", "1234567890123456");//营销活动ID
        discountInfoMap.put("discountCode", "1245");
        discountInfoMap.put("goodsInfo", goodsInfoList);

        return discountInfoMap;
    }


}
