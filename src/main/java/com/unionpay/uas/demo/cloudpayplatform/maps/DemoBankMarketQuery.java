package com.unionpay.uas.demo.cloudpayplatform.maps;

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

/**
 * 统一接出网关的示例demo:
 *  1. 接收接出网关发起的请求,解析请求头和请求报文
 *  2. 根据平台规范对请求数据进行验签
 *  3. 若有敏感信息则对敏感密文做解密
 *  4. 若需要返回敏感信息则对敏感明文做加密
 *  5. 根据平台规范对返回数据进行签名
 *  6. 将数据应答给接出网关
 *
 *  签名验签方式有国际和国密两种方式, 推荐使用国密算法
 *  本demo代码需要jdk版本为1.8.0_161及以上版本
 *  以下代码流程仅为示例且无法直接运行, 需要根据实际业务场景进行改造
 */
public class DemoBankMarketQuery {

    private static final Logger logger = Logger.getLogger(DemoBankMarketQuery.class);

    public static String gm(){

        GmSDKConfig.loadPropertiesFromSrc();


        // TODO 模拟已解析接出网关发起的HTTP请求头和请求报文
        // 注：在某些特殊场景(如HTTP 2.0)下收到的头字段可能为全小写, 此时需要自行解析时做兼容处理
        Map<String, String> reqHeader = new HashMap<>();
        reqHeader.put("appId","00010000");//银行收到银联的请求appid=00010000
        reqHeader.put("appType","00");//银行收到银联的请求appType=00
        reqHeader.put("Content-Type","application/json;charset=utf-8");
        reqHeader.put("version","1.0.0");
        reqHeader.put("sign","DamiFlRn103fdxiUQREgQrTSBrTC2+cqRGj3pmlESEGzSRtJICYs/04YkJTRZaobrqgmJ5jdpWX+BNbEuTpO5g==");
        reqHeader.put("bizMethod","upntMaps.bankCoupon.qry");
        reqHeader.put("signId","1");
        reqHeader.put("signMethod","SM2");
        reqHeader.put("reqId","9876543210111112022083115065220220831150652293469");

        String reqBodyStr = "{\"indUsrId\":\"c02102927528\",\"orderNo\":\"20220831150652293469\",\"txnTime\":\"20220831150652\",\"bussCd\":\"J0_9800_YX01\",\"bussInfo\":\"00030000\",\"currencyCode\":\"156\",\"txnAmt\":\"30000\",\"upDcAmt\":\"30000\",\"acqInsCode\":\"01039999\",\"termId\":\"222\",\"cardNo\":\"6226000000001235\",\"acqAddnData\":\"eyJkaXNjb3VudE1vZGUiOiIxIiwiZ2lmdFN1YmplY3QiOiJ2Y0x4anNVVEtyYnd3VmciLCJzaG9wT3JkZXJObyI6IjA0ODQyMTA4MTYzMzAwMDAwMDUwMDEiLCJzdG9yZUlkIjoiMTcxMyJ9\",\"mchntInfo\":{\"mchntCd\":\"987654321011111\",\"mchntCatCode\":\"6104\",\"mchntName\":\"DEMO商户\"}}";


        // 银行开始处理
        // 第一步：组装验签报文+验签
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

        //以下一行是为了计算银联->银行的请求报文的sign，银行不需要这一行
        Map<String, String> myReqMapgm = GmUasService.sign(verifyMap);

        if (!GmUasService.validate(myReqMapgm)) {
            logger.info("验证银联签名失败\n");
            return reqBodyStr;
        } else {
            logger.info("验证银联签名成功\n");
        }



        Map<String, Object> repBody = null;
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



        Map<String, String> resData = new TreeMap<>();

        // 组装HTTP应答报文头
        resData.put(SDKConstants.param_bizMethod, "upntMaps.bankCoupon.qry");       // 交易类型 根据实际业务填写
        resData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        resData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        // resData.put(SDKConstants.param_signMethod, "SM2-CERT");               // 签名方法
        resData.put(SDKConstants.param_appId, "00030000");        // 发送方系统索引号 根据实际业务填写  注意这里不是原样返回，而是填写真实机构appid
        resData.put(SDKConstants.param_appType, "01");                   // 01机构、02商户  根据实际业务填写
        resData.put(SDKConstants.param_signId, "69629716832");
        resData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP应答报文体 根据实际业务填写
        List<Object> bankCouponInfoList = new ArrayList<>();
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        Map<String, String> map3 = new HashMap<>();
        map1.put("issuerId", "0");
        map1.put("id", "a");
        map2.put("issuerId", "1");
        map2.put("id", "b");
        map3.put("issuerId", "2");
        map3.put("id", "c");
        bankCouponInfoList.add(map1);
        bankCouponInfoList.add(map2);
        bankCouponInfoList.add(map3);

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("code", "0000000000");
        bodyMap.put("msg", "交易成功");
        //bodyMap.put("subCode", "bodyValue3");//失败可选存在
        //bodyMap.put("subMsg", "bodyValue4");//失败可选存在
        bodyMap.put("indUsrId", repBody.get("indUsrId"));
        bodyMap.put("mchntInfo", "交易成功");
        
        Map<String, Object> bodyMchntMap = new HashMap<>(); 
        bodyMchntMap.put("mchntCd","987654321011111");
        bodyMchntMap.put("mchntCatCode","6104");
        bodyMap.put("mchntInfo", bodyMchntMap);//商户信息 

        bodyMap.put("orderNo", repBody.get("orderNo"));//订单号原样应答
        bodyMap.put("txnTime", repBody.get("txnTime"));//订单时间原样应答
        bodyMap.put("bankCouponInfo", bankCouponInfoList);//银行优惠信息 银行自营营销信息，银行侧自营营销存在时候返回
        bodyMap.put("resReserved", "demo保留域");
        
        //营销查询接口不涉及敏感信息
        //Map<String, Object> encDataMap = new HashMap<>(); // 需要加密的敏感信息报文 根据实际业务填写
        //encDataMap.put("bodyKey4", "bodyEncValue4");
        //encDataMap.put("bodyKey5", "bodyEncValue5");

        //String toBeEncData = JSONObject.wrap(encDataMap).toString(); // 待加密报文串
        //logger.info("加密前的敏感信息明文: " + toBeEncData);

        //Map<String, String> toBeEncMap = null;
        //try {
            // 随机生成sm4临时公钥16长度
         //   String key = DemoUtil.getRandomString(16);
         //   logger.info("随机生成的对称密钥key: " + key + "\n");
         //   toBeEncMap = GmUasService.gmEnc(toBeEncData, key, "utf-8");
      //  } catch (Exception e) {
         //   logger.error(e);
        //}

        //bodyMap.put(SDKConstants.param_encryptData, toBeEncMap); // 如果不需要携带敏感信息并由网关解密, 则注释这行代码

        resData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

        Map<String, String> resMap = GmUasService.sign(resData);

        // 最后将数据应答给银联接出网关
        logger.info(DemoUtil.getPrintMapWithLf(resMap));
        return DemoUtil.getPrintMapWithLf(resMap);
    }

    public static String rsa(){

        SDKConfig.loadPropertiesFromSrc();

        // TODO 模拟已解析接出网关发起的HTTP请求头和请求报文
        // 注：在某些特殊场景(如HTTP 2.0)下收到的头字段可能为全小写, 此时需要自行解析时做兼容处理
        //以下是 模拟银联->银行的请求报文
        Map<String, String> reqHeader = new HashMap<>();
        reqHeader.put("appId","00010000");
        reqHeader.put("appType","00");
        reqHeader.put("Content-Type","application/json;charset=utf-8");
        reqHeader.put("version","1.0.0");
        reqHeader.put("bizMethod","upntMaps.bankCoupon.qry");
        reqHeader.put("signId","2");
        reqHeader.put("signMethod","RSA2");
        reqHeader.put("reqId","9876543210111112022083115065220220831150652293470");
        String reqBodyStr = "{\"indUsrId\":\"c02102927528\",\"orderNo\":\"20220831150652293469\",\"txnTime\":\"20220831150652\",\"bussCd\":\"J0_9800_YX01\",\"bussInfo\":\"00020000\",\"currencyCode\":\"156\",\"txnAmt\":\"30000\",\"upDcAmt\":\"30000\",\"acqInsCode\":\"01039999\",\"termId\":\"222\",\"cardNo\":\"6226000000001235\",\"acqAddnData\":\"eyJkaXNjb3VudE1vZGUiOiIxIiwiZ2lmdFN1YmplY3QiOiJ2Y0x4anNVVEtyYnd3VmciLCJzaG9wT3JkZXJObyI6IjA0ODQyMTA4MTYzMzAwMDAwMDUwMDEiLCJzdG9yZUlkIjoiMTcxMyJ9\",\"mchntInfo\":{\"mchntCd\":\"987654321011111\",\"mchntCatCode\":\"6104\",\"mchntName\":\"DEMO商户\"}}";

        // 银行开始处理
        // 第一步：组装验签报文+验签
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
        //以下一行是为了计算银联->银行的请求报文的sign，银行不需要这一行
        Map<String, String> myReqMap = UasService.sign(verifyMap);

        // 银行对银联报文做验证
        if(!UasService.validate(myReqMap)) {
            logger.info("验证银联签名失败\n");
            return reqBodyStr;
        }else {
            logger.info("验证银联签名成功\n");
        }

        // 银行营销查询不涉及敏感信息
        Map<String, Object> repBody = null;
        try {
            logger.info("从银联获得HTTP请求报文为：" + reqBodyStr + "\n");
            repBody = DemoUtil.unwarpJson(new JSONObject(reqBodyStr));
            if (repBody.containsKey("encryptData")) {
                Map<String, Object> encryptData = (Map<String, Object>) repBody.get(SDKConstants.param_encryptData);
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



        Map<String, String> resData = new TreeMap<>();

        // 第二步：组装http应答报文头
        resData.put(SDKConstants.param_bizMethod, "upntMaps.bankCoupon.qry");       // 交易类型 根据实际业务填写
        resData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        resData.put(SDKConstants.param_signMethod, "RSA2");              // 签名方法
        // resData.put(SDKConstants.param_signMethod, "RSA2-CERT");              // 签名方法
        resData.put(SDKConstants.param_appId, "00020000");               // 发送方系统索引号 根据实际业务填写  注意这里不是原样返回，而是填写真实机构appid
        resData.put(SDKConstants.param_appType, "01");                   // 01机构、02商户
        resData.put(SDKConstants.param_signId, "2");
        resData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则



        // 组装HTTP应答报文体 根据实际业务填写
        List<Object> bankCouponInfoList = new ArrayList<>();
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        Map<String, String> map3 = new HashMap<>();
        map1.put("issuerId", "0");
        map1.put("id", "a");
        map2.put("issuerId", "1");
        map2.put("id", "b");
        map3.put("issuerId", "2");
        map3.put("id", "c");
        bankCouponInfoList.add(map1);
        bankCouponInfoList.add(map2);
        bankCouponInfoList.add(map3);

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("code", "0000000000");
        bodyMap.put("msg", "交易成功");
        //bodyMap.put("subCode", "bodyValue3");//失败可选存在
        //bodyMap.put("subMsg", "bodyValue4");//失败可选存在
        bodyMap.put("indUsrId", repBody.get("indUsrId"));
        bodyMap.put("mchntInfo", "交易成功");
        
        Map<String, Object> bodyMchntMap = new HashMap<>(); 
        bodyMchntMap.put("mchntCd","987654321011111");
        bodyMchntMap.put("mchntCatCode","6104");
        bodyMap.put("mchntInfo", bodyMchntMap);//商户信息 

        bodyMap.put("orderNo", repBody.get("orderNo"));//订单号原样应答
        bodyMap.put("txnTime", repBody.get("txnTime"));//订单时间原样应答
        bodyMap.put("bankCouponInfo", bankCouponInfoList);//银行优惠信息 银行自营营销信息，银行侧自营营销存在时候返回
        bodyMap.put("resReserved", "demo保留域");



        //营销查询接口不涉及敏感信息加密
        //Map<String, Object> encDataMap = new HashMap<>(); // 需要加密的敏感信息报文 
        //encDataMap.put("acqAddnData", "bodyEncValue4");//收款方附加数据

        //String toBeEncData = JSONObject.wrap(encDataMap).toString(); // 待加密报文串
        //logger.info("加密前的敏感信息明文:" + toBeEncData);

        //Map<String, String> toBeEncMap = null;
        //try {
            // 随机生成临时公钥32长度
          //  String key = DemoUtil.getRandomString(32);
            //logger.info("随机生成的对称密钥key: " + key + "\n");
            //toBeEncMap = UasService.rsaEnc(toBeEncData, key, "utf-8");
        //} catch (Exception e) {
          //  logger.error(e);
        //}

        //bodyMap.put(SDKConstants.param_encryptData, toBeEncMap); // 如果不需要携带敏感信息并由网关解密, 则注释这行代码

        resData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

        Map<String, String> resMap = UasService.sign(resData);

        // 最后将数据应答给银联接出网关
        logger.info(DemoUtil.getPrintMapWithLf(resMap));
        return DemoUtil.getPrintMapWithLf(resMap);


    }

    public static void main(String[] args) {
        // 使用国密方法
        gm();

        // 使用国际方法
        //rsa();
    }


}
